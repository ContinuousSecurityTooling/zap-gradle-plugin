package net.continuoussecuritytools.zap

import net.cst.zap.api.model.AnalysisType
import net.cst.zap.api.model.AuthenticationType
import net.cst.zap.api.model.SeleniumDriver
import net.cst.zap.commons.ZapInfo
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ZapPluginExtensionSpec extends Specification {

    def project = ProjectBuilder.builder().build()
    def extension = new ZapPluginExtension(project)

    def "defaults reportPath to <buildDir>/zap-reports"() {
        expect:
        extension.reportPath == new File(project.layout.buildDirectory.get().asFile, 'zap-reports')
    }

    def "buildZapInfo requires zapPort"() {
        when:
        extension.buildZapInfo()

        then:
        def e = thrown(GradleException)
        e.message == 'zap.zapPort must be set'
    }

    def "buildZapInfo builds a ZapInfo from the configured values"() {
        given:
        extension.zapPort = 8090
        extension.zapHost = 'zap.example.com'
        extension.zapApiKey = 'secret'
        extension.failingRiskCodeThreshold = 5

        when:
        ZapInfo zapInfo = extension.buildZapInfo()

        then:
        zapInfo.host == 'zap.example.com'
        zapInfo.port == 8090
        zapInfo.apiKey == 'secret'
        zapInfo.failingRiskCode == 5
    }

    def "buildAuthenticationInfo returns null when authenticationType is not set"() {
        expect:
        extension.buildAuthenticationInfo() == null
    }

    def "buildAuthenticationInfo builds an AuthenticationInfo when authenticationType is set"() {
        given:
        extension.authenticationType = 'form'
        extension.loginUrl = 'http://localhost:8080/login'
        extension.username = 'user'
        extension.password = 'pass'

        when:
        def authenticationInfo = extension.buildAuthenticationInfo()

        then:
        authenticationInfo.type == AuthenticationType.FORM
        authenticationInfo.loginUrl == 'http://localhost:8080/login'
        authenticationInfo.username == 'user'
        authenticationInfo.password == 'pass'
        authenticationInfo.seleniumDriver == SeleniumDriver.FIREFOX
    }

    def "buildAnalysisInfo requires targetUrl"() {
        when:
        extension.buildAnalysisInfo()

        then:
        def e = thrown(GradleException)
        e.message == 'zap.targetUrl must be set'
    }

    def "buildAnalysisInfo derives the analysis type from the spider flags"() {
        given:
        extension.targetUrl = 'http://localhost:8080'
        extension.shouldRunAjaxSpider = ajax
        extension.shouldRunPassiveScanOnly = passiveOnly

        when:
        def analysisInfo = extension.buildAnalysisInfo()

        then:
        analysisInfo.analysisType == expectedType

        where:
        ajax  | passiveOnly || expectedType
        false | false       || AnalysisType.WITH_SPIDER
        true  | false       || AnalysisType.WITH_AJAX_SPIDER
        false | true        || AnalysisType.SPIDER_ONLY
        true  | true        || AnalysisType.SPIDER_AND_AJAX_SPIDER_ONLY
    }

    def "buildAnalysisInfo(AnalysisType) overrides the derived analysis type"() {
        given:
        extension.targetUrl = 'http://localhost:8080'

        when:
        def analysisInfo = extension.buildAnalysisInfo(AnalysisType.ACTIVE_SCAN_ONLY)

        then:
        analysisInfo.analysisType == AnalysisType.ACTIVE_SCAN_ONLY
    }
}
