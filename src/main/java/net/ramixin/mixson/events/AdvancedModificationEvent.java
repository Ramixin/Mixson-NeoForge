package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.BuiltResourceReference;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedModificationEvent extends MixsonEventTypes.Modification {

    @NotNull JsonElement run(JsonElement elem, HashMap<ResourceLocation, BuiltResourceReference> references);

    @Override
    default String getName() {
        return "Advanced Modification Event";
    }
}
