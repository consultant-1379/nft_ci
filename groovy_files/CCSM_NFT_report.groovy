pipelineJob('CCSM_NFT_report') {
    description('CCSM NFT CL2 automatic report generator')

    logRotator(-1, 15, 1, -1)

    authorization {
        permission('hudson.model.Item.Read:authenticated')
    }

    parameters {
        booleanParam("COLLECT_REPORT_DATA", true, "Parse all CCSM-NFT Jenkins builds within the selected datetimes generating a manually updatable JSON file")
        booleanParam("INCLUDE_STILL_RUNNING", true, "Parse also jobs that are not yet finished (still building)")
        booleanParam("INCLUDE_INTERNAL", false, "Include INTERNAL job builds in report")
        stringParam("DATETIME_FROM", "", "Format: yyyyMMddHHmmss (i.e. Oct 13th 2021 8:30:00) => 20211013083000.<br><b>Leave empty for last 24hours.</b>")
        stringParam("DATETIME_TO", "", "Format: yyyyMMddHHmmss.<br><b>Leave empty to collect all data until now.</b><br><br><hr>")
        booleanParam("CREATE_HTML_REPORT", true, "Using generated Json data file or user selected one in INPUT_JSON_DATA_FILE to create a HTML report")
        textParam('INPUT_YAML_REPORT', "", "Provide this parameter to use a previous generated YAML data file<br><b>IMPORTANT: Limit 2000 lines</b><br><hr>")
        booleanParam("SEND_REPORT_BY_EMAIL", false, "Send generated HTML report by email to the EMAIL_RECIPIENT list")
        choiceParam("EMAIL_TO", ccsm_default_params['email']['choices'], "Send email to this recipients<br><br>")
    }

    definition {
        cps {
            sandbox(true)
            script('''
@Library("PipelineNft5gLibrary") _

cl2Reporter {}
''')
        }
    }
}
