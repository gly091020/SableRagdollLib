package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Optional;

public record RagdollJoints(List<JointData> jointData) {
    public static final Codec<RagdollJoints> CODEC = Codec.list(JointData.CODEC).xmap(
            RagdollJoints::new,
            RagdollJoints::jointData
    );
    public record JointData(String a, String b, Vec3 posA, Vec3 posB, Optional<JointSettings> jointSettings){
        public static final Codec<JointData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("A").forGetter(JointData::a),
                Codec.STRING.fieldOf("B").forGetter(JointData::b),
                Vec3.CODEC.fieldOf("posA").forGetter(JointData::posA),
                Vec3.CODEC.fieldOf("posB").forGetter(JointData::posB),
                JointSettings.CODEC.optionalFieldOf("settings").forGetter(JointData::jointSettings)
        ).apply(i, JointData::new));

        public Vector3dc getVector3dcA(ServerSubLevel subLevel){
            var p1 = new Pose3d(subLevel.logicalPose());
            return localToWorld(p1, new Vector3d(posA.x, posA.y, posA.z).div(16));
        }

        public Vector3dc getVector3dcB(ServerSubLevel subLevel){
            var p1 = subLevel.logicalPose();
            return localToWorld(p1, new Vector3d(posB.x, posB.y, posB.z).div(16));
        }

        // 在碰Pose3dc.transformPosition我就是傻逼
        // 我真的再碰了Pose3dc.transformPosition，我就是傻逼
        public static Vector3d localToWorld(Pose3dc pose, Vector3dc local) {
            var dest = new Vector3d();
            double x = local.x();
            double y = local.y();
            double z = local.z();

            Vector3dc s = pose.scale();
            x *= s.x();
            y *= s.y();
            z *= s.z();

            pose.orientation().transform(x, y, z, dest);

            return dest.add(pose.rotationPoint());
        }
    }

    public record JointSettings(boolean contacts, Optional<JointMotor> jointMotor){
        public static final Codec<JointSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("contacts", true).forGetter(JointSettings::contacts),
                JointMotor.CODEC.optionalFieldOf("motor").forGetter(JointSettings::jointMotor)
        ).apply(i, JointSettings::new));
    }

    public record JointMotor(int stiffness, int damping){
        public static final Codec<JointMotor> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("stiffness").forGetter(JointMotor::stiffness),
                Codec.INT.fieldOf("damping").forGetter(JointMotor::damping)
        ).apply(i, JointMotor::new));
    }
}
