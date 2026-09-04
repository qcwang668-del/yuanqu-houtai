<template>
  <div class="agent-square">
    <div v-if="view === 'square'" class="square-wrap">
      <div class="square-header">
        <h2>智能体广场</h2>
        <p>选择一个 AI 智能体开始对话</p>
      </div>
      <div v-loading="listLoading" class="card-grid">
        <el-card
          v-for="agent in agents"
          :key="agent.id"
          class="agent-card"
          shadow="hover"
          @click="enterChat(agent)"
        >
          <div class="card-body">
            <el-avatar :size="56" :src="agent.avatar">{{ agent.name?.charAt(0) }}</el-avatar>
            <div class="card-main">
              <div class="card-title">
                <span class="name">{{ agent.name }}</span>
                <el-tag v-if="agent.mcpEnabled" size="small" type="success">已接数据源</el-tag>
                <el-tag v-else size="small" type="info">接入中</el-tag>
              </div>
              <div class="desc">{{ agent.description }}</div>
            </div>
          </div>
          <div class="card-footer">
            <el-button type="primary" plain size="small">开始对话</el-button>
          </div>
        </el-card>
        <el-empty v-if="!listLoading && agents.length === 0" description="暂无智能体" />
      </div>
    </div>
    <div v-else class="chat-wrap">
      <div class="chat-side">
        <el-button class="back-btn" text @click="backToSquare">
          <el-icon><ArrowLeft /></el-icon> 返回广场
        </el-button>
        <div class="side-agent">
          <el-avatar :size="32" :src="currentAgent?.avatar">{{ currentAgent?.name?.charAt(0) }}</el-avatar>
          <span>{{ currentAgent?.name }}</span>
        </div>
        <el-button class="new-btn" type="primary" plain size="small" @click="startNewConversation">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
        <div class="conv-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: conv.id === conversationId }"
            @click="switchConversation(conv.id)"
          >
            {{ conv.title || '新对话' }}
          </div>
        </div>
      </div>
      <div class="chat-main">
        <div ref="msgScroll" class="msg-scroll">
          <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role">
            <el-avatar :size="34" :src="msg.role === 'user' ? undefined : currentAgent?.avatar">
              {{ msg.role === 'user' ? '我' : currentAgent?.name?.charAt(0) }}
            </el-avatar>
            <div class="bubble">
              <span v-if="msg.content" class="content">{{ msg.content }}</span>
              <span v-else class="thinking">思考中…</span>
            </div>
          </div>
          <el-empty v-if="messages.length === 0" description="发送第一条消息开始对话" />
        </div>
        <div class="input-bar">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="输入你的问题，Enter 发送"
            :disabled="sending"
            @keydown.enter.exact.prevent="onSend"
          />
          <el-button type="primary" :loading="sending" :disabled="!inputText.trim()" @click="onSend">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/config/axios'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { getAccessToken } from '@/utils/auth'
import { config } from '@/config/axios/config'

defineOptions({ name: 'AgentSquare' })

const AgentApi = {
  getAgentList: () => request.get({ url: '/liqi/agent/list' }),
  createConversation: (agentId: number) =>
    request.post({ url: '/liqi/agent/conversation/create', data: { agentId } }),
  getConversationList: (agentId: number) =>
    request.get({ url: `/liqi/agent/conversation/list?agentId=${agentId}` }),
  getMessageList: (conversationId: number) =>
    request.get({ url: `/liqi/agent/message/list?conversationId=${conversationId}` }),
  sendMessageStream: (
    conversationId: number,
    content: string,
    ctrl: AbortController,
    onMessage: (ev: any) => void,
    onError: (err: any) => void,
    onClose: () => void
  ) => {
    const token = getAccessToken()
    return fetchEventSource(`${config.base_url}/liqi/agent/message/send-stream`, {
      method: 'post',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      openWhenHidden: true,
      body: JSON.stringify({ conversationId, content }),
      onmessage: onMessage,
      onerror: onError,
      onclose: onClose,
      signal: ctrl.signal
    })
  }
}

const view = ref<'square' | 'chat'>('square')
const listLoading = ref(false)
const agents = ref<any[]>([])
const currentAgent = ref<any>(null)
const conversations = ref<any[]>([])
const conversationId = ref<number | null>(null)
const messages = ref<{ role: string; content: string }[]>([])
const inputText = ref('')
const sending = ref(false)
const msgScroll = ref<HTMLElement>()
let ctrl: AbortController | null = null

