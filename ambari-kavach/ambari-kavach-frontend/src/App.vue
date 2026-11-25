<template>
  <v-app>
    <router-view />
    <!-- Global snackbar for notifications -->
    <v-snackbar
      v-model="snackbar.visible.value"
      :color="snackbar.color.value"
      :timeout="snackbar.timeout.value"
      location="bottom right"
      min-width="280"
    >
      {{ snackbar.message.value }}
      <template #actions>
        <v-btn variant="text" @click="snackbar.visible.value = false">Close</v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script setup>
import { useSnackbar } from './composables/useSnackbar'
const snackbar = useSnackbar()
</script>

<style>
/* Global overrides */
.v-application {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif !important;
}
</style>
