pipelineJob('CCSM_NFT_verify_nft_automation') {
    description('CCSM NFT repository commits Verify')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        stringParam('GERRIT_BRANCH', 'master', 'GIT repository branch name')
        stringParam('GERRIT_REFSPEC', 'master', 'GIT repository refspec value')
    }

    properties {
        pipelineTriggers {
            triggers {
                gerrit {
                    serverName('adp')
                    triggerOnEvents {
                        patchsetCreated()
                    }
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern('HSS/CCSM/nft')
                            branches {
                                branch {
                                    compareType('ANT')
                                    pattern('**')
                                }
                            }
                            filePaths {
                                filePath {
                                    compareType('REG_EXP')
                                    pattern('nft_automation.*')
                                }
                            }
                            forbiddenFilePaths {
                                filePath {
                                    compareType("ANT")
                                    pattern("**/*.feature")
                                }
                            }
                            disableStrictForbiddenFileVerification(true)
                        }
                    }
                    buildSuccessfulMessage(null)
                }
            }
        }
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        name('origin')
                        url('https://esdccci@gerritmirror-ha.rnd.ki.sw.ericsson.se/a/HSS/CCSM/nft')
                        credentials('userpwd-adp')
                        branch('${GERRIT_BRANCH}')
                        refspec('${GERRIT_REFSPEC}')
                    }
                    extensions {
                        wipeOutWorkspace()
                        choosingStrategy {
                            gerritTrigger()
                        }
                    }
                }
                lightweight(false)
                scriptPath('jenkinsFiles/Jenkinsfile.verify-nft_automation')
            }
        }
    }
}
