package com.sst.projectplugin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project


class ProjectPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("this is =============")
        println("this is ProjectPlugin")
        println("this is =============")
    }
}