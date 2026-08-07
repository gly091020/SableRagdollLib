package com.gly091020.SableRagdollLib.api.control;

import java.util.Locale;

/**
 * 基于部件名称的部位识别规则：名称全部转小写后按关键字匹配。
 * 例：同时包含 "left" 与 "arm" 即识别为左手。
 */
public final class RagdollPartNameRules {
    private RagdollPartNameRules() {
    }

    /**
     * 按名称识别部件角色，无法判断时返回 null。
     */
    public static PartRole match(String partName) {
        String name = partName.toLowerCase(Locale.ROOT);
        if (name.contains("head")) {
            return PartRole.HEAD;
        }
        if (name.contains("body") || name.contains("torso") || name.contains("chest")) {
            return PartRole.BODY;
        }
        if (containsArm(name)) {
            if (isLeft(name) || name.contains("arml") || name.endsWith("larm")) {
                return PartRole.LEFT_ARM;
            }
            if (isRight(name) || name.contains("armr") || name.endsWith("rarm")) {
                return PartRole.RIGHT_ARM;
            }
        }
        if (containsLeg(name)) {
            if (isLeft(name) || name.contains("legl") || name.endsWith("lleg")) {
                return PartRole.LEFT_LEG;
            }
            if (isRight(name) || name.contains("legr") || name.endsWith("rleg")) {
                return PartRole.RIGHT_LEG;
            }
        }
        return null;
    }

    private static boolean containsArm(String name) {
        return (name.contains("arm") && !name.contains("armor") && !name.contains("armour"))
                || name.contains("hand");
    }

    private static boolean containsLeg(String name) {
        return name.contains("leg") || name.contains("foot");
    }

    private static boolean isLeft(String name) {
        return name.contains("left") || name.startsWith("l_");
    }

    private static boolean isRight(String name) {
        return name.contains("right") || name.startsWith("r_");
    }
}