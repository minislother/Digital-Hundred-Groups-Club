import { createApp } from 'vue'
import App from './App.vue'
import './styles.css'

// 门户应用只负责身份选择和跳转，不持有业务状态，避免和三端登录态互相影响。
createApp(App).mount('#app')
