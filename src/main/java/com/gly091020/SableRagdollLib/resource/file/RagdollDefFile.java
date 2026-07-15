package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record RagdollDefFile(
        ResourceLocation type,
        List<String> allParts,
        RagdollHitbox hitbox,
        RagdollPosition position,
        RagdollRenderData renderData,
        RagdollJoints joints,
        RagdollExpressions expressions,
        Optional<String> mainBody,
        CompoundTag extra) {
    public static final Codec<RagdollDefFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(RagdollDefFile::type),
            Codec.STRING.listOf().fieldOf("allParts").forGetter(RagdollDefFile::allParts),
            RagdollHitbox.CODEC.fieldOf("hitbox").forGetter(RagdollDefFile::hitbox),
            RagdollPosition.CODEC.fieldOf("position").forGetter(RagdollDefFile::position),
            RagdollRenderData.CODEC.fieldOf("render").forGetter(RagdollDefFile::renderData),
            RagdollJoints.CODEC.fieldOf("joints").forGetter(RagdollDefFile::joints),
            RagdollExpressions.CODEC.optionalFieldOf("expressions", RagdollExpressions.EMPTY).forGetter(RagdollDefFile::expressions),
            Codec.STRING.optionalFieldOf("mainBody").forGetter(RagdollDefFile::mainBody),
            CompoundTag.CODEC.optionalFieldOf("extraData", new CompoundTag()).forGetter(RagdollDefFile::extra)
    ).apply(i, RagdollDefFile::new));
}
