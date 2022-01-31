package ru.hh.android.synthetic_plugin.visitor

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentsOfType
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlElementType
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import ru.hh.android.synthetic_plugin.model.AndroidViewContainer
import ru.hh.android.synthetic_plugin.utils.Const

class AndroidViewXmlSyntheticsRefsVisitor : KotlinRecursiveElementVisitor() {

    private companion object {
        const val KOTLIN_WITH_EXPRESSION = "with"
    }

    private val result = mutableListOf<AndroidViewContainer>()

    fun getResult(): List<AndroidViewContainer> = result.toList()

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        result.addAll(expression.references.findSyntheticRefs())
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        (findViewRefInMethodCall(expression) ?: findViewRefInWithCall(expression))?.let(result::add)
        result.addAll(findNestedRefsInExpression(expression))
    }

    /**
     * with(view_id) { ... }
     */
    private fun findViewRefInWithCall(expression: KtCallExpression): AndroidViewContainer? {
        expression.children.firstOrNull()?.let { psiElement ->
            val isWithExpression = psiElement.text == KOTLIN_WITH_EXPRESSION
            if (isWithExpression && expression.children.size >= 2) {
                return expression.children[1]
                    .children.firstOrNull()
                    ?.children?.firstOrNull()
                    ?.tryFindViewWithKtRefExpression()
            }
        }
        return null
    }

    /**
     * with(anything) { view_id.doSomething() }
     * anything.setOnClickListener { view_id_another.gone() }
     */
    private fun findNestedRefsInExpression(expression: KtCallExpression): List<AndroidViewContainer> {
        val result = mutableListOf<AndroidViewContainer>()
        val nestedExpressions = expression.lambdaArguments.firstOrNull()
            ?.getLambdaExpression()?.functionLiteral?.bodyBlockExpression?.children ?: return emptyList()
        nestedExpressions.forEach { nestedExp ->
            tryFindViewRefOrGoDeeper(nestedExp, result, 0)
        }
        return result.toList()
    }

    private fun tryFindViewRefOrGoDeeper(element: PsiElement, result: MutableList<AndroidViewContainer>, depth: Int) {
        element.children.forEach { nestedChild ->
            nestedChild.tryFindViewWithKtRefExpression()?.let(result::add)
            if (depth < 5) {
                tryFindViewRefOrGoDeeper(nestedChild, result, depth + 1)
            }
        }
    }

    /**
     * someFunction(view_id)
     */
    private fun findViewRefInMethodCall(expression: KtCallExpression): AndroidViewContainer? {
        return expression.references.firstOrNull()
            ?.element?.children?.getOrNull(1)
            ?.children?.firstOrNull()
            ?.children?.firstOrNull()
            ?.tryFindViewWithKtRefExpression()
    }

    /**
     * view_id.viewProperty (getter)
     * view_id.viewProperty = something (setter)
     * view_id?.let { ... }
     * view_id.apply { ... }
     */
    private fun Array<PsiReference>.findSyntheticRefs(): List<AndroidViewContainer> {
        return filterIsInstance<SyntheticPropertyAccessorReference>()
            .flatMap { it.element.references.toList() }
            .mapNotNull { ref ->
                ref.resolve()?.takeIfAndroidViewIdAttr()?.let { asXmlAttr ->
                    AndroidViewContainer.PsiRef(ref, asXmlAttr, ref.element.isNeedBindingPrefix())
                }
            }
    }

    private fun PsiElement.tryFindViewWithKtRefExpression(): AndroidViewContainer? {
        return (this as? KtReferenceExpression)?.let { ref ->
            ref.resolve()?.takeIfAndroidViewIdAttr()?.let { asXmlAttr ->
                AndroidViewContainer.KtRefExp(ref, asXmlAttr, ref.isNeedBindingPrefix())
            }
        }
    }

    private fun PsiElement.takeIfAndroidViewIdAttr(): XmlAttributeValue? {
        return when {
            elementType == XmlElementType.XML_ATTRIBUTE_VALUE &&
                    (this as XmlAttributeValue).value.startsWith(Const.ANDROID_VIEW_ID) -> this
            else -> null
        }
    }

    private fun PsiElement.isNeedBindingPrefix(): Boolean {
        return parentsOfType(KtCallExpression::class.java).firstOrNull {
            it.text.contains(Const.CELL_WITH_VIEW_HOLDER)
        } == null
    }
}
