package com.leopiguy.manhuntplugin.util;

import com.leopiguy.manhuntplugin.MainGameContainer;
import com.leopiguy.manhuntplugin.ManhuntGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GamePickerGUI implements Listener {

    private Inventory gui;
    private ItemStack selectedGame;
    private final int numRows;
    private Player holdingPlayer;
    private final MainGameContainer containerRef;
    private boolean isOpen;
    private boolean isCanceled;
    private boolean isRefresh;
    private final BiConsumer<Player, String> callback;
    private final Consumer<Player> cancelCallback;

    public GamePickerGUI(String name, HashMap<String, ManhuntGame> gameList, MainGameContainer containerRef, BiConsumer<Player, String> callback, Consumer<Player> cancelCallback) {
        this.containerRef = containerRef;
        this.callback = callback;
        this.cancelCallback = cancelCallback;
        this.selectedGame = null;
        this.holdingPlayer = null;
        this.isCanceled = true;
        this.isOpen = false;
        this.isRefresh = false;
        int numRows = gameList.size() / 9;
        if (gameList.size() % 9 != 0) numRows++;
        this.gui = Bukkit.createInventory(null, (numRows + 1) * 9, name);
        this.numRows = numRows;
        initGames(gameList);
        initCancel();
    }

    public void openInventory(CommandSender sender) {
        Player player = (Player) sender;
        this.holdingPlayer = player;
        this.isCanceled = false;
        this.isOpen = true;
        player.openInventory(gui);
    }

    private void initCancel() {

        final int cancelSlot = (this.numRows * 9) + 4;
        final ItemStack cancelItem = new ItemStack(Material.BARRIER, 1);
        final ItemMeta cancelMeta = cancelItem.getItemMeta();

        assert cancelMeta != null;

        cancelMeta.setDisplayName(ChatColor.RED + "Cancel Game Selection");
        cancelItem.setItemMeta(cancelMeta);

        gui.setItem(cancelSlot, cancelItem);
    }

    private void initGames(HashMap<String, ManhuntGame> gameList) {
        int i = 0;

        for (Map.Entry<String, ManhuntGame> game : gameList.entrySet()) {
            gui.addItem(createCompass(game, ++i));
        }
    }

    private ItemStack createCompass(Map.Entry<String, ManhuntGame> game, int num) {
        final ItemStack compass = new ItemStack(Material.COMPASS, 1);
        final ItemMeta itemMeta = compass.getItemMeta();

        assert itemMeta != null;

        itemMeta.setDisplayName(String.format("%sGame %d", ChatColor.BLUE, num));
        ArrayList<String> lore = new ArrayList<>();
        lore.add(String.format("%sClick to select!", ChatColor.GREEN));
        lore.add(String.format("%s-----------", ChatColor.GRAY));
        for (Player player : game.getValue().getHuntersList().values()) {
            lore.add(player.getDisplayName());
        }
        lore.add(String.format("%s-----------", ChatColor.GRAY));
        for (Player player : game.getValue().getHuntedList().values()) {
            lore.add(player.getDisplayName());
        }
        lore.add(String.format("%s%s", ChatColor.GRAY, game.getKey()));
        itemMeta.setLore(lore);
        compass.setItemMeta(itemMeta);

        return compass;
    }

    private void initSelectors() {
        final int row = this.numRows + 1;
        final int confirmSlot = (row * 9) - 1;
        final int deselectSlot = (row - 1) * 9;
        final ItemStack confirmItem = new ItemStack(Material.GREEN_CONCRETE, 1);
        final ItemStack deselectItem = new ItemStack(Material.RED_CONCRETE, 1);
        final ItemMeta confirmMeta = confirmItem.getItemMeta();
        final ItemMeta deselectMeta = deselectItem.getItemMeta();

        assert confirmMeta != null;
        assert deselectMeta != null;

        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Selection");
        confirmItem.setItemMeta(confirmMeta);

        deselectMeta.setDisplayName(ChatColor.RED + "Deselect Selection");
        deselectItem.setItemMeta(deselectMeta);

        gui.setItem(confirmSlot, confirmItem);
        gui.setItem(deselectSlot, deselectItem);
    }

    private void deleteSelectors() {
        final int row = this.numRows + 1;
        final int confirmSlot = (row * 9) - 1;
        final int deselectSlot = (row - 1) * 9;

        gui.removeItem(gui.getItem(confirmSlot));
        gui.removeItem(gui.getItem(deselectSlot));
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getInventory() == gui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getInventory() != gui) return;
        event.setCancelled(true);

        final ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        switch (clickedItem.getType()) {
            case RED_CONCRETE:
                deselectAll();
                break;
            case GREEN_CONCRETE:
                confirm();
                break;
            case BARRIER:
                earlyClose();
                break;
            case COMPASS:
                if(isSelected(clickedItem)) deselect(clickedItem);
                else select(clickedItem);
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if(event.getInventory() != gui) return;
        earlyClose();
    }

    private void deselectAll() {
        for (ItemStack item : gui.getContents()) {
            if(selectedGame.equals(item)) {
                deselect(item);
            }
        }
    }

    private void confirm() {
        this.isCanceled = true;
        List<String> lore = Objects.requireNonNull(this.selectedGame.getItemMeta()).getLore();
        assert lore != null;
        int length = lore.size();
        this.callback.accept(this.holdingPlayer, lore.get(length - 1));
        if(!isOpen) return;
        isOpen = false;
        holdingPlayer.closeInventory();
    }

    private void earlyClose() {
        if(isCanceled) return;
        isCanceled = true;
        this.cancelCallback.accept(holdingPlayer);
        if(!isOpen) return;
        isOpen = false;
        holdingPlayer.closeInventory();
    }

    private boolean isSelected(ItemStack item) {
        return selectedGame.equals(item);
    }

    private void deselect(ItemStack item) {
        for (ItemStack content : gui.getContents()) {
            if(content.isSimilar(item)) {
                ItemMeta oldMeta = content.getItemMeta();
                assert oldMeta != null;
                oldMeta.setDisplayName(ChatColor.RED + ChatColor.stripColor(oldMeta.getDisplayName()));
                List<String> newLore = oldMeta.getLore();
                assert newLore != null;
                newLore.set(0, String.format("%sClick to select!", ChatColor.GREEN));
                oldMeta.setLore(newLore);
                content.setItemMeta(oldMeta);
                selectedGame = null;
                deleteSelectors();
                break;
            }
        }
    }

    private void select(ItemStack item) {
        for (ItemStack content : gui.getContents()) {
            if(content == null) continue;
            if(content.isSimilar(item)) {
                ItemMeta oldMeta = content.getItemMeta();
                assert oldMeta != null;
                oldMeta.setDisplayName(String.format("%s%s%s", ChatColor.AQUA, ChatColor.BOLD, ChatColor.stripColor(oldMeta.getDisplayName())));
                List<String> newLore = oldMeta.getLore();
                assert newLore != null;
                newLore.set(0,String.format("%s%sSELECTED!", ChatColor.GREEN, ChatColor.BOLD));
                oldMeta.setLore(newLore);
                content.setItemMeta(oldMeta);
                if(selectedGame == null) initSelectors();
                else deselectAll();
                selectedGame = item;
                break;
            }
        }
    }
}
