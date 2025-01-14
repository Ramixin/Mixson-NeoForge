package net.ramixin.mixson.debug;

import org.jetbrains.annotations.NotNull;

public record CallCountEntry(int eventCalls, int fileOperations) implements Comparable<CallCountEntry> {

    public static final CallCountEntry DEFAULT = new CallCountEntry(0, 0);

    @Override
    public int compareTo(@NotNull CallCountEntry o) {
        int first = Integer.compare(eventCalls, o.eventCalls);
        if (first != 0) return -first;
        return -Integer.compare(fileOperations, o.fileOperations);
    }

    public CallCountEntry update(int fileOperations) {
        return new CallCountEntry(this.eventCalls + 1 , this.fileOperations + fileOperations);
    }
}
