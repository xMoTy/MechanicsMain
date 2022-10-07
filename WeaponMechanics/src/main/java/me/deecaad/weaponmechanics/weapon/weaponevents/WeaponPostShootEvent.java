package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called after all the {@link WeaponShootEvent}s are called. This is useful
 * for weapons like shotguns, which fire multiple pellets in the same shot (and
 * therefor call multiple {@link WeaponShootEvent}s).
 */
public class WeaponPostShootEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public WeaponPostShootEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand) {
        super(weaponTitle, weaponStack, shooter, hand);
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}