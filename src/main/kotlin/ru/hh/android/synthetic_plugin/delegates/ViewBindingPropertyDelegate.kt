package ru.hh.android.synthetic_plugin.delegates

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import ru.hh.android.synthetic_plugin.extensions.*
import ru.hh.android.synthetic_plugin.utils.ClassParentsFinder
import ru.hh.android.synthetic_plugin.utils.Const
import ru.hh.android.synthetic_plugin.utils.Const.ON_DESTROY_FUNC_DECLARATION


/**
 * TODO:
 *
 * - [ ] For Cells we should handle case when there is no `with(viewHolder.itemView)` block
 * (case with direct invocation of `viewHolder.itemView.view_id`)
 */
class ViewBindingPropertyDelegate(
    private val psiFactory: KtPsiFactory,
    private val file: KtFile,
    private val androidFacet: AndroidFacet?,
    private val isUsingViewBindingPropertyDelegate: Boolean,
    private val project: Project,
) {

    private companion object {
        const val ANDROID_ACTIVITY_CLASS = "android.app.Activity"
        const val ANDROID_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        const val ANDROID_VIEW_CLASS = "android.view.View"

        // Cells in hh.ru - wrapper for reducing boilerplate in delegates for RecyclerView
        const val HH_CELL_INTERFACE = "ru.hh.shared.core.ui.cells_framework.cells.interfaces.Cell"

        const val HH_IMPORT_BINDING_PLUGIN =
            "ru.hh.shared.core.ui.framework.fragment_plugin.common.viewbinding.viewBindingPlugin"

        const val HH_IMPORT_INFLATE_AND_BIND_FUN = "ru.hh.shared.core.ui.design_system.utils.widget.inflateAndBindView"

        const val HH_IMPORT_CELLS_GET_VIEW_BINDING_FUN = "ru.hh.shared.core.ui.cells_framework.cells.getViewBinding"
    }

    /**
     * Return non-formatted list of import synthetic directories
     */
    val syntheticImportDirectives = file.importDirectives.filter { it.importPath?.pathStr.isKotlinSynthetic() }

    /**
     * Support for multiple binding in single .kt file
     */
    private val importDirectives = syntheticImportDirectives.map { it.toFormattedDirective().toFormattedBindingName() }.toSet()

    val hasMultipleBindingsInFile = importDirectives.size > 1

    /**
     * Left for single case - HH cell processing
     */
    private val bindingClassName = importDirectives.firstOrNull()

    private val bindingQualifiedClassNames = run {
        importDirectives.map { bindingClassName ->
            "${androidFacet?.getPackageName().orEmpty()}.databinding.$bindingClassName"
        }
    }

    fun addViewBindingProperties() {
        // `as Array<PsiClass` is necessary because of MISSING_DEPENDENCY_CLASS error from Kotlin Gradle plugin
        // https://youtrack.jetbrains.com/issue/KTIJ-19485
        // https://youtrack.jetbrains.com/issue/KTIJ-10861
        val classes = (file.getClasses() as Array<PsiClass>).mapNotNull { psiClass ->
            val ktClass = ((psiClass as? KtLightElement<*, *>)?.kotlinOrigin as? KtClass)
            if (ktClass == null) {
                null
            } else {
                psiClass to ktClass
            }
        }
        classes.forEach { (psiClass, ktClass) ->
            val parents = ClassParentsFinder(psiClass)
            when {
                parents.isChildOf(ANDROID_ACTIVITY_CLASS) -> processActivity(ktClass)
                parents.isChildOf(ANDROID_FRAGMENT_CLASS) -> processFragment(ktClass)
                parents.isChildOf(ANDROID_VIEW_CLASS) -> processView(ktClass)
                parents.isChildOf(HH_CELL_INTERFACE) -> processCell(ktClass)
                else -> println("Can't add ViewBinding property to class: ${psiClass.qualifiedName}")
            }
            addImports(*bindingQualifiedClassNames.toTypedArray())
            formatCode(ktClass)
        }
    }

    private fun processActivity(ktClass: KtClass) {
        if (isUsingViewBindingPropertyDelegate) {
            val body = ktClass.getOrCreateBody()
            importDirectives.forEach { bindingClassName ->
                val text = bindingClassName.toDelegatePropertyFormat(hasMultipleBindingsInFile)
                val viewBindingDeclaration = psiFactory.createProperty(text)

                tryToAddAfterCompanionObject(body, viewBindingDeclaration)

                addImports(
                    HH_IMPORT_BINDING_PLUGIN,
                )
            }
        } else {
            val body = ktClass.getOrCreateBody()
            importDirectives.forEach { bindingClassName ->
                val text = bindingClassName.toActivityPropertyFormat(hasMultipleBindingsInFile)
                val viewBindingDeclaration = psiFactory.createProperty(text)

                tryToAddAfterCompanionObject(body, viewBindingDeclaration)
            }
        }
    }

    private fun processFragment(ktClass: KtClass) {
        if (isUsingViewBindingPropertyDelegate) {
            val body = ktClass.getOrCreateBody()
            importDirectives.forEach { bindingClassName ->
                val text = bindingClassName.toDelegatePropertyFormat(hasMultipleBindingsInFile)
                val viewBindingDeclaration = psiFactory.createProperty(text)

                tryToAddAfterCompanionObject(body, viewBindingDeclaration)

                addImports(
                    HH_IMPORT_BINDING_PLUGIN,
                )
            }
        } else {
            val body = ktClass.getOrCreateBody()
            importDirectives.forEach { bindingClassName ->
                val mutablePropertyText = bindingClassName.toMutablePropertyFormat(hasMultipleBindingsInFile)
                val immutablePropertyText = bindingClassName.toImmutablePropertyFormat(hasMultipleBindingsInFile)
                val mutableViewBinding = psiFactory.createProperty(mutablePropertyText)
                val immutableViewBinding = psiFactory.createProperty(immutablePropertyText)

                tryToAddAfterCompanionObject(body, mutableViewBinding, immutableViewBinding)

                addBindingInitializationForFragment(body, bindingClassName)
                addBindingDisposingForFragment(body, bindingClassName)
            }
        }
    }

    private fun processView(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        val inflateViewExpression = body.anonymousInitializers.getOrNull(0)
            ?.body?.children?.firstOrNull { it.text.contains("inflateView") }
        inflateViewExpression?.delete()

        if (isUsingViewBindingPropertyDelegate) {
            importDirectives.forEach { bindingClassName ->
                val text = bindingClassName.toViewDelegatePropertyFormat(hasMultipleBindingsInFile)
                val viewBindingDeclaration = psiFactory.createProperty(text)

                tryToAddAfterCompanionObject(body, viewBindingDeclaration)

                addImports(
                    HH_IMPORT_INFLATE_AND_BIND_FUN,
                )
            }
        } else {
            // TODO("Need to do manually initialization or use ViewBindingPropertyDelegate")
            importDirectives.forEach { bindingClassName ->
                val text = bindingClassName.toMutablePropertyFormat(hasMultipleBindingsInFile)
                val viewBindingDeclaration = psiFactory.createProperty(text)

                tryToAddAfterCompanionObject(body, viewBindingDeclaration)
            }
        }
    }

    private fun processCell(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        val bindFunc = body.functions.firstOrNull { it.name == "bind" }
        val withExpression = bindFunc
            ?.children?.firstOrNull { it.text.contains(Const.CELL_WITH_VIEW_HOLDER) }
        withExpression
            ?.children?.firstOrNull()?.children?.getOrNull(1)
            ?.children?.firstOrNull()?.let { withArg ->
                val newElement = psiFactory.createArgument("viewHolder.getViewBinding(${bindingClassName}::bind)")
                withArg.replace(newElement)
            }
        addImports(
            HH_IMPORT_CELLS_GET_VIEW_BINDING_FUN,
        )
    }

    private fun addImports(vararg imports: String) {
        file.importList?.let { importList ->
            imports.forEach { import ->
                val importPath = ImportPath.fromString(import)
                val importDirective = psiFactory.createImportDirective(importPath)
                importList.add(getNewLine())
                importList.add(importDirective)
            }
        }
    }

    /**
     * Try to add binding declarations after last companion object (if we have one)
     */
    private fun tryToAddAfterCompanionObject(
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

    private fun addBindingInitializationForFragment(
        body: KtClassBody,
        bindingClassName: String,
    ) {
        body.functions.find { it.name == "onCreateView" }?.let {
            val text = bindingClassName.toFragmentInitializationFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = psiFactory.createExpression(text)
            viewBindingDeclaration.add(getNewLine())
            it.addAfter(viewBindingDeclaration, it.bodyBlockExpression?.lBrace)
        }
    }

    private fun addBindingDisposingForFragment(
        body: KtClassBody,
        bindingClassName: String,
    ) {
        var onDestroyViewFunc = body.functions.find { it.name == "onDestroyView" }

        // Create onDestroyView() fun if we don't have
        if (onDestroyViewFunc == null) {
            val newOnDestroyViewFunc = psiFactory.createFunction(ON_DESTROY_FUNC_DECLARATION)
            body.addBefore(newOnDestroyViewFunc, body.rBrace)
        }
        onDestroyViewFunc = body.functions.find { it.name == "onDestroyView" }

        onDestroyViewFunc?.let {
            val text = bindingClassName.toFragmentDisposingFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = psiFactory.createExpression(text)
            viewBindingDeclaration.add(getNewLine())
            it.addAfter(viewBindingDeclaration, it.bodyBlockExpression?.lBrace)
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

    /**
     * Format code after changes
     */
    private fun formatCode(ktClass: KtClass) {
        val codeStyleManager: CodeStyleManager = CodeStyleManager.getInstance(project)
        codeStyleManager.reformat(ktClass)
    }

    /**
     * New line for imports because psiFactory.createNewLine()
     * generates leading whitespace
     */
    private fun getNewLine(): PsiElement {
        val parserFacade = PsiParserFacade.SERVICE.getInstance(project)
        return parserFacade.createWhiteSpaceFromText("\n")
    }
}
