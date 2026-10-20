// 네이티브 앱에서는 location.origin이 실제 도메인이 아니라 Capacitor 가상 주소(https://localhost)라서,
// 공유 링크에 쓰면 받는 사람 기기에서 열리지 않음. 네이티브 빌드에서만 VITE_PUBLIC_WEB_URL로 실제 도메인을 대신 씀
export function getPublicOrigin() {
  return import.meta.env.VITE_PUBLIC_WEB_URL || window.location.origin
}
