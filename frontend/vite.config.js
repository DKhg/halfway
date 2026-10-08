import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      // 새 버전 배포되면 사용자가 다음에 앱을 열 때 별도 확인 없이 자동으로 최신 버전으로 교체
      registerType: 'autoUpdate',
      includeAssets: ['favicon.png', 'apple-touch-icon.png'],
      manifest: {
        name: '어디서봄',
        short_name: '어디서봄',
        description: '친구 모임의 날짜 투표, 공평한 중간지점, 막차 계산, 정산을 링크 하나로',
        lang: 'ko',
        start_url: '/',
        display: 'standalone',
        background_color: '#c5f27a',
        theme_color: '#c5f27a',
        icons: [
          { src: '/pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: '/pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          {
            src: '/maskable-icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        // API 응답은 실시간 데이터(방 상태·투표 결과)라 캐싱 대상에서 제외, 정적 파일(앱 셸)만 미리 캐싱
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
