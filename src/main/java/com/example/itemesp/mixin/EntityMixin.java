package com.example.itemesp.mixin;

import com.example.itemesp.ItemEspClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void itemesp$color(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof ItemEntity item) {
            Integer color = ItemEspClient.colorFor(item);
            if (color != null) {
                cir.setReturnValue(color);
            }
        }
    }
}
