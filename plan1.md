# Kestra 到 DataFlare 迁移改造计划

## 项目概述

本项目是将开源工作流编排平台 Kestra 迁移到 DataFlare 品牌的改造计划。通过代码分析发现，当前项目已经开始了部分迁移工作，主要体现在UI层面已经添加了DataFlare的logo文件，但大部分代码、配置和文档仍然使用Kestra品牌。

## 当前迁移状态分析

### 已完成的迁移部分
1. **UI Logo资源**：
   - ✅ 已添加 `ui/src/assets/dataflare-logo.svg`
   - ✅ 已添加 `ui/src/assets/dataflare-logo-white.svg`
   - ✅ Logo组件已更新使用DataFlare logo (`ui/src/components/home/Logo.vue`)

2. **页面标题**：
   - ✅ HTML页面标题已更新为"DataFlare" (`ui/index.html`)

3. **部分翻译文件**：
   - ✅ 英文翻译中"kestra"已改为"DataFlare" (`ui/src/translations/en.json`)
   - ✅ 部分feed标题已更新为"What's new at DataFlare"

### 待迁移的主要部分

#### 1. 前端UI层面
- **翻译文件**：多语言翻译文件中仍有大量Kestra引用
- **组件名称**：部分组件仍使用Kestra相关命名
- **文档链接**：指向kestra.io的链接需要更新
- **包名称**：package.json中name仍为"kestra"

#### 2. 后端Java代码
- **包结构**：所有Java包仍使用`io.kestra`命名空间
- **类名和注释**：大量类、方法、注释中包含Kestra引用
- **配置文件**：application.yml等配置文件中的Kestra配置
- **API文档**：OpenAPI文档标题仍为"Kestra"

#### 3. 构建和部署
- **Gradle配置**：build.gradle中的项目信息
- **Docker配置**：docker-compose文件中的镜像名称
- **Maven坐标**：groupId仍为"io.kestra"

#### 4. 文档和元数据
- **README文件**：项目描述仍为Kestra
- **许可证信息**：版权信息需要更新
- **版本信息**：版本提供者等元数据

## 详细迁移计划

### 阶段一：前端UI完整迁移（优先级：高）

#### 1.1 翻译文件更新
**文件范围**：`ui/src/translations/`
- [ ] 更新所有语言翻译文件中的Kestra引用
- [ ] 更新文档链接从kestra.io到dataflare.io
- [ ] 更新产品名称引用

**影响文件**：
- `ui/src/translations/de.json`
- `ui/src/translations/es.json`
- `ui/src/translations/fr.json`
- `ui/src/translations/hi.json`
- `ui/src/translations/it.json`
- `ui/src/translations/ja.json`
- `ui/src/translations/ko.json`
- `ui/src/translations/pl.json`
- `ui/src/translations/pt.json`
- `ui/src/translations/ru.json`
- `ui/src/translations/zh_CN.json`

#### 1.2 包配置更新
**文件范围**：`ui/package.json`
- [ ] 更新项目名称从"kestra"到"dataflare"
- [ ] 更新相关依赖包引用

#### 1.3 组件和样式更新
**文件范围**：`ui/src/components/`
- [ ] 更新组件中的硬编码Kestra引用
- [ ] 更新CSS类名中的kestra前缀
- [ ] 更新组件导入路径中的@kestra-io引用

#### 1.4 配置文件更新
**文件范围**：`ui/`
- [ ] 更新vite.config.js中的配置
- [ ] 更新tsconfig.json中的路径配置
- [ ] 更新其他构建配置文件

### 阶段二：后端Java代码迁移（优先级：中）

#### 2.1 包名重构
**影响范围**：所有Java源码
- [ ] 制定包名迁移策略（io.kestra → io.dataflare）
- [ ] 创建包名重构脚本
- [ ] 分模块执行包名重构

**主要模块**：
- `core/src/main/java/io/kestra/`
- `cli/src/main/java/io/kestra/`
- `webserver/src/main/java/io/kestra/`
- `model/src/main/java/io/kestra/`
- 其他模块

