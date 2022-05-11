provider "azurerm" {
  version = "~> 2.25"
  features {}

}

data "azurerm_key_vault" "wa_key_vault" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-wa-task-management-api"
}

resource "azurerm_key_vault_secret" "s2s_secret_task_management_api" {
  name         = "s2s-secret-task-management-api"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location

  tags = var.common_tags
}

//Create Database
module "wa_task_management_api_database" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}"
  name               = "${var.postgres_db_component_name}-postgres-db"
  location           = "${var.location}"
  env                = "${var.env}"
  database_name      = "${var.postgresql_database_name}"
  postgresql_user    = "${var.postgresql_user}"
  postgresql_version = "11"
  common_tags        = "${merge(var.common_tags, map("lastUpdated", "${timestamp()}"))}"
  subscription       = "${var.subscription}"
  sku_capacity       = "${var.database_sku_capacity}"
  sku_name           = "${var.database_sku_name}"
}


//Save secrets in vault
resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.postgres_db_component_name}-POSTGRES-USER"
  value        = module.wa_task_management_api_database.user_name
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PASS"
  value        = module.wa_task_management_api_database.postgresql_password
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.postgres_db_component_name}-POSTGRES-HOST"
  value        = module.wa_task_management_api_database.host_name
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PORT"
  value        = module.wa_task_management_api_database.postgresql_listen_port
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-DATABASE"
  value        = module.wa_task_management_api_database.postgresql_database
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "AZURE_APPINSGHTS_KEY" {
  name         = "AppInsightsInstrumentationKey"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.component}-appinsights-${var.env}"
  location            = var.appinsights_location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"

  tags = var.common_tags

  lifecycle {
    ignore_changes = [
      # Ignore changes to appinsights as otherwise upgrading to the Azure provider 2.x
      # destroys and re-creates this appinsights instance
      application_type,
    ]
  }
}
