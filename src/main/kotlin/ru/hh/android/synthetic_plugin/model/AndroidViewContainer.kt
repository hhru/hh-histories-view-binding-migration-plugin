package ru.hh.android.synthetic_plugin.model

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiReference
import com.intellij.psi.xml.XmlAttributeValue
import org.jetbrains.kotlin.psi.KtReferenceExpression
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


    fun getElementName(): String {
        val idCamelCase = xml.text
            .removeSurrounding("\"")
            .removePrefix(Const.ANDROID_VIEW_ID)
            .toCamelCase()
            .decapitalize()

        val prefix = when {
            isNeedBindingPrefix -> "binding."
            else -> ""
        }
        return "$prefix$idCamelCase"
    }
}
