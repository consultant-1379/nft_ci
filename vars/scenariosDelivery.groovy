/**
 * parameters:
 *    dtg_repository
 */
def call(body) {
    def config_in = [:]  // User parameters in the Jenkinsfile
    config_in['dtg_repository'] = "5G_automation/DTG-traffic-scenarios/udm"
    def config    = [:]  // All parameters

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    node("CCSM_NFT") {
        def lfiles = []
        def dtgRepoDir = "dtgRepo"
        def applyChanges = false
        def maven_version = "3.5.3"

        stage("Initialize") {
            if (env.GERRIT_PATCHSET_REVISION == null) {
                error "This job can not be build manually, a Gerrit event is required"
            }
            config = nft5g.customizeConfig(params, config_in)
            nft5g.checkOutRepoBranch(config.repository, config.NFT_BRANCH, config.credentials, config.COMMIT_ID)
            common.log('info', "env.GERRIT_PATCHSET_REVISION : ${env.GERRIT_PATCHSET_REVISION}")
            common.log('info', "env.GERRIT_PATCHSET_REVISION~1 : ${env.GERRIT_PATCHSET_REVISION}~1")
            lfiles = common.getListFromSh("git diff ${env.GERRIT_PATCHSET_REVISION}..${env.GERRIT_PATCHSET_REVISION}~1 --name-only")
            common.log('debug', "lfiles=${lfiles}")
        }

        stage("Clone DTG-scenarios repository") {
            def dtgRepoUrl = "https://${common.DEFAULT_GERRIT_SERVER}/a/${config_in.dtg_repository}"
            nft5g.checkOutRepoBranch(dtgRepoUrl, config.NFT_BRANCH, config.credentials, "", dtgRepoDir)
        }

        for (cfile in lfiles) {
            if (cfile.endsWith(".properties") || cfile.endsWith(".feature")) {
                applyChanges = true
                break
            }
        }
        if (applyChanges) {
            stage("Apply changes") {
                // Copy specific new/modified files from CCSM-NFT repo to DTG-traffic-scenarios repo
                def addFiles = []
                lfiles.each { cfile->
                    if (cfile.endsWith(".properties") || cfile.endsWith(".feature")) {
                        def destFile = cfile.replace('nft_automation/ccsm-nonfunctional-testcases/', 'dtg-traffic-scenarios-udm/')
                        def destDir  = common.dirname(destFile)
                        sh "mkdir -p ${dtgRepoDir}/${destDir} && cp -r ${cfile} ${dtgRepoDir}/${destFile}"
                        addFiles.add(destFile)
                    }
                }
                // Set project version in pom.xml
                def version = nft5g.getProjectVersion("nft_automation/pom.xml")
                nft5g.setProjectVersion("${dtgRepoDir}/dtg-traffic-scenarios-udm/pom.xml", version)

                // Push changes to DTG-traffic-scenarios repository
                withCredentials(
                    [usernamePassword(
                        credentialsId: config.credentials,
                        passwordVariable: 'cred_pass',
                        usernameVariable: 'cred_user')
                    ])
                {
                    def dtgRepoUrl = "https://${cred_user}:${cred_pass}@gerrit.ericsson.se/a/${config_in.dtg_repository}"
                    sh "cd ${dtgRepoDir} && git config --global user.name ${env.USER} && git config --global user.email 1016106_jose-carlos.fernandez@ericsson.com"
                    sh "cd ${dtgRepoDir} && git remote set-url origin --push ${dtgRepoUrl}"
                    sh returnStatus:true, script:"cd ${dtgRepoDir} && git add dtg-traffic-scenarios-udm/pom.xml"
                    sh "cd ${dtgRepoDir} && git add ${addFiles.join(' ')}"
                    sh "cd ${dtgRepoDir} && git commit -m 'Create ${version} version'"
                    sh "cd ${dtgRepoDir} && git push -u origin HEAD:master"
                }
            }

            stage("Deploy JAR") {
                // Take settings.xml from CCSM-NFT to use the credentials needed to push to ARM
                // Deploy DTG-traffic-scenarios JAR
                sh "/app/modules/0/bin/modulecmd bash add maven/${maven_version}"
                sh "cd ${dtgRepoDir}/dtg-traffic-scenarios-udm && mvn --settings=$WORKSPACE/nft_automation/ccsm-nonfunctional-testcases/src/main/resources/settings.xml clean install deploy"
            }
        }
    }

}
