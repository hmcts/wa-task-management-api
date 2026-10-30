import {
  for_each = var.env == "dev" ? toset(["import"]) : toset([])

  to = module.sdp_db_user.azurerm_key_vault_secret.sdp_vault_sdp_read_user_name
  id = "https://mi-vault-dev.vault.azure.net/secrets/cft-task-postgres-db-flexible-replica-read-user-name/523938717c914b0a8fa4cda906212d54"
}

import {
  for_each = var.env == "dev" ? toset(["import"]) : toset([])

  to = module.sdp_db_user.azurerm_key_vault_secret.sdp_vault_sdp_read_user_password
  id = "https://mi-vault-dev.vault.azure.net/secrets/cft-task-postgres-db-flexible-replica-read-user-password/4602bbc69cda4c49b21264fc23125fbd"
}
