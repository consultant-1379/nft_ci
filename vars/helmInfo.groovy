//*************************************************************************************************
// Get information from installed CCSM releases
// Requirements:
//  - Call function "getCcsmVersion()" within a "node('CCSM_NFT') { ... }" block
//  - 'rosso.groovy' need to be executed first so the cluster admin.conf file is available
// Author: ejacjim
//*************************************************************************************************

import groovy.json.JsonSlurperClassic


// Return a list which elements are maps with the information of installed helm releases
// Empty list if nothing installed or error
def listAllReleases(admin_conf) {
    common.log('debug', "listAllReleases::begin")
    def allreleases = []
    def helm_cmd = common.getListFromSh("'ls' -t /usr/local/bin/helm*")[0]
    if (admin_conf?:"" != "") {
        helm_cmd += " --kubeconfig ${admin_conf}"
    }

    String output = ""
    try {
        output = sh(returnStdout:true, script:"${helm_cmd} list --all-namespaces -o json")
    } catch (error) {
        common.log('error', "Kubernetes cluster is unreachable: ${error}")
    }

    if (output?:"" != "") {
        JsonSlurperClassic jsonSlurper = new JsonSlurperClassic()
        try {
            allreleases = jsonSlurper.parseText(output)
        } catch (error) {
            common.log('error', "Unable to parse Helm output: ${error}")
        }
    }
    common.log('debug', "listAllReleases::end(${allreleases})")
    return allreleases
}


// Return a list with installed "eric-ccsm" and "eric-ccsm-service-mesh" chart names and version
// (null, null) if not installed or error
def getCcsmVersions(admin_conf) {
    common.log('debug',"getCcsmVersions::begin")
    def all_rel = listAllReleases(admin_conf)
    def ccsm_ver = null
    def smesh_ver = null

    // We deploy 3 packages
    if (all_rel.toString().contains('eric-ccsm-3')) {
        for (release in all_rel) {
            switch(release['name']) {
               case 'eric-ccsm-3':            ccsm_ver  = release['chart']; break
               case 'eric-ccsm-2':            smesh_ver = release['chart']; break
            }
            if (ccsm_ver != null && smesh_ver != null) {
               break
            }
        }
    } else { // We deploy 2 packages
        for (release in all_rel) {
            switch(release['name']) {
               case 'eric-ccsm-2':            ccsm_ver  = release['chart']; break
               case 'eric-ccsm-1':            smesh_ver = release['chart']; break
            }
            if (ccsm_ver != null && smesh_ver != null) {
               break
            }
        }
    }
    common.log('debug',"getCcsmVersions::end")
    return [ccsm_ver, smesh_ver]
}

