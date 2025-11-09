package settingdust.mod_sets.data

sealed interface SavingData {
    fun reload()

    fun save()
}
