package org.go.gostamina.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.go.gostamina.GOStaminaPlugin;
import org.go.gostamina.config.ActionSettings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StaminaListener implements Listener {
    private final GOStaminaPlugin plugin;
    private final Map<String, Set<UUID>> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastJumpConsume = new ConcurrentHashMap<>();

    public StaminaListener(GOStaminaPlugin plugin) { this.plugin = plugin; }

    public void startContinuousTasks() {
        for (String key : List.of("sprint", "swim", "mining", "bow", "crossbow", "shield")) {
            ActionSettings action = plugin.settings().action(key);
            if (!action.enabled()) continue;
            Bukkit.getScheduler().runTaskTimer(plugin, () -> tickContinuous(key), 20L, action.intervalSeconds() * 20L);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePolledContinuousStates(player);
            }
        }, 5L, 5L);
    }

    private void tickContinuous(String key) {
        ActionSettings action = plugin.settings().action(key);
        if (!action.enabled()) return;
        for (UUID uuid : new HashSet<>(active.getOrDefault(key, Set.of()))) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) { active.getOrDefault(key, Set.of()).remove(uuid); continue; }
            if (!plugin.staminaManager().consume(player, action.amount())) stopIfEmpty(key, player);
        }
    }

    private void stopIfEmpty(String key, Player player) {
        if (key.equals("sprint")) player.setSprinting(false);
        if (key.equals("swim")) player.setSwimming(false);
        if (key.equals("shield")) player.setCooldown(Material.SHIELD, 20);
        setActive(key, player, false);
    }

    private boolean consumeInstant(Player player, String key) {
        ActionSettings action = plugin.settings().action(key);
        return !action.enabled() || plugin.staminaManager().consume(player, action.amount());
    }

    private void updatePolledContinuousStates(Player player) {
        setActive("shield", player, plugin.settings().action("shield").enabled() && player.isBlocking());
        setActive("swim", player, plugin.settings().action("swim").enabled() && player.isSwimming());

        ItemStack activeItem = activeUseItem(player);
        boolean handRaised = isHandRaised(player);
        setActive("bow", player, plugin.settings().action("bow").enabled() && handRaised && activeItem != null && activeItem.getType() == Material.BOW);
        setActive("crossbow", player, plugin.settings().action("crossbow").enabled() && handRaised && activeItem != null && activeItem.getType() == Material.CROSSBOW);
    }

    private boolean isHandRaised(Player player) {
        Object value = invokeNoArgs(player, "isHandRaised");
        return value instanceof Boolean raised && raised;
    }

    private ItemStack activeUseItem(Player player) {
        Object activeItem = invokeNoArgs(player, "getActiveItem");
        if (activeItem instanceof ItemStack item && item.getType() != Material.AIR) return item;
        Object itemInUse = invokeNoArgs(player, "getItemInUse");
        if (itemInUse instanceof ItemStack item && item.getType() != Material.AIR) return item;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.BOW || mainHand.getType() == Material.CROSSBOW) return mainHand;
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() == Material.BOW || offHand.getType() == Material.CROSSBOW) return offHand;
        return null;
    }

    private Object invokeNoArgs(Player player, String methodName) {
        try {
            return player.getClass().getMethod(methodName).invoke(player);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void setActive(String key, Player player, boolean value) {
        active.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet());
        if (value) active.get(key).add(player.getUniqueId()); else active.get(key).remove(player.getUniqueId());
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { plugin.staminaManager().load(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { active.values().forEach(set -> set.remove(event.getPlayer().getUniqueId())); lastJumpConsume.remove(event.getPlayer().getUniqueId()); plugin.staminaManager().unload(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onSprint(PlayerToggleSprintEvent e) { if (e.isSprinting() && !plugin.staminaManager().hasStamina(e.getPlayer()) && plugin.settings().action("sprint").enabled()) e.setCancelled(true); else setActive("sprint", e.getPlayer(), e.isSprinting()); }
    @EventHandler(ignoreCancelled = true) public void onSwim(EntityToggleSwimEvent e) { if (e.getEntity() instanceof Player p) { if (e.isSwimming() && !plugin.staminaManager().hasStamina(p) && plugin.settings().action("swim").enabled()) e.setCancelled(true); else setActive("swim", p, e.isSwimming()); } }
    @EventHandler(ignoreCancelled = true) public void onBlockDamage(BlockDamageEvent e) { if (plugin.settings().action("mining").enabled() && !plugin.staminaManager().hasStamina(e.getPlayer())) e.setCancelled(true); else setActive("mining", e.getPlayer(), true); }
    @EventHandler public void onAbort(BlockDamageAbortEvent e) { setActive("mining", e.getPlayer(), false); }
    @EventHandler(ignoreCancelled = true) public void onBreak(BlockBreakEvent e) { setActive("mining", e.getPlayer(), false); if (!consumeInstant(e.getPlayer(), "break-block")) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onAttack(EntityDamageByEntityEvent e) { if (e.getDamager() instanceof Player p && !consumeInstant(p, "attack")) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onJumpMove(PlayerMoveEvent e) {
        if (!plugin.settings().action("jump").enabled()) return;
        if (e.getTo() == null || e.getTo().getY() <= e.getFrom().getY()) return;
        Player player = e.getPlayer();
        if (player.isFlying() || player.isGliding() || player.isSwimming()) return;
        if (player.getVelocity().getY() < 0.35D) return;
        long now = System.currentTimeMillis();
        long last = lastJumpConsume.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 250L) return;
        lastJumpConsume.put(player.getUniqueId(), now);
        if (!consumeInstant(player, "jump")) {
            e.setCancelled(true);
            player.setVelocity(player.getVelocity().setY(0.0D));
        }
    }
    @EventHandler(ignoreCancelled = true) public void onInteract(PlayerInteractEvent e) { if (e.getHand() != EquipmentSlot.HAND) return; ItemStack item = e.getItem(); if (item == null) return; Player p = e.getPlayer(); if (item.getType() == Material.BOW) { if (!plugin.staminaManager().hasStamina(p) && plugin.settings().action("bow").enabled()) e.setCancelled(true); else setActive("bow", p, true); } if (item.getType() == Material.CROSSBOW) { if (!plugin.staminaManager().hasStamina(p) && plugin.settings().action("crossbow").enabled()) e.setCancelled(true); else setActive("crossbow", p, true); } if (item.getType() == Material.SHIELD && !plugin.staminaManager().hasStamina(p) && plugin.settings().action("shield").enabled()) e.setCancelled(true); }
    @EventHandler public void onShoot(EntityShootBowEvent e) { if (e.getEntity() instanceof Player p) { setActive("bow", p, false); setActive("crossbow", p, false); } }
    @EventHandler public void onItemHeld(PlayerItemHeldEvent e) { setActive("bow", e.getPlayer(), false); setActive("crossbow", e.getPlayer(), false); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { setActive("bow", e.getPlayer(), false); setActive("crossbow", e.getPlayer(), false); }
    @EventHandler(ignoreCancelled = true) public void onConsume(PlayerItemConsumeEvent e) { plugin.staminaManager().restore(e.getPlayer(), plugin.settings().foodRegen(itemKey(e.getItem()))); }

    private String itemKey(ItemStack item) {
        if (item.hasItemMeta()) {
            for (String key : List.of("craftengine:id", "craftengine:item_id", "craftengine:custom_item_id")) {
                NamespacedKey namespacedKey = NamespacedKey.fromString(key);
                String value = item.getItemMeta().getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
                if (value != null && !value.isBlank()) return value.toLowerCase();
            }
        }
        return item.getType().getKey().asString().toLowerCase();
    }
}
