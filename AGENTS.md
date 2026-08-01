# Repository Guidelines

## 项目简介

Sable: RagdollLib 是一个基于 NeoForge 的布娃娃库（Minecraft 1.21.1），允许通过代码和数据包快速创建布娃娃，目前仅被作者的其他模组使用。注意：虽然名称与 Sable: Ragdolls 相似，但两者实现原理完全不同，本库未使用其任何代码。

## 项目结构与模块组织

- `src/main/java/com/gly091020/SableRagdollLib/`：Java 源码，按功能分包，如 `api/`（对外 API）、`editor/`（布娃娃编辑器）、`client/`、`entity/`、`block/`、`mixin/`、`command/`、`common/`、`resource/`、`test/`（开发调试用）。
- `src/main/resources/`：资源文件，`assets/sableragdolllib/` 存放语言与贴图，`data/sableragdolllib/ragdoll/` 存放布娃娃数据定义。
- `src/main/templates/`：模组元数据模板，由 `generateModMetadata` 构建时展开。
- `src/generated/resources/`：数据生成器输出，勿手动编辑。
- `build.gradle`、`gradle.properties`：构建脚本与版本配置（`mod_version`、`minecraft_version` 等）。

## 构建、测试与开发命令

- `.\gradlew runClient`：启动客户端调试环境。
- `.\gradlew runServer`：启动无界面专用服务器。
- `.\gradlew runData`：运行数据生成器，输出到 `src/generated/resources/`。
- `.\gradlew runGameTestServer`：启动 GameTestServer 运行注册的游戏测试。
- `.\gradlew build`：构建模组 jar 至 `build/libs/`。
- `.\gradlew sourcesJar`：生成源码 jar。

需要 Java 21 工具链；Minecraft、NeoForge 及依赖版本均在 `gradle.properties` 中维护。

## 编码风格与命名

- Java，4 空格缩进，遵循 NeoForge/Minecraft 惯例，无自动格式化工具。
- 类名使用 PascalCase，方法/字段使用 camelCase，常量使用 SCREAMING_SNAKE。
- 包名为 `com.gly091020.SableRagdollLib`；mod id 为小写 `sableragdolllib`，资源路径与其对应。
- 改动应与周边代码风格保持一致。

## 测试指南

- 无 JUnit 单元测试；使用 NeoForge GameTest，通过 `.\gradlew runGameTestServer` 运行，命名空间为 `sableragdolllib`。
- 测试代码位于 `com.gly091020.SableRagdollLib.test`，对应数据位于 `data/sableragdolllib/ragdoll/test/`。

## 提交与 Pull Request 指南

- 提交信息遵循仓库历史风格：简短的中文动宾短语，直接描述变更，如“添加API”“bug修复”“更改版本号”。
- PR 需说明改动内容与动机；涉及 API 变更时需说明影响范围；有关联问题时请链接 issue。
- 变更版本号时同步更新 `gradle.properties` 中的 `mod_version`。

## 安全与配置提示

- `run/` 为本地运行目录，已在 `.gitignore` 中忽略，请勿提交。
- 项目采用 LGPL-3.0 许可证，贡献代码即表示同意在该许可证下分发。
