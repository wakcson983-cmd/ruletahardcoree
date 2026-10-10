package com.ruleta.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor: lo que se eligió en el menú (color, título, texto). */
public record SelectPayload(int color, String title, String text) implements CustomPayload {
    public static final CustomPayload.Id<SelectPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ruleta", "select"));
    public static final PacketCodec<RegistryByteBuf, SelectPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SelectPayload::color,
            PacketCodecs.STRING, SelectPayload::title,
            PacketCodecs.STRING, SelectPayload::text,
            SelectPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
