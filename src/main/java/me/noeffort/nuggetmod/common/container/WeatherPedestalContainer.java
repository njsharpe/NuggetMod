package me.noeffort.nuggetmod.common.container;

import me.noeffort.nuggetmod.core.init.ContainerTypeInit;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class WeatherPedestalContainer extends Container {

    public static WeatherPedestalContainer from(int id, PlayerInventory inventory, PacketBuffer buf) {
        return new WeatherPedestalContainer(id, inventory);
    }

    public WeatherPedestalContainer(int id, PlayerInventory inventory) {
        super(ContainerTypeInit.WEATHER_PEDESTAL_CONTAINER_TYPE.get(), id);
        ContainerHelper.addPlayerInventory(this::addSlot, inventory, 8, 84);
    }

    @Override
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return true;
    }

}
