package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.resource.file.RagdollRenderData;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditorRagdollRenderData{

    public List<EditorRenderEntry> renderData;


    public EditorRagdollRenderData(
            List<EditorRenderEntry> renderData
    ) {
        this.renderData = renderData;
    }


    public EditorRagdollRenderData() {
        this.renderData = new ArrayList<>();
    }


    public static EditorRagdollRenderData from(
            RagdollRenderData data
    ) {

        List<EditorRenderEntry> list = new ArrayList<>();

        data.renderData().forEach((key, value) ->
                list.add(
                        new EditorRenderEntry(
                                key,
                                EditorPartRenderData.from(value)
                        )
                )
        );

        return new EditorRagdollRenderData(list);
    }


    public RagdollRenderData toRagdollRenderData() {

        Map<String, RagdollRenderData.PartRenderData> map =
                new LinkedHashMap<>();

        for (EditorRenderEntry entry : renderData) {

            map.put(
                    entry.name,
                    entry.data.toPartRenderData()
            );
        }

        return new RagdollRenderData(map);
    }


    public static class EditorRenderEntry
            implements IConfigurable {

        @Configurable
        public String name;


        @Configurable(subConfigurable = true)
        public EditorPartRenderData data;


        public EditorRenderEntry() {
            this.name = "";
            this.data = new EditorPartRenderData();
        }


        public EditorRenderEntry(
                String name,
                EditorPartRenderData data
        ) {
            this.name = name;
            this.data = data;
        }
    }


    public static class EditorPartRenderData
            implements IConfigurable {


        @Configurable
        @ConfigList(
                configuratorMethod = "buildPartConfigurator",
                addDefaultMethod = "addDefaultPart"
        )
        public List<EditorEveryPart> parts;


        @Configurable
        public Vector3f transform;


        @Configurable
        public Vector3f rotation;


        @Configurable
        public Vector3f scale;



        public EditorPartRenderData(
                List<EditorEveryPart> parts,
                Vector3f transform,
                Vector3f rotation,
                Vector3f scale
        ) {
            this.parts = parts;
            this.transform = transform;
            this.rotation = rotation;
            this.scale = scale;
        }


        public EditorPartRenderData() {

            this.parts = new ArrayList<>();

            this.transform = new Vector3f();

            this.rotation = new Vector3f();

            this.scale = new Vector3f(1, 1, 1);
        }



        private Configurator buildPartConfigurator(
                Supplier<EditorEveryPart> getter,
                Consumer<EditorEveryPart> setter
        ) {

            var part = getter.get();

            return part != null
                    ? part.createDirectConfigurator()
                    : new Configurator();
        }



        private EditorEveryPart addDefaultPart() {

            return new EditorEveryPart();
        }



        public static EditorPartRenderData from(
                RagdollRenderData.PartRenderData data
        ) {

            List<EditorEveryPart> parts =
                    new ArrayList<>();

            for (RagdollRenderData.EveryPart part : data.parts()) {

                parts.add(
                        EditorEveryPart.from(part)
                );
            }

            return new EditorPartRenderData(
                    parts,
                    vecToVector(data.transform()),
                    vecToVector(data.rotation()),
                    vecToVector(data.scale())
            );
        }



        public RagdollRenderData.PartRenderData toPartRenderData() {

            List<RagdollRenderData.EveryPart> list =
                    new ArrayList<>();

            for (EditorEveryPart part : parts) {

                list.add(
                        part.toEveryPart()
                );
            }


            return new RagdollRenderData.PartRenderData(
                    list,
                    vectorToVec(transform),
                    vectorToVec(rotation),
                    vectorToVec(scale)
            );
        }



        private static Vector3f vecToVector(Vec3 vec) {

            return new Vector3f(
                    (float) vec.x,
                    (float) vec.y,
                    (float) vec.z
            );
        }



        private static Vec3 vectorToVec(Vector3f vec) {

            return new Vec3(
                    vec.x,
                    vec.y,
                    vec.z
            );
        }
    }


    public static class EditorEveryPart
            implements IConfigurable {


        @Configurable
        public String partName;


        @Configurable
        public boolean flatChild;


        public EditorEveryPart(
                String partName,
                boolean flatChild
        ) {
            this.partName = partName;
            this.flatChild = flatChild;
        }


        public EditorEveryPart() {
            this.partName = "";
            this.flatChild = false;
        }


        public static EditorEveryPart from(
                RagdollRenderData.EveryPart part
        ) {

            return new EditorEveryPart(
                    part.partName(),
                    part.flatChild()
            );
        }


        public RagdollRenderData.EveryPart toEveryPart() {

            return new RagdollRenderData.EveryPart(
                    partName,
                    flatChild
            );
        }
    }
}