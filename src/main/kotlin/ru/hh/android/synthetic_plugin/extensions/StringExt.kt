package ru.hh.android.synthetic_plugin.extensions

import ru.hh.android.synthetic_plugin.utils.Const

fun String?.isKotlinSynthetic(): Boolean {
    return this?.startsWith(Const.KOTLINX_SYNTHETIC) == true
}