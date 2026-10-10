package com.ruleta;

import com.ruleta.net.OpenMenuPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class RuletaItem extends Item {
    public RuletaItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        // Solo los jugadores en creativo pueden abrir el menú.
        if (!user.isCreative()) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            ServerPlayNetworking.send(player, OpenMenuPayload.INSTANCE);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
