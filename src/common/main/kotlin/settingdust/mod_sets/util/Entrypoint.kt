package settingdust.mod_sets.util

interface Entrypoint {
    companion object : Entrypoint {
        private val services by lazy { ServiceLoaderUtil.findServices<Entrypoint>(required = false) }

        override fun construct() {
            services.forEach { it.construct() }
        }

        override fun init() {
            services.forEach { it.init() }
        }

        override fun clientInit() {
            services.forEach { it.clientInit() }
        }
    }

    fun construct() {}

    fun init() {}

    fun clientInit() {}
}