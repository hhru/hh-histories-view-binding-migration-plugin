package ru.hh.android.synthetic_plugin.extensions

import org.jetbrains.kotlin.psi.KtFile

fun KtFile.haveSyntheticImports(): Boolean {
    return importDirectives.any { it.importPath?.pathStr.isKotlinSynthetic() }
}