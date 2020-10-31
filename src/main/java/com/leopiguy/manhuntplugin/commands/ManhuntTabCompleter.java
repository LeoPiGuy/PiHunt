package com.leopiguy.manhuntplugin.commands;

import com.leopiguy.manhuntplugin.ManhuntPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ManhuntTabCompleter implements TabCompleter {

    private ManhuntPlugin pluginRef;

    public ManhuntTabCompleter(ManhuntPlugin pluginRef) {
        this.pluginRef = pluginRef;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] arguments) {
        if(command.getName().equalsIgnoreCase("manhunt")) {
            if(arguments.length == 1) {
                List<String> args = new ArrayList<>();
                args.add("init");
                args.add("add");
                return args;
            }
            return new ArrayList<>();
        }
        return null;
    }
}
