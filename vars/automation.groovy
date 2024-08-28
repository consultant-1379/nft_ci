// ********************************************************
//  Common job methods: reporting, workspace handling, ...
//  Author: etobdia
// ********************************************************


/**
 * Patch software before running JCAT
 * Parameters:
 *   CCSM_PACKAGE
 *   SMESH_PACKAGE
 */
def patch_software(config) {
    common.log('begin', "patch_software::begin(CCSM_PACKAGE=${config.CCSM_PACKAGE}, " +
               "SMESH_PACKAGE=${config.SMESH_PACKAGE})")
    def deployment_output = "${WORKSPACE}/${config.project_dir}/patchedDeploymentConfig.yaml"
    def deployment_cfg_path =
        "${config.project_dir}/ccsm-nonfunctional-testcases/src/main/resources/deployConfigurationFiles/deployments"
    if (!fileExists(deployment_cfg_path)) {
        deployment_cfg_path = "${config.project_dir}/src/main/resources/deployConfigurationFiles/deployments"
    }

    def deployment_config = "${deployment_cfg_path}/deployments_config_ccsm.yaml"
    def addons_pyyaml_module_u18 = 'python/3.6-addons-pyyaml-5.1.2'
    def addons_pyyaml_module_u20 = 'python/3.6-addons-pyyaml-5.4.1'
    def ubuntu_version = sh(returnStdout:true, script:" lsb_release -a ")
    println "ubuntu_version=${ubuntu_version}"
    def py_script_aux = ""
    if (ubuntu_version && ubuntu_version.contains("20"))
       py_script_aux = "${addons_pyyaml_module_u20}"
    else
       py_script_aux = "${addons_pyyaml_module_u18}"

    patch_script = common.copyGlobalLibraryScript('tools/patchSoftwareConfig3')
    patch_script = "module add python/3.6.6 ${py_script_aux}; ${patch_script}"
    sh """
        . /etc/home/bashrc
        'cp' ${deployment_config} ${deployment_output}
        ${patch_script} --ccsm-package ${config.CCSM_PACKAGE} \
            --service-mesh-package ${config.SMESH_PACKAGE} --nft-branch ${config.NFT_BRANCH} ${deployment_output}
    """
    config.deployment_data = sh(returnStdout:true, script:"cat ${deployment_output}").trim()

    common.log('debug', "patch_software::end(CCSM_PACKAGE=${config.CCSM_PACKAGE}, SMESH_PACKAGE=${config.SMESH_PACKAGE}, deployment_data=${config.deployment_data})")
    return config
}


/**
 * Modify CCSM deployment files to use provided package versions
 */
def patchVersions(cfg) {
    common.log('debug', "patchVersions::begin(${cfg.CCSM_PACKAGE}, ${cfg.SMESH_PACKAGE})")
    if ((cfg.CCSM_PACKAGE != null) && (cfg.SMESH_PACKAGE != null)) {
        cfg = patch_software(cfg)

        // Get NFT branch
        def yaml = readYaml text:cfg.deployment_data
        if (yaml.toString().contains('eric-ccsm-3')){
            common.log('debug', "Installation with 3 packages")
            yaml.each {
                if (it.releaseName == "nft") {
                    cfg.NFT_BRANCH = it.version
                    common.log('debug', "NFT brancn is {$it.version}")
                }
                if (it.releaseName == "eric-ccsm-3") {
                    cfg.CCSM_VERSION = it.version
                }
                if (it.releaseName == "eric-ccsm-2") {
                    cfg.SMESH_VERSION = it.version
               }
            }
        }else{
            common.log('debug', "Installation with 2 packages")
            yaml.each {
                if (it.releaseName == "nft") {
                    cfg.NFT_BRANCH = it.version
                    common.log('debug', "NFT brancn is {$it.version}")
                }
                if (it.releaseName == "eric-ccsm-2") {
                    cfg.CCSM_VERSION = it.version
                }
                if (it.releaseName == "eric-ccsm-1") {
                    cfg.SMESH_VERSION = it.version
               }
            }
        }
    }
    common.log('debug', "patchVersions::end(${cfg.CCSM_VERSION},${cfg.SMESH_VERSION})")
    return cfg
}


