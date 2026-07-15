package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;

public record RagdollExpressions(Map<String, Map<String, Expression>> expressions) {
    public static final Codec<RagdollExpressions> CODEC = Codec.unboundedMap(
            Codec.STRING,
            Codec.unboundedMap(Codec.STRING, Expression.CODEC)
    ).xmap(
            RagdollExpressions::new,
            RagdollExpressions::expressions
    );
    public record Expression(String actionType, Vec3 transform, Vec3 rotation){
        public static final Codec<Expression> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("actionType", "none").forGetter(Expression::actionType),
                Vec3.CODEC.optionalFieldOf("transform", Vec3.ZERO).forGetter(Expression::transform),
                Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter(Expression::rotation)
        ).apply(i, Expression::new));
    }

    public Optional<Map<String, Expression>> getExpression(String partName){
        return Optional.ofNullable(expressions.get(partName));
    }

    public static final RagdollExpressions EMPTY = new RagdollExpressions(Map.of());
}
