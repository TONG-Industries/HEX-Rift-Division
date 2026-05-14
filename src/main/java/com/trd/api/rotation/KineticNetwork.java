package com.trd.api.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.trd.main.MainRegistry.LOGGER;

public class KineticNetwork {
    private final UUID networkId;

    private final Set<BlockPos> members = new HashSet<>();
    private final Set<BlockPos> generators = new HashSet<>();
    
    private long currentSpeed = 0;
    private long totalGeneratedTorque = 0;
    private long totalConsumedTorque = 0;
    private double totalInertia = 1.0;
    private double bearingFrictionMultiplier = 1.0;
    private long targetNetworkSpeed = 0;
    private boolean needsRecalculation = true;

    private boolean isOverloaded = false;
    private double loadFactor = 0;

    public KineticNetwork() {
        this.networkId = UUID.randomUUID();
    }

    public KineticNetwork(UUID id) {
        this.networkId = id;
    }

    public UUID getId() {
        return networkId;
    }

    public boolean tick(ServerLevel level) {
        if (members.isEmpty())
            return false;

        if (this.needsRecalculation) {
            this.recalculate(level);
            this.needsRecalculation = false;
        }

        long oldSpeed = this.currentSpeed;
        
        // OVERLOAD CHECK: если сеть критически перегружена (>= 125%) — принудительно останавливаем
        if (isOverloaded && loadFactor >= 1.25) {
            this.targetNetworkSpeed = 0;
        }

        // 1. РАЗГОН / ТОРМОЖЕНИЕ
        if (totalGeneratedTorque > 0) {
            long effectiveTorque = totalGeneratedTorque - totalConsumedTorque;
            if (loadFactor < 1.25 && effectiveTorque <= 0 && Math.abs(this.currentSpeed) < Math.abs(this.targetNetworkSpeed)) {
                effectiveTorque = 1;
            }

            long deltaSpeed = (long) (effectiveTorque * 10 / totalInertia);
            if (deltaSpeed == 0 && effectiveTorque != 0) deltaSpeed = (long) Math.signum(effectiveTorque);

            if (this.currentSpeed < targetNetworkSpeed) {
                this.currentSpeed = Math.min(targetNetworkSpeed, this.currentSpeed + Math.max(1, deltaSpeed));
            } else if (this.currentSpeed > targetNetworkSpeed) {
                this.currentSpeed = Math.max(targetNetworkSpeed, this.currentSpeed - Math.max(1, Math.abs(deltaSpeed)));
            }
        } else {
            // ТОРМОЖЕНИЕ (Инерция)
            long frictionTorque = (long) (2 + totalInertia * 0.01);
            long deltaSpeed = (long) (frictionTorque * 5 / totalInertia);
            if (deltaSpeed == 0) deltaSpeed = 1;

            if (this.currentSpeed > 0) {
                this.currentSpeed = Math.max(0, this.currentSpeed - deltaSpeed);
            } else if (this.currentSpeed < 0) {
                this.currentSpeed = Math.min(0, this.currentSpeed + deltaSpeed);
            }
        }

        if (oldSpeed != this.currentSpeed) {
            updateMembers(level);
            return true;
        }
        return false;
    }

