package me.deecaad.weaponmechanics.weapon.explode.exposures;

import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponcompatibility.WeaponCompatibilityAPI;
import me.deecaad.weaponcompatibility.projectile.HitBox;
import me.deecaad.weaponmechanics.weapon.explode.raytrace.Ray;
import me.deecaad.weaponmechanics.weapon.explode.raytrace.TraceCollision;
import me.deecaad.weaponmechanics.weapon.explode.raytrace.TraceResult;
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.deecaad.weaponmechanics.WeaponMechanics.debug;

public class DefaultExposure implements ExplosionExposure {

    @Nonnull
    @Override
    public Map<LivingEntity, Double> mapExposures(@Nonnull Location origin, @Nonnull ExplosionShape shape) {

        List<LivingEntity> entities = shape.getEntities(origin);

        // Map to store all the calculated entities in
        Map<LivingEntity, Double> temp = new HashMap<>();

        // How far away from the explosion to damage players
        double damageRadius = shape.getMaxDistance() * 2.0F;

        // Gets data on the location of the explosion
        World world = origin.getWorld();
        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();

        if (world == null) {
            debug.log(LogLevel.ERROR, "Explosion in null world? Location: " + origin, "Please report error to devs");
            return temp;
        }

        Vector vector = new Vector(x, y, z);

        for (LivingEntity entity : entities) {
            Vector entityLocation = entity.getLocation().toVector();

            // Gets the "rate" or percentage of how far the entity
            // is from the explosion. For example, it the distance
            // is 8 and explosion radius is 10, the rate will be 1/5
            double impactRate = (damageRadius - entityLocation.distance(vector)) / damageRadius;

            if (impactRate > 1.0D) {
                debug.log(LogLevel.DEBUG, "Entity " + entity + " was just outside the blast radius");
                continue;
            }

            Vector betweenEntityAndExplosion = entityLocation.subtract(vector);
            double distance = betweenEntityAndExplosion.length();

            // If there is distance between the entity and the explosion
            if (distance != 0.0) {

                // Normalize
                betweenEntityAndExplosion.multiply(1 / distance);

                double exposure = getExposure(vector, entity);
                double impact = impactRate * exposure;

                temp.put(entity, impact);
            }
        }

        return temp;
    }

    /**
     * Gets a double [0, 1] representing how exposed the entity is to the explosion
     *
     * @param vec3d Vector between explosion and entity
     * @param entity The entity exposed to the explosion
     * @return The level of exposure of the entity to the epxlosion
     */
    private static double getExposure(Vector vec3d, Entity entity) {
        HitBox box = WeaponCompatibilityAPI.getProjectileCompatibility().getHitBox(entity);

        if (box == null) {
            return 0.0;
        }

        // Get the dimensions of the bounding box
        double width = box.getWidth();
        double height = box.getHeight();
        double depth = box.getDepth();

        // Gets the size of the grid in each axis
        double stepX = width * 2.0 + 1.0;
        double stepY = height * 2.0 + 1.0;
        double stepZ = depth * 2.0 + 1.0;
        double gridX = 1.0 / stepX;
        double gridY = 1.0 / stepY;
        double gridZ = 1.0 / stepZ;

        // Outside of the grid
        if (gridX < 0.0 || gridY < 0.0 || gridZ < 0.0) return 0.0;

        double d3 = (1.0 - Math.floor(stepX) * gridX) / 2.0;
        double d4 = (1.0 - Math.floor(stepZ) * gridZ) / 2.0;

        // Setup variables for the loop
        World world = entity.getWorld();

        int successfulTraces = 0;
        int totalTraces = 0;

        // For each grid on the bounding box
        for (double x = 0; x <= 1; x += gridX) {
            for (double y = 0; y <= 1; y += gridY) {
                for (double z = 0; z <= 1; z += gridZ) {
                    double a = NumberUtil.lerp(box.min.getX(), box.max.getX(), x);
                    double b = NumberUtil.lerp(box.min.getY(), box.max.getY(), y);
                    double c = NumberUtil.lerp(box.min.getZ(), box.max.getZ(), z);

                    // Calculates a path from the origin of the explosion
                    // (0, 0, 0) to the current grid on the entity's bounding
                    // box. The Vector is then ray traced to check for obstructions
                    Vector vector = new Vector(a + d3, b, c + d4).subtract(vec3d);

                    Ray ray = new Ray(vec3d.toLocation(world), vector);
                    TraceResult trace = ray.trace(TraceCollision.BLOCK, 0.3);
                    if (trace.getBlocks().isEmpty()) {
                        successfulTraces++;
                    }

                    totalTraces++;
                }
            }
        }

        // The percentage of successful traces
        return ((double) successfulTraces) / totalTraces;
    }
}
