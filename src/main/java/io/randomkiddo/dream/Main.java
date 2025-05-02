package io.randomkiddo.dream;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class Main extends JavaPlugin implements Listener {
    private static ArrayList<PlayerData> playerData = new ArrayList<>();
    @Override public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        this.getPlayerData();
    }
    private void getPlayerData() {
        Collection<? extends Player> playerLikeEntities = Bukkit.getOnlinePlayers();
        for (Player player : playerLikeEntities) {
            PlayerInventory liveInventory = player.getInventory();
            ItemStack[] inventory = liveInventory.getContents().clone();
            Location respawnLoc = player.getRespawnLocation();
            Main.playerData.add(new PlayerData(player.getUniqueId(), inventory, respawnLoc));
        }
    }
    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack[] inventory = player.getInventory().getContents().clone();
        Location respawnLoc = player.getRespawnLocation();
        Main.playerData.add(new PlayerData(player.getUniqueId(), inventory, respawnLoc));
    }
    @EventHandler public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < Main.playerData.size(); ++i) {
            PlayerData data = Main.playerData.get(i);
            if (data.playerUuid().equals(player.getUniqueId())) {
                Main.playerData.remove(i);
                break;
            }
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0) {
            event.setCancelled(true);
            player.setHealth(1.0);
            for (int i = 0; i < Main.playerData.size(); ++i) {
                PlayerData data = Main.playerData.get(0);
                if (player.getUniqueId().equals(data.playerUuid())) {
                    Location respawnLoc = data.bedSpawn();
                    ItemStack[] inventory = data.inventory();
                    player.teleport(respawnLoc);
                    player.getInventory().setContents(inventory);
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.BLINDNESS, 20, 1, false, false
                    ));
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                }
            }
        }
    }
    @EventHandler public void onPlayerSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Location respawn = event.getPlayer().getRespawnLocation();
                if (respawn != null) {
                    for (int i = 0; i < Main.playerData.size(); ++i) {
                        PlayerData data = Main.playerData.get(i);
                        if (data.playerUuid().equals(player.getUniqueId())) {
                            Main.playerData.set(i, new PlayerData(
                                    player.getUniqueId(),
                                    player.getInventory().getContents().clone(),
                                    respawn
                            ));
                            break;
                        }
                    }
                }
            }, 1L);
        }
    }
    @EventHandler public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType().name().endsWith("_BED") || block.getType() == Material.RESPAWN_ANCHOR) {
            Location brokenLoc = block.getLocation();
            for (int i = 0; i < Main.playerData.size(); ++i) {
                PlayerData data = Main.playerData.get(i);
                Location respawnLoc = data.bedSpawn();
                if (respawnLoc != null && Math.abs(respawnLoc.getBlockX()-brokenLoc.getBlockX()) <= 1.5 &&
                respawnLoc.getBlockY() == brokenLoc.getBlockY() &&
                Math.abs(respawnLoc.getBlockZ()-brokenLoc.getBlockZ()) <= 1.5) {
                    System.out.println("Here");
                    System.out.println(respawnLoc);
                    Location spawn;
                    try {
                        World world = Bukkit.getWorld("world");
                        spawn = world.getSpawnLocation();
                    } catch (NullPointerException npterr) {
                        World world = Bukkit.getWorlds().get(0);
                        spawn = new Location(world, 0, 0, 0);
                    }
                    System.out.println(spawn);
                    Main.playerData.set(i, new PlayerData(
                            data.playerUuid(),
                            data.inventory(),
                            spawn
                    ));
                }
            }
        }
    }
    @EventHandler public void onExplosion(EntityExplodeEvent event) {
        List<Block> blocks = event.blockList();
        for (Block block : blocks) {
            if (block.getType().name().endsWith("_BED") || block.getType() == Material.RESPAWN_ANCHOR) {
                Location brokenLoc = block.getLocation();
                for (int i = 0; i < Main.playerData.size(); ++i) {
                    PlayerData data = Main.playerData.get(i);
                    Location respawnLoc = data.bedSpawn();
                    if (brokenLoc.equals(respawnLoc)) {
                        Location spawn;
                        try {
                            World world = Bukkit.getWorld("world");
                            spawn = world.getSpawnLocation();
                        } catch (NullPointerException npterr) {
                            World world = Bukkit.getWorlds().get(0);
                            spawn = new Location(world, 0, 0, 0);
                        }
                        Main.playerData.set(i, new PlayerData(
                                data.playerUuid(),
                                data.inventory(),
                                spawn
                        ));
                    }
                }
            }
        }
    }
    @EventHandler public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            Player player = event.getPlayer();
            if (block.getType() == Material.RESPAWN_ANCHOR) {
                for (int i = 0; i < Main.playerData.size(); ++i) {
                    PlayerData data = Main.playerData.get(i);
                    if (data.playerUuid().equals(player.getUniqueId())) {
                        Location respawnLoc = block.getLocation();
                        Main.playerData.set(i, new PlayerData(
                                data.playerUuid(),
                                player.getInventory().getContents().clone(),
                                respawnLoc
                        ));
                    }
                }
            } else if (block.getBlockData() instanceof Bed) {
                Location before = player.getRespawnLocation();
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Location after = player.getRespawnLocation();
                    if (after != null && (!after.equals(before))) {
                        for (int i = 0; i < Main.playerData.size(); ++i) {
                            PlayerData data = Main.playerData.get(i);
                            if (data.playerUuid().equals(player.getUniqueId())) {
                                Main.playerData.set(i, new PlayerData(
                                        player.getUniqueId(),
                                        player.getInventory().getContents().clone(),
                                        after
                                ));
                                break;
                            }
                        }
                    }
                }, 1L);
            }
        }
    }
}
