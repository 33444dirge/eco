package com.willfp.eco.internal.spigot.data

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.util.StringUtils
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

/**
 * /ecocleaner <enable|disable|status>
 *
 * Toggles [PlayerBlockCleaner] at runtime. The toggle isn't written to config.yml, so a restart
 * goes back to `clean-stale-block-markers`.
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
                sender.send("&aStale block marker cleaner enabled. Chunks will be cleaned as they load.")
            }

            "disable" -> {
                cleaner.isEnabled = false
                sender.send("&cStale block marker cleaner disabled.")
            }

            "status" -> {
                val (removed, scanned) = cleaner.totals
                val state = if (cleaner.isEnabled) "&aenabled" else "&cdisabled"
                sender.send("&fCleaner is $state&f. Since startup: &e$removed&f markers removed from &e$scanned&f chunks.")
            }

            else -> sender.send("&fUsage: &e/ecocleaner <enable|disable|status>")
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
        val OPTIONS = listOf("enable", "disable", "status")
    }
}
