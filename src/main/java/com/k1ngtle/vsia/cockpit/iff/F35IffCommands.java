package com.k1ngtle.vsia.cockpit.iff;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class F35IffCommands {
    private F35IffCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("f35iff")
            .then(Commands.literal("create").then(Commands.argument("name",StringArgumentType.word()).executes(c->run(c,70,StringArgumentType.getString(c,"name")))))
            .then(Commands.literal("invite").then(Commands.argument("player",StringArgumentType.word()).executes(c->run(c,71,StringArgumentType.getString(c,"player")))))
            .then(Commands.literal("load").then(Commands.argument("mission",StringArgumentType.word()).then(Commands.argument("slot",StringArgumentType.word()).executes(c->run(c,72,StringArgumentType.getString(c,"mission")+":"+StringArgumentType.getString(c,"slot"))))))
            .then(Commands.literal("rotate").executes(c->run(c,73,"")))
            .then(Commands.literal("revoke").then(Commands.argument("player",StringArgumentType.word()).executes(c->run(c,74,StringArgumentType.getString(c,"player")))))
            .then(Commands.literal("list").executes(c->run(c,75,"")))
            .then(Commands.literal("retire").executes(c->run(c,76,""))));
    }
    private static int run(CommandContext<CommandSourceStack> context,int action,String value)throws CommandSyntaxException{
        ServerPlayer player=context.getSource().getPlayerOrException();F35CockpitSeatBlockEntity cockpit=F35SeatController.cockpitFor(player);
        if(cockpit==null){context.getSource().sendFailure(Component.literal("Sit in the cockpit you want to manage first."));return 0;}
        F35IffKeyActions.perform(player,cockpit,action,value);return 1;
    }
}
