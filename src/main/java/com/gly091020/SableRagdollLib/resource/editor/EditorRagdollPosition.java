package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.resource.file.RagdollPosition;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorRagdollPosition{

    public List<EditorPositionEntry> position;


    public EditorRagdollPosition(
            List<EditorPositionEntry> position
    ) {
        this.position = position;
    }


    public EditorRagdollPosition() {
        this.position = new ArrayList<>();
    }


    public static EditorRagdollPosition from(
            RagdollPosition position
    ) {

        List<EditorPositionEntry> list = new ArrayList<>();

        position.position().forEach((key, value) ->
                list.add(
                        new EditorPositionEntry(
                                key,
                                EditorPartSetting.from(value)
                        )
                )
        );

        return new EditorRagdollPosition(list);
    }


    public RagdollPosition toRagdollPosition() {

        Map<String, RagdollPosition.PartSetting> map =
                new LinkedHashMap<>();

        for (EditorPositionEntry entry : position) {

            map.put(
                    entry.name,
                    entry.setting.toPartSetting()
            );
        }

        return new RagdollPosition(map);
    }


    public static class EditorPositionEntry
            implements IConfigurable {

        @Configurable
        public String name;


        @Configurable(subConfigurable = true)
        public EditorPartSetting setting;


        public EditorPositionEntry() {
            this.name = "";
            this.setting = new EditorPartSetting();
        }


        public EditorPositionEntry(
                String name,
                EditorPartSetting setting
        ) {
            this.name = name;
            this.setting = setting;
        }
    }


    public static class EditorPartSetting
            implements IConfigurable {

        @Configurable
        public Vector3f transform;


        @Configurable
        public Vector3f rotation;


        public EditorPartSetting(
                Vector3f transform,
                Vector3f rotation
        ) {
            this.transform = transform;
            this.rotation = rotation;
        }


        public EditorPartSetting() {
            this.transform = new Vector3f();
            this.rotation = new Vector3f();
        }


        public Pose3d getPose(
                Vector3f origin
        ) {

            Pose3d pose = new Pose3d();

            pose.position().set(
                    new Vector3f(origin)
                            .add(new Vector3f(transform)
                                    .mul(1 / 16f))
            );

            pose.orientation().rotateXYZ(
                    Math.toRadians(rotation.x),
                    Math.toRadians(rotation.y),
                    Math.toRadians(rotation.z)
            );

            return pose;
        }


        public static EditorPartSetting from(
                RagdollPosition.PartSetting setting
        ) {

            return new EditorPartSetting(
                    new Vector3f(
                            (float) setting.transform().x,
                            (float) setting.transform().y,
                            (float) setting.transform().z
                    ),
                    new Vector3f(
                            (float) setting.rotation().x,
                            (float) setting.rotation().y,
                            (float) setting.rotation().z
                    )
            );
        }


        public RagdollPosition.PartSetting toPartSetting() {

            return new RagdollPosition.PartSetting(
                    new Vec3(
                            transform.x,
                            transform.y,
                            transform.z
                    ),
                    new Vec3(
                            rotation.x,
                            rotation.y,
                            rotation.z
                    )
            );
        }
    }
}