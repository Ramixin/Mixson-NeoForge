package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface CreationEvent extends MixsonEventTypes.Creation {

    @Nullable JsonElement run();

}
