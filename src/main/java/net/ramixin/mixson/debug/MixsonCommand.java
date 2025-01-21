package net.ramixin.mixson.debug;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.ramixin.mixson.inline.Mixson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class MixsonCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson Debug");

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
        final List<UUID> order = Mixson.getCallCountsOrder();
        LinkedHashMap<String, CallCountEntry> map = new LinkedHashMap<>();
        if(order.isEmpty()) {
            String spacer = "-".repeat(30);
            list.add(spacer);
            list.add("no events have been called yet");
            list.add(spacer);
            return list;
        }
        for(UUID uuid : order) map.put(Mixson.getEventName(uuid), Mixson.getCallCount(uuid));
        int eventNameSpacing = 9;
        for(String eventName : map.keySet()) if(eventName.length() > eventNameSpacing) eventNameSpacing = eventName.length();
        eventNameSpacing++;
        list.add(fillToLength("event id ", eventNameSpacing)+"| Calls | File Operations");
        String spacer = "-".repeat(25 + eventNameSpacing);
        list.add(spacer);
        for(Map.Entry<String, CallCountEntry> sequencedEntry : map.sequencedEntrySet()) {
            CallCountEntry callCountEntry = sequencedEntry.getValue();
            list.add(fillToLength(sequencedEntry.getKey(), eventNameSpacing) + "| "+fillToLength(String.valueOf(callCountEntry.eventCalls()), 6)+"| "+callCountEntry.fileOperations());
        }
        list.add(spacer);
        return list;
    }
}
