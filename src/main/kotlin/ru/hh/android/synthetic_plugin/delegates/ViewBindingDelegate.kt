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
    private val viewBindingPsiProcessor: ViewBindingPsiProcessor
) {

    private companion object {
        const val ANDROID_ACTIVITY_CLASS = "android.app.Activity"
        const val ANDROID_FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
        const val ANDROID_VIEW_CLASS = "android.view.View"
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
                parents.isChildOf(ANDROID_ACTIVITY_CLASS) -> viewBindingPsiProcessor.processActivity(ktClass)
                parents.isChildOf(ANDROID_FRAGMENT_CLASS) -> viewBindingPsiProcessor.processFragment(ktClass)
                parents.isChildOf(ANDROID_VIEW_CLASS) -> viewBindingPsiProcessor.processView(ktClass)
                viewBindingPsiProcessor.canHandle(parents, ktClass) -> viewBindingPsiProcessor.processCustomCases(parents, ktClass)
                else -> projectInfo.project.notifyError("Can't add ViewBinding property to class: ${psiClass.qualifiedName}")
            }
            viewBindingPsiProcessor.addViewBindingImports(ktClass)
            viewBindingPsiProcessor.formatCode(ktClass)
        }
    }
}
