package com.gly091020.SableRagdollLib.api.control;

import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;
import com.gly091020.SableRagdollLib.resource.file.RagdollJoints;
import com.gly091020.SableRagdollLib.resource.file.RagdollPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 库内置的通用识别器，不依赖具体模型命名，供任意布娃娃兜底使用。
 * <p>
 * 顺序：mainBody 定身体 -> 名称规则定头/四肢 -> 关节连接最多的部件定身体兜底 ->
 * 高度与横向偏移定四肢兜底。真实模型通常由名称规则直接命中，
 * 几何兜底只保证"任何布娃娃都能识别出尽量多的部位"。
 */
public class DefaultRagdollPartRecognizer implements IRagdollPartRecognizer {
    @Override
    public Map<PartRole, String> recognize(RagdollDefFile defFile, Map<PartRole, String> current) {
        Map<PartRole, String> result = new HashMap<>(current);

        // 1. mainBody 权威定义身体
        defFile.mainBody().ifPresent(name -> result.putIfAbsent(PartRole.BODY, name));

        List<String> parts = candidateParts(defFile);

        // 2. 名称规则
        for (String part : parts) {
            if (result.containsValue(part)) {
                continue;
            }
            PartRole role = RagdollPartNameRules.match(part);
            if (role != null) {
                result.putIfAbsent(role, part);
            }
        }

        // 3. 兜底：关节连接最多的部件作为身体（人体模型的连接枢纽）
        if (!result.containsKey(PartRole.BODY)) {
            String hub = mostConnected(defFile);
            if (hub != null) {
                result.put(PartRole.BODY, hub);
            }
        }

        // 4. 兜底：按高度与横向偏移补剩余部位
        fillByPosition(defFile, parts, result);
        return result;
    }

    private static List<String> candidateParts(RagdollDefFile defFile) {
        if (!defFile.allParts().isEmpty()) {
            return new ArrayList<>(defFile.allParts());
        }
        return new ArrayList<>(defFile.position().position().keySet());
    }

    private static String mostConnected(RagdollDefFile defFile) {
        Map<String, Integer> counts = new HashMap<>();
        for (RagdollJoints.JointData joint : defFile.joints().jointData()) {
            counts.merge(joint.a(), 1, Integer::sum);
            counts.merge(joint.b(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static void fillByPosition(RagdollDefFile defFile, List<String> parts, Map<PartRole, String> result) {
        Map<String, RagdollPosition.PartSetting> positions = defFile.position().position();
        Set<String> assigned = new HashSet<>(result.values());
        List<String> rest = new ArrayList<>();
        for (String part : parts) {
            if (!assigned.contains(part) && positions.containsKey(part)) {
                rest.add(part);
            }
        }
        if (rest.isEmpty()) {
            return;
        }

        // 头：y 最高的剩余部件
        if (!result.containsKey(PartRole.HEAD)) {
            rest.sort(Comparator.comparingDouble((String p) -> positions.get(p).transform().y).reversed());
            String head = rest.removeFirst();
            result.put(PartRole.HEAD, head);
            assigned.add(head);
        }

        String body = result.get(PartRole.BODY);
        if (body == null || !positions.containsKey(body)) {
            return;
        }
        double bodyY = positions.get(body).transform().y;
        List<String> upper = new ArrayList<>();
        List<String> lower = new ArrayList<>();
        for (String part : rest) {
            if (assigned.contains(part)) {
                continue;
            }
            double y = positions.get(part).transform().y;
            if (y >= bodyY - 3.0) {
                upper.add(part);
            } else {
                lower.add(part);
            }
        }
        if (!result.containsKey(PartRole.LEFT_ARM) || !result.containsKey(PartRole.RIGHT_ARM)) {
            fillPair(result, pickLateral(upper, positions, 2), positions, PartRole.LEFT_ARM, PartRole.RIGHT_ARM);
        }
        if (!result.containsKey(PartRole.LEFT_LEG) || !result.containsKey(PartRole.RIGHT_LEG)) {
            fillPair(result, pickLateral(lower, positions, 2), positions, PartRole.LEFT_LEG, PartRole.RIGHT_LEG);
        }
    }

    /** 取横向偏移最大的几个部件作为左右成对部位 */
    private static List<String> pickLateral(List<String> candidates, Map<String, RagdollPosition.PartSetting> positions, int count) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble((String p) -> Math.abs(positions.get(p).transform().x)).reversed())
                .limit(count)
                .toList();
    }

    /** 成对部位按 x 符号分左右，x 为正视为左侧（部分模型方向相反，属兜底推断） */
    private static void fillPair(Map<PartRole, String> result, List<String> pair,
                                 Map<String, RagdollPosition.PartSetting> positions,
                                 PartRole left, PartRole right) {
        if (pair.isEmpty()) {
            return;
        }
        String first = pair.get(0);
        if (!result.containsKey(left) && !result.containsKey(right)) {
            result.put(positions.get(first).transform().x >= 0 ? left : right, first);
        }
        if (pair.size() < 2) {
            return;
        }
        String second = pair.get(1);
        result.putIfAbsent(result.containsValue(second) ? (result.containsKey(left) ? right : left)
                : (positions.get(second).transform().x >= 0 ? left : right), second);
    }
}