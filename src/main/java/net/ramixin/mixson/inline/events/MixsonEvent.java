package net.ramixin.mixson.inline.events;

import net.ramixin.mixson.inline.EventContext;

@FunctionalInterface
public interface MixsonEvent {

    void runEvent(EventContext context);

    default int ordinal() {
        return -1;
    }

}
