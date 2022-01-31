package com.github.pstrelchenko.hhhistoriesviewbindingmigrationplugin.services

import com.intellij.openapi.project.Project
import com.github.pstrelchenko.hhhistoriesviewbindingmigrationplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
