package com.trd.block.entity.weapons;

import com.trd.block.entity.ModBlockEntities;
import com.trd.api.energy.EnergyNetworkManager;
import com.trd.api.energy.IEnergyConnector;
import com.trd.api.energy.IEnergyReceiver;
import com.trd.block.entity.industrial.energy.EnergyNodeBlockEntity;
import com.trd.capability.ModCapabilities;
import com.trd.item.energy.ModBatteryItem;
import com.trd.item.energy.EnergyCellItem;
import com.trd.item.weapons.missiles.MissileItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MissileTurretBlockEntity extends EnergyNodeBlockEntity {

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

    // === ЭНЕРГОСИСТЕМА ===
    private final long MAX_RECEIVE = 10000;

    // === БОЕПРИПАСЫ ===
    private final MissileAmmoContainer missileContainer = new MissileAmmoContainer();
    private final LazyOptional<ItemStackHandler> itemHandlerOptional = LazyOptional.of(() -> missileContainer);

    // GUI-related поля
    private boolean isSwitchedOn = false;
    private int bootTimer = 0;
    private static final int BOOT_DURATION = 60;

    private boolean targetHostile = true;
    private boolean targetNeutral = false;
    private boolean targetPlayers = true;

    private int killCount = 0;
    private long lifetimeTicks = 0;

    // Дополнительные кнопки
    private boolean extraButton1 = true;
    private boolean extraButton2 = true;

    // Кэш статуса для GUI
    private int cachedStatus = 0;

    // Тип ракеты для текущего залпа
    private String currentMissileType = "standard";

    public MissileTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_TURRET_BE.get(), pos, state);
        this.capacity = 100000;
        this.missileContainer.setOnContentsChanged(this::setChanged);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MissileTurretBlockEntity turret) {
        if (level.isClientSide) return;
        turret.lifetimeTicks++;
        turret.tickServer((ServerLevel) level, pos, state);
    }

    private void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        // === ЗАРЯДКА ОТ БАТАРЕЙКИ В СЛОТЕ 10 ===
        ItemStack batteryStack = this.missileContainer.getStackInSlot(MissileAmmoContainer.BATTERY_SLOT_INDEX);
        if (!batteryStack.isEmpty() && this.energy < this.capacity) {
            batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energyStorage -> {
                if (energyStorage.canExtract()) {
                    int maxReceive = (int) Math.min(this.MAX_RECEIVE, this.capacity - this.energy);
                    if (maxReceive > 0) {
                        int extracted = energyStorage.extractEnergy(maxReceive, false);
                        if (extracted > 0) {
                            this.energy += extracted;
                            this.setChanged();
                        }
                    }
                }
            });

            if (this.energy < this.capacity) {
                batteryStack.getCapability(ModCapabilities.ENERGY_PROVIDER).ifPresent(provider -> {
                    if (provider.canExtract()) {
                        long maxReceive = Math.min(this.MAX_RECEIVE, this.capacity - this.energy);
                        long extracted = provider.extractEnergy(maxReceive, false);
                        if (extracted > 0) {
                            this.energy += extracted;
                            this.setChanged();
                        }
                    }
                });
            }
        }

        // === ЛОГИКА ВЫКЛЮЧАТЕЛЯ ===
        if (!this.isSwitchedOn) {
            return;
        }

        // === ЛОГИКА ЗАГРУЗКИ ===
        if (this.bootTimer > 0) {
            this.bootTimer--;
            return;
        }

        // === ОСНОВНАЯ ЛОГИКА РАКЕТНИЦЫ ===
        if (cooldownTimer > 0) cooldownTimer--;

        long drainPerShot = 5000;

        if (isSalvoActive) {
            salvoTimer--;
            if (salvoTimer <= 0) {
                if (this.energy >= drainPerShot && this.missileContainer.hasMissiles()) {
                    this.energy -= drainPerShot;
                    this.setChanged();
                    fireMissile(level, pos, state);
                    salvoCounter++;
                    if (salvoCounter >= SALVO_SIZE) {
                        isSalvoActive = false;
                        salvoCounter = 0;
                        cooldownTimer = COOLDOWN_TICKS;
                    } else {
                        salvoTimer = SALVO_INTERVAL;
                    }
                } else {
                    isSalvoActive = false;
                    salvoCounter = 0;
                }
            }
            return;
        }

        if (cooldownTimer <= 0) {
            scanTimer--;
            if (scanTimer <= 0) {
                scanTimer = SCAN_INTERVAL;
                currentTarget = findBestTarget(level, pos);
                if (currentTarget != null) {
                    if (this.energy >= drainPerShot * SALVO_SIZE && this.missileContainer.hasMissiles()) {
                        String missileType = this.missileContainer.peekMissileType();
                        if (missileType != null) {
                            this.currentMissileType = missileType;
                            startSalvo();
                        }
                    }
                }
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

    /**
     * === НОВАЯ СИСТЕМА ПОЗИЦИЙ ПУСКА С УЧЁТОМ FACING ===
     * Координаты из Blockbench (пиксели) конвертируем в блоки.
     * Позиции относительно центра блока (локальные координаты):
     * Ракета 1: (+3.5px, 0, +3.5px) = (+0.21875, 0, +0.21875)
     * Ракета 2: (+3.5px, 0, -3.5px) = (-0.21875, 0, +0.21875)
     * Ракета 3: (-3.5px, 0, -3.5px) = (-0.21875, 0, -0.21875)
     *
     * Затем поворачиваем в зависимости от FACING блока.
     * Y = LAUNCH_HEIGHT (1.0) над блоком
     */
    private void fireMissile(ServerLevel level, BlockPos pos, BlockState state) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            isSalvoActive = false;
            return;
        }

        // Забираем ракету из контейнера
        String missileType = this.missileContainer.takeMissileAndGetType();
        if (missileType == null) {
            isSalvoActive = false;
            salvoCounter = 0;
            return;
        }

        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.BLOCKS, 2.0F, 0.8F + level.random.nextFloat() * 0.4F);

        // Базовая позиция — центр блока + высота пуска
        Vec3 basePos = Vec3.atCenterOf(pos).add(0, LAUNCH_HEIGHT, 0);

        // Позиции пуска в локальных координатах (Blockbench → блоки)
        double pixelToBlock = 1.0 / 16.0;
        double offset = 3.5 * pixelToBlock; // = 0.21875

        Vec3 localOffset;
        switch (salvoCounter) {
            case 0 -> localOffset = new Vec3(offset, 0, offset);      // +X, +Z
            case 1 -> localOffset = new Vec3(offset, 0, -offset);     // +X, -Z  (🔥 ИЗМЕНЕНО: противоположный угол)
            case 2 -> localOffset = new Vec3(-offset, 0, -offset);    // -X, -Z
            default -> localOffset = Vec3.ZERO;
        }

        // === ПОВОРОТ В ЗАВИСИМОСТИ ОТ FACING ===
        Direction facing = state.getValue(com.trd.block.basic.weapons.MissileTurretBlock.FACING);
        Vec3 worldOffset = rotateOffsetByFacing(localOffset, facing);

        Vec3 launchPos = basePos.add(worldOffset);

        com.trd.entity.weapons.missiles.MissileLightEntity missile =
                new com.trd.entity.weapons.missiles.MissileLightEntity(
                        level, launchPos, currentTarget, null, missileType);
        level.addFreshEntity(missile);
    }

    /**
     * Поворачивает локальное смещение в зависимости от направления блока.
     * Локальные координаты: +Z = "вперёд" (к лицу блока), +X = "вправо"
     */
    private Vec3 rotateOffsetByFacing(Vec3 offset, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(-offset.x, offset.y, -offset.z); // 180°
            case SOUTH -> new Vec3(offset.x, offset.y, offset.z);  // 0° (default)
            case WEST -> new Vec3(-offset.z, offset.y, offset.x);  // 90°
            case EAST -> new Vec3(offset.z, offset.y, -offset.x);  // -90°
            default -> offset;
        };
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

    // === МЕТОДЫ GUI ===

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

    // === EnergyNodeBlockEntity overrides ===

    @Override
    public long getReceiveSpeed() { return MAX_RECEIVE; }

    @Override
    public long getProvideSpeed() { return 0; }

    @Override
    public boolean canConnectEnergy(Direction side) { return side != Direction.UP; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        itemHandlerOptional.invalidate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (side == Direction.UP) return super.getCapability(cap, side);
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerOptional.invalidate();
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldownTimer);
        tag.putBoolean("SalvoActive", isSalvoActive);
        tag.putInt("SalvoCounter", salvoCounter);
        tag.put("MissileContainer", missileContainer.serializeNBT());

        tag.putBoolean("SwitchedOn", isSwitchedOn);
        tag.putInt("BootTimer", bootTimer);
        tag.putBoolean("TargetHostile", targetHostile);
        tag.putBoolean("TargetNeutral", targetNeutral);
        tag.putBoolean("TargetPlayers", targetPlayers);
        tag.putInt("KillCount", killCount);
        tag.putLong("Lifetime", lifetimeTicks);
        tag.putBoolean("ExtraButton1", extraButton1);
        tag.putBoolean("ExtraButton2", extraButton2);
        tag.putString("CurrentMissileType", currentMissileType);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cooldownTimer = tag.getInt("Cooldown");
        isSalvoActive = tag.getBoolean("SalvoActive");
        salvoCounter = tag.getInt("SalvoCounter");
        if (tag.contains("MissileContainer")) missileContainer.deserializeNBT(tag.getCompound("MissileContainer"));

        isSwitchedOn = tag.getBoolean("SwitchedOn");
        bootTimer = tag.getInt("BootTimer");
        targetHostile = tag.getBoolean("TargetHostile");
        targetNeutral = tag.getBoolean("TargetNeutral");
        targetPlayers = tag.getBoolean("TargetPlayers");
        killCount = tag.getInt("KillCount");
        lifetimeTicks = tag.getLong("Lifetime");
        extraButton1 = tag.getBoolean("ExtraButton1");
        extraButton2 = tag.getBoolean("ExtraButton2");
        if (tag.contains("CurrentMissileType")) {
            currentMissileType = tag.getString("CurrentMissileType");
        }
    }

    // === ContainerData ===

    public int getEnergyStoredInt() { return (int) Math.min(energy, Integer.MAX_VALUE); }
    public int getMaxEnergyStoredInt() { return (int) Math.min(capacity, Integer.MAX_VALUE); }

    private int getStatusInt() {
        if (!isSwitchedOn) return 0; // OFFLINE
        if (bootTimer > 0) return 200 + (int)((1.0 - (double)bootTimer / BOOT_DURATION) * 100); // BOOTING
        if (isSalvoActive) return 1; // ONLINE (стрельба)
        if (cooldownTimer > 0) return 1000 + cooldownTimer; // COOLDOWN
        if (energy < 10000) return 0; // Недостаточно энергии
        if (!missileContainer.hasMissiles()) return 3000; // NO AMMO
        return 1; // ONLINE (готов)
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getEnergyStoredInt();
                case 1 -> getMaxEnergyStoredInt();
                case 2 -> getStatusInt();
                case 3 -> isSwitchedOn ? 1 : 0;
                case 4 -> bootTimer;
                case 5 -> targetHostile ? 1 : 0;
                case 6 -> targetNeutral ? 1 : 0;
                case 7 -> targetPlayers ? 1 : 0;
                case 8 -> killCount;
                case 9 -> (int)(lifetimeTicks / 20);
                case 10 -> extraButton1 ? 1 : 0;
                case 11 -> extraButton2 ? 1 : 0;
                case 12 -> missileContainer.countMissiles();
                case 13 -> missileContainer.countMissilesByType("standard");
                case 14 -> missileContainer.countMissilesByType("he");
                case 15 -> missileContainer.countMissilesByType("fire");
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = value;
                case 3 -> isSwitchedOn = (value == 1);
                case 5 -> targetHostile = (value == 1);
                case 6 -> targetNeutral = (value == 1);
                case 7 -> targetPlayers = (value == 1);
            }
        }

        @Override
        public int getCount() {
            return 16;
        }
    };

    public ContainerData getDataAccess() { return this.data; }
    public MissileAmmoContainer getMissileContainer() { return missileContainer; }

    private record TargetScore(Monster entity, double score) {}
}