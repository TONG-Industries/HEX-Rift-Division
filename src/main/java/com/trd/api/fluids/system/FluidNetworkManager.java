package com.trd.api.fluids.system;

import com.trd.block.entity.industrial.fluids.FluidPipeBlockEntity;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;

public class FluidNetworkManager extends SavedData {
    private static final String DATA_NAME = "trd_fluid_networks";

    private final ServerLevel level;
    private final Long2ObjectMap<FluidNode> allNodes = new Long2ObjectOpenHashMap<>();
    private final Set<FluidNetwork> networks = Sets.newHashSet();

    public FluidNetworkManager(ServerLevel level, CompoundTag nbt) {
        this(level);
        if (nbt.contains("nodes")) {
            long[] nodePositions = nbt.getLongArray("nodes");
            for (long posLong : nodePositions) {
                allNodes.put(posLong, new FluidNode(BlockPos.of(posLong)));
            }
        }
    }

    public FluidNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static FluidNetworkManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                nbt -> new FluidNetworkManager(level, nbt),
                () -> new FluidNetworkManager(level),
                DATA_NAME
        );
    }

    public void rebuildAllNetworks() {
        networks.clear();
        for (FluidNode node : allNodes.values()) {
            node.setNetwork(null);
        }

        allNodes.values().removeIf(node -> !node.isValid(level));

        Set<FluidNode> processedNodes = new java.util.HashSet<>();

        for (FluidNode startNode : allNodes.values()) {
            if (processedNodes.contains(startNode)) {
                continue;
            }

            FluidNetwork newNetwork = new FluidNetwork(this);
            networks.add(newNetwork);

            java.util.Queue<FluidNode> queue = new java.util.LinkedList<>();
            queue.add(startNode);
            processedNodes.add(startNode);

            while (!queue.isEmpty()) {
                FluidNode currentNode = queue.poll();
                newNetwork.addNode(currentNode);

                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = currentNode.getPos().relative(dir);
                    if (!canConnectLogically(currentNode.getPos(), neighborPos)) {
                        continue;
                    }

                    FluidNode neighbor = allNodes.get(neighborPos.asLong());

                    if (neighbor != null && !processedNodes.contains(neighbor)) {
                        processedNodes.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        setDirty();
    }

    public void tick() {
        // 1. Делаем копию списка сетей (new HashSet) и тикаем каждую из них.
        // Переменную в лямбде назовем 'net', чтобы точно ничего не конфликтовало.
        new java.util.HashSet<>(networks).forEach(net -> net.tick(level));

        // 2. Безопасно удаляем все пустые сети в одну строчку (без создания доп. списков)
        networks.removeIf(FluidNetwork::isEmpty);
    }

    // ==================== ЛОГИКА ДОБАВЛЕНИЯ УЗЛА ====================
    public void addNode(BlockPos pos) {
        addNode(pos, null);
    }

    private void addNode(BlockPos pos, @Nullable FluidNetwork networkToAvoid) {
        if (allNodes.containsKey(pos.asLong())) return;

        FluidNode newNode = new FluidNode(pos);
        allNodes.put(pos.asLong(), newNode);
        setDirty();

        FluidNetwork assignedNetwork = null;

        // Ищем соседей (6 сторон)
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidNode neighborNode = allNodes.get(neighborPos.asLong());

            if (neighborNode != null && neighborNode.getNetwork() != networkToAvoid) {
                // ПРОВЕРКА ФИЛЬТРОВ! Соединяем сети, только если жидкости совпадают (или одна пустая)
                if (canConnectLogically(pos, neighborPos)) {
                    FluidNetwork neighborNetwork = neighborNode.getNetwork();
                    if (neighborNetwork != null) {
                        if (assignedNetwork == null) {
                            assignedNetwork = neighborNetwork;
                            assignedNetwork.addNode(newNode);
                        } else if (assignedNetwork != neighborNetwork) {
                            // --- ЗАЩИТА ОТ РАЗРЫВОВ ПРИ СЛИЯНИИ ---
                            if (neighborNetwork.getNodeCount() > assignedNetwork.getNodeCount()) {
                                neighborNetwork.merge(assignedNetwork);
                                assignedNetwork = neighborNetwork; // <--- ОБЯЗАТЕЛЬНО!
                            } else {
                                assignedNetwork.merge(neighborNetwork);
                            }
                        }
                    }
                }
            }
        }

        if (assignedNetwork == null) {
            assignedNetwork = new FluidNetwork(this);
            assignedNetwork.addNode(newNode);
            networks.add(assignedNetwork);
        }
    }

    // ==================== ПРОВЕРКА ФИЛЬТРОВ И МАШИН ====================
    private boolean canConnectLogically(BlockPos pos1, BlockPos pos2) {
        if (!level.isLoaded(pos1) || !level.isLoaded(pos2)) {
            // Разрешаем соединение условно, чтобы не разорвать выгруженную сеть.
            return true;
        }

        BlockEntity be1 = level.getBlockEntity(pos1);
        BlockEntity be2 = level.getBlockEntity(pos2);

        boolean isPipe1 = be1 instanceof FluidPipeBlockEntity;
        boolean isPipe2 = be2 instanceof FluidPipeBlockEntity;

        // 1. Если это две трубы - они должны иметь одинаковый фильтр
        if (isPipe1 && isPipe2) {
            net.minecraft.world.level.material.Fluid f1 = ((FluidPipeBlockEntity) be1).getFilterFluid();
            net.minecraft.world.level.material.Fluid f2 = ((FluidPipeBlockEntity) be2).getFilterFluid();
            return f1 == f2;
        }

        // --- ФИКС ДЛЯ МУЛЬТИБЛОКОВ ---
        // Если чанк контроллера выгружен, getCapability() вернет empty.
        // Поэтому мы жестко проверяем роль части мультиблока, чтобы не разрывать сеть!
        if (isPipe1 && be2 instanceof com.trd.multiblock.system.MultiblockPartEntity part2) {
            com.trd.multiblock.system.PartRole role = part2.getPartRole();
            if (role == com.trd.multiblock.system.PartRole.FLUID_CONNECTOR || role == com.trd.multiblock.system.PartRole.UNIVERSAL_CONNECTOR || role == com.trd.multiblock.system.PartRole.FLUID_INPUT || role == com.trd.multiblock.system.PartRole.FLUID_OUTPUT || role == com.trd.multiblock.system.PartRole.FLUID_LADDER) {
                return true;
            }
        }
        if (isPipe2 && be1 instanceof com.trd.multiblock.system.MultiblockPartEntity part1) {
            com.trd.multiblock.system.PartRole role = part1.getPartRole();
            if (role == com.trd.multiblock.system.PartRole.FLUID_CONNECTOR || role == com.trd.multiblock.system.PartRole.UNIVERSAL_CONNECTOR || role == com.trd.multiblock.system.PartRole.FLUID_INPUT || role == com.trd.multiblock.system.PartRole.FLUID_OUTPUT || role == com.trd.multiblock.system.PartRole.FLUID_LADDER) {
                return true;
            }
        }

        // 2. Если Труба соединяется с Бочкой (или любой другой машиной с баком)
        if (isPipe1 && be2 != null) {
            return be2.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).isPresent();
        }
        if (isPipe2 && be1 != null) {
            return be1.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).isPresent();
        }

        // 3. Если две бочки/машины стоят впритык друг к другу
        if (!isPipe1 && !isPipe2 && be1 != null && be2 != null) {
            if (be1 instanceof com.trd.multiblock.system.MultiblockPartEntity || be2 instanceof com.trd.multiblock.system.MultiblockPartEntity) return true;
            return be1.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).isPresent() &&
                    be2.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).isPresent();
        }

        return false;
    }

    // ==================== УДАЛЕНИЕ УЗЛА ====================
    public void removeNode(BlockPos pos) {
        FluidNode node = allNodes.remove(pos.asLong());
        if (node == null) return;

        FluidNetwork network = node.getNetwork();
        if (network != null) {
            network.removeNode(node);
        }
        setDirty();
    }

    void reAddNode(BlockPos pos, @Nullable FluidNetwork networkToAvoid) {
        allNodes.remove(pos.asLong());
        addNode(pos, networkToAvoid);
    }

    public void removeNetwork(FluidNetwork network) {
        networks.remove(network);
    }

    // ==================== УТИЛИТЫ И ОТЛАДКА ====================
    public boolean hasNode(BlockPos pos) {
        return allNodes.containsKey(pos.asLong());
    }

    public void debugLog() {
        if (networks.isEmpty()) return;

        System.out.println("=== FLUID NETWORK STATUS ===");
        System.out.println("Total registered pipes (nodes): " + allNodes.size());
        System.out.println("Active networks: " + networks.size());

        int i = 1;
        for (FluidNetwork net : networks) {
            System.out.println("  Network #" + i + " | Nodes: " + net.getNodeCount());
            i++;
        }
        System.out.println("==============================");
    }

    public FluidNetwork getNetwork(BlockPos pos) {
        FluidNode node = allNodes.get(pos.asLong());
        return node != null ? node.getNetwork() : null;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        nbt.putLongArray("nodes", allNodes.keySet().toLongArray());
        return nbt;
    }
}
