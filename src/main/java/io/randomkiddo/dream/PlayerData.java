package io.randomkiddo.dream;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record PlayerData(UUID playerUuid, ItemStack[] inventory, Location bedSpawn) {}

