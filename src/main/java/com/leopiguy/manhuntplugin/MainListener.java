package com.leopiguy.manhuntplugin;

import com.leopiguy.manhuntplugin.events.AddPlayerEvent;
import com.leopiguy.manhuntplugin.events.InitEvent;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Objects;

public class MainListener implements Listener {

    private ManhuntPlugin pluginRef;
    private MainGameContainer containerRef;

    public MainListener(ManhuntPlugin pluginRef, MainGameContainer containerRef) {
        this.pluginRef = pluginRef;
        this.containerRef = containerRef;
    }

    @EventHandler
    public void handleInitEvent(InitEvent event) {
        this.containerRef.initNewGame(event.getSender());
        event.setCancelled(true);
    }

    @EventHandler
    public void handleAddPlayerEvent(AddPlayerEvent event) {
        this.containerRef.triggerAddPlayer(event.getSender());
        event.setCancelled(true);
    }

    @EventHandler
    public void handleDeathEvent(PlayerDeathEvent event) {
        this.containerRef.handleDeath(event.getEntity());
    }

    @EventHandler
    public void handleRespawnEvent(PlayerRespawnEvent event) {
        this.containerRef.handleRespawn(event.getPlayer(), event);
    }

    @EventHandler
    public void handleGameEnd(EntityDeathEvent event) {
        if(event.getEntity() instanceof EnderDragon)
            this.containerRef.handleHuntedWin(Objects.requireNonNull(event.getEntity().getKiller()));
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        this.containerRef.handlePlayerRejoin(event.getPlayer());
    }
}
