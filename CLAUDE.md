# CLAUDE.md

Guidance for Claude Code (or any contributor) working in this repo.

## What this is

A Gradle plugin (`net.continuous-security-tools.zap-gradle`) intended to run OWASP ZAP
security scans as part of a Gradle build. It is the Gradle counterpart to
[zap-maven-plugin](https://github.com/ContinuousSecurityTooling/zap-maven-plugin).

[`ZapPlugin`](src/main/groovy/net/continuoussecuritytools/zap/ZapPlugin.groovy) registers a
`zap {}` extension and three tasks (`startZap`, `zapAnalyze`, `zapSeleniumAnalyze`) that mirror
the Maven plugin's `startZap`/`analyze`/`seleniumAnalyze` goals — see [README.md](README.md) for
user-facing usage. Build, publishing, and release pipeline are fully wired up and working.

## Build

```shell
./gradlew build              # compile, test, assemble, validatePlugins
./gradlew publishToMavenLocal  # install a SNAPSHOT locally for testing consumption
```

Source is Groovy, under `src/main/groovy` (the `groovy` plugin is applied, not `java`).

## ZAP integration architecture

The plugin does not reimplement any ZAP logic — it's a thin Gradle wrapper around the
`net.continuous-security-tools:zap-utils` / `zap-client-api` / `zap-report-parser` libraries
(the `net.cst.zap.*` packages; see [Dependencies](#dependencies-on-zap-java) below), the same
libraries `zap-maven-plugin`'s Mojos call directly. When extending plugin behavior, check what
[`ZapMojo`/`AnalyzeMojo`/`StartZapMojo`/`SeleniumAnalyzeMojo`](https://github.com/ContinuousSecurityTooling/zap-maven-plugin/tree/develop/src/main/java/net/cst/zap/maven)
do first — the Gradle side should stay behaviorally equivalent.

- [`ZapPluginExtension`](src/main/groovy/net/continuoussecuritytools/zap/ZapPluginExtension.groovy)
  holds all `zap {}` config (plain mutable Groovy properties, not the lazy `Property<T>` API —
  intentionally kept simple since this isn't aiming for configuration-cache support yet) and the
  `buildZapInfo()` / `buildAuthenticationInfo()` / `buildAnalysisInfo()` builder methods, mirroring
  `ZapMojo`'s protected builder methods. `targetUrl` and `zapPort` are validated as required at
  build-time (throws `GradleException`) since Gradle extensions don't have Maven's
  `@Parameter(required = true)` binding-time validation.
- `StartZapTask`, `ZapAnalyzeTask`, `ZapSeleniumAnalyzeTask` each hold a `zapExtension` field
  (type `ZapPluginExtension`, `@Internal`) **set once by `ZapPlugin.apply()` at configuration
  time** — do not fetch the extension via `project.extensions.getByType(...)` from inside
  `@TaskAction` methods or `onlyIf` closures. `Task.getProject()` at execution time is deprecated
  in Gradle 8/9 (breaks configuration-cache compatibility, hard error in Gradle 10) and nebula-test's
  `IntegrationTestKitSpec` fails builds on deprecation warnings by default — this was caught by the
  functional tests, not by `./gradlew build` alone.
- `zap.skip` is enforced via `onlyIf { !zapExtension.skip }` on each task (not an early-return
  inside the task action) so `SKIPPED` shows correctly in Gradle's task outcome reporting.

### Dependencies on zap-java

`build.gradle` pulls in `net.continuous-security-tools:zap-utils`,
`net.continuous-security-tools:zap-client-api`, and `net.continuous-security-tools:zap-report-parser`
at a shared `zapJavaVersion` (`ext.zapJavaVersion` in build.gradle). **Note:** `zap-maven-plugin`'s
`pom.xml` pins these to `1.0.11`, but that version was never published — verify against
`https://search.maven.org/solrsearch/select?q=g:net.continuous-security-tools` before bumping;
`0.4.1` was the latest actually-published version as of 2026-07.

## Coordinates

- Group: `net.continuous-security-tools`
- Artifact: `zap-gradle-plugin`
- Plugin id: `net.continuous-security-tools.zap-gradle`
- Implementation class: `net.continuoussecuritytools.zap.ZapPlugin`

Note the group uses a hyphen (`continuous-security-tools`), but the Java package name can't
contain one, hence `net.continuoussecuritytools.zap` (no hyphen) for the actual class package.

## Publishing setup

Publishes to **two independent destinations**, both configured in [build.gradle](build.gradle):

1. **Maven Central**, via the `com.vanniktech.maven.publish` plugin (`mavenPublishing {}`
   block), configured with `GradlePublishPlugin()` since `com.gradle.plugin-publish` is also
   applied — that combination is what makes vanniktech emit sources/javadoc jars and wire the
   plugin marker publication correctly. Do not add `withSourcesJar()`/`withJavadocJar()` to the
   `java {}` block — vanniktech's `GradlePublishPlugin()` already registers those tasks, and
   doing both causes duplicate task registration.
   - Auth: `ORG_GRADLE_PROJECT_mavenCentralUsername` / `ORG_GRADLE_PROJECT_mavenCentralPassword`
     (a Central Portal user token, not your login password — generate at
     [central.sonatype.com](https://central.sonatype.com)).
   - Signing: `ORG_GRADLE_PROJECT_signingInMemoryKey` (ASCII-armored private key) +
     `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`. No keyring import needed — Gradle's
     `signing` plugin consumes the armored key directly in memory.
   - The old `oss.sonatype.org` (legacy OSSRH) endpoint is dead; don't reintroduce it.
2. **Gradle Plugin Portal**, via `com.gradle.plugin-publish` (`gradlePlugin {}` block) +
   `./gradlew publishPlugins`.
   - Auth: `GRADLE_PUBLISH_KEY` / `GRADLE_PUBLISH_SECRET` env vars, or `gradle.publish.key` /
     `gradle.publish.secret` in `~/.gradle/gradle.properties`. A local `.env` (gitignored) holds
     these for convenience — it is not consumed automatically by Gradle, it's just where the
     values live locally.

`jcenter()` is gone from this repo (shut down in 2021) — repositories use `mavenCentral()`.

## Versioning

`version` in build.gradle defaults to `0.0.1-SNAPSHOT`, but reads
`findProperty('releaseVersion')` first:

```groovy
version = findProperty('releaseVersion') ?: '0.0.1-SNAPSHOT'
```

This exists specifically so [release.yml](.github/workflows/release.yml) can pass
`-PreleaseVersion=X.Y.Z` without editing/committing a version bump into build.gradle (unlike the
Maven sibling project, which uses `maven-release-plugin` to mutate and commit `pom.xml`). Local
and CI builds without that property stay on the `-SNAPSHOT` version.

## Release workflow

[.github/workflows/release.yml](.github/workflows/release.yml) is manually triggered
(`workflow_dispatch`, `releaseversion` input) and:

1. Mints a GitHub App token (`vars.CI_APP_ID` / `secrets.CI_PRIVATE_KEY`) for bot-authored git
   operations — same app used by `zap-maven-plugin`.
2. Runs `publishAndReleaseToMavenCentral publishPlugins -PreleaseVersion=...`.
3. Generates changelog entries with `git-cliff` (`cliff.toml`), commits `CHANGELOG.md`, tags
   `vX.Y.Z`, pushes.
4. Drafts a GitHub release via `avakar/tag-and-release`.

`pipeline.yml` is the separate, unrelated CI workflow that just runs `./gradlew build` on every
push.

## Testing

Two test specs under `src/test/groovy`, both Spock:

- [`ZapPluginExtensionSpec`](src/test/groovy/net/continuoussecuritytools/zap/ZapPluginExtensionSpec.groovy) —
  unit tests for the extension's builder/validation logic, using `ProjectBuilder` (from
  `gradleTestKit()`) to construct a throwaway `Project` without a full build.
- [`ZapPluginFunctionalSpec`](src/test/groovy/net/continuoussecuritytools/zap/ZapPluginFunctionalSpec.groovy) —
  applies the plugin in a real (temp) build via nebula-test's `IntegrationTestKitSpec`
  (`runTasks` / `runTasksAndFail`), verifying task registration, `zap.skip`, and required-property
  validation. Deliberately doesn't exercise `zapAnalyze`/`zapSeleniumAnalyze` against a real ZAP
  instance — no ZAP binary in CI — only the failure paths that don't need one.

**Gotcha:** Gradle's `test` task defaults to JUnit4 detection, but Spock 2.x is JUnit
Platform-based. Without `test { useJUnitPlatform() }` in build.gradle, `./gradlew test` reports
`BUILD SUCCESSFUL` while silently running **zero** tests — check `build/test-results/test/*.xml`
for actual test counts if a change to this area seems suspiciously green.

**Gotcha (Renovate):** `nebula-test:10.6.2` used to bring in `spock-core`/`spock-junit4`
transitively. Starting with `nebula-test:12.x`, its Gradle module metadata dropped Spock entirely
(only `assertj-core`, `jspecify`, `junit-platform-launcher` remain) even though
`IntegrationTestKitSpec` still extends `spock.lang.Specification` — a Renovate bump to 12.x alone
breaks `compileTestGroovy` with an unresolved `spock.lang.Specification` symbol. Fixed by declaring
`org.spockframework:spock-core` and `spock-junit4` explicitly as `testImplementation`, pinned to
`2.3-groovy-3.0` (must match the Groovy line Gradle bundles — check `./gradlew --version`; Gradle
8.14.5 bundles Groovy 3.0.25). If a future Gradle bump moves to Groovy 4.x, this pin needs to move
to a `-groovy-4.0` Spock build too.

`gradleTestKit()` is declared explicitly as `testImplementation` (not left to
`java-gradle-plugin`'s auto-wiring) since it's needed for both `ProjectBuilder` (unit tests) and
`GradleRunner` (functional tests, via nebula-test).
