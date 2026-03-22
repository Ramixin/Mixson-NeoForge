package net.ramixin.mixson.enums;

public enum DebugOption {
    EXPORT_PATCHED_FILE,
    EXPORT_UNPATCHED_FILE,
    BASIC_LOGGING,
    EXTRA_LOGGING

    ;

    public int getMask() {
        return 1 << ordinal();
    }
}
