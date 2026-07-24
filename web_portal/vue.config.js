'use strict'

const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

// 统一身份入口单独占用 9527，避免和学生端、管理员端、超级管理员端开发服务冲突。
const port = process.env.port || process.env.npm_config_port || 9527

module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  lintOnSave: false,
  productionSourceMap: false,
  devServer: {
    port,
    open: true,
    client: {
      overlay: {
        warnings: false,
        errors: true
      }
    }
  },
  configureWebpack: {
    name: '数字百团统一入口',
    resolve: {
      alias: {
        '@': resolve('src')
      }
    }
  }
}
