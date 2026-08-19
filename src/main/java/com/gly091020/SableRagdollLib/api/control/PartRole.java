package com.gly091020.SableRagdollLib.api.control;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * 布娃娃六部位的角色。
 * <p>
 * 识别结果用于通用控制（如木偶模式）时按角色定位部件。
 */
public enum PartRole implements StringRepresentable {
    HEAD,
    BODY,
    LEFT_ARM,
    RIGHT_ARM,
    LEFT_LEG,
    RIGHT_LEG;

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static final Codec<PartRole> CODEC = StringRepresentable.fromEnum(PartRole::values);
    public static final StreamCodec<ByteBuf, PartRole> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}