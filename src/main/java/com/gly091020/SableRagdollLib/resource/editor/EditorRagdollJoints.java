package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.resource.file.RagdollJoints;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditorRagdollJoints{

    public List<EditorJointData> jointData;

    public EditorRagdollJoints(List<EditorJointData> jointData) {
        this.jointData = jointData;
    }

    public EditorRagdollJoints() {
        this.jointData = new ArrayList<>();
    }

    public static EditorRagdollJoints from(RagdollJoints joints) {
        List<EditorJointData> list = new ArrayList<>();

        for (RagdollJoints.JointData joint : joints.jointData()) {
            list.add(EditorJointData.from(joint));
        }

        return new EditorRagdollJoints(list);
    }

    public RagdollJoints toRagdollJoints() {
        List<RagdollJoints.JointData> list = new ArrayList<>();

        for (EditorJointData joint : jointData) {
            list.add(joint.toJointData());
        }

        return new RagdollJoints(list);
    }


    public static class EditorJointData implements IConfigurable {

        @Configurable
        public String a;
        @Configurable
        public String b;

        @Configurable
        public Vector3f posA;
        @Configurable
        public Vector3f posB;

        @Configurable(subConfigurable = true)
        public EditorJointSettings jointSettings;

        public EditorJointData(
                String a,
                String b,
                Vector3f posA,
                Vector3f posB,
                EditorJointSettings jointSettings
        ) {
            if(jointSettings == null)jointSettings = new EditorJointSettings(false);
            this.a = a;
            this.b = b;
            this.posA = posA;
            this.posB = posB;
            this.jointSettings = jointSettings;
        }

        public EditorJointData() {
            this.a = "";
            this.b = "";
            this.posA = new Vector3f();
            this.posB = new Vector3f();
            this.jointSettings = new EditorJointSettings(false);
        }

        public static EditorJointData from(RagdollJoints.JointData data) {
            return new EditorJointData(
                    data.a(),
                    data.b(),
                    new Vector3f(
                            (float) data.posA().x,
                            (float) data.posA().y,
                            (float) data.posA().z
                    ),
                    new Vector3f(
                            (float) data.posB().x,
                            (float) data.posB().y,
                            (float) data.posB().z
                    ),
                    data.jointSettings()
                            .map(EditorJointSettings::from)
                            .orElse(new EditorJointSettings(false))
            );
        }

        public RagdollJoints.JointData toJointData() {
            return new RagdollJoints.JointData(
                    a,
                    b,
                    new Vec3(
                            posA.x,
                            posA.y,
                            posA.z
                    ),
                    new Vec3(
                            posB.x,
                            posB.y,
                            posB.z
                    ),
                    Optional.ofNullable(jointSettings.toJointSettings())
            );
        }
    }


    public static class EditorJointSettings implements IToggleConfigurable {
        public boolean enable = true;

        @Configurable
        public boolean contacts;

        @Configurable(subConfigurable = true)
        public EditorJointMotor jointMotor;

        public EditorJointSettings(
                boolean contacts,
                EditorJointMotor jointMotor
        ) {
            if(jointMotor == null)jointMotor = new EditorJointMotor(false);
            this.contacts = contacts;
            this.jointMotor = jointMotor;
        }

        public EditorJointSettings(boolean enable) {
            this.contacts = true;
            this.jointMotor = new EditorJointMotor(false);
            this.enable = enable;
        }

        public static EditorJointSettings from(RagdollJoints.JointSettings settings) {
            return new EditorJointSettings(
                    settings.contacts(),
                    settings.jointMotor()
                            .map(EditorJointMotor::from)
                            .orElse(new EditorJointMotor(false))
            );
        }

        public RagdollJoints.JointSettings toJointSettings() {
            if(!enable)return null;
            return new RagdollJoints.JointSettings(
                    contacts,
                    Optional.ofNullable(jointMotor.toJointMotor())
            );
        }

        @Override
        public boolean isEnable() {
            return enable;
        }

        @Override
        public void setEnable(boolean enable) {
            this.enable = enable;
        }
    }


    public static class EditorJointMotor implements IToggleConfigurable {
        public boolean enable = true;

        @Configurable
        public int stiffness;
        @Configurable
        public int damping;

        public EditorJointMotor(
                int stiffness,
                int damping
        ) {
            this.stiffness = stiffness;
            this.damping = damping;
        }

        public EditorJointMotor(boolean enable) {
            this.stiffness = 0;
            this.damping = 0;
            this.enable = enable;
        }

        public static EditorJointMotor from(RagdollJoints.JointMotor motor) {
            return new EditorJointMotor(
                    motor.stiffness(),
                    motor.damping()
            );
        }

        public RagdollJoints.JointMotor toJointMotor() {
            if(!enable)return null;
            return new RagdollJoints.JointMotor(
                    stiffness,
                    damping
            );
        }

        @Override
        public boolean isEnable() {
            return enable;
        }

        @Override
        public void setEnable(boolean enable) {
            this.enable = enable;
        }
    }
}