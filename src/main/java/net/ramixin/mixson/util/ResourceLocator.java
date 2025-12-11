package net.ramixin.mixson.util;

import net.minecraft.resources.Identifier;

import java.util.function.Function;

@FunctionalInterface
public interface ResourceLocator extends Function<Identifier, Boolean> {

}
