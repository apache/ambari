module.exports = {
  root: true,
  env: { node: true },
  extends: ['plugin:vue/vue3-essential', 'eslint:recommended'],
  parserOptions: { parser: '@babel/eslint-parser' },
  globals: {
    defineProps: 'readonly',
    defineEmits: 'readonly',
    defineExpose: 'readonly',
    withDefaults: 'readonly',
  },
  rules: {
    'vue/multi-word-component-names': ['error', { ignores: ['Dashboard', 'Login', 'Default', 'App'] }],
  },
}
