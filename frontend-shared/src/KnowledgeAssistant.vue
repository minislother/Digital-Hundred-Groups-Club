<template>
  <main class="knowledge-page">
    <header><p class="eyebrow">社团知识库</p><h1>把问题交给社团助手</h1><p>查询社团资料与办事说明。回答由 TeamRag 提供，请结合原始资料核实。</p></header>
    <section class="knowledge-tools" aria-label="选择知识库">
      <label for="knowledge-group">选择社团</label>
      <select id="knowledge-group" v-model="groupId" :disabled="busy" @change="resetConversation">
        <option value="">请选择社团</option><option v-for="group in groups" :key="group.id" :value="group.id">{{ group.name }}</option>
      </select>
      <button type="button" :disabled="busy || !validGroup" @click="loadDocuments">查看资料</button>
      <button type="button" :disabled="busy" @click="resetConversation">新建会话</button>
      <label v-if="canUpload" class="file-label">上传资料（最多10MB）<input type="file" :disabled="busy || !validGroup" @change="uploadDocument"></label>
    </section>
    <p v-if="error" role="alert" class="knowledge-error">{{ error }}</p>
    <section v-if="documents.length" class="knowledge-documents" aria-label="知识资料">
      <h2>知识资料</h2><ul><li v-for="(document, index) in documents" :key="document.documentId || index">{{ document.filename || document.fileName || document.documentId }} · {{ document.status }}</li></ul>
    </section>
    <section class="knowledge-chat" aria-label="问答记录" aria-live="polite" :aria-busy="busy">
      <p v-if="!messages.length" class="knowledge-empty">选择有权访问的社团，然后询问入团条件、活动要求或社团章程。</p>
      <article v-for="(message, index) in messages" :key="index" :class="['knowledge-message', message.role]">
        <strong>{{ message.role === 'user' ? '我' : '社团助手' }}</strong><p>{{ message.text || '正在查阅资料…' }}</p>
      </article>
    </section>
    <form class="knowledge-compose" @submit.prevent="ask">
      <label for="knowledge-question">你的问题</label>
      <textarea id="knowledge-question" v-model="query" rows="3" maxlength="4000" :disabled="busy" placeholder="例如：社团活动申请需要准备哪些材料？" />
      <div><span>{{ query.length }}/4000</span><button v-if="busy" type="button" @click="cancel">停止回答</button><button v-else type="submit" :disabled="!validGroup || !query.trim()">发送问题</button></div>
    </form>
  </main>
</template>