const loadAgents = async () => {
  listLoading.value = true
  try { agents.value = (await AgentApi.getAgentList()) || [] }
  catch (e) { agents.value = [] }
  finally { listLoading.value = false }
}
const enterChat = async (agent: any) => {
  currentAgent.value = agent
  view.value = 'chat'
  messages.value = []
  conversationId.value = null
  await loadConversations()
  await startNewConversation()
}
const loadConversations = async () => {
  try { conversations.value = (await AgentApi.getConversationList(currentAgent.value.id)) || [] }
  catch (e) { conversations.value = [] }
}
const startNewConversation = async () => {
  try {
    const res: any = await AgentApi.createConversation(currentAgent.value.id)
    conversationId.value = typeof res === 'number' ? res : res?.conversationId
    messages.value = []
  } catch (e) { ElMessage.error('创建会话失败') }
}
const switchConversation = async (id: number) => {
  if (id === conversationId.value) return
  conversationId.value = id
  try {
    const list: any = (await AgentApi.getMessageList(id)) || []
    messages.value = list.map((m: any) => ({ role: m.role, content: m.content }))
    scrollToBottom()
  } catch (e) { messages.value = [] }
}
const backToSquare = () => { if (ctrl) ctrl.abort(); view.value = 'square' }
const scrollToBottom = () => {
  nextTick(() => { if (msgScroll.value) msgScroll.value.scrollTop = msgScroll.value.scrollHeight })
}
const onSend = async () => {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  if (!conversationId.value) await startNewConversation()
  inputText.value = ''
  sending.value = true
  messages.value.push({ role: 'user', content: text })
  const assistant = { role: 'assistant', content: '' }
  messages.value.push(assistant)
  scrollToBottom()
  ctrl = new AbortController()
  await AgentApi.sendMessageStream(
    conversationId.value!, text, ctrl,
    (ev: any) => {
      try {
        const res = JSON.parse(ev.data)
        if (res.code !== 0) { assistant.content += `\n[错误] ${res.msg || '服务异常'}`; return }
        const inc = res.data?.receive?.content
        if (inc) { assistant.content += inc; scrollToBottom() }
        if (res.data?.done) { sending.value = false }
      } catch (e) {}
    },
    (err: any) => { assistant.content += assistant.content ? '' : '[连接错误，请重试]'; sending.value = false; throw err },
    () => { sending.value = false }
  )
}
onMounted(loadAgents)
</script>

<style lang="scss" scoped>
.agent-square { height: calc(100vh - 120px); }
.square-header { margin-bottom: 20px; }
.square-header h2 { margin: 0; font-size: 22px; }
.square-header p { margin: 6px 0 0; color: #909399; font-size: 13px; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.agent-card { cursor: pointer; transition: transform 0.15s; }
.agent-card:hover { transform: translateY(-2px); }
.card-body { display: flex; gap: 14px; }
.card-main { flex: 1; min-width: 0; }
.card-title { display: flex; align-items: center; gap: 8px; }
.card-title .name { font-size: 16px; font-weight: 600; }
.desc { margin-top: 6px; color: #606266; font-size: 13px; line-height: 1.6; }
.card-footer { margin-top: 14px; text-align: right; }
.chat-wrap { display: flex; height: 100%; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; }
.chat-side { width: 220px; border-right: 1px solid #ebeef5; padding: 12px; display: flex; flex-direction: column; gap: 10px; background: #fafafa; }
.side-agent { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.conv-list { flex: 1; overflow-y: auto; }
.conv-item { padding: 8px 10px; border-radius: 6px; font-size: 13px; color: #606266; cursor: pointer; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.conv-item:hover { background: #eef1f6; }
.conv-item.active { background: #e6f0ff; color: #409eff; }
.chat-main { flex: 1; display: flex; flex-direction: column; }
.msg-scroll { flex: 1; overflow-y: auto; padding: 20px; }
.msg-row { display: flex; gap: 12px; margin-bottom: 18px; }
.msg-row.user { flex-direction: row-reverse; }
.bubble { max-width: 72%; padding: 10px 14px; border-radius: 10px; background: #f4f4f5; white-space: pre-wrap; word-break: break-word; line-height: 1.7; font-size: 14px; }
.msg-row.user .bubble { background: #e6f0ff; }
.thinking { color: #909399; }
.input-bar { display: flex; gap: 10px; padding: 12px; border-top: 1px solid #ebeef5; align-items: flex-end; }
.input-bar .el-textarea { flex: 1; }
</style>
