package me.deecaad.weaponmechanics.weapon.explode.shapes;

import me.deecaad.core.file.Configuration;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.deecaad.weaponmechanics.WeaponMechanics.debug;

/**
 * Uses parabolas to calculate the area of explosions, where a parabola is defined as: y = m(x -
 * xOffset)^2 + yOffset xOffset is ignored here yOffset is the depth. Negative values ONLY are
 * allowed. m is the angle. Under 1 makes the explosion wider. Over 1 makes the explosion thinner
 */
public class ParabolicExplosion implements ExplosionShape {

    private static final Configuration config = WeaponMechanics.getBasicConfigurations();

    private final double depth; // This is assumed to be negative
    private final double angle;

    public ParabolicExplosion(double depth) {
        this(depth, 0.5);
    }

    public ParabolicExplosion(double depth, double angle) {
        this.depth = -Math.abs(depth); // this check is also done by serializer, but add it here for devs
        this.angle = angle;
    }

    /**
     * @return Depth of the explosion
     */
    public double getDepth() {
        return depth;
    }

    /**
     * @return Angle of the parabola
     */
    public double getAngle() {
        return angle;
    }

    @NotNull @Override
    public List<Block> getBlocks(@NotNull Location origin) {
        List<Block> temp = new ArrayList<>();

        // Solve for x
        // y = angle * x^2 + depth
        // y - depth = angle * x^2
        // (y - depth) / angle = x^2
        // sqrt((y - depth) / angle) = x
        // Set y = 0 to find x intercept (sqrt always returns a positive double)
        // sqrt(-depth / angle) = x
        double intercept = Math.sqrt(-depth / angle);

        double noiseDistance = config.getDouble("Explosions.Spherical.Noise_Distance", 1.25);
        double noiseChance = config.getDouble("Explosions.Spherical.Noise_Chance", 0.25);

        for (double x = -intercept; x < intercept; x++) {
            for (double y = depth; y < -depth; y++) {
                for (double z = -intercept; z < intercept; z++) {
                    if (test(x, y, z)) {

                        // Checking chance first for resource usage
                        if (RandomUtil.chance(noiseChance) && isNearEdge(x, y, z, noiseDistance)) {
                            debug.log(LogLevel.DEBUG, "Skipping block (" + x + ", " + y + ", " + z + ") due to noise.");
                            continue; // outer noise checker
                        }

                        temp.add(origin.clone().add(x, y, z).getBlock());
                    }
                }
            }
        }
        return temp;
    }

    @NotNull @Override
    public List<LivingEntity> getEntities(@NotNull Location origin) {
        List<LivingEntity> all = origin.getWorld().getLivingEntities();
        List<LivingEntity> temp = new ArrayList<>(all.size());

        for (LivingEntity entity : all) {
            if (isContained(origin, entity.getLocation()))
                temp.add(entity);
        }

        return temp;
    }

    @Override
    public double getMaxDistance() {
        double xOffset = Math.sqrt(-depth / angle);
        return Math.max(xOffset, -depth);
    }

    /**
     * Tests if a location is within this explosion based on the given origin
     *
     * @param origin Center of the explosion
     * @param loc Point to test
     * @return If the location is in the explosion
     */
    public boolean test(Location origin, Location loc) {
        loc.subtract(origin);
        return test(loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public boolean isContained(@NotNull Location origin, @NotNull Location point) {
        return test(point.getX() - origin.getX(), point.getY() - origin.getY(), point.getZ() - origin.getZ());
    }

    @Override
    public double getArea() {

        // We need some calculus to accurately determine the area.
        return (3.0 * Math.PI * depth * depth) / angle;
    }

    public boolean test(double x, double y, double z) {
        double temp1 = +angle * (x * x) + depth - y;
        double temp2 = -angle * (x * x) - depth - y;
        double temp3 = +angle * (z * z) + depth - y;
        double temp4 = -angle * (z * z) - depth - y;

        return temp1 <= 0 && temp2 >= 0 && temp3 <= 0 && temp4 >= 0;
    }

    public boolean isNearEdge(double x, double y, double z, double distance) {
        double temp1 = +angle * (x * x) + depth - y;
        double temp2 = -angle * (x * x) - depth - y;
        double temp3 = +angle * (z * z) + depth - y;
        double temp4 = -angle * (z * z) - depth - y;

        return temp1 > -distance ||
            temp2 < distance ||
            temp3 > -distance ||
            temp4 < distance;
    }

    @Override
    public String toString() {
        return "ParabolicExplosion{" +
            "depth=" + depth +
            ", angle=" + angle +
            '}';
    }
}