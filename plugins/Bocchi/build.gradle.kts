version = "1.0.5"
description = "More lenient message grouping"

android {
    namespace = "moe.lava.awoocord.bocchi"
}

aliucord {
    // Changelog of your plugin
    changelog.set("""
        # 1.0.5
        * Don't clump messages between a new day (midnight)

        # 1.0.4
        * Don't clump forwarded messages to avoid ambiguity

        # 1.0.3
        * Clump more than 6 messages together

        # 1.0.2
        * Fix (inverted) webhook clumping

        # 1.0.1
        * Hide blank space w.r.t attachments and embeds

        # 1.0.0
        * Initial release >w<
    """.trimIndent())

    deploy.set(true)
}
