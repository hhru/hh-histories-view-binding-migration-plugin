package ru.hh.android.synthetic_plugin.delegates

import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import ru.hh.android.synthetic_plugin.model.AndroidViewContainer
import ru.hh.android.synthetic_plugin.visitor.AndroidViewXmlSyntheticsRefsVisitor
import ru.hh.android.synthetic_plugin.visitor.SyntheticsImportsVisitor

object ConvertKtFileDelegate {

    /**
     * This function should be invoked inside [com.intellij.openapi.project.Project.executeWriteCommand]
     * because this method modify your codebase.
     */
    fun perform(
        file: KtFile,
        project: Project,
        androidFacet: AndroidFacet?,
        psiFactory: KtPsiFactory = KtPsiFactory(project),
        isUsingViewBindingPropertyDelegate: Boolean = false,
    ) {
        val xmlRefsVisitor = AndroidViewXmlSyntheticsRefsVisitor()
        val importsVisitor = SyntheticsImportsVisitor()
        file.accept(xmlRefsVisitor)
        file.accept(importsVisitor)
        val xmlViewRefs = xmlRefsVisitor.getResult()
        val syntheticImports = importsVisitor.getResult()
        val viewBindingPropertyDelegate = ViewBindingPropertyDelegate(
            psiFactory = psiFactory,
            file = file,
            androidFacet = androidFacet,
            isUsingViewBindingPropertyDelegate = isUsingViewBindingPropertyDelegate,
            project = project,
        )
        viewBindingPropertyDelegate.addViewBindingProperties()
        replaceSynthCallsToViews(
            psiFactory = psiFactory,
            xmlViewRefs = xmlViewRefs,
            viewBindingProperties = viewBindingPropertyDelegate.viewBindingProperties,
            isMultipleBindingInFile = viewBindingPropertyDelegate.isMultipleBindingInFile,
        )
        removeKotlinxSyntheticsImports(syntheticImports)

        println("Converted synthetics to view binding for ${file.name} successfully")
    }

    private fun replaceSynthCallsToViews(
        psiFactory: KtPsiFactory,
        xmlViewRefs: List<AndroidViewContainer>,
        viewBindingProperties: List<KtImportDirective>,
        isMultipleBindingInFile: Boolean,
    ) {
        xmlViewRefs.forEach { refContainer ->
            val newElement = psiFactory.createArgument(
                refContainer.getElementName(
                    viewBindingProperties = viewBindingProperties,
                    isMultipleBindingInFile = isMultipleBindingInFile,
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
