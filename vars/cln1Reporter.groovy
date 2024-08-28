import hudson.console.ConsoleNote
import java.util.regex.Matcher
import java.util.regex.Pattern
import com.ericsson.Cl2Report


// CONSTANTS DEFINITION
//----------------------

// Filter for selecting what jobs to parse
def getJOB_FILTER() {
    return 'CCSM_CLn1_*_Upgrade_Main'
}

// Possible values to apply to previous filter
// Report structure: key=Sections, values=SubSections
def getPARENT_JOBS() {
    return [
        'UDM_AUSF_EIR':   ['UDM_AUSF_EIR'],
        'IMS+EPC':        ['IMS_EPC']
    ]
}

// Hide these suffixes when showing subjobs sections
def getHIDE_SUFFIXES() {
    return ['_Upgrade_Main']
}

// Hide these preffixes when showing subjobs sections
def getHIDE_PREFFIXES() {
    return ['CCSM_CLn1_', 'CCSM_IBD_']
}

// Parse console logs
// - TC starting point samples:
// Install
//    [INFO] TC-CCSM-MNT-0101  TC-CCSM-MNT-0101: Initial deployment(no traffic)
// TrafficMix
//    [INFO] TC-UDM-STAB-0100  TC-UDM-STAB-0100: Traffic mix
// Robustness
//    [INFO] TC-CCSM-ROB-0403  TC-CCSM-ROB-0403: Worker node kubelet fault
// Licenses
//    [INFO] TC-EIR-LIC-0102  TC-EIR-LIC-0102: Check EIR License Capacity reports
// Maintainability
//    [INFO] TC-CCSM-MNT-0300  TC-CCSM-MNT-0300: Manual Increase/decrease the replicas of one POD
// Overload
//    [INFO] TC-UDM-ROB-0212  Overload test for eric-udm-ueauth without replica scaling
// Stability
//    [INFO] STABILITY  Generic Stability
// Stability EPC
//    [INFO] TC-HSS-EPC-STAB-0100.ASYNC.EPC_1 PID: 31249
//
def getPARSE_LOG_REGEXPS() {
    return [
        'TC_NAME': [
            /\[INFO\] (TC[\-_][A-Z0-9\-_]+)  TC[A-Z0-9\-_]+: (.*)/,   // Install, TrafficMix, Robustness, License, Maintainability
            /\[INFO\] (TC[\-_][A-Z0-9\-_]+)  (Overload .*)/,          // Overload
            /\[INFO\] (STABILITY|TC-UDM-STAB-generic|TC_CCSM_STAB_generic|TC_HSS-EPC_STAB_generic|TC_HSS-IMS_STAB_generic)  (.*)/,  // Stability
            /\[INFO\] (TC-HSS-EPC-STAB-[A-Z0-9\-\._]+) PID: [0-9]+/   // Stability EPC
        ],
        'TC_RESULT': /Total tests run: ([0-9]+), Passes: ([0-9]+), Failures: ([0-9]+), Skips: ([0-9]+)/,
        'LOG_MESSAGE': [
            /\[(ERROR|WARN|Assert|Fail)\] (.*)/,
	    /\[(INFO)\] --> (TC execution duration time .*)/
        ]
    ]
}

// Ignore these log messages (regular expressions) when parsing Jenkins build console-logs
def getIGNORE_LOGS() {
    return [
        /Remote system password is null or empty\. SFTP session will not be created\!/,
        /SFTP Session to disconnect is null\. Skipping disconnect procedure/,
        /Log file not generated/,
        /'get_env_info' tool failed/,
        /Error killing [0-9]+ process as regular user\. Reason:/,
        /Test Case Skipped/,
        /Found pod that is not ready: udrsim\-/
    ]
}


// CLASS FUNCTIONALITY
//---------------------

def call(body) {
    def dateto = new Date()
    def datefrom = dateto - 1
    if ('DATETIME_FROM' in params) {
        datefrom = new Date().parse("yyyyMMddHHmmss", params['DATETIME_FROM'])
    }
    if ('DATETIME_TO' in params) {
        dateto   = new Date().parse("yyyyMMddHHmmss", params['DATETIME_TO'])
    }
    def reportBaseName = "cln1Report_${dateto.format('yyyyMMddHHmmss').toString()}"
    Cl2Report clReport = new Cl2Report(reportBaseName, HIDE_SUFFIXES+HIDE_PREFFIXES)

    // Execute body
    def config_in = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    node("CCSM_NFT") {
        def report = "${reportBaseName}.yaml"
        if (params['COLLECT_REPORT_DATA'] == true) {
            stage("Generating report") {
                PARENT_JOBS.each { section->
                    clReport.setSection(section.key)
                    section.value.each { jobType->
                        def job = Jenkins.instance.getItem("${JOB_FILTER.replace('*', jobType)}")
                        if (job != null) {   // Job exists
                            clReport.setSubSection(jobType)
                            if (job.lastBuild != null) {  // Job has builds
                                def build_num = 1
                                while (build_num <= job.lastBuild.number) {
                                    def build = job.getBuildByNumber(build_num++)
                                    if (build && (params['INCLUDE_STILL_RUNNING'] || !build.building)
                                              && !build.displayName.toLowerCase().contains('ignore')
                                              && build.time >= datefrom && build.time <= dateto) {
                                         parseBuild(build, clReport, build.building)
                                    }
                                }
                            }
                        }
                    }
                }

                if (fileExists(report)) {
                    sh "rm -f ${report}"
                }
                writeYaml file:report, data:clReport.getReportData()
            }  // stage
        }

        if (params['CREATE_HTML_REPORT'] == true) {
            stage("Publish report") {
                if (params['INPUT_YAML_REPORT']?:'' != '') {
                    def inputData = readYaml text:params['INPUT_YAML_REPORT']
                    clReport.setReportData(inputData)
                }
                report = "${reportBaseName}.html"
                writeFile file:report, text:clReport.createHtmlReport()
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: false,
                    reportDir: WORKSPACE,
                    reportFiles: report,
                    reportName: reportBaseName,
                    reportTitles: ''])
            }  // stage
        }

        if (params['SEND_REPORT_BY_EMAIL'] == true) {
            stage("Send email") {
                def tittleText = "CCSM NFT CLn1 report (${datefrom.toString()} -> ${dateto.toString()})"
                def emailFrom = "jenkins-no-reply@ericsson.com"
                writeFile file:"report.html", text:readFile(report)

                emailext mimeType:'text/html',
                         body: '${FILE,path="report.html"}',
                         from: emailFrom,
                         subject: tittleText,
                         to: params['EMAIL_TO']
            }
        }
    }  // node
}  // call


