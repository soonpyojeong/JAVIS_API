const isProduction = process.env.NODE_ENV === 'production';

module.exports = {
  transpileDependencies: true,

  publicPath: isProduction ? '/' : '/',
  outputDir: isProduction
    ? '../src/main/resources/static'
    : 'dist',

  devServer: {
    port: process.env.VUE_APP_DEV_SERVER_PORT || 8812,

    proxy: {
      '/api': {
        target: process.env.VUE_APP_API_URL || 'http://localhost:8813',
        changeOrigin: true,
        secure: false,
        // 🔥 pathRewrite 필요 없음 → 동일한 경로일 경우 제거 가능
        // pathRewrite: { '^/api': '/api' }, ← 불필요
      },
    },

    // ✅ client 설정은 devServer.client로 바르게 위치해야 함
    client: {
      webSocketURL: 'ws://localhost:8812/ws',
    },
  },

  productionSourceMap: false,
};
