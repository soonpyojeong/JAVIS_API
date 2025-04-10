const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true
})

module.exports = {
  outputDir: '../src/main/resources/static',
  devServer: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // Spring Boot 서버 URL
        changeOrigin: true,
      },
    },
  },
};
