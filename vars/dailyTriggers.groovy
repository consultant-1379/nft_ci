/**
 * CCSM non-FT CL2 test suites execution
 */
import groovy.time.*


def getSecondsToWait(def dateStart, def dateParam) {
    def result = null
    def matcher = dateParam =~ /([0-9]{4}[-]{0,1}[0-9]{2}[-]{0,1}[0-9]{2}[ ]{0,1}[0-9]{2}[:]{0,1}[0-9]{2}).*|([0-9]{2}[:]{0,1}[0-9]{2}).*/
    if (matcher) {
        String dateEnd_in = ""
        if (matcher.group(2)) {
            def month = "${dateStart.month+1}"
            if (month.toInteger() < 10)
                month = "0${month}"
            def day = "${dateStart.getAt(Calendar.DAY_OF_MONTH)}"
            if (day.toInteger() < 10)
                day = "0${day}"
            dateEnd_in = "${dateStart.year+1900}${month}${day}${matcher.group(2).replace('-','').replace(':', '')}00"
        } else {
            dateEnd_in = "${matcher.group(1).replace('-','').replace(':', '').replace(' ', '')}00"
        }
        def dateEnd = Date.parse("yyyyMMddHHmmss", dateEnd_in)
        TimeDuration duration = TimeCategory.minus(dateEnd, dateStart)
        result = duration.toMilliseconds()/1000
    }
    return result
}


def call(body) {
    def config_in = [:]
    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    def dateStart = new Date()
    def nameList = [
        'UDM_AUSF_EIR_RELEASE',
        'UDM_AUSF_EIR_RELEASE_IPV6',
        'UDM_AUSF_EIR_RELEASE_CLEAR',
        'UDM_AUSF_EIR_INTERNAL',
        'EPC_RELEASE',
        'EPC_INTERNAL',
        'IMS_RELEASE',
        'IMS_INTERNAL',
        'RELEASE',
        'RELEASE_WF',
        'INTERNAL'
    ]
    def buildsMap = [:]

    node("CCSM_NFT") {

        stage("Configure") {
            nameList.each { jobPreffix->
                if (params["${jobPreffix}__TIME"]) {
                    buildsMap[jobPreffix] = {
                        stage(jobPreffix) {
                            def waitTime = getSecondsToWait(dateStart, params["${jobPreffix}__TIME"])
                            if (waitTime != null) {
                                def pList = [string(name: 'UDM_NFT_AUTO_VERSION', value: params["${jobPreffix}__NFT_VERSION"]),
                                             string(name: 'NFT_BRANCH',           value: params["${jobPreffix}__NFT_BRANCH"]),
                                             string(name: 'CCSM_PACKAGE',         value: params["${jobPreffix}__CCSM"]),
                                             string(name: 'SMESH_PACKAGE',        value: params["${jobPreffix}__SMESH"]),
                                             string(name: 'CLOUD',                value: params["${jobPreffix}__CLOUD"])]
                                params.each { pkey,pvalue->
                                    if (pkey.startsWith("${jobPreffix}_RUN_")) {
                                        pList.add(booleanParam(name: pkey.replace("${jobPreffix}_", ""), value: pvalue))
                                    }
                                }
                                build(quietPeriod: waitTime, wait: false, propagate: false, job: "CCSM_NFT_${jobPreffix}", parameters: pList)
                            }
                        }
                    }
                }
            }
        }

        parallel buildsMap

    }  // node

}  // call
