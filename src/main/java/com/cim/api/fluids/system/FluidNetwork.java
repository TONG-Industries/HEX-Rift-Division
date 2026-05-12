package com.cim.api.fluids.system;

import com.cim.block.basic.industrial.fluids.FluidPipeBlock;
import com.cim.block.entity.industrial.fluids.FluidBarrelBlockEntity;
import com.cim.block.entity.industrial.fluids.FluidPipeBlockEntity;
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

    public FluidNetwork(FluidNetworkManager manager) {
        this.manager = manager;
    }

    public void tick(ServerLevel level) {
        if (nodes.isEmpty()) return;
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);
        if (nodes.size() < 2) return;

        List<IFluidHandler> pureProviders = new ArrayList<>();
        List<IFluidHandler> pureReceivers = new ArrayList<>();
        List<IFluidHandler> buffers = new ArrayList<>();

        for (FluidNode node : nodes) {
            BlockPos pos = node.getPos();
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || be instanceof FluidPipeBlockEntity) continue;
            be.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
                // ФИКС: проверяем любой блок с режимом, а не только обычную бочку
                if (be instanceof ITankWithMode tank) {
                    int m = tank.getMode();
                    if (m == 1) pureReceivers.add(handler);
                    else if (m == 2) pureProviders.add(handler);
                    else buffers.add(handler); // mode 0 = балансировка
                } else {
                    pureProviders.add(handler);
                    pureReceivers.add(handler);
                }
            });
        }

        if (transfer(level, pureProviders, pureReceivers)) return;
        if (transfer(level, pureProviders, buffers)) return;
        if (transfer(level, buffers, pureReceivers)) return;
        balance(level, buffers);
    }

    private boolean transfer(ServerLevel level, List<IFluidHandler> sources, List<IFluidHandler> destinations) {
        for (IFluidHandler source : sources) {
            FluidStack available = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty() || available.getAmount() <= 0) continue;
            int remaining = available.getAmount();
            for (IFluidHandler dest : destinations) {
                if (remaining <= 0) break;
                if (source == dest) continue;
                int accepted = dest.fill(new FluidStack(available.getFluid(), remaining), IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    FluidStack drained = source.drain(new FluidStack(available.getFluid(), accepted), IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        dest.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        remaining -= drained.getAmount();
                        if (!hasCheckedMeltdown) {
                            if (checkMeltdown(level, drained.getFluid())) return true;
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
            }
        }
        return false;
    }

    private void balance(ServerLevel level, List<IFluidHandler> buffers) {
        if (buffers.size() < 2) return;
        long totalFluid = 0;
        int validBuffers = 0;
        net.minecraft.world.level.material.Fluid type = null;
        for (IFluidHandler buf : buffers) {
            FluidStack fs = buf.getFluidInTank(0);
            if (!fs.isEmpty()) {
                totalFluid += fs.getAmount();
                if (type == null) type = fs.getFluid();
            }
        }
        for (IFluidHandler buf : buffers) {
            FluidStack fs = buf.getFluidInTank(0);
            if (fs.isEmpty() || fs.getFluid() == type) validBuffers++;
        }
        if (totalFluid == 0 || type == null || validBuffers < 2) return;

        int avg = (int) (totalFluid / validBuffers);
        List<IFluidHandler> donors = new ArrayList<>();
        List<IFluidHandler> receivers = new ArrayList<>();
        for (IFluidHandler buf : buffers) {
            FluidStack fs = buf.getFluidInTank(0);
            if (fs.isEmpty() || fs.getFluid() == type) {
                int amt = fs.isEmpty() ? 0 : fs.getAmount();
                if (amt > avg + 5) donors.add(buf);
                else if (amt < avg - 5) receivers.add(buf);
            }
        }

        for (IFluidHandler donor : donors) {
            int donorExcess = donor.getFluidInTank(0).getAmount() - avg;
            if (donorExcess <= 0) continue;
            for (IFluidHandler receiver : receivers) {
                int receiverAmt = receiver.getFluidInTank(0).isEmpty() ? 0 : receiver.getFluidInTank(0).getAmount();
                int receiverDeficit = avg - receiverAmt;
                if (receiverDeficit <= 0) continue;
                int acceptedSim = receiver.fill(new FluidStack(type, Math.min(donorExcess, receiverDeficit)), IFluidHandler.FluidAction.SIMULATE);
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
                if (donorExcess <= 0) break;
            }
        }
    }

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