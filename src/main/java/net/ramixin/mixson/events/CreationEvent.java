package net.ramixin.mixson.events;

import com.google.gson.JsonElement;

@FunctionalInterface
public interface CreationEvent extends MixsonEventTypes.Creation {

    JsonElement run();

}
