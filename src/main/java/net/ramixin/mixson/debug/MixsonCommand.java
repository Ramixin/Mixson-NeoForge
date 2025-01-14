package net.ramixin.mixson.debug;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.ramixin.mixson.Mixson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "mixson", bus = EventBusSubscriber.Bus.GAME)
public class MixsonCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson Command");

    @SubscribeEvent()
    public static void onInitialize(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mixson").executes(
                context -> {
                    for(String line : buildOutput()) LOGGER.info(line);
                    context.getSource().sendSuccess(() -> Component.literal("Dumped Mixson event calls to console"), true);
                    return 1;
                }
        ).then(Commands.literal("clear").executes(context -> {
            Mixson.clearCalls();
            context.getSource().sendSuccess(() -> Component.literal("cleared Mixson event calls"), true);
            return 1;
        })));
    }

    private static String fillToLength(String string, int length) {
        int repeatFor = length - string.length();
        if(repeatFor <= 0) return string;
        return string + " ".repeat(repeatFor);
    }

    private static List<String> buildOutput() {
        List<String> list = new ArrayList<>();
        final List<ResourceLocation> eventIds = Mixson.callCountsSet();
        if(eventIds.isEmpty()) {
            String spacer = "-".repeat(30);
            list.add(spacer);
            list.add("no events have been called yet");
            list.add(spacer);
            return list;
        }
        int eventNameSpacing = 9;
        for(ResourceLocation eventId : eventIds) if(eventId.toString().length() > eventNameSpacing) eventNameSpacing = eventId.toString().length();
        eventNameSpacing++;
        list.add(fillToLength("event id ", eventNameSpacing)+"| Calls | File Operations");
        String spacer = "-".repeat(25 + eventNameSpacing);
        list.add(spacer);
        for(ResourceLocation eventId : eventIds) {
            CallCountEntry entry = Mixson.getCallCount(eventId);
            list.add(fillToLength(eventId.toString(), eventNameSpacing) + "| "+fillToLength(String.valueOf(entry.eventCalls()), 6)+"| "+entry.fileOperations());
        }
        list.add(spacer);
        return list;
    }
}
