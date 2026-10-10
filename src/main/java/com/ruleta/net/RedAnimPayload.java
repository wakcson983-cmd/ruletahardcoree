package com.ruleta.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

/**
 * Servidor -> todos los clientes: animación de la cara roja.
 * death=false: ruleta en rojo (con filtro rojo y sonidos).
 * death=true: murió un jugador (sin filtro y sin música, con nombre y causa debajo).
 */
public record RedAnimPayload(boolean death, String name, Text cause) implements CustomPayload {
    public static final CustomPayload.Id<RedAnimPayload> ID =
            new CustomPayload.Id<>(Identifier.of("ruleta", "red_anim"));
    public static final PacketCodec<RegistryByteBuf, RedAnimPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, RedAnimPayload::death,
            PacketCodecs.STRING, RedAnimPayload::name,
            TextCodecs.PACKET_CODEC, RedAnimPayload::cause,
            RedAnimPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
