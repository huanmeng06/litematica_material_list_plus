# Release 检查表

- [ ] 使用新的版本号；没有覆盖既有 Tag 或 Release。
- [ ] `versions.toml` 中的支持版本和依赖正确。
- [ ] 七个版本分支工作区干净，并与 GitHub 同步。
- [ ] `./tools/lmlp regression-check` 全部通过。
- [ ] `./tools/lmlp release-prepare <版本>` 全部构建成功。
- [ ] `release-manifest.json` 中每个构建的 `gitDirty` 都为 `false`。
- [ ] SHA256SUMS.txt 包含全部正式 JAR。
- [ ] 各实例中只有一个有效 LMLP JAR。
- [ ] 完成对应版本的游戏内 UI、光束、HUD、热键、JEI 和性能检查。
- [ ] Release description 明确支持版本和不支持版本。
- [ ] 最后才执行 `release-publish`。
