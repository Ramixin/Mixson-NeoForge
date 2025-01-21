package net.ramixin.mixson.inline;

public class EventEntry extends AbstractEntry {

    private final BuiltMixsonEvent event;

    public EventEntry(int priority, BuiltMixsonEvent event) {
        super(priority);
        this.event = event;
    }

    public BuiltMixsonEvent event() {
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
