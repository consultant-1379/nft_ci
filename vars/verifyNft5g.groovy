/*
 * Gerrit patchset-verify code including SonarQube integration
 */
import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate
import java.util.regex.Matcher
import java.util.regex.Pattern

def call(body) {
    def config_in = [:]
    def config    = [:]

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    stage("Initialize") {
        config = nft5g.customizeConfig_verify(params, config_in)
    }

    podTemplate(
        cloud:               config.cloud,
        namespace:           config.namespace,
        label:               config.label,
        containers:          config.containers,
        imagePullSecrets:    [config.credentials])
    {
        node(config.label) {
            stage("Clone ${config.project_name}") {
                nft5g.checkOutRepository(config)
            }
            stage("Compile") {
                container("build") {
                    def mvn_options = ""
                    if (config.skip_tests) {
                        mvn_options = "-DskipTests"
                    }
                    result = runMaven("clean install ${mvn_options}", config.project_dir)
                }
            }

            stage("Code analyis") {
                container("build") {
                    result = runMaven("checkstyle:checkstyle", config.project_dir)
                }
            }

            if (!config.skipTests) {
                stage("Unit test") {
                    container("build") {
                        result = runMaven("test", config.project_dir)
                    }
                }
            }

            stage("Sonar Scanner") {
                withSonarQubeEnv('code-analyzer') {
                    def sonar_params = getSonarScannerParameters(config)
                    container("build") {
                        result = runMaven("sonar:sonar ${sonar_params}", config.project_dir)
                    }
                }  // withSonarQubeEnv
            }
            if (result == 0) {
                stage("Quality Gate verdict") {
                    container("sonar") {
                        result = sonarVerdict(config.project_dir.split(/\//)[-1], config.branch, config.sonar_token)
                    }
                }
            }
        }
    }
}


//---------------------------------------------------------------------------------------------------------------------

def sonarVerdict(prjKey, branch, token) {
    common.log('debug', "sonarVerdict::begin(${prjKey}, ${branch})")
    def qg = [:]
    def QG_RETRIES = 5

    // Make sure "report-task.txt" is available in its original path
    sh(returnStatus:true, script:"cat ${env.WORKSPACE}/scanner/report-task.txt")

    @NonCPS
    int index = 1
    while (index <= QG_RETRIES) {
        try {
            timeout(time: 2, unit: 'MINUTES') {
                qg = waitForQualityGate(credentialsId:"sonar-token-adp-lmera")
            }
            break
        } catch (exc) {
            if (index < QG_RETRIES) {
                common.log('warning', "Failed to obtain Quality Gate result. Retrying")
                sleep 10
            } else {
                common.msgError("Unable to get a Quality Gate result from SonarQube: ${exc}")
            }
        }
        index++
    }
    common.log('debug', "sonarVerdict:: QG result: ${qg?.status}")

    if (qg?.status == 'IN_PROGRESS') {
        common.log('WARNING', "sonarVerdict:: SonarQube didn't answer whitin time: Skipping")
    } else if (qg?.status != 'OK') {
        common.msgError("Aborted due to quality gate verdict: ${qg?.status}")
    }

    common.log('debug', "sonarVerdict::end")
}


//---------------------------------------------------------------------------------------------------------------------

def getSonarScannerParameters(cfg) {
    common.log('debug', "getSonarScannerParameters::begin(${cfg})")
    def lang_params = new ArrayList()
    def xtra_params = new ArrayList()
    def skip_paths  = [".release", ".debug", "vendor", ".git", ".doc", ".scannerwork", ".mvn"]
    def branch = cfg.branch?:"master"
    def files = common.getListFromSh(
        "find . -name \\*.java | grep -v ${skip_paths.join(' | grep -v ')}")
    common.log('debug', "source files: ${files}")
    def ltests   = new ArrayList()
    def lsources = new ArrayList()
    String  regex   = /.*(test\.|\/test_|Test\.|\/Test_).*/
    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL)
    def prj_version = nft5g.getProjectVersion("${cfg.project_dir}/pom.xml")

    if (cfg.sonar_token != null && cfg.sonar_token != '') {
        xtra_params.add("-D sonar.login=${cfg.sonar_token}")
    }
    if (cfg.DEBUG?:false == true) {
        xtra_params.add('-X')
    }
    def exclusions = "**/*.pb.h,**/*.pb.cc,**/*.capnp.c++,**/*.capnp.h," +\
        "CMakeLists.txt,${skip_paths.join('/**/*,')}/**/*"
    lang_params.add("sonar.host.url=https://codeanalyzer2.internal.ericsson.com/")
    lang_params.add("sonar.projectBaseDir=.")
    lang_params.add("sonar.projectKey=${cfg.project_dir.split(/\//)[-1]}")
    lang_params.add("sonar.projectName=${cfg.project_name}/${cfg.project_dir}")
    lang_params.add("sonar.projectVersion=${prj_version}")
    lang_params.add("sonar.exclusions=${exclusions}")
    lang_params.add("sonar.cpd.exclusions=${exclusions}")
    lang_params.add("sonar.coverage.exclusions=${exclusions}")
    lang_params.add("sonar.login=6f29249c680e04051797afb66baed4e338e64744")
    lang_params.add("sonar.working.directory=${WORKSPACE}/scanner")
    lang_params.add("sonar.branch.name=${branch}")

    files.each {
        Matcher matcher = pattern.matcher(it)
        if (! matcher.matches()) {
            lsources.add(it)
        }
    }
    if (lsources.size() == 0) {
        common.msgError("No source code or test files where found in this repository")
    }

    // Put all paramters together
    def all_params = "-D${lang_params.join(' -D')} ${xtra_params.join(' ')}"

    common.log('debug', "getSonarScannerParameters::end(${all_params})")
    return all_params
}


//---------------------------------------------------------------------------------------------------------------------

// Uses function 'runMavenDirect' to build dependencies if not done yet
// Then uses 'runMavenDirect' again to execute the provided Maven rule
def runMaven(target, prj_dir) {
    common.log('debug', "runMaven::begin(${target})")
    def result = 0
    def settings_file = common.getPipelineResource().SETTINGS_XML

    container("build") {
        def cmd = "mvn --settings ${settings_file} ${target} -q"
        common.log('debug', "Command to execute: '${cmd}'")
        result = sh(returnStatus:true, script:"cd ${prj_dir}; ${cmd}")
        if (result != 0) {
           error "Command failed: '${cmd}'"
        }
    }

    common.log('debug', "runMaven::end(${result})")
    return result
}
