package com.ericsson

/**
 * <p>Store all CL2 parsed data</p>
 * <p>Expected data structure:</p>
 *
 * <p>section: [
 *     subsection: [
 *         build(parent job): [
 *             url
 *             description
 *             startTime
 *             testsuites(child jobs): [
 *                 summary: [
 *                     pass:<int>,
 *                     fail:<int>,
 *                     skip:<int>
 *                 ],
 *                 testcases: [
 *                     tc1:[
 *                         verdict:PASS|FAIL|SKIP,
 *                         logs: [
 *                             'error1',
 *                             'error2'
 *                         ]
 *                     ],
 *                     tc2:[
 *                         verdict:PASS|FAIL|SKIP,
 *                         logs: [
 *                             'error1',
 *                             'error2'
 *                         ]
 *                     ]
 *                 ]
 *             ]
 *         ]
 *     ]
 * ]</p>
 */

class Cl2Report implements Serializable {

    def    data
    String currentSection
    String currentSubSection
    String currentBuild
    String currentChildBuild
    String currentTcName
    String reportBaseName
    def    replaceList

    /**
     * <p><b>Constructor:</b> Set initial values</p>
     */
    Cl2Report(String filename, def removeStrings) {
        // Properties initialization can not be refactorized in new methods due to
        // cps-transformed from non-cps-transformed calls:
        // https://www.jenkins.io/doc/book/pipeline/cps-method-mismatches/#use-of-pipeline-steps-fromnoncps
        // To-try: "@NonCPS" in cps-transformed methods
        reportBaseName = filename
        replaceList = removeStrings
        currentSection = ""
        currentSubSection = ""
        currentBuild = ""
        currentChildBuild = ""
        currentTcName = "beforeTest"
        data = [:]
    }

    /**
     * <p> set map 1st level key</p>
     */
    void setSection(String section) {
        currentSection = section
        currentSubSection = ""
        currentBuild = ""
        currentChildBuild = ""
        currentTcName = "beforeTest"
        data[currentSection] = [:]
    }

    /**
     * <p> set map 2nd level key</p>
     */
    void setSubSection(String jobType) {
        currentSubSection = jobType
        currentBuild = ""
        currentChildBuild = ""
        currentTcName = "beforeTest"
        data[currentSection][currentSubSection] = [:]
    }

    /**
     * <p> set map 3rd level key: CCSM-NFT parent job build</p>
     */
    void addParentBuild(def build, boolean building) {
        currentBuild = "${build.project.name}/${build.number}"
        currentChildBuild = ""
        currentTcName = "beforeTest"
        data[currentSection][currentSubSection][currentBuild] = [:]
        data[currentSection][currentSubSection][currentBuild]['url'] = build.absoluteUrl
        data[currentSection][currentSubSection][currentBuild]['description'] = build.description?.replace('<br>', ', ')
        data[currentSection][currentSubSection][currentBuild]['startTime'] = new Date(build.startTimeInMillis).format("yyyy-MM-dd HH:mm")
        data[currentSection][currentSubSection][currentBuild]['testsuites'] = [:]
        data[currentSection][currentSubSection][currentBuild]['building'] = building
    }

