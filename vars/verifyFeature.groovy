/**
 * parameters:
 *    timeout
 */

def call(body) {
    def config_in = [:]  // User parameters in the Jenkinsfile
    config_in['project_name'] = "HSS/CCSM/nft"
    config_in['repository']   = config_in['project_name']
    config_in['credentials']  = "userpwd-adp"
    def config    = [:]  // All parameters

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    node("CCSM_NFT") {
        def lfiles = []
        def applyChanges = false

        stage("Configure") {
            if (env.GERRIT_PATCHSET_REVISION == null) {
                error "This job can not be build manually, a Gerrit event is required"
            }
            config = nft5g.customizeConfig_verify(params, config_in)
            nft5g.checkOutRepository(config)
            def lfiles_all = common.getListFromSh("git diff-tree --name-status --no-commit-id -r HEAD")
            lfiles_all.each { cfile->
                if ((cfile.startsWith("A") || cfile.startsWith("M")) && cfile.endsWith(".feature")) {
                    lfiles.add(cfile.substring(1).trim())
                }
            }
            common.log('info', "lfiles=${lfiles}")
        }

        if (lfiles.size() > 0) {
            stage("Verify changes") {
                def verify_script = common.copyGlobalLibraryScript('tools/feature_verifier.py')
                lfiles.each { cfile->
                    sh "${verify_script} ${cfile}"
                }
            }
        }

    }  // node
}  // call
