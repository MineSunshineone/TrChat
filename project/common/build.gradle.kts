dependencies {
    compileOnly(project(":project:module-chat"))
    compileOnly("com.eatthepath:fast-uuid:0.2.0")
    testImplementation(kotlin("stdlib"))
}

val roomCheck by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("me.arasple.mc.trchat.util.proxy.ChatRoomsCheckKt")
}
tasks.check { dependsOn(roomCheck) }

taboolib { subproject = true }
