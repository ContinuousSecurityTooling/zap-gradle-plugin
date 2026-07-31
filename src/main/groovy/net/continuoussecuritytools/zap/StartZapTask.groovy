package net.continuoussecuritytools.zap

import net.cst.zap.commons.boot.Zap
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class StartZapTask extends DefaultTask {

    @Internal
    ZapPluginExtension zapExtension

    StartZapTask() {
        group = 'ZAP'
        description = 'Starts OWASP ZAP, typically ahead of Selenium-driven integration tests that proxy through it.'
        onlyIf { !zapExtension.skip }
    }

    @TaskAction
    void startZap() {
        Zap.startZap(zapExtension.buildZapInfo())
    }
}
