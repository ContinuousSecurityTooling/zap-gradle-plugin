package net.continuoussecuritytools.zap

import org.gradle.api.Plugin
import org.gradle.api.Project

class ZapPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        ZapPluginExtension extension = project.extensions.create('zap', ZapPluginExtension, project)

        project.tasks.register('startZap', StartZapTask) { it.zapExtension = extension }
        project.tasks.register('zapAnalyze', ZapAnalyzeTask) { it.zapExtension = extension }
        project.tasks.register('zapSeleniumAnalyze', ZapSeleniumAnalyzeTask) { it.zapExtension = extension }
    }
}
