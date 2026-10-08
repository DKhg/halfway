# 배포할 AWS 리전. ap-northeast-2 = 서울
variable "aws_region" {
  description = "배포할 AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

# EC2 인스턴스 사양. t3.small (RAM 2GB) — Postgres+Spring Boot+Nginx 세 컨테이너를
# 같이 돌리기엔 t3.micro(1GB)보다 여유 있어서 선택. 크레딧 잔액으로 충분히 감당 가능
variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t3.small"
}

# SSH 접속용 키 페어 이름. AWS 콘솔에서 미리 만들어둔 키 페어 "이름"만 적는 곳
# (키 파일 자체를 여기 넣는 게 아니라, 콘솔에 등록된 키 페어를 "이름으로 지목"하는 것)
# default가 없어서 필수값 → terraform.tfvars에서 반드시 채워줘야 함
variable "key_name" {
  description = "SSH 접속용 키 페어 이름"
  type        = string
}

# SSH(22번 포트) 접속을 허용할 IP 대역. "내 IP/32"면 그 IP만, "0.0.0.0/0"이면 전체 허용
# (이 프로젝트는 와이파이가 자주 바뀌어서 기본값을 전체 허용으로 둠 — 키 페어 없인 어차피 로그인 불가능)
variable "ssh_allowed_cidr" {
  description = "SSH 접속을 허용할 IP 대역"
  type        = string
  default     = "0.0.0.0/0"
}