/**
 * Build properties files and put it as JOB output
 * Parameters:
 */
def build_properties(config) {
    common.log('debug', "build_properties::begin(${WORKSPACE})")

    def artif_data = ""
    def resul_data = ""
    def summary    = []
    def list_tmp1  = findFiles(glob: "${config.project_dir}/test-output/custom-testng-results.xml")
    def list_tmp2  = findFiles(glob: '**/jenkins.xml')

    // TestNG
    try {
        list_tmp1.each {
            common.log('info', "Found TestNG report: ${it.path}")
            summary += common.parseTestng(it.path)
        }
    } catch (exc) {}

    // JUnit
    try {
        list_tmp2.each {
            common.log('info', "Found JUnit report: ${it.path}")
            summary += common.parseJunit(it.path)
        }
    } catch (exc) {}

    results_data = common.generateResultProperties(config, summary)

    // Disable 'results.properties' until the jobs are integrated in a Spinnaker pipeline
    //writeFile(file: 'results.properties',  text:results_data)
    //archiveArtifacts allowEmptyArchive: true, artifacts: 'results.properties',  onlyIfSuccessful: true
    common.log('debug', "build_properties::end")
}


/**
 * Report JUnit and TestNG results
 */
def report_results(config) {
    common.log('debug', "report_results::begin(${config.testsuite}, ${config.run_metrics})")

    step([$class: 'Publisher', unstableFails: 1, unstableSkips: 1,
          reportFilenamePattern: "**/${config.project_dir}/test-output/custom-testng-results.xml"])
    junit allowEmptyResults:true, testResults: "**/jenkins.xml"
    sh returnStatus:true, script:"cp ${config.project_dir}/test-output/custom-testng-results.xml ${WORKSPACE}/JCAT_LOGS/"

    if (!nft5g.isVerifyMode()) {
        if (fileExists("${WORKSPACE}/JCAT_LOGS")) {
            def yang_instance_data = common.getListFromSh("find ${WORKSPACE}/JCAT_LOGS -name yang_instance_data")
            if (yang_instance_data.size() == 1) {
                common.log('debug', "yang instance data: ${yang_instance_data}")
                def yang_path = yang_instance_data.sort()[-1];
                sh(returnStdout:true, script:"ls -1 ${yang_path}").split('\n').each { sub_path->
                    if (sh(returnStatus:true, script:"tar -czf ${sub_path}.tgz ${yang_path}/*") == 0) {
                        withCredentials([usernamePassword(
                                credentialsId: 'userpwd-adp',
                                usernameVariable: 'user',
                                passwordVariable: 'pwd')
                        ]) {
                            sh(returnStatus:true, script:"curl -u${user}:${pwd} -T ${sub_path}.tgz ${common.ARM_CCSM_NFT_LOGS_REPO}/ossmim/")
                        }
                    }
                }
            }

            def log_name = JOB_NAME.replace('CCSM_NFT_', '').toLowerCase()
            def date_build = new Date().format("yyyyMMdd").toString();
            def logs_package = "${log_name}_logs_${BUILD_NUMBER}_${date_build}.tgz"
            if (sh(returnStatus:true, script:"tar -czf ${logs_package} ${WORKSPACE}/JCAT_LOGS/*") == 0) {
                withCredentials([usernamePassword(
                    credentialsId: 'userpwd-adp',
                    usernameVariable: 'user',
                    passwordVariable: 'pwd')
                ]) {
                    sh(returnStatus:true, script:"curl -u${user}:${pwd} -T ${logs_package} ${common.ARM_CCSM_NFT_LOGS_REPO}")
                }
            }
            // JCAT_LOGS link in Jenkins build
            addBadge(icon:"db_out.gif", text:"JCAT_LOGS", link:"${common.ARM_CCSM_NFT_LOGS_REPO}/${logs_package}")
            //createSummary shows the information in the build page but it has a bug preventing the icon to be shown properly
            //createSummary(icon:"db_out.gif", text:"<a href=\"${common.ARM_CCSM_NFT_LOGS_REPO}/${logs_package}\">JCAT_LOGS</a>")
        }
    }

    // Publish metrics if generated
    def date = new Date().format("yyyyMM").toString()
    def metric_files = common.getListFromSh("ls -1t ${WORKSPACE}/JCAT_LOGS/${date}/${date}*/metrics_*.jsonz")
    if (metric_files.size() > 0) {
        warnError("Metrics could not be published") {
            def metric_file = metric_files[0]
            sh "metric_plot ${metric_file} --skip-display"
            common.getListFromSh("ls -1t *.html").each {
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: false,
                    reportDir: WORKSPACE,
                    reportFiles: it,
                    reportName: it.replace('.html', ''),
                    reportTitles: ''])
            }
        }
    }
    common.getListFromSh("ls -1 ${WORKSPACE}/*.gif").each { gif->
        archiveArtifacts allowEmptyArchive: true, artifacts:gif
    }

    common.log('debug', "report_results::end")
}


