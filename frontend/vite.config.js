import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      // 새 서비스워커는 설치 후 곧바로 활성화하지 않고 "대기" 상태로 둠(skipWaiting 미적용).
      // usePwaUpdate 훅이 주기적으로 체크해서 대기 중인 버전을 찾으면 배너를 띄우고,
      // 사용자가 새로고침을 누른 시점에만 활성화시켜 입력 중인 데이터 유실을 방지
      registerType: 'prompt',
      // index.html에 등록 스크립트를 자동 주입하지 않음 — usePwaUpdate 훅에서 virtual:pwa-register를
      // 직접 호출해 업데이트 체크 주기·배너 노출을 제어하기 위함
      injectRegister: false,
      includeAssets: ['favicon.png', 'apple-touch-icon.png', 'logo-mark.png'],
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
