const isProduction = process.env.NODE_ENV === 'production';

module.exports = {
  transpileDependencies: true,

  publicPath: isProduction ? '/' : '/',
  outputDir: isProduction
    ? '../src/main/resources/static'
    : 'dist',

  devServer: {
    port: process.env.VUE_APP_DEV_SERVER_PORT || 8812, // Vue dev 서버  포트

    proxy: {
      // API 요청 프록시
      '/api': {
        target: process.env.VUE_APP_API_URL || 'http://localhost:8813',
        changeOrigin: true,
        secure: false,
        pathRewrite: { '^/api': '/api' },
      },
    client: {
        webSocketURL: false,
      },
  },

  productionSourceMap: false,
};
