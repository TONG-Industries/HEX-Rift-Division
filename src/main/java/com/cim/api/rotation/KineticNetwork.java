package com.cim.api.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.cim.api.rotation.Rotational; // Твой интерфейс для получения Speed/Torque

import static com.cim.main.CrustalIncursionMod.LOGGER;

public class KineticNetwork {
    private final UUID networkId;

    // Все участники сети (валы, шестерни и т.д.)
    private final Set<BlockPos> members = new HashSet<>();
    // Только источники энергии (моторы, ветряки)
    private final Set<BlockPos> generators = new HashSet<>();

    private long currentSpeed = 0;
    private long totalGeneratedTorque = 0;
    private long totalConsumedTorque = 0;
    private long totalInertia = 1; // Защита от деления на ноль
    private long totalFriction = 0;
    private long targetNetworkSpeed = 0;
    private boolean needsRecalculation = true;// К чему стремится мотор

    public KineticNetwork() {
        this.networkId = UUID.randomUUID();
    }

    // Конструктор специально для загрузки сети из памяти
    public KineticNetwork(UUID id) {
        this.networkId = id;
    }

    public UUID getId() {
        return networkId;
    }

    /**
     * Главный мозг сети. Вызывается при любом изменении состава блоков.
     */

    public boolean tick(ServerLevel level) {
        if (members.isEmpty())
            return false;

        if (this.needsRecalculation) {
            this.recalculate(level);
            this.needsRecalculation = false;
        }

        long oldSpeed = this.currentSpeed;
        long deltaSpeed = 0;

        // DIAGNOSTIC: логируем состояние при торможении только в debug-режиме
        if (totalGeneratedTorque == 0 && this.currentSpeed != 0) {
            LOGGER.debug(
                    "[Kinetic-DIAG] Network {} BRAKING: speed={}, torque={}, friction={}, inertia={}, generators={}",
                    networkId.toString().substring(0, 8),
                    this.currentSpeed, this.totalGeneratedTorque, this.totalFriction, this.totalInertia,
                    generators.size());
        }

        // 1. РАЗГОН (если генераторы работают)
        if (totalGeneratedTorque > 0) {
            // Формула из плана
            long effectiveTorque = totalGeneratedTorque - totalFriction - totalConsumedTorque;

            if (effectiveTorque > 0) {
                // Умножаем на 10 для запаса точности при RPM-масштабе
                deltaSpeed = (effectiveTorque * 10) / totalInertia;
                if (deltaSpeed == 0)
                    deltaSpeed = 1; // Минимальный шаг

                // Направляем ускорение в нужную сторону
                deltaSpeed = targetNetworkSpeed > 0 ? deltaSpeed : -deltaSpeed;

                // Не разгоняемся быстрее целевой скорости за один тик
                if (targetNetworkSpeed > 0 && currentSpeed + deltaSpeed > targetNetworkSpeed) {
                    deltaSpeed = targetNetworkSpeed - currentSpeed;
                } else if (targetNetworkSpeed < 0 && currentSpeed + deltaSpeed < targetNetworkSpeed) {
                    deltaSpeed = targetNetworkSpeed - currentSpeed;
                }
            } else if (this.currentSpeed != 0) {
                // Плавное замедление (торможение под нагрузкой)
                long overload = Math.abs(effectiveTorque);
                long overloadBrake = overload / totalInertia;
                long percentBrake = Math.abs(this.currentSpeed) / 10;
                deltaSpeed = Math.max(Math.max(overloadBrake, percentBrake), 1);

                if (this.currentSpeed > 0) {
                    deltaSpeed = -Math.min(deltaSpeed, this.currentSpeed);
                } else {
                    deltaSpeed = Math.min(deltaSpeed, -this.currentSpeed);
                }
            }
        }
        // 2. ИНЕРЦИЯ И ОСТАНОВКА (моторы выключены) [cite: 30]
        else {
            if (this.currentSpeed != 0) {
                // Мгновенная остановка при минимальной скорости
                if (Math.abs(this.currentSpeed) <= 1) {
                    deltaSpeed = -this.currentSpeed; // Прямое обнуление
                } else {
                    // Агрессивное торможение: берём максимум из трения и 10% текущей скорости
                    long frictionBrake = totalFriction / totalInertia;
                    long percentBrake = Math.abs(this.currentSpeed) / 10; // 10% от текущей скорости
                    deltaSpeed = Math.max(Math.max(frictionBrake, percentBrake), 1); // Минимум 1

                    // Трение всегда против движения [cite: 28, 29]
                    if (this.currentSpeed > 0) {
                        deltaSpeed = -Math.min(deltaSpeed, this.currentSpeed);
                    } else {
                        deltaSpeed = Math.min(deltaSpeed, -this.currentSpeed);
                    }
                }
            }
        }

        // 3. ПРИМЕНЯЕМ УСКОРЕНИЕ
        if (deltaSpeed != 0) {
            long newSpeed = this.currentSpeed + deltaSpeed;

            // DIAGNOSTIC (debug only)
            if (totalGeneratedTorque == 0) {
                LOGGER.debug("[Kinetic-DIAG] Network {} APPLYING: delta={}, oldSpeed={}, newSpeed={}",
                        networkId.toString().substring(0, 8), deltaSpeed, this.currentSpeed, newSpeed);
            }

            // Ограничения скорости
            if (totalGeneratedTorque > 0) {
                // Не даем разогнаться быстрее мотора
                if ((targetNetworkSpeed > 0 && newSpeed > targetNetworkSpeed) ||
                        (targetNetworkSpeed < 0 && newSpeed < targetNetworkSpeed)) {
                    newSpeed = targetNetworkSpeed;
                }
            } else {
                // Если тормозим, то останавливаемся ровно в нуле [cite: 30]
                if ((this.currentSpeed > 0 && newSpeed < 0) ||
                        (this.currentSpeed < 0 && newSpeed > 0)) {
                    newSpeed = 0;
                }
            }

            // 4. РАССЫЛАЕМ ОБНОВЛЕНИЯ
            if (this.currentSpeed != newSpeed) {
                this.currentSpeed = newSpeed;

                // Флаг: достигли ли мы предела разгона или полной остановки?
                boolean reachedTarget = (this.currentSpeed == targetNetworkSpeed) || (this.currentSpeed == 0);

                for (BlockPos pos : members) {
                    if (level.isLoaded(pos)) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof Rotational node) {
                            node.setSpeed(this.currentSpeed);

                            // Если стабилизировались — заставляем клиент обновиться на 100%
                            if (reachedTarget) {
                                node.forceSyncVisuals(level, pos);
                            }
                        }
                    }
                }
            }
        }
        return oldSpeed != this.currentSpeed;
    }

    public void recalculate(ServerLevel level) {
        this.totalGeneratedTorque = 0;
        this.totalInertia = 0;
        this.totalFriction = 0;
        this.totalConsumedTorque = 0;
        this.targetNetworkSpeed = 0;

        // 1. Собираем физику со всех участников
        // Инерция и момент приводятся к первичному валу (ratio = networkScale блока):
        // - Инерция: I_ref = I * ratio² (маховик на вторичном валу тяжелее «кажется» с
        // запасом)
        // - Потреблённый момент: T_ref = T / ratio (момент на вторичном делится на
        // передаточное число)
        for (BlockPos pos : members) {
            // ВАЖНО: Проверяем загрузку чанка! [cite: 42]
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof Rotational node) {
                float scale = node.getNetworkScale();
                float absScale = Math.abs(scale);

                // Инерция приведённая: I * ratio²
                this.totalInertia += (long) (node.getInertiaContribution() * absScale * absScale);

                // Трение приведённое: Fr * ratio²
                this.totalFriction += (long) (node.getFrictionContribution() * absScale * absScale);

                // Потреблённый момент приведённый: T / ratio (если ratio != 0)
                if (absScale > 0.001f) {
                    this.totalConsumedTorque += (long) (node.getConsumedTorque() * node.getFrictionMultiplier()
                            / absScale);
                }

                node.setSpeed(this.currentSpeed);
            }
        }

        // 2. Опрашиваем генераторы
        for (BlockPos genPos : generators) {
            if (level.isLoaded(genPos) && level.getBlockEntity(genPos) instanceof Rotational gen) {
                totalGeneratedTorque += gen.getTorque();

                if (targetNetworkSpeed == 0) {
                    long speed = gen.getGeneratedSpeed();
                    // ... (Тут твоя текущая магия синхронизации осей через Direction)
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(genPos);
                    if (state.hasProperty(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                        net.minecraft.core.Direction facing = state
                                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
                        if (facing == net.minecraft.core.Direction.SOUTH || facing == net.minecraft.core.Direction.EAST
                                || facing == net.minecraft.core.Direction.UP) {
                            speed = -speed;
                        }
                    }
                    targetNetworkSpeed = speed;
                }
            }
        }

        // Защита от нулевой инерции
        if (this.totalInertia <= 0)
            this.totalInertia = 1;
    }

    public boolean checkConflict(ServerLevel level) {
        if (generators.size() <= 1)
            return false;

        BlockPos rootPos = generators.iterator().next();
        BlockEntity rootBE = level.getBlockEntity(rootPos);
        if (!(rootBE instanceof Rotational rootNode))
            return false;

        long rootBaseSpeed = rootNode.getGeneratedSpeed();
        net.minecraft.world.level.block.state.BlockState rootState = level.getBlockState(rootPos);
        if (rootState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            net.minecraft.core.Direction facing = rootState
                    .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            if (facing == net.minecraft.core.Direction.SOUTH || facing == net.minecraft.core.Direction.EAST
                    || facing == net.minecraft.core.Direction.UP) {
                rootBaseSpeed = -rootBaseSpeed;
            }
        }

        float expectedRootSpeed = rootBaseSpeed;
        float globalVector = expectedRootSpeed * rootNode.getNetworkScale();

        for (BlockPos genPos : generators) {
            if (genPos.equals(rootPos))
                continue;

            BlockEntity be = level.getBlockEntity(genPos);
            if (be instanceof Rotational gen) {
                long genBaseSpeed = gen.getGeneratedSpeed();
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(genPos);
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                    net.minecraft.core.Direction facing = state
                            .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
                    if (facing == net.minecraft.core.Direction.SOUTH || facing == net.minecraft.core.Direction.EAST
                            || facing == net.minecraft.core.Direction.UP) {
                        genBaseSpeed = -genBaseSpeed;
                    }
                }

                float expectedSpeed = genBaseSpeed;
                float localVector = expectedSpeed * gen.getNetworkScale();

                if (Math.signum(globalVector) != Math.signum(localVector) && globalVector != 0 && localVector != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateMembers(ServerLevel level) {
        for (BlockPos pos : members) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Rotational node) {
                node.setSpeed(this.currentSpeed);
                // Тут мы будем вызывать обновление визуалов Flywheel через пакеты [cite: 11,
                // 12]
            }
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("Id", networkId);
        nbt.putLong("Speed", currentSpeed);

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
        // Создаем сеть с сохраненным UUID
        KineticNetwork net = new KineticNetwork(nbt.getUUID("Id"));
        net.currentSpeed = nbt.getLong("Speed");

        ListTag membersTag = nbt.getList("Members", Tag.TAG_LONG);
        for (int i = 0; i < membersTag.size(); i++) {
            // Берем тег из списка, кастуем его к LongTag и вытаскиваем long
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

    public void addMember(BlockPos pos) {
        this.members.add(pos);
    }

    public void addGenerator(BlockPos pos) {
        this.generators.add(pos);
        this.members.add(pos); // Генератор всегда является и обычным участником [cite: 2]
    }

    public void removeMember(BlockPos pos) {
        this.members.remove(pos);
        this.generators.remove(pos);
    }

    public void requestRecalculation() {
        this.needsRecalculation = true;
    }

    // Также добавь геттер для members, если его нет (нужен для Merge)
    public Set<BlockPos> getMembers() {
        return members;
    }

    // Добавь эти методы в KineticNetwork.java
    public long getSpeed() {
        return currentSpeed;
    }

    public Set<BlockPos> getGenerators() {
        return generators;
    }

    public long getTargetSpeed() {
        return targetNetworkSpeed;
    }

    public void setCurrentSpeed(long speed) {
        this.currentSpeed = speed;
    }

    public long getTotalTorque() {
        return totalGeneratedTorque;
    }

    public long getTotalInertia() {
        return totalInertia;
    }

    public long getTotalFriction() {
        return totalFriction;
    }

}