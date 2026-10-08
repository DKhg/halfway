# 이 Terraform 설정이 어떤 provider(클라우드 업체)를 쓸지, 어떤 버전을 쓸지 선언
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"  # 5.x 버전대 아무거나 (5.0 이상, 6.0 미만)
    }
  }
}

# aws 프로바이더 설정. 리전은 variables.tf에서 정의한 변수(기본값 서울) 사용
provider "aws" {
  region = var.aws_region
}

# 최신 Amazon Linux 2023 AMI(= 가상머신 이미지)를 자동으로 검색해서 가져옴
# AMI ID는 리전·시점마다 다르게 갱신되는 값이라, 하드코딩하지 않고 "조건에 맞는 최신 걸 찾아라"는 식으로 작성
data "aws_ami" "amazon_linux" {
  most_recent = true          # 조건에 맞는 것 중 가장 최근에 나온 버전
  owners      = ["amazon"]    # 아마존 공식에서 배포한 이미지만
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]  # 이름에 "al2023-ami-"로 시작하는 64비트 이미지
  }
}

# 방화벽 역할을 하는 보안 그룹. 여기 정의된 포트만 외부에서 접근 가능
resource "aws_security_group" "halfway" {
  name        = "halfway-sg"
  description = "Security group for halfway service"

  # ingress = 인바운드(외부 → 이 서버로 들어오는 트래픽) 허용 규칙
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_cidr]  # variables.tf에서 정한 범위 (지금은 전체 허용)
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # 웹사이트 접속용이라 전체 공개가 당연히 맞음
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # 나중에 도메인+인증서 붙일 때 대비해서 미리 열어둠 (지금 당장 안 써도 무방)
  }

  # egress = 아웃바운드(이 서버 → 외부로 나가는 트래픽). "-1"은 모든 프로토콜/포트를 의미
  # 서버가 Docker 이미지 받아오거나 외부 API(카카오·TAGO 등) 호출할 때 필요해서 전체 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 실제 EC2 가상서버 한 대
resource "aws_instance" "halfway" {
  ami                    = data.aws_ami.amazon_linux.id   # 위에서 찾은 AMI
  instance_type          = var.instance_type               # variables.tf 기본값: t3.micro
  key_name                = var.key_name                    # terraform.tfvars에서 지정한 "halfway-key"
  vpc_security_group_ids  = [aws_security_group.halfway.id] # 위에서 만든 보안 그룹 연결

  # user_data: EC2가 "최초 부팅"될 때 자동으로 한 번 실행되는 쉘 스크립트
  # (SSH로 직접 접속해서 명령어 치지 않아도, 서버가 켜지자마자 Docker가 설치돼 있게 미리 지시해두는 것)
  user_data = <<-EOF
    #!/bin/bash
    dnf update -y
    dnf install -y docker git
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ec2-user

    # "docker compose"(띄어쓰기, 최신 플러그인 방식)가 동작하도록 Docker CLI 플러그인 폴더에 설치
    # 로컬(Docker Desktop)에서 쓰던 "docker compose -f ... up" 명령어를 서버에서도 똑같이 쓸 수 있게 하기 위함
    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
  EOF

  tags = {
    Name = "halfway-server"  # AWS 콘솔에서 인스턴스 목록 볼 때 표시되는 이름표
  }
}

# 고정 IP(Elastic IP) 생성 후 위 인스턴스에 연결
# 이게 없으면 EC2를 재시작할 때마다 퍼블릭 IP가 바뀌어서, 그때마다 도메인/CORS 설정을 다시 해야 함
resource "aws_eip" "halfway" {
  instance = aws_instance.halfway.id
  domain   = "vpc"
}
