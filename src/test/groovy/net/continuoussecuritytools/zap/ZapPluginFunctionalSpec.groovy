package net.continuoussecuritytools.zap

import nebula.test.IntegrationTestKitSpec
import org.gradle.testkit.runner.TaskOutcome

class ZapPluginFunctionalSpec extends IntegrationTestKitSpec {

    def setup() {
        buildFile << '''
            plugins {
                id 'net.continuous-security-tools.zap-gradle'
            }
        '''.stripIndent()
    }

    def "registers the ZAP tasks"() {
        when:
        def result = runTasks('tasks', '--group', 'ZAP')

        then:
        result.output.contains('startZap')
        result.output.contains('zapAnalyze')
        result.output.contains('zapSeleniumAnalyze')
    }

    def "skips the ZAP tasks when zap.skip is true"() {
        given:
        buildFile << '''
            zap {
                skip = true
                targetUrl = 'http://localhost:8080'
                zapPort = 8090
            }
        '''.stripIndent()

        when:
        def result = runTasks('startZap', 'zapAnalyze', 'zapSeleniumAnalyze')

        then:
        result.task(':startZap').outcome == TaskOutcome.SKIPPED
        result.task(':zapAnalyze').outcome == TaskOutcome.SKIPPED
        result.task(':zapSeleniumAnalyze').outcome == TaskOutcome.SKIPPED
    }

    def "fails startZap when zapPort is not configured"() {
        given:
        buildFile << '''
            zap {
                targetUrl = 'http://localhost:8080'
            }
        '''.stripIndent()

        when:
        def result = runTasksAndFail('startZap')

        then:
        result.output.contains('zap.zapPort must be set')
    }

    def "fails zapAnalyze when targetUrl is not configured"() {
        given:
        buildFile << '''
            zap {
                zapPort = 8090
            }
        '''.stripIndent()

        when:
        def result = runTasksAndFail('zapAnalyze')

        then:
        result.output.contains('zap.targetUrl must be set')
    }
}
