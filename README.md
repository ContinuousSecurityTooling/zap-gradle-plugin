# ZAP Gradle Plugin

A Gradle plugin for running [OWASP ZAP](https://www.zaproxy.org/) security scans as part of
your build, companion to
[zap-maven-plugin](https://github.com/ContinuousSecurityTooling/zap-maven-plugin) for Maven
users. It's a thin Gradle wrapper around the same `zap-utils` / `zap-client-api` /
`zap-report-parser` libraries the Maven plugin uses, so behavior and configuration map closely
to that plugin's goals.

## Requirements

- Gradle 8.14.5 or compatible (see [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties))
- Java 17+

## Usage

Apply the plugin from the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/net.continuous-security-tools.zap-gradle):

```groovy
plugins {
    id 'net.continuous-security-tools.zap-gradle' version 'X.Y.Z'
}
```

```kotlin
plugins {
    id("net.continuous-security-tools.zap-gradle") version "X.Y.Z"
}
```

Replace `X.Y.Z` with the latest released version.

### Tasks

| Task | Maps to Maven goal | What it does |
| --- | --- | --- |
| `startZap` | `startZap` | Starts (or connects to) ZAP. Use this ahead of Selenium-driven integration tests that proxy through ZAP. |
| `zapAnalyze` | `analyze` | Starts ZAP, runs the Spider (and optionally the AJAX Spider), runs the Active Scan, saves reports, then stops ZAP. |
| `zapSeleniumAnalyze` | `seleniumAnalyze` | Runs an Active Scan only (no Spider) against an already-running ZAP instance, using navigation recorded during integration tests, then stops ZAP. Run `startZap` first. |

### Configuration

Configure the `zap {}` extension. `targetUrl` and `zapPort` are always required; everything else
is optional.

```groovy
zap {
    // required
    targetUrl = 'http://localhost:8080'
    zapPort = 8090

    // ZAP process
    zapHost = 'localhost'                 // default: localhost
    zapPath = '/opt/zap/zap.sh'           // set to auto-start a local ZAP install
    zapJvmOptions = '-Xmx512m'            // default
    shouldRunWithDocker = false           // default
    zapApiKey = ''                        // default (empty = ZAP API key disabled)
    initializationTimeoutInMillis = 120000 // default

    // analysis
    failingRiskCodeThreshold = 10         // default; build fails if a higher risk is found
    shouldRunAjaxSpider = false           // default
    shouldRunPassiveScanOnly = false      // default
    shouldStartNewSession = true          // default
    analysisTimeoutInMinutes = 480        // default
    spiderStartingPointUrl = null
    activeScanStartingPointUrl = null
    context = ['/app.*']
    technologies = ['Linux', 'MySQL']
    reportPath = file("$buildDir/zap-reports") // default

    // authentication (optional): 'http', 'form', 'cas', or 'selenium'
    authenticationType = 'form'
    loginUrl = 'http://localhost:8080/login'
    username = 'user'
    password = 'pass'
    usernameParameter = 'username'        // default
    passwordParameter = 'password'        // default
    loggedInRegex = null
    loggedOutRegex = null
    excludeFromScan = []
    protectedPages = []
    httpSessionTokens = []
    seleniumDriver = 'firefox'            // default: htmlunit, firefox, chrome, or phantomjs
    hostname = null                       // http auth only
    realm = null                          // http auth only
    authenticationPort = 80               // http auth only
}
```

Set `zap.skip = true` to disable all ZAP tasks (e.g. per-environment).

### Selenium-driven flow

Run `startZap` before your integration tests (so they can proxy through it), then
`zapSeleniumAnalyze` afterward to scan the recorded traffic:

```groovy
tasks.named('integrationTest') {
    dependsOn 'startZap'
    finalizedBy 'zapSeleniumAnalyze'
}
```

## Development

```shell
./gradlew build               # compile, test, assemble
./gradlew publishToMavenLocal  # try out a snapshot locally
```

## Releasing

Releases are cut via the [Create release](.github/workflows/release.yml) GitHub Actions
workflow (manual `workflow_dispatch`), which publishes to Maven Central and the Gradle Plugin
Portal and drafts a GitHub release with a generated changelog.

## License

[Apache License 2.0](LICENSE)
