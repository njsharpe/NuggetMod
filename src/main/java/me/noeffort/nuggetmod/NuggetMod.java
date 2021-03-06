package me.noeffort.nuggetmod;

import com.refinedmods.refinedstorage.api.IRSAPI;
import com.refinedmods.refinedstorage.api.RSAPIInject;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.Network;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import com.refinedmods.refinedstorage.render.BakedModelOverrideRegistry;
import com.refinedmods.refinedstorage.render.model.FullbrightBakedModel;
import com.refinedmods.refinedstorage.tile.BaseTile;
import com.refinedmods.refinedstorage.tile.data.TileDataManager;
import me.noeffort.nuggetmod.client.screen.TimePedestalScreen;
import me.noeffort.nuggetmod.client.screen.TravelBagScreen;
import me.noeffort.nuggetmod.client.screen.WeatherPedestalScreen;
import me.noeffort.nuggetmod.common.block.refinedstorage.CreativeWirelessTransmitterBlock;
import me.noeffort.nuggetmod.common.capability.travelbag.TravelBagCapability;
import me.noeffort.nuggetmod.common.capability.world.HasBlock;
import me.noeffort.nuggetmod.common.capability.world.HasBlockCapability;
import me.noeffort.nuggetmod.common.container.TravelBagContainer;
import me.noeffort.nuggetmod.common.item.TravelBagItem;
import me.noeffort.nuggetmod.common.refinedstorage.CreativeWirelessTransmitterNetworkNode;
import me.noeffort.nuggetmod.common.tileentity.refinedstorage.CreativeWirelessTransmitterTileEntity;
import me.noeffort.nuggetmod.core.init.*;
import me.noeffort.nuggetmod.core.itemgroup.NuggetItemGroup;
import me.noeffort.nuggetmod.core.network.*;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.command.FunctionObject;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.loot.LootConditionType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeTileEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mod(NuggetMod.MOD_ID)
@Mod.EventBusSubscriber(modid = NuggetMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NuggetMod {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "nuggetmod";
    public static final String NETWORK_VERSION = "0.1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "network"),
            () -> NETWORK_VERSION,
            NETWORK_VERSION::equals,
            NETWORK_VERSION::equals
    );

    @RSAPIInject
    public static final IRSAPI REFINED_STORAGE = API.instance();

    public NuggetMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ItemInit.ITEMS.register(bus);
        BlockInit.BLOCKS.register(bus);
        ContainerTypeInit.CONTAINER_TYPES.register(bus);
        TileEntityTypeInit.TILE_ENTITY_TYPES.register(bus);
        FluidInit.FLUIDS.register(bus);
        EnchantmentInit.ENCHANTMENTS.register(bus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void enqueueIMC(final InterModEnqueueEvent event) {
        SlotTypePreset[] slots = { SlotTypePreset.BACK, SlotTypePreset.CHARM, SlotTypePreset.CHARM };
        List<SlotTypeMessage.Builder> builders = Arrays.stream(slots)
                .map(SlotTypePreset::getMessageBuilder)
                .collect(Collectors.toList());
        builders.forEach(builder -> {
            SlotTypeMessage message = builder.build();
            InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE,
                    () -> message);
        });
    }

    @SubscribeEvent
    public static void onRegisterItems(final RegistryEvent.Register<Item> event) {
        BlockInit.BLOCKS.getEntries().stream().map(RegistryObject::get).forEach(block ->
                event.getRegistry().register(new BlockItem(block,
                        new Item.Properties().tab(NuggetItemGroup.TAB_NUGGET_MOD))
                        .setRegistryName(block.getRegistryName())));
        ItemInit.VariantGroups.TRAVEL_BAG_VARIANTS.register(event.getRegistry());
        ItemInit.VariantGroups.TRAVEL_BAG_UPGRADE_VARIANTS.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        TravelBagCapability.register();
        HasBlockCapability.register();

        CHANNEL.registerMessage(0, TravelBagOpenMessage.class, TravelBagOpenMessage::encode, TravelBagOpenMessage::decode, TravelBagOpenMessage::handle);
        CHANNEL.registerMessage(1, WeatherPedestalUpdateMessage.class, WeatherPedestalUpdateMessage::encode, WeatherPedestalUpdateMessage::decode, WeatherPedestalUpdateMessage::handle);
        CHANNEL.registerMessage(2, TimePedestalUpdateMessage.class, TimePedestalUpdateMessage::encode, TimePedestalUpdateMessage::decode, TimePedestalUpdateMessage::handle);
        CHANNEL.registerMessage(3, PickBlockMessage.class, PickBlockMessage::encode, PickBlockMessage::decode, PickBlockMessage::handle);

        REFINED_STORAGE.getNetworkNodeRegistry().add(CreativeWirelessTransmitterNetworkNode.NODE_ID, (tag, world, pos) -> {
            NetworkNode node = new CreativeWirelessTransmitterNetworkNode(world, pos);
            node.read(tag);
            return node;
        });
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ScreenManager.register(ContainerTypeInit.WEATHER_PEDESTAL_CONTAINER_TYPE.get(), WeatherPedestalScreen::new);
        ScreenManager.register(ContainerTypeInit.TIME_PEDESTAL_CONTAINER_TYPE.get(), TimePedestalScreen::new);
        for(TravelBagItem.Type type : TravelBagItem.Type.values()) {
            ScreenManager.register(ContainerTypeInit.getTravelBagContainer(type), (TravelBagContainer c, PlayerInventory i, ITextComponent t) ->
                    new TravelBagScreen(c, i, t, type));
        }

        RenderTypeLookup.setRenderLayer(BlockInit.CREATIVE_WIRELESS_TRANSMITTER.get(), RenderType.cutout());
        // ClientRegistry.registerKeyBinding(KeybindInit.OPEN_TRAVEL_BAG);
    }

    public static ResourceLocation location(String location) {
        return new ResourceLocation(NuggetMod.MOD_ID, location);
    }

}
