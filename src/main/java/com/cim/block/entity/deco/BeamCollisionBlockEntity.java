package com.cim.block.entity.deco;

import com.cim.block.basic.ModBlocks;
import com.cim.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BeamCollisionBlockEntity extends BlockEntity {
    public static class BeamData {
        public Vec3 startPos;
        public Vec3 endPos;
        public int[] segmentsToRender;
        public boolean isMaster;
        public BlockPos masterPos;

        public BeamData(Vec3 startPos, Vec3 endPos, int[] segmentsToRender, boolean isMaster, BlockPos masterPos) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.segmentsToRender = segmentsToRender;
            this.isMaster = isMaster;
            this.masterPos = masterPos;
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("StartX", startPos.x);
            tag.putDouble("StartY", startPos.y);
            tag.putDouble("StartZ", startPos.z);
            tag.putDouble("EndX", endPos.x);
            tag.putDouble("EndY", endPos.y);
            tag.putDouble("EndZ", endPos.z);
            tag.putIntArray("Segments", segmentsToRender);
            tag.putBoolean("IsMaster", isMaster);
            if (!isMaster && masterPos != null) {
                tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
            }
            return tag;
        }

        public static BeamData deserialize(CompoundTag tag) {
            Vec3 start = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
            Vec3 end = new Vec3(tag.getDouble("EndX"), tag.getDouble("EndY"), tag.getDouble("EndZ"));
            int[] segments = tag.getIntArray("Segments");
            boolean isMaster = tag.getBoolean("IsMaster");
            BlockPos master = isMaster ? null : (tag.contains("MasterPos") ? NbtUtils.readBlockPos(tag.getCompound("MasterPos")) : null);
            return new BeamData(start, end, segments, isMaster, master);
        }
    }

    private final java.util.List<BeamData> beams = new java.util.concurrent.CopyOnWriteArrayList<>();
    public boolean isDestroyed = false;

    // Model Properties для DynamicBakedModel
    public static final net.minecraftforge.client.model.data.ModelProperty<java.util.List<BeamData>> BEAMS_LIST = new net.minecraftforge.client.model.data.ModelProperty<>();
    public static final net.minecraftforge.client.model.data.ModelProperty<BlockPos> MY_POS = new net.minecraftforge.client.model.data.ModelProperty<>();

    public BeamCollisionBlockEntity(BlockPos pPos, BlockState pBlockState) {
        // Обязательно замени на свой BEAM_COLLISION_BE из ModBlockEntities!
        super(ModBlockEntities.BEAM_COLLISION_BE.get(), pPos, pBlockState);
    }

    public void addMasterData(Vec3 start, Vec3 end, int[] segments) {
        if (!hasBeam(start, end)) {
            this.beams.add(new BeamData(start, end, segments, true, null));
            this.cachedShape = null;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    public void addSlaveData(BlockPos masterPos, Vec3 start, Vec3 end, int[] segments) {
        if (!hasBeam(start, end)) {
            this.beams.add(new BeamData(start, end, segments, false, masterPos));
            this.cachedShape = null;
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    private boolean hasBeam(Vec3 start, Vec3 end) {
        for (BeamData data : beams) {
            if (data.startPos.distanceToSqr(start) < 0.01 && data.endPos.distanceToSqr(end) < 0.01) {
                return true;
            }
        }
        return false;
    }

    public java.util.List<BeamData> getBeams() { return beams; }

    // --- СОХРАНЕНИЕ NBT ---

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
        for (BeamData data : beams) {
            listTag.add(data.serialize());
        }
        pTag.put("BeamsList", listTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.beams.clear();
        this.cachedShape = null;
        if (pTag.contains("BeamsList")) {
            net.minecraft.nbt.ListTag listTag = pTag.getList("BeamsList", 10); // 10 is CompoundTag type
            for (int i = 0; i < listTag.size(); i++) {
                this.beams.add(BeamData.deserialize(listTag.getCompound(i)));
            }
        } else if (pTag.contains("StartX")) {
            // Легаси-поддержка старых балок, если они остались в мире
            Vec3 start = new Vec3(pTag.getDouble("StartX"), pTag.getDouble("StartY"), pTag.getDouble("StartZ"));
            Vec3 end = new Vec3(pTag.getDouble("EndX"), pTag.getDouble("EndY"), pTag.getDouble("EndZ"));
            int[] segments = pTag.contains("Segments") ? pTag.getIntArray("Segments") : new int[0];
            boolean isM = pTag.getBoolean("IsMaster");
            BlockPos mPos = (!isM && pTag.contains("MasterPos")) ? NbtUtils.readBlockPos(pTag.getCompound("MasterPos")) : null;
            this.beams.add(new BeamData(start, end, segments, isM, mPos));
        }
    }

    // --- СИНХРОНИЗАЦИЯ КЛИЕНТА (ОЧЕНЬ ВАЖНО ДЛЯ РЕНДЕРА) ---

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (this.level != null && this.level.isClientSide) {
            this.requestModelDataUpdate();
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (this.level != null && this.level.isClientSide) {
            this.requestModelDataUpdate();
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // --- КОРОБКА РЕНДЕРА И КОЛЛИЗИИ ---

    private net.minecraft.world.phys.shapes.VoxelShape cachedShape = null;

    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape() {
        if (cachedShape == null) {
            cachedShape = computeShape();
        }
        return cachedShape;
    }

    private net.minecraft.world.phys.shapes.VoxelShape computeShape() {
        if (beams.isEmpty()) {
            return net.minecraft.world.level.block.Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
        }

        net.minecraft.world.phys.shapes.VoxelShape finalShape = net.minecraft.world.phys.shapes.Shapes.empty();
        BlockPos myPos = this.getBlockPos();
        AABB myBox = new AABB(myPos);
        double radius = 3.0 / 16.0;

        for (BeamData data : beams) {
            Vec3 direction = data.endPos.subtract(data.startPos).normalize();
            double distance = data.startPos.distanceTo(data.endPos);
            double step = 0.125;
            int steps = (int) (distance / step);

            for (int i = 0; i <= steps; i++) {
                Vec3 point = data.startPos.add(direction.scale(i * step));
                AABB pointBox = new AABB(point.x - radius, point.y - radius, point.z - radius,
                                         point.x + radius, point.y + radius, point.z + radius);

                if (pointBox.intersects(myBox)) {
                    AABB localBox = new AABB(
                        pointBox.minX - myPos.getX(), pointBox.minY - myPos.getY(), pointBox.minZ - myPos.getZ(),
                        pointBox.maxX - myPos.getX(), pointBox.maxY - myPos.getY(), pointBox.maxZ - myPos.getZ()
                    );
                    localBox = localBox.intersect(new AABB(0, 0, 0, 1, 1, 1));
                    finalShape = net.minecraft.world.phys.shapes.Shapes.or(finalShape, net.minecraft.world.phys.shapes.Shapes.create(localBox));
                }
            }
        }

        if (finalShape.isEmpty()) {
            return net.minecraft.world.level.block.Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
        }
        return finalShape.optimize();
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB box = super.getRenderBoundingBox();
        for (BeamData data : beams) {
            if (data.isMaster) {
                box = box.minmax(new AABB(data.startPos, data.endPos).inflate(1.0));
            }
        }
        return box;
    }

    public void removeBeamData(Vec3 start, Vec3 end) {
        beams.removeIf(data -> data.startPos.distanceToSqr(start) < 0.01 && data.endPos.distanceToSqr(end) < 0.01);
        cachedShape = null;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            if (beams.isEmpty()) {
                this.level.removeBlock(this.getBlockPos(), false);
            }
        }
    }

    public void breakEntireBeam(net.minecraft.world.level.Level level) {
        if (this.isDestroyed || beams.isEmpty()) return;
        this.isDestroyed = true;

        // Копируем список, так как removeBeamData будет модифицировать его
        java.util.List<BeamData> beamsCopy = new java.util.ArrayList<>(this.beams);

        for (BeamData data : beamsCopy) {
            double distance = data.startPos.distanceTo(data.endPos);
            int amountToDrop = (int) Math.ceil(distance);

            ItemStack dropStack = new ItemStack(ModBlocks.BEAM_BLOCK.get(), amountToDrop);
            Containers.dropItemStack(level, this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ(), dropStack);

            Vec3 direction = data.endPos.subtract(data.startPos).normalize();
            double stepSize = 0.5;
            int steps = (int) (distance / stepSize);

            for (int i = 1; i < steps; i++) {
                Vec3 stepVec = data.startPos.add(direction.scale(i * stepSize));
                BlockPos posOnLine = BlockPos.containing(stepVec);

                // Очищаем блоки вдоль линии (оригинальный путь)
                removeBeamDataFromPos(level, posOnLine, data.startPos, data.endPos);
                
                // Проверяем соседние блоки на случай смещенных служебных блоков
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    removeBeamDataFromPos(level, posOnLine.relative(dir), data.startPos, data.endPos);
                }
            }
        }
    }

    private void removeBeamDataFromPos(net.minecraft.world.level.Level level, BlockPos pos, Vec3 start, Vec3 end) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BeamCollisionBlockEntity slaveBE) {
            slaveBE.removeBeamData(start, end);
        }
    }

    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
        return net.minecraftforge.client.model.data.ModelData.builder()
            .with(BEAMS_LIST, new java.util.ArrayList<>(beams)) // Отдаем копию
            .with(MY_POS, this.getBlockPos())
            .build();
    }

    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
        return net.minecraftforge.client.model.data.ModelData.builder()
            .with(START_POS, startPos)
            .with(END_POS, endPos)
            .with(SEGMENTS, segmentsToRender)
            .with(MY_POS, this.getBlockPos())
            .build();
    }
}