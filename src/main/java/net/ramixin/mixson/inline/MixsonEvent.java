package net.ramixin.mixson.inline;

@FunctionalInterface
public interface MixsonEvent<T> {

    void runEvent(EventContext<T> context);

    default int ordinal() {
        return -1;
    }

}
