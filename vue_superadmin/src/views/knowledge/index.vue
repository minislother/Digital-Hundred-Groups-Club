<template><knowledge-assistant :api-base="apiBase" :get-token="getToken" :can-upload="true" /></template>
<script>
import KnowledgeAssistant from '@baituan/frontend-shared/src/KnowledgeAssistant.vue'
import { getToken } from '@/utils/auth'
export default {
  name: 'ClubKnowledge', components: { KnowledgeAssistant },
  data() { return { apiBase: process.env.VUE_APP_BASE_API || '' } },
  methods: { getToken }
}
</script>
