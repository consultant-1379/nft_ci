package com.ericsson

class NftParameters {

    def config = [:]  // Map (key, value)

    /**
     * Constructor
     */
    NftParameters(jenkinsfile_params, jenkinsjob_params=[:]) {
        env.DEBUG = ((jenkinsjob_params.DEBUG?:env.DEBUG?:'false').toBoolean() ||
                      common.getPipelineResource().debug.value).toString()
        config['DEBUG'] = env.DEBUG.toBoolean()

        // Add all Jenkins job parameters
        if (jenkinsjob_params.keySet().size() > 0) {
            jenkinsjob_params.keySet().each { key->
                config[key] = jenkinsjob_params[key]
            }
        }
        // Add all Jenkinsfile parameters (priority over job parameters)
        jenkinsfile_params.keySet().each { key->
            config[key] = jenkinsfile_params[key]
        }
    }

    /**
     * Set a single parameter
     */
    def setParameter(key, value) {
        config[key] = value
    }

    /**
     * Get a single parameter
     */
    def getParameter(key) {
        return config[key]
    }

    /**
     * If parameter does not exist in the config map add it with the provided default value
     * otherwise do nothing
     */
    def assureParameter(key, default_value) {
        if (!config.containsKey(key)) {
            config[key] = default_value
        }
    }

    /**
     * Merge the provided config map to the internal config map
     */
    def addConfig(config_in) {
        config_in.keySet().each { key->
            config[key] = config_in[key]
        }
    }

    /**
     * Get current config map
     */
    def getConfig() {
        return config
    }

}
