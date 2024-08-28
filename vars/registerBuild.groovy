import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Safety integer conversion of a string containing number or a empty/null
 */
int toInt(String str) {
    int result = 0
    if (str?:'' != '') {
        result = str.toInteger()
    }
    return result
}


/**
 * Aritmethic sum of two strings that should contain numbers
 */
String sumValues(String sum1, String sum2) {
    return sumValues(sum1, toInt(sum2))
}


/**
 * Aritmethic sum of a string (containing number) and an integer
 */
String sumValues(String sum1, Integer sum2) {
    common.log('debug', "sumValues::begin(${sum1}, ${sum2})")
    def result = (toInt(sum1) + sum2).toString()
    common.log('debug', "sumValues::end(${result})")
    return result
}


/**
 * Get CCSM version from a Jenkins job build description
 */
def getCcsmVersionFromBuildDescription(String description) {
    def ccsmVersion = ""
    def rg1 = description =~ /.*(eric-ccsm-[0-9\.\-]+).*/
    if (rg1) {
        ccsmVersion = rg1.group(1)
    }
    return ccsmVersion
}


/**
 * Get CCSM "drop" from Jenkins build info
 */
def getDropFromBuild(def build, def cfg, def ccsmPkg) {
    def dropName = ""
    def rg1 = build.description =~ /([a-z0-9]+)\(([a-z0-9]+)\).*/
    if (rg1) {
        if (rg1.group(1) == "latest") {
            nft5g.checkOutRepoBranch(cfg.repository, cfg.NFT_BRANCH, cfg.credentials, rg1.group(2))
            def rg2 = nft5g.getProjectVersion("nft_automation/pom.xml") =~ /([0-9]+)\.([0-9]+)\.([0-9]+.*)-SNAPSHOT/
            if (rg2) {
                dropName = "${rg2.group(2)}-${rg2.group(3)}"
            }
        } else {
            dropName = "${rg1.group(1).replace('drop', '')}-${ccsmPkg.split('-')[-1]}"
        }
    }
    return dropName
}


/**
 * CCSM non-FT CL2 test suites execution
 */