#### 2.2 类名和常量更新
- [ ] 更新类名中的Kestra引用
- [ ] 更新常量值中的kestra字符串
- [ ] 更新注释和文档字符串

#### 2.3 配置文件更新
**文件范围**：
- [ ] `cli/src/main/resources/application.yml`
- [ ] `core/src/test/resources/application-test.yml`
- [ ] `webserver/src/test/resources/application-test.yml`
- [ ] 其他配置文件

#### 2.4 API文档更新
**文件范围**：`webserver/src/main/java/io/kestra/webserver/Application.java`
- [ ] 更新OpenAPI文档标题
- [ ] 更新API描述信息
- [ ] 更新许可证链接

### 阶段三：构建和部署配置迁移（优先级：中）

#### 3.1 Gradle构建配置
**文件范围**：
- [ ] `build.gradle` - 更新项目组织信息
- [ ] `settings.gradle` - 更新项目名称
- [ ] 各模块的`build.gradle`文件

#### 3.2 Docker配置
**文件范围**：
- [ ] `docker-compose.yml`
- [ ] `docker-compose-dind.yml`
- [ ] `docker-compose-ci.yml`
- [ ] `Dockerfile`

#### 3.3 Maven坐标更新
- [ ] 更新groupId从io.kestra到io.dataflare
- [ ] 更新artifactId相关命名
- [ ] 更新发布配置

### 阶段四：文档和元数据迁移（优先级：低）

#### 4.1 项目文档
**文件范围**：
- [ ] `README.md` - 完整重写项目描述
- [ ] `SECURITY.md` - 更新安全政策
- [ ] 其他markdown文档

#### 4.2 许可证和版权
**文件范围**：
- [ ] `LICENSE` - 更新版权持有者
- [ ] 源码文件头部版权信息
- [ ] 第三方许可证声明

#### 4.3 开发工具脚本
**文件范围**：`dev-tools/`
- [ ] 更新发布脚本中的仓库引用
- [ ] 更新插件检查脚本
- [ ] 更新版本标记脚本

### 阶段五：测试和验证（优先级：高）

#### 5.1 功能测试
- [ ] 前端UI功能完整性测试
- [ ] 后端API功能测试
- [ ] 集成测试验证

#### 5.2 构建测试
- [ ] 本地构建测试
- [ ] Docker镜像构建测试
- [ ] 发布流程测试

#### 5.3 兼容性测试
- [ ] 数据库迁移兼容性
- [ ] 插件系统兼容性
- [ ] 配置文件向后兼容性

## 实施策略

### 风险控制
1. **分阶段实施**：按优先级分阶段执行，确保每个阶段完成后系统仍可正常运行
2. **备份策略**：在重大更改前创建代码备份
3. **回滚计划**：为每个阶段准备回滚方案

### 技术考虑
1. **包名重构**：使用IDE的重构功能或编写脚本批量处理
2. **配置管理**：保持配置的向后兼容性，支持渐进式迁移
3. **测试覆盖**：确保迁移过程中不破坏现有功能

### 时间估算
- **阶段一（前端UI）**：2-3周
- **阶段二（后端Java）**：4-6周
- **阶段三（构建部署）**：1-2周
- **阶段四（文档元数据）**：1周
- **阶段五（测试验证）**：2周

**总计**：10-14周

## 注意事项

1. **功能保持不变**：本次迁移仅涉及品牌和命名更改，不涉及功能修改
2. **API兼容性**：确保API接口保持兼容，不影响现有集成
3. **数据迁移**：如需要，准备数据库schema更新脚本
4. **文档同步**：确保所有文档与代码更改保持同步

## 下一步行动

1. 确认迁移范围和优先级
2. 准备开发环境和测试环境
3. 开始阶段一的前端UI迁移工作
4. 建立持续集成和测试流程

## 技术实施细节

### 关键文件清单

