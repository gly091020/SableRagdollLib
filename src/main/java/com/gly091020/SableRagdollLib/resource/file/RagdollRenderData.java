package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public record RagdollRenderData(Map<String, PartRenderData> renderData) {
    public static final Codec<RagdollRenderData> CODEC = Codec.unboundedMap(
            Codec.STRING,
            PartRenderData.CODEC
    ).xmap(
            RagdollRenderData::new,
            RagdollRenderData::renderData
    );

    /**
     * parts 为渲染白名单，blacklist 中的部件及其子树不会渲染。
     */
    public record PartRenderData(List<EveryPart> parts, List<String> blacklist, Vec3 transform, Vec3 rotation, Vec3 scale){
        private static final Vec3 ONE = new Vec3(1, 1, 1);
        public static final Codec<PartRenderData> CODEC = RecordCodecBuilder.create(i -> i.group(
                EveryPart.CODEC.listOf().fieldOf("parts").forGetter(PartRenderData::parts),
                Codec.STRING.listOf().optionalFieldOf("blacklist", List.of()).forGetter(PartRenderData::blacklist),
                Vec3.CODEC.optionalFieldOf("transform", Vec3.ZERO).forGetter(PartRenderData::transform),
                Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter(PartRenderData::rotation),
                Vec3.CODEC.optionalFieldOf("scale", ONE).forGetter(PartRenderData::scale)
        ).apply(i, PartRenderData::new));

        public PartRenderData(List<EveryPart> parts, Vec3 transform, Vec3 rotation, Vec3 scale) {
            this(parts, List.of(), transform, rotation, scale);
        }

        public PartRenderData {
            parts = parts == null ? List.of() : List.copyOf(parts);
            blacklist = blacklist == null ? List.of() : List.copyOf(blacklist);
        }

        public boolean isBlacklisted(String partName) {
            return blacklist.contains(partName);
        }

        public Set<String> blacklistSet() {
            return blacklist.isEmpty() ? Set.of() : Set.copyOf(blacklist);
        }
    }

    /**
     * flatChild 为旧格式兼容字段；新数据请使用 PartRenderData.blacklist。
     */
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
