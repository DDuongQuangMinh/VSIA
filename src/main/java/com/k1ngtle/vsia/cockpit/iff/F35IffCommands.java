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
            .then(Commands.literal("status").executes(F35IffCommands::status))
            .then(Commands.literal("create").then(Commands.argument("name",StringArgumentType.word()).executes(c->run(c,70,StringArgumentType.getString(c,"name")))))
            .then(Commands.literal("invite").then(Commands.argument("player",StringArgumentType.word()).executes(c->run(c,71,StringArgumentType.getString(c,"player")))))
            .then(Commands.literal("load").then(Commands.argument("mission",StringArgumentType.word()).then(Commands.argument("slot",StringArgumentType.word()).executes(c->run(c,72,StringArgumentType.getString(c,"mission")+":"+StringArgumentType.getString(c,"slot"))))))
            .then(Commands.literal("rotate").executes(c->run(c,73,"")))
            .then(Commands.literal("revoke").then(Commands.argument("player",StringArgumentType.word()).executes(c->run(c,74,StringArgumentType.getString(c,"player")))))
            .then(Commands.literal("list").executes(c->run(c,75,"")))
            .then(Commands.literal("retire").executes(c->run(c,76,""))));
    }
    private static int status(CommandContext<CommandSourceStack> context)throws CommandSyntaxException{
        ServerPlayer player=context.getSource().getPlayerOrException();F35CockpitSeatBlockEntity cockpit=F35SeatController.cockpitFor(player);
        if(cockpit==null){context.getSource().sendFailure(Component.literal("Sit in the cockpit you want to inspect first."));return 0;}
        long now=java.time.Instant.now().getEpochSecond();F35IffConfig own=cockpit.iff();
        String header="IFF v2.7.11 cockpit "+cockpit.cockpitId().toString().substring(0,8)+" MASTER "+own.master()+" | "+F35IffService.localStatus(own,F35IffMissions.get(player.server),now);
        context.getSource().sendSuccess(()->Component.literal(header),false);
        if(!com.k1ngtle.vsia.cockpit.F35VsShipHelper.shipSnapshot(cockpit).detected()){
            context.getSource().sendSuccess(()->Component.literal("TIMER OFF: own cockpit is not on a detected VS ship."),false);return 1;
        }
        var observations=com.k1ngtle.vsia.cockpit.network.F35RadarSyncEvents.iffObservations(cockpit.cockpitId());
        long tick=player.serverLevel().getGameTime();
        if(observations.isEmpty())context.getSource().sendSuccess(()->Component.literal("No active ship windows. Check master/modes and sensor range; allow a sensor update after sitting."),false);
        for(var item:observations.stream().limit(12).toList()){
            String line="SHIP "+item.ship().toString().substring(0,8)+" "+item.affiliation()+" | "+item.reply()+" | "+String.format(java.util.Locale.ROOT,"%.2f",item.failedTicks()/20.0)+" / 10.00 game seconds | snapshot age "+Math.max(0,tick-item.lastTick())+" ticks";
            context.getSource().sendSuccess(()->Component.literal(line),false);
        }
        context.getSource().sendSuccess(()->Component.literal("200 server ticks = 10s at 20 TPS. CODE_REPLY stays UNKNOWN; only authenticated secure replies prove FRIEND. Status does not advance timers."),false);
        return 1;
    }
    private static int run(CommandContext<CommandSourceStack> context,int action,String value)throws CommandSyntaxException{
        ServerPlayer player=context.getSource().getPlayerOrException();F35CockpitSeatBlockEntity cockpit=F35SeatController.cockpitFor(player);
        if(cockpit==null){context.getSource().sendFailure(Component.literal("Sit in the cockpit you want to manage first."));return 0;}
        F35IffKeyActions.perform(player,cockpit,action,value);return 1;
    }
}
