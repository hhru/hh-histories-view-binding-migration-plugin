package ru.hh.android.synthetic_plugin.delegates

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.psi.KtClass
import ru.hh.android.synthetic_plugin.extensions.notifyError
import ru.hh.android.synthetic_plugin.model.ProjectInfo
import ru.hh.android.synthetic_plugin.utils.ClassParentsFinder


/**
 * TODO:
 *
 * - [ ] For Cells we should handle case when there is no `with(viewHolder.itemView)` block
 * (case with direct invocation of `viewHolder.itemView.view_id`)
 */
class ViewBindingDelegate(
    private val projectInfo: ProjectInfo,
    private val viewBindingProvider: ViewBindingProvider
) {

    private companion object {
        const val ANDROID_ACTIVITY_CLASS = "android.app.Activity"
        const val ANDROID_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        const val ANDROID_VIEW_CLASS = "android.view.View"

        // Cells in hh.ru - wrapper for reducing boilerplate in delegates for RecyclerView
        const val HH_CELL_INTERFACE = "ru.hh.shared.core.ui.cells_framework.cells.interfaces.Cell"
    }

    fun addViewBindingProperties() {
        // `as Array<PsiClass` is necessary because of MISSING_DEPENDENCY_CLASS error from Kotlin Gradle plugin
        // https://youtrack.jetbrains.com/issue/KTIJ-19485
        // https://youtrack.jetbrains.com/issue/KTIJ-10861
        val classes = (projectInfo.file.getClasses() as Array<PsiClass>).mapNotNull { psiClass ->
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
                parents.isChildOf(ANDROID_ACTIVITY_CLASS) -> viewBindingProvider.processActivity(ktClass)
                parents.isChildOf(ANDROID_FRAGMENT_CLASS) -> viewBindingProvider.processFragment(ktClass)
                parents.isChildOf(ANDROID_VIEW_CLASS) -> viewBindingProvider.processView(ktClass)
                parents.isChildOf(HH_CELL_INTERFACE) -> viewBindingProvider.processCell(ktClass)
                else -> projectInfo.project.notifyError("Can't add ViewBinding property to class: ${psiClass.qualifiedName}")
            }
            viewBindingProvider.addViewBindingImports(ktClass)
            viewBindingProvider.formatCode(ktClass)
        }
    }
}
