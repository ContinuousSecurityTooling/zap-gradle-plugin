package net.continuoussecuritytools.zap

import net.cst.zap.api.ZapClient
import net.cst.zap.api.model.AnalysisInfo
import net.cst.zap.api.model.AnalysisType
import net.cst.zap.api.model.AuthenticationInfo
import net.cst.zap.api.report.ZapReport
import net.cst.zap.commons.ZapInfo
import net.cst.zap.commons.boot.Zap
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class ZapSeleniumAnalyzeTask extends DefaultTask {

    @Internal
    ZapPluginExtension zapExtension

    ZapSeleniumAnalyzeTask() {
        group = 'ZAP'
        description = 'Runs an OWASP ZAP Active Scan only (no Spider) against an already-running ZAP instance, using the navigation recorded by Selenium-driven integration tests. Run the startZap task beforehand.'
        onlyIf { !zapExtension.skip }
    }

    @TaskAction
    void analyze() {
        logger.lifecycle("Starting ZAP analysis at target: ${zapExtension.targetUrl}")

        ZapInfo zapInfo = zapExtension.buildZapInfo()
        AuthenticationInfo authenticationInfo = zapExtension.buildAuthenticationInfo()
        AnalysisInfo analysisInfo = zapExtension.buildAnalysisInfo(AnalysisType.ACTIVE_SCAN_ONLY)

        ZapClient zapClient = new ZapClient(zapInfo, authenticationInfo)
        try {
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
