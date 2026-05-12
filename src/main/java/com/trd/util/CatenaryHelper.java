package com.trd.util;

import net.minecraft.world.phys.Vec3;

public class CatenaryHelper {

    private static final double SLACK = 1.002;

    public record CatenaryData(boolean isVertical, double offsetX, double offsetY,
                               double scale, Vec3 delta, double horLength, Vec3 vecA) {
        public Vec3 getPoint(double t) {
            if (isVertical) {
                return new Vec3(vecA.x + delta.x * t, vecA.y + delta.y * t, vecA.z + delta.z * t);
            }
            double x = vecA.x + delta.x * t;
            double y = vecA.y + scale * Math.cosh((t * horLength - offsetX) / scale) + offsetY;
            double z = vecA.z + delta.z * t;
            return new Vec3(x, y, z);
        }
    }

    public static CatenaryData compute(Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double horLength = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        if (horLength < 0.05) {
            return new CatenaryData(true, 0, 0, 1, delta, 0, start);
        }

        double wireLength = delta.length() * SLACK;
        double l;
        {
            double goal = Math.sqrt(wireLength * wireLength - delta.y * delta.y) / horLength;
            double lower = 0, upper = 1;
            while (Math.sinh(upper) / upper < goal) {
                lower = upper;
                upper *= 2;
            }
            for (int i = 0; i < 20; i++) {
                double mid = (lower + upper) / 2.0;
                double val = Math.sinh(mid) / mid;
                if (val < goal)
                    lower = mid;
                else if (val > goal)
                    upper = mid;
                else
                    break;
            }
            l = (lower + upper) / 2.0;
        }

        double scale = horLength / (2 * l);
        double offsetX = (horLength - scale * Math.log(
                (wireLength + delta.y) / (wireLength - delta.y))) * 0.5;
        double offsetY = -scale * Math.cosh((-offsetX) / scale);

        return new CatenaryData(false, offsetX, offsetY, scale, delta, horLength, start);
    }
}
