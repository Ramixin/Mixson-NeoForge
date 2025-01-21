package net.ramixin.mixson.inline;

public record ResourceReference(int priority, String resourceId, String referenceId, int ordinal) {

    public ResourceReference(int priority, String resourceId, String referenceId) {
        this(priority, resourceId, referenceId, 0);
    }

}
