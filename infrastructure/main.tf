provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
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

locals {
  computed_tags = {
    lastUpdated = timestamp()
  }
  common_tags = merge(var.common_tags, local.computed_tags)
  db_name = "${var.postgres_db_component_name}-postgres-db-flexible"
}

//New Azure Flexible database
module "wa_task_management_api_database_flexible" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source                      = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product                     = var.product
  component                   = var.component
  name                        = "${var.postgres_db_component_name}-postgres-db-flexible"
  pgsql_sku                   = var.pgsql_sku
  pgsql_storage_mb            = var.pgsql_storage_mb
  location                    = var.location
  business_area               = var.business_area
  env                         = var.env
  action_group_name           = join("-", [local.db_name, var.action_group_name])
  email_address_key           = var.email_address_key
  email_address_key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
  pgsql_databases = [
    {
      name : var.postgresql_database_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "wal_level"
      value = "logical"
    },
    {
      name  = "azure.extensions"
      value = "btree_gin"
    },
    {
      name  = "statement_timeout"
      value = "60000"
    }
  ]

  pgsql_version = 14
  common_tags   = local.common_tags

  admin_user_object_id = var.jenkins_AAD_objectId

}

//New Azure Flexible database replica
module "wa_task_management_api_database_flexible_replica" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source                      = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  product                     = var.product
  component                   = var.component
  name                        = "${var.postgres_db_component_name}-postgres-db-flexible-replica"
  location                    = var.location
  business_area               = var.business_area
  env                         = var.env
  action_group_name           = join("-", [local.db_name, var.action_group_name])
  email_address_key           = var.email_address_key
  email_address_key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
  pgsql_databases = [
    {
      name : var.postgresql_database_name
    }
  ]

  pgsql_version = 14
  common_tags   = local.common_tags

  admin_user_object_id = var.jenkins_AAD_objectId

}

//flexible server
resource "azurerm_key_vault_secret" "POSTGRES-USER-FLEXIBLE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-USER-FLEXIBLE"
  value        = module.wa_task_management_api_database_flexible.username
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-FLEXIBLE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PASS-FLEXIBLE"
  value        = module.wa_task_management_api_database_flexible.password
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST-FLEXIBLE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-HOST-FLEXIBLE"
  value        = module.wa_task_management_api_database_flexible.fqdn
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT-FLEXIBLE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PORT-FLEXIBLE"
  value        = "5432"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE-FLEXIBLE" {
  name         = "${var.postgres_db_component_name}-POSTGRES-DATABASE-FLEXIBLE"
  value        = "cft_task_db"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

//replica
resource "azurerm_key_vault_secret" "POSTGRES-USER-FLEXIBLE-REPLICA" {
  name         = "${var.postgres_db_component_name}-POSTGRES-USER-FLEXIBLE-REPLICA"
  value        = module.wa_task_management_api_database_flexible_replica.username
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-FLEXIBLE-REPLICA" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PASS-FLEXIBLE-REPLICA"
  value        = module.wa_task_management_api_database_flexible_replica.password
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST-FLEXIBLE-REPLICA" {
  name         = "${var.postgres_db_component_name}-POSTGRES-HOST-FLEXIBLE-REPLICA"
  value        = module.wa_task_management_api_database_flexible_replica.fqdn
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT-FLEXIBLE-REPLICA" {
  name         = "${var.postgres_db_component_name}-POSTGRES-PORT-FLEXIBLE-REPLICA"
  value        = "5432"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE-FLEXIBLE-REPLICA" {
  name         = "${var.postgres_db_component_name}-POSTGRES-DATABASE-FLEXIBLE-REPLICA"
  value        = "cft_task_db"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

