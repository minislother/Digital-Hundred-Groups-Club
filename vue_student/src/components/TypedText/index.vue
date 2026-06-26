<template>
  <h3 class="typing">{{ displayText }}</h3>
</template>

<script>
export default {
  name: 'TypedText',
  props: {
    strings: {
      type: Array,
      required: true
    },
    typeSpeed: {
      type: Number,
      default: 120
    },
    pause: {
      type: Number,
      default: 1600
    }
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      displayText: '',
      stringIndex: 0,
      charIndex: 0,
      timer: null
    }
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    this.tick()
  },
  // 组件卸载前释放监听器或定时器。
  beforeUnmount() {
    clearTimeout(this.timer)
  },
  methods: {
    // 处理当前组件的业务逻辑或用户交互。
    tick() {
      const current = this.strings[this.stringIndex] || ''
      this.displayText = current.slice(0, this.charIndex + 1)
      this.charIndex += 1

      if (this.charIndex >= current.length) {
        this.timer = setTimeout(() => {
          this.stringIndex = (this.stringIndex + 1) % this.strings.length
          this.charIndex = 0
          this.displayText = ''
          this.tick()
        }, this.pause)
        return
      }

      this.timer = setTimeout(this.tick, this.typeSpeed)
    }
  }
}
</script>
