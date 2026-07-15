package com.gly091020.SableRagdollLib.mixin;

import com.gly091020.SableRagdollLib.block.AbstractPartBlock;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 给某些小人的 Mixin
// 不用 tick 是因为对性能会有影响
@Mixin(value = LevelChunk.class, priority = 1001)
public class LevelChunkMixin {
    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void sableragdolllib$prePreSetBlockState(BlockPos blockPos, BlockState blockState, boolean p_62867_, CallbackInfoReturnable<BlockState> cir){
        if(blockState.getBlock() instanceof AbstractPartBlock &&
                SableCompanion.INSTANCE.getContaining(level, blockPos) == null)
            cir.setReturnValue(this.level.getBlockState(blockPos));
    }
}
