import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import org.gradle.api.GradleException
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("io.freefair.lombok") version "6.6.3"
    id("org.springframework.boot") version "2.7.11"
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Spring boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("io.netty:netty-all")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.httpcomponents:httpclient")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    
    // Database
    runtimeOnly("com.mysql:mysql-connector-j:8.0.33")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.1.3")
    runtimeOnly("org.xerial:sqlite-jdbc:3.41.2.1")
    implementation("com.github.gwenn:sqlite-dialect:0.1.4")
    
    // JSR305 for nullable
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // Others
    implementation("commons-fileupload:commons-fileupload:1.5")
}

group = "icu.samnya"
version = "0.0.47a"
description = "Aqua Server"
java.sourceCompatibility = JavaVersion.VERSION_17

val buildTime: String by extra(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC")).format(Instant.now()))

tasks.processResources {
    filesMatching("**/application.properties") {
        expand(project.properties)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

val aquaViewerDistDir = file("../aqua-viewer/dist/aqua-viewer")
val embeddedWebStagingDir = layout.buildDirectory.dir("generated/aqua-viewer-web")

/**
 * Stages the AquaViewer build output under build/. The WebUI lives in the
 * aqua-viewer repository and is never committed here, so this staging directory
 * is the only way it ends up inside a jar.
 */
val syncAquaViewerUi by tasks.registering(Copy::class) {
    group = "build"
    description = "Copy AquaViewer build output into a staging directory for jar packaging."

    doFirst {
        if (!aquaViewerDistDir.exists()) {
            throw GradleException(
                "AquaViewer dist not found at ${aquaViewerDistDir.path}. " +
                        "Run 'npm run build' in ../aqua-viewer first, or use 'bootJar' for a jar without the WebUI."
            )
        }
        // Never let files of an older UI build survive in the staging directory.
        delete(embeddedWebStagingDir)
    }

    from(aquaViewerDistDir)
    into(embeddedWebStagingDir)
    includeEmptyDirs = false

    doLast {
        val stagedIndex = embeddedWebStagingDir.get().file("index.html").asFile
        if (stagedIndex.exists()) {
            val content = stagedIndex.readText(Charsets.UTF_8)
            if (content.contains("<base href=\"/\">")) {
                stagedIndex.writeText(content.replace("<base href=\"/\">", "<base href=\"/web/\">"), Charsets.UTF_8)
            }
        }
    }
}

// The WebUI is produced by ../aqua-viewer, so a stale copy sitting in
// src/main/resources/web must never sneak into the jar. It is only added back by
// bootJarWithUi, from the staging directory above.
tasks.withType<ProcessResources>().configureEach {
    exclude("web/**")
}

// Two packaging variants:
//   gradlew bootJar        - jar WITHOUT the WebUI, serve it from a 'web' folder
//   gradlew bootJarWithUi  - jar WITH the WebUI embedded under /web/
tasks.register("bootJarWithUi") {
    group = "build"
    description = "Build an executable jar with the AquaViewer UI embedded under /web/. Requires ../aqua-viewer/dist/aqua-viewer."
    dependsOn(syncAquaViewerUi)
    dependsOn(tasks.named("bootJar"))
}

tasks.named<BootJar>("bootJar") {
    description = "Build an executable jar without the AquaViewer UI."
    mustRunAfter(syncAquaViewerUi)
}

gradle.taskGraph.whenReady {
    if (allTasks.any { it.name == "bootJarWithUi" }) {
        tasks.named<BootJar>("bootJar").configure {
            from(embeddedWebStagingDir) {
                into("BOOT-INF/classes/web")
            }
            archiveClassifier.set("with-ui")
        }
    }
}