<script>
import { useKnowledgeSession } from './stores/knowledge'
// 三端复用同一交互；请求始终经过数字百团后端，不保存 TeamRag 凭据。
export default {
  name: 'KnowledgeAssistant',
  props: { apiBase: { type: String, default: '' }, getToken: { type: Function, required: true }, canUpload: Boolean },
  data() { return { groupId: '', groups: [], query: '', session: useKnowledgeSession(), documents: [], error: '', busy: false, controller: null } },
  mounted() { this.loadGroups() },
  computed: {
    validGroup() { return Number.isInteger(Number(this.groupId)) && Number(this.groupId) > 0 },
    messages: { get() { return this.session.messages }, set(value) { this.session.messages = value } },
    conversationId: { get() { return this.session.conversationId }, set(value) { this.session.conversationId = value } }
  },
  beforeUnmount() { this.cancel(); this.session.reset() },
  methods: {
    cancel() { if (this.controller) this.controller.abort() },
    resetConversation() { this.cancel(); this.messages = []; this.documents = []; this.error = ''; this.conversationId = '' },
    endpoint(suffix) { return `${this.apiBase.replace(/\/$/, '')}/knowledge/groups/${Number(this.groupId)}/${suffix}` },
    headers() { return { 'X-Token': this.getToken() || '' } },
    async loadGroups() {
      this.busy = true
      try {
        const data = await this.readResult(await fetch(`${this.apiBase.replace(/\/$/, '')}/knowledge/groups`, { headers: this.headers() }))
        this.groups = data.groups || []
        if (!this.groups.length) this.error = '暂无可访问的社团知识库，请联系社团管理员'
      } catch (error) { this.error = error.message } finally { this.busy = false }
    },
    async readResult(response) {
      const result = await response.json()
      if (!response.ok || result.code !== 20000) throw new Error(result.message || '请求失败，请稍后重试')
      return result.data
    },
    async loadDocuments() {
      this.error = ''; this.busy = true
      try { const data = await this.readResult(await fetch(this.endpoint('documents'), { headers: this.headers() })); this.documents = data.documents || [] }
      catch (error) { this.error = error.message } finally { this.busy = false }
    },
    async uploadDocument(event) {
      const file = event.target.files[0]
      if (!file) return
      if (file.size > 10 * 1024 * 1024) { this.error = '请选择不超过10MB的文档'; event.target.value = ''; return }
      this.error = ''; this.busy = true
      try {
        const body = new FormData(); body.append('file', file)
        await this.readResult(await fetch(this.endpoint('documents'), { method: 'POST', headers: this.headers(), body }))
        await this.loadDocuments()
      } catch (error) { this.error = error.message } finally { this.busy = false; event.target.value = '' }
    },
    async ask() {
      if (this.busy || !this.validGroup || !this.query.trim()) return
      this.error = ''; this.busy = true; this.controller = new AbortController()
      if (!this.conversationId) this.conversationId = `${Date.now()}-${Math.random().toString(36).slice(2)}`
      const question = this.query.trim(); this.query = ''
      this.messages.push({ role: 'user', text: question }, { role: 'assistant', text: '' })
      const index = this.messages.length - 1
      try {
        const response = await fetch(this.endpoint('chat'), { method: 'POST', signal: this.controller.signal,
          headers: { ...this.headers(), 'Content-Type': 'application/json' }, body: JSON.stringify({ query: question, conversationId: this.conversationId }) })
        if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream')) {
          await this.readResult(response); throw new Error('知识库未返回有效回答')
        }
        const reader = response.body.getReader(); const decoder = new TextDecoder(); let pending = ''
        const consume = frame => {
          const lines = frame.split('\n'); const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).replace(/^ /, '')).join('\n')
          if (lines.some(line => /^event:\s*error$/.test(line))) throw new Error(data || '回答中断，请稍后重试')
          if (data && data !== '[DONE]') this.messages[index].text += data
        }
        while (true) {
          const { value, done } = await reader.read()
          pending = (pending + decoder.decode(value, { stream: !done })).replace(/\r\n/g, '\n')
          if (pending.length > 1024 * 1024) throw new Error('回答片段过大，请缩小问题范围')
          let boundary
          while ((boundary = pending.indexOf('\n\n')) >= 0) { consume(pending.slice(0, boundary)); pending = pending.slice(boundary + 2) }
          if (done) { if (pending.trim()) consume(pending); break }
        }
        if (!this.messages[index].text) throw new Error('未收到回答，请稍后重试')
      } catch (error) { if (error.name !== 'AbortError') this.error = error.message }
      finally { this.controller.abort(); this.controller = null; this.busy = false }
    }
  }
}
</script>

<style scoped>
.knowledge-page{max-width:1040px;margin:0 auto;padding:32px 24px;color:#1e293b;font-size:16px;line-height:1.6}.eyebrow{color:#2563eb;font-weight:600;letter-spacing:.08em}h1{font-size:30px;line-height:1.3;margin:8px 0}header>p:last-child{color:#475569}.knowledge-tools{display:flex;align-items:center;flex-wrap:wrap;gap:12px;padding:20px 0}.knowledge-tools input[type=number]{width:110px}.knowledge-page button,.knowledge-page input,.knowledge-page textarea,.knowledge-page select{font:inherit;border:1px solid #cbd5e1;border-radius:8px;padding:10px 14px}.knowledge-page button{cursor:pointer;background:#fff;min-height:44px}.knowledge-page button:hover{background:#eff6ff}.knowledge-page button:disabled{opacity:.55;cursor:not-allowed}.knowledge-page :focus-visible{outline:3px solid #2563eb;outline-offset:3px}.file-label{font-size:14px;display:flex;flex-direction:column}.file-label input{max-width:280px}.knowledge-error{padding:12px 16px;color:#991b1b;background:#fef2f2;border-left:3px solid #b91c1c}.knowledge-chat{min-height:260px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:24px}.knowledge-empty{color:#475569;margin:40px auto;max-width:480px}.knowledge-message{max-width:85%;margin:0 0 20px;padding:16px;background:#fff;border:1px solid #e2e8f0;border-radius:10px}.knowledge-message.user{margin-left:auto;background:#eff6ff}.knowledge-message p{white-space:pre-wrap;overflow-wrap:anywhere;margin:6px 0 0}.knowledge-compose{display:grid;gap:8px;margin-top:20px}.knowledge-compose textarea{resize:vertical;width:100%;box-sizing:border-box}.knowledge-compose>div{display:flex;justify-content:space-between;align-items:center}.knowledge-compose button[type=submit]{background:#2563eb;color:#fff}.knowledge-documents{padding:16px;background:#f8fafc;margin-bottom:16px}.knowledge-documents h2{font-size:18px;margin:0}@media(max-width:600px){.knowledge-page{padding:20px 16px}h1{font-size:24px}.knowledge-message{max-width:100%}.knowledge-chat{padding:16px}}
</style>
