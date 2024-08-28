/*
 * parameters:
 *   timeout
 *   testsuite
 */

def call(body) {
    def config_in = [:]  // User parameters in the Jenkinsfile
    def config    = [:]  // All parameters
    def resourceToLock = ""

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    stage("Initialize") {
        config_in.testsuite = "batch"
        config = nft5g.customizeConfig(params, config_in)
        resourceToLock = "hss_5g_exec_${config.BUILD_NODE}"
        if (currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause))
            resourceToLock = "hss_5g_${config.BUILD_NODE}"
    }

    lock(resourceToLock) {
        node(config.BUILD_NODE) {

            stage('Configure') {
                config = automation.rosettaConfig(config)
                nft5g.cleanUpWorkspace()
                nft5g.checkOutRepository(config)
                common.setBuildDescription(config)
                nft5g.getJarPackage(config)
            }

            stage("Run ${config.testsuite}") {
                timeout(time: config.timeout, unit: 'MINUTES') {
                    warnError("JCAT execution failed") {
                        nft5g.execute_java_command(config)
                    }
                }
            }

            stage('Build properties files') {
                automation.build_properties(config)
            }

            stage('Report results') {
                automation.report_results(config)
            }

            stage('Clean up workspace') {
                nft5g.cleanUpWorkspace()
            }
        }  // node
    }  // lock
}  // call
