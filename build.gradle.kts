import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.12"
  kotlin("plugin.spring") version "2.4.21"
  kotlin("plugin.jpa") version "2.4.21"
  kotlin("jvm") version "2.4.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.jetbrains.kotlin.plugin.allopen") version "2.4.21"
  id("org.owasp.dependencycheck") version "12.2.2"
}

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  implementation { exclude(module = "applicationinsights-spring-boot-starter") }
  implementation { exclude(module = "applicationinsights-logging-logback") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

// fix to prevent the mismatch of kotlin versions for detekt
configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("2.0.21")
    }
  }
}

allOpen {
  annotations("jakarta.persistence.Entity")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20260101.1")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.1")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.1.1")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.security:spring-security-access")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.11.0")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.14")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mock-server:mockserver-netty-no-dependencies:9.1.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("io.mockk:mockk:1.14.11")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
    compileKotlin {
      compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
      }
    }
    compileTestKotlin {
      compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
      }
    }
  }
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}

detekt {
  toolVersion = "1.23.8"
  config.setFrom("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
}
// pin version of ktlint as HMPPS gradle orb 6.1.2 brings in dependency with older, breaking version. Remove when updated.
ktlint {
  version.set("1.4.1")
}
