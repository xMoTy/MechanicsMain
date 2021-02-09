package me.deecaad.core.packetlistener;

import me.deecaad.core.utils.ReflectionUtil;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * This class outlines a wrapper for storing nms packets exchanged between the
 * minecraft server and players connected to the server.
 */
public class Packet {

    private final Player player;
    private final Object packet;
    private boolean cancelled;

    public Packet(Player player, Object packet) {
        this.player = player;
        this.packet = packet;
    }

    /**
     * The player whose channel the packet belongs to. If the packet
     * was originates from an internal server channel, this method
     * will return <code>null</code>.
     *
     * @return The nullable player who is involved in this packet.
     */
    @Nullable
    public Player getPlayer() {
        return this.player;
    }

    /**
     * The nms packet instance.
     *
     * @return The nonnull packet that was intercepted.
     */
    @Nonnull
    public Object getPacket() {
        return this.packet;
    }

    /**
     * Returns <code>true</code> if this packet has been cancelled. Cancelled
     * packets do not reach their destination (Outgoing packets do not reach
     * the client, and incoming packets do not reach the server).
     *
     * Note that packet wrappers are plugin specific, so this method returning
     * <code>true</code> means that, at some point, your plugin has cancelled
     * the packet.
     *
     * @return The cancellation state.
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Sets the cancellation state of the packet. If <code>cancelled</code> is
     * equal to <code>true</code>, this packet will not be sent to it's
     * destination.
     *
     * @param cancelled The cancellation state to set.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Gets field from some specific super class of this instance's packet instance
     *
     * @param fieldName the field name to search
     * @param superClass the amount of super class
     * @return the field from the super class
     */
    public Field getField(String fieldName, int superClass) {
        Class<?> packetClass = this.packet.getClass();
        if (superClass > 0) {
            int i = 0;
            while (i < superClass) {
                packetClass = packetClass.getSuperclass();
                ++i;
            }
        }
        return ReflectionUtil.getField(packetClass, fieldName);
    }

    /**
     * Gets value from some field from this instance's packet instance
     *
     * @param fieldName the field name to search
     * @return the object of field from the super class
     */
    public Object getFieldValue(String fieldName) {
        return getFieldValue(fieldName, 0);
    }

    /**
     * Gets value from some field from specific super class of this instance's packet instance
     *
     * @param fieldName the field name to search
     * @param superClass the amount of super class
     * @return the object of field from the super class
     */
    public Object getFieldValue(String fieldName, int superClass) {
        return ReflectionUtil.invokeField(getField(fieldName, superClass), this.packet);
    }

    /**
     * Sets new value for field in some specific super class
     *
     * @param fieldName the field name to change
     * @param value the new value for the field
     * @param superClass the amount of super class
     */
    public void setFieldValue(String fieldName, Object value, int superClass) {
        ReflectionUtil.setField(getField(fieldName, superClass), this.packet, value);
    }

    /**
     * Gets value from some field of this instance's packet instance
     *
     * @param field the field invoked
     * @return the object of field
     */
    public Object getFieldValue(Field field) {
        return ReflectionUtil.invokeField(field, this.packet);
    }

    /**
     * Sets new value for field
     *
     * @param field the field invoked
     * @param value the new value for the field
     */
    public void setFieldValue(Field field, Object value) {
        ReflectionUtil.setField(field, this.packet, value);
    }
}