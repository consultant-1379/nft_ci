pipelineJob('CCSM_NFT_scaling') {
    description('CCSM Non Functional Scaling Test')

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
        booleanParam(ccsm_default_params['small_deployment']['name'], ccsm_default_params['small_deployment']['value'].toBoolean(), ccsm_default_params['small_deployment']['help'])

        stringParam(ccsm_default_params['evnfm_server_name']['name'], ccsm_default_params['evnfm_server_name']['value'], ccsm_default_params['evnfm_server_name']['help'])
        stringParam(ccsm_default_params['csar_url']['name'], ccsm_default_params['csar_url']['value'], ccsm_default_params['csar_url']['help'])
        stringParam(ccsm_default_params['csar_path']['name'], ccsm_default_params['csar_path']['value'], ccsm_default_params['csar_path']['help'])
        stringParam(ccsm_default_params['csar_version']['name'], ccsm_default_params['csar_version']['value'], ccsm_default_params['csar_version']['help'])
        stringParam(ccsm_default_params['wf_ccsm_version']['name'], ccsm_default_params['wf_ccsm_version']['value'], ccsm_default_params['wf_ccsm_version']['help'])
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
                scriptPath('jenkinsFiles/Jenkinsfile.scaling')
            }
        }
    }
}
