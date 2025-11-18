package settingdust.mod_sets.data

import settingdust.mod_sets.util.ServiceLoaderUtil

interface SavingData {
    companion object : SavingData {
        private val instances by lazy { ServiceLoaderUtil.findServices<SavingData>() }

        override fun reload() {
            for (data in instances) {
                data.reload()
            }
        }

        override fun save() {
            for (data in instances) {
                data.save()
            }
        }
    }

    fun reload()

    fun save()
}
