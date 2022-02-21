package ru.hh.android.synthetic_plugin.delegates

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.getOrCreateBody
import ru.hh.android.synthetic_plugin.extensions.toDelegatePropertyFormat
import ru.hh.android.synthetic_plugin.extensions.toViewDelegatePropertyFormat
import ru.hh.android.synthetic_plugin.model.ProjectInfo
import ru.hh.android.synthetic_plugin.utils.ClassParentsFinder
import ru.hh.android.synthetic_plugin.utils.Const

class HHViewBindingPsiProcessor(
    projectInfo: ProjectInfo,
) : ViewBindingPsiProcessor(
    projectInfo,
) {
    private companion object {
        const val HH_IMPORT_BINDING_PLUGIN =
            "ru.hh.shared.core.ui.framework.fragment_plugin.common.viewbinding.viewBindingPlugin"

        const val HH_IMPORT_INFLATE_AND_BIND_FUN = "ru.hh.shared.core.ui.design_system.utils.widget.inflateAndBindView"

        const val HH_IMPORT_CELLS_GET_VIEW_BINDING_FUN = "ru.hh.shared.core.ui.cells_framework.cells.getViewBinding"

        // Cells in hh.ru - wrapper for reducing boilerplate in delegates for RecyclerView
        const val HH_CELL_INTERFACE = "ru.hh.shared.core.ui.cells_framework.cells.interfaces.Cell"
    }

    /**
     * Left for single case - HH cell processing
     */
    private val bindingClassName = importDirectives.firstOrNull()

    override fun processActivity(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        importDirectives.forEach { bindingClassName ->
            val text = bindingClassName.toDelegatePropertyFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createProperty(text)

            tryToAddAfterCompanionObject(body, viewBindingDeclaration)

            addImports(
                HH_IMPORT_BINDING_PLUGIN,
            )
        }
    }

    override fun processFragment(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        importDirectives.forEach { bindingClassName ->
            val text = bindingClassName.toDelegatePropertyFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createProperty(text)

            tryToAddAfterCompanionObject(body, viewBindingDeclaration)

            addImports(
                HH_IMPORT_BINDING_PLUGIN,
            )
        }
    }

    override fun processView(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        val inflateViewExpression = body.anonymousInitializers.getOrNull(0)
            ?.body?.children?.firstOrNull { it.text.contains("inflateView") }
        inflateViewExpression?.delete()

        importDirectives.forEach { bindingClassName ->
            val text = bindingClassName.toViewDelegatePropertyFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createProperty(text)

            tryToAddAfterCompanionObject(body, viewBindingDeclaration)

            addImports(
                HH_IMPORT_INFLATE_AND_BIND_FUN,
            )
        }
    }

    override fun canHandle(parents: ClassParentsFinder, ktClass: KtClass): Boolean {
        return when {
            parents.isChildOf(HH_CELL_INTERFACE) -> true
            else -> false
        }
    }

    override fun processCustomCases(parents: ClassParentsFinder, ktClass: KtClass) {
        when {
            parents.isChildOf(HH_CELL_INTERFACE) -> processCell(ktClass)
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
                val newElement = projectInfo.psiFactory.createArgument("viewHolder.getViewBinding(${bindingClassName}::bind)")
                withArg.replace(newElement)
            }
        addImports(
            HH_IMPORT_CELLS_GET_VIEW_BINDING_FUN,
        )
    }
}