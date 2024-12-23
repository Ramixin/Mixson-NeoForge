package net.ramixin.mixson.events;

import com.google.gson.JsonElement;

@FunctionalInterface
public interface ModificationEvent extends MixsonEventTypes.Modification {

    JsonElement run(JsonElement elem);

}
