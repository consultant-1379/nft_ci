//*************************************************************************************************
// Rosso access functions
// Author: etobdia
//*************************************************************************************************
import groovy.json.*


def rosetta_query(String query, boolean raw=false) {
    common.log('debug', "rosetta_query::begin(${query}, ${raw})")
    def app_form = (raw) ? '' : '?format=json'
    def result   = httpRequest httpMode: 'GET',
        url: "https://rosso.gic.ericsson.se/api/1.0/${query}${app_form}",
        customHeaders:[
            [name:'Authorization', value:"Token d64229c7d4c0f07ce14b30aa4c68a8b9a4786dbb"],
            [name:'Connection', value:"close"]
        ]
    if (raw) {
        result = result.content
    } else {
        JsonSlurperClassic json = new JsonSlurperClassic()
        try {
            result = json.parseText(result.content)
        } catch (error) {
            echo "Failed to parse rosetta answer: ${error}"
            result = null
        }
    }

    common.log('debug', "rosetta_query::end()")
    return result
}


/**
 * Create a private file
 */
def writePrivateFile(filename, content) {
    if (fileExists(filename)) {
        sh "chmod 660 ${filename}"  // -rw-rw----
    }
    common.log('info', "Create file: ${filename}")
    writeFile(file:filename, text:content)
    sh "chmod 400 ${filename}"  // -r--------
}


/**
 * Create KubeConfig parameter
 */
String get_kubeconf_from_node(nodeName, isIbd, envName) {
    common.log('debug',"get_kubeconf_from_node::begin(${nodeName})")
    kubeConfig = ""
    kubeConfFileName = "${WORKSPACE}/${envName}_admin.conf"
    kubeOrig = rosetta_query("eccds/${nodeName}/download_config_file/", true)
    if (kubeOrig != null) {
        common.log('debug',"get_kubeconf_from_node::kubeOrig exists, IBD=${isIbd}")
        if (isIbd == true) {
            def reobj = kubeOrig =~ /server: (.*):/

            // Update server in KUBE_CONFIG
            if (reobj.matches()) {
                def serverInfo = reobj.group(1)
                def oldServer = "${serverInfo}:443"
                def newServer = "${serverInfo}:6443"
                common.log('debug',"get_kubeconf_from_node::Update KUBECONFIG ${oldServer} --> ${newServer}")
                kubeConfig = kubeOrig.replace(oldServer, newServer)
            } else {
                kubeConfig = kubeOrig
            }
        } else {
            kubeConfig = kubeOrig
        }
        writePrivateFile(kubeConfFileName, kubeConfig)
    }
    common.log('debug',"get_kubeconf_from_node::end")
    return kubeConfFileName
}


/**
 * Look for and set RSA_key from credentials of type eccd node
 */
def set_rsakey_from_eccd_node_credentials(node, result) {
    common.log('debug', "set_rsakey_from_eccd_node_credentials::begin(${result['ENVIRONMENT_NAME']})")
    def rsa_file = "${WORKSPACE}/id_rsa_${result['ENVIRONMENT_NAME']}"
    credential_array = [:]
    node.credentials.each { credential->
        common.log('debug', "set_rsakey_from_eccd_node_credentials:: credential.name=${credential.name}")
        def reobj = credential.name =~ /eccd[\-_]private|ssh\-key/
        if (reobj.matches()) {
            credential_array['username'] = credential.username
            credential_array['id'] = credential.id
        }
    }
    if (credential_array.size() > 0) {
        common.log('debug', "set_rsakey_from_eccd_node_credentials:: credential.username=${credential_array['username']}, credential.id=${credential_array['id']}")
        result['ECCD_USER'] = credential_array['username']
        common.log('debug', "set_rsakey_from_eccd_node_credentials::username=${credential_array['username']}")
        result['RSA_KEY'] = rosetta_query("credentials/${credential_array['id']}/download_ssh_key_file/", true)
        writePrivateFile(rsa_file, result['RSA_KEY'])
        result['RSA_FILE'] = rsa_file
    }
    if (result['ECCD_USER'] == null || result['ECCD_USER'] == "") {
        result['ECCD_USER'] = "eccd";
    }
    common.log('debug', "set_rsakey_from_eccd_node_credentials::end(${result['RSA_FILE']}, ${result['ECCD_USER']})")
    return result
}


