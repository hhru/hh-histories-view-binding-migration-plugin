package ru.hh.android.synthetic_plugin.extensions

import org.jetbrains.kotlin.psi.KtImportDirective
import ru.hh.android.synthetic_plugin.utils.Const.CONST_SYNTHETIC_IMPORT_LENGTH

/**
 * Returns in format "layout_name.viewName"
 */
fun KtImportDirective.toFormattedDirective(): String {
    return this.importPath?.pathStr
        .orEmpty()
        .removeSuffix(".*")
        .removeSuffix(".view")
        .drop(CONST_SYNTHETIC_IMPORT_LENGTH)
}

/**
 * Returns in format "layoutName"
 */
fun KtImportDirective.getShortBindingName(): String {
    return this.toFormattedDirective().toShortFormattedBindingName()
}