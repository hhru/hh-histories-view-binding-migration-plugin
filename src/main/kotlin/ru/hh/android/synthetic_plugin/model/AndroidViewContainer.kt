package ru.hh.android.synthetic_plugin.model

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiReference
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.containers.isNullOrEmpty
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import ru.hh.android.synthetic_plugin.extensions.getShortBindingName
import ru.hh.android.synthetic_plugin.utils.Const

/**
 * Model for holding information about XML view.
 */
sealed class AndroidViewContainer {

    abstract val xml: XmlAttributeValue
    abstract val isNeedBindingPrefix: Boolean

    data class PsiRef(
        val ref: PsiReference,
        override val xml: XmlAttributeValue,
        override val isNeedBindingPrefix: Boolean,
    ) : AndroidViewContainer()

    data class KtRefExp(
        val ref: KtReferenceExpression,
        override val xml: XmlAttributeValue,
        override val isNeedBindingPrefix: Boolean,
    ) : AndroidViewContainer()


    /**
     * [viewBindingProperties] support for multiple bindings naming in single .kt file
     */
    fun getElementName(
        viewBindingProperties: List<KtImportDirective>,
        isMultipleBindingInFile: Boolean,
    ): String {
        val idCamelCase = xml.text
            .removeSurrounding("\"")
            .removePrefix(Const.ANDROID_VIEW_ID)
            .toCamelCase()
            .decapitalize()

        val prefix = if (!isMultipleBindingInFile) {
            when {
                isNeedBindingPrefix -> "binding."
                else -> ""
            }
        } else {
            val newPrefix = viewBindingProperties.first {
                it.importPath?.pathStr?.contains(idCamelCase) == true
            }.getShortBindingName()

            when {
                isNeedBindingPrefix -> "${newPrefix}Binding."
                else -> ""
            }
        }

        return "$prefix$idCamelCase"
    }
}
