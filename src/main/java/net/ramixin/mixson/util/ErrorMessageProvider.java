package net.ramixin.mixson.util;

import net.minecraft.resources.ResourceLocation;

public interface ErrorMessageProvider {

    String getRuntimeMessage(ResourceLocation resourceId);

    String getRegistrationMessage();

    boolean failSilently();
}
