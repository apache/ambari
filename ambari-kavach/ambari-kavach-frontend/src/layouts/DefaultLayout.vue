<template>
  <div class="default-layout">
    <AppLayout>
      <router-view />
    </AppLayout>
  </div>
</template>

<style scoped>
.default-layout {
  min-height: 100vh;
}
</style>

<script setup>
import AppLayout from '../components/AppLayout.vue'
</script>
