/*
 * JAR generation+deploy(ARM) after Gerrit change-merged event
 */
import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate


def call(body) {
    def config_in = [:]  // Jenkinsfile parameters
    def config    = [:]  // All parameters

    // Execute body
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate        = config_in
    body()

    List<ContainerTemplate> containers = new ArrayList<ContainerTemplate>()
    stage('Initialize') {
        // Adding all Jenkins build parameters
        config_in.keySet().each { key->
            config[key] = config_in[key]
        }
        if (params.keySet().size() > 0) {
            params.keySet().each { key->
                config[key] = params[key]
            }
        }
        // Adding all Jenkinsfile parameters (they have priority over build parameters)
        config.label = "${JOB_NAME}${BUILD_NUMBER}"
        config.project_dir = common.SOURCE_ROOT_DIR
        config.settings_xml = common.getPipelineResource().SETTINGS_XML
        config.repository = common.formatGerritUrl(config.project_name)
        config.credentials = 'userpwd-adp'
        config.cloud = 'kubernetes'
        config.namespace = 'udm-5gcicd'
        config.NFT_BRANCH = config.GERRIT_BRANCH

        // Change default JNLP image to 'alpine'
        containers.add(
            containerTemplate(
                name:            'jnlp',
                image:           'armdocker.rnd.ericsson.se/proj-5g-cicd-release/jenkins/inbound-agent:4.11-1-jdk11-eric-certs',
                alwaysPullImage: false,
                args:            '${computer.jnlpmac} ${computer.name}'
            )
        )

        // Maven steps
        containers.add(
            containerTemplate(
                name:            'deploy',
                image:           'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-java11mvnbuilder:latest',
                alwaysPullImage: false,
                ttyEnabled:      true,
                command:         'cat'
            )
        )
    }

    podTemplate(
        cloud:               config.cloud,
        namespace:           config.namespace,
        label:               config.label,
        containers:          containers,
        imagePullSecrets:    [config.credentials])
    {
        node(config.label) {
            stage("Checkout ${config.project_name}") {
                nft5g.checkOutRepository(config)
            }

            stage('Deploy jar package') {
                container('deploy') {
                    timeout(time: (config.TIMEOUT?:'20').toInteger(), unit: 'MINUTES') {
                        sh """cd ${config.project_dir}
                        mvn --settings ${config.settings_xml} deploy
                        """
                    }
                }
            }
        }
    }
}
