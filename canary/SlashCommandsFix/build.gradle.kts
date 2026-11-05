import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

version = "8.18.1"
description = "Beta backport of SlashCommandsFix"

aliucord {
    changelog.set("""
        # 8.18.1
        * Fix localized commands and options failing ("interaction failed" / INTERACTION_APPLICATION_COMMAND_OPTION_INVALID): names are shown in your language but sent as the name_default Discord provides
        * Fix 429 Too Many Requests loops: honor Retry-After, keep using cached commands while rate limited, back off after failures
        * No more network requests while the command list is rendered; one request per command list at a time
        * Refresh command lists after 10 minutes (picks up user-installed app changes)

        # 8.18.0
        * Don't use custom props anymore (core has them)

        # 7.16.2
        * Use new props

        # 7.16.1
        * Prompt restarts

        # 7.16.0
        * Initial port >w< thanks @jedenastka
    """.trimIndent())

    deploy.set(true)
}

apply {
    plugin(libs.plugins.shadow.get().pluginId)
}

val shadowDir = File(buildDir, "intermediates/shadowed")

tasks.register<ShadowJar>("relocateJar") {
    val javaTask = tasks.findByName("compileDebugJavaWithJavac")!!
    val kotlinTask = tasks.findByName("compileDebugKotlin")!!
    from(javaTask.outputs, kotlinTask.outputs)
    relocate("com.aliucord.coreplugins.slashcommandsfix", "moe.lava.corenary.slashcommandsfix")
    archiveClassifier.set("shadowed")
    destinationDirectory.set(File(buildDir, "intermediates"))
}

tasks.register<Sync>("copyShadowed") {
    val reloc = tasks.findByName("relocateJar")!! as ShadowJar
    dependsOn(reloc)
    from(zipTree(reloc.archiveFile))
    into(shadowDir)
}

project.afterEvaluate {
    tasks.compileDex {
        val copyShadowed = tasks.findByName("copyShadowed")!! as Sync
        dependsOn(copyShadowed)
        input.setFrom(shadowDir)
    }
}