    public void recalculate(ServerLevel level) {
        this.totalGeneratedTorque = 0;
        this.totalInertia = 0.0;
        this.bearingFrictionMultiplier = 1.0;
        this.totalConsumedTorque = 0;
        this.targetNetworkSpeed = 0;

        // 1. Собираем физику со всех участников
        for (BlockPos pos : members) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof Rotational node) {
                float scale = node.getNetworkScale();
                float absScale = Math.abs(scale);

                this.totalInertia += node.getInertiaContribution();
                this.bearingFrictionMultiplier += node.getBearingFrictionCoefficient();

                if (absScale > 0.001f) {
                    this.totalConsumedTorque += (long) (node.getConsumedTorque() * node.getFrictionMultiplier() * absScale);
                }
                node.setSpeed(this.currentSpeed);
                checkNodeFailure(level, pos, node);
            }
        }

        this.totalConsumedTorque = (long) (this.totalConsumedTorque * this.bearingFrictionMultiplier);

        // 2. Опрашиваем генераторы
        for (BlockPos genPos : generators) {
            if (level.isLoaded(genPos) && level.getBlockEntity(genPos) instanceof Rotational gen) {
                totalGeneratedTorque += gen.getTorque();

                long genSpeed = gen.getGeneratedSpeed();
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(genPos);
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                    net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
                    if (facing == net.minecraft.core.Direction.SOUTH || facing == net.minecraft.core.Direction.EAST || facing == net.minecraft.core.Direction.UP) {
                        genSpeed = -genSpeed;
                    }
                }

                if (targetNetworkSpeed == 0) {
                    targetNetworkSpeed = genSpeed;
                } else {
                    if (genSpeed != targetNetworkSpeed) {
                        KineticNetworkManager.get(level).scheduleBreakage(genPos);
                    }
                }
            }
        }

        // 3. OVERLOAD CHECK
        if (totalGeneratedTorque > 0) {
            this.loadFactor = (double) totalConsumedTorque / totalGeneratedTorque;
        } else {
            this.loadFactor = totalConsumedTorque > 0 ? Double.POSITIVE_INFINITY : 0;
        }

        this.isOverloaded = this.loadFactor > 1.0;

        if (this.isOverloaded) {
            if (this.loadFactor >= 1.25) {
                this.targetNetworkSpeed = 0;
            } else {
                double multiplier = (1.25 - this.loadFactor) / 0.25;
                this.targetNetworkSpeed = (long) (this.targetNetworkSpeed * multiplier);
            }
        }

        if (this.totalInertia <= 0) this.totalInertia = 0.1;

        // 4. STRUCTURAL INTEGRITY CHECK (Torque limit)
        for (BlockPos pos : members) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof Rotational node) {
                if (totalGeneratedTorque > node.getMaxTorque()) {
                    KineticNetworkManager.get(level).scheduleStructuralFailure(pos);
                }
            }
        }
    }

    private void checkNodeFailure(ServerLevel level, BlockPos pos, Rotational node) {
        if (Math.abs(node.getSpeed()) > node.getMaxSpeed()) {
            KineticNetworkManager.get(level).scheduleStructuralFailure(pos);
        }
    }

    private void updateMembers(ServerLevel level) {
        for (BlockPos pos : members) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Rotational node) {
                node.setSpeed(this.currentSpeed);
                checkNodeFailure(level, pos, node);
                
                if (totalGeneratedTorque > node.getMaxTorque()) {
                    KineticNetworkManager.get(level).scheduleStructuralFailure(pos);
                }
            }
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("Id", networkId);
        nbt.putLong("Speed", currentSpeed);
        nbt.putBoolean("Overloaded", isOverloaded);
        ListTag membersTag = new ListTag();
        for (BlockPos pos : members) {
            membersTag.add(net.minecraft.nbt.LongTag.valueOf(pos.asLong()));
        }
        nbt.put("Members", membersTag);
        ListTag generatorsTag = new ListTag();
        for (BlockPos pos : generators) {
            generatorsTag.add(net.minecraft.nbt.LongTag.valueOf(pos.asLong()));
        }
        nbt.put("Generators", generatorsTag);
        return nbt;
    }

    public static KineticNetwork deserializeNBT(CompoundTag nbt) {
        KineticNetwork net = new KineticNetwork(nbt.getUUID("Id"));
        net.currentSpeed = nbt.getLong("Speed");
        net.isOverloaded = nbt.getBoolean("Overloaded");
        ListTag membersTag = nbt.getList("Members", Tag.TAG_LONG);
        for (int i = 0; i < membersTag.size(); i++) {
            long posLong = ((net.minecraft.nbt.LongTag) membersTag.get(i)).getAsLong();
            net.members.add(BlockPos.of(posLong));
        }
        ListTag gensTag = nbt.getList("Generators", Tag.TAG_LONG);
        for (int i = 0; i < gensTag.size(); i++) {
            long posLong = ((net.minecraft.nbt.LongTag) gensTag.get(i)).getAsLong();
            net.generators.add(BlockPos.of(posLong));
        }
        return net;
    }

    public void addMember(BlockPos pos) { this.members.add(pos); }
    public void addGenerator(BlockPos pos) { this.generators.add(pos); this.members.add(pos); }
    public void removeMember(BlockPos pos) { this.members.remove(pos); this.generators.remove(pos); }
    public void requestRecalculation() { this.needsRecalculation = true; }
    public Set<BlockPos> getMembers() { return members; }
    public long getSpeed() { return currentSpeed; }
    public Set<BlockPos> getGenerators() { return generators; }
    public long getTargetSpeed() { return targetNetworkSpeed; }
    public void setCurrentSpeed(long speed) { this.currentSpeed = speed; }
    public long getTotalTorque() { return totalGeneratedTorque; }
    public double getTotalInertia() { return totalInertia; }
    public double getFrictionMultiplier() { return bearingFrictionMultiplier; }
    public boolean isOverloaded() { return isOverloaded; }
    public long getTotalConsumedTorque() { return totalConsumedTorque; }
    public double getLoadFactor() { return loadFactor; }
}