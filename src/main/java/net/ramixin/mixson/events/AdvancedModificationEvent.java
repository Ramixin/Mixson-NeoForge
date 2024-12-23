package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.BuiltResourceReference;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedModificationEvent extends MixsonEventTypes.Modification {

    JsonElement run(JsonElement elem, HashMap<ResourceLocation, BuiltResourceReference> references);

}
