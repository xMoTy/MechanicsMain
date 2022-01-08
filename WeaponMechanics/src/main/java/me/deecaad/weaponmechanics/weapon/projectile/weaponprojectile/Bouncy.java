package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.file.Serializer;
import me.deecaad.weaponmechanics.compatibility.WeaponCompatibilityAPI;
import me.deecaad.weaponmechanics.compatibility.projectile.IProjectileCompatibility;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.io.File;

public class Bouncy implements Serializer<Bouncy> {

    private static final IProjectileCompatibility projectileCompatibility = WeaponCompatibilityAPI.getProjectileCompatibility();

    // -1 = infinite
    private int maximumBounceAmount;

    private ListHolder<Material> blocks;
    private ListHolder<EntityType> entities;

    private double requiredMotionToStartRolling;
    private ListHolder<Material> rollingBlocks;

    /**
     * Empty for serializers
     */
    public Bouncy() { }

    public Bouncy(int maximumBounceAmount, ListHolder<Material> blocks, ListHolder<EntityType> entities,
                  double requiredMotionToStartRolling, ListHolder<Material> rollingBlocks) {
        this.maximumBounceAmount = maximumBounceAmount;
        this.blocks = blocks;
        this.entities = entities;
        this.requiredMotionToStartRolling = requiredMotionToStartRolling;
        this.rollingBlocks = rollingBlocks;
    }

    /**
     * If rolling isn't used, this value is used determine whether projectile can't bounce anymore.
     * If rolling is used, this value is used to determine when projectile starts rolling instead of bouncing.
     *
     * @return the required motion to start rolling or die
     */
    public double getRequiredMotionToStartRollingOrDie() {
        return requiredMotionToStartRolling;
    }

    /**
     * @param projectile the projectile
     * @param hit the hit entity or block
     * @return true if projectile bounced or started rolling, false if projectile should die
     */
    public boolean handleBounce(WeaponProjectile projectile, RayTraceResult hit) {
        Double speedModifier;
        if (hit.isBlock()) {
            speedModifier = blocks != null ? blocks.isValid(hit.getBlock().getType()) : null;
        } else {
            speedModifier = entities != null ? entities.isValid(hit.getLivingEntity().getType()) : null;
        }

        // Speed modifier null would mean that it wasn't valid material or entity type
        if (speedModifier == null || (maximumBounceAmount != -1 && maximumBounceAmount - projectile.getBounces() < 1)) {
            // Projectile should die
            return false;
        }

        Vector motion = projectile.getMotion();
        if (speedModifier != 1.0) motion.multiply(speedModifier);

        switch (hit.getHitFace()) {
            case UP: case DOWN:
                motion.setY(-motion.getY());
                break;
            case EAST: case WEST:
                motion.setX(-motion.getX());
                break;
            case NORTH: case SOUTH:
                motion.setZ(-motion.getZ());
                break;
            default:
                break;
        }

        projectile.setMotion(motion);

        return true;
    }

    /**
     * @param projectile the projectile
     * @param block the block below
     * @return true if projectile kept rolling, false if projectile should die
     */
    public boolean handleRolling(WeaponProjectile projectile, Block block) {
        if (rollingBlocks == null) return false;

        Double speedModifier = rollingBlocks.isValid(block.getType());

        if (speedModifier == null) {
            // Projectile should die since block wasn't valid
            return false;
        }

        projectile.setRolling(true);

        Vector motion = projectile.getMotion();
        if (speedModifier != 1.0) motion.multiply(speedModifier);

        // Remove vertical motion since projectile should start/keep rolling
        motion.setY(0);

        projectile.setMotion(motion);
        return true;
    }

    /**
     * @param projectile the projectile
     * @return true if projectile is unable to keep rolling AND should die
     */
    public boolean checkForRollingCancel(WeaponProjectile projectile) {
        Vector slightlyBelow = projectile.getLocation().add(new Vector(0, -0.05, 0));
        Block slightlyBelowBlock = projectile.getWorld().getBlockAt(slightlyBelow.getBlockX(), slightlyBelow.getBlockY(), slightlyBelow.getBlockZ());
        if (projectileCompatibility.getHitBox(slightlyBelowBlock) != null) {
            // Check speed modifier of block below and apply it
            if (!handleRolling(projectile, slightlyBelowBlock)) {
                // Block below wasn't valid rolling block, remove projectile
                return true;
            }
            if (projectile.getMotionLength() < 0.05) {
                // The motion is so slow at this point, simply apply sticked data to block below
                projectile.setStickedData(new StickedData(projectile, slightlyBelowBlock));
                projectile.setRolling(false);
            }
        } else {
            // Block below is air or passable block, toggle rolling off
            // When rolling is off, gravity is applied again
            projectile.setRolling(false);
        }
        return false;
    }

    @Override
    public String getKeyword() {
        return "Bouncy";
    }

    @Override
    public Bouncy serialize(File file, ConfigurationSection configurationSection, String path) {
        ListHolder<Material> blocks = new ListHolder<Material>().serialize(file, configurationSection, path + ".Blocks", Material.class);
        ListHolder<EntityType> entities = new ListHolder<EntityType>().serialize(file, configurationSection, path + ".Entities", EntityType.class);

        if (blocks == null && entities == null) return null;

        int maximumBounceAmount = configurationSection.getInt(path + ".Maximum_Bounce_Amount", 1);

        ListHolder<Material> rollingBlocks = new ListHolder<Material>().serialize(file, configurationSection, path + ".Rolling.Blocks", Material.class);
        double requiredMotionToStartRolling = configurationSection.getDouble(path + ".Rolling.Required_Motion_To_Start_Rolling", 3) * 0.1;

        return new Bouncy(maximumBounceAmount, blocks, entities, requiredMotionToStartRolling, rollingBlocks);
    }
}