package ru.hh.android.synthetic_plugin.model

import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class ProjectInfo(
    val file: KtFile,
    val project: Project,
    val androidFacet: AndroidFacet?,
    val psiFactory: KtPsiFactory = KtPsiFactory(project),
)