package net.ramixin.mixson.util.functions;

import net.ramixin.mixson.EventContext;

@FunctionalInterface
public interface Event<T> {

    void runEvent(EventContext<T> context);
}
