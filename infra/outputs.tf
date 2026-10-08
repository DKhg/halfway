# terraform apply가 끝난 뒤 터미널에 바로 출력해줄 값
# (매번 AWS 콘솔 들어가서 IP 찾지 않아도, apply 끝나자마자 바로 보임)
output "server_public_ip" {
  description = "배포 서버 고정 IP"
  value       = aws_eip.halfway.public_ip
}
