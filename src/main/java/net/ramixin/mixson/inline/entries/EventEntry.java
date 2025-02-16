package net.ramixin.mixson.inline.entries;

import net.ramixin.mixson.inline.BuiltMixsonEvent;

public class EventEntry<T> extends AbstractEntry {

    private final BuiltMixsonEvent<T> event;

    public EventEntry(int priority, BuiltMixsonEvent<T> event) {
        super(priority);
        this.event = event;
    }

    public BuiltMixsonEvent<T> event() {
        return event;
    }


    @Override
    public String getName() {
        return event.eventName();
    }

    @Override
    public int getOrdinal() {
        return event.event().ordinal();
    }
}
