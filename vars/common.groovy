/**
 * This file contains helper common functions for the Jenkins NFT pipelines
 */
import org.codehaus.groovy.runtime.StackTraceUtils
import java.util.regex.Matcher
import java.util.regex.Pattern
import hudson.console.ConsoleNote


def getPipelineResource() {
    def result = readJSON text:libraryResource('com/ericsson/default-values-ccsm.json')
    log("debug", "getPipelineResource::result=${result}")
    return result
}


/**
 * Allow smooth transition between old "udm_nft_auto" directory and
 * the new "nft_automation" directory
 */
def getSOURCE_ROOT_DIR() {
    def result = getPipelineResource().SOURCE_ROOT_DIR
    log("debug", "getSOURCE_ROOT_DIR::result=${result}")
    return result
}


/**
 * Return dictionary with CCSM-NFT ARM repositories
 */
def getARM_CCSM_NFT_REPO() {
    def result = [
        'DEV': getPipelineResource().ARM_CCSM_NFT_DEV_REPO,
        'STAGING': getPipelineResource().ARM_CCSM_NFT_STAGING_REPO,
        'RELEASE': getPipelineResource().ARM_CCSM_NFT_RELEASE_REPO
    ]
    log("debug", "getARM_CCSM_NFT_REPO::result=${result}")
    return result
}


/**
 * Return CCSM-NFT ARM repositories
 */
def getARM_CCSM_NFT_LOGS_REPO() {
    def result = getPipelineResource().ARM_CCSM_NFT_LOGS_REPO
    log("debug", "getARM_CCSM_LOGS_REPO::result=${result}")
    return result
}


/**
 * Clone repository using this mirror location
 */
def getDEFAULT_GERRIT_SERVER() {
    "gerritmirror-ha.rnd.ki.sw.ericsson.se"
}


/**
 * Return a list of elements compossed by the lines in a shell command output
 */
def getListFromSh(cmd) {
	log('info', cmd)
    def list = new ArrayList()
    try {
        sh(returnStdout:true, script:cmd).trim().split(/[\n\r]/).each {
            if (it.size() > 1)
                list.add(it)
        }
    } catch(java.io.NotSerializableException exc) {
        log('warning', "${exc.toString()}")
    } catch(exc) {
        log('warning', "Command did not return any result")
    }

    return list
}


/**
 * Get Jenkins credential ID
 */
def getCredentialId(String unit, String type="gerrit") {
    log("debug", "getCredentialId::begin(${unit}, ${type})")
    String credent = ""
    switch(type) {
        case "usrpwd":  credent = "userpwd";     break
        case "userpwd": credent = "userpwd";     break
        case "sonar":   credent = "sonar-token"; break
        case "arm":     credent = "armpass";     break
        default:        credent = "userpwd";     break
    }
    String result = "${credent}-${unit}"
    log("debug", "getCredentialId::end(${result})")
    return result
}


/**
 * Reformat a Gerrit URL(ssh:29418) if necesary
 */
def formatGerritUrl(url_in) {
    log('debug', "formatGerritUrl::begin(${url_in})")
    def url_out = url_in

    def matcher = url_out =~ /(ssh:|https:)\/\/([a-zA-Z0-9]*@){0,1}([\w\.\-]+)(:29418|\/a){0,1}\/(.*)/
    if (matcher.matches()) {
        url_out = "https://${matcher.group(2)?:""}${matcher.group(3)}/a/${matcher.group(5)}"
    } else {
        url_out = "https://${DEFAULT_GERRIT_SERVER}/a/${url_in}"
    }

    log('debug', "formatGerritUrl::end(${url_out})")
    return url_out
}


/**
 * Parse a TestNG XML file to extract information about
 * TSs, TCs and results
 */
def parseTestng(tng_file) {
    log('debug', "parseTestng::begin(${tng_file})")
    def data      = readFile(tng_file)
    def parsedXml = new XmlParser().parseText(data)
    def suites    = parsedXml.suite
    List<String> results = new ArrayList<String>()

    suites.each { ts->
        log('debug', "parseTestng::TS=${ts.'@name'}")
        ts.test.each { tc->
            log('debug', "parseTestng::  TC=${tc.'@name'}")
            tc.class.each { cl->
                log('debug', "parseTestng::  class=${cl.'@name'}")
                cl.'test-method'.each { mt->
                    log('debug', "  parseTestng::  test-method=${mt.'@name'}")
                    def info = ""
                    if (mt.'@status' == 'PASS') {
                        info = mt.'reporter-output'.text().trim()
                    } else {
                        info = mt.exception.'message'.text().trim()
                    }
                    results.add([
                        'testSuiteName'   : ts.'@name',
                        'testCaseName'    : tc.'@name'.split(': ')[0],
                        'methodName'      : mt.'@description'.split(': ')[-1],
                        'verdict'         : mt.'@status',
                        'duration'        : mt.'@duration-ms',
                        'info'            : info
                    ])
                }
            }
        }
    }
    log('debug', "parseTestng::end(${results})")
    return results
}