def get_node_data(String node_name, String node_type) {
    common.log('debug', "get_node_data::begin(${node_name}, ${node_type})")
    def result = null
    try {
        result = rosetta_query("${node_type}/${node_name}/")
    } catch (error) {
        common.log('warning', "Failed to get node data: ${error}")
    }
    common.log('debug', "get_node_data::end()")
    return result
}


def get_environment_data(String environment_name) {
    common.log('debug', "get_environment_data::begin(${environment_name})")
    def result = rosetta_query("environments/${environment_name}/")
    common.log('debug', "get_environment_data::end()")
    return result
}


String get_related_environment(String node) {
    common.log('debug', "get_related_environment::begin(${node})")
    def result = rosetta_query("nodes/${node}/related_environments/")[0]
    common.log('debug', "get_related_environment::end()")
    return result
}


String get_tg_oam(node) {
    common.log('debug', "get_tg_oam::begin(${node})")
    for (network in node.networks) {
        if (network.name.startsWith("UDM 5G OAM")) {
            return network.hosts.values().pop()
        }
    }
    common.log('debug', "get_tg_oam::end(${node.name})")
    return node.name
}


def get_nodes(String environment_name) {
    common.log('debug', "get_nodes::begin(${environment_name})")
    def result = []
    def environment = get_environment_data(environment_name)
    for (node in environment.nodes) {
        result.add([node.name, node.classtype])
    }
    common.log('debug', "get_nodes::end(${result})")
    return result
}


def get_eccd_ibd_data(eccd, result) {
    common.log('debug', "get_eccd_ibd_data::begin()")
    def environment = get_related_environment(eccd.name)
    result.put('ENVIRONMENT_NAME', environment)

    def eccdRel = get_environment_data(environment)
    set_nodes_data(eccdRel, result)

    common.log('debug', "get_eccd_ibd_data::end(${result['ENVIRONMENT_NAME']}, ${result['DTG_IP']}, " +
                        "${result['BUILD_NODE']}, ${result['IBD']}, ${result['KUBE_CONF']}, " +
                        "${result['ECCD_USER']}, ${result['RSA_FILE']})")
    return result
}


/**
 * Look for and set Virtual IPs from networks of type eccd node
 */
def set_vips_from_eccd_node_network(node, result) {
    node.networks.each { network->
        def netName = network.name
        if (netName.equals("DIRECTOR_OAM")) {
            result.put('DIRECTOR_OAM_VIP', network.vips.network_def_oam_vip)
            common.log('debug', "set_vips_from_eccd_node_network:DIRECTOR_OAM_VIP=${result.DIRECTOR_OAM_VIP}")
        } else if (netName == "WORKER_OAM") {
            result.put('WORKER_OAM', network.vips.worker_oam_vip)
            common.log('debug', "set_vips_from_eccd_node_network:WORKER_OAM_VIP=${result.WORKER_OAM}")
        } else if (netName == "WORKER_SIG") {
            result.put('SIG_VIP', network.vips.worker_sig_vip)
            common.log('debug', "set_vips_from_eccd_node_network:SIG_VIP=${result.SIG_VIP}")
        }
    }
    return result
}


