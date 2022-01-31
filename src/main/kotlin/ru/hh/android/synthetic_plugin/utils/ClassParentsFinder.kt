package ru.hh.android.synthetic_plugin.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil

class ClassParentsFinder(psiClass: PsiClass) {

    private val parents = InheritanceUtil.getSuperClasses(psiClass)

    fun isChildOf(vararg classQualifiedNames: String): Boolean {
        return parents.any { parentClass ->
            parentClass.qualifiedName in classQualifiedNames
        }
    }
}
