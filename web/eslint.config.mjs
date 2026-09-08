import vue from 'eslint-plugin-vue'
export default [
  { ignores: ['dist/**', 'node_modules/**'] },
  ...vue.configs['flat/essential'],
  { files: ['**/*.{js,mjs,vue}'], languageOptions: { ecmaVersion: 'latest', sourceType: 'module' }, rules: { 'vue/multi-word-component-names': 'off' } },
]
