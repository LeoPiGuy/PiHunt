package com.leopiguy.manhuntplugin.util;

import com.leopiguy.manhuntplugin.MainGameContainer;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import static org.bukkit.Bukkit.getLogger;

public class PlayerPickerGUI implements Listener {

    private Inventory gui;
    private ArrayList<ItemStack> selectedHeads;
    private final String gameId;
    private final int numRows;
    private Player holdingPlayer;
    private final MainGameContainer containerRef;
    private boolean isOpen;
    private boolean isCanceled;
    private boolean isRefresh;

    public PlayerPickerGUI(String name, ArrayList<Player> players, String gameId, MainGameContainer containerRef) {
        this.gameId = gameId;
        this.containerRef = containerRef;
        this.selectedHeads = new ArrayList<>();
        this.holdingPlayer = null;
        this.isCanceled = true;
        this.isOpen = false;
        this.isRefresh = false;
        int numRows = players.size() / 9;
        if (players.size() % 9 != 0) numRows++;
        this.gui = Bukkit.createInventory(null, (numRows + 1) * 9, name);
        this.numRows = numRows;
        initHeads(players);
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

        cancelMeta.setDisplayName(ChatColor.RED + "Cancel Manhunt Creation");
        cancelItem.setItemMeta(cancelMeta);

        gui.setItem(cancelSlot, cancelItem);
    }

    private void initHeads(ArrayList<Player> players) {
        for (Player player : players) {
            gui.addItem(createHead(player));
        }
    }

    private ItemStack createHead(Player player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        final SkullMeta headMeta = (SkullMeta) head.getItemMeta();

        assert headMeta != null;

        headMeta.setDisplayName(ChatColor.RED + player.getDisplayName());
        headMeta.setOwningPlayer(player);
        headMeta.setLore(new ArrayList<>(Collections.singletonList(String.format("%sClick to select!", ChatColor.GREEN))));
        head.setItemMeta(headMeta);

        return head;
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
            case PLAYER_HEAD:
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
            if(selectedHeads.contains(item)) {
                deselect(item);
            }
        }
    }

    private void confirm() {
        final ArrayList<Player> selectedPlayers = new ArrayList<>();
        for (ItemStack head : selectedHeads) {
            if(head.getType().equals(Material.PLAYER_HEAD)) {
                final SkullMeta meta = (SkullMeta) head.getItemMeta();
                assert meta != null;
                final Player player = Objects.requireNonNull(meta.getOwningPlayer()).getPlayer();
                selectedPlayers.add(player);
            }
        }
        this.isCanceled = true;
        containerRef.guiConfirm(selectedPlayers, gameId, this, holdingPlayer);
    }

    private void earlyClose() {
        if(isRefresh) {
            isRefresh = false;
            return;
        }
        if(isCanceled) return;
        isCanceled = true;
        containerRef.initCanceled(gameId,this, holdingPlayer);
        if(!isOpen) return;
        isOpen = false;
        holdingPlayer.closeInventory();
    }

    private boolean isSelected(ItemStack item) {
        return selectedHeads.contains(item);
    }

    private void deselect(ItemStack item) {
        for (ItemStack content : gui.getContents()) {
            if(content.isSimilar(item)) {
                ItemMeta oldMeta = content.getItemMeta();
                assert oldMeta != null;
                oldMeta.setDisplayName(ChatColor.RED + ChatColor.stripColor(oldMeta.getDisplayName()));
                oldMeta.setLore(new ArrayList<>(Collections.singletonList(String.format("%sClick to select!", ChatColor.GREEN))));
                content.setItemMeta(oldMeta);
                selectedHeads.remove(item);
                if(selectedHeads.isEmpty()) deleteSelectors();
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
                oldMeta.setDisplayName(ChatColor.GREEN + ChatColor.stripColor(oldMeta.getDisplayName()));
                oldMeta.setLore(new ArrayList<>(Collections.singletonList(String.format("%s%sSELECTED", ChatColor.GREEN, ChatColor.BOLD))));
                content.setItemMeta(oldMeta);
                if(selectedHeads.isEmpty()) initSelectors();
                selectedHeads.add(item);
                break;
            }
        }
    }

    public void refreshGui(String name, ArrayList<Player> removedPlayers) {
        deselectAll();
        ItemStack[] oldContents = gui.getContents();
        this.isRefresh = true;
        holdingPlayer.closeInventory();
        this.gui = Bukkit.createInventory(null, (numRows + 1) * 9 ,name);
        gui.setContents(oldContents);
        for (Player removedPlayer : removedPlayers) {
            for (ItemStack item : gui.getContents()) {
                if(item == null) continue;
                if(item.getType().equals(Material.PLAYER_HEAD)) {
                    final SkullMeta meta = (SkullMeta) item.getItemMeta();
                    assert meta != null;
                    if(removedPlayer.equals(Objects.requireNonNull(meta.getOwningPlayer()).getPlayer()))
                        gui.remove(item);
                }
            }
        }
        this.isCanceled = false;
        this.isOpen = true;
        holdingPlayer.openInventory(gui);
    }
}
