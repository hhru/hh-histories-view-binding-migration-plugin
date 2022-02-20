package ru.hh.android.synthetic_plugin.delegates

import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import ru.hh.android.synthetic_plugin.extensions.notifyInfo
import ru.hh.android.synthetic_plugin.model.AndroidViewContainer
import ru.hh.android.synthetic_plugin.model.ProjectInfo
import ru.hh.android.synthetic_plugin.visitor.AndroidViewXmlSyntheticsRefsVisitor
import ru.hh.android.synthetic_plugin.visitor.SyntheticsImportsVisitor

object ConvertKtFileDelegate {

    /**
     * This function should be invoked inside [com.intellij.openapi.project.Project.executeWriteCommand]
     * because this method modify your codebase.
     */
    fun perform(
        projectInfo: ProjectInfo,
        isUsingViewBindingPropertyDelegate: Boolean = false,
    ) {
        val xmlRefsVisitor = AndroidViewXmlSyntheticsRefsVisitor()
        val importsVisitor = SyntheticsImportsVisitor()
        projectInfo.file.accept(xmlRefsVisitor)
        projectInfo.file.accept(importsVisitor)
        val xmlViewRefs = xmlRefsVisitor.getResult()
        val syntheticImports = importsVisitor.getResult()
        val viewBindingProvider = getViewBindingProvider(projectInfo, isUsingViewBindingPropertyDelegate)

        val viewBindingDelegate = ViewBindingDelegate(
            projectInfo = projectInfo,
            viewBindingProvider = viewBindingProvider,
        )

        viewBindingDelegate.addViewBindingProperties()
        replaceSynthCallsToViews(
            psiFactory = projectInfo.psiFactory,
            xmlViewRefs = xmlViewRefs,
            viewBindingProperties = viewBindingProvider.syntheticImportDirectives,
            hasMultipleBindingsInFile = viewBindingProvider.hasMultipleBindingsInFile,
        )
        removeKotlinxSyntheticsImports(syntheticImports)

        projectInfo.project.notifyInfo("File ${projectInfo.file.name} converted successfully!")
    }

    private fun getViewBindingProvider(
        projectInfo: ProjectInfo,
        isUsingViewBindingPropertyDelegate: Boolean,
    ) = when {
        isUsingViewBindingPropertyDelegate -> {
            ViewBindingPropertyDelegateImpl(projectInfo)
        }
        else -> {
            CommonViewBindingImpl(projectInfo)
        }
    }

    private fun replaceSynthCallsToViews(
        psiFactory: KtPsiFactory,
        xmlViewRefs: List<AndroidViewContainer>,
        viewBindingProperties: List<KtImportDirective>,
        hasMultipleBindingsInFile: Boolean,
    ) {
        xmlViewRefs.forEach { refContainer ->
            val newElement = psiFactory.createArgument(
                refContainer.getElementName(
                    viewBindingProperties = viewBindingProperties,
                    hasMultipleBindingsInFile = hasMultipleBindingsInFile,
                )
            )

            when (refContainer) {
                is AndroidViewContainer.KtRefExp -> {
                    refContainer.ref.replace(newElement)
                }
                is AndroidViewContainer.PsiRef -> {
                    refContainer.ref.element.replace(newElement)
                }
            }
        }
    }

    private fun removeKotlinxSyntheticsImports(syntheticImports: List<KtImportDirective>) {
        syntheticImports.forEach { import ->
            import.delete()
        }
    }
}
