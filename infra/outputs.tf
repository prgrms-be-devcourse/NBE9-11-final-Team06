output "vpc_id" {
  description = "Created VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_id" {
  description = "Created public subnet ID"
  value       = aws_subnet.public.id
}

output "security_group_id" {
  description = "Created security group ID"
  value       = aws_security_group.web.id
}

output "instance_id" {
  description = "Created EC2 instance ID"
  value       = aws_instance.web.id
}

output "public_ip" {
  description = "Created EC2 public IP"
  value       = aws_instance.web.public_ip
}

output "public_dns" {
  description = "Created EC2 public DNS"
  value       = aws_instance.web.public_dns
}