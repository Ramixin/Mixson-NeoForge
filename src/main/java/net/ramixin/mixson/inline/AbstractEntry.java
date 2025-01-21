package net.ramixin.mixson.inline;

public abstract class AbstractEntry {

    private final int priority;

    AbstractEntry(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    public abstract String getName();

    public abstract int getOrdinal();

}
