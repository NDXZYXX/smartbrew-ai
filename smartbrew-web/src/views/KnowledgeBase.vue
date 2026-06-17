<template>
  <div class="knowledge-chat">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <h3 class="chat-title">发酵知识库</h3>
      <div class="chat-header-right">
        <el-tag v-if="lastModel" size="small" effect="plain" type="info">模型: {{ lastModel }}</el-tag>
        <el-button size="small" text @click="clearChat">清空对话</el-button>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-body" ref="chatBodyRef">
      <!-- 初始快捷问题 -->
      <div v-if="messages.length === 0 && !loading" class="quick-questions">
        <p class="quick-title">试试问我这些问题：</p>
        <div class="quick-cards">
          <div
            class="quick-card"
            v-for="q in quickQuestions"
            :key="q"
            @click="sendMessage(q)"
          >
            {{ q }}
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
        <div class="message-bubble" :class="msg.role">
          <div class="message-avatar">
            {{ msg.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="message-content">{{ msg.content }}</div>
          <div v-if="msg.model" class="message-meta">{{ msg.model }} · {{ msg.responseTimeMs }}ms</div>
        </div>
      </div>

      <!-- 加载动画 -->
      <div v-if="loading" class="message-row assistant">
        <div class="message-bubble assistant">
          <div class="message-avatar">AI</div>
          <div class="message-content thinking">
            <span class="dot-pulse"></span> 正在思考...
          </div>
        </div>
      </div>

      <!-- 错误消息 -->
      <div v-if="errorMsg" class="error-tip">
        <el-alert :title="errorMsg" type="error" :closable="true" @close="errorMsg = ''" />
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="chat-footer">
      <div class="input-row">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入你的发酵问题..."
          :disabled="loading"
          @keydown="handleKeydown"
        />
        <el-button
          type="primary"
          :disabled="!inputText.trim() || loading"
          :loading="loading"
          @click="sendMessage(inputText)"
        >
          发送
        </el-button>
      </div>
      <div class="input-hint">Enter 发送 · Shift+Enter 换行</div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { askKnowledge } from '../api'
import { ElMessage } from 'element-plus'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const errorMsg = ref('')
const lastModel = ref('')
const chatBodyRef = ref(null)

const quickQuestions = [
  '苹果酒发酵变慢了怎么办？',
  '米酒长毛了还能吃吗？',
  '葡萄酒有臭鸡蛋味是什么原因？'
]

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    if (inputText.value.trim() && !loading.value) {
      sendMessage(inputText.value)
    }
  }
}

async function sendMessage(text) {
  const question = text.trim()
  if (!question || loading.value) return

  messages.value.push({ role: 'user', content: question })
  inputText.value = ''
  errorMsg.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const result = await askKnowledge(question)
    messages.value.push({
      role: 'assistant',
      content: result.answer,
      model: result.model,
      responseTimeMs: result.responseTimeMs
    })
    if (result.model) {
      lastModel.value = result.model
    }
  } catch (e) {
    const errText = e.message || '请求失败，请稍后重试'
    errorMsg.value = errText
    ElMessage.error(errText)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  errorMsg.value = ''
  lastModel.value = ''
}
</script>

<style scoped>
.knowledge-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 顶部栏 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;
  flex-shrink: 0;
}

.chat-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.chat-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 聊天区域 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9fafb;
}

/* 快捷问题 */
.quick-questions {
  text-align: center;
  margin-top: 40px;
}

.quick-title {
  font-size: 14px;
  color: #9ca3af;
  margin-bottom: 16px;
}

.quick-cards {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.quick-card {
  padding: 12px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  color: #374151;
  font-size: 14px;
  transition: all 0.2s;
  max-width: 400px;
  width: 100%;
  text-align: center;
}

.quick-card:hover {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

/* 消息行 */
.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

/* 消息气泡 */
.message-bubble {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.message-bubble.user {
  align-items: flex-end;
}

.message-bubble.assistant {
  align-items: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
  flex-shrink: 0;
}

.message-bubble.user .message-avatar {
  background: #409eff;
  color: #fff;
}

.message-bubble.assistant .message-avatar {
  background: #10b981;
  color: #fff;
}

.message-content {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble.user .message-content {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 2px;
}

.message-bubble.assistant .message-content {
  background: #fff;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 2px;
}

.message-content.thinking {
  color: #9ca3af;
  font-style: italic;
}

.message-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.dot-pulse {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9ca3af;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.error-tip {
  margin-top: 12px;
}

/* 底部输入区 */
.chat-footer {
  padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
  flex-shrink: 0;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-row :deep(.el-textarea) {
  flex: 1;
}

.input-hint {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 6px;
  text-align: right;
}
</style>
