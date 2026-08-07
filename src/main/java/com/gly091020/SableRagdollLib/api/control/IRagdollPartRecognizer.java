package com.gly091020.SableRagdollLib.api.control;

import com.gly091020.SableRagdollLib.resource.file.RagdollDefFile;

import java.util.Map;

/**
 * 布娃娃部位识别器。
 * <p>
 * 输入当前已识别结果，返回补充或覆盖后的新结果。识别器按注册顺序依次执行，
 * 后注册的识别器可以覆盖先前的识别结果，附属模组可借此注册自定义规则
 * 修正通用识别器的推断。
 */
@FunctionalInterface
public interface IRagdollPartRecognizer {
    Map<PartRole, String> recognize(RagdollDefFile defFile, Map<PartRole, String> current);
}