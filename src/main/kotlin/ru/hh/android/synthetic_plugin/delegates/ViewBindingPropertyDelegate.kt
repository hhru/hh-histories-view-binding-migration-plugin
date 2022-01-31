package ru.hh.android.synthetic_plugin.delegates

import android.databinding.tool.ext.toCamelCase
import com.intellij.psi.PsiClass
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.kotlin.resolve.ImportPath
import ru.hh.android.synthetic_plugin.utils.ClassParentsFinder
import ru.hh.android.synthetic_plugin.utils.Const
import ru.hh.android.synthetic_plugin.util.getPackageName
import ru.hh.android.synthetic_plugin.util.isKotlinSynthetic

/**
 * TODO:
 *
 * - [ ] It would be nice to place generated property after companion objects inside Fragments
 * and Views (if we have one). Also we should add [newLine] before generated property declaration.
 *
 * - [ ] For Cells we should handle case when there is no `with(viewHolder.itemView)` block
 * (case with direct invocation of `viewHolder.itemView.view_id`)
 */
class ViewBindingPropertyDelegate(
    private val psiFactory: KtPsiFactory,
    private val file: KtFile,
    androidFacet: AndroidFacet?,
) {

    private companion object {
        const val ANDROID_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        const val ANDROID_VIEW_CLASS = "android.view.View"

        // Cells in hh.ru - wrapper for reducing boilerplate in delegates for RecyclerView
        const val HH_CELL_INTERFACE = "ru.hh.shared.core.ui.cells_framework.cells.interfaces.Cell"

        const val HH_IMPORT_BINDING_PLUGIN =
            "ru.hh.shared.core.ui.framework.fragment_plugin.common.viewbinding.viewBindingPlugin"

        const val HH_IMPORT_INFLATE_AND_BIND_FUN = "ru.hh.shared.core.ui.design_system.utils.widget.inflateAndBindView"

        const val HH_IMPORT_CELLS_GET_VIEW_BINDING_FUN = "ru.hh.shared.core.ui.cells_framework.cells.getViewBinding"
    }

    private val bindingClassName = run {
        val synthImport = file.importDirectives.first { it.importPath?.pathStr.isKotlinSynthetic() }
        val synthImportStr = synthImport.importPath?.pathStr.orEmpty()
            .removeSuffix(".*").removeSuffix(".view")
        synthImportStr
            .drop(synthImportStr.lastIndexOf('.') + 1)
            .toCamelCase()
            .capitalize()
            .plus("Binding")
    }

    private val bindingQualifiedClassName = run {
        val packageName = "${androidFacet?.getPackageName().orEmpty()}.databinding."
        "${packageName}${bindingClassName}"
    }

    fun addViewBindingProperty() {
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
                parents.isChildOf(ANDROID_FRAGMENT_CLASS) -> processFragment(ktClass)
                parents.isChildOf(ANDROID_VIEW_CLASS) -> processView(ktClass)
                parents.isChildOf(HH_CELL_INTERFACE) -> processCell(ktClass)
                else -> println("Can't add ViewBinding property to class: ${psiClass.qualifiedName}")
            }
            addImports(bindingQualifiedClassName)
        }
    }

    private fun processFragment(ktClass: KtClass) {
        val text =
            "private val binding by viewBindingPlugin($bindingClassName::bind)"
        val viewBindingDeclaration = psiFactory.createProperty(text)
        val body = ktClass.getOrCreateBody()

        // It would be nice to place generated property after companion objects inside Fragments
        // and Views (if we have one). Also we should add [newLine] before generated property declaration.
        body.addAfter(viewBindingDeclaration, body.lBrace)
        addImports(
            HH_IMPORT_BINDING_PLUGIN,
        )
    }

    private fun processView(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        val inflateViewExpression = body.anonymousInitializers.getOrNull(0)
            ?.body?.children?.firstOrNull { it.text.contains("inflateView") }
        inflateViewExpression?.delete()
        val text = "private val binding = inflateAndBindView($bindingClassName::inflate)"
        val viewBindingDeclaration = psiFactory.createProperty(text)

        // It would be nice to place generated property after companion objects inside Fragments
        // and Views (if we have one). Also we should add [newLine] before generated property declaration.
        body.addAfter(viewBindingDeclaration, body.lBrace)
        addImports(
            HH_IMPORT_INFLATE_AND_BIND_FUN,
        )
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
                importList.add(psiFactory.createNewLine())
                importList.add(importDirective)
            }
        }
    }
}
