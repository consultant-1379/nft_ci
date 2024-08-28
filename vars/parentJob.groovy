/**
 * CCSM non-FT CL2 test suites execution
 */
def call(body) {
    def config_in    = [:]  // User parameters in the Jenkinsfile
    def config       = [:]  // All parameters
    def baseParams = []
    def commonParams = []
    def robParams    = []

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    stage("Initialize") {
        config = nft5g.customizeConfig(params, config_in)
    }

    lock("hss_5g_${config.BUILD_NODE}") {
        node(config.BUILD_NODE) {
            def lvl1_result
            def wf_lvl0_result
            def wf_lvl1_result

            stage("Configure") {
                config = automation.rosettaConfig(config)
                nft5g.checkOutRepository(config)
                config = automation.patchVersions(config)
                common.setBuildDescription(config)
                baseParams = [
                    booleanParam(name: 'DEBUG',                value: config.DEBUG),
                    string      (name: 'CLOUD',                value: config.BUILD_NODE)
                ]
                cleanupParams = baseParams + [
                    booleanParam(name: 'UNINSTALL_TOOLS', value: config.UNINSTALL_TOOLS)
                ]
                commonParams = baseParams + [
                    string      (name: 'UDM_NFT_AUTO_VERSION', value: config.UDM_NFT_AUTO_VERSION),
                    string      (name: 'NFT_BRANCH',           value: config.NFT_BRANCH),
                    booleanParam(name: 'LATEST_NFT_CODE',      value: config.LATEST_NFT_CODE),
                    // Not supported yet: booleanParam(name: 'IS_ANSI', value: config.IS_ANSI),
                    booleanParam(name: 'SIMU_TRAFFIC_ENABLED', value: config.SIMU_TRAFFIC_ENABLED),
                    booleanParam(name: 'SMS_OVER_NAS',         value: config.SMS_OVER_NAS)]
                robParams = commonParams
                wfParams = commonParams + [
                    string(name: 'EVNFM_SERVER_NAME',       value: config.EVNFM_SERVER_NAME),
                    string(name: 'CSAR_URL',                value: config.CSAR_URL),
                    string(name: 'CSAR_PATH',               value: config.CSAR_PATH),
                    string(name: 'CSAR_VERSION',            value: config.CSAR_VERSION),
                    string(name: 'WF_CCSM_VERSION',         value: config.WF_CCSM_VERSION)]
                if (!config.EPC && !config.IMS) {
                    robParams = commonParams + [
                        string(name: 'ENGINEERING_CAP',        value: config.ROBUSTNESS_ENGINEERING_CAP),
                        string(name: 'PASS_RATE',              value: config.ROBUSTNESS_PASS_RATE),
                        booleanParam(name: 'SMALL_DEPLOYMENT', value: config.SMALL_DEPLOYMENT),
                        string(name: 'DTG_VERSION',            value: config.DTG_VERSION)]
                }
            }

            stage("Clean up") {
                build(job: "${config.jobPreffix}cleanup${config.jobSuffix}", parameters: cleanupParams)
            }

            if (JOB_NAME.contains('_WF')) {
                stage("Onboarding"){
                    warnError("Onboarding failed"){
                        wf_lvl0_result = build(job: "${config.jobPreffix}onboarding${config.jobSuffix}", parameters: wfParams)
                    }
                }
                echo "Onboarding result=${wf_lvl0_result?.result}"
                if (wf_lvl0_result?.result == 'SUCCESS') {  // Onboarding SUCCESS
                    stage("Instantiate"){
                        warnError("Instantiate failed"){
                            wf_lvl1_result = build(job: "${config.jobPreffix}instantiate${config.jobSuffix}", parameters: wfParams)
                        }
                    }
                    echo"Instantiate result=${wf_lvl1_result?.result}"
                }

            } else { // No workflows
                stage("Installation") {
                    warnError("Installation failed") {
                        def instParams = commonParams + [string(name: 'CCSM_PACKAGE',           value: config.CCSM_PACKAGE),
                                                         string(name: 'SMESH_PACKAGE',          value: config.SMESH_PACKAGE),
                                                         booleanParam(name: 'SMALL_DEPLOYMENT', value: config.SMALL_DEPLOYMENT)]
                        if (config.SIMU_TRAFFIC_ENABLED) {
                            instParams += [booleanParam(name: 'SMS_OVER_NAS',    value: true),
                                           booleanParam(name: 'UMTS_ENABLED',    value: true),
                                           booleanParam(name: 'EIR_MAP_ENABLED', value: true)]
                        }
                        if (config.cl2Type == "Internal") {
                            lvl1_result = build(job: "${config.jobPreffix}installation_INTERNAL${config.jobSuffix}", parameters: instParams)
                        } else {
                            lvl1_result = build(job: "${config.jobPreffix}installation${config.jobSuffix}", parameters: instParams)
                        }
                    }
                }
                echo "Installation result=${lvl1_result?.result}"
            }

            if ((lvl1_result?.result == 'SUCCESS') || (wf_lvl1_result?.result == 'SUCCESS')) {  // Installation SUCCESS
                def lvl2_result
                if (params.RUN_TRAFFICMIX) {
                    stage("TrafficMix") {
                        warnError("TrafficMix failed") {
                            def tmixParams = commonParams
                            if (!config.EPC && !config.IMS) {
                                tmixParams += [string(name: 'ENGINEERING_CAP',        value: config.TRAFFICMIX_ENGINEERING_CAP),
                                               string(name: 'PASS_RATE',              value: config.TRAFFICMIX_PASS_RATE),
                                               booleanParam(name: 'SMALL_DEPLOYMENT', value: config.SMALL_DEPLOYMENT),
                                               string(name: 'DTG_VERSION',            value: config.DTG_VERSION)]
                            }
                            if (config.cl2Type == "Internal") {
                                lvl2_result = build(job: "${config.jobPreffix}trafficmix_INTERNAL${config.jobSuffix}", parameters: tmixParams)
                            } else {
                                lvl2_result = build(job: "${config.jobPreffix}trafficmix${config.jobSuffix}", parameters: tmixParams)
                            }
                        }
                    }
                }

                echo "TraffixMix result=${lvl2_result?.result}"
                if (!params.RUN_TRAFFICMIX || lvl2_result?.result == 'SUCCESS') {  // TrafficMix SUCCESS or Skipped
                    if (params.RUN_STABILITY) {
                        stage("Stability") {
                            warnError("Stability failed") {
                                def stabParams = commonParams +
                                                 [string(name: 'DURATION',               value: config.STABILITY_DURATION),
                                                  string(name: 'SAMPLE_RATE',            value: config.STABILITY_SAMPLE_RATE),
                                                  string(name: 'PASS_RATE',              value: config.STABILITY_PASS_RATE),
                                                  booleanParam(name: 'SMALL_DEPLOYMENT', value: config.SMALL_DEPLOYMENT)]
                                if ((config.EPC || config.IMS) && config.USE_DTG_TRAFFIC) {
                                    stabParams += [booleanParam(name: 'USE_DTG_TRAFFIC', value: config.USE_DTG_TRAFFIC),
                                                   string(name: 'GTLA_BACKUP_NAME',      value: config.GTLA_BACKUP_NAME),
                                                   string(name: 'ENGINEERING_CAP',       value: config.STABILITY_ENGINEERING_CAP),
                                                   string(name: 'ERROR_RATE',            value: config.STABILITY_ERROR_RATE),
                                                   string(name: 'DTG_VERSION',           value: config.DTG_VERSION)]
                                } else if (!config.EPC && !config.IMS) {
                                    stabParams += [string(name: 'ENGINEERING_CAP', value: config.STABILITY_ENGINEERING_CAP),
                                                   string(name: 'DTG_VERSION',     value: config.DTG_VERSION)]
                                } else{
                                    stabParams += [string(name: 'STABILITY_ENGINEERING_CAP_ASYNC',         value: config.STABILITY_ENGINEERING_CAP_ASYNC),
                                                   string(name: 'STABILITY_ENGINEERING_CAP_SYNC',          value: config.STABILITY_ENGINEERING_CAP_SYNC),
                                                   string(name: 'STABILITY_ENGINEERING_INITIAL_CPS_ASYNC', value: config.STABILITY_ENGINEERING_INITIAL_CPS_ASYNC),
                                                   string(name: 'STABILITY_ENGINEERING_INITIAL_CPS_SYNC',  value: config.STABILITY_ENGINEERING_INITIAL_CPS_SYNC)]
                                }
                                build(job: "${config.jobPreffix}stability${config.jobSuffix}", parameters: stabParams)
                            }
                        }
                    }

                    if (params.RUN_STABILITY_LONG) {
                        stage("Long Stability") {
                            warnError("Stability failed") {
                                def stabParams = commonParams +
                                                 [string(name: 'DURATION',               value: config.STABILITY_DURATION),
                                                  string(name: 'SAMPLE_RATE',            value: config.STABILITY_SAMPLE_RATE),
                                                  string(name: 'ENGINEERING_CAP',        value: config.STABILITY_ENGINEERING_CAP),
                                                  string(name: 'PASS_RATE',              value: config.STABILITY_PASS_RATE),
                                                  booleanParam(name: 'SMALL_DEPLOYMENT', value: config.SMALL_DEPLOYMENT)]
                                if ((config.EPC || config.IMS) && config.USE_DTG_TRAFFIC) {
                                    stabParams += [booleanParam(name: 'USE_DTG_TRAFFIC', value: config.USE_DTG_TRAFFIC),
                                                   string(name: 'GTLA_BACKUP_NAME',      value: config.GTLA_BACKUP_NAME),
                                                   string(name: 'ERROR_RATE',            value: config.STABILITY_ERROR_RATE),
                                                   string(name: 'DTG_VERSION',           value: config.DTG_VERSION)]
                                } else if (!config.EPC && !config.IMS) {
                                    stabParams += [string(name: 'DTG_VERSION', value: config.DTG_VERSION)]
                                }
                                build(job: "${config.jobPreffix}stability_long${config.jobSuffix}", parameters: stabParams)
                            }
                        }
                    }

                    if (params.RUN_MAINTAINABILITY) {
                        stage("Maintainability") {
                            warnError("Maintainability failed") {
                                build(job: "${config.jobPreffix}maintainability${config.jobSuffix}", parameters: robParams)
                            }
                        }
                    }

                    if (config.cl2Type == "Internal") {
                        if (params.RUN_ROBUSTNESS_INTERNAL) {
                            stage("Robustness Internal") {
                                warnError("Robustness Internal failed") {
                                    build(job: "${config.jobPreffix}robustness_INTERNAL${config.jobSuffix}", parameters: robParams)
                                }
                            }
                        }

                        if (params.RUN_ROBUSTNESS_INPROGRESS) {
                            stage("Robustness In Progress") {
                                warnError("Robustness In Progress failed") {
                                    build(job: "${config.jobPreffix}robustness_inprogress${config.jobSuffix}", parameters: robParams)
                                }
                            }
                        }

                        if (params.RUN_ROBUSTNESS_RELEASE) {
                            stage("Robustness Release") {
                                warnError("Robustness Release failed") {
                                    build(job: "${config.jobPreffix}robustness_RELEASE${config.jobSuffix}", parameters: robParams)
                                }
                            }
                        }
                    } else {
                        if (params.RUN_ROBUSTNESS) {
                            stage("Robustness") {
                                warnError("Robustness failed") {
                                    build(job: "${config.jobPreffix}robustness${config.jobSuffix}", parameters: robParams)
                                }
                            }
                        }
                    }
                    if (params.RUN_PODSFAULTS) {
                        stage("PodsFaults") {
                            warnError("PodFaults failed") {
                                build(job: "${config.jobPreffix}podsfaults${config.jobSuffix}", parameters: robParams)
                            }
                        }
                    }
                    if (params.RUN_COMMONFAILURES) {
                        stage("CommonFailures") {
                            warnError("CommonFailures failed") {
                                build(job: "${config.jobPreffix}commonfailures${config.jobSuffix}", parameters: robParams)
                            }
                        }
                    }
                    if (params.RUN_LICENSES) {
                        stage("Licenses") {
                            warnError("Licenses failed") {
                                build(job: "${config.jobPreffix}licenses${config.jobSuffix}", parameters: robParams)
                            }
                        }
                    }

                    if (params.RUN_OVERLOAD_TCS_DAILY) {
                        stage("Overload TCs daily") {
                            warnError("Overload TCs daily failed") {
                                build(job: "${config.jobPreffix}overload_daily${config.jobSuffix}", parameters: robParams)
                            }
                        }
                    }
                    if ((config.cl2Type == "Internal" || config.cl2Type == "Release") && params.RUN_OVERLOAD_TCS) {
                        stage("OVERLOAD TCs") {
                            warnError("OVERLOAD TCs failed") {
                                build(job: "${config.jobPreffix}overload${config.jobSuffix}", parameters: robParams)
                            }
                        }
                    }
                    if (config.cl2Type == "Internal") {
                        if (params.RUN_OVERLOAD_TCS_INTERNAL) {
                            stage("Overload TCs Internal") {
                                warnError("RobustnessInternal failed") {
                                    build(job: "${config.jobPreffix}overload_internal${config.jobSuffix}", parameters: robParams)
                                }
                            }
                        }

                        if (params.RUN_OVERLOAD_TCS_INPROGRESS) {
                            stage("Overload TCs In Progress") {
                                warnError("Overload TCs In Progress failed") {
                                    build(job: "${config.jobPreffix}overload_inprogress${config.jobSuffix}", parameters: robParams)
                                }
                            }
                        }
                    }

                    if (params.RUN_SCALING) {
                        stage("Scaling") {
                            warnError("Scaling failed") {
                                build(job: "${config.jobPreffix}scaling${config.jobSuffix}", parameters: wfParams)
                            }
                        }
                    }

                    if (params.RUN_UPDATE) {
                        stage("Update") {
                            warnError("Update failed") {
                                wfParams += [string(name: 'CSAR_UPGRARDE_PATH',      value: config.CSAR_UPGRARDE_PATH),
                                             string(name: 'CSAR_UPGRADE_VERSION',    value: config.CSAR_UPGRADE_VERSION),
                                             string(name: 'WF_CCSM_UPGRADE_VERSION', value: config.WF_CCSM_UPGRADE_VERSION)]
                                build(job: "${config.jobPreffix}update${config.jobSuffix}", parameters: wfParams)
                            }
                        }
                    }

                }  // TraffixMix SUCCESS
            }  // Installation or Onboarding+Instantiate SUCCESS

            if ( JOB_NAME.contains('_WF')) {
                stage("Terminate"){
                    warnError("Terminate failed"){
                        build(job: "${config.jobPreffix}terminate${config.jobSuffix}", parameters: wfParams)
                    }
                }
           }
        }  // node
    }  // lock
}  // call
