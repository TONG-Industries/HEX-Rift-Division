package com.trd.api.fluids.system;

import com.trd.block.basic.industrial.fluids.FluidPipeBlock;
import com.trd.block.entity.industrial.fluids.FluidBarrelBlockEntity;
import com.trd.block.entity.industrial.fluids.FluidPipeBlockEntity;
import com.trd.multiblock.system.IMultiblockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.*;

public class FluidNetwork {
    private final FluidNetworkManager manager;
    private final Set<FluidNode> nodes = new HashSet<>();
    private final UUID id = UUID.randomUUID();
    private boolean hasCheckedMeltdown = false;
    
    // Кэширование типа жидкости для оптимизации (чтобы не перебирать все ноды каждый тик)
    private Set<net.minecraft.world.level.material.Fluid> cachedFluids = null;
    private boolean isFluidCached = false;
    private int lastNodeCount = -1;

    public FluidNetwork(FluidNetworkManager manager) {
        this.manager = manager;
    }

    public void tick(ServerLevel level) {
        if (nodes.isEmpty()) return;
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);
        if (nodes.isEmpty()) return;

        List<IFluidHandler> pureProviders = new ArrayList<>();
        List<IFluidHandler> pureReceivers = new ArrayList<>();
        List<IFluidHandler> buffers = new ArrayList<>();

