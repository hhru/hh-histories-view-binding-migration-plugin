package ru.hh.android.synthetic_plugin.delegates

import org.jetbrains.kotlin.idea.intentions.isZero
import org.jetbrains.kotlin.psi.*
import ru.hh.android.synthetic_plugin.extensions.*
import ru.hh.android.synthetic_plugin.model.ProjectInfo
import ru.hh.android.synthetic_plugin.utils.Const

class CommonViewBindingImpl(
    projectInfo: ProjectInfo,
) : ViewBindingProvider(
    projectInfo,
) {
    override fun processActivity(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        importDirectives.forEach { bindingClassName ->
            val text = bindingClassName.toActivityPropertyFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createProperty(text)

            tryToAddAfterCompanionObject(body, viewBindingDeclaration)
        }
    }

    override fun processFragment(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        importDirectives.forEach { bindingClassName ->
            val mutablePropertyText = bindingClassName.toMutablePropertyFormat(hasMultipleBindingsInFile)
            val immutablePropertyText = bindingClassName.toImmutablePropertyFormat(hasMultipleBindingsInFile)
            val mutableViewBinding = projectInfo.psiFactory.createProperty(mutablePropertyText)
            val immutableViewBinding = projectInfo.psiFactory.createProperty(immutablePropertyText)

            tryToAddAfterCompanionObject(body, mutableViewBinding, immutableViewBinding)

            addBindingInitializationForFragment(body, bindingClassName)
            addBindingDisposingForFragment(body, bindingClassName)
        }
    }

    override fun processView(ktClass: KtClass) {
        val body = ktClass.getOrCreateBody()
        importDirectives.forEach { bindingClassName ->
            val text = bindingClassName.toViewPropertyFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createProperty(text)

            tryToAddAfterCompanionObject(body, viewBindingDeclaration)
        }
        tryToRemoveExistingViewInflaters(body)
    }

    override fun processCell(ktClass: KtClass) = Unit

    private fun addBindingInitializationForFragment(
        body: KtClassBody,
        bindingClassName: String,
    ) {
        body.functions.find { it.name == "onCreateView" }?.let {
            val text = bindingClassName.toFragmentInitializationFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createExpression(text)
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
            val newOnDestroyViewFunc = projectInfo.psiFactory.createFunction(Const.ON_DESTROY_FUNC_DECLARATION)
            body.addBefore(newOnDestroyViewFunc, body.rBrace)
        }
        onDestroyViewFunc = body.functions.find { it.name == "onDestroyView" }

        onDestroyViewFunc?.let {
            val text = bindingClassName.toFragmentDisposingFormat(hasMultipleBindingsInFile)
            val viewBindingDeclaration = projectInfo.psiFactory.createExpression(text)
            viewBindingDeclaration.add(getNewLine())
            it.addAfter(viewBindingDeclaration, it.bodyBlockExpression?.lBrace)
        }
    }

    private fun tryToRemoveExistingViewInflaters(
        body: KtClassBody,
    ) {
        body.declarations.filterIsInstance<KtClassInitializer>().forEach {
            val viewInflaters = it.body?.children?.find { element ->
                element.text.contains(Const.LAYOUT_INFLATER_PREFIX)
                        || element.text.contains(Const.VIEW_INFLATER_PREFIX)
            }

            // Remove whole init block if it has only one view inflater children
            if (viewInflaters != null && it.body?.children?.size == 1) {
                it.delete()
                return
            }
            viewInflaters?.delete()
        }
    }
}