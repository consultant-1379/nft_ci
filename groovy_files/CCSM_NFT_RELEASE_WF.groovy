pipelineJob('CCSM_NFT_RELEASE_WF') {
    description('CCSM work flows release Non Function Test parent job')

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
        stringParam(ccsm_default_params['udm_nft_auto_version']['name'], ccsm_default_params['udm_nft_auto_version']['value'], ccsm_default_params['udm_nft_auto_version']['help'])
        stringParam(ccsm_default_params['nft_branch']['name'], ccsm_default_params['nft_branch']['value'], ccsm_default_params['nft_branch']['help'])

        stringParam(ccsm_default_params['evnfm_server_name']['name'], ccsm_default_params['evnfm_server_name']['value'], ccsm_default_params['evnfm_server_name']['help'])
        stringParam(ccsm_default_params['csar_url']['name'], ccsm_default_params['csar_url']['value'], ccsm_default_params['csar_url']['help'])
        stringParam(ccsm_default_params['csar_path']['name'], ccsm_default_params['csar_path']['value'], ccsm_default_params['csar_path']['help'])
        stringParam(ccsm_default_params['csar_version']['name'], ccsm_default_params['csar_version']['value'], ccsm_default_params['csar_version']['help'])
        stringParam(ccsm_default_params['wf_ccsm_version']['name'], ccsm_default_params['wf_ccsm_version']['value'], ccsm_default_params['wf_ccsm_version']['help'])

        booleanParam(ccsm_default_params['small_deployment']['name'], ccsm_default_params['small_deployment']['value'].toBoolean(), ccsm_default_params['small_deployment']['help'])
        booleanParam(ccsm_default_params['simu_traffic_enabled']['name'], ccsm_default_params['simu_traffic_enabled']['value'].toBoolean(), ccsm_default_params['simu_traffic_enabled']['help'])
        booleanParam(ccsm_default_params['sms_over_nas']['name'], ccsm_default_params['sms_over_nas']['value'].toBoolean(), ccsm_default_params['sms_over_nas']['help'])
        stringParam(ccsm_default_params['dtg_version']['name'], ccsm_default_params['dtg_version']['value'], ccsm_default_params['dtg_version']['help'])

        booleanParam(ccsm_default_params['run_trafficmix']['name'], ccsm_default_params['run_trafficmix']['value'].toBoolean(), ccsm_default_params['run_trafficmix']['help'])
        stringParam(ccsm_default_params['trafficmix_engineering_cap']['name'], ccsm_default_params['trafficmix_engineering_cap']['value'], ccsm_default_params['trafficmix_engineering_cap']['help'])
        stringParam(ccsm_default_params['trafficmix_pass_rate']['name'], ccsm_default_params['trafficmix_pass_rate']['value'], ccsm_default_params['trafficmix_pass_rate']['help'])

        booleanParam(ccsm_default_params['run_stability']['name'], ccsm_default_params['run_stability']['value'].toBoolean(), ccsm_default_params['run_stability']['help'])
        stringParam(ccsm_default_params['stability_engineering_cap']['name'], ccsm_default_params['stability_engineering_cap']['value'], ccsm_default_params['stability_engineering_cap']['help'])
        stringParam(ccsm_default_params['stability_pass_rate']['name'], ccsm_default_params['stability_pass_rate']['value'], ccsm_default_params['stability_pass_rate']['help'])
        stringParam(ccsm_default_params['stability_duration']['name'], ccsm_default_params['stability_duration']['value'], ccsm_default_params['stability_duration']['help'])
        stringParam(ccsm_default_params['stability_sample_rate']['name'], ccsm_default_params['stability_sample_rate']['value'], ccsm_default_params['stability_sample_rate']['help'])

        booleanParam(ccsm_default_params['run_maintainability']['name'], ccsm_default_params['run_maintainability']['value'].toBoolean(), ccsm_default_params['run_maintainability']['help'])

        booleanParam(ccsm_default_params['run_robustness']['name'], ccsm_default_params['run_robustness']['value'].toBoolean(), ccsm_default_params['run_robustness']['help'])
        stringParam(ccsm_default_params['robustness_engineering_cap']['name'], ccsm_default_params['robustness_engineering_cap']['value'], ccsm_default_params['robustness_engineering_cap']['help'])
        stringParam(ccsm_default_params['robustness_pass_rate']['name'], ccsm_default_params['robustness_pass_rate']['value'], ccsm_default_params['robustness_pass_rate']['help'])

        booleanParam(ccsm_default_params['run_licenses']['name'], ccsm_default_params['run_licenses']['value'].toBoolean(), ccsm_default_params['run_licenses']['help'])

        booleanParam(ccsm_default_params['run_overload_tcs']['name'], ccsm_default_params['run_overload_tcs']['value'].toBoolean(), ccsm_default_params['run_overload_tcs']['help'])

        booleanParam(ccsm_default_params['run_scaling']['name'], ccsm_default_params['run_scaling']['value'].toBoolean(), ccsm_default_params['run_scaling']['help'])

        booleanParam(ccsm_default_params['run_update']['name'], ccsm_default_params['run_update']['value'].toBoolean(), ccsm_default_params['run_update']['help'])
        stringParam(ccsm_default_params['csar_upgrade_path']['name'], ccsm_default_params['csar_upgrade_path']['value'], ccsm_default_params['csar_upgrade_path']['help'])
        stringParam(ccsm_default_params['csar_upgrade_version']['name'], ccsm_default_params['csar_upgrade_version']['value'], ccsm_default_params['csar_upgrade_version']['help'])
        stringParam(ccsm_default_params['wf_ccsm_upgrade_version']['name'], ccsm_default_params['wf_ccsm_upgrade_version']['value'], ccsm_default_params['wf_ccsm_upgrade_version']['help'])
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
                scriptPath('jenkinsFiles/Jenkinsfile.RELEASE_WF')
            }
        }
    }
}
