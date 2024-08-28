pipelineJob('CCSM_NFT_UDM_AUSF_EIR_KPIs') {
    description('UDM+AUSF+EIR KPIs validation parent job')

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
        stringParam(ccsm_default_params['ccsm_package']['name'], ccsm_default_params['ccsm_package']['value'], ccsm_default_params['ccsm_package']['help'])
        stringParam(ccsm_default_params['smesh_package']['name'], ccsm_default_params['smesh_package']['value'], ccsm_default_params['smesh_package']['help'])
        booleanParam('RUN_KPI', true, 'Execute KPIs validation test suite')
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
                scriptPath('jenkinsFiles/Jenkinsfile.UDM_AUSF_EIR_KPIs')
            }
        }
    }
}
