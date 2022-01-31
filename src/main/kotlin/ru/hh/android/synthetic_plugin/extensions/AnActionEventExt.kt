package ru.hh.android.synthetic_plugin.extensions

import com.android.tools.idea.util.androidFacet
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiElement
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.util.module


fun AnActionEvent.androidFacet(): AndroidFacet? = getSelectedPsiElement()?.module?.androidFacet

fun AnActionEvent.getSelectedPsiElement(): PsiElement? = getData(PlatformDataKeys.PSI_ELEMENT)