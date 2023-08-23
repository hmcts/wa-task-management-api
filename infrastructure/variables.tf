variable "product" {
  default = "wa"
}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {
  type        = string
  default     = ""
  description = "Deployment Namespace. Optional (only used in PRs)"
}

variable "common_tags" {
  type = map(string)
}

variable "appinsights_instrumentation_key" {
  default = ""
}

variable "postgres_db_component_name" {
  default = "cft-task"
}
variable "postgresql_database_name" {
  default = "cft_task_db"
}

variable "postgresql_user" {
  default = "wa_wa"
}

variable "database_sku_name" {
  default = "GP_Gen5_8"
}

variable "database_sku_capacity" {
  default = "8"
}

variable "business_area" {
  default = "cft"
}

variable "postgres_replica_db_schema" {
  default = "cft_task_db"
}

variable jenkins_AAD_objectId {}

variable aks_subscription_id {}
