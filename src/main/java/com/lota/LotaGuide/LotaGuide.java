package com.lota.LotaGuide;

import com.lota.LotaGuide.item.ImageBookItem;
import com.lota.LotaGuide.item.SignedImageBookItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(LotaGuide.MODID)
public class LotaGuide {
    public static final String MODID = "lotaguide";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> IMAGE_BOOK = ITEMS.register("image_book", 
        () -> new ImageBookItem(new Item.Properties()));
    public static final RegistryObject<Item> SIGNED_IMAGE_BOOK = ITEMS.register("signed_image_book", 
        () -> new SignedImageBookItem(new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> LOTA_TAB = CREATIVE_MODE_TABS.register("lota_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.lotaguide.lota_tab"))
            .icon(() -> IMAGE_BOOK.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(IMAGE_BOOK.get());
                output.accept(SIGNED_IMAGE_BOOK.get());
            }).build());

    public LotaGuide(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        modEventBus.addListener(this::commonSetup);
        
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("LotaGuide mod initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("LotaGuide server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("LotaGuide client setup");
        }
    }
}
