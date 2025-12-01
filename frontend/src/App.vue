<template>
  <div class="app-container">
    <header class="app-header">
      <h1>产品配置Agent</h1>
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
      eventSource: null
    }
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
    async sendMessage() {
      if (!this.userInput.trim() || this.loading) {
        return
      }

      const input = this.userInput.trim()
      this.userInput = ''
      this.addUserMessage(input)
      this.loading = true

      try {
        // 创建会话
        const response = await axios.post('/api/v1/sessions', {
          user_input: input
        })

        this.sessionId = response.data.sessionId
        this.session = response.data
        this.updateSessionDisplay()

        // 使用 SSE 订阅会话进度
        this.startEventStream()
      } catch (error) {
        console.error('Error creating session:', error)
        this.addSystemMessage('错误: ' + (error.response?.data?.message || error.message))
        this.loading = false
      }
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

    startEventStream() {
      this.cleanupEventSource()
      if (!this.sessionId) return

      const url = `/api/v1/sessions/${this.sessionId}/events`
      this.eventSource = new EventSource(url)

      this.eventSource.onmessage = (e) => {
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
      if (!this.session || !this.session.data) return

      // 根据当前步骤显示不同的信息
      const step = this.session.currentStep
      const data = this.session.data

      if (step === 'step1' && data.productSerial) {
        this.addSystemMessage(`识别到产品系列: ${data.productSerial}, 总套数: ${data.totalQuantity}`)
      } else if (step === 'step2' && Array.isArray(data)) {
        this.addSystemMessage(`已解析 ${data.length} 个产品规格需求`)
      } else if (step === 'step3' && data.productCode) {
        this.addSystemMessage(`已选择产品: ${data.productCode}, 配置完成`)
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

.app-header h1 {
  font-size: 1.5rem;
  font-weight: 600;
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

