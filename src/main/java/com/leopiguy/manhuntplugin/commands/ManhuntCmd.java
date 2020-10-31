package com.leopiguy.manhuntplugin.commands;

import com.leopiguy.manhuntplugin.events.AddPlayerEvent;
import com.leopiguy.manhuntplugin.events.InitEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ManhuntCmd implements CommandExecutor {

    // /manhunt init

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(args.length == 0) return false;
        switch(args[0]) {
            case "init":
                InitEvent initEvent = new InitEvent(sender);
                Bukkit.getPluginManager().callEvent(initEvent);
                break;
            case "add":
                AddPlayerEvent addPlayerEvent = new AddPlayerEvent(sender);
                Bukkit.getPluginManager().callEvent(addPlayerEvent);
                break;
            default:
                sender.sendMessage(String.format("%sInvalid parameter. Please check /help manhunt for more information.", ChatColor.RED));
        }
        return true;
    }

}
