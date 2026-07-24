module.exports = {
  root: true,
  env: {
    browser: true,
    node: true,
    es6: true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/essential'
  ],
  parserOptions: {
    ecmaVersion: 2020,
    sourceType: 'module'
  },
  rules: {
    // 门户页存在少量中文展示文案，保持模板规则基础可用即可。
    'vue/no-multiple-template-root': 'off'
  }
}