        // Set для дедупликации: один мультиблок может дать один хэндлер через несколько нод (FLUID_CONNECTOR + controller).
        // Без дедупликации один бак попадает и в buffers и в pureProviders одновременно, ломая баланс.
        Set<IFluidHandler> seenHandlers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (FluidNode node : nodes) {
            BlockPos pos = node.getPos();
            if (!level.isLoaded(pos)) continue;
            
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FluidPipeBlock)) continue;

            for (Direction dir : Direction.values()) {
                if (state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                    BlockPos neighborPos = pos.relative(dir);
                    if (!level.isLoaded(neighborPos)) continue;
                    BlockState neighborState = level.getBlockState(neighborPos);
                    
                    if (neighborState.getBlock() instanceof FluidPipeBlock) {
                        continue;
                    }
                    
                    BlockEntity be = level.getBlockEntity(neighborPos);
                    if (be == null) continue;

                    be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(handler -> {
                        if (!seenHandlers.add(handler)) return;

                        ITankWithMode tank = null;
                        if (be instanceof ITankWithMode direct) {
                            tank = direct;
                        } else if (be instanceof IMultiblockPart part && part.getControllerPos() != null) {
                            BlockEntity ctrl = level.getBlockEntity(part.getControllerPos());
                            if (ctrl instanceof ITankWithMode ctrlTank) tank = ctrlTank;
                        }

                        if (tank != null) {
                            int m = tank.getMode();
                            if (m == 1) pureReceivers.add(handler);
                            else if (m == 2) pureProviders.add(handler);
                            else buffers.add(handler);
                        } else {
                            pureProviders.add(handler);
                            pureReceivers.add(handler);
                        }
                    });
                }
            }
        }

        Set<net.minecraft.world.level.material.Fluid> allowedFluids = determineNetworkFluids(level);
        if (allowedFluids == null) return; // Сеть содержит трубы без идентификатора! Трансфер заблокирован.

        if (transfer(level, pureProviders, pureReceivers, allowedFluids)) return;
        if (transfer(level, pureProviders, buffers, allowedFluids)) return;
        if (transfer(level, buffers, pureReceivers, allowedFluids)) return;
        balance(level, buffers, allowedFluids);
    }

    public void invalidateFluidCache() {
        this.isFluidCached = false;
    }

    @javax.annotation.Nullable
    private Set<net.minecraft.world.level.material.Fluid> determineNetworkFluids(ServerLevel level) {
        if (isFluidCached && lastNodeCount == nodes.size()) {
            return cachedFluids;
        }

        boolean hasPipes = false;
        Set<net.minecraft.world.level.material.Fluid> filters = new HashSet<>();
        
        for (FluidNode node : nodes) {
            BlockPos pos = node.getPos();
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidPipeBlockEntity pipeBE) {
                hasPipes = true;
                net.minecraft.world.level.material.Fluid f = pipeBE.getFilterFluid();
                if (f == net.minecraft.world.level.material.Fluids.EMPTY) {
                    // Если хотя бы одна труба пустая, блокируем ВСЮ сеть!
                    cachedFluids = null;
                    isFluidCached = true;
                    lastNodeCount = nodes.size();
                    return null;
                }
                filters.add(f);
            }
        }
        
        if (hasPipes) {
            cachedFluids = filters;
        } else {
            cachedFluids = Collections.emptySet(); // Если труб нет вообще, разрешаем любые жидкости
        }

        isFluidCached = true;
        lastNodeCount = nodes.size();
        return cachedFluids;
    }

    private boolean transfer(ServerLevel level, List<IFluidHandler> sources, List<IFluidHandler> destinations, Set<net.minecraft.world.level.material.Fluid> allowedFluids) {
        for (IFluidHandler source : sources) {
            FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty() || available.getAmount() <= 0) continue;
            if (!allowedFluids.isEmpty() && !allowedFluids.contains(available.getFluid())) continue; // Заблокировано фильтром

            int remaining = available.getAmount();
            net.minecraft.world.level.material.Fluid fluid = available.getFluid();

            Map<IFluidHandler, Integer> validDestinations = new HashMap<>();
            long totalCapacity = 0;

            for (IFluidHandler dest : destinations) {
                if (source == dest) continue;
                int accepted = dest.fill(new FluidStack(fluid, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    validDestinations.put(dest, accepted);
                    totalCapacity += accepted;
                }
            }

            if (validDestinations.isEmpty()) continue;

            while (remaining > 0 && !validDestinations.isEmpty() && totalCapacity > 0) {
                int initialRemaining = remaining;
                long currentTotalCapacity = totalCapacity;
                
                Iterator<Map.Entry<IFluidHandler, Integer>> it = validDestinations.entrySet().iterator();
                while (it.hasNext() && remaining > 0) {
                    Map.Entry<IFluidHandler, Integer> entry = it.next();
                    IFluidHandler dest = entry.getKey();
                    int maxAcceptable = entry.getValue();

                    long share = (long) ((double) initialRemaining * ((double) maxAcceptable / currentTotalCapacity));
                    if (share <= 0) share = 1; 
                    
                    int toFill = (int) Math.min(share, maxAcceptable);
                    toFill = Math.min(toFill, remaining);
                    
                    if (toFill > 0) {
                        FluidStack drained = source.drain(new FluidStack(fluid, toFill), IFluidHandler.FluidAction.EXECUTE);
                        if (!drained.isEmpty()) {
                            dest.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                            remaining -= drained.getAmount();
                            
                            if (!hasCheckedMeltdown) {
                                if (checkMeltdown(level, fluid)) return true;
                                hasCheckedMeltdown = true;
                                for (FluidNode node : nodes) {
                                    BlockPos nodePos = node.getPos();
                                    if (level.isLoaded(nodePos)) {
                                        BlockEntity be = level.getBlockEntity(nodePos);
                                        if (be instanceof FluidPipeBlockEntity pipeBE) pipeBE.setHasFlowed(true);
                                    }
                                }
                            }
                        }
                    }
                    
                    int newAccepted = dest.fill(new FluidStack(fluid, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
                    totalCapacity -= maxAcceptable;
                    if (newAccepted > 0) {
                        entry.setValue(newAccepted);
                        totalCapacity += newAccepted;
                    } else {
                        it.remove();
                    }
                }
            }
        }
        return false;
    }

    private void balance(ServerLevel level, List<IFluidHandler> buffers, Set<net.minecraft.world.level.material.Fluid> allowedFluids) {
        if (buffers.size() < 2) return;

        // Группируем баки по типу жидкости
        Map<net.minecraft.world.level.material.Fluid, List<IFluidHandler>> byFluid = new HashMap<>();
        
        for (IFluidHandler buf : buffers) {
            FluidStack fs = buf.getFluidInTank(0);
            if (!fs.isEmpty()) {
                if (!allowedFluids.isEmpty() && !allowedFluids.contains(fs.getFluid())) continue;
                byFluid.computeIfAbsent(fs.getFluid(), k -> new ArrayList<>()).add(buf);
            }
        }
        
        // Для каждой жидкости балансируем свои баки
        for (Map.Entry<net.minecraft.world.level.material.Fluid, List<IFluidHandler>> entry : byFluid.entrySet()) {
            net.minecraft.world.level.material.Fluid type = entry.getKey();
            List<IFluidHandler> activeBuffers = entry.getValue();
            
            // Также добавляем пустые баки, которые могут принять эту жидкость
            for (IFluidHandler buf : buffers) {
                if (buf.getFluidInTank(0).isEmpty() && buf.isFluidValid(0, new FluidStack(type, 1))) {
                    activeBuffers.add(buf);
                }
            }
            
            if (activeBuffers.size() < 2) continue;
            
            long totalFluid = 0;
            long totalCapacity = 0;
            
            for (IFluidHandler buf : activeBuffers) {
                totalFluid += buf.getFluidInTank(0).getAmount();
                totalCapacity += buf.getTankCapacity(0);
            }
            
            if (totalFluid == 0 || totalCapacity == 0) continue;
            
            final double avgRatio = (double) totalFluid / totalCapacity;
            final int DEADBAND = 5;

            List<IFluidHandler> donors    = new ArrayList<>();
            List<IFluidHandler> receivers = new ArrayList<>();

            for (IFluidHandler buf : activeBuffers) {
                int current = buf.getFluidInTank(0).getAmount();
                int target  = (int) (buf.getTankCapacity(0) * avgRatio);

                if (current > target + DEADBAND) donors.add(buf);
                else if (current < target - DEADBAND) receivers.add(buf);
            }

            for (IFluidHandler donor : donors) {
                FluidStack donorFluid = donor.getFluidInTank(0);
                if (donorFluid.isEmpty()) continue;

                int donorCap     = donor.getTankCapacity(0);
                int donorTarget  = (int) (donorCap * avgRatio);
                int donorExcess  = donorFluid.getAmount() - donorTarget;
                if (donorExcess <= 0) continue;

                for (IFluidHandler receiver : receivers) {
                    if (donorExcess <= 0) break;

                    FluidStack recFluid = receiver.getFluidInTank(0);
                    int recCap    = receiver.getTankCapacity(0);
                    int recCurrent = recFluid.isEmpty() ? 0 : recFluid.getAmount();
                    int recTarget  = (int) (recCap * avgRatio);
                    int recDeficit = recTarget - recCurrent;
                    if (recDeficit <= 0) continue;

                    int toMove = Math.min(donorExcess, recDeficit);
                    int acceptedSim = receiver.fill(new FluidStack(type, toMove), IFluidHandler.FluidAction.SIMULATE);
                    if (acceptedSim > 0) {
                        FluidStack drained = donor.drain(new FluidStack(type, acceptedSim), IFluidHandler.FluidAction.EXECUTE);
                        if (!drained.isEmpty()) {
                            receiver.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                            donorExcess -= drained.getAmount();
                            if (!hasCheckedMeltdown) {
                                if (checkMeltdown(level, drained.getFluid())) return;
                                hasCheckedMeltdown = true;
                            }
                        }
                    }
                }
            }
        }
    }

    // Balancing removed from here as it's fully replaced above.
    private boolean checkMeltdown(ServerLevel level, net.minecraft.world.level.material.Fluid fluid) {
        int tempC = getFluidTemperatureCelsius(fluid);
        int corr = 0;
        if (fluid.getFluidType() instanceof BaseFluidType base) corr = base.getCorrosivity();
        else if (fluid == net.minecraft.world.level.material.Fluids.LAVA || fluid == net.minecraft.world.level.material.Fluids.FLOWING_LAVA) tempC = 1000;

        List<BlockPos> toMelt = new ArrayList<>();
        for (FluidNode node : new ArrayList<>(nodes)) {
            BlockPos pos = node.getPos();
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FluidPipeBlock pipeBlock) {
                PipeTier tier = pipeBlock.getTier();
                if (tempC > tier.getMaxTemperature() || corr > tier.getMaxCorrosivity()) toMelt.add(pos);
            }
        }

        if (!toMelt.isEmpty()) {
            for (BlockPos pos : toMelt) {
                level.destroyBlock(pos, false);
                BlockState fluidBlock = fluid.defaultFluidState().createLegacyBlock();
                if (!fluidBlock.isAir()) level.setBlock(pos, fluidBlock, 3);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }

    private static int getFluidTemperatureCelsius(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == net.minecraft.world.level.material.Fluids.WATER || fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER) return 20;
        if (fluid == net.minecraft.world.level.material.Fluids.LAVA || fluid == net.minecraft.world.level.material.Fluids.FLOWING_LAVA) return 1000;
        if (fluid.getFluidType() instanceof BaseFluidType base) return base.getDisplayTemperature();
        return fluid.getFluidType().getTemperature() - 273;
    }

    public void addNode(FluidNode node) {
        node.setNetwork(this);
        nodes.add(node);
        this.hasCheckedMeltdown = false;
    }

    public void removeNode(FluidNode node) {
        nodes.remove(node);
        node.setNetwork(null);
        this.hasCheckedMeltdown = false;
        if (!nodes.isEmpty()) rebuildNetwork();
        else manager.removeNetwork(this);
    }

    public boolean isEmpty() { return nodes.isEmpty(); }

    private void rebuildNetwork() {
        if (nodes.isEmpty()) return;

        // Строим pos→node карту для O(1) поиска соседей (вместо O(N) перебора всех узлов)
        Map<BlockPos, FluidNode> posMap = new HashMap<>(nodes.size() * 2);
        for (FluidNode n : nodes) posMap.put(n.getPos(), n);

        Set<FluidNode> allReachableNodes = new HashSet<>();
        Queue<FluidNode> queue = new LinkedList<>();
        FluidNode startNode = nodes.iterator().next();
        queue.add(startNode);
        allReachableNodes.add(startNode);

        while (!queue.isEmpty()) {
            FluidNode current = queue.poll();
            BlockPos pos = current.getPos();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                FluidNode potentialNeighbor = posMap.get(neighborPos); // O(1) вместо O(N) цикла
                if (potentialNeighbor != null && !allReachableNodes.contains(potentialNeighbor)) {
                    allReachableNodes.add(potentialNeighbor);
                    queue.add(potentialNeighbor);
                }
            }
        }
        if (allReachableNodes.size() < nodes.size()) {
            Set<FluidNode> lostNodes = new HashSet<>(nodes);
            lostNodes.removeAll(allReachableNodes);
            nodes.removeAll(lostNodes);
            for (FluidNode lostNode : lostNodes) {
                lostNode.setNetwork(null);
                manager.reAddNode(lostNode.getPos(), this);
            }
            if (nodes.size() < 2) {
                for (FluidNode remainingNode : nodes) {
                    remainingNode.setNetwork(null);
                    manager.reAddNode(remainingNode.getPos(), this);
                }
                nodes.clear();
                manager.removeNetwork(this);
            }
        }
    }

    public void merge(FluidNetwork other) {
        if (this == other) return;
        if (other.nodes.size() > this.nodes.size()) { other.merge(this); return; }
        for (FluidNode node : other.nodes) { node.setNetwork(this); this.nodes.add(node); }
        other.nodes.clear();
        manager.removeNetwork(other);
        this.hasCheckedMeltdown = false;
    }

    public int getNodeCount() { return nodes.size(); }
    public Set<FluidNode> getNodes() { return nodes; }
}