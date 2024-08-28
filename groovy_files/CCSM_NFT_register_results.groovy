pipelineJob('CCSM_NFT_register_results') {
    description('Collect and register all NFT results')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    parameters {
        booleanParam(ccsm_default_params['debug']['name'], ccsm_default_params['debug']['value'].toBoolean(), ccsm_default_params['debug']['help'])
        stringParam('SOURCE_JOBNAME', '', 'Parent job name that triggered CCSM-NFT child jobs')
        stringParam('SOURCE_BUILD_NUMBER', '', 'Parent build number that triggered CCSM-NFT child jobs')
        booleanParam('DRY_RUN', false, 'Execute all steps but upload result')
    }

    definition {
        cps {
            sandbox(true)
            script('''
@Library("PipelineNft5gLibrary") _

registerBuild {}
''')
        }
    }
}
