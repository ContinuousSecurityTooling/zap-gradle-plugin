package net.continuoussecuritytools.zap

import net.cst.zap.api.model.AnalysisInfo
import net.cst.zap.api.model.AnalysisType
import net.cst.zap.api.model.AuthenticationInfo
import net.cst.zap.api.model.SeleniumDriver
import net.cst.zap.api.report.ZapReport
import net.cst.zap.api.report.ZapReportUtil
import net.cst.zap.commons.ZapInfo
import net.cst.zap.reporting.ZapReportParser
import org.gradle.api.GradleException
import org.gradle.api.Project

class ZapPluginExtension {

    boolean skip = false

    // Analysis
    String targetUrl
    int failingRiskCodeThreshold = 10
    String spiderStartingPointUrl
    String activeScanStartingPointUrl
    List<String> context
    List<String> technologies
    int analysisTimeoutInMinutes = 480
    boolean shouldRunAjaxSpider = false
    boolean shouldRunPassiveScanOnly = false
    boolean shouldStartNewSession = true

    // ZAP
    Integer zapPort
    String zapHost = 'localhost'
    String zapApiKey = ''
    String zapPath
    String zapJvmOptions = '-Xmx512m'
    String zapOptions = ZapInfo.DEFAULT_OPTIONS
    boolean shouldRunWithDocker = false
    long initializationTimeoutInMillis = 120000
    File reportPath

    // Authentication
    String authenticationType
    String loginUrl
    String username
    String password
    String extraPostData
    String loggedInRegex
    String loggedOutRegex
    List<String> excludeFromScan
    List<String> protectedPages
    String usernameParameter = 'username'
    String passwordParameter = 'password'
    List<String> httpSessionTokens
    String seleniumDriver = 'firefox'
    String hostname
    String realm
    int authenticationPort = 80

    ZapPluginExtension(Project project) {
        this.reportPath = project.layout.buildDirectory.dir('zap-reports').get().asFile
    }

    ZapInfo buildZapInfo() {
        require(zapPort, 'zapPort')
        return ZapInfo.builder()
            .host(zapHost)
            .port(zapPort)
            .failingRiskCode(failingRiskCodeThreshold)
            .apiKey(zapApiKey)
            .path(zapPath)
            .jmvOptions(zapJvmOptions)
            .options(zapOptions)
            .initializationTimeoutInMillis(initializationTimeoutInMillis)
            .shouldRunWithDocker(shouldRunWithDocker)
            .build()
    }

    AuthenticationInfo buildAuthenticationInfo() {
        if (authenticationType == null) {
            return null
        }
        return AuthenticationInfo.builder()
            .type(authenticationType)
            .loginUrl(loginUrl)
            .username(username)
            .password(password)
            .extraPostData(extraPostData)
            .loggedInRegex(loggedInRegex)
            .loggedOutRegex(loggedOutRegex)
            .excludeFromScan(excludeFromScan as String[])
            .protectedPages(protectedPages as String[])
            .usernameParameter(usernameParameter)
            .passwordParameter(passwordParameter)
            .loginRequestData()
            .httpSessionTokens(httpSessionTokens as String[])
            .seleniumDriver(SeleniumDriver.valueOf(seleniumDriver.toUpperCase()))
            .hostname(hostname)
            .realm(realm)
            .port(authenticationPort)
            .build()
    }

    AnalysisInfo buildAnalysisInfo() {
        AnalysisType analysisType = AnalysisType.WITH_SPIDER
        if (shouldRunAjaxSpider && shouldRunPassiveScanOnly) {
            analysisType = AnalysisType.SPIDER_AND_AJAX_SPIDER_ONLY
        }
        else {
            if (shouldRunAjaxSpider) {
                analysisType = AnalysisType.WITH_AJAX_SPIDER
            }
            if (shouldRunPassiveScanOnly) {
                analysisType = AnalysisType.SPIDER_ONLY
            }
        }
        return buildAnalysisInfo(analysisType)
    }

    AnalysisInfo buildAnalysisInfo(AnalysisType analysisType) {
        require(targetUrl, 'targetUrl')
        return AnalysisInfo.builder()
            .targetUrl(targetUrl)
            .spiderStartingPointUrl(spiderStartingPointUrl)
            .activeScanStartingPointUrl(activeScanStartingPointUrl)
            .context(context as String[])
            .technologies(technologies as String[])
            .analysisTimeoutInMinutes(analysisTimeoutInMinutes)
            .analysisType(analysisType)
            .shouldStartNewSession(shouldStartNewSession)
            .build()
    }

    void saveReport(ZapReport zapReport) {
        if (reportPath != null) {
            ZapReportUtil.saveAllReports(zapReport, reportPath.absolutePath)
        }
        else {
            ZapReportUtil.saveAllReports(zapReport)
        }
    }

    void verifyRiskThreshold(ZapReport zapReport) {
        int riskCode = new ZapReportParser().getHighestRiskCode(zapReport)
        if (riskCode > failingRiskCodeThreshold) {
            throw new GradleException("Detected too high risk code: ${riskCode} (threshold: ${failingRiskCodeThreshold})")
        }
    }

    private static void require(Object value, String property) {
        if (value == null) {
            throw new GradleException("zap.${property} must be set")
        }
    }
}
