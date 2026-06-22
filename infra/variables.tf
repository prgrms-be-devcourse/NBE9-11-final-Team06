variable "prefix" {
  description = "Prefix for resource names"
  type        = string
  default     = "gotoday-tf"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "availability_zone" {
  description = "Availability zone for public subnet"
  type        = string
  default     = "ap-northeast-2b"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for public subnet"
  type        = string
  default     = "10.0.28.0/24"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB"
  type        = number
  default     = 20
}

variable "key_name" {
  description = "EC2 key pair name"
  type        = string
  default     = "team06-key"
}