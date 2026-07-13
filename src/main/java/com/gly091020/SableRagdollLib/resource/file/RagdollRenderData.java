package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record RagdollRenderData(Map<String, PartRenderData> renderData) {
    public static final Codec<RagdollRenderData> CODEC = Codec.unboundedMap(
            Codec.STRING,
            PartRenderData.CODEC
    ).xmap(
            RagdollRenderData::new,
            RagdollRenderData::renderData
    );

    public record PartRenderData(List<EveryPart> parts, Vec3 transform, Vec3 rotation, Vec3 scale){
        private static final Vec3 ONE = new Vec3(1, 1, 1);
        public static final Codec<PartRenderData> CODEC = RecordCodecBuilder.create(i -> i.group(
                EveryPart.CODEC.listOf().fieldOf("parts").forGetter(PartRenderData::parts),
                Vec3.CODEC.optionalFieldOf("transform", Vec3.ZERO).forGetter(PartRenderData::transform),
                Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter(PartRenderData::rotation),
                Vec3.CODEC.optionalFieldOf("scale", ONE).forGetter(PartRenderData::scale)
        ).apply(i, PartRenderData::new));
    }

    public record EveryPart(String partName, boolean flatChild){
        public static final Codec<EveryPart> ORIGIN_CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("partName").forGetter(EveryPart::partName),
                Codec.BOOL.optionalFieldOf("flatChild", false).forGetter(EveryPart::flatChild)
        ).apply(i, EveryPart::new));
        public static final Codec<EveryPart> STRING_CODEC =
                Codec.STRING.xmap(
                        s -> new EveryPart(s, false),
                        EveryPart::partName
                );
        public static final Codec<EveryPart> CODEC = Codec.either(
                ORIGIN_CODEC,
                STRING_CODEC
        ).xmap(
                either -> either.map(
                        Function.identity(),
                        Function.identity()
                ),
                Either::left
        );
    }
}
