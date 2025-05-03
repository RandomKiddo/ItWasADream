/**
 * This file is licensed by the GNU GPLv3 License.
 * Copyright © 2025 RandomKiddo
 */

package io.randomkiddo.dream;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Record to hold player data for recall upon respawn.
 * @param playerUuid The player's unique ID.
 * @param inventory The copied inventory contents of the player in ItemStack[] form.
 * @param bedSpawn The bed spawn location, a.k.a. the respawn location.
 */
public record PlayerData(UUID playerUuid, ItemStack[] inventory, Location bedSpawn) {}

