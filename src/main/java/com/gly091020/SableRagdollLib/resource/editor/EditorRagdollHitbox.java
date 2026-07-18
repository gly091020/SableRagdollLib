package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.resource.file.RagdollHitbox;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditorRagdollHitbox{

    public List<EditorHitboxEntry> hitbox;


    public EditorRagdollHitbox(
            List<EditorHitboxEntry> hitbox
    ) {
        this.hitbox = hitbox;
    }


    public EditorRagdollHitbox() {
        this.hitbox = new ArrayList<>();
    }


    public static EditorRagdollHitbox from(
            RagdollHitbox hitbox
    ) {

        List<EditorHitboxEntry> list = new ArrayList<>();

        hitbox.hitbox().forEach((key, value) ->
                list.add(
                        new EditorHitboxEntry(
                                key,
                                EditorPartBox.from(value)
                        )
                )
        );

        return new EditorRagdollHitbox(list);
    }


    public RagdollHitbox toRagdollHitbox() {

        Map<String, RagdollHitbox.PartBox> map =
                new LinkedHashMap<>();

        for (EditorHitboxEntry entry : hitbox) {

            map.put(
                    entry.name,
                    entry.box.toPartBox()
            );
        }

        return new RagdollHitbox(map);
    }


    public static class EditorHitboxEntry
            implements IConfigurable {


        @Configurable
        public String name;


        @Configurable(subConfigurable = true)
        public EditorPartBox box;


        public EditorHitboxEntry() {
            this.name = "";
            this.box = new EditorPartBox();
        }


        public EditorHitboxEntry(
                String name,
                EditorPartBox box
        ) {
            this.name = name;
            this.box = box;
        }
    }


    public static class EditorPartBox
            implements IConfigurable {


        @Configurable
        @ConfigList(
                configuratorMethod = "buildBoxConfigurator",
                addDefaultMethod = "addDefaultBox"
        )
        public List<EditorBox> boxes;



        public EditorPartBox(
                List<EditorBox> boxes
        ) {
            this.boxes = boxes;
        }


        public EditorPartBox() {
            this.boxes = new ArrayList<>();
        }



        private Configurator buildBoxConfigurator(
                Supplier<EditorBox> getter,
                Consumer<EditorBox> setter
        ) {

            var box = getter.get();

            return box != null
                    ? box.createDirectConfigurator()
                    : new Configurator();
        }



        private EditorBox addDefaultBox() {

            return new EditorBox();
        }



        public static EditorPartBox from(
                RagdollHitbox.PartBox box
        ) {

            List<EditorBox> list =
                    new ArrayList<>();

            for (RagdollHitbox.Box value : box.boxes()) {

                list.add(
                        EditorBox.from(value)
                );
            }

            return new EditorPartBox(list);
        }



        public RagdollHitbox.PartBox toPartBox() {

            List<RagdollHitbox.Box> list =
                    new ArrayList<>();

            for (EditorBox box : boxes) {

                list.add(
                        box.toBox()
                );
            }

            return new RagdollHitbox.PartBox(list);
        }

        public Vector3f getCenter() {
            float x = 0;
            float y = 0;
            float z = 0;

            for (var box : boxes) {
                x += (box.minX + box.maxX) / 2f;
                y += (box.minY + box.maxY) / 2f;
                z += (box.minZ + box.maxZ) / 2f;
            }

            x /= boxes.size();
            y /= boxes.size();
            z /= boxes.size();

            return new Vector3f(x, y, z);
        }

        public VoxelShape toVoxelShapeCenter() {
            VoxelShape shape = Shapes.empty();

            Vector3f center = getCenter();

            for (EditorBox box : boxes) {
                shape = Shapes.or(
                        shape,
                        Shapes.box(
                                box.minX - center.x,
                                box.minY - center.y,
                                box.minZ - center.z,
                                box.maxX - center.x,
                                box.maxY - center.y,
                                box.maxZ - center.z
                        )
                );
            }

            if (shape.isEmpty()) {
                return Shapes.block();
            }

            return shape;
        }
    }


    public static class EditorBox
            implements IConfigurable {


        @Configurable
        public float minX;

        @Configurable
        public float minY;

        @Configurable
        public float minZ;

        @Configurable
        public float maxX;

        @Configurable
        public float maxY;

        @Configurable
        public float maxZ;


        public EditorBox(
                float minX,
                float minY,
                float minZ,
                float maxX,
                float maxY,
                float maxZ
        ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;

            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }


        public EditorBox() {

            this.minX = 0;
            this.minY = 0;
            this.minZ = 0;

            this.maxX = 1;
            this.maxY = 1;
            this.maxZ = 1;
        }


        public static EditorBox from(
                RagdollHitbox.Box box
        ) {

            return new EditorBox(
                    box.minX(),
                    box.minY(),
                    box.minZ(),
                    box.maxX(),
                    box.maxY(),
                    box.maxZ()
            );
        }


        public RagdollHitbox.Box toBox() {

            return new RagdollHitbox.Box(
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ
            );
        }
    }
}