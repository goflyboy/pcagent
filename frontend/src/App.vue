<template>
  <div class="app-container">
    <header class="app-header">
      <h1>产品配置Agent</h1>
      <div class="mode-switch">
        <label class="switch-label">
          <input 
            type="checkbox" 
            v-model="useMockData" 
            @change="onModeChange"
            class="switch-input"
          />
          <span class="switch-text">{{ useMockData ? '模拟数据' : '真实服务' }}</span>
        </label>
      </div>
    </header>
    
    <main class="app-main">
      <div class="chat-container">
        <!-- 任务总进展 -->
        <div class="progress-section" v-if="session && session.progress">
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :style="{ width: (session.progress.current / session.progress.total * 100) + '%' }"
            ></div>
          </div>
          <div class="progress-text">
            步骤 {{ session.progress.current }} / {{ session.progress.total }}: 
            {{ session.progress.message }}
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="messages-container" ref="messagesContainer">
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            :class="['message', message.type]"
          >
            <div class="message-content">
              <div class="message-text">{{ message.text }}</div>
              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-section">
          <input 
            v-model="userInput" 
            @keyup.enter="sendMessage"
            placeholder="请输入配置需求..."
            class="input-field"
            :disabled="loading"
          />
          <button 
            @click="sendMessage" 
            :disabled="loading || !userInput.trim()"
            class="send-button"
          >
            {{ loading ? '处理中...' : '发送' }}
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import axios from 'axios'
import { MockApiService } from './mockData'