/**
 * Parse a JUnit XML file to extract information about
 * TSs, TCs and results
 */
def parseJunit(junit_file) {
    def data      = readFile(junit_file)
    def parsedXml = new XmlParser().parseText(data)
    def tsuites   = parsedXml.testsuites
    List<String> results = new ArrayList<String>()

    tsuites.each { testsuite->
        testsuite.test.each { testcase->
            def info = ""
            if (testcase.'@status' == 'failed') {
                info = testcase.failure.text().trim()
            }
            results.add([
                'testSuiteName'   : testcase.'@classname'.split('.')[-1],
                'testCaseName'    : 'testcase',
                'methodName'      : testcase.'@name',
                'verdict'         : testcase.'@status',
                'info'            : info
            ])
        }
    }

    return results
}


/**
 * Generate 'results.properties' file
 */
def generateResultProperties(cfg, summ) {
    log('debug', "generateResultsProperties::begin")
    def data = ""

    // results.properties
    summ.each { tc->
        if (tc.containsKey('testSuiteName') && tc.containsKey('testCaseName') && tc.containsKey('methodName')) {
            data += "${tc.testSuiteName}.${tc.testCaseName}.${tc.methodName}.verdict=${tc.verdict}\n"
            data += "${tc.testSuiteName}.${tc.testCaseName}.${tc.methodName}.info=${tc.info}\n"
            if (tc.verdict.contains("FAIL") || tc.verdict.contains("SKIP")) {
                unstable("There are FAILED/SKIPPED TC results")
            }
        } else {
            (tc.keySet() as List)[1..-1].each { prop->
                data += "${tc[tc.keySet()[0]]}.${prop}" + "=" + tc."${prop}" + "\n"
            }
        }
    }
    log('info', "generateResultsProperties::data=${data}")

    log('debug', "generateResultsProperties::end")
    return data
}


/**
 * Print log message with some stacktrace info
 */
def log(mode, msg) {
    def     logmode = mode.toUpperCase()
    def     prefix  = "[${logmode}] "
    Boolean debug = (env.DEBUG?:'').toBoolean()

    if (debug || logmode.equals('ERROR')) {
        def marker  = new Throwable()
        def funct   = StackTraceUtils.sanitize(marker).stackTrace[1].toString()
        def matcher = funct =~ /.*\(([a-zA-Z0-9_\-\/:\.]+)\).*/
        if (matcher.matches()) {
            def source_file = matcher.group(1)
            if (source_file.contains('/')) {
                prefix += source_file.tokenize('/')[-1] + ": "
            } else if (source_file.contains('.')) {
                prefix += source_file.tokenize('.')[-1] + ": "
            } else {
                prefix += source_file + ": "
            }
        }
    }

    if ((logmode.equals('DEBUG') && debug) || !logmode.equals('DEBUG')) {
        echo "${prefix} ${msg}"
    }
}


/**
 * Print error message with additional info and abort the build
 */
