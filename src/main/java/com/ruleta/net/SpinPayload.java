package com.ruleta.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> todos los clientes: mostrar la ruleta girando hasta el color indicado (sin decir quién la giró). */
public record SpinPayload(int color) implements CustomPayload {
    public static final CustomPayload.Id<SpinPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ruleta", "spin"));
    public static final PacketCodec<RegistryByteBuf, SpinPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, SpinPayload::color, SpinPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
