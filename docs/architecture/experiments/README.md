# T-0002 Maven API 隔离实验

用途：验证架构评审中的具体 API 假设。这里没有 RepoOnboard 产品分析器、根构建配置或可发布程序；不要运行这些 POM 的 Maven lifecycle。

环境与实测：2026-09-08，Windows 11，Java/Javac 21.0.6，Apache Maven 3.6.1 分发包。Java 源文件启动模式使用该分发包的 lib，不安装依赖、不调用 mvn、不使用网络解析器。Maven 3 模型接口产生 4 条 deprecation 警告，运行退出码为 0；这只验证了 Maven 3 API 的行为假设，不代表推荐的 Maven 3.9 依赖版本或 Maven 4 API 已通过验证。

从仓库根目录复现，将占位路径替换成已有 Maven 3.9.12 解压目录：

```text
java --class-path "<maven-3.6.1-directory>/lib/*" docs/architecture/experiments/T0002MavenProbe.java docs/architecture/experiments/maven-probe
```

8 项断言全部 PASS：

1. 本地 POM 属性可以插值。
2. 缺失 parent 会产生模型诊断，原始 POM 仍能独立读取。
3. request properties 为空时，默认 selector 仍会激活宿主 OS profile（反例确认）。
4. 不注册隐式 activator 的 selector 保留 activeByDefault 行为。
5. 同一受限 selector 支持显式 profile。
6. 默认本地 relativePath 查找绕过 ModelResolver（反例确认）。
7. 有界 ModelSource2 拒绝扫描根外 parent，转为缺失模型诊断。
8. StAX 预检接受普通 XML、拒绝 DTD 声明。

第 6 项把 `maven-probe/child` 当作扫描根，受控父 POM 位于其外一层，但仍完全处于本仓库实验目录中；不访问任何第三方仓库。第 7 项用相同输入验证边界修正。

局限：未实现完整本地坐标 resolver、BOM/profile 组合、符号链接竞态防御、输入资源上限或跨编码读取层；DTD 检查针对内存文本，不等同于生产级 XML 安全验证。实验未证明所有输入绝无网络访问，仅证明此调用路径用拒绝型 resolver 且不执行 lifecycle；正式实现仍需完整离线集成测试。M1/M2 必须用最终锁定的 Maven 3.9 依赖版本重跑等价测试。实验不得替代 M2 的回归测试。
