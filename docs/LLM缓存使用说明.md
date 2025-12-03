# LLM 缓存使用说明

## 概述

LLM 缓存功能用于在开发过程中缓存大模型的输入和输出，减少等待时间，提高开发效率。

## 功能特性

1. **本地文件缓存**：缓存存储在项目根目录下的 `.llm-cache` 目录中
2. **自动缓存管理**：使用 prompt 的 SHA-256 hash 作为文件名，避免文件名过长
3. **缓存验证**：缓存时会验证 prompt 是否匹配，防止 hash 冲突
4. **默认禁用**：默认情况下不启用缓存，需要显式启用

## 使用方式

### 1. 通过系统属性启用（推荐用于测试）

在运行测试时，通过系统属性启用缓存：

```bash
# Maven
mvn test -Dllm.cache.enabled=true

 
System.setProperty("llm.cache.enabled", "true");
```

### 2. 通过环境变量启用

```bash
export LLM_CACHE_ENABLED=true
mvn test
```

### 3. 通过注解启用（推荐用于单元测试）

在测试类或测试方法上使用 `@EnableLLMCache` 注解：
  注意：不要在测试类setUp这里设置 llm.cache.enabled，应该使用 @EnableLLMCache 注解
   LLMCacheTestExtension 会在 beforeEach 中根据注解设置缓存状态

```java
import com.pcagent.util.EnableLLMCache;
import com.pcagent.util.LLMCacheTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LLMCacheTestExtension.class)
@EnableLLMCache
class MyTest {
    @Test
    void testWithCache() {
        // 这个测试会使用缓存
        // 如果缓存存在，直接使用缓存
        // 如果缓存不存在，调用大模型并保存到缓存
    }
    
    @Test
    @EnableLLMCache(false)  // 方法级别禁用缓存
    void testWithoutCache() {
        // 这个测试不使用缓存
    }
}
```

## 缓存行为

### 默认行为（缓存未启用）

- 每次调用都会请求大模型
- 不会读取或写入缓存

### 启用缓存后的行为

1. **首次调用**：
   - 检查缓存是否存在
   - 如果不存在，调用大模型
   - 将响应保存到缓存

2. **后续调用**：
   - 检查缓存是否存在
   - 如果存在，直接返回缓存内容
   - 如果不存在，调用大模型并保存到缓存

## 缓存文件结构

```
项目根目录/
  └── .llm-cache/
      ├── a1b2c3d4e5f6...json  (prompt hash)
      ├── f6e5d4c3b2a1...json
      └── ...
```

每个缓存文件包含：
```json
{
  "prompt": "原始 prompt 内容",
  "response": "LLM 响应内容",
  "timestamp": 1234567890123
}
```

## 清除缓存

### 手动清除

删除 `.llm-cache` 目录下的所有文件，或使用代码：

```java
LLMCache.clear();
```

### 自动清除

缓存文件不会自动过期，需要手动管理。建议在以下情况清除缓存：

- 修改了 prompt 模板
- 更新了大模型版本
- 需要重新测试大模型响应

## 注意事项

1. **缓存目录**：`.llm-cache` 目录应该添加到 `.gitignore` 中，避免提交到版本控制
2. **缓存一致性**：如果修改了 prompt 模板或大模型配置，应该清除相关缓存
3. **测试隔离**：不同测试之间的缓存是共享的，注意测试之间的影响
4. **性能考虑**：缓存可以显著提高测试速度，但要注意缓存的有效性

## 示例

### 示例 1：在系统测试中使用缓存

```java
@ExtendWith(LLMCacheTestExtension.class)
@EnableLLMCache
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class LLMInvokerSysTest {
    @Test
    void shouldUseCacheWhenAvailable() {
        // 第一次运行：调用大模型并缓存
        // 第二次运行：使用缓存，不调用大模型
        String response = llmInvoker.callLLM("test prompt");
    }
}
```

### 示例 2：在测试方法中动态启用缓存

```java
class MyTest {
    @Test
    void testWithDynamicCache() {
        // 启用缓存
        System.setProperty("llm.cache.enabled", "true");
        try {
            String response = llmInvoker.callLLM("test prompt");
        } finally {
            // 恢复设置
            System.clearProperty("llm.cache.enabled");
        }
    }
}
```

## 故障排除

### 缓存不生效

1. 检查是否启用了缓存：`System.getProperty("llm.cache.enabled")` 应该返回 `"true"`
2. 检查缓存目录是否存在：`.llm-cache` 目录应该存在
3. 检查日志：查看是否有缓存命中的日志

### 缓存文件过多

定期清理 `.llm-cache` 目录，删除不需要的缓存文件。

### 缓存内容过期

如果怀疑缓存内容过期，可以：
1. 删除特定的缓存文件
2. 调用 `LLMCache.clear()` 清除所有缓存
3. 重新运行测试以生成新的缓存

