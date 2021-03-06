package me.noeffort.nuggetmod.common.tileentity;

import me.noeffort.nuggetmod.core.init.TileEntityTypeInit;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WeatherPedestalTileEntity extends TileEntity implements ITickableTileEntity {

    private Weather weather = Weather.CLEAR;

    public WeatherPedestalTileEntity() {
        this(TileEntityTypeInit.WEATHER_PEDESTAL_TILE_ENTITY.get());
    }

    public WeatherPedestalTileEntity(TileEntityType<?> type) {
        super(type);
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putString("Weather", this.weather.name());
        return nbt;
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
        super.load(state, nbt);
        this.weather = Weather.valueOf(nbt.getString("Weather"));
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.getBlockPos(), -1, this.getUpdateTag());
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        return this.save(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        this.load(state, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        this.handleUpdateTag(this.getBlockState(), pkt.getTag());
    }

    @Override
    public void tick() {
        if(this.level == null) throw new IllegalStateException("Block cannot be in a null world!");
        switch(this.weather) {
            case RAIN:
                this.level.setRainLevel(1.0F);
                break;
            case THUNDER:
                this.level.setRainLevel(1.0F);
                this.level.setThunderLevel(1.0F);
                break;
            case CLEAR:
            default:
                this.level.setRainLevel(0.0F);
                this.level.setThunderLevel(0.0F);
                break;
        }
    }

    public enum Weather {

        CLEAR,
        RAIN,
        THUNDER

    }

}
