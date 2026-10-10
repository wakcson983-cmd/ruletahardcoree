package com.ruleta;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ruleta.net.OpenMenuPayload;
import com.ruleta.net.RedAnimPayload;
import com.ruleta.net.SelectPayload;
import com.ruleta.net.SpinPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class RuletaMod implements ModInitializer {
    public static final String MOD_ID = "ruleta";

    /**
     * Con "minecraft" el item se da con: /give @a ruleta
     * (si algún día da problemas, cambia a Identifier.of(MOD_ID, "ruleta") y mueve
     * assets/minecraft/models/item/ruleta.json a assets/ruleta/models/item/ruleta.json)
     */
    public static final Identifier ITEM_ID = Identifier.ofVanilla("ruleta");

    public static Item RULETA_ITEM;
    public static SoundEvent RISA;
    public static SoundEvent CANCION;

    /** Ticks que tarda la ruleta en frenar en el cliente (4 s). */
    private static final int TICKS_ATERRIZAJE = 80;
    /** Ticks hasta que arranca la animación roja (cuando la ruleta ya desapareció). */
    private static final int TICKS_ANIM_ROJA = 120;

    private static final String BARRA = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private static final List<Task> TASKS = new ArrayList<>();

    private static final class Task {
        int ticks;
        final Runnable run;

        Task(int ticks, Runnable run) {
            this.ticks = ticks;
            this.run = run;
        }
    }

    @Override
    public void onInitialize() {
        RULETA_ITEM = Registry.register(Registries.ITEM, ITEM_ID, new RuletaItem(new Item.Settings().maxCount(1)));
        RISA = registrarSonido("risa");
        CANCION = registrarSonido("cancion");

        // Solo en el inventario creativo, sin receta de crafteo.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(RULETA_ITEM));

        PayloadTypeRegistry.playS2C().register(OpenMenuPayload.ID, OpenMenuPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpinPayload.ID, SpinPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RedAnimPayload.ID, RedAnimPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectPayload.ID, SelectPayload.CODEC);

        // Menú: solo jugadores en creativo.
        ServerPlayNetworking.registerGlobalReceiver(SelectPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!player.isCreative()) return;
            RuletaColor color = RuletaColor.byIndex(payload.color());
            if (color == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;
            girar(server, color, recortar(payload.title(), 48), recortar(payload.text(), 200));
        });

        // Comandos /ruleta <color> [texto]: cualquier jugador con operador.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root =
                    CommandManager.literal("ruleta").requires(src -> src.hasPermissionLevel(2));
            for (RuletaColor c : RuletaColor.values()) {
                root.then(CommandManager.literal(c.id)
                        .executes(ctx -> {
                            girar(ctx.getSource().getServer(), c, "", "");
                            return 1;
                        })
                        .then(CommandManager.argument("texto", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    girar(ctx.getSource().getServer(), c, "",
                                            recortar(StringArgumentType.getString(ctx, "texto"), 200));
                                    return 1;
                                })));
            }
            dispatcher.register(root);
        });

        // Muerte de un jugador: la animación sale a todos (sin filtro rojo ni música).
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity dead)) return;
            MinecraftServer server = dead.getServer();
            if (server == null) return;
            Text causa = source.getDeathMessage(dead);
            RedAnimPayload payload = new RedAnimPayload(true, dead.getName().getString(), causa);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, payload);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (TASKS.isEmpty()) return;
            List<Task> snapshot = new ArrayList<>(TASKS);
            for (Task t : snapshot) {
                t.ticks--;
                if (t.ticks <= 0) {
                    TASKS.remove(t);
                    t.run.run();
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TASKS.clear());
    }

    private static SoundEvent registrarSonido(String name) {
        Identifier id = Identifier.of(MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static String recortar(String s, int max) {
        if (s == null) return "";
        s = s.strip();
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Gira la ruleta para todos los jugadores. No dice quién la giró. */
    public static void girar(MinecraftServer server, RuletaColor color, String titulo, String texto) {
        SpinPayload spin = new SpinPayload(color.ordinal());
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, spin);
        }

        TASKS.add(new Task(TICKS_ATERRIZAJE, () -> enviarChat(server, color, titulo, texto)));

        if (color == RuletaColor.ROJO) {
            TASKS.add(new Task(TICKS_ANIM_ROJA, () -> {
                RedAnimPayload anim = new RedAnimPayload(false, "", Text.empty());
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(p, anim);
                }
            }));
        }
    }

    /** Mensaje en el chat: líneas rojas arriba y abajo, título en mayúsculas y negritas, texto en el color elegido. */
    private static void enviarChat(MinecraftServer server, RuletaColor color, String titulo, String texto) {
        if (titulo.isBlank() && texto.isBlank()) return;

        Text barra = Text.literal(BARRA).setStyle(Style.EMPTY.withColor(0xFF3333));
        MutableText tituloTxt = null;
        if (!titulo.isBlank()) {
            tituloTxt = Text.literal(titulo.toUpperCase(Locale.ROOT))
                    .setStyle(Style.EMPTY.withColor(color.rgb).withBold(true));
        }
        MutableText cuerpoTxt = null;
        if (!texto.isBlank()) {
            cuerpoTxt = Text.literal(texto).setStyle(Style.EMPTY.withColor(color.rgb));
        }

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(barra, false);
            if (tituloTxt != null) p.sendMessage(tituloTxt, false);
            if (cuerpoTxt != null) p.sendMessage(cuerpoTxt, false);
            p.sendMessage(barra, false);
            p.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.8f, 1.0f);
        }
    }
}
