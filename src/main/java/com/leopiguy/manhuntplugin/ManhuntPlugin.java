package com.leopiguy.manhuntplugin;

import com.leopiguy.manhuntplugin.commands.ManhuntCmd;
import com.leopiguy.manhuntplugin.commands.ManhuntTabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ManhuntPlugin extends JavaPlugin {

    private FileConfiguration config;

    @Override
    public void onEnable(){
        MainGameContainer mainGameContainer = new MainGameContainer(this);
        this.getCommand("manhunt").setExecutor(new ManhuntCmd());
        getServer().getPluginManager().registerEvents(new MainListener(this, mainGameContainer), this);
        this.getCommand("manhunt").setTabCompleter(new ManhuntTabCompleter(this));
        getLogger().info("Initialized!");
    }

    @Override
    public void onDisable() {
    }

    // TO-DO LIST

    // Remind players that the compass is or isn't updating (IMPL, NEED TO TEST) -- TESTED
    // Players that rejoin don't break the game (IMPL, NEED TO TEST) -- TESTED
    // Starting countdown (IMPL, NEED TO TEST) -- TESTED
    // Hold hunters in place while they disperse (IMPL, NEED TO TEST) -- TESTED
    // Blind and slowness (IMPL, NEED TO TEST) -- TESTED
    // Tab Completer (IMPL, NEED TO TEST) -- TESTED
    // Peaceful mode at beginning (IMPL, NEED TO TEST) -- TESTED
    // Time Set Day when the game starts (IMPL, NEED TO TEST) -- TESTED
    // Clear inventory, set gamemode, and teleport players to one spot (set spawn) (IMPL, NEED TO TEST) -- TESTED
    // Kill All Items (IMPL, NEED TO TEST) -- TESTED
    // Reset advancements (IMPL, NEED TO TEST) -- TESTED
    // Put people in teams (IMPL, NEED TO TEST) -- TESTED
    // Titlebar showing who is being hunted (IMPL, NEED TO TEST) -- TESTED
    // Clear Poeples names (IMPL, NEED TO TEST) -- TESTED
    // Add timer to bossbar -- TESTED
    // TODO: Right click compass to pick who is being tracked
    // TODO: End a game with selector
    // TODO: Add people mid-game
    // TODO: Config file
}
