provider "azurerm" {
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

data "azurerm_key_vault_secret" "test_law_firm_a_username" {
  name         = "test-law-firm-a-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id

}

data "azurerm_key_vault_secret" "test_law_firm_a_password" {
  name          = "test-law-firm-a-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

locals {
  S2S_SECRET_TASK_MANAGEMENT_API = "${data.azurerm_key_vault_secret.s2s_secret.value}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name        = "microservicekey-wa-task-management-api"
}
