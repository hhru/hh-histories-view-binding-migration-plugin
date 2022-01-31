package ru.hh.android.synthetic_plugin.visitor

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtVisitor
import ru.hh.android.synthetic_plugin.extensions.isKotlinSynthetic

class SyntheticsImportsVisitor : KtVisitor<Any, Any>() {

    private val result = mutableListOf<KtImportDirective>()

    fun getResult(): List<KtImportDirective> = result.toList()

    override fun visitKtFile(file: KtFile, data: Any?): Any? {
        result.addAll(
            file.importDirectives.filter { it.importPath?.pathStr.isKotlinSynthetic() }
        )
        return null
    }
}