def set_nodes_data(eccd, result) {
    common.log('debug', "set_nodes_data::eccd_name=${eccd['name']}")
    eccd["nodes"].each { node->
        common.log('debug', "set_nodes_data::node type=${node.classtype}")
        if (node.classtype.toLowerCase() == "eccd") {
            result.put('IBD', node["is_ibd"])
            result.put('KUBE_CONF', get_kubeconf_from_node(node.name, result.IBD, result.ENVIRONMENT_NAME))
            result = set_rsakey_from_eccd_node_credentials(node, result)
            result = set_vips_from_eccd_node_network(node, result)
        } else if (node.classtype.toLowerCase() == "tool") {
            common.log('debug', "set_nodes_data::infratype.name=${node.infratype.name}")
            if (node.infratype.name.startsWith("5g_tg_")) {
                result.put('DTG_IP', get_tg_oam(node))
                result.put('BUILD_NODE', node["name"])
            }
        }
    }
    common.log('debug', "set_nodes_data::end(dtg_ip=${result['DTG_IP']}, ibd=${result['IBD']}," +
                        "build_node=${result['BUILD_NODE']}, kube_config=${result['KUBE_CONF']})")
    return result
}


def get_eccd_vdc_data(eccd, result) {
    common.log('debug', "get_eccd_vdc_data::begin()")
    result.put('ENVIRONMENT_NAME', eccd.name)
    result = set_nodes_data(eccd, result)
    common.log('debug', "get_eccd_vdc_data::end()")
    return result
}


def get_eccd_data(eccd_name) {
    common.log('debug', "get_eccd_data::begin(${eccd_name})")
    def result = [:]
    // Try ECCD as IBD node
    eccd = get_node_data(eccd_name, "eccds")
    if (eccd != null) {
        if (eccd.infratype.name.startsWith("eccd_vdc")) {
            // Node is spread over the environment (VDC)
            eccd_name = get_related_environment(eccd_name)
        } else {
            // Node is an IBD
            result = get_eccd_ibd_data(eccd, result)
            result.put("ECCD_VERSION", get_eccd_version(eccd))
        }
    }
    if (result == [:]) {
        // Try ECCD as VDC environment
        eccd = get_environment_data(eccd_name)
        if (eccd != null) {
            result = get_eccd_vdc_data(eccd, result)
            result.put("ECCD_VERSION", get_eccd_version(eccd))
        } else {
            throw new Exception("Node ${eccd_name} is not a ECCD IBD/VDC node")
        }
    }

    common.log('debug', "get_eccd_data::end(${result['ENVIRONMENT_NAME']}, ${result['DTG_IP']}, " +
                        "${result['BUILD_NODE']}, ${result['IBD']}, ${result['KUBE_CONF']}, " +
                        "${result['ECCD_USER']}, ${result['RSA_FILE']})")
    return result
}


// Get ECCD installed software version
String get_eccd_version(eccd) {
    String eccdVersion = "Unknown"
    try {
        if (eccd.containsKey('eccd_version')) {
            eccdVersion = eccd.eccd_version
        } else {
            eccdVersion = eccd.baseline.software[0].version
        }
    } catch (error) {
        common.log('warning', "Failed to get ECCD version: ${error}")
    }
    return eccdVersion
}


// Check for "AUTO" values and replace by data from rosso
def fill_auto_values(config) {
    common.log('debug', "fill_auto_values::begin(${config})")

    // Get rosso config
    def rosso_config = get_eccd_data(config.CLOUD)

    // Copy all parameters in rosso that are not defined in input config
    rosso_config.keySet().each { key->
        if (config[key]?:"" == "" && rosso_config[key]?:"" != "") {
            config[key] = rosso_config[key]
        }
    }
    
    // Replace values with "AUTO"
    config.each { pair->
        if (pair.value == "AUTO") {
            if (rosso_config.containsKey(pair.key)) {
                config[pair.key] = rosso_config[pair.key]
            } else {
                common.log("debug", "fill_auto_values:: Unknown parameter '${pair.key}' to find in Rosso")
            }
        }
    }

    common.log('debug', "fill_auto_values::end(${rosso_config})")
    return config
}
