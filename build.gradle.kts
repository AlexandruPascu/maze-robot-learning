import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    application
}

group = "io.github.alexandrupascu"
version = "1.0.0"

repositories {
    mavenCentral()
}

// The trained exploration weights ship with the program, next to the class that loads them. Only
// that file is packaged, so other files under models/ never end up in the jar.
tasks.processResources {
    from("models") {
        include("explorer.weights")
        into("io/github/alexandrupascu/maze/learning")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "io.github.alexandrupascu.maze.cli.Main"
    applicationName = "maze-robot"
}

// `run` pipes the program's output through Gradle, hiding the terminal from it; this tells `show`
// that colour will probably still reach one. NO_COLOR=1 or --color never turn it off.
tasks.named<JavaExec>("run") {
    environment("MAZE_ROBOT_VIA_GRADLE", "true")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
