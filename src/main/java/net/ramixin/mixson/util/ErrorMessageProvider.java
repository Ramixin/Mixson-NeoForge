package net.ramixin.mixson.util;

import net.minecraft.resources.Identifier;

public interface ErrorMessageProvider {

    String getRuntimeMessage(Identifier resourceId);

    String getRegistrationMessage();

    boolean failSilently();
}
