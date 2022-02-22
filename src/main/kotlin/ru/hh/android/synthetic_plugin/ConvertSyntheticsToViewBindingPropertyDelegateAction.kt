package ru.hh.android.synthetic_plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import ru.hh.android.synthetic_plugin.delegates.ConvertKtFileDelegate
import ru.hh.android.synthetic_plugin.extensions.androidFacet
import ru.hh.android.synthetic_plugin.extensions.haveSyntheticImports
import ru.hh.android.synthetic_plugin.model.ProjectInfo

/**
 * TODO:
 *
 * ## build.gradle files modification
 * You should split implementations for Groovy and Kotlin
 *
 * - [ ] Remove `id("kotlin-android-extensions")` from `plugins` block in `build.gradle` file
 * - [ ] Add `android.buildFeatures.viewBinding = true` line into `build.gradle`
 */
class ConvertSyntheticsToViewBindingPropertyDelegateAction : AnAction() {

    private companion object {
        const val COMMAND_NAME = "ConvertSyntheticsToViewBindingPropertyDelegateCommand"
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        val isValidForFile = e.project != null
                && psiFile != null
                && psiFile is KtFile
                && psiFile.haveSyntheticImports()

        e.presentation.isVisible = isValidForFile
        e.presentation.isEnabled = isValidForFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) as KtFile
        val project = e.project as Project
        val projectInfo = ProjectInfo(file, project, e.androidFacet())

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        project.executeWriteCommand(COMMAND_NAME) {
            ConvertKtFileDelegate.perform(
                projectInfo = projectInfo,
                isUsingViewBindingPropertyDelegate = true,
            )
        }
    }
}
