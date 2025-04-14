const { defineConfig } = require('@vue/cli-service');

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: '/',
  outputDir: '../src/main/resources/static',
  devServer: {
    port: process.env.VUE_APP_DEV_SERVER_PORT, // 개발 서버 포트를 환경 변수로 설정
    proxy: {
      '/': {
        target: process.env.VUE_APP_API_URL, // Spring Boot 서버 URL을 환경 변수로 설정
        changeOrigin: true,
      },
    },
  },
});