// JENKINS BUILDS AND LOG PARSING FUNCTIONS
//------------------------------------------

// Parse a parent build console log
void parseBuild(obuild, clReport, building) {
    common.log('debug', "parseBuild(${obuild.project.name})")
    clReport.addParentBuild(obuild, building)
    getTriggeredBuildsByBuild(obuild).each { childBuild->
        if (childBuild) {
            def showName = childBuild.project.name.toLowerCase()
            if (!showName.contains("results") && !showName.contains('cleanup')) {
                common.log('debug', "parseBuild:: addChildBuild:${childBuild.project.name}/${childBuild.number}")
                clReport.addChildBuild(childBuild)
                (HIDE_SUFFIXES+HIDE_PREFFIXES).each {
                    showName = showName.replace(it, '')
                }
                parseConsoleOutput(childBuild, clReport)
            }
        }
    }
}

// Parse a child build console log
void parseConsoleOutput(childBuild, clReport) {
    common.log('debug', "parseConsoleOutput(${childBuild.project.name})")
    Pattern patternRE = Pattern.compile(PARSE_LOG_REGEXPS['TC_RESULT'], Pattern.DOTALL)
    boolean tcLine = false
    boolean insideTc = false

    childBuild.logFile.text.split('\n').each { line->
        line = ConsoleNote.removeNotes(line);
        for (regexp in PARSE_LOG_REGEXPS['TC_NAME']) {
            def matcherTC = line =~ regexp
            if (matcherTC) {
                String tcName = matcherTC.group(1)
                String descrp = ''
                if (matcherTC.groupCount() > 1) {
                    descrp = matcherTC.group(2)
                }
                common.log('debug', "parseConsoleOutput:: addTc(${tcName}, ${descrp})")
                clReport.addTc(tcName, descrp)
                tcLine = true
                insideTc = true
                break
            }
            tcLine = false
        }
        if (!tcLine && insideTc) {
            Matcher matcherRE = patternRE.matcher(line)
            if (matcherRE) {
                common.log('debug', "parseConsoleOutput:: setTcResult(${matcherRE[0][1]}, ${matcherRE[0][2]}, ${matcherRE[0][3]}, ${matcherRE[0][4]})")
                if (matcherRE[0][1].toInteger() == 1) {
                    clReport.setTcResult(matcherRE[0][2], matcherRE[0][3], matcherRE[0][4])
                } else {
                    clReport.setChildBuildResult(matcherRE[0][1], matcherRE[0][3], matcherRE[0][4])
                }
                insideTc = false
            } else {
                for (regexp in PARSE_LOG_REGEXPS['LOG_MESSAGE']) {
                    def matcherLG = line =~ regexp
                    if (matcherLG) {
                        String msgType = matcherLG.group(1)
                        String msgText = matcherLG.group(2)
                        boolean skipLog = false
                        for (noexp in IGNORE_LOGS) {
                            if (msgText=~noexp) {
                                skipLog = true
                                break
                            }
                        }
                        if (!skipLog) {
                            common.log('debug', "parseConsoleOutput:: Add msg(${msgType}, ${msgText})")
                            clReport.addTcLog(msgType, msgText)
                        }
                        break
                    }
                }
            }
        }
    }
}

// Parsing the log of the Run object to get sub builds triggered by it
def getTriggeredBuildsByBuild(run) {
    common.log('debug', "getTriggeredBuildsByBuild(${run.project.name})")
    def list = []
    if (run?.logFile != null) {
        run.logFile.text.split('\n').each { line->
            //strip off jenkins specific encoding
            line = ConsoleNote.removeNotes(line);
            Matcher matcher = line=~/.*Starting building: (.*) #(\d+)/
            if (matcher) {
               def foundJob = matcher[0][1]
               def foundBuildNum = Integer.parseInt(matcher[0][2])
               list.add(Jenkins.instance.getItem(foundJob).getBuildByNumber(foundBuildNum))
            }
        }
    }
    return list
}


// Add OverloadTCs and ActionPoints spreadsheet links to an already generated HTML report
String addAPlinks(String reportFile) {
}
