package com.gly091020.SableRagdollLib.mixin;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.command.SableSubLevelCommands;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SableSubLevelCommands.class)
public class SableSubLevelCommandsMixin {
    @WrapOperation(method = "executeRemoveSubLevelCommand", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/sublevel/SubLevelContainer;removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V"))
    private static void markRemoved(SubLevelContainer instance, int x, int z, SubLevelRemovalReason reason, Operation<Void> original, @Local(name = "target")SubLevel target){
        // 俺寻思能修复
        if(target.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof AbstractPartBlockEntity)
            target.markRemoved();
        original.call(instance, x, z, reason);
    }
}
