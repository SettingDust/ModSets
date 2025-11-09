package settingdust.mod_sets.data

sealed interface SavingData {
    companion object : SavingData {
        private val instances =
            SavingData::class.sealedSubclasses
                .mapNotNull { klass -> klass.objectInstance?.takeIf { it != Companion } }
                .filter { it !== Companion }

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
