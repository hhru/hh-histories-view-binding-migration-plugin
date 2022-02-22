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
 * Support action for adding view bindings in common way, without ViewBindingPropertyDelegate
 *
 * For Activities:
 *      "private val binding by lazy { ActivityBinding.inflate(layoutInflater) }"
 *
 * For Fragments:
 *
 *      Declaration:
 *      "private var _binding: FragmentBinding? = null"
 *      "private val binding: FragmentBinding get() = _binding"
 *
 *      onCreateView(...) { _binding = FragmentBinding.inflate(inflater, container, false) }
 *
 *      onDestroyView(...) { _binding = null }
 *
 * For Views:
 *
 *      Declaration:
 *      "private val binding = SomeViewBinding.inflate(LayoutInflater.from(context), this)"
 */
class ConvertSyntheticsToCommonViewBindingAction : AnAction() {

    private companion object {
        const val COMMAND_NAME = "ConvertSyntheticsToCommonViewBindingCommand"
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
            ConvertKtFileDelegate.perform(projectInfo)
        }
    }
}
