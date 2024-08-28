pipelineJob('CCSM_NFT_verify_feature') {
    description('CCSM NFT repository commits Verify: ".feature" files specific')

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
                                    compareType('ANT')
                                    pattern('**/*.feature')
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
        cps {
            sandbox(true)
            script('''
@Library("PipelineNft5gLibrary") _

verifyFeature {}
''')
        }
    }

}
