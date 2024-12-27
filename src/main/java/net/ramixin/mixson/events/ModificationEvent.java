package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ModificationEvent extends MixsonEventTypes.Modification {

    @NotNull JsonElement run(JsonElement elem);

}
