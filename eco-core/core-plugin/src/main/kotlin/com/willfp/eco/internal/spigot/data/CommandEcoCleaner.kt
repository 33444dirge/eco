package com.willfp.eco.internal.spigot.data

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.util.StringUtils
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

/**
 * /ecocleaner <enable|disable|clean|status>
 *
 * Toggles [PlayerBlockCleaner] at runtime or manually triggers full cleanup.
 * The toggle isn't written to config.yml, so a restart goes back to `clean-stale-block-markers`.
 */
class CommandEcoCleaner(
    plugin: EcoPlugin,
    private val cleaner: PlayerBlockCleaner
) : PluginCommand(
    plugin,
    "ecocleaner",
    "eco.command.ecocleaner",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        when (args.firstOrNull()?.lowercase()) {
            "enable" -> {
                cleaner.isEnabled = true
                sender.send("&aAutomatic PDC cleaner enabled. Chunks will be cleaned as they load.")
            }

            "disable" -> {
                cleaner.isEnabled = false
                sender.send("&cAutomatic PDC cleaner disabled.")
            }

            "clean" -> {
                sender.send("&eStarting manual cleanup of all loaded chunks...")
                var totalCleaned = 0
                var totalChunks = 0

                for (world in plugin.server.worlds) {
                    for (chunk in world.loadedChunks) {
                        totalChunks++
                        totalCleaned += cleaner.clean(chunk)
                    }
                }

                sender.send("&aCleaned &e$totalCleaned&a markers from &e$totalChunks&a loaded chunks.")
            }

            "status" -> {
                val (removed, scanned) = cleaner.totals
                val state = if (cleaner.isEnabled) "&aenabled" else "&cdisabled"
                sender.send("&fAutomatic cleaner is $state&f.")
                sender.send("&fSince startup: &e$removed&f markers removed from &e$scanned&f chunks.")
            }

            else -> sender.send("&fUsage: &e/ecocleaner <enable|disable|clean|status>")
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size != 1) {
            return emptyList()
        }

        return StringUtil.copyPartialMatches(args[0], OPTIONS, mutableListOf())
    }

    private fun CommandSender.send(message: String) {
        this.sendMessage(StringUtils.format(message))
    }

    private companion object {
        val OPTIONS = listOf("enable", "disable", "clean", "status")
    }
}
