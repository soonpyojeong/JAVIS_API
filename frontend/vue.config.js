const isProduction = process.env.NODE_ENV === 'production';

module.exports = {
  transpileDependencies: true,

  // 빌드 모드일 때와 개발 모드일 때 `publicPath`와 `outputDir` 설정 분리
  publicPath: isProduction ? '/' : '/',
  outputDir: isProduction
    ? '../src/main/resources/static'
    : 'dist', // 개발 환경에서는 기본 `dist` 폴더를 사용

  devServer: {
    port: process.env.VUE_APP_DEV_SERVER_PORT || 8813, // 기본 포트 설정
    proxy: {
      // HTTP API 프록시 설정
      '/': {
        target: process.env.VUE_APP_API_URL || 'http://10.90.4.60:8813', // Spring Boot 서버 URL
        changeOrigin: true,
        secure: false, // HTTPS 요청 시 필요할 수 있음
      },

    },
  },

  // 빌드 옵션 추가
  productionSourceMap: false, // 배포 환경에서 소스맵 비활성화
};
