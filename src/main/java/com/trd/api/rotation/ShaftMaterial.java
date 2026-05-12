package com.trd.api.rotation;

public record ShaftMaterial(
        String name,
        long baseTorque,
        long baseSpeed,
        int maxDeflection,
        long baseInertia,
        double baseFriction,
        int baseMaxSpeed,
        int baseMaxTorque
) {
    public static final ShaftMaterial IRON = new ShaftMaterial("iron", 500, 256, 5, 5, 1.0, 1000, 1000);
    public static final ShaftMaterial DURALUMIN = new ShaftMaterial("duralumin", 1000, 640, 7, 2, 0.4, 2000, 800);
    public static final ShaftMaterial STEEL = new ShaftMaterial("steel", 3500, 160, 10, 25, 1.5, 1500, 2500);
    public static final ShaftMaterial TITANIUM = new ShaftMaterial("titanium", 6000, 800, 14, 12, 0.2, 3000, 4000);
    public static final ShaftMaterial TUNGSTEN_CARBIDE = new ShaftMaterial("tungsten_carbide", 15000, 1200, 20, 8, 0.05, 2500, 6000);

    public int getBaseMaxSpeed() { return baseMaxSpeed; }
    public int getBaseMaxTorque() { return baseMaxTorque; }
}
