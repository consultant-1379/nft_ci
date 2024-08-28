/**
 * This file contains helper automation functions for the Jenkins NFT pipelines
 */
import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import java.util.regex.Matcher
import hudson.console.ConsoleNote


/**
 * Clones a GIT repository from Gerrit, supports regular and Gerrit events
 * based clones (like 'patchset-verify' or 'change-merged').
 *
 *  Parameters:
 *   __NAME__________________  __USAGE__  __DESCRIPTION________________________________________
 *   ('cfg' input map)
 *   repository                MANDATORY  Git repository URL, relays on the Integration library
 *                                        for the right URL to use.
 *   credentials               MANDATORY  Proper authentication to access Git repository,
 *                                        provided by the Integration library.
 *   NFT_BRANCH                OPTIONAL   NFT branch for compiling NFT code (default: master)
 *                                        usually: "master:latest" for testing NFT TC changes
 *   COMMIT_ID                 OPTIONAL   Gerrit commit ID just for description purposes
 *                                        autocalculated if omitted
 *   ('env' (Jenkins global variables)
 *   GERRIT_REFSPEC            OPTIONAL   Defined automatically by the Gerrit plugin in Jenkins
 *   GERRIT_PATCHSET_REVISION             Defined automatically by the Gerrit plugin in Jenkins
 *
 */
def checkOutRepository(cfg) {
    common.log('debug', "checkOutRepository::begin(repo=${cfg.repository}, nft_branch=${cfg.NFT_BRANCH}, " +
        "patchset=${env.GERRIT_PATCHSET_REVISION}, refspec=${env.GERRIT_REFSPEC}, commitId=${cfg.COMMIT_ID})")
    def cfg_out = cfg

    if (isVerifyMode()) {
        cfg_out.commitId = checkOutRepoPatchSet(cfg_out.repository, cfg_out.credentials)
    } else {
        cfg_out.commitId = checkOutRepoBranch(cfg_out.repository, cfg_out.NFT_BRANCH, cfg_out.credentials, cfg_out.COMMIT_ID)
    }

    // Keep a list with all submodules for, i.e., skip Dockerfiles in them
    cfg_out.submodules = common.getListFromSh("git submodule foreach --quiet 'echo \$name'")
    common.log('debug', "checkOutRepository:: Submodules found: ${cfg_out.submodules}")
    common.log('debug', "checkOutRepository::end(commitId=${cfg_out.commitId})")
    return cfg_out
}


/**
 * Clones Gerrit change pending for code review (patchset-verify)
 *
 *  Parameters:
 *   __NAME__________________  __USAGE__  __DESCRIPTION________________________________________
 *   repository                MANDATORY  Gerrit project, relays on the Integration library
 *                                        for the right URL
 *   credentials               MANDATORY  Proper authentication to access Git repository,
 *                                        provided by the Integration library
 *   ['env' (Jenkins global variables)]
 *   GERRIT_REFSPEC                       Defined automatically by the Gerrit plugin in Jenkins
 *   GERRIT_PATCHSET_REVISION             Defined automatically by the Gerrit plugin in Jenkins
 *
 */
def checkOutRepoPatchSet(repository, credentials) {
    common.log('debug', "checkOutRepoPatchSet::begin()")
    checkout([
        $class: 'GitSCM',
        branches: [[name: env.GERRIT_PATCHSET_REVISION]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[
            $class:              'SubmoduleOption',
            disableSubmodules:   false,
            parentCredentials:   true,
            recursiveSubmodules: true,
            reference:           '',
            trackingSubmodules:  false
        ]],
        submoduleCfg: [],
        userRemoteConfigs: [[
            credentialsId: credentials,
            refspec: env.GERRIT_REFSPEC,
            url: repository
        ]]
    ])
    common.log('debug', "checkOutRepoPatchSet::end()")
    return env.GERRIT_PATCHSET_REVISION[0..6]
}


/**
 * Clones a branch from Gerrit project (change-merged)
 *
 *  Parameters:
 *   __NAME__________________  __USAGE__  __DESCRIPTION________________________________________
 *   repository                MANDATORY  Gerrit project, relays on the Integration library
 *                                        for the right URL
 *   branch_in                 MANDATORY  Repository branch to clone
 *   credentials               MANDATORY  Proper authentication to access Git repository,
 *                                        provided by the Integration library
 *   COMMIT_ID                 OPTIONAL   Gerrit commit ID just for description purposes
 *                                        autocalculated if omitted
 *   directory                 OPTIONAL   If provided cloned files will be created into this
 *                                        directory
 */
