package net.ramixin.mixson.atp;

import com.google.gson.JsonElement;

public enum MixsonEventType {
    MODIFICATION(JsonElement.class, true),
    DELETION(void.class, false),
    CREATION(JsonElement.class, false)

    ;
    private final Class<?> returnType;
    private final boolean provideJson;

    MixsonEventType(Class<?> returnType, boolean provideJson) {
        this.returnType = returnType;
        this.provideJson = provideJson;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public boolean providesJson() {
        return provideJson;
    }
}
