pipelineJob('CCSM_NFT_UDM_AUSF_EIR_INTERNAL') {
    description('UDM+AUSF+EIR internal Non Function Test parent job')

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
                    value "${ccsm_default_params['cloud']['value']},${ccsm_default_params['cloud_ipv6']['value']}"
                    defaultValue ccsm_default_params['cloud']['defaultValue']
                    descriptionPropertyValue "${ccsm_default_params['cloud']['value_description']},${ccsm_default_params['cloud_ipv6']['value_description']}"
                    multiSelectDelimiter ','
                }
            }
        }
    }
    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        stringParam(ccsm_default_params['udm_nft_auto_version']['name'], ccsm_default_params['udm_nft_auto_version']['value'], ccsm_default_params['udm_nft_auto_version']['help'])
        stringParam(ccsm_default_params['nft_branch']['name'], ccsm_default_params['nft_branch']['value'], ccsm_default_params['nft_branch']['help'])
        stringParam(ccsm_default_params['ccsm_package']['name'], ccsm_default_params['ccsm_package']['value'], ccsm_default_params['ccsm_package']['help'])
        stringParam(ccsm_default_params['smesh_package']['name'], ccsm_default_params['smesh_package']['value'], ccsm_default_params['smesh_package']['help'])
        booleanParam(ccsm_default_params['mtls']['name'], ccsm_default_params['mtls']['value'].toBoolean(), ccsm_default_params['mtls']['help'])
        booleanParam(ccsm_default_params['small_deployment']['name'], ccsm_default_params['small_deployment']['value'].toBoolean(), ccsm_default_params['small_deployment']['help'])
        booleanParam(ccsm_default_params['uninstall_tools']['name'], ccsm_default_params['uninstall_tools']['value'].toBoolean(), ccsm_default_params['uninstall_tools']['help'])
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

        booleanParam('RUN_ROBUSTNESS_INTERNAL', ccsm_default_params['run_robustness']['value'].toBoolean(), ccsm_default_params['run_robustness']['help'])
        booleanParam('RUN_ROBUSTNESS_INPROGRESS', ccsm_default_params['run_robustness']['value'].toBoolean(), ccsm_default_params['run_robustness']['help'])
        booleanParam('RUN_ROBUSTNESS_RELEASE', ccsm_default_params['run_robustness']['value'].toBoolean(), ccsm_default_params['run_robustness']['help'])
        booleanParam(ccsm_default_params['run_podsfaults']['name'], ccsm_default_params['run_podsfaults']['value'].toBoolean(), ccsm_default_params['run_podsfaults']['help'])
        booleanParam(ccsm_default_params['run_commonfailures']['name'], ccsm_default_params['run_commonfailures']['value'].toBoolean(), ccsm_default_params['run_commonfailures']['help'])
        stringParam(ccsm_default_params['robustness_engineering_cap']['name'], ccsm_default_params['robustness_engineering_cap']['value'], ccsm_default_params['robustness_engineering_cap']['help'])
        stringParam(ccsm_default_params['robustness_pass_rate']['name'], ccsm_default_params['robustness_pass_rate']['value'], ccsm_default_params['robustness_pass_rate']['help'])

        booleanParam(ccsm_default_params['run_licenses']['name'], ccsm_default_params['run_licenses']['value'].toBoolean(), ccsm_default_params['run_licenses']['help'])

        booleanParam(ccsm_default_params['run_overload_tcs']['name'], ccsm_default_params['run_overload_tcs']['value'].toBoolean(), ccsm_default_params['run_overload_tcs']['help'])
        booleanParam('RUN_OVERLOAD_TCS_INTERNAL', ccsm_default_params['run_overload_tcs']['value'].toBoolean(), ccsm_default_params['run_overload_tcs']['help'])
        booleanParam('RUN_OVERLOAD_TCS_INPROGRESS', ccsm_default_params['run_overload_tcs']['value'].toBoolean(), ccsm_default_params['run_overload_tcs']['help'])
        booleanParam('RUN_OVERLOAD_TCS_DAILY', ccsm_default_params['run_overload_tcs']['value'].toBoolean(), ccsm_default_params['run_overload_tcs']['help'])
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
                scriptPath('jenkinsFiles/Jenkinsfile.UDM_AUSF_EIR_INTERNAL')
            }
        }
    }
}