def checkOutRepoBranch(repository, branch_in, credentials, commit_id, directory=null) {
    common.log('debug', "checkOutRepoBranch::begin()")
    def branch = 'master'
    if (branch_in != null && branch_in.toLowerCase() != 'latest')
        branch = branch_in

    if (directory == null) {
        checkout([
            $class: 'GitSCM',
            branches:   [[name: branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[
                $class:              'SubmoduleOption',
                disableSubmodules:   false,
                parentCredentials:   true,
                recursiveSubmodules: true,
                reference:           '',
                trackingSubmodules:  false
            ]],
            submoduleCfg: [],
            userRemoteConfigs: [[
                credentialsId: credentials,
                url:           repository
            ]]
        ])
    } else {
        checkout([
            $class: 'GitSCM',
            branches:   [[name: branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [
                    $class:              'SubmoduleOption',
                    disableSubmodules:   false,
                    parentCredentials:   true,
                    recursiveSubmodules: true,
                    reference:           '',
                    trackingSubmodules:  false
                ],
                [
                    $class: 'RelativeTargetDirectory',
                    relativeTargetDir: directory
                ]
            ],
            submoduleCfg: [], 
            userRemoteConfigs: [[
                credentialsId: credentials,
                url:           repository
            ]]
        ])
    }
    if (commit_id?:'' != '') {
        sh(returnStatus:true, script:"git checkout ${commit_id}")
    }
    common.log('debug', "checkOutRepoBranch::end()")
    return sh(returnStdout:true, script:"git show --format=%h --quiet").trim()
}


/**
 * Return true if the Jenkins build was trigger by a Gerrit verify-type event
 */
def isVerifyMode() {
    common.log('debug', "isVerifyMode::begin(GERRIT_EVENT_TYPE=${env.GERRIT_EVENT_TYPE}," +
        "GERRIT_REFSPEC=${env.GERRIT_REFSPEC},GERRIT_PATCHSET_REVISION=${env.GERRIT_PATCHSET_REVISION})")
    def result = true

    if (env.GERRIT_EVENT_TYPE == null && env.GERRIT_PATCHSET_REVISION == null)
    {
        result = false
    }

    common.log('debug', "isVerifyMode::end(${result})")
    return result
}


/**
 * Returns NFT_Automation_5G Jenkins jobs default values in a map data type
 * collecting all needed environment/Jenkins-global variables and all build parameters.
 *
 *  Parameters:
 *   __NAME__________________  __USAGE__  __DESCRIPTION________________________________________
 *   repository                Optional   Git repository URL, relays on the Integration library
 *                                        for the right URL to use
 *   credentials               Optional   Proper authentication to access Git repository,
 *                                        provided by the Integration library
 *   NFT_BRANCH                Optional   NFT branch for executing NFT (default: master)
 *                                        usually: dropXX for a proper alligment with installed
 *                                        system configuration files and traffic
 */
def customizeConfig(parms, cfg_in=[:]) {
    env.DEBUG = ((parms.DEBUG?:env.DEBUG?:'false').toBoolean() ||
                  common.getPipelineResource().debug.value).toString()
    common.log('debug', "customizeConfig::begin(${parms}, ${cfg_in})")
    def cfg_out = [:]
    cfg_out['DEBUG'] = env.DEBUG.toBoolean()

    // Adding all Jenkins build parameters
    parms.keySet().each { key->
        cfg_out[key] = parms[key]
    }
    // What Jenkins parameter provides is actually the TG (even if the value first points to the environment name)
    // This is due to the way "ExtendedChoiceParameter" works: It shows a list of Environments but it is internally
    // mapped to the TGs list
    if (parms.containsKey('CLOUD')) {
        cfg_out['BUILD_NODE'] = parms['CLOUD']
    }

    // Adding all Jenkinsfile parameters (they have priority over build parameters)
    cfg_in.keySet().each { key->
        cfg_out[key] = cfg_in[key]
    }

    // Complete missing common parameters with defaults.
    // If they already exist they will keep their value.
    if (!cfg_out.containsKey('repository'))
        cfg_out.repository    = common.formatGerritUrl("HSS/CCSM/nft")
    if (!cfg_out.containsKey('NFT_BRANCH'))
        cfg_out.NFT_BRANCH    = 'master'
    if (!cfg_out.containsKey('UDM_NFT_AUTO_VERSION'))
        cfg_out.UDM_NFT_AUTO_VERSION = 'latest'
    if (!cfg_out.containsKey('credentials'))
        cfg_out.credentials   = common.getCredentialId('hss', 'usrpwd')
    if (!cfg_out.containsKey('jdk_version'))
        cfg_out.jdk_version   = "11.0.21"
    if (!cfg_out.containsKey('maven_version'))
        cfg_out.maven_version = "3.5.3"
    if (!cfg_out.containsKey('timeout'))
        cfg_out.timeout = Math.ceil((cfg_out.DURATION?:"1").toFloat())*3
    cfg_out.timeout = Math.round(cfg_out.timeout*60).toInteger()
    if (!cfg_out.containsKey('ENABLE_MTLS'))
        cfg_out.ENABLE_MTLS = true
    if (!cfg_out.containsKey('SMALL_DEPLOYMENT'))
        cfg_out.SMALL_DEPLOYMENT = false
    if (!cfg_out.containsKey('UNINSTALL_TOOLS'))
        cfg_out.UNINSTALL_TOOLS = false
    if (!cfg_out.containsKey('IS_ANSI'))
        cfg_out.IS_ANSI = false
    if (!cfg_out.containsKey('UMTS_ENABLED'))
        cfg_out.UMTS_ENABLED = false
    if (!cfg_out.containsKey('EIR_MAP_ENABLED'))
        cfg_out.EIR_MAP_ENABLED = false
    if (!cfg_out.containsKey('SIMU_TRAFFIC_ENABLED')){
        cfg_out.SIMU_TRAFFIC_ENABLED = false
    }
    if (!cfg_out.containsKey('SMS_OVER_NAS')){
        cfg_out.SMS_OVER_NAS = false
        cfg_out.UMTS_ENABLED = false
        cfg_out.SIMU_TRAFFIC_ENABLED = false
        cfg_out.EIR_MAP_ENABLED = false
        cfg_out.IS_ANSI = false
    }
    if (!cfg_out.containsKey('UDM_AUSF_EIR'))
        if (JOB_NAME.contains('_UDM_AUSF_EIR'))
            cfg_out.UDM_AUSF_EIR = true
        else
            cfg_out.UDM_AUSF_EIR = false
    if (!cfg_out.containsKey('EPC'))
        if (JOB_NAME.contains('_EPC'))
            cfg_out.EPC = true
        else
            cfg_out.EPC = false
    if (!cfg_out.containsKey('IMS'))
        if (JOB_NAME.contains('_IMS'))
            cfg_out.IMS = true
        else
            cfg_out.IMS = false
    if (!cfg_out.containsKey('NRF'))
         cfg_out.NRF = true

    if (!cfg_out.containsKey('project_dir'))
        cfg_out.project_dir = common.SOURCE_ROOT_DIR

    // Jenkins job classification
    cfg_out.jobPreffix = getCcsmNftJobPreffix(cfg_out)
    cfg_out.cl2Type = getCcsmNftJobType()
    cfg_out.jobSuffix = getCcsmNftJobSuffix(cfg_out)

    common.log('debug', "customizeConfig::end(${cfg_out})")
    return cfg_out
}

/**
 * Return CCSM NFT Jenkins job preffix if applicable, empty otherwise
 */
def getCcsmNftJobPreffix(def cfg, def jobname=JOB_NAME) {
    common.log('debug', "getCcsmNftJobPreffix::begin(${jobname})")
    def preffix = ''
    if (jobname.contains('UDM_AUSF_EIR')) {
        preffix = 'CCSM_NFT_UDM_AUSF_EIR_'
    } else if (jobname.contains('_IMS_EPC')) {
        preffix = 'CCSM_NFT_IMS_EPC_'
    } else if (jobname.contains('_EPC')) {
        preffix = 'CCSM_NFT_EPC_'
    } else if (jobname.contains('_IMS')) {
        preffix = 'CCSM_NFT_IMS_'
    } else if (jobname.contains('CCSM_NFT_')) {
        preffix = 'CCSM_NFT_'
    }
    common.log('debug', "getCcsmNftJobPreffix::end(${preffix})")
    return preffix
}


/**
 * Return type of CCSM NFT Jenkins job if applicable, null otherwise
 * possible values are: {'Release', 'Internal', null}
 */
def getCcsmNftJobType(def jobname=JOB_NAME) {
    common.log('debug', "getCcsmNftJobType::begin(${jobname})")
    def cl2Type = ''
    if (jobname.contains('RELEASE')) {
        cl2Type = 'Release'
    } else if (jobname.contains('INTERNAL')) {
        cl2Type = 'Internal'
    }
    common.log('debug', "getCcsmNftJobType::end(${cl2Type})")
    return cl2Type
}


/**
 * Return CCSM NFT Jenkins job suffix if applicable, empty otherwise
 * Both CLEAR and IPV6 will be returned if they are in use (in that order)
 */
def getCcsmNftJobSuffix(def cfg, def jobname=JOB_NAME) {
    common.log('debug', "getCcsmNftJobSuffix::begin(${jobname})")
    def suffix = ''
    if ((cfg.containsKey('ENABLE_MTLS') && !cfg.ENABLE_MTLS) && !jobname.contains('INTERNAL') || jobname.contains('_CLEAR')) {
        suffix += "_CLEAR"
    }
    if ((cfg.containsKey('IPV6') && cfg.IPV6) || jobname.contains('_IPV6')) {
        suffix += "_IPV6"
    }
    common.log('debug', "getCcsmNftJobSuffix::end(${suffix})")
    return suffix
}


/**
 * Load "config" map with needed values for a code verify Jenkins build
 */
def customizeConfig_verify(parms, cfg_in) {
    env.DEBUG = ((parms.DEBUG?:env.DEBUG?:'false').toBoolean() ||
                  common.getPipelineResource().debug.value).toString()
    common.log('debug', "customizeConfig::begin(${parms}, ${cfg_in})")
    def cfg_out = [:]
    cfg_out['DEBUG'] = env.DEBUG.toBoolean()

    // Check mandatory parameters
    ['project_name', 'GERRIT_BRANCH', 'GERRIT_REFSPEC'].each { key->
        if (!cfg_in.containsKey(key) && !parms.containsKey(key)) {
            error "Missing mandatory parameter: ${key}"
        }
    }

    // User values
    cfg_in.keySet().each { key->
        common.log('debug', "customizeConfig:: Found Jenkinsfile option: ${key}")
        cfg_out[key] = cfg_in[key]  // Add rest of user parameters to the config map
    }
    if (!cfg_out.containsKey('skip_tests'))
        cfg_out.mvn_options = false

    // Autocalc extra parameters
    if (!cfg_out.containsKey('cloud'))
        cfg_out.cloud        = "kubernetes"
    if (!cfg_out.containsKey('label'))
        cfg_out.label        = "${JOB_NAME}${BUILD_NUMBER}"
    if (!cfg_out.containsKey('repository'))
        cfg_out.repository   = common.formatGerritUrl("HSS/CCSM/nft")
    if (!cfg_out.containsKey('credentials'))
        cfg_out.credentials  = common.getCredentialId('hss', 'usrpwd')
    cfg_out.repository = common.formatGerritUrl(cfg_out.repository)
    if (!cfg_out.containsKey('branch'))
        cfg_out.branch       = parms.GERRIT_BRANCH
    if (!cfg_out.containsKey('refspec'))
        cfg_out.refspec      = parms.GERRIT_REFSPEC
    if (!cfg_out.containsKey('project_dir'))
        cfg_out.project_dir  = common.SOURCE_ROOT_DIR
    if (!cfg_out.containsKey('sonar_server'))
        cfg_out.sonar_server = "sonarqube-5g-lmera"

    // Prepare containers
    List<ContainerTemplate> containers = new ArrayList<ContainerTemplate>()
    // Change default JNLP image to "alpine"
    containers.add(
        containerTemplate(
            name:                  "jnlp",
            image:                 "armdocker.rnd.ericsson.se/proj-5g-cicd-release/jenkins/inbound-agent:4.11-1-jdk11-eric-certs",
            alwaysPullImage:       false,
            args:                  '${computer.jnlpmac} ${computer.name}'
        )
    )
    // Maven steps
    containers.add(
        containerTemplate(
            name:                  "build",
            image:                 "armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-java11mvnbuilder:latest",
            alwaysPullImage:       false,
            ttyEnabled:            true,
            command:               'cat'
        )
    )
    // Sonar steps
    containers.add(
        containerTemplate(
            name:                  "sonar",
            image:                 "armdocker.rnd.ericsson.se/proj-5g-cicd-dev/jenkins/mason/hss_sonar_scanner:latest",
            alwaysPullImage:       false,
            ttyEnabled:            true,
            command:               'cat'
        )
    )
    cfg_out.containers = containers

    common.log('debug', "customizeConfig_verify::end(${cfg_out})")
    return cfg_out
}


/**
 * Locate client Jar file to execute in Jenkins build workspace
 */
def getJarFile(jarMajorVer) {
    common.log('debug', "getJarFile::begin(${jarMajorVer})")
    def result = null
    def jarList = common.getListFromSh("find ${WORKSPACE} -name 'ccsm-cli*.jar'")
    if (jarMajorVer == 1) {
        jarList = common.getListFromSh("find ${WORKSPACE} -name 'ccsm-nonfunctional-testcases*-jenkins.jar'")
    }

    if (jarList.size() == 0) {
        error "Unable to find a JAR client to execute"
    } else {
        result = jarList.sort()[-1]
    }
    common.log('debug', "getJarFile::end(${result})")
    return result
}


/**
 * Execute JCAT Java command with all needed parameters
 */
def execute_java_command(cfg) {
    common.log('debug', "execute_java_command::begin(${cfg})")
    def prj_version     = getProjectVersion("${cfg.project_dir}/pom.xml")
    int jarMajorVersion = getJarMajorVersion(prj_version)
    def jcatParams      = getJcatParams(cfg, jarMajorVersion)
    def jarFile         = getJarFile(jarMajorVersion)
    def jarParams       = ''

    if (jarMajorVersion <= 2) {
        if (jarMajorVersion == 2) {
            jarParams = getJarParams(cfg, jarMajorVersion)
        }
        jarParams = "${jarParams} ${cfg.testsuite}"
        if (cfg.testsuite.startsWith('install')) {
            if (cfg.traffic?:false == false) {
                jarParams = "${jarParams} -w"
            }
        } else if (!cfg.testsuite.startsWith('cleanup') && cfg.ts_xml?:"" != "") {
            jarParams = "${jarParams} ${cfg.ts_xml}"
        }
    }

    def exec_cmd = """
        . /etc/home/bashrc
        export PATH="$HOME/bin:$PATH"
        mkdir -p $HOME/bin
        rm -f $HOME/bin/helm
        ln -s /usr/local/bin/helm-v3.13.0 $HOME/bin/helm
        helm version
        module add openjdk/${cfg.jdk_version}
        module add maven/${cfg.maven_version}
        module list
        java -version
        cd ${cfg.project_dir}
    """
    if ((cfg.EPC || cfg.IMS) && cfg.testsuite.startsWith('batch')) {
        exec_cmd = """
            . /etc/home/bashrc
            export ST_TOOL_PATH=/opt/hss/system_test
            export TTCN3_DIR=/opt/titan/5/R3A
            export TTCN3_LICENSE_FILE=/proj/hss_est/tcm_env/ttcn_admin/LICENSES/stccsm.dat
            export PATH=$PATH:/opt/titan/5/R3A/bin:/opt/hss/system_test/bin:/usr/bin/:/sbin/:/usr/local/bin:/usr/bin:/bin:/usr/bin/X11:/usr/gam:/opt/hss/system_test/Automation
            export BAT_CFG_PATH=/opt/hss/system_test/share/BAT/

            echo "Verify CNHSS environment:"
            get_env_info
            get_env_info --cloud-credential

            export PATH="$HOME/bin:\$PATH"
            mkdir -p $HOME/bin
            rm -f $HOME/bin/helm
            ln -s /usr/local/bin/helm-v3.13.0 $HOME/bin/helm
            helm version
            module add openjdk/${cfg.jdk_version}
            module add maven/${cfg.maven_version}
            module list
            java -version
            cd ${cfg.project_dir}
        """
    }

    withCredentials([usernamePassword(
           credentialsId: 'userpwd-adp',
           usernameVariable: 'JENKINS_USER',
           passwordVariable: 'JENKINS_PASSWORD')])
    {
        jcatParams += " -Ddtg.remotePassword=${JENKINS_PASSWORD}"
        exec_cmd += """exec java ${jcatParams} -jar ${jarFile} ${jarParams}
        """
        sh exec_cmd
    }

    common.log('debug', "execute_java_command::end")
}


/**
 * Get all needed parameters to execute JCAT Java command
 */
def getJcatParams(cfg, jarMajorVersion) {
    common.log('debug', "getJcatParams::begin(${jarMajorVersion})")
    def jcatParams = []

    def deployCfgFile = ""
    if ((cfg.testsuite == "install" || cfg.testsuite == "upgrade") && cfg.containsKey("deployment_data")) {
        deployCfgFile = "${WORKSPACE}/${cfg.project_dir}/patchedDeploymentConfig.yaml"
        writeFile(file:deployCfgFile, text:cfg.deployment_data)
    }

    if (cfg.DEBUG == true)
        jcatParams.add("-Djcat.logging=debug")
    jcatParams.add("-Djcat.enableProtocolMessagesInConsole=false")

    if (jarMajorVersion >= 3) {

        jcatParams.add("-DjenkinsExecutionEnabled=true")
        jcatParams.add("-DbinariesPath=${WORKSPACE}/bin")
        jcatParams.add("-Dlogdir=${WORKSPACE}/JCAT_LOGS")
        jcatParams.add("-DprePostEnabled=${cfg.PREPOST?:'false'}")
        jcatParams.add("-DautoOpenLogsEnabled=false")
        jcatParams.add("-DmtlsEnabled=${cfg.ENABLE_MTLS}")
        jcatParams.add("-DsmallSystemEnabled=${cfg.SMALL_DEPLOYMENT}")
        jcatParams.add("-DuninstallTools=${cfg.UNINSTALL_TOOLS}")
        jcatParams.add("-DsimuTrafficEnabled=${cfg.SIMU_TRAFFIC_ENABLED}")
        jcatParams.add("-DsmsOverNasEnabled=${cfg.SMS_OVER_NAS}")
        jcatParams.add("-DisAnsi=${cfg.IS_ANSI}")
        jcatParams.add("-DumtsEnabled=${cfg.UMTS_ENABLED}")
        jcatParams.add("-DeirMapEnabled=${cfg.EIR_MAP_ENABLED}")
        jcatParams.add("-DudmAusfEirEnabled=${cfg.UDM_AUSF_EIR}")
        jcatParams.add("-DepcEnabled=${cfg.EPC}")
        jcatParams.add("-DimsEnabled=${cfg.IMS}")
        jcatParams.add("-DnrfEnabled=${cfg.NRF}")
        jcatParams.add("-DuseNewTools=true")

        String vnfm_credentials = "vnfm-creds-36783-5g2"
        withCredentials([usernamePassword(
            credentialsId: vnfm_credentials,
            usernameVariable: 'evnfm_user',
            passwordVariable: 'evnfm_pwd')
        ]) {
            jcatParams.add("-DevnfmServerUser=${evnfm_user}")
            jcatParams.add("-DevnfmServerPassword=${evnfm_pwd}")
        }
        if (cfg.containsKey("EVNFM_SERVER_NAME"))
            jcatParams.add("-DevnfmServer=${cfg.EVNFM_SERVER_NAME}")
        if (cfg.containsKey("CSAR_URL"))
            jcatParams.add("-DcsarUrl=${cfg.CSAR_URL}")
        if (cfg.containsKey("CSAR_PATH"))
            jcatParams.add("-DcsarPath=${cfg.CSAR_PATH}")
        if (cfg.containsKey("CSAR_VERSION"))
            jcatParams.add("-DcsarVersion=${cfg.CSAR_VERSION}")
        if (cfg.containsKey("WF_CCSM_VERSION"))
            jcatParams.add("-DccsmVersion=${cfg.WF_CCSM_VERSION}")
        if (cfg.containsKey("CSAR_UPGRARDE_PATH"))
            jcatParams.add("-DcsarUpgradePath=${cfg.CSAR_UPGRARDE_PATH}")
        if (cfg.containsKey("CSAR_UPGRADE_VERSION"))
            jcatParams.add("-DcsarUpgradeVersion=${cfg.CSAR_UPGRADE_VERSION}")
        if (cfg.containsKey("WF_CCSM_UPGRADE_VERSION"))
            jcatParams.add("-DccsmUpgradeVersion=${cfg.WF_CCSM_UPGRADE_VERSION}")

        if (cfg.containsKey("DURATION")) {
            jcatParams.add("-DdtgTraffic.durationHours=${cfg.DURATION}")
            jcatParams.add("-DtitansimTraffic.durationHours=${cfg.DURATION}")
        }else {
            if (cfg.containsKey("TRAFFICMIX_DURATION"))
                jcatParams.add("-DdtgTraffic.durationHours=${cfg.TRAFFICMIX_DURATION}")
            else if (cfg.containsKey("STABILITY_DURATION"))
                jcatParams.add("-DdtgTraffic.durationHours=${cfg.STABILITY_DURATION}")
            else if (cfg.containsKey("STABILITY_LONG_DURATION"))
                jcatParams.add("-DdtgTraffic.durationHours=${cfg.STABILITY_LONG_DURATION}")
        }
        if (cfg.containsKey("PASS_RATE"))
            jcatParams.add("-DdtgTraffic.passRateThreshold=${cfg.PASS_RATE}")
        else {
           if(cfg.containsKey("TRAFFICMIX_PASS_RATE"))
                jcatParams.add("-DdtgTraffic.passRateThreshold=${cfg.TRAFFICMIX_PASS_RATE}")
            else if(cfg.containsKey("STABILITY_PASS_RATE"))
                jcatParams.add("-DdtgTraffic.passRateThreshold=${cfg.STABILITY_PASS_RATE}")
            else if(cfg.containsKey("MAINTAINABILITY_PASS_RATE"))
                jcatParams.add("-DdtgTraffic.passRateThreshold=${cfg.MAINTAINABILITY_PASS_RATE}")
            else if(cfg.containsKey("ROBUSTNESS_PASS_RATE"))
                jcatParams.add("-DdtgTraffic.passRateThreshold=${cfg.ROBUSTNESS_PASS_RATE}")
            else if(cfg.containsKey("LICENSES_PASS_RATE"))
                jcatParams.add("-DdtgTraffic.passRateThreshold=${cfg.LICENSES_PASS_RATE}")
        }
        if (cfg.containsKey("ERROR_RATE"))
            jcatParams.add("-DtitansimTraffic.errorRateThreshold=${cfg.ERROR_RATE}")
        if (cfg.containsKey("ENGINEERING_CAP"))
            jcatParams.add("-DdtgTraffic.engineeringCapacity=${cfg.ENGINEERING_CAP}")
        else {
            if (cfg.containsKey("TRAFFICMIX_ENGINEERING_CAP"))
                jcatParams.add("-DdtgTraffic.engineeringCapacity=${cfg.TRAFFICMIX_ENGINEERING_CAP}")
            else if (cfg.containsKey("STABILITY_ENGINEERING_CAP"))
                jcatParams.add("-DdtgTraffic.engineeringCapacity=${cfg.STABILITY_ENGINEERING_CAP}")
            else if (cfg.containsKey("MAINTAINABILITY_ENGINEERING_CAP"))
                jcatParams.add("-DdtgTraffic.engineeringCapacity=${cfg.MAINTAINABILITY_ENGINEERING_CAP}")
            else if (cfg.containsKey("ROBUSTNESS_ENGINEERING_CAP"))
                jcatParams.add("-DdtgTraffic.engineeringCapacity=${cfg.ROBUSTNESS_ENGINEERING_CAP}")
        }
        if (cfg.containsKey("STABILITY_ENGINEERING_CAP_ASYNC"))
            jcatParams.add("-DtitansimTraffic.engineeringCapacityAsync=${cfg.STABILITY_ENGINEERING_CAP_ASYNC}")
        if (cfg.containsKey("STABILITY_ENGINEERING_CAP_SYNC"))
            jcatParams.add("-DtitansimTraffic.engineeringCapacitySync=${cfg.STABILITY_ENGINEERING_CAP_SYNC}")
        if (cfg.containsKey("STABILITY_ENGINEERING_INITIAL_CPS_ASYNC"))
            jcatParams.add("-DtitansimTraffic.initialCapacityAsync=${cfg.STABILITY_ENGINEERING_INITIAL_CPS_ASYNC}")
        if (cfg.containsKey("STABILITY_ENGINEERING_INITIAL_CPS_SYNC"))
            jcatParams.add("-DtitansimTraffic.initialCapacitySync=${cfg.STABILITY_ENGINEERING_INITIAL_CPS_SYNC}")
        if (cfg.containsKey("ENVIRONMENT_NAME"))
            jcatParams.add("-DenvironmentName=${cfg.ENVIRONMENT_NAME}")
        if (deployCfgFile != "")
            jcatParams.add("-DinstallationConfigFile=${deployCfgFile}")
        jcatParams.add("-Dtest.suites=${cfg.ts_xml}")
        if (cfg.containsKey("TRAFFIC_TYPE"))
            jcatParams.add("-DtitansimTraffic.trafficType=${cfg.TRAFFIC_TYPE}")
        if (cfg.containsKey("USE_DTG_TRAFFIC"))
            jcatParams.add("-DhssDtgTrafficEnabled=${cfg.USE_DTG_TRAFFIC}")
        if (cfg.containsKey("DTG_REMOTE_ENABLE"))
            jcatParams.add("-Ddtg.remoteServerEnabled=${cfg.DTG_REMOTE_ENABLE}")
        if (cfg.containsKey("GTLA_BACKUP_NAME")) {
            if (cfg.GTLA_BACKUP_NAME != "") {
               jcatParams.add("-DgtlaBackupName=${cfg.GTLA_BACKUP_NAME}")
            }
        }
        if (cfg.containsKey("PREPOST_SCRIPT_PATH"))
            jcatParams.add("-DprePostScriptFolderPath=${cfg.PREPOST_SCRIPT_PATH}")

        if (cfg.containsKey("KPI_THRESHOLD")) {
            if (cfg.KPI_THRESHOLD != "") {
               jcatParams.add("-DpassRateThreshold=${cfg.KPI_THRESHOLD}")
            }
        }
        if (cfg.containsKey("KPI_RATE")) {
           if (cfg.KPI_RATE != "") {
              jcatParams.add("-DdtgTraffic.engineeringCapacity=${cfg.KPI_RATE}")
           }
        }
        if (cfg.containsKey("KPI_EPC_RATE")) {
           if (cfg.KPI_EPC_RATE != "") {
              jcatParams.add("-Deckpidtgepc=${cfg.KPI_EPC_RATE}")
           }
        }
        if (cfg.containsKey("KPI_STORAGE_PATH")) {
            if (cfg.KPI_STORAGE_PATH != "") {
               jcatParams.add("-DkpiStoragePath=${cfg.KPI_STORAGE_PATH}")
            }
        }
        if (cfg.containsKey("DTG_VERSION")) {
            if (cfg.DTG_VERSION != "") {
               jcatParams.add("-Ddtg.version=${cfg.DTG_VERSION}")
            }
        }

        // Parameters for the Workflows TCs
        if (cfg.containsKey("EVNFM_SERVER_NAME"))
            jcatParams.add("-DevnfmServer=${cfg.EVNFM_SERVER_NAME}")
        if (cfg.containsKey("EVNFM_SERVER_USER"))
            jcatParams.add("-DevnfmServerUser=${cfg.EVNFM_SERVER_USER}")
        if (cfg.containsKey("EVNFM_SERVER_PWD"))
            jcatParams.add("-DevnfmServerPassword=${cfg.EVNFM_SERVER_PWD}")
        if (cfg.containsKey("CSAR_URL"))
            jcatParams.add("-DcsarUrl=${cfg.CSAR_URL}")
        if (cfg.containsKey("CSAR_PATH"))
            jcatParams.add("-DcsarPath=${cfg.CSAR_PATH}")
        if (cfg.containsKey("CSAR_VERSION"))
            jcatParams.add("-DcsarVersion=${cfg.CSAR_VERSION}")
        if (cfg.containsKey("WF_CCSM_VERSION"))
            jcatParams.add("-DccsmVersion=${cfg.WF_CCSM_VERSION}")
        if (cfg.containsKey("CSAR_UPGRADE_PATH"))
            jcatParams.add("-DcsarUpgradePath=${cfg.CSAR_UPGRADE_PATH}")
        if (cfg.containsKey("CSAR_UPGRADE_VERSION"))
            jcatParams.add("-DcsarUpgradeVersion=${cfg.CSAR_UPGRADE_VERSION}")
        if (cfg.containsKey("CCSM_UPGRADE_VERSION"))
            jcatParams.add("-DccsmUpgradeVersion=${cfg.CCSM_UPGRADE_VERSION}")

    } else {  // backward compatibility: Both v1 and v2
        jcatParams.add("-Djenkins.execution=true")
        jcatParams.add("-Dbinaries.path=${WORKSPACE}/bin")
        if (cfg.containsKey("TRAFFIC_CASES"))
            jcatParams.add("-Dtraffic.cases=" + "\"" + "${cfg.TRAFFIC_CASES}" + "\"")
        if (cfg.containsKey("TRAFFIC_MULTI_CASES"))
            jcatParams.add("-Dtraffic.multi.cases=" + "\"" + "${cfg.TRAFFIC_MULTI_CASES}" + "\"")
        if (cfg.containsKey("TRAFFIC_MULTI"))
            jcatParams.add("-Dtraffic.multi=${cfg.TRAFFIC_MULTI}")
        if (cfg.containsKey("IPV6"))
            jcatParams.add("-Dis.ipV6=${cfg.IPV6?:'false'}")
        if (cfg.containsKey("UDM_AUSF_EIR"))
            jcatParams.add("-Dis.udm.ausf.eir=${cfg.UDM_AUSF_EIR}")
        if (cfg.containsKey("EPC"))
            jcatParams.add("-Dis.epc=${cfg.EPC}")
        if (cfg.containsKey("NRF"))
            jcatParams.add("-Dis.nrf=${cfg.NRF}")
        if (deployCfgFile != "")
            jcatParams.add("-Dinstallation.config.file=${deployCfgFile}")
        if (cfg.containsKey("ENVIRONMENT_NAME"))
            jcatParams.add("-Denvironment.name=${cfg.ENVIRONMENT_NAME}")

        if (jarMajorVersion == 1) {  // v1
            jcatParams.add("-Dlogdir=${WORKSPACE}/JCAT_LOGS")
            jcatParams.add("-Dpre.post=false")
            jcatParams.add("-Denable.mtls=${cfg.ENABLE_MTLS}")
            jcatParams.add("-Dauto.open.logs=true")
            if (cfg.containsKey("DIRECTOR_OAM_VIP"))
                jcatParams.add("-Ddirector.address=${cfg.DIRECTOR_OAM_VIP}")
            if (cfg.containsKey("WORKER_OAM_VIP"))
                jcatParams.add("-Doam.vip=${cfg.WORKER_OAM_VIP}")
            if (cfg.containsKey("SIG_VIP"))
                jcatParams.add("-Dsig.vip=${cfg.SIG_VIP}")
            if (cfg.containsKey("KUBE_CONF"))
                jcatParams.add("-Dkube.config.file=${cfg.KUBE_CONF}")
            if (cfg.containsKey("IBD")) {
                jcatParams.add("-Dis.ibd=${cfg.IBD}")
                if (cfg.IBD == true) {
                    if (cfg.containsKey("tunnel_dest_ip"))
                        jcatParams.add("-Dtunnel.dest.ip=${cfg.tunnel_dest_ip}")
                    if (cfg.containsKey("rsa_key"))
                        jcatParams.add("-Ddirector.rsa.key=${cfg.rsa_key}")
                }
            }
            if (cfg.containsKey("DEPLOY_NAMESPACE"))
                jcatParams.add("-Dkube.namespace=${cfg.DEPLOY_NAMESPACE}")
            if (cfg.containsKey("SAMPLE_RATE"))
                jcatParams.add("-Dsampling.period.minutes=${cfg.SAMPLE_RATE}")
            if (cfg.containsKey("PASS_RATE"))
                jcatParams.add("-Dpass.rate.threshold=${cfg.PASS_RATE}")
            if (cfg.containsKey("ENGINEERING_CAP"))
                jcatParams.add("-Dengineering.capacity=${cfg.ENGINEERING_CAP}")
            if (cfg.containsKey("DURATION"))
                jcatParams.add("-Dduration.hours=${cfg.DURATION}")
        } else if (jarMajorVersion == 2) {  // v2
            cfg.ENVIRONMENT_NAME = cfg.CLOUD
        } else {
            error "Unrecognized JAR Major version: '${jarMajorVersion}'"
        }
    }

    common.log('debug', "getJcatParams::end(${jcatParams})")
    return jcatParams.join(' ')
}


/**
 * Get all needed parameters for NFT JAR client
 */
def getJarParams(cfg, jarMajorVersion) {
    common.log('debug', "getJarParams::begin(${jarMajorVersion})")
    def jarParams = []

    jarParams.add("--log-dir=${WORKSPACE}/JCAT_LOGS")
    jarParams.add("--auto-open-logs=false")
    jarParams.add("--pre-post=false")
    jarParams.add("--enable-mtls=${cfg.ENABLE_MTLS}")
    if (cfg.containsKey("DURATION") && cfg.DURATION != null)
        jarParams.add("--duration-hours=${cfg.DURATION}")
    if (cfg.containsKey("PASS_RATE"))
        jarParams.add("--pass-rate-threshold=${cfg.PASS_RATE}")
    if (cfg.containsKey("DEPLOY_NAMESPACE"))
        jarParams.add("--namespace=${cfg.DEPLOY_NAMESPACE}")
    if (cfg.containsKey("ENGINEERING_CAP"))
        jarParams.add("--engineering-capacity=${cfg.ENGINEERING_CAP}")
    if (jarMajorVersion == 2) {
        if (cfg.containsKey("SAMPLE_RATE"))
            jarParams.add("--sampling-period-minutes=${cfg.SAMPLE_RATE}")
    } else if (jarMajorVersion >= 3) {
        if (cfg.containsKey("ENVIRONMENT_NAME"))
            jarParams.add("--environment=${cfg.ENVIRONMENT_NAME}")
        if (cfg.containsKey("SAMPLE_RATE"))
            jarParams.add("--sampling.period.minutes=${cfg.SAMPLE_RATE}")
    }

    common.log('debug', "getJarParams::end(${jarParams})")
    return jarParams.join(' ')
}


/**
 * Free some space in the workspace
 *  if little disk space clean up everything in the workspace
 *  otherwise Remove just old jar packages
 */
def cleanUpWorkspace() {
    common.log('debug', 'cleanUpWorkspace::begin')
    String space = sh(returnStdout:true, script:"df -l --output=avail /tmp").split('\n')[1]
    common.log('debug', "cleanUpWorkspace::space=${space}")
    if (space.toInteger() < 1000000) {  // ~1GB
        common.log('debug', "cleanUpWorkspace:: Less than 1GB")
        sh(returnStdout:true, script:"ls -1 ${WORKSPACE}/..").split('\n').each {
            if (!it.contains(JOB_NAME)) {
                sh(returnStdout:true, script:"rm -rf ${WORKSPACE}/../${it}")
            }
        }
    } else {
        common.log('debug', "cleanUpWorkspace:: More than 1GB")
        sh returnStatus:true, script:
            """rm -rf ci csar_ccsm_nft j2ee JCAT_LOGS jenkinsFiles nft_automation tools udm_nft_docs download* *.jar *.tgz 2>/dev/null
            """
    }
    common.log('debug', 'cleanUpWorkspace::end')
}


/**
 * Obtain the JAR package to execute depending on the trigger condition:
 *  - Patchset verify or non master branch: Compile it
 *  - Otherwise: Download it from ARM
 */
def getJarPackage(cfg) {
    common.log('debug', "getJarPackage::begin(${cfg})")
    timeout(time: (cfg.timeout?:'20').toInteger(), unit: 'MINUTES') {
        if (isVerifyMode() || cfg.NFT_BRANCH != 'master') {
            def settings_xml = common.getPipelineResource().SETTINGS_XML
            common.log('info', "Compiling JAR package")
            sh """
                . /etc/home/bashrc
                module add openjdk/${cfg.jdk_version}
                module add maven/${cfg.maven_version}
                module list
                cd ${cfg.project_dir}
                mvn --settings ${settings_xml} install -DskipTests
            """
        } else {
            common.log('info', "Downloading JAR package")
            withCredentials(
                [usernamePassword(
                    credentialsId: 'userpwd-adp',
                    usernameVariable: 'arm_user',
                    passwordVariable: 'arm_pwd')
                ])
            {
                def download_script = common.copyGlobalLibraryScript('tools/download_package_from_arm3')
                sh "${download_script} -o ${WORKSPACE} -u ${arm_user} ${arm_pwd} -v ${cfg.UDM_NFT_AUTO_VERSION}"
            }
        }
    }
    common.log('debug', "getJarPackage::end")
}


/**
 * Parse parent 'pom.xml' to get project version
 */
def getProjectVersion(pom) {
    common.log('debug', "getProjectVersion::begin(${pom})")
    def pom_data = readFile(pom)
    def parsed_xml = new XmlSlurper().parseText(pom_data)
    String prj_version = parsed_xml.version

    common.log('debug', "getProjectVersion::end(${prj_version})")
    return prj_version
}


/**
 * Set project version in input 'pom.xml'
 */
def setProjectVersion(pom, version) {
    common.log('debug', "setProjectVersion::begin(${pom}, ${version})")
    sh "sed -i 's#<version>.*<\\/version>#<version>${version}<\\/version>#g' ${pom}"
    common.log('debug', "setProjectVersion::end()")
}


/**
 * Return true if project major version is greater than 1
 */
def getJarMajorVersion(prj_version) {
    common.log('debug', "getJarMajorVersion::begin(${prj_version})")
    int result = 3
    if (prj_version != null) {
        result = prj_version.split(/\./)[0].toInteger()
    }
    common.log('debug', "getJarMajorVersion::end(${result})")
    return result
}


/**
 * Get test_suite parameter (XML file) from a finished build
 */
def getTsXml(def build) {
    common.log('debug', "getTsXml::begin(${build.project.name})")
    def ts = ""
    if (build?.logFile != null) {
        build.logFile.text.split('\n').each { line->
            line = ConsoleNote.removeNotes(line);  //strip off jenkins specific encoding
            Matcher matcher = line=~/.* -Dtest.suites=(.*\.xml) .*/
            if (matcher) {
               ts = common.getListFromSh("find . -name ${matcher[0][1]}")[0]
               common.log('debug', "getTsXml:: ts='${ts}'")
            }
        }
    }
    common.log('debug', "getTsXml:end(${ts})")
    return ts
}


/**
 * Parse total number of TCs to be executed in a Jenkins Job
 * Requirements:
 *  - CCSM NFT automation repository has to be already cloned in the current workspace
 *  - 'jobName' need to be a Child-job executing tests
 *  - 'tsXml' need to be a existing TS XML file
 */
int getTotalNumberOfTcs(def build) {
    int result = 0
    if (build) {
        def jobName = build.project.name
        common.log('debug', "getTotalNumberOfTcs::begin(${build.project.name})")
        if (!jobName.contains('clean') && !jobName.contains('install')) {
            String tsFile = getTsXml(build)
            if (tsFile?:"" != "") {
                result += sh(returnStdout:true, script:"grep -E '(<test name=|<suite-file path=)' ${tsFile} | grep -v '<\\!--' -c").toInteger()
            }
        }
        common.log('debug', "getTotalNumberOfTcs::end(${result})")
    }
    return result
}
