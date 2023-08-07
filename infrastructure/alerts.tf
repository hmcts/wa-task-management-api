locals {
  alert_resource_group_name = "wa-${var.env}"
}

module "wa-task-disposal-action-group" {
  source                 = "git@github.com:hmcts/cnp-module-action-group"
  location               = "global"
  env                    = var.env
  resourcegroup_name     = local.alert_resource_group_name
  action_group_name      = "${var.application_name}-${var.env}-ag"
  short_name             = "dispr-alert"
  email_receiver_name    = "WA Task Deletion Failure Alert"
  email_receiver_address = data.azurerm_key_vault_secret.wa-support-email.value
}

module "task-deletion-failure-alert" {
  source                     = "git@github.com:hmcts/cnp-module-metric-alert"
  location                   = var.location
  app_insights_name          = "wa-${var.env}"
  alert_name                 = "${var.application_name}-${var.env}-failures"
  alert_desc                 = "Alert when task fail to delete for case id"
  app_insights_query         = "traces | where message contains 'Unable to delete all tasks for case id:' or message contains 'Deleted some UNTERMINATED tasks:'"
  custom_email_subject       = "Alert: Task deletion failure in wa-${var.env}"
  #run every 6 hrs for early alert
  frequency_in_minutes       = "360"
  # window of 1 day as data extract needs to run daily
  time_window_in_minutes     = "1440"
  severity_level             = "2"
  action_group_name          = module.wa-task-disposal-action-group.action_group_name
  trigger_threshold_operator = "GreaterThan"
  trigger_threshold          = "0"
  resourcegroup_name         = local.alert_resource_group_name
  enabled                    = var.enable_alerts
  common_tags                = var.common_tags
}
