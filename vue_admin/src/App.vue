<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
  mounted() {
    window.addEventListener('unload', this.saveState)
  },
  methods: {
    saveState() {
      sessionStorage.setItem('clubState', JSON.stringify({
        clubid: this.$store.state.clubid,
        clubname: this.$store.state.clubname
      }))
    }
  }
}
</script>

