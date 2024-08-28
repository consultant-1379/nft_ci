package com.ericsson.nftci


def config = [:]  // Map (key, value)

/**
 * Constructor
 */
NftParameters(jenkinsfile_params, jenkinsjob_params=[:]) {
    env.DEBUG = ((jenkinsjob_params.DEBUG?:env.DEBUG?:'false').toBoolean()).toString()
    this.config['DEBUG'] = env.DEBUG.toBoolean()

    // Add all Jenkins job parameters
    if (jenkinsjob_params.keySet().size() > 0) {
        jenkinsjob_params.keySet().each { key->
            this.config[key] = jenkinsjob_params[key]
        }
    }
    // Add all Jenkinsfile parameters (priority over job parameters)
    jenkinsfile_params.keySet().each { key->
        this.config[key] = jenkinsfile_params[key]
    }
}

/**
 * Set a single parameter
 */
def setParameter(key, value) {
    this.config[key] = value
}

/**
 * Get a single parameter
 */
def getParameter(key) {
    return this.config[key]
}

/**
 * If parameter does not exist in the config map add it with the provided default value
 * otherwise do nothing
 */
def assureParameter(key, default_value) {
    if (!this.config.containsKey(key)) {
        this.config[key] = default_value
    }
}

/**
 * Merge the provided config map to the internal config map
 */
def addConfig(config_in) {
    config_in.keySet().each { key->
        this.config[key] = config_in[key]
    }
}

/**
 * Get current config map
 */
def getConfig() {
    return this.config
}


return this