def msgError(msg) {
    def where   = ""
    def marker  = new Throwable()
    def funct   = StackTraceUtils.sanitize(marker).stackTrace[1].toString()
    def matcher = funct =~ /.*\(([a-zA-Z0-9_\-\/:\.]+)\).*/
    if (matcher.matches()) {
        def source_file = matcher.group(1)
        if (source_file.contains('/')) {
            where = source_file.split(/\//)[-1]
        } else if (source_file.contains('.')) {
            where = source_file.split(/\./)[-1]
        } else {
            where = source_file
        }
    }

    error "(${where}) ${msg}"
}


/**
 * Generates a path to a temporary file location, ending with {@code path} parameter.
 *
 * @param path path suffix
 * @return path to file inside a temp directory
 */
@NonCPS
String createTempLocation(String path) {
    String tmpDir = pwd tmp: true
    return tmpDir + File.separator + new File(path).getName()
}


/**
 * Returns the path to a temp location of a script from the global library (resources/ subdirectory)
 *
 * @param srcPath path within the resources/ subdirectory of this repo
 * @param destPath destination path (optional)
 * @return path to local file
 */
String copyGlobalLibraryScript(String srcPath, String destPath = null) {
    destPath = destPath ?: WORKSPACE + File.separator + new File(srcPath).getName()
    writeFile file: destPath, text: libraryResource(srcPath)
    log('debug', "copyGlobalLibraryScript: copied ${srcPath} to ${destPath}")
    sh "chmod u+rx ${destPath}"
    log('debug', "copyGlobalLibraryScript: rx permissions set")
    return destPath
}


/**
 * Create a private file
 */
def writePrivateFile(filename, content) {
    if (fileExists(filename)) {
        sh "chmod 660 ${filename}"  // -rw-rw----
    }
    log('info', "Create file: ${filename}")
    writeFile(file:filename, text:content)
    sh "chmod 400 ${filename}"  // -r--------
}


/**
 * Set build description for current Jenkins build
 */
def setBuildDescription(cfg) {
    log('debug', "setBuildDescription::begin")
    def description = cfg.UDM_NFT_AUTO_VERSION

    if (cfg.commitId?:"" != "") {
        description += "(${cfg.commitId})"
    }

    if ((cfg.CCSM_PACKAGE != null) && (cfg.SMESH_PACKAGE != null)) {
        description += "<br>ccsm: ${cfg.CCSM_VERSION}" +
                       "  sm: ${cfg.SMESH_VERSION}"
        log('debug', "CCSM: (${cfg.CCSM_VERSION}), SM: (${cfg.SMESH_VERSION})")
        if (cfg.SMALL_DEPLOYMENT != null && cfg.SMALL_DEPLOYMENT == true) {
            description += " -> SMALL deployment"
        }

        if (cfg.USE_DTG_TRAFFIC != null && cfg.USE_DTG_TRAFFIC == true) {
            description += "<br> Run traffic with DTG"
        } else if (cfg.USE_DTG_TRAFFIC != null && cfg.USE_DTG_TRAFFIC == false) {
            description += "<br> Run traffic with TitanSim"
        }
    } else if (cfg.testsuite != 'cleanup') {
        def ccsm_ver = ""
        def smesh_ver = ""
        (ccsm_ver, smesh_ver) = helmInfo.getCcsmVersions(cfg.KUBE_CONF)
        cfg.CCSM_VERSION = ccsm_ver
        cfg.SMESH_VERSION = smesh_ver
        if (cfg.CCSM_VERSION != null && cfg.SMESH_VERSION != null) {
            description += "<br>${cfg.CCSM_VERSION}<br>${cfg.SMESH_VERSION}"
        }
    }
    description += "<br>${cfg.CLOUD} (${cfg.ECCD_VERSION})"

    currentBuild.description = description
    log('debug', "setBuildDescription::end")
}


/**
 * Get the Environment(eccd) name from TG server name
 */
def getEnvName(build_node) {
    log('debug', "getEnvName::begin(${build_node})")
    def result = ""

    // First try Rosetta
    try {
        def envInfo = sh returnStdout:true, script:"""
            . /etc/home/bashrc
            export ST_TOOL_PATH=/opt/hss/system_test
            export BAT_CFG_PATH=/opt/hss/system_test/share/BAT/
            get_env_info
        """
        String regex = /.*Name:[\n\r\t ]+([A-Za-z0-9\-_]+)[\t\n\r ]+Active.*/
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL)
        Matcher matcher = pattern.matcher(envInfo.replace('\n', ' '))
        if (matcher.matches()) {
            result = matcher.group(1)
        }
    } catch (exc) {
        log('warn', "'get_env_info' tool failed")
    }

    if (result == "") {
        // get Jenkins node description
        for (node in hudson.model.Hudson.instance.slaves) {
            if (node.getDisplayName().equals(build_node)) {
                result = node.getNodeDescription()
                break
            }
        }
    }

    log('debug', "getEnvName::end(${result})")
    return result
}


/**
 * Return parent directory
 */
def dirname(name) {
  return name.split(/\//)[0..-2].join('/')+'/'
}


/**
 * Get triggered child job list by parsing a finished build console log
 */
def getChildJobList(def build) {
    log('debug', "getChildJobList(${build.project.name})")
    def list = []
    if (build?.logFile != null) {
        build.logFile.text.split('\n').each { line->
            line = ConsoleNote.removeNotes(line);  //strip off jenkins specific encoding
            Matcher matcher = line=~/.*Starting building: (.*) #(\d+)/
            if (matcher) {
               def foundJob = matcher[0][1]
               def foundBuildNum = Integer.parseInt(matcher[0][2])
               def tmp = Jenkins.instance.getItem(foundJob).getBuildByNumber(foundBuildNum)
               if (tmp) {
                   list.add(tmp)
               }
            }
        }
    }
    log('debug', "getChildJobList(${list})")
    return list
}


/**
 * Get triggered child job list by parsing current build console log
 * NOTE1: CurrentBuild is assumed to be still running.
 * NOTE2: Method used to parse still running build logs "currentBuild.rawBuild.getLog()"
 *        presents an important restriction: it is a non-serializable method so
 *        following serializable methods could fail.
 */
@NonCPS
def getCurrentChildJobList() {
    log('debug', "getCurrentChildJobList::begin()")
    def list = []
    currentBuild.rawBuild.getLog().readLines().each { line->
        line = ConsoleNote.removeNotes(line);  //strip off jenkins specific encoding
        Matcher matcher = line=~/.*Starting building: (.*) #(\d+)/
        if (matcher) {
            list.add([matcher[0][1],matcher[0][2]])
        }
    }
    log('debug', "getCurrentChildJobList::end(${list})")
    return list
}
