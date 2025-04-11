const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true
})

module.exports = {
  outputDir: '../src/main/resources/static',
  devServer: {
    port: 8812, // 개발 서버 포트 변경
    proxy: {
      '/api': {
        target: 'http://10.90.4.60:8813', // Spring Boot 서버 URL
        changeOrigin: true,
      },
    },
  },
};
