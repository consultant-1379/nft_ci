pipelineJob('CCSM_NFT_UDM_AUSF_EIR_KPIs_validation') {
    description('UDM+AUSF+EIR KPIs validation')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    configure {
        project->
            project / 'properties' << 'hudson.model.ParametersDefinitionProperty' {
                parameterDefinitions {
                    'com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition' {
                    name ccsm_default_params['cloud_kpis']['name']
                    quoteValue 'false'
                    type 'PT_SINGLE_SELECT'
                    value ccsm_default_params['cloud_kpis']['value']
                    defaultValue ccsm_default_params['cloud_kpis']['defaultValue']
                    descriptionPropertyValue ccsm_default_params['cloud_kpis']['value_description']
                    multiSelectDelimiter ','
                }
            }
        }
    }
    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        stringParam(ccsm_default_params['udm_nft_auto_version']['name'], ccsm_default_params['udm_nft_auto_version']['value'], ccsm_default_params['udm_nft_auto_version']['help'])
        stringParam(ccsm_default_params['nft_branch']['name'], ccsm_default_params['nft_branch']['value'], ccsm_default_params['nft_branch']['help'])
        stringParam(ccsm_default_params['kpi_threshold']['name'], ccsm_default_params['kpi_threshold']['value'], ccsm_default_params['kpi_threshold']['help'])
        stringParam(ccsm_default_params['kpi_rate']['name'], ccsm_default_params['kpi_rate']['value'], ccsm_default_params['kpi_rate']['help'])
        stringParam(ccsm_default_params['kpi_epc_rate']['name'], ccsm_default_params['kpi_epc_rate']['value'], ccsm_default_params['kpi_epc_rate']['help'])
        stringParam(ccsm_default_params['kpi_storage_path']['name'], ccsm_default_params['kpi_storage_path']['value'], ccsm_default_params['kpi_storage_path']['help'])
        stringParam(cnhss_default_params['gtla_backup_name']['name'], cnhss_default_params['gtla_backup_name']['value'], cnhss_default_params['gtla_backup_name']['help'])
        stringParam(ccsm_default_params['dtg_version']['name'], ccsm_default_params['dtg_version']['value'], ccsm_default_params['dtg_version']['help'])
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
                scriptPath('jenkinsFiles/Jenkinsfile.UDM_AUSF_EIR_KPIs_validation')
            }
        }
    }
}