    /**
     * <p> set map 4th level key: CCSM-NFT child job build</p>
     */
    void addChildBuild(def childBuild) {
        currentChildBuild = "${childBuild.project.name}/${childBuild.number}"
        currentTcName = "beforeTest"
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild] = [:]
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['url'] = childBuild.absoluteUrl
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'] = [:]
    }

    /**
     * <p> set map 4th level result values</p>
     */
    void setChildBuildResult(String totalCount, String failCount, String skipCount) {
        setChildBuildResult(totalCount.toInteger(), failCount.toInteger(), skipCount.toInteger())
    }

    void setChildBuildResult(int totalCount, int failCount, int skipCount) {
        def save = true
        if (data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild].containsKey('summary')) {
            if (data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['summary']['total'] >= totalCount) {
                save = false
            }
        }
        if (save) {
            data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['summary'] = [
                'total': totalCount,
                'pass': totalCount-skipCount-failCount,
                'fail': failCount,
                'skip': skipCount
            ]
        }
    }

    /**
     * <p> set map 5th level test case name</p>
     */
    void addTc(String tcName, String description='') {
        currentTcName = tcName
        if (description != '') {
            currentTcName += " |${description}"
        }
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName] = [:]
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['verdict'] = "PASS"
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['logs'] = []
        data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['analysis'] = ""
    }

    /**
     * <p> set map 5th level test case verdict</p>
     */
    void setTcResult(String pass, String fail, String skip) {
        if (!data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild].containsKey('summary')) {
            data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['summary'] = ['total':0, 'pass':0, 'skip':0, 'fail':0]
        }
        if (currentTcName != "" && currentTcName != "beforeTest") {
            data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['summary']['total'] += 1
            def verdict = "SKIP"
            if (fail == "1") {
                verdict = "FAIL"
            } else if (pass == "1") {
                verdict = "PASS"
                data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['logs'] =
                    cleanUpPassedTcLogs(data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['logs'])
            }
            data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['summary'][verdict.toLowerCase()] += 1
            data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['verdict'] = verdict
        }
    }

    /**
     * <p> set map 5th level test case log messages</p>
     */
    void addTcLog(String type, String text) {
        // i.e. TC = "beforeTest"
        if (!data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'].containsKey(currentTcName)) {
            addTc(currentTcName)
        }
        if (!data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['logs'].contains(text)) {
            if (type.toLowerCase() == 'assert' || type.toLowerCase() == 'fail') {
                data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['verdict'] = "FAIL"
            } else if (type.toLowerCase() == 'info') {
                text = "${type}: ${text}"
            }
            data[currentSection][currentSubSection][currentBuild]['testsuites'][currentChildBuild]['testcases'][currentTcName]['logs'].add(text)
        }
    }

    /**
     * <p> Do not show logs for PASS tests unless it is the max rate in GetMaxRate scenarios</p>
     */
    def cleanUpPassedTcLogs(def logList) {
        def passLogs = []
        logList.each {
            if (it.toLowerCase().startsWith("info")) {
                passLogs.add(it)
            }
        }
        return passLogs
    }

    /**
     * <p> - Remove empty sections</p>
     * <p> - Remove logs for passed TCs</p>
     */
    void postProcessData() {
        def dataFiltered = [:]
        boolean sectionAdded = false

        // Remove empty sections
        data.each { section->
            sectionAdded = false
            section.value.each { subsection->
                if (subsection.value != [:]) {
                    if (!sectionAdded) {
                        dataFiltered[section.key] = [:]
                        sectionAdded = true
                    }
                    dataFiltered[section.key][subsection.key] = subsection.value
                }
            }
        }
        // Remove logs in passed TCs
        dataFiltered.each { section->
            section.value.each { subsection->
                subsection.value.each { parent->
                    parent.value.'testsuites'.each { ts->
                        ts.value.'testcases'.each { tc->
                            if (tc.value.verdict == 'PASS' && tc.value.logs != []) {
                                tc.value.logs = cleanUpPassedTcLogs(tc.value.logs)
                            }
                        }
                    }
                }
            }
        }
        return dataFiltered
    }

    /**
     * <p> Return generated report (map data type) </p>
     */
    def getReportData() {
        return data
    }

    /**
     * <p> Instead of generating the report use the provided map</p>
     */
    void setReportData(def inputData) {
        data = inputData
    }

    /**
     * <p> Apply a different style to test case description (if exists) during "createHtmlReport"</p>
     */
    String printTc(def testCase) {
        def tc = testCase.key.split(/\|/)
        if (tc.size() > 1) {
            return "${tc[0]}<font class='tcdesc'> - \"${tc[1].stripIndent()}\"</font>"
        }
        return tc[0]
    }

    /**
     * <p> Use generated data to create a HTML report</p>
     */
    String createHtmlReport() {
        String colorPass = "#95e717"
        String colorSkip = "#ffbb00"
        String colorFail = "#ff0000"
        String htmlOutput = """<html><head><meta http-equiv="content-type" content="text/html; charset=UTF-8"><style>
            BODY, TABLE, TD, TH, P {
                font-family: Calibri, Verdana, Helvetica, sans serif;
                font-size: 12px;
                color: black;
            }
            .section {
                color: white;
                background-color: blue;
                font-weight: bold;
                width: 200%;
            }
            .subsection {
                color: white;
                background-color: lightblue;
                font-size: 150%;
                font-weight: bold;
                padding-left: 1em;
            }
            .build {
                color: black;
                font-size: 110%;
                padding-left: 2em;
            }
            .job {
                color: black;
                font-size: 110%;
                padding-left: 4em;
            }
            .tc {
                color: black;
                font-size: 100%;
                font-weight: normal;
                padding-left: 6em;
            }
            .tcdesc {
                color: black;
                font-size: 100%;
                font-weight: normal;
            }
            .console {
                color: #888888;
                font-family: Courier New;
                padding-left: 8em;
                font-weight: normal;
            }
            .analysis {
                color:royalblue;
                padding-left: 8em;
                font-weight: bold;
            }
            .passedtest {
                height: 10px;
                width: 10px;
                background-color: ${colorPass};
                border-radius: 50%;
                display: inline-block;
                right: 0px;
            }
            .failedtest {
                height: 10px;
                width: 10px;
                background-color: ${colorFail};
                border-radius: 50%;
                display: inline-block;
            }
            .skippedtest {
                height: 10px;
                width: 10px;
                background-color: ${colorSkip};
                border-radius: 50%;
                display: inline-block;
            }
            </style></head><body>
        """

        def finalData = postProcessData()
        finalData.each { section->
            htmlOutput += "<h1 class='section'>${section.key}</h1>\n"
            section.value.each { subSection->
                htmlOutput += "<h2 class='subsection'>${subSection.key}</h2>\n"
                subSection.value.each { parentBuild->
                    htmlOutput += "<p class='build'><b><u><a href='${parentBuild.value.url}'>#${parentBuild.key.split(/\//)[-1]}</a></u></b>"
                    htmlOutput += "&nbsp;-&nbsp;${parentBuild.value.startTime}\n"
                    htmlOutput += "&nbsp;-&nbsp;${parentBuild.value.description}\n"
                    if (parentBuild.value.building) {
                        htmlOutput += "&nbsp;-&nbsp;<font color='orange'>STILL BUILDING</font>\n"
                    }
                    htmlOutput += "</p>"
                    parentBuild.value.testsuites.each { testsuite->
                        String tsName = testsuite.key.split(/\//)[0]
                        replaceList.each {
                            tsName = tsName.replace(it, '')
                        }
                        htmlOutput += "<table><tbody><tr><td><span class='job'><b><a href='${testsuite.value.url}'>${tsName}</a></b>"
                        if (testsuite.value.containsKey('summary')) {
                            // Coloring the most significant result only
                            String fontPassStart = ""
                            String fontPassEnd   = ""
                            String fontSkipStart = ""
                            String fontSkipEnd   = ""
                            String fontFailStart = ""
                            String fontFailEnd   = ""
                            if (testsuite.value.summary.pass > 0) {
                                fontPassStart = "<font color=${colorPass}>"
                                fontPassEnd   = "</font>"
                            }
                            if (testsuite.value.summary.skip > 0) {
                                fontPassStart = ""
                                fontPassEnd   = ""
                                fontSkipStart = "<font color=${colorSkip}>"
                                fontSkipEnd   = "</font>"
                            }
                            if (testsuite.value.summary.fail > 0) {
                                fontPassStart = ""
                                fontPassEnd   = ""
                                fontSkipStart = ""
                                fontSkipEnd   = ""
                                fontFailStart = "<font color=${colorFail}>"
                                fontFailEnd   = "</font>"
                            }
                            htmlOutput += ": ${fontPassStart}${testsuite.value.summary.pass} pass${fontPassEnd} "
                            htmlOutput += "- ${fontSkipStart}${testsuite.value.summary.skip} skip${fontSkipEnd} "
                            htmlOutput += "- ${fontFailStart}${testsuite.value.summary.fail} fail${fontFailEnd}"
                        }
                        htmlOutput += "</span></td></tr>\n"
                        testsuite.value.testcases.each { testcase->
                            if (testcase.key != "beforeTest") {
                                String verdictClass = "skippedtest"
                                if (testcase.value.verdict == "FAIL") {
                                    verdictClass = "failedtest"
                                } else if (testcase.value.verdict == "PASS") {
                                    verdictClass = "passedtest"
                                }
                                htmlOutput += "<tr><td><span class='tc'><div class='${verdictClass}'></div> ${printTc(testcase)}</span></td></tr>\n"
                            } else {
                                htmlOutput += "<tr><td><span class='tc'>${printTc(testcase)}</span></td></tr>\n"
                            }
                            testcase.value.logs.each {log->
                                htmlOutput += "<tr><td><span class='console'>${log}</span></td></tr>\n"
                            }
                            if (testcase.value.containsKey('analysis') && testcase.value.analysis != "") {
                                htmlOutput += "<tr><td><span class='analysis'>Analysis: ${testcase.value.analysis}</td></tr>\n"
                            }
                        }
                        htmlOutput += "</tbody></table><br>\n"
                    }
                }
            }
        }
        htmlOutput += "</body></html>\n"
        return htmlOutput
    }

}
