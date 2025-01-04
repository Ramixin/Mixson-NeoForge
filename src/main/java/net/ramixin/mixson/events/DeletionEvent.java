package net.ramixin.mixson.events;

@FunctionalInterface
public interface DeletionEvent extends MixsonEventTypes.Deletion {
    
    boolean run();

    @Override
    default String getName() {
        return "Deletion Event";
    }
}
