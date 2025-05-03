/**
 * This file is licensed by the GNU GPLv3 License.
 * Copyright © 2025 RandomKiddo
 */

package io.randomkiddo.dream;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
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
import java.util.UUID;
import java.util.HashMap;

/**
 * The Main plugin class, extends JavaPlugin and implements Listener.
 *
 * @see JavaPlugin
 * @see Listener
 */
public class Main extends JavaPlugin implements Listener {
    /**
     * The list of player data for the current server.
     */
    private static ArrayList<PlayerData> playerData = new ArrayList<>();

    private static HashMap<UUID, Boolean> playersResourcePackStatus = new HashMap<>();
    /**
     * Dictates behavior on plugin enable. Registers events and fetches current player data.
     */
    @Override public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        this.getPlayerData();
    }

    /**
     * Gets all player data needed from the current status of the server.
     */
    private void getPlayerData() {
        Collection<? extends Player> playerLikeEntities = Bukkit.getOnlinePlayers();
        for (Player player : playerLikeEntities) {
            PlayerInventory liveInventory = player.getInventory();
            ItemStack[] inventory = liveInventory.getContents().clone();
            Location respawnLoc = player.getRespawnLocation();
            Main.playerData.add(new PlayerData(player.getUniqueId(), inventory, respawnLoc));
        }
    }

    /**
     * Handles behavior on player joining. Adds their player data to the list.
     * @param event The player join event instance.
     */
    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack[] inventory = player.getInventory().getContents().clone();
        Location respawnLoc = player.getRespawnLocation();
        Main.playerData.add(new PlayerData(player.getUniqueId(), inventory, respawnLoc));

        player.setResourcePack("https://github.com/RandomKiddo/ItWasADream/releases/download/rp/ItWasADreamRP.zip");
    }

    /**
     * Handles behavior on player quitting. Removes their player data from the list.
     * @param event The player quit event instance.
     */
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

    /**
     * Handles behavior when the player is damaged. Applies "dream"-like behavior when player death
     * is supposed to occur. Highest event priority. Only handles player damage.
     * @param event The entity damage event instance.
     */
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
                    if (!Main.playersResourcePackStatus.getOrDefault(player.getUniqueId(), false)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    } else {
                        player.playSound(player.getLocation(), "minecraft:custom.flashbang", SoundCategory.MASTER, 1.0f, 1.0f);
                    }
                }
            }
        }
    }

    /**
     * Handles behavior when a player attempts to sleep or update respawn at a bed. Updates player's
     * respawn location when required.
     * @param event The player enter bed event instance.
     */
    @EventHandler public void onPlayerSleep(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Location currentRespawn = player.getRespawnLocation();
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Location respawn = player.getRespawnLocation();
                if (respawn != null && !respawn.equals(currentRespawn)) {
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

    /**
     * Handles behavior when a block is broken. Only handles instances when respawn anchors or beds are broken.
     * Updates the respawn location for the player's respawn point when it is lost, either to the world's main
     * spawn location, or (0, 0, 0), depending on the success of the world instance being fetched.
     * @param event The block break event instance.
     */
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

    /**
     * Handles behavior when an explosion occurs. Only handles when blocks are broken, and the block in question
     * is either a bed or a respawn anchor. Updates the respawn location for the player's respawn point when it
     * is lost, either to the world's main spawn location, or (0, 0, 0), depending on the success of the world
     * instance being fetched.
     * @param event The entity explode event instance.
     */
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

    /**
     * Handles behavior when a player interacts with an object. Only handles instances where the right-clicked
     * block is a respawn anchor. Updates the given player's respawn location.
     * @param event The player interact event instance.
     */
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

    @EventHandler public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();

        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            System.out.println("true");
            Main.playersResourcePackStatus.put(player.getUniqueId(), true);
        } else {
            System.out.println("false");
            Main.playersResourcePackStatus.put(player.getUniqueId(), false);
        }
    }
}
