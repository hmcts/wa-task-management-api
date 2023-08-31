provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "sdp_vault"
  subscription_id            = local.sdp_environment_ids[local.sdp_environment].subscription
}

locals {
  sdp_cft_environments_map = {
    sandbox  = "sbox"
    aat      = "dev"
    perftest = "test"
  }

  sdp_environment = lookup(local.sdp_cft_environments_map, var.env, var.env)

  sdp_environment_ids = {
    sbox = {
      subscription = "a8140a9e-f1b0-481f-a4de-09e2ee23f7ab"
    }
    dev = {
      subscription = "867a878b-cb68-4de5-9741-361ac9e178b6"
    }
    test = {
      subscription = "3eec5bde-7feb-4566-bfb6-805df6e10b90"
    }
    ithc = {
      subscription = "ba71a911-e0d6-4776-a1a6-079af1df7139"
    }
    stg = {
      subscription = "74dacd4f-a248-45bb-a2f0-af700dc4cf68"
    }
    prod = {
      subscription = "5ca62022-6aa2-4cee-aaa7-e7536c8d566c"
    }
  }
}

//Only adding read user for the replica created by module wa_task_management_api_database_flexible_replica
module "sdp_db_user" {

  providers = {
    azurerm.sdp_vault = azurerm.sdp_vault
  }

  source = "git@github.com:hmcts/terraform-module-sdp-db-user?ref=master"
  env    = local.sdp_environment

  server_name       = "${var.postgres_db_component_name}-postgres-db-flexible-replica"
  server_fqdn       = module.wa_task_management_api_database_flexible_replica.fqdn
  server_admin_user = module.wa_task_management_api_database_flexible_replica.username
  server_admin_pass = module.wa_task_management_api_database_flexible_replica.password

  databases = [
    {
      name : var.postgresql_database_name
    }
  ]

  common_tags = local.common_tags
}
