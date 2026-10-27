repositories {
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:module-adventure"))
    compileOnly(project(":project:module-chat"))
    compileOnly(project(":project:module-compat"))
    compileOnly(project(":project:module-nms"))
    compileOnly("ink.ptms.core:v260100:260100")
    compileOnly("net.md-5:bungeecord-chat:1.21-R0.3")
    compileOnly(fileTree(rootDir.resolve("libs")))

    compileOnly("me.clip:placeholderapi:2.12.2") { isTransitive = false }
    compileOnly("com.discordsrv:discordsrv:1.26.0") { isTransitive = false }
    testRuntimeOnly("net.kyori:adventure-text-minimessage:4.26.1")
}

taboolib { subproject = true }

configurations.testImplementation { extendsFrom(configurations.compileOnly.get()) }

val chatCheck by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("me.arasple.mc.trchat.ChatCheckKt")
}
tasks.check { dependsOn(chatCheck) }