export default {
  name: 'App',
  data() {
    return {
      userInput: '',
      messages: [],
      session: null,
      sessionId: null,
      loading: false,
      pollInterval: null,
      eventSource: null,
      useMockData: true, // 默认使用模拟数据，方便调试
      mockApiService: null
    }
  },
  created() {
    this.mockApiService = new MockApiService()
  },
  mounted() {
    this.addSystemMessage('欢迎使用产品配置Agent！请输入您的配置需求。')
    // 设置默认用户输入
    this.userInput = `我有一名高端客户，需要建立数据中心，要求如下：

数据中心服务器 500台

1. CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核 

2. 内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。`
  },
  beforeUnmount() {
    this.cleanupPolling()
    this.cleanupEventSource()
  },
  methods: {
    onModeChange() {
      // 切换模式时清理当前状态
      this.cleanupEventSource()
      this.cleanupPolling()
      this.session = null
      this.sessionId = null
      this.messages = []
      this.addSystemMessage('模式已切换，请重新发送消息')
    },

    async sendMessage() {
      if (!this.userInput.trim() || this.loading) {
        return
      }

      const input = this.userInput.trim()
      this.userInput = ''
      this.addUserMessage(input)
      this.loading = true

      try {
        if (this.useMockData) {
          // 使用模拟数据
          await this.sendMessageWithMock(input)
        } else {
          // 使用真实服务
          await this.sendMessageWithReal(input)
        }
      } catch (error) {
        console.error('Error creating session:', error)
        this.addSystemMessage('错误: ' + (error.response?.data?.message || error.message))
        this.loading = false
      }
    },

    async sendMessageWithMock(input) {
      // 使用模拟数据
      const response = await this.mockApiService.createSession(input)
      
      this.sessionId = response.data.sessionId
      this.session = response.data
      this.updateSessionDisplay()

      // 使用模拟 SSE 订阅会话进度
      this.startMockEventStream()
    },

    async sendMessageWithReal(input) {
      // 使用真实服务
      const response = await axios.post('/api/v1/sessions', {
        user_input: input
      })

      this.sessionId = response.data.sessionId
      this.session = response.data
      this.updateSessionDisplay()

      // 使用 SSE 订阅会话进度
      this.startEventStream()
    },

    // 兼容保留：轮询方式（目前不再默认使用）
    async startPolling() {
      if (!this.sessionId) return

      this.pollInterval = setInterval(async () => {
        try {
          const response = await axios.get(`/api/v1/sessions/${this.sessionId}`)
          this.session = response.data
          this.updateSessionDisplay()

          // 如果完成，停止轮询
          if (this.session.progress && 
              this.session.progress.current >= this.session.progress.total) {
            this.stopPolling()
            this.loading = false
            this.addSystemMessage('配置完成！')
          }
        } catch (error) {
          console.error('Error polling session:', error)
          this.stopPolling()
          this.loading = false
        }
      }, 1000) // 每秒轮询一次
    },

    stopPolling() {
      this.cleanupPolling()
    },

    cleanupPolling() {
      if (this.pollInterval) {
        clearInterval(this.pollInterval)
        this.pollInterval = null
      }
    },

    startMockEventStream() {
      this.cleanupEventSource()
      if (!this.sessionId) return

      // 使用模拟的 EventSource
      const mockEventSource = this.mockApiService.subscribeSession(this.sessionId, (event) => {
        try {
          const session = JSON.parse(event.data)
          this.session = session
          this.updateSessionDisplay()

          if (session.progress &&
              session.progress.current >= session.progress.total) {
            this.loading = false
            this.addSystemMessage('配置完成！')
            this.cleanupEventSource()
          }
        } catch (err) {
          console.error('Error parsing SSE data:', err)
        }
      })

      // 将模拟的 EventSource 存储起来，以便后续清理
      this.eventSource = mockEventSource
    },

    startEventStream() {
      this.cleanupEventSource()
      if (!this.sessionId) return

      const url = `/api/v1/sessions/${this.sessionId}/events`
      this.eventSource = new EventSource(url)

      const handleSessionUpdate = (e) => {
        try {
          const session = JSON.parse(e.data)
          this.session = session
          this.updateSessionDisplay()

          if (session.progress &&
              session.progress.current >= session.progress.total) {
            this.loading = false
            this.addSystemMessage('配置完成！')
            this.cleanupEventSource()
          }
        } catch (err) {
          console.error('Error parsing SSE data:', err)
        }
      }

      // 后端使用 SseEmitter.event().name("session-update") 发送自定义事件名
      // 这里通过 addEventListener 订阅该事件
      this.eventSource.addEventListener('session-update', handleSessionUpdate)

      this.eventSource.onerror = (e) => {
        console.error('SSE connection error:', e)
        this.cleanupEventSource()
        this.loading = false
      }
    },

    cleanupEventSource() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
    },

    updateSessionDisplay() {
      if (!this.session || !this.session.displayData) return

      const step = this.session.currentStep
      const displayData = this.session.displayData

      if (step === 'step1') {
        // 配置需求显示
        const configReq = displayData
        if (configReq.productSerial) {
          this.addSystemMessage(`识别到产品系列: ${configReq.productSerial}`)
        }
        if (configReq.totalQuantity) {
          this.addSystemMessage(`总套数: ${configReq.totalQuantity}${configReq.totalQuantityMemo ? ' (' + configReq.totalQuantityMemo + ')' : ''}`)
        }
        if (configReq.specReqItems && configReq.specReqItems.length > 0) {
          this.addSystemMessage(`规格需求项:`)
          configReq.specReqItems.forEach((item, index) => {
            this.addSystemMessage(`  ${index + 1}. ${item}`)
          })
        }
        if (configReq.configStrategy) {
          this.addSystemMessage(`配置策略: ${configReq.configStrategy}`)
        }
      } else if (step === 'step2') {
        // 规格解析结果或产品选型结果
        if (displayData.items && Array.isArray(displayData.items)) {
          // 规格解析结果
          this.addSystemMessage(`完成原始规格需求解析为标准规格，需求结果如下:`)
          const tableRows = displayData.items.map(item => 
            `| ${item.index} | ${item.originalSpec} | ${item.stdSpec} |`
          ).join('\n')
          this.addSystemMessage(`\n| 序号 | 原始规格需求 | 标准规格需求 |\n| :- | :--- | :--- |\n${tableRows}`)
        } else if (displayData.selectedProductCode) {
          // 产品选型结果
          this.addSystemMessage(`选型结果如下，按照匹配度对候选产品进行排序，选择产品${displayData.selectedProductName || displayData.selectedProductCode}，Top3的排序如下:`)
          
          if (displayData.candidates && displayData.candidates.length > 0) {
            const tableRows = displayData.candidates.map(item => 
              `| ${item.rank} | **${item.productName || item.productCode}** | ${item.deviationDegree}% | ${item.description || ''} |`
            ).join('\n')
            this.addSystemMessage(`\n| 排序 | 产品名称 | 偏离度 | 说明 |\n| :-- | :--- | :--- | :---- |\n${tableRows}`)
          }

          if (displayData.deviationDetails && displayData.deviationDetails.length > 0) {
            this.addSystemMessage(`\n正在做**${displayData.selectedProductName || displayData.selectedProductCode}的规格偏离度计算，偏离度为${displayData.candidates && displayData.candidates.length > 0 ? displayData.candidates[0].deviationDegree : 100}%，详细如下：**`)
            const tableRows = displayData.deviationDetails.map(item => 
              `| ${item.index} | ${item.originalSpecReq} | ${item.stdSpecReq} | ${item.productSpecValue} | ${item.satisfy ? 'Y' : 'N'} | ${item.deviationType} |`
            ).join('\n')
            this.addSystemMessage(`\n| 序号 | 原始规格需求 | 标准规格需求 | 本产品规格值 | 是否满足 | 偏离情况 |\n| :- | :--- | :--- | :----- | :--- | :--- |\n${tableRows}`)
          }
        }
      } else if (step === 'step3') {
        // 参数配置结果
        const paramConfig = displayData
        if (paramConfig.productCode) {
          this.addSystemMessage(`现在开始对选中的产品进行参数配置`)
          this.addSystemMessage(`产品: ${paramConfig.productName || paramConfig.productCode}`)
          
          if (paramConfig.items && paramConfig.items.length > 0) {
            this.addSystemMessage(`参数的配置结果如下：`)
            const tableRows = paramConfig.items.map(item => 
              `| ${item.index} | ${item.configReq || ''} | ${item.parameterName || item.parameterCode} | ${item.value} |`
            ).join('\n')
            this.addSystemMessage(`\n| 排序 | 配置需求 | 参数 | 参数值(配置结果) |\n| :- | :--- | :---- | :-------- |\n${tableRows}`)
          }

          if (paramConfig.checkResult) {
            if (paramConfig.checkResult.errorCode === 0) {
              this.addSystemMessage(`完成校验检查，没有错误，符合要求`)
            } else {
              this.addSystemMessage(`校验检查发现错误: ${paramConfig.checkResult.errorMessage || '未知错误'}`)
            }
          }
        }
      }
    },

    addUserMessage(text) {
      this.messages.push({
        type: 'user',
        text: text,
        time: new Date().toLocaleTimeString()
      })
      this.scrollToBottom()
    },

    addSystemMessage(text) {
      this.messages.push({
        type: 'system',
        text: text,
        time: new Date().toLocaleTimeString()
      })
      this.scrollToBottom()
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f5f5;
}

.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.app-header {
  background-color: #2c3e50;
  color: white;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-header h1 {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
}

.mode-switch {
  display: flex;
  align-items: center;
}

.switch-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.switch-input {
  margin-right: 0.5rem;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.switch-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
}

.app-main {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2rem;
}

.chat-container {
  width: 100%;
  max-width: 800px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
}

.progress-section {
  padding: 1rem;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-fill {
  height: 100%;
  background-color: #007bff;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.875rem;
  color: #6c757d;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  display: flex;
  margin-bottom: 1rem;
}

.message.user {
  justify-content: flex-end;
}

.message.system {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  word-wrap: break-word;
}

.message.user .message-content {
  background-color: #007bff;
  color: white;
}

.message.system .message-content {
  background-color: #f1f3f5;
  color: #212529;
}

.message-text {
  margin-bottom: 0.25rem;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
}

.input-section {
  display: flex;
  padding: 1rem;
  border-top: 1px solid #e9ecef;
  gap: 0.5rem;
}

.input-field {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 1rem;
}

.input-field:focus {
  outline: none;
  border-color: #007bff;
}

.send-button {
  padding: 0.75rem 1.5rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
}

.send-button:hover:not(:disabled) {
  background-color: #0056b3;
}

.send-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}
</style>

