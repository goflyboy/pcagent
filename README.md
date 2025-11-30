# 产品配置Agent系统

## 项目简介

这是一个基于Spring Boot和Vue的产品配置Agent系统，支持根据用户需求自动识别配置需求、解析产品规格、进行产品选型和参数配置。

## 技术栈

### 后端
- Java 21
- Spring Boot 3.2.0
- Spring AI 1.0.0-M4 (支持OpenAI、DeepSeek、Qinwen)
- Maven

### 前端
- Vue 3
- Vite
- Axios

## 项目结构

```
pcagent/
├── src/main/java/com/pcagent/
│   ├── model/          # 数据模型
│   ├── service/        # 业务服务
│   ├── controller/     # 控制器
│   ├── util/           # 工具类
│   └── exception/      # 异常类
├── src/main/resources/
│   ├── data/           # 样例数据JSON文件
│   └── application.yml # 配置文件
├── frontend/           # Vue前端
└── pom.xml            # Maven配置
```

## 快速开始

### 1. 后端启动

1. 配置Spring AI（可选，如果不配置将使用简单解析）：
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPENAI_API_KEY}
   ```

2. 编译和运行：
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. 后端服务将在 http://localhost:8080 启动

### 2. 前端启动

1. 进入前端目录：
   ```bash
   cd frontend
   ```

2. 安装依赖：
   ```bash
   npm install
   ```

3. 启动开发服务器：
   ```bash
   npm run dev
   ```

4. 前端将在 http://localhost:3000 启动

## API接口

### 创建会话
```
POST /api/v1/sessions
Content-Type: application/json

{
  "user_input": "数据中心服务器 10套，处理器核心数≥16核，内存≥256GB"
}
```

### 获取会话状态
```
GET /api/v1/sessions/{session_id}
```

## 样例数据

系统包含以下样例数据：
- 企业市场
  - 服务器
    - 数据中心服务器（PowerEdge R760xa, R860xa, T860xa）
    - 家庭服务器（Dell 16 Plus, Dell 17 Plus）
  - ONU（OptiXstar P813E-E, P813L, P813W）

## 功能特性

1. **配置需求识别**：使用LLM解析用户输入，识别产品系列、总套数、规格需求等
2. **规格解析**：将原始规格需求解析为标准规格需求
3. **产品选型**：根据规格需求计算产品偏离度，选择最合适的产品
4. **参数配置**：对选中的产品进行参数自动配置
5. **会话管理**：支持会话创建、查询、继续、确认、销毁等操作

## 开发说明

### 添加新的产品数据

编辑 `src/main/resources/data/product-onto-data.json` 文件，添加新的产品、规格和参数信息。

### 配置LLM提供商

在 `application.yml` 中配置相应的API密钥：
- OpenAI: `spring.ai.openai.api-key`
- DeepSeek: `spring.ai.deepseek.api-key` (需要添加相应依赖)
- 通义千问: `spring.ai.qinwen.api-key` (需要添加相应依赖)

## 注意事项

1. 如果未配置LLM API，系统将使用简单的规则解析，功能会受限
2. 样例数据存储在JSON文件中，生产环境建议使用数据库
3. Spring AI 1.0.0-M4 是里程碑版本，生产环境建议等待正式版本

## 许可证

MIT License

