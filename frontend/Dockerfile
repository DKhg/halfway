# ===== 1단계(build stage): React 소스를 정적 파일(dist/)로 빌드 =====
# node:20-alpine: Node.js 20 + Alpine Linux(용량 작은 경량 배포판). 빌드 과정에서만 쓰고 버려질 이미지라 가벼운 걸로 선택
FROM node:20-alpine AS build
WORKDIR /app

# package.json·package-lock.json만 먼저 복사 (소스코드는 아직 안 복사)
COPY package.json package-lock.json ./
# npm ci: package-lock.json에 적힌 버전 그대로 설치 (npm install과 달리 lock 파일과 다르면 에러를 냄 → CI/배포 환경에 적합)
# 이 시점의 결과(node_modules)가 Docker 레이어로 캐싱돼서, package.json이 안 바뀌면 다음 빌드 때 재사용됨
RUN npm ci

# 나머지 소스 전체 복사
# .dockerignore에 node_modules를 넣어둔 게 중요함: 안 넣으면 방금 리눅스용으로 설치한 node_modules를
# 로컬(Windows)의 node_modules가 덮어써버려서, esbuild 같은 OS별 네이티브 바이너리가 섞여 빌드가 깨짐
COPY . .
# Vite 프로덕션 빌드 → 결과물은 /app/dist (정적 HTML/JS/CSS)
RUN npm run build

# ===== 2단계(runtime stage): 빌드된 정적 파일을 Nginx로 서빙 =====
# nginx:alpine: 정적 파일 서빙 + 리버스 프록시 용도로 가장 가벼운 조합
FROM nginx:alpine

# 1단계에서 만든 dist 폴더를 Nginx가 기본으로 서빙하는 경로에 복사
COPY --from=build /app/dist /usr/share/nginx/html
# 같은 디렉토리에 작성해둔 nginx.conf로 nginx 기본 설정(default.conf)을 덮어씀
# (SPA 라우팅 처리 + /api 리버스 프록시 설정이 여기 들어있음, 아래 nginx.conf 참고)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 이 컨테이너가 80 포트를 쓴다는 문서화 (docker-compose에서 실제로 80:80 매핑을 해줘야 외부에 열림)
EXPOSE 80
