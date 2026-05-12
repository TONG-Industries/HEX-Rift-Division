package com.trd.event;

import com.trd.api.energy.EnergyNetworkManager;
import com.trd.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnergyPlacementEvents {

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        BlockPos placedPos = event.getPos();
        
        EnergyNetworkManager manager = EnergyNetworkManager.get(serverLevel);
        if (manager.isBlockObstructingAnyWire(placedPos)) {
            event.setCanceled(true);
            
            if (event.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.literal("§cЭто место занято электрическим проводом!"), true);
                
                if (!player.isCreative()) {
                    net.minecraft.world.level.block.Block block = event.getPlacedBlock().getBlock();
                    net.minecraft.world.InteractionHand hand = player.getMainHandItem().is(block.asItem()) ? net.minecraft.world.InteractionHand.MAIN_HAND : 
                                                         (player.getOffhandItem().is(block.asItem()) ? net.minecraft.world.InteractionHand.OFF_HAND : null);
                    
                    if (hand != null) {
                        player.getItemInHand(hand).shrink(1);
                        net.minecraft.world.level.block.Block.popResource(serverLevel, placedPos, new net.minecraft.world.item.ItemStack(block.asItem()));
                    }
                }
            }
        }
    }
}
