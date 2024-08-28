pipelineJob('CCSM_NFT_IMS_EPC_stability') {
    description('HSS-IMS+EPC Non Functional Stability Test')

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
        choiceParam(cnhss_default_params['traffic_type']['name'], cnhss_default_params['traffic_type']['value'], cnhss_default_params['traffic_type']['help'])
        booleanParam(ccsm_default_params['pre_post']['name'], true, ccsm_default_params['pre_post']['help'])
        booleanParam(cnhss_default_params['use_dtg_traffic']['name'], cnhss_default_params['use_dtg_traffic']['value'].toBoolean(), cnhss_default_params['use_dtg_traffic']['help'])
        stringParam(ccsm_default_params['dtg_version']['name'], ccsm_default_params['dtg_version']['value'], ccsm_default_params['dtg_version']['help'])

        stringParam(cnhss_default_params['gtla_backup_name']['name'], cnhss_default_params['gtla_backup_name']['value'], cnhss_default_params['gtla_backup_name']['help'])
        stringParam(cnhss_default_params['stability_engineering_cap']['name'], cnhss_default_params['stability_engineering_cap']['value'], cnhss_default_params['stability_engineering_cap']['help'])
        stringParam(cnhss_default_params['stability_engineering_cap_async']['name'], cnhss_default_params['stability_engineering_cap_async']['value'], cnhss_default_params['stability_engineering_cap_async']['help'])
        stringParam(cnhss_default_params['stability_engineering_cap_sync']['name'], cnhss_default_params['stability_engineering_cap_sync']['value'], cnhss_default_params['stability_engineering_cap_sync']['help'])
        stringParam(cnhss_default_params['stability_engineering_initial_async']['name'], cnhss_default_params['stability_engineering_initial_async']['value'], cnhss_default_params['stability_engineering_initial_async']['help'])
        stringParam(cnhss_default_params['stability_engineering_initial_sync']['name'], cnhss_default_params['stability_engineering_initial_sync']['value'], cnhss_default_params['stability_engineering_initial_sync']['help'])
        stringParam(ccsm_default_params['stability_pass_rate']['name'], ccsm_default_params['stability_pass_rate']['value'], ccsm_default_params['stability_pass_rate']['help'])
        stringParam(ccsm_default_params['stability_sample_rate']['name'], ccsm_default_params['stability_sample_rate']['value'], ccsm_default_params['stability_sample_rate']['help'])
        stringParam(cnhss_default_params['stability_duration']['name'], cnhss_default_params['stability_duration']['value'], cnhss_default_params['stability_duration']['help'])
        stringParam(cnhss_default_params['stability_error_rate']['name'], cnhss_default_params['stability_error_rate']['value'], cnhss_default_params['stability_error_rate']['help'])
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
                scriptPath('jenkinsFiles/Jenkinsfile.IMS_EPC_stability')
            }
        }
    }
}
