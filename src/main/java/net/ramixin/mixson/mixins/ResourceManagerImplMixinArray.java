package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin({MultiPackResourceManager.class, ReloadableResourceManager.class})
public class ResourceManagerImplMixinArray {

    @ModifyReturnValue(method = "listResources", at = @At("RETURN"))
    private Map<ResourceLocation, Resource> runMixsonEvents(Map<ResourceLocation, Resource> original) {
        return Mixson.runEvents(original);
    }

}
