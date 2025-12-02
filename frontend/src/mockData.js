// 模拟数据 - 用于前端调试
// 基于 Agent对话交互UI详细实例 生成

// Step1: 配置需求
export const mockSessionStep1 = {
  sessionId: 'mock-session-001',
  currentStep: 'step1',
  progress: {
    current: 0,
    total: 3,
    message: '开始配置'
  },
  displayData: {
    productSerial: '数据中心服务器',
    totalQuantity: 55,
    specReqItems: [
      'CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核',
      '内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。'
    ],
    configStrategy: '目标价最小优先',
    totalQuantityMemo: '',
    country: '中国'
  }
}

// Step2: 规格解析结果
export const mockSessionStep2Parse = {
  sessionId: 'mock-session-001',
  currentStep: 'step2',
  progress: {
    current: 1,
    total: 3,
    message: '原始规格需求解析为标准规格需求'
  },
  displayData: {
    items: [
      {
        index: 1,
        originalSpec: 'CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核',
        stdSpec: '处理器核心数>=16'
      },
      {
        index: 2,
        originalSpec: '内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展',
        stdSpec: '内存容量>=256'
      }
    ]
  }
}

// Step2.5: 产品选型结果
export const mockSessionStep2Selection = {
  sessionId: 'mock-session-001',
  currentStep: 'step2',
  progress: {
    current: 2,
    total: 3,
    message: '按标准规格需求进行产品选型'
  },
  displayData: {
    selectedProductCode: 'PowerEdge R760xa',
    selectedProductName: 'PowerEdge R760xa',
    candidates: [
      {
        rank: 1,
        productCode: 'PowerEdge R760xa',
        productName: 'PowerEdge R760xa',
        deviationDegree: 100,
        description: ''
      },
      {
        rank: 2,
        productCode: 'PowerEdge R860xa',
        productName: 'PowerEdge R860xa',
        deviationDegree: 80,
        description: '内存负偏离'
      }
    ],
    deviationDetails: [
      {
        index: 1,
        originalSpecReq: 'CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核',
        stdSpecReq: '处理器核心数>=16',
        productSpecValue: '32',
        satisfy: true,
        deviationType: '正偏离'
      },
      {
        index: 2,
        originalSpecReq: '内存：配置≥256GB DDR4 ECC Registered内存',
        stdSpecReq: '内存容量>=256',
        productSpecValue: '256',
        satisfy: true,
        deviationType: '无偏离'
      }
    ]
  }
}

// Step3: 参数配置结果
export const mockSessionStep3 = {
  sessionId: 'mock-session-001',
  currentStep: 'step3',
  progress: {
    current: 3,
    total: 3,
    message: '对选中的产品进行参数配置'
  },
  displayData: {
    productCode: 'PowerEdge R760xa',
    productName: 'PowerEdge R760xa',
    items: [
      {
        index: 1,
        configReq: 'CPU:处理器核心数>=16核',
        parameterName: 'CPU核数',
        parameterCode: 'CPU_CONFIG',
        value: '16核'
      },
      {
        index: 2,
        configReq: '内存:内存容量>=256',
        parameterName: '内存容量',
        parameterCode: 'MEM_CONFIG',
        value: '256G'
      },
      {
        index: 3,
        configReq: '55套',
        parameterName: '主机套数',
        parameterCode: 'QTY',
        value: '55'
      }
    ],
    checkResult: {
      errorCode: 0,
      errorMessage: ''
    }
  }
}

// 模拟SSE事件流 - 按时间顺序推送
// 注意：第一个事件（step1）已经在 createSession 中返回，所以这里从 step2 开始
export const mockSSEEvents = [
  { delay: 1500, data: mockSessionStep2Parse },
  { delay: 2500, data: mockSessionStep2Selection },
  { delay: 3500, data: mockSessionStep3 }
]

// 模拟API服务
export class MockApiService {
  constructor() {
    this.currentStep = 0
  }

  // 模拟创建会话
  async createSession(userInput) {
    return new Promise((resolve) => {
      setTimeout(() => {
        this.currentStep = 0
        resolve({
          data: mockSessionStep1
        })
      }, 500)
    })
  }

  // 模拟SSE事件流
  subscribeSession(sessionId, onMessage) {
    let eventIndex = 0
    let isClosed = false
    const timeouts = []
    
    const sendNextEvent = () => {
      if (isClosed || eventIndex >= mockSSEEvents.length) {
        return
      }
      
      const event = mockSSEEvents[eventIndex]
      const timeout = setTimeout(() => {
        if (!isClosed) {
          onMessage({
            type: 'session-update',
            data: JSON.stringify(event.data)
          })
          eventIndex++
          if (eventIndex < mockSSEEvents.length) {
            sendNextEvent()
          } else {
            // 所有事件发送完成
            isClosed = true
          }
        }
      }, event.delay)
      
      timeouts.push(timeout)
    }

    // 立即发送第一个事件
    sendNextEvent()

    // 返回一个可以关闭的模拟EventSource
    return {
      close: () => {
        isClosed = true
        timeouts.forEach(timeout => clearTimeout(timeout))
        timeouts.length = 0
      },
      addEventListener: (eventName, handler) => {
        // 这个方法在真实 EventSource 中用于添加事件监听器
        // 在模拟环境中，我们已经在 onMessage 中处理了
      },
      onerror: null
    }
  }

  // 模拟获取会话
  async getSession(sessionId) {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 根据当前步骤返回对应的数据
        let sessionData
        if (this.currentStep === 0) {
          sessionData = mockSessionStep1
        } else if (this.currentStep === 1) {
          sessionData = mockSessionStep2Parse
        } else if (this.currentStep === 2) {
          sessionData = mockSessionStep2Selection
        } else {
          sessionData = mockSessionStep3
        }
        resolve({ data: sessionData })
      }, 200)
    })
  }
}

