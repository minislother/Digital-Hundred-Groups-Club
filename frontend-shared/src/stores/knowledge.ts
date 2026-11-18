import { defineStore } from 'pinia'

type Message = { role: 'user' | 'assistant'; text: string }

/** 仅存于当前应用内存的问答会话；不持久化敏感资料或跨三端共享登录态。 */
export const useKnowledgeSession = defineStore('knowledge-session', {
  state: () => ({ conversationId: '', messages: [] as Message[] }),
  actions: {
    reset() { this.conversationId = ''; this.messages = [] }
  }
})
