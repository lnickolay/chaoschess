plugins {
	java
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.chaoschess"
version = "0.0.1-SNAPSHOT"
description = "Checkmate, Atheists!"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// die Reihenfolge, in der die Dependencies angegeben werden, ist egal
dependencies {
    // automatisch von Spring Boot erstellte Einträge
	implementation("org.springframework.boot:spring-boot-starter-web")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // manuell hinzugefügte WebSocket-Starter-Dependency
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    // manuell hinzugefügter Eintrag für Actuator (genutzt zum Debugging, konkret zum Abrufen von Thread-Infos via REST)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // manuell hinzugefügter Eintrag der spezifischen Dependency für Parameterized Tests
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
