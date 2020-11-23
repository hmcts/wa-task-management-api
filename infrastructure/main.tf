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

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name        = "microservicekey-wa-task-management-api"
}

resource "azurerm_key_vault_secret" "s2s_secret_task_management_api" {
  name         = "s2s-secret-task-management-api"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}


data "azurerm_key_vault_secret" "test_law_firm_a_username" {
  name         = "test-law-firm-a-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "test_law_firm_a_password" {
  name          = "test-law-firm-a-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "test_law_firm_b_username" {
  name         = "test-law-firm-b-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "test_law_firm_b_password" {
  name          = "test-law-firm-b-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_idam_client_id" {
  name         = "wa-idam-client-id"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_idam_client_secret" {
  name          = "wa-idam-client-secret"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_a_username" {
  name         = "wa-test-caseofficer-public-a-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_a_password" {
  name          = "wa-test-caseofficer-public-a-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_b_username" {
  name         = "wa-test-caseofficer-public-b-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_b_password" {
  name          = "wa-test-caseofficer-public-b-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_c_username" {
  name         = "wa-test-caseofficer-public-c-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_c_password" {
  name          = "wa-test-caseofficer-public-c-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_d_username" {
  name         = "wa-test-caseofficer-public-d-username"
  key_vault_id = data.azurerm_key_vault.wa_key_vault.id
}

data "azurerm_key_vault_secret" "wa_test_caseofficer_public_d_password" {
  name          = "wa-test-caseofficer-public-d-password"
  key_vault_id  = data.azurerm_key_vault.wa_key_vault.id
}
