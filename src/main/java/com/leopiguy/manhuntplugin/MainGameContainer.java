package com.leopiguy.manhuntplugin;

import com.leopiguy.manhuntplugin.util.GamePickerGUI;
import com.leopiguy.manhuntplugin.util.PlayerPickerGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static org.bukkit.Bukkit.*;

public class MainGameContainer {

    private HashMap<String, Player> playersInAGame;
    private HashMap<String, ManhuntGame> gameList;
    private HashMap<String, String> playerGameMap;
    private ManhuntPlugin pluginRef;
    private boolean isGameRunning;
    private BukkitTask gameScheduler;

    public MainGameContainer(ManhuntPlugin pluginRef) {
        playersInAGame = new HashMap<>();
        gameList = new HashMap<>();
        playerGameMap = new HashMap<>();
        this.pluginRef = pluginRef;
        this.isGameRunning = false;
    }

    public void initNewGame(CommandSender creator) {
        Collection<? extends Player> onlinePlayers = getServer().getOnlinePlayers();
        ArrayList<Player> availablePlayers = new ArrayList<>();
        for (Player onlinePlayer : onlinePlayers)
            if(!isInGame(onlinePlayer)) availablePlayers.add(onlinePlayer);
        String gameId = UUID.randomUUID().toString();
        gameList.put(gameId, new ManhuntGame(this.pluginRef, gameId));
        PlayerPickerGUI mainPicker = new PlayerPickerGUI("Select the hunted player(s)", availablePlayers, gameId, this);
        getServer().getPluginManager().registerEvents(mainPicker, pluginRef);
        mainPicker.openInventory(creator);
    }

    private boolean isInGame(Player player) {
        return playersInAGame.containsKey(player.getUniqueId().toString());
    }

    public void initCanceled(String gameId, PlayerPickerGUI guiRef, Player guiHolder) {
        HandlerList.unregisterAll(guiRef);
        endGame(gameId);
        guiHolder.sendMessage(String.format("%sGame initiation canceled.", ChatColor.RED));
    }

    public void guiConfirm(ArrayList<Player> selectedPlayers, String gameId, PlayerPickerGUI guiRef, Player guiHolder) {
        ManhuntGame game = gameList.get(gameId);
        if(game != null) {
            if(!game.isHuntedInit()) {
                game.setHunted(selectedPlayers);
                this.registerPlayers(selectedPlayers, gameId);
                guiRef.refreshGui("Select the hunter(s)", selectedPlayers);
                return;
            } else {
                if(game.isHuntersInit()) {
                    guiHolder.sendMessage(String.format("%sGame has already been initialized.", ChatColor.RED));
                } else {
                    game.setHunters(selectedPlayers);
                    this.registerPlayers(selectedPlayers, gameId);
                    game.initialize(guiHolder);
                    this.initialize();
                    guiHolder.sendMessage(String.format("%sGame initialized!", ChatColor.GREEN));
                }
            }
        } else {
            guiHolder.sendMessage(String.format("%sGame initiation failed. Please try again.", ChatColor.RED));
        }
        HandlerList.unregisterAll(guiRef);
        guiHolder.closeInventory();
    }

    private void registerPlayers(ArrayList<Player> players, String gameId) {
        for (Player player : players) {
            playersInAGame.put(player.getUniqueId().toString(), player);
            playerGameMap.put(player.getUniqueId().toString(),gameId);
        }
    }

    private void initialize() {
        if(this.isGameRunning) return;
        this.isGameRunning = true;
        this.scheduler();
    }

    private void scheduler() {
        if(!isGameRunning) return;
        try {
            for (ManhuntGame game : gameList.values()) {
                game.updateCompasses();
            }
        } catch (IllegalArgumentException e) {}
        this.gameScheduler = Bukkit.getScheduler().runTaskLater(pluginRef, this::scheduler, 5);
    }

    public void handleDeath(Player player) {
        String gameId = playerGameMap.get(player.getUniqueId().toString());
        if(gameId == null) return;
        ManhuntGame game = gameList.get(gameId);
        if(game.handleDeath(player)) {
            broadcastMessage(String.format("%sThe Hunters have won the game! Congratulations!", ChatColor.GOLD));
            this.endGame(playerGameMap.get(player.getUniqueId().toString()));
        }
    }

    public void handleRespawn(Player player, PlayerRespawnEvent event) {
        String gameId = playerGameMap.get(player.getUniqueId().toString());
        if(gameId == null) return;
        ManhuntGame game = gameList.get(gameId);
        game.handleRespawn(player, event);
    }
    private void endGame(String gameId) {
        HashMap<String, String> clonedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : playerGameMap.entrySet()) {
            clonedMap.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : clonedMap.entrySet()) {
            if(entry.getValue().equals(gameId)) {
                playerGameMap.remove(entry.getKey());
                Player player = playersInAGame.get(entry.getKey());
                player.setDisplayName(player.getName());
                playersInAGame.remove(entry.getKey());
            }
        }
        gameList.get(gameId).endGame();
        gameList.remove(gameId);
        if(gameList.size() == 0) isGameRunning = false;
    }

    public void handleHuntedWin(Player killer) {
        broadcastMessage(String.format("%sThe Hunted have won the game! Congratulations!", ChatColor.GOLD));
        String gameId = playerGameMap.get(killer.getUniqueId().toString());
        if(gameId == null) return;
        this.endGame(playerGameMap.get(killer.getUniqueId().toString()));
    }

    public void handlePlayerRejoin(Player player) {
        String uuid = player.getUniqueId().toString();
        String gameId = playerGameMap.get(uuid);
        if(gameId == null) return;
        playersInAGame.replace(uuid, player);
        ManhuntGame game = gameList.get(gameId);
        game.updatePlayer(player);
    }

    public void triggerAddPlayer(CommandSender sender) {
        Collection<? extends Player> onlinePlayers = getServer().getOnlinePlayers();
        ArrayList<Player> availablePlayers = new ArrayList<>();
        for (Player onlinePlayer : onlinePlayers)
            if(!isInGame(onlinePlayer)) availablePlayers.add(onlinePlayer);
        GamePickerGUI gamePicker = new GamePickerGUI("Select the game to add players to", gameList, this, (holder, gameId) -> triggerAddPlayerPicker(holder, gameId, availablePlayers), (player) -> player.sendMessage(String.format("%sAdding player canceled.", ChatColor.RED)));
        getServer().getPluginManager().registerEvents(gamePicker, pluginRef);
        gamePicker.openInventory(sender);
    }

    public void triggerAddPlayerPicker(Player holder, String gameId, ArrayList<Player> availablePlayers) {

    }

    public void addPlayer(Player player, String gameId) {

    }
}