#### 高优先级文件（立即需要更新）
1. **前端核心文件**：
   - `ui/package.json` - 项目名称和依赖
   - `ui/src/translations/*.json` - 所有翻译文件
   - `ui/src/main.js` - 应用入口点
   - `ui/index.html` - 页面标题和元数据

2. **后端核心文件**：
   - `webserver/src/main/java/io/kestra/webserver/Application.java` - API文档
   - `cli/src/main/java/io/kestra/cli/App.java` - CLI应用名称
   - `core/src/main/java/io/kestra/core/utils/VersionProvider.java` - 版本信息

3. **构建配置**：
   - `build.gradle` - 项目组织信息
   - `settings.gradle` - 项目名称
   - `gradle.properties` - 版本和配置

#### 中优先级文件（后续更新）
1. **Java包结构**：
   - 所有`io.kestra`包需要重命名为`io.dataflare`
   - 涉及约200+个Java文件

2. **配置文件**：
   - `application.yml`文件中的kestra配置项
   - Docker compose文件中的服务名称
   - 环境变量名称（KESTRA_*）

3. **文档和脚本**：
   - README.md和其他markdown文件
   - dev-tools/目录下的脚本文件
   - Makefile中的变量名

### 自动化脚本建议

#### 1. 翻译文件批量更新脚本
```bash
#!/bin/bash
# 更新所有翻译文件中的kestra引用
find ui/src/translations -name "*.json" -exec sed -i 's/kestra\.io/dataflare.io/g' {} \;
find ui/src/translations -name "*.json" -exec sed -i 's/Kestra/DataFlare/g' {} \;
```

#### 2. Java包名重构脚本
```bash
#!/bin/bash
# 批量重命名Java包
find . -name "*.java" -exec sed -i 's/io\.kestra/io.dataflare/g' {} \;
find . -name "*.java" -exec sed -i 's/package io\.kestra/package io.dataflare/g' {} \;
find . -name "*.java" -exec sed -i 's/import io\.kestra/import io.dataflare/g' {} \;
```

#### 3. 配置文件更新脚本
```bash
#!/bin/bash
# 更新配置文件中的kestra引用
find . -name "application*.yml" -exec sed -i 's/kestra:/dataflare:/g' {} \;
find . -name "*.properties" -exec sed -i 's/kestra\./dataflare./g' {} \;
```

### 数据库迁移考虑

如果系统中存储了包名或类名信息，可能需要数据库迁移脚本：

```sql
-- 示例：更新存储的类名引用
UPDATE flows SET source = REPLACE(source, 'io.kestra.', 'io.dataflare.');
UPDATE templates SET source = REPLACE(source, 'io.kestra.', 'io.dataflare.');
```

### 测试验证清单

#### 功能测试
- [ ] 前端页面正常加载
- [ ] 所有翻译文本正确显示
- [ ] Logo和品牌元素正确显示
- [ ] API接口正常响应
- [ ] 工作流执行功能正常

#### 构建测试
- [ ] Gradle构建成功
- [ ] Docker镜像构建成功
- [ ] 前端资源打包成功
- [ ] 所有测试用例通过

#### 兼容性测试
- [ ] 现有工作流定义仍可正常执行
- [ ] 插件系统正常工作
- [ ] 数据库连接和操作正常
- [ ] 配置文件向后兼容

### 回滚策略

1. **代码回滚**：
   - 使用Git分支管理，每个阶段创建独立分支
   - 保留原始Kestra分支作为备份

2. **数据库回滚**：
   - 在执行数据库迁移前创建完整备份
   - 准备反向迁移脚本

3. **配置回滚**：
   - 保留原始配置文件副本
   - 使用配置管理工具支持快速切换

### 部署策略

1. **蓝绿部署**：
   - 在新环境中部署DataFlare版本
   - 验证功能正常后切换流量

2. **灰度发布**：
   - 逐步将用户流量切换到新版本
   - 监控系统稳定性和性能

3. **监控告警**：
   - 设置关键指标监控
   - 配置异常情况自动告警

---

*本计划将根据实际实施情况进行调整和更新*
