package net.continuoussecuritytools.zap

import net.cst.zap.api.ZapClient
import net.cst.zap.api.model.AnalysisInfo
import net.cst.zap.api.model.AuthenticationInfo
import net.cst.zap.api.report.ZapReport
import net.cst.zap.commons.ZapInfo
import net.cst.zap.commons.boot.Zap
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ZapAnalyzeTask extends DefaultTask {

    @Internal
    ZapPluginExtension zapExtension

    ZapAnalyzeTask() {
        group = 'ZAP'
        description = 'Starts OWASP ZAP, runs the Spider (and optionally the AJAX Spider) followed by the Active Scan, and generates reports.'
        onlyIf { !zapExtension.skip }
    }

    @TaskAction
    void analyze() {
        logger.lifecycle("Starting ZAP analysis at target: ${zapExtension.targetUrl}")

        ZapInfo zapInfo = zapExtension.buildZapInfo()
        AuthenticationInfo authenticationInfo = zapExtension.buildAuthenticationInfo()
        AnalysisInfo analysisInfo = zapExtension.buildAnalysisInfo()

        ZapClient zapClient = new ZapClient(zapInfo, authenticationInfo)
        try {
            Zap.startZap(zapInfo)
            ZapReport zapReport = zapClient.analyze(analysisInfo)
            zapExtension.saveReport(zapReport)
            zapExtension.verifyRiskThreshold(zapReport)
        }
        finally {
            Zap.stopZap()
        }

        logger.lifecycle('ZAP analysis finished.')
    }
}
