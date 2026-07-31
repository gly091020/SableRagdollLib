package com.gly091020.SableRagdollLib.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.gly091020.SableRagdollLib.api.event.EntityHurtBySubLevelEvent;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// 943 看到要被气死的 mixin²
@Mixin(value = LivingEntity.class, priority = 1001)
public class LivingEntityMixinMixin {
    @TargetHandler(
            mixin = "dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision.LivingEntityMixin",
            name = "sable$computeCollisionEffects"
    )
    @ModifyVariable(
            method = "@MixinSquared:Handler",
            at = @At(value = "STORE", ordinal = 0),
            name = "damageAmount"
    )
    private float modifyDamage(float originalDamage, @Local SubLevel collidedSubLevel, @Local double magnitude) {
        return NeoForge.EVENT_BUS.post(new EntityHurtBySubLevelEvent(collidedSubLevel,
                (LivingEntity)(Object)this,
                magnitude, originalDamage)).getDamage();
    }
}
