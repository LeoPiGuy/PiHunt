package com.leopiguy.manhuntplugin.events;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AddPlayerEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled;
    private CommandSender sender;


    public AddPlayerEvent(CommandSender sender) {
        this.sender = sender;
        this.isCancelled = false;
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public HandlerList getHandlers() {
        return this.HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
