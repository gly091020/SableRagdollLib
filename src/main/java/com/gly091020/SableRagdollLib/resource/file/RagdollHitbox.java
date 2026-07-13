package com.gly091020.SableRagdollLib.resource.file;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public record RagdollHitbox(Map<String, PartBox> hitbox) {
    public static final Codec<RagdollHitbox> CODEC = Codec.unboundedMap(
            Codec.STRING,
            PartBox.CODEC
    ).xmap(
            RagdollHitbox::new,
            RagdollHitbox::hitbox
    );

    public record PartBox(List<Box> boxes){
        public static final Codec<PartBox> CODEC = Codec.either(
            Box.CODEC.comapFlatMap(
                    box -> DataResult.success(new PartBox(List.of(box))),
                    partBox -> {
                        if(partBox.boxes.isEmpty())throw new RuntimeException("partBox不能为空");
                        return partBox.boxes.getFirst();
                    }
            ), Box.CODEC.listOf()
        ).xmap(
                either -> either.map(
                        box -> box,
                        PartBox::new
                ),
                partBox -> {
                    if (partBox.boxes().size() == 1) {
                        return Either.left(partBox);
                    } else {
                        return Either.right(partBox.boxes());
                    }
                }
        );

        public VoxelShape toVoxelShape() {
            VoxelShape shape = Shapes.empty();
            for (Box box : boxes()) {
                shape = Shapes.or(
                        shape,
                        Shapes.box(
                                box.minX(), box.minY(), box.minZ(),
                                box.maxX(), box.maxY(), box.maxZ()
                        )
                );
            }
            if(shape.isEmpty())return Shapes.block();
            return shape;
        }
    }

    public record Box(float minX, float minY, float minZ,
                      float maxX, float maxY, float maxZ) {

        public static final Codec<Box> CODEC =
                Codec.INT_STREAM.comapFlatMap(stream -> {
                    int[] arr = stream.toArray();

                    if (arr.length == 3) {
                        return DataResult.success(
                                Box.fromSize(arr[0], arr[1], arr[2])
                        );
                    } else if (arr.length == 6) {
                        return DataResult.success(new Box(
                                arr[0] / 16f, arr[1] / 16f, arr[2] / 16f,
                                arr[3] / 16f, arr[4] / 16f, arr[5] / 16f
                        ));
                    } else {
                        return DataResult.error(() ->
                                "Box requires 3 (size) or 6 (min/max) integers, got: " + arr.length
                        );
                    }
                }, box -> IntStream.of(
                        (int) (box.minX * 16f),
                        (int) (box.minY * 16f),
                        (int) (box.minZ * 16f),
                        (int) (box.maxX * 16f),
                        (int) (box.maxY * 16f),
                        (int) (box.maxZ * 16f)
                )).stable();

        public static Box fromSize(float xSize, float ySize, float zSize) {
            return new Box(
                    (8 - xSize / 2f) / 16f,
                    (8 - ySize / 2f) / 16f,
                    (8 - zSize / 2f) / 16f,
                    (8 + xSize / 2f) / 16f,
                    (8 + ySize / 2f) / 16f,
                    (8 + zSize / 2f) / 16f
            );
        }
    }
}
