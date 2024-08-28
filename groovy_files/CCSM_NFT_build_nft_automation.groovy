pipelineJob('CCSM_NFT_build_nft_automation') {
    description('CCSM NFT Build')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        stringParam('GERRIT_BRANCH', 'master', 'GIT repository branch name')
        stringParam('GERRIT_REFSPEC', 'master', 'GIT repository refspec value')
        stringParam('TIMEOUT', '20', 'Fail the build if it takes more than this time in minutes')
    }

    properties {
        pipelineTriggers {
            triggers {
                gerrit {
                    serverName('adp')
                    triggerOnEvents {
                        changeMerged()
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
                                    pattern('.*nft_auto.*')
                                }
                            }
                            disableStrictForbiddenFileVerification(false)
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
                scriptPath('jenkinsFiles/Jenkinsfile.build-nft_automation')
            }
        }
    }
}
