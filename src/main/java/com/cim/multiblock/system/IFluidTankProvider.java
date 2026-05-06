package com.cim.multiblock.system;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

public interface IFluidTankProvider {
    LazyOptional<IFluidHandler> getFluidHandlerCapability();
}