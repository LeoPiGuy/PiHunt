package com.leopiguy.manhuntplugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class ManhuntGame {

    private boolean isHuntedInit;
    private boolean isHuntersInit;

    private HashMap<String, Player> huntedList;
    private HashMap<String, Player> huntersList;
    private HashMap<String, Location> lastKnownLocation;
    private HashMap<String, Boolean> isTrackingList;
    private BukkitTask coundownScheduler;
    private final ManhuntPlugin pluginRef;
    private Location spawn;
    private final String gameId;
    private Team huntedTeam;
    private Team hunterTeam;
    private BossBar hunterBar;
    private BossBar huntedBar;

    public ManhuntGame(ManhuntPlugin mainPluginRef, String uuid) {
        isHuntedInit = false;
        isHuntersInit = false;
        huntedList = new HashMap<>();
        huntersList = new HashMap<>();
        lastKnownLocation = new HashMap<>();
        isTrackingList = new HashMap<>();
        pluginRef = mainPluginRef;
        spawn = null;
        gameId = uuid;
        createTeams();
        hunterBar = null;
        huntedBar = null;
    }

    public boolean isHuntedInit() {
        return isHuntedInit;
    }

    public boolean isHuntersInit() {
        return isHuntersInit;
    }

    public void setHunted(ArrayList<Player> players) {
        for (Player player : players) {
            huntedList.put(player.getUniqueId().toString(), player);
        }
        isHuntedInit = true;
    }

    public void setHunters(ArrayList<Player> players) {
        for (Player player : players) {
            huntersList.put(player.getUniqueId().toString(), player);
            isTrackingList.put(player.getUniqueId().toString(), false);
        }
        isHuntersInit = true;
    }

    public HashMap<String, Player> getHuntedList() {
        return huntedList;
    }

    public HashMap<String, Player> getHuntersList() {
        return huntersList;
    }

    public void initialize(Player startingPlayer) {
        World world = startingPlayer.getWorld();
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(1000);
        World overworld = null;
        World nether = null;
        World end = null;

        if(world.getEnvironment() == World.Environment.NORMAL) {
            overworld = world;
            nether = Bukkit.getWorld(world.getName() + "_nether");
            end = Bukkit.getWorld(world.getName() + "_the_end");
        } else if(world.getEnvironment() == World.Environment.NETHER) {
            String name = world.getName().split("[_]")[0];
            overworld = Bukkit.getWorld(name);
            nether = world;
            end = Bukkit.getWorld(name + "_the_end");
        } else {
            String name = world.getName().split("[_]")[0];
            overworld = Bukkit.getWorld(name);
            nether = Bukkit.getWorld(name + "_nether");
            end = world;
        }

        if(overworld != null) for (Entity entity : overworld.getEntities()) if(entity instanceof Item) entity.remove();
        if(nether != null) for (Entity entity : nether.getEntities()) if(entity instanceof Item) entity.remove();
        if(end != null) for (Entity entity : end.getEntities()) if(entity instanceof Item) entity.remove();

        this.spawn = startingPlayer.getLocation();
        createBossbars();
        for (Player player : huntedList.values()) {
            setPlayerHuntedNames(player);
            this.setPlayerStartupConfig(player);
        }
        for (Player player : huntersList.values()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,60 * 20, 100));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,60 * 20, 250));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,59 * 20, 250));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,60 * 20, 100));
            setPlayerHuntedNames(player);
            this.setPlayerStartupConfig(player);
            giveCompass(player);
        }
        this.countdown(60);
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = compass.getItemMeta();
        assert meta != null;
        meta.setDisplayName(String.format("%sTracking Compass", ChatColor.GREEN));
        meta.setLore(new ArrayList<>(Collections.singletonList("Tracks nearest hunted player")));
        compass.setItemMeta(meta);
        player.setCompassTarget(getNearestHuntedLocation(player));
        player.getInventory().setItemInOffHand(compass);
    }

    private Location getNearestHuntedLocation(Player player) {
        Location currentPlayerLocation = player.getLocation();
        ArrayList<Location> locationArray = new ArrayList<>();
        for (Player p : huntedList.values()) {
            if (Objects.requireNonNull(p.getLocation().getWorld()).getEnvironment() == World.Environment.NORMAL) {
                lastKnownLocation.put(p.getUniqueId().toString(), p.getLocation());
                locationArray.add(p.getLocation());
            } else {
                locationArray.add(lastKnownLocation.get(p.getUniqueId().toString()));
            }
        }
        if(locationArray.size() == 1) return locationArray.get(0);
        if(locationArray.size() == 0) return Objects.requireNonNull(currentPlayerLocation.getWorld()).getSpawnLocation();

        Location closestLoc = null;
        double closestDist = Integer.MAX_VALUE;
        for (Location location : locationArray) {
            if(closestLoc == null) {
                closestLoc = location;
                closestDist = currentPlayerLocation.distanceSquared(location);
            }
            double dist = currentPlayerLocation.distanceSquared(location);
            if(dist < closestDist) {
                closestDist = dist;
                closestLoc = location;
            }
        }
        return closestLoc;
    }

    public void updateCompasses() {
        for (Player hunter : huntersList.values()) {
            if(hunter.getInventory().getItemInMainHand().getType() == Material.COMPASS || hunter.getInventory().getItemInOffHand().getType() == Material.COMPASS) {
                if(!isTrackingList.get(hunter.getUniqueId().toString())) {
                    hunter.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Tracker Updating...", net.md_5.bungee.api.ChatColor.GREEN));
                    isTrackingList.put(hunter.getUniqueId().toString(), true);
                }
                hunter.setCompassTarget(getNearestHuntedLocation(hunter));
            } else {
                if(isTrackingList.get(hunter.getUniqueId().toString())) {
                    hunter.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Tracker NOT Updating", net.md_5.bungee.api.ChatColor.RED));
                    isTrackingList.put(hunter.getUniqueId().toString(), false);
                }
            }
        }
    }

    public boolean handleDeath(Player player) {
        huntedList.remove(player.getUniqueId().toString());
        return huntedList.size() == 0;
    }

    public void handleRespawn(Player player, PlayerRespawnEvent event) {
        if(!event.isBedSpawn() && !event.getRespawnLocation().equals(spawn)) {
            event.setRespawnLocation(spawn);
            resetPlayerSpawn(player);
        }
        if(huntersList.containsKey(player.getUniqueId().toString())) giveCompass(player);
    }

    public void updatePlayer(Player player) {
        if(huntedList.replace(player.getUniqueId().toString(), player) != null) {
            setPlayerHuntedNames(player);
        }
        if(huntersList.replace(player.getUniqueId().toString(), player) != null) {
            setPlayerHunterNames(player);
        }
    }

    private void countdown(int seconds) {
        if(seconds == 0) {
            this.endCountdown();
            this.huntedBar.setProgress(1.0);
            return;
        }
        this.huntedBar.setProgress(((double)(60 - seconds))/60);
        for (Player player : huntersList.values()) {
            player.sendTitle(String.format("%sTHE HUNT WILL BEGIN IN", ChatColor.GOLD), String.format("%s%d seconds", ChatColor.GREEN, seconds),0,30,2);
        }

        this.coundownScheduler = Bukkit.getScheduler().runTaskLater(this.pluginRef, () -> countdown(seconds - 1), 20);
    }

    private void endCountdown() {
        Objects.requireNonNull(Bukkit.getWorld("world")).setDifficulty(Difficulty.EASY);
        for (Player player : huntersList.values()) {
            player.sendTitle(String.format("%sTHE HUNT HAS BEGUN!", ChatColor.GREEN), "",0,20,5);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1000f,1f);
        }
        for (Player player : huntedList.values())
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1000f,1f);
    }

    private void setPlayerStartupConfig(Player player) {
        player.setBedSpawnLocation(spawn, true);
        player.teleport(spawn);
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        Iterator<Advancement> advancementIterator = Bukkit.getServer().advancementIterator();
        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            Collection<String> awardedCriteria = player.getAdvancementProgress(advancement).getAwardedCriteria();
            for (String awardedCriterion : awardedCriteria)
                player.getAdvancementProgress(advancement).revokeCriteria(awardedCriterion);
        }
    }

    private void resetPlayerSpawn(Player player) {
        player.setBedSpawnLocation(spawn, true);
    }

    private void createTeams() {
        String huntedTeamName = String.format("Hunted_%s", gameId.split("[-]")[0]);
        String hunterTeamName = String.format("Hunters_%s", gameId.split("[-]")[0]);
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team oldHunterTeam = scoreboard.getTeam(hunterTeamName);
        Team oldHuntedTeam = scoreboard.getTeam(huntedTeamName);
        hunterTeam = (oldHunterTeam == null) ? scoreboard.registerNewTeam(hunterTeamName) : oldHunterTeam;
        huntedTeam = (oldHuntedTeam == null) ? scoreboard.registerNewTeam(huntedTeamName) : oldHuntedTeam;

        hunterTeam.setColor(ChatColor.RED);
        hunterTeam.setPrefix(String.format("%s[%sHunter%s] %s", ChatColor.DARK_RED, ChatColor.RED, ChatColor.DARK_RED, ChatColor.RESET));
        hunterTeam.setDisplayName(String.format("%sHunter%s", ChatColor.DARK_RED, ChatColor.RESET));
        hunterTeam.setAllowFriendlyFire(true);

        huntedTeam.setColor(ChatColor.DARK_GREEN);
        huntedTeam.setPrefix(String.format("%s[%sHunted%s] %s", ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.RESET));
        huntedTeam.setDisplayName(String.format("%sHunted%s", ChatColor.GREEN, ChatColor.RESET));
        huntedTeam.setAllowFriendlyFire(true);
    }

    public void endGame() {
        if(hunterTeam != null) hunterTeam.unregister();
        if(huntedTeam != null) huntedTeam.unregister();
        if(huntedBar != null) huntedBar.removeAll();
        if(hunterBar != null) hunterBar.removeAll();
    }

    private void createBossbars() {
        String huntedNames = "";
        int entriesLeft = huntedList.size();
        if(entriesLeft == 0) return;
        if(entriesLeft == 1) huntedNames = huntedList.values().iterator().next().getDisplayName();
        if(entriesLeft > 1) {
            boolean isFirst = true;
            for( Player player : huntedList.values()) {
                if(entriesLeft == 1) {
                    huntedNames = huntedNames.concat(" and " + player.getName());
                    break;
                }
                if(isFirst) {
                    isFirst = false;
                    huntedNames = player.getName();
                } else huntedNames = huntedNames.concat(", " + player.getName());
                entriesLeft--;
            }
        }

        hunterBar = Bukkit.createBossBar("You are hunting " + huntedNames, BarColor.RED, BarStyle.SOLID);
        huntedBar = Bukkit.createBossBar("You are being hunted!", BarColor.RED, BarStyle.SOLID);
        huntedBar.setProgress(0);
    }

    public void setPlayerHuntedNames(Player player) {
        player.setDisplayName(String.format("%s[%sHunted%s] %s%s%s", ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.DARK_GREEN, player.getName(), ChatColor.RESET));
        huntedTeam.addEntry(player.getName());
        huntedBar.addPlayer(player);
    }
    public void setPlayerHunterNames(Player player) {
        player.setDisplayName(String.format("%s[%sHunter%s] %s%s%s", ChatColor.DARK_RED, ChatColor.RED, ChatColor.DARK_RED, ChatColor.RED, player.getName(), ChatColor.RESET));
        hunterTeam.addEntry(player.getName());
        hunterBar.addPlayer(player);
    }
}
