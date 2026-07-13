package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public record RagdollPosition(Map<String, PartSetting> position) {
    public static final Codec<RagdollPosition> CODEC = Codec.unboundedMap(
            Codec.STRING,
            PartSetting.CODEC
    ).xmap(
            RagdollPosition::new,
            RagdollPosition::position
    );
    public record PartSetting(Vec3 transform, Vec3 rotation){
        public static final Codec<PartSetting> CODEC = RecordCodecBuilder.create(i -> i.group(
                Vec3.CODEC.optionalFieldOf("transform", Vec3.ZERO).forGetter(PartSetting::transform),
                Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter(PartSetting::rotation)
        ).apply(i, PartSetting::new));

        public Pose3d getPose(Vec3 origin){
            Pose3d pose = new Pose3d();
            pose.position().set(origin.add(transform.scale(1 / 16f)).toVector3f());
            pose.orientation().rotateXYZ(
                    Math.toRadians(rotation.x),
                    Math.toRadians(rotation.y),
                    Math.toRadians(rotation.z)
            );
            return pose;
        }
    }
}
