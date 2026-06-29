package com.verity.mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("verity")
public class VerityMod {

    public static final String MOD_ID = "verity";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public VerityMod() {
        // Register config first
        VerityConfig.register();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("[Verity] Mod loading... ✨ Powered by Google Gemini!");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("[Verity] Client setup complete. Verity is ready to help!");
    }
}
