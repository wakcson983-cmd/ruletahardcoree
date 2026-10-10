package com.ruleta.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> cliente: abrir el menú de la ruleta. */
public record OpenMenuPayload() implements CustomPayload {
    public static final OpenMenuPayload INSTANCE = new OpenMenuPayload();
    public static final CustomPayload.Id<OpenMenuPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ruleta", "open_menu"));
    public static final PacketCodec<RegistryByteBuf, OpenMenuPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
