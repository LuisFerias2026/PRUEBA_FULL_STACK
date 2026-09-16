# Terraform — cola de auditoría (SQS)

Destino conceptual en AWS para el outbox de FlashReserve.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
```

**No aplicar** en la prueba. El processor publica por el puerto `AuditPublisher`. Sin `OUTBOX_SQS_QUEUE_URL` escribe logs; con la URL hace `SendMessage` a esta cola.

La política IAM solo permite `sqs:SendMessage` sobre esa cola. En un entorno real se adjuntaría al rol de la tarea del backend y se exportaría la URL de la cola.
