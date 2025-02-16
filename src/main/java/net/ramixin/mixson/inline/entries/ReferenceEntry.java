package net.ramixin.mixson.inline.entries;

import net.ramixin.mixson.inline.BuiltResourceReference;

public class ReferenceEntry<T> extends AbstractEntry {

    private final BuiltResourceReference<T> reference;

    public ReferenceEntry(int priority, BuiltResourceReference<T> event) {
        super(priority);
        this.reference = event;
    }

    public BuiltResourceReference<T> reference() {
        return reference;
    }

    @Override
    public String getName() {
        return reference.getReferenceId().toString();
    }

    @Override
    public int getOrdinal() {
        return reference.getOrdinal();
    }
}
