pipelineJob('CCSM_NFT_EPC_commonfailures') {
    description('HSS-EPC internal Non Functional CommonFailures Test')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    configure {
        project->
            project / 'properties' << 'hudson.model.ParametersDefinitionProperty' {
                parameterDefinitions {
                    'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition' {
                    name cnhss_default_params['cloud']['name']
                    quoteValue 'false'
                    type 'PT_SINGLE_SELECT'
                    value cnhss_default_params['cloud']['value']
                    defaultValue cnhss_default_params['cloud']['defaultValue']
                    descriptionPropertyValue cnhss_default_params['cloud']['value_description']
                    multiSelectDelimiter ','
                }
            }
        }
    }
    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        stringParam(ccsm_default_params['udm_nft_auto_version']['name'], ccsm_default_params['udm_nft_auto_version']['value'], ccsm_default_params['udm_nft_auto_version']['help'])
        stringParam(ccsm_default_params['nft_branch']['name'], ccsm_default_params['nft_branch']['value'], ccsm_default_params['nft_branch']['help'])
        booleanParam(cnhss_default_params['use_dtg_traffic']['name'], cnhss_default_params['use_dtg_traffic']['value'].toBoolean(), cnhss_default_params['use_dtg_traffic']['help'])
        stringParam(ccsm_default_params['dtg_version']['name'], ccsm_default_params['dtg_version']['value'], ccsm_default_params['dtg_version']['help'])
        stringParam(ccsm_default_params['robustness_engineering_cap']['name'], ccsm_default_params['robustness_engineering_cap']['value'], ccsm_default_params['robustness_engineering_cap']['help'])
        stringParam(ccsm_default_params['robustness_pass_rate']['name'], ccsm_default_params['robustness_pass_rate']['value'], ccsm_default_params['robustness_pass_rate']['help'])
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        name('origin')
                        url('https://esdccci@gerritmirror-ha.rnd.ki.sw.ericsson.se/a/HSS/CCSM/nft')
                        credentials('userpwd-adp')
                        branch('${'+ccsm_default_params['nft_branch']['name']+'}')
                    }
                }
                lightweight(false)
                scriptPath('jenkinsFiles/Jenkinsfile.EPC_commonfailures')
            }
        }
    }
}
