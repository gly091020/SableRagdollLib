package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.resource.file.*;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditorDefFile implements IConfigurable {

    @Configurable
    public ResourceLocation type;


    @Configurable
    public List<String> allParts;


    @Configurable
    @ConfigList(
            configuratorMethod = "buildHitboxConfigurator",
            addDefaultMethod = "addDefaultHitbox"
    )
    public List<EditorRagdollHitbox.EditorHitboxEntry> hitbox;


    @Configurable
    @ConfigList(
            configuratorMethod = "buildPositionConfigurator",
            addDefaultMethod = "addDefaultPosition"
    )
    public List<EditorRagdollPosition.EditorPositionEntry> position;


    @Configurable
    @ConfigList(
            configuratorMethod = "buildRenderDataConfigurator",
            addDefaultMethod = "addDefaultRenderData"
    )
    public List<EditorRagdollRenderData.EditorRenderEntry> renderData;


    @Configurable
    @ConfigList(
            configuratorMethod = "buildJointsConfigurator",
            addDefaultMethod = "addDefaultJoints"
    )
    public List<EditorRagdollJoints.EditorJointData> joints;


    @Configurable
    @ConfigList(
            configuratorMethod = "buildExpressionsConfigurator",
            addDefaultMethod = "addDefaultExpressions"
    )
    public List<EditorRagdollExpressions.EditorExpressionPart> expressions;


    @Configurable
    public String mainBody;


    @Configurable
    public CompoundTag extra;



    public EditorDefFile() {
        this(
                EMPTY,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                new CompoundTag()
        );
    }


    public EditorDefFile(
            ResourceLocation type,
            List<String> allParts,
            List<EditorRagdollHitbox.EditorHitboxEntry> hitbox,
            List<EditorRagdollPosition.EditorPositionEntry> position,
            List<EditorRagdollRenderData.EditorRenderEntry> renderData,
            List<EditorRagdollJoints.EditorJointData> joints,
            List<EditorRagdollExpressions.EditorExpressionPart> expressions,
            String mainBody,
            CompoundTag extra
    ) {
        this.type = type;
        this.allParts = allParts;
        this.hitbox = hitbox;
        this.position = position;
        this.renderData = renderData;
        this.joints = joints;
        this.expressions = expressions;
        this.mainBody = mainBody;
        this.extra = extra;
    }



    public RagdollDefFile toRecord() {

        Map<String, RagdollHitbox.PartBox> hitboxMap =
                new LinkedHashMap<>();

        for (var entry : hitbox) {
            hitboxMap.put(
                    entry.name,
                    entry.box.toPartBox()
            );
        }


        Map<String, RagdollPosition.PartSetting> positionMap =
                new LinkedHashMap<>();

        for (var entry : position) {
            positionMap.put(
                    entry.name,
                    entry.setting.toPartSetting()
            );
        }


        Map<String, RagdollRenderData.PartRenderData> renderMap =
                new LinkedHashMap<>();

        for (var entry : renderData) {
            renderMap.put(
                    entry.name,
                    entry.data.toPartRenderData()
            );
        }


        List<RagdollJoints.JointData> jointList =
                new ArrayList<>();

        for (var joint : joints) {
            jointList.add(
                    joint.toJointData()
            );
        }


        Map<String, Map<String, RagdollExpressions.Expression>> expressionMap =
                new LinkedHashMap<>();

        for (var part : expressions) {

            Map<String, RagdollExpressions.Expression> map =
                    new LinkedHashMap<>();

            for (var entry : part.expressions) {
                map.put(
                        entry.name,
                        entry.expression.toExpression()
                );
            }

            expressionMap.put(
                    part.partName,
                    map
            );
        }


        return new RagdollDefFile(
                type,
                new ArrayList<>(allParts),
                new RagdollHitbox(hitboxMap),
                new RagdollPosition(positionMap),
                new RagdollRenderData(renderMap),
                new RagdollJoints(jointList),
                new RagdollExpressions(expressionMap),
                Optional.ofNullable(mainBody),
                extra
        );
    }



    public static EditorDefFile fromRecord(
            RagdollDefFile defFile
    ) {

        List<EditorRagdollHitbox.EditorHitboxEntry> hitbox =
                new ArrayList<>();

        defFile.hitbox().hitbox().forEach(
                (name, box) ->
                        hitbox.add(
                                new EditorRagdollHitbox.EditorHitboxEntry(
                                        name,
                                        EditorRagdollHitbox.EditorPartBox.from(box)
                                )
                        )
        );


        List<EditorRagdollPosition.EditorPositionEntry> position =
                new ArrayList<>();

        defFile.position().position().forEach(
                (name, setting) ->
                        position.add(
                                new EditorRagdollPosition.EditorPositionEntry(
                                        name,
                                        EditorRagdollPosition.EditorPartSetting.from(setting)
                                )
                        )
        );


        List<EditorRagdollRenderData.EditorRenderEntry> renderData =
                new ArrayList<>();

        defFile.renderData().renderData().forEach(
                (name, data) ->
                        renderData.add(
                                new EditorRagdollRenderData.EditorRenderEntry(
                                        name,
                                        EditorRagdollRenderData.EditorPartRenderData.from(data)
                                )
                        )
        );


        List<EditorRagdollJoints.EditorJointData> joints =
                new ArrayList<>();

        for (var joint : defFile.joints().jointData()) {
            joints.add(
                    EditorRagdollJoints.EditorJointData.from(joint)
            );
        }


        List<EditorRagdollExpressions.EditorExpressionPart> expressions =
                new ArrayList<>();

        defFile.expressions().expressions().forEach(
                (partName, map) -> {

                    List<EditorRagdollExpressions.EditorExpressionEntry> list =
                            new ArrayList<>();

                    map.forEach(
                            (name, expression) ->
                                    list.add(
                                            new EditorRagdollExpressions.EditorExpressionEntry(
                                                    name,
                                                    EditorRagdollExpressions.EditorExpression.from(expression)
                                            )
                                    )
                    );

                    expressions.add(
                            new EditorRagdollExpressions.EditorExpressionPart(
                                    partName,
                                    list
                            )
                    );
                }
        );


        return new EditorDefFile(
                defFile.type(),
                new ArrayList<>(defFile.allParts()),
                hitbox,
                position,
                renderData,
                joints,
                expressions,
                defFile.mainBody().orElse(null),
                defFile.extra()
        );
    }



    public static final ResourceLocation EMPTY =
            ResourceLocation.fromNamespaceAndPath(
                    SableRagdollLib.MODID,
                    "empty"
            );



    public static EditorDefFile createEmpty() {

        return new EditorDefFile(
                EMPTY,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                new CompoundTag()
        );
    }

    private Configurator buildHitboxConfigurator(
            Supplier<EditorRagdollHitbox.EditorHitboxEntry> getter,
            Consumer<EditorRagdollHitbox.EditorHitboxEntry> setter
    ) {
        var entry = getter.get();

        return entry != null
                ? entry.createDirectConfigurator()
                : new Configurator();
    }


    private EditorRagdollHitbox.EditorHitboxEntry addDefaultHitbox() {

        return new EditorRagdollHitbox.EditorHitboxEntry();
    }

    private Configurator buildPositionConfigurator(
            Supplier<EditorRagdollPosition.EditorPositionEntry> getter,
            Consumer<EditorRagdollPosition.EditorPositionEntry> setter
    ) {
        var value = getter.get();

        return value != null
                ? value.createDirectConfigurator()
                : new Configurator();
    }


    private EditorRagdollPosition.EditorPositionEntry addDefaultPosition(){
        return new EditorRagdollPosition.EditorPositionEntry();
    }

    private Configurator buildJointsConfigurator(
            Supplier<EditorRagdollJoints.EditorJointData> getter,
            Consumer<EditorRagdollJoints.EditorJointData> setter
    ) {
        var value = getter.get();

        return value != null
                ? value.createDirectConfigurator()
                : new Configurator();
    }


    private EditorRagdollJoints.EditorJointData addDefaultJoints(){
        return new EditorRagdollJoints.EditorJointData();
    }

    private Configurator buildExpressionsConfigurator(
            Supplier<EditorRagdollExpressions.EditorExpressionPart> getter,
            Consumer<EditorRagdollExpressions.EditorExpressionPart> setter
    ) {
        var value = getter.get();

        return value != null
                ? value.createDirectConfigurator()
                : new Configurator();
    }


    private EditorRagdollExpressions.EditorExpressionPart addDefaultExpressions(){
        return new EditorRagdollExpressions.EditorExpressionPart();
    }

    private Configurator buildRenderDataConfigurator(
            Supplier<EditorRagdollRenderData.EditorRenderEntry> getter,
            Consumer<EditorRagdollRenderData.EditorRenderEntry> setter
    ) {
        var value = getter.get();

        return value != null
                ? value.createDirectConfigurator()
                : new Configurator();
    }


    private EditorRagdollRenderData.EditorRenderEntry addDefaultRenderData(){
        return new EditorRagdollRenderData.EditorRenderEntry();
    }
}