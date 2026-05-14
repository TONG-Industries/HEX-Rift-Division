package com.trd.api.rotation;

/**
 * Материал вала, определяющий его физические свойства и пределы прочности.
 * 
 * @param name        Уникальное имя материала.
 * @param baseTorque  Базовый предел крутящего момента (при превышении вал
 *                    ломается).
 * @param baseSpeed   Базовый предел скорости вращения (при превышении вал
 *                    ломается).
 * @param baseInertia Масса вращения материала (влияет на разгон сети).
 */
public record ShaftMaterial(
        String name,
        long baseTorque,
        long baseSpeed,
        double baseInertia) {
    public static final ShaftMaterial IRON = new ShaftMaterial("iron", 1000, 1000, 3.0);
    public static final ShaftMaterial DURALUMIN = new ShaftMaterial("duralumin", 800, 4000, 1.0);
    public static final ShaftMaterial STEEL = new ShaftMaterial("steel", 2500, 2000, 4.0);
    public static final ShaftMaterial TITANIUM = new ShaftMaterial("titanium", 4000, 3000, 2.0);
    public static final ShaftMaterial TUNGSTEN_CARBIDE = new ShaftMaterial("tungsten_carbide", 6000, 2500, 6.0);
}
