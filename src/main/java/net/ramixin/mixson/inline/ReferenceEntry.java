package net.ramixin.mixson.inline;

public class ReferenceEntry extends AbstractEntry {

    private final BuiltResourceReference reference;

    public ReferenceEntry(int priority, BuiltResourceReference event) {
        super(priority);
        this.reference = event;
    }

    public BuiltResourceReference reference() {
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
