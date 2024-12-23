package net.ramixin.mixson;

import net.minecraft.resources.ResourceLocation;

public record ResourceReference(int priority, ResourceLocation resourceId, ResourceLocation referenceId) {
}
