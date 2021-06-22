variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {}

variable "common_tags" {
  type = map(string)
}

variable "appinsights_instrumentation_key" {
  default = ""
}


variable "postgresql_database_name" {
  default = "cft_task_db"
}

variable "postgresql_user" {
  default = "wa_wa"
}

