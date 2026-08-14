package com.example.vm.client;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.registry.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ComputerInMinecraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlockEntities.COMPUTER_MONITOR_BLOCK_ENTITY.get(), ComputerMonitorRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.COMPUTER_BLOCK_ENTITY.get(), ComputerScreenRenderer::new);
        });
    }
}
