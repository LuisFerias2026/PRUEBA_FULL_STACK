terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type        = string
  description = "AWS region for the conceptual outbox destination."
  default     = "us-east-1"
}

variable "project" {
  type        = string
  description = "Name prefix for the audit queue."
  default     = "flashreserve"
}

variable "environment" {
  type        = string
  description = "Deployment environment name."
  default     = "dev"
}

resource "aws_sqs_queue" "audit" {
  name                       = "${var.project}-${var.environment}-audit"
  message_retention_seconds  = 345600
  visibility_timeout_seconds = 30
  sqs_managed_sse_enabled    = true
}

data "aws_iam_policy_document" "outbox_publish" {
  statement {
    sid       = "PublishAuditEvents"
    effect    = "Allow"
    actions   = ["sqs:SendMessage", "sqs:GetQueueAttributes", "sqs:GetQueueUrl"]
    resources = [aws_sqs_queue.audit.arn]
  }
}

resource "aws_iam_policy" "outbox_publish" {
  name   = "${var.project}-${var.environment}-outbox-publish"
  policy = data.aws_iam_policy_document.outbox_publish.json
}

output "audit_queue_url" {
  description = "SQS URL that the outbox processor would publish to in AWS."
  value       = aws_sqs_queue.audit.url
}

output "audit_queue_arn" {
  value = aws_sqs_queue.audit.arn
}

output "outbox_publish_policy_arn" {
  description = "Attach this policy to the backend task/instance role."
  value       = aws_iam_policy.outbox_publish.arn
}
