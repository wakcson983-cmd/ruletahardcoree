package com.ruleta.client;

import com.ruleta.net.OpenMenuPayload;
import com.ruleta.net.RedAnimPayload;
import com.ruleta.net.SpinPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class RuletaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenMenuPayload.ID,
                (payload, context) -> context.client().setScreen(new RuletaScreen()));

        ClientPlayNetworking.registerGlobalReceiver(SpinPayload.ID,
                (payload, context) -> Anim.startWheel(payload.color()));

        ClientPlayNetworking.registerGlobalReceiver(RedAnimPayload.ID,
                (payload, context) -> Anim.startRed(payload.death(), payload.name(), payload.cause()));

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> Anim.render(drawContext));
    }
}
