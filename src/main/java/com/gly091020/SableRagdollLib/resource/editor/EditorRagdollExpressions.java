package com.gly091020.SableRagdollLib.resource.editor;

import com.gly091020.SableRagdollLib.resource.file.RagdollExpressions;
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

public class EditorRagdollExpressions{

    public List<EditorExpressionPart> expressions;


    public EditorRagdollExpressions(
            List<EditorExpressionPart> expressions
    ) {
        this.expressions = expressions;
    }


    public EditorRagdollExpressions() {
        this.expressions = new ArrayList<>();
    }


    public static EditorRagdollExpressions from(
            RagdollExpressions expressions
    ) {

        List<EditorExpressionPart> list =
                new ArrayList<>();

        expressions.expressions().forEach((partName, map) -> {

            List<EditorExpressionEntry> entries =
                    new ArrayList<>();

            map.forEach((name, expression) ->

                    entries.add(
                            new EditorExpressionEntry(
                                    name,
                                    EditorExpression.from(expression)
                            )
                    )
            );


            list.add(
                    new EditorExpressionPart(
                            partName,
                            entries
                    )
            );
        });


        return new EditorRagdollExpressions(list);
    }


    public RagdollExpressions toRagdollExpressions() {

        Map<String, Map<String, RagdollExpressions.Expression>> map =
                new LinkedHashMap<>();


        for (EditorExpressionPart part : expressions) {

            Map<String, RagdollExpressions.Expression> expressions =
                    new LinkedHashMap<>();


            for (EditorExpressionEntry entry : part.expressions) {

                expressions.put(
                        entry.name,
                        entry.expression.toExpression()
                );
            }


            map.put(
                    part.partName,
                    expressions
            );
        }


        return new RagdollExpressions(map);
    }


    public static class EditorExpressionPart
            implements IConfigurable {


        @Configurable
        public String partName;


        @Configurable
        @ConfigList(
                configuratorMethod = "buildExpressionConfigurator",
                addDefaultMethod = "addDefaultExpression"
        )
        public List<EditorExpressionEntry> expressions;



        public EditorExpressionPart() {

            this.partName = "";

            this.expressions =
                    new ArrayList<>();
        }


        public EditorExpressionPart(
                String partName,
                List<EditorExpressionEntry> expressions
        ) {

            this.partName = partName;

            this.expressions = expressions;
        }



        private Configurator buildExpressionConfigurator(
                Supplier<EditorExpressionEntry> getter,
                Consumer<EditorExpressionEntry> setter
        ) {

            var entry = getter.get();

            return entry != null
                    ? entry.createDirectConfigurator()
                    : new Configurator();
        }



        private EditorExpressionEntry addDefaultExpression() {

            return new EditorExpressionEntry();
        }
    }


    public static class EditorExpressionEntry
            implements IConfigurable {


        @Configurable
        public String name;


        @Configurable(subConfigurable = true)
        public EditorExpression expression;


        public EditorExpressionEntry() {

            this.name = "";
            this.expression =
                    new EditorExpression();
        }


        public EditorExpressionEntry(
                String name,
                EditorExpression expression
        ) {

            this.name = name;
            this.expression = expression;
        }
    }


    public static class EditorExpression
            implements IConfigurable {


        @Configurable
        public String actionType;


        @Configurable
        public Vector3f transform;


        @Configurable
        public Vector3f rotation;


        public EditorExpression(
                String actionType,
                Vector3f transform,
                Vector3f rotation
        ) {

            this.actionType = actionType;
            this.transform = transform;
            this.rotation = rotation;
        }


        public EditorExpression() {

            this.actionType = "none";

            this.transform =
                    new Vector3f();

            this.rotation =
                    new Vector3f();
        }


        public static EditorExpression from(
                RagdollExpressions.Expression expression
        ) {

            return new EditorExpression(
                    expression.actionType(),

                    new Vector3f(
                            (float) expression.transform().x,
                            (float) expression.transform().y,
                            (float) expression.transform().z
                    ),

                    new Vector3f(
                            (float) expression.rotation().x,
                            (float) expression.rotation().y,
                            (float) expression.rotation().z
                    )
            );
        }


        public RagdollExpressions.Expression toExpression() {

            return new RagdollExpressions.Expression(

                    actionType,

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