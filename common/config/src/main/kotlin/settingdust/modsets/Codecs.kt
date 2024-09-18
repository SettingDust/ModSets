package settingdust.modsets

import com.mojang.serialization.Codec

fun <T> Codec<T>.setOf() = listOf().xmap({ it.toMutableSet() }, { it.toList() })!!
