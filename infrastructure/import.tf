 import {
   for_each = var.env == "perftest" ? toset(["import"]) : toset([])

   to = module.sdp_db_user.azurerm_key_vault_secret.sdp_vault_sdp_read_user_name
   id = "https://mi-vault-test.vault.azure.net/secrets/cft-task-postgres-db-flexible-replica-read-user-name"
 }
