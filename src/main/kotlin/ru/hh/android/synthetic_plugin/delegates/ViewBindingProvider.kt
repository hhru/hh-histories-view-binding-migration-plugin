package ru.hh.android.synthetic_plugin.delegates

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.ImportPath
import ru.hh.android.synthetic_plugin.extensions.*
import ru.hh.android.synthetic_plugin.model.ProjectInfo

abstract class ViewBindingProvider(val projectInfo: ProjectInfo) {

    abstract fun processActivity(ktClass: KtClass)

    abstract fun processFragment(ktClass: KtClass)

    abstract fun processView(ktClass: KtClass)

    abstract fun processCell(ktClass: KtClass)

    /**
     * Return non-formatted list of import synthetic directories
     */
    val syntheticImportDirectives =
        projectInfo.file.importDirectives.filter { it.importPath?.pathStr.isKotlinSynthetic() }

    /**
     * Support for multiple binding in single .kt file
     */
    protected val importDirectives = syntheticImportDirectives.map { directive ->
        directive.toFormattedDirective().toFormattedBindingName()
    }.toSet()

    val hasMultipleBindingsInFile = importDirectives.size > 1

    private val bindingQualifiedClassNames = run {
        importDirectives.map { bindingClassName ->
            "${projectInfo.androidFacet?.getPackageName().orEmpty()}.databinding.$bindingClassName"
        }
    }

    /**
     * Try to add binding declarations after last companion object (if we have one)
     */
    fun tryToAddAfterCompanionObject(
        body: KtClassBody,
        vararg properties: KtProperty,
    ) {
        if (body.allCompanionObjects.isNotEmpty()) {
            val lastCompanionObject = body.allCompanionObjects.last()
            val nextPsiElement = findNextNonWhitespaceElement(objectDeclaration = lastCompanionObject)
            properties.forEach {
                lastCompanionObject.body?.addBefore(it, nextPsiElement)
            }
        } else {
            properties.forEach {
                body.addAfter(it, body.lBrace)
            }
        }
    }

    /**
     * Trick to add binding elements properly formatted
     */
    private fun findNextNonWhitespaceElement(
        objectDeclaration: KtObjectDeclaration? = null,
        expression: PsiElement? = null,
    ): PsiElement? {
        var nextPsiElement: PsiElement? = objectDeclaration?.nextSibling ?: expression?.nextSibling
        do {
            if (nextPsiElement is PsiWhiteSpace) {
                nextPsiElement = nextPsiElement.nextSibling
                continue
            }
            return nextPsiElement
        } while (true)
    }

    fun addImports(vararg imports: String) {
        projectInfo.file.importList?.let { importList ->
            imports.forEach { import ->
                val importPath = ImportPath.fromString(import)
                val importDirective = projectInfo.psiFactory.createImportDirective(importPath)
                importList.add(getNewLine())
                importList.add(importDirective)
            }
        }
    }

    /**
     * New line for imports because psiFactory.createNewLine()
     * generates leading whitespace
     */
    fun getNewLine(): PsiElement {
        val parserFacade = PsiParserFacade.SERVICE.getInstance(projectInfo.project)
        return parserFacade.createWhiteSpaceFromText("\n")
    }

    fun addViewBindingImports(ktClass: KtClass) {
        addImports(*bindingQualifiedClassNames.toTypedArray())
    }

    /**
     * Format code after changes
     */
    fun formatCode(ktClass: KtClass) {
        val codeStyleManager: CodeStyleManager = CodeStyleManager.getInstance(projectInfo.project)
        codeStyleManager.reformat(ktClass)
    }

    /**
     * Returns proper binding name for setContentView() in Activities
     * or returns default "binding" if no name was found or there is
     * only one import exists in file
     */
    fun getContentViewBindingForActivity(layoutName: String): String {
        return if (hasMultipleBindingsInFile) {
            syntheticImportDirectives.find { it.importPath?.pathStr?.contains(layoutName) == true }
                ?.toFormattedDirective()
                ?.toFormattedBindingName()
                ?.decapitalize() ?: "binding"
        } else {
            "binding"
        }
    }
}