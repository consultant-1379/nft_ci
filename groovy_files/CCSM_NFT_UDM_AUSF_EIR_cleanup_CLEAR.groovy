pipelineJob('CCSM_NFT_UDM_AUSF_EIR_cleanup_CLEAR') {
    description('Environment/ECCD clean up')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    configure {
        project->
            project / 'properties' << 'hudson.model.ParametersDefinitionProperty' {
                parameterDefinitions {
                    'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition' {
                    name ccsm_default_params['cloud']['name']
                    quoteValue 'false'
                    type 'PT_SINGLE_SELECT'
                    value ccsm_default_params['cloud']['value']
                    defaultValue ccsm_default_params['cloud']['defaultValue']
                    descriptionPropertyValue ccsm_default_params['cloud']['value_description']
                    multiSelectDelimiter ','
                }
            }
        }
    }
    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        booleanParam(ccsm_default_params['uninstall_tools']['name'], ccsm_default_params['uninstall_tools']['value'].toBoolean(), ccsm_default_params['uninstall_tools']['help'])
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        name('origin')
                        url('https://esdccci@gerritmirror-ha.rnd.ki.sw.ericsson.se/a/HSS/CCSM/nft')
                        credentials('userpwd-adp')
                        branch('master')
                    }
                }
                lightweight(true)
                scriptPath('jenkinsFiles/Jenkinsfile.cleanup')
            }
        }
    }
}
