package com.trd.block.entity.weapons;

import com.trd.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MissileTurretBlockEntity extends BlockEntity {

    public static final double SEARCH_RADIUS = 100.0;
    public static final double SEARCH_RADIUS_SQR = SEARCH_RADIUS * SEARCH_RADIUS;
    public static final int COOLDOWN_TICKS = 200;
    public static final int SALVO_SIZE = 3;
    public static final int SALVO_INTERVAL = 20;
    public static final float LAUNCH_HEIGHT = 1.0f;

    private int cooldownTimer = 0;
    private int salvoCounter = 0;
    private int salvoTimer = 0;
    private boolean isSalvoActive = false;
    private LivingEntity currentTarget = null;
    private int scanTimer = 0;
    private static final int SCAN_INTERVAL = 5;

    // [НОВОЕ] GUI-related поля (1:1 с TurretLightPlacerBlockEntity)
    private boolean isSwitchedOn = false;
    private int bootTimer = 0;
    private static final int BOOT_DURATION = 60;

    private boolean targetHostile = true;
    private boolean targetNeutral = false;
    private boolean targetPlayers = true;

    private int killCount = 0;
    private long lifetimeTicks = 0;

    // [НОВОЕ] Дополнительные кнопки
    private boolean extraButton1 = true; // По умолчанию включена
    private boolean extraButton2 = true; // По умолчанию включена

    // [НОВОЕ] Энергия (заглушки для совместимости GUI)
    private long energy = 0;
    private long capacity = 100000;
    private static final long MAX_RECEIVE = 10000;

    public MissileTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_TURRET_BE.get(), pos, state);
        this.ammoContainer.setOnContentsChanged(this::setChanged);
    }
    private final TurretAmmoContainer ammoContainer = new TurretAmmoContainer();
    public TurretAmmoContainer getAmmoContainer() { return ammoContainer; }
    public static void tick(Level level, BlockPos pos, BlockState state, MissileTurretBlockEntity turret) {
        if (level.isClientSide) return;
        turret.lifetimeTicks++;
        turret.tickServer((ServerLevel) level, pos, state);
    }

    private void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        // [НОВОЕ] Логика загрузки и выключателя
        if (!isSwitchedOn) {
            return;
        }

        if (bootTimer > 0) {
            bootTimer--;
            return;
        }

        if (cooldownTimer > 0) cooldownTimer--;

        if (isSalvoActive) {
            salvoTimer--;
            if (salvoTimer <= 0) {
                fireMissile(level, pos, state);
                salvoCounter++;
                if (salvoCounter >= SALVO_SIZE) {
                    isSalvoActive = false;
                    salvoCounter = 0;
                    cooldownTimer = COOLDOWN_TICKS;
                } else {
                    salvoTimer = SALVO_INTERVAL;
                }
            }
            return;
        }

        if (cooldownTimer <= 0) {
            scanTimer--;
            if (scanTimer <= 0) {
                scanTimer = SCAN_INTERVAL;
                currentTarget = findBestTarget(level, pos);
                if (currentTarget != null) startSalvo();
            }
        }
    }

    private boolean hasOpenSky(ServerLevel level, Vec3 mobPos) {
        Vec3 from = mobPos.add(0, 1.5, 0);
        Vec3 to = new Vec3(from.x, level.getMaxBuildHeight(), from.z);

        HitResult hit = level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean hasLineOfSight(ServerLevel level, BlockPos turretPos, Vec3 from, Vec3 to) {
        HitResult hit = level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        ));

        if (hit.getType() == HitResult.Type.MISS) return true;

        if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(turretPos)) {
            Vec3 dir = to.subtract(from).normalize();
            Vec3 newFrom = hit.getLocation().add(dir.scale(0.05));
            HitResult hit2 = level.clip(new ClipContext(
                    newFrom, to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
            ));
            if (hit2.getType() == HitResult.Type.MISS) return true;
            double hitDist = hit2.getLocation().distanceToSqr(to);
            return hitDist < 4.0;
        }

        double hitDist = hit.getLocation().distanceToSqr(to);
        return hitDist < 4.0;
    }

    private LivingEntity findBestTarget(ServerLevel level, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(SEARCH_RADIUS);

        List<Monster> monsters = level.getEntitiesOfClass(
                Monster.class,
                searchBox,
                entity -> {
                    if (!entity.isAlive() || entity.isRemoved()) return false;
                    double dx = entity.getX() - pos.getX();
                    double dz = entity.getZ() - pos.getZ();
                    return (dx * dx + dz * dz) <= SEARCH_RADIUS_SQR;
                }
        );

        if (monsters.isEmpty()) return null;

        List<TargetScore> scoredTargets = new ArrayList<>();

        for (Monster monster : monsters) {
            Vec3 targetCenter = monster.getBoundingBox().getCenter();

            if (!hasOpenSky(level, targetCenter)) continue;

            double distSqr = monster.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            double score = calculateTargetScore(monster, distSqr, Vec3.atCenterOf(pos));
            scoredTargets.add(new TargetScore(monster, score));
        }

        if (scoredTargets.isEmpty()) return null;

        scoredTargets.sort(Comparator.comparingDouble(ts -> ts.score));
        return scoredTargets.get(0).entity;
    }

    private double calculateTargetScore(Monster monster, double distSqr, Vec3 turretPos) {
        double score = Math.sqrt(distSqr);

        if (monster.getTarget() instanceof net.minecraft.world.entity.player.Player) {
            score *= 0.3;
        }

        if (monster.getLastHurtByMob() != null) {
            score *= 0.6;
        }

        if (!monster.onGround()) {
            score *= 1.3;
        }

        score *= (1.0 + monster.getHealth() / monster.getMaxHealth());

        return score;
    }

    private void startSalvo() {
        isSalvoActive = true;
        salvoCounter = 0;
        salvoTimer = 0;
    }

    private void fireMissile(ServerLevel level, BlockPos pos, BlockState state) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            isSalvoActive = false;
            return;
        }

        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS, 2.0F, 0.8F + level.random.nextFloat() * 0.4F);

        net.minecraft.core.Direction facing = state.getValue(
                com.trd.block.basic.weapons.MissileTurretBlock.FACING);

        Vec3 basePos = Vec3.atCenterOf(pos).add(0, LAUNCH_HEIGHT, 0);
        Vec3 launchPos;

        Vec3 right = new Vec3(-facing.getStepZ(), 0, facing.getStepX());
        Vec3 up = new Vec3(0, 1, 0);

        switch (salvoCounter) {
            case 0 -> launchPos = basePos.add(right.scale(0.25)).add(up.scale(0.15));
            case 1 -> launchPos = basePos.add(right.scale(-0.25)).add(up.scale(0.15));
            case 2 -> launchPos = basePos.add(right.scale(-0.25)).add(up.scale(-0.15));
            default -> launchPos = basePos;
        }

        double spreadX = (level.random.nextDouble() - 0.5) * 0.1;
        double spreadZ = (level.random.nextDouble() - 0.5) * 0.1;
        launchPos = launchPos.add(spreadX, 0, spreadZ);

        com.trd.entity.weapons.missiles.MissileLightEntity missile =
                new com.trd.entity.weapons.missiles.MissileLightEntity(
                        level, launchPos, currentTarget, null);
        level.addFreshEntity(missile);
    }

    public int getCooldownProgress() {
        if (isSalvoActive) return 15;
        if (cooldownTimer <= 0) return 0;
        return (int) (15.0 * (1.0 - (double) cooldownTimer / COOLDOWN_TICKS));
    }

    public void onPlace() {
        cooldownTimer = 40;
    }

    public void onRemove() {}

    // [НОВОЕ] Методы для GUI (1:1 с TurretLightPlacerBlockEntity)
    public void togglePower() {
        this.isSwitchedOn = !this.isSwitchedOn;
        if (this.isSwitchedOn) {
            this.bootTimer = BOOT_DURATION;
        } else {
            this.bootTimer = 0;
        }
        setChanged();
    }

    public void toggleExtraButton(int buttonId) {
        if (buttonId == 1) {
            this.extraButton1 = !this.extraButton1;
        } else if (buttonId == 2) {
            this.extraButton2 = !this.extraButton2;
        }
        setChanged();
    }

    public void updateAttackSetting(int index, boolean value) {
        switch (index) {
            case 0 -> this.targetHostile = value;
            case 1 -> this.targetNeutral = value;
            case 2 -> this.targetPlayers = value;
        }
        setChanged();
    }

    public void incrementKills() {
        this.killCount++;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldownTimer);
        tag.putBoolean("SalvoActive", isSalvoActive);
        tag.putInt("SalvoCounter", salvoCounter);
        tag.put("AmmoContainer", ammoContainer.serializeNBT());

        // [НОВОЕ] Сохранение GUI-данных
        tag.putBoolean("SwitchedOn", isSwitchedOn);
        tag.putInt("BootTimer", bootTimer);
        tag.putBoolean("TargetHostile", targetHostile);
        tag.putBoolean("TargetNeutral", targetNeutral);
        tag.putBoolean("TargetPlayers", targetPlayers);
        tag.putInt("KillCount", killCount);
        tag.putLong("Lifetime", lifetimeTicks);
        tag.putBoolean("ExtraButton1", extraButton1);
        tag.putBoolean("ExtraButton2", extraButton2);
        tag.putLong("Energy", energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cooldownTimer = tag.getInt("Cooldown");
        isSalvoActive = tag.getBoolean("SalvoActive");
        salvoCounter = tag.getInt("SalvoCounter");
        if (tag.contains("AmmoContainer")) ammoContainer.deserializeNBT(tag.getCompound("AmmoContainer"));
        // [НОВОЕ] Загрузка GUI-данных
        isSwitchedOn = tag.getBoolean("SwitchedOn");
        bootTimer = tag.getInt("BootTimer");
        targetHostile = tag.getBoolean("TargetHostile");
        targetNeutral = tag.getBoolean("TargetNeutral");
        targetPlayers = tag.getBoolean("TargetPlayers");
        killCount = tag.getInt("KillCount");
        lifetimeTicks = tag.getLong("Lifetime");
        extraButton1 = tag.getBoolean("ExtraButton1");
        extraButton2 = tag.getBoolean("ExtraButton2");
        energy = tag.getLong("Energy");
    }

    // [НОВОЕ] ContainerData для синхронизации с GUI
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(MissileTurretBlockEntity.this.energy, Integer.MAX_VALUE);
                case 1 -> (int) Math.min(MissileTurretBlockEntity.this.capacity, Integer.MAX_VALUE);
                case 2 -> getStatusInt();
                case 3 -> MissileTurretBlockEntity.this.isSwitchedOn ? 1 : 0;
                case 4 -> MissileTurretBlockEntity.this.bootTimer;
                case 5 -> MissileTurretBlockEntity.this.targetHostile ? 1 : 0;
                case 6 -> MissileTurretBlockEntity.this.targetNeutral ? 1 : 0;
                case 7 -> MissileTurretBlockEntity.this.targetPlayers ? 1 : 0;
                case 8 -> MissileTurretBlockEntity.this.killCount;
                case 9 -> (int)(MissileTurretBlockEntity.this.lifetimeTicks / 20);
                case 10 -> MissileTurretBlockEntity.this.extraButton1 ? 1 : 0;
                case 11 -> MissileTurretBlockEntity.this.extraButton2 ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MissileTurretBlockEntity.this.energy = value;
                case 3 -> MissileTurretBlockEntity.this.isSwitchedOn = (value == 1);
                case 5 -> MissileTurretBlockEntity.this.targetHostile = (value == 1);
                case 6 -> MissileTurretBlockEntity.this.targetNeutral = (value == 1);
                case 7 -> MissileTurretBlockEntity.this.targetPlayers = (value == 1);
            }
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    private int getStatusInt() {
        if (!isSwitchedOn) return 0;
        if (bootTimer > 0) return 200 + (int)((1.0 - (double)bootTimer / BOOT_DURATION) * 100);
        if (isSalvoActive) return 1;
        if (cooldownTimer > 0) return 1000 + cooldownTimer;
        return 1;
    }

    public ContainerData getDataAccess() { return this.data; }

    private record TargetScore(Monster entity, double score) {}
}