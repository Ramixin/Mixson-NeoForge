package net.ramixin.mixson.util;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

@FunctionalInterface
public interface ResourceLocator extends Function<ResourceLocation, Boolean> {

}