def call(body) {
    def config_in = [:]  // User parameters in the Jenkinsfile
    def config    = [:]  // All parameters
    def sourceBuild = null
    def environmentName = ""
    def ccsmVersion = ""
    def drop = ""
    def cl2Type = ""
    def summaryResults = [:]

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    stage("Initialize") {
        config = nft5g.customizeConfig(params, config_in)
    }

    node("CCSM_NFT") {

        stage("Configure") {
            currentBuild.description = "${params.SOURCE_JOBNAME}#${params.SOURCE_BUILD_NUMBER}"
            sourceBuild = Jenkins.instance.getItem(SOURCE_JOBNAME).getBuildByNumber(SOURCE_BUILD_NUMBER.toInteger())
            environmentName = common.getEnvName(sourceBuild.envVars['CLOUD'])
            ccsmVersion = getCcsmVersionFromBuildDescription(sourceBuild.description)
            drop = getDropFromBuild(sourceBuild, config, ccsmVersion)
            cl2Type = "CL2 ${nft5g.getCcsmNftJobType(SOURCE_JOBNAME)} Stability"
        }

        stage("Summary results") {
            summaryResults[cl2Type] = [
                'Execution Date': new Date(sourceBuild.startTimeInMillis),
                'Build number': SOURCE_BUILD_NUMBER,
                'environment': environmentName,
                'CCSM Drop': drop,
                'CCSM Version': ccsmVersion,
                'Total Number of TCs': 0,
                'Number of executed TCs': '',
                'Successful TCs': '',
                'Cluster-related Failures': '',  // TODO: Calc this value
                'Failed TCs (unstable)': '',
                'Failed TCs (CCSM)': '',
                'Failed TCs (ADP)': '',          // TODO: Calc this value
                'Uninstallation TS Failures': '',
                'Installation TS Failures': '',
                'Traffic Mix TS Failures': '',
                'Stability 1 hour TS Failures': '',
                'Maintainability TS Failures': '',
                'Robustness TS Failures': '',
                'License TS Failures': ''
            ]
            // IBD only columns
            if (environmentName.contains('ibd') || environmentName.contains('vnf')) {
                summaryResults[cl2Type]['Overload TS Failures'] = ''
            }

            common.getChildJobList(sourceBuild).each { childBuild->
                // Total number of TCs
                summaryResults[cl2Type]['Total Number of TCs'] += nft5g.getTotalNumberOfTcs(childBuild)
                // Add test results
                testResultAction = childBuild.getAction(hudson.tasks.junit.TestResultAction.class)
                if (testResultAction) {
                    common.log('debug', "childBuild:: testResultAction:${testResultAction.totalCount}")
                    summaryResults[cl2Type]['Number of executed TCs'] = sumValues(summaryResults[cl2Type]['Number of executed TCs'], testResultAction.totalCount)
                    summaryResults[cl2Type]['Failed TCs (CCSM)'] = sumValues(summaryResults[cl2Type]['Failed TCs (CCSM)'], testResultAction.failCount)
                    summaryResults[cl2Type]['Successful TCs'] = sumValues(summaryResults[cl2Type]['Successful TCs'], testResultAction.totalCount-testResultAction.failCount-testResultAction.skipCount)
                    if ((toInt(summaryResults[cl2Type]['Failed TCs (CCSM)']) + toInt(summaryResults[cl2Type]['Successful TCs']))
                          > summaryResults[cl2Type]['Total Number of TCs']) {
                        summaryResults[cl2Type]['Total Number of TCs'] = sumValues(summaryResults[cl2Type]['Failed TCs (CCSM)'],
                                                                                   summaryResults[cl2Type]['Successful TCs']).toInteger()
                    }
                    if (childBuild.project.name.contains('cleanup')) {
                        summaryResults[cl2Type]['Uninstallation TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('install')) {
                        summaryResults[cl2Type]['Installation TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('trafficmix')) {
                        summaryResults[cl2Type]['Traffic Mix TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('stability')) {
                        summaryResults[cl2Type]['Stability 1 hour TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('maintainbility')) {
                        summaryResults[cl2Type]['Maintainability TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('robustness')) {
                        summaryResults[cl2Type]['Robustness TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('licenses')) {
                        summaryResults[cl2Type]['License TS Failures'] = testResultAction.failCount.toString()
                    } else if (childBuild.project.name.contains('overload')) {
                        summaryResults[cl2Type]['Overload TS Failures'] = testResultAction.failCount.toString()
                    }
                    summaryResults[cl2Type]['Failed TCs (unstable)'] = sumValues(summaryResults[cl2Type]['Failed TCs (unstable)'], testResultAction.failCount)
                }
            }
            //summaryResults[cl2Type].clusterRelatedFailures = summaryResults[cl2Type].totalNumberOfTcs - summaryResults["${cl2Type}"].numberOfExecutedTcs
            // Save summary to a YAML file
            String yamlSummaryFileName = "summary_results.yaml"
            if (fileExists(yamlSummaryFileName)) {
                sh returnStatus:true, script:"rm -f ${yamlSummaryFileName}"
            }
            writeYaml file:yamlSummaryFileName, data:summaryResults
            // Update XLSX file
            def py_script = common.copyGlobalLibraryScript('tools/yamlToXlsx.py')
            def extra_params = "-n ${common.getPipelineResource().links.NFT_Automation_TCs.fileName} -o ${common.getPipelineResource().links.NFT_Automation_TCs.pageId}"
            if (sourceBuild.project.name.contains("IPV6")) {
                extra_params += " --ipv6"
            }
            if (sourceBuild.project.name.contains("CLEAR")) {
                extra_params += " --clear"
            }

            if (config.DRY_RUN) {
                extra_params += " --dry-run"
            }

            withCredentials(
                [usernamePassword(
                    credentialsId: 'userpwd-adp',
                    usernameVariable: 'jenkins_user',
                    passwordVariable: 'jenkins_pwd')
                ])
            {
                def py_module = 'python/3.6.6'
                def addons_pyyaml_module_u18 = 'python/3.6-addons-pyyaml-5.1.2'
                def addons_pyyaml_module_u20= 'python/3.6-addons-pyyaml-5.4.1'
                def addons_requests_module = 'python/3.6-addons-requests'
                def addons_openpyxl_module_18 = 'python/3.6-addons-openpyxl-3.0.7'
                def addons_openpyxl_module_20 = 'python/3.6-addons-openpyxl-3.0.9'

                def ubuntu_version = sh(returnStdout:true, script:" lsb_release -a ")
                println "ubuntu_version=${ubuntu_version}"
                def py_script_aux = "module add ${py_module} ${addons_requests_module}"
                if (ubuntu_version && ubuntu_version.contains("20"))
                    py_script_aux = " ${py_script_aux} ${addons_pyyaml_module_u20} ${addons_openpyxl_module_20}"
                else
                    py_script_aux = " ${py_script_aux} ${addons_pyyaml_module_u18} ${addons_openpyxl_module_18}"

                py_script = "${py_script_aux}; ${py_script}"

                sh """
                    . /etc/home/bashrc
                    ${py_script} -i ${yamlSummaryFileName} -u ${jenkins_user} -p ${jenkins_pwd} ${extra_params}
                """
            }

        }  // stage
    }  // node
}  // call