/**
 * Set a IP tunnel for IBD environments
 */
def tunnel_up(config) {
    common.log('debug', "tunnel_up::begin(${config.ENVIRONMENT_NAME}, ${config.RSA_FILE}, ${config.ECCD_USER}, ${USER}, ${config.DIRECTOR_OAM_VIP})")
    common.log('info', "tunnel_up:: Get MASTER_IP")
    def masterNode = sh returnStdout:true, script:"""ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
        -i ${config.RSA_FILE} ${config.ECCD_USER}@${config.DIRECTOR_OAM_VIP} \
        "kubectl get nodes -A -o jsonpath='{.items[0].status.addresses[0].address}'"
    """
    if (masterNode.contains(':')) {
        masterNode = "[${masterNode}]"
    }
    common.log('info', "tunnel_up:: set tunnel")
    sh """ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -M -S ci_tunnel -fnNT \
          -l ${config.ECCD_USER} -i ${config.RSA_FILE} -L:6443:${masterNode}:6443 ${config.DIRECTOR_OAM_VIP}
    """
    common.log('debug', "tunnel_up::end()")
}


/**
 * Unset a previously set IP tunnel for IBD environments
 */
def tunnel_down(config) {
    common.log('info', "tunnel_down:: remove tunnel")
    sh """ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -S ci_tunnel -O exit \
          -l ${config.ECCD_USER} ${config.DIRECTOR_OAM_VIP}
    """
    common.log('debug', "tunnel_down::end()")
}


/**
 * Get Cluster/TG data from Rosetta
 */
def rosettaConfig(cfg_in) {
    common.log('debug', "rosettaConfig::begin(${cfg_in['BUILD_NODE']})")
    def cfg_out = [:]
    cfg_in.keySet().each { key->
        cfg_out[key] = cfg_in[key]
    }

    // Get the actual CLOUD name before accesing to Rosetta
    cfg_out['CLOUD'] = common.getEnvName(cfg_out['BUILD_NODE'])
    if (cfg_out['CLOUD'] == null || cfg_out['CLOUD'] == "") {
        common.msgError("Unable to get Environment info (check node availability/permissions in Rosso and output of 'get_env_info')")
    }
    cfg_out = rosso.fill_auto_values(cfg_out)
    if (cfg_out['DTG_IP'] != cfg_out['BUILD_NODE']) {
        common.log('debug', "BUILD_NODE(Jenkins)=${cfg_out['BUILD_NODE']} TG(Rosetta)=${cfg_out['DTG_IP']}")
    }

    common.log('debug', "rosettaConfig::end(${cfg_out['CLOUD']})")
    return cfg_out
}
