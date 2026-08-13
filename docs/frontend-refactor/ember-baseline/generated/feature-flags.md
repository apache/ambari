# Ember 非 Metrics Feature Flag 使用点

> 由 `tools/extract-ember-baseline.mjs` 生成。识别 JavaScript 和 Handlebars 中的 `App.supports.flag` 及 `App.get('supports.flag')`；默认值和服务端覆盖语义见手写权限文档。

共 23 个不同名称。

| 名称 | 使用形式 | 调用点数 | 位置 |
| --- | --- | ---: | --- |
| `alwaysEnableManagedMySQLForHive` | `JavaScript` | 1 | `ambari-web/classic/app/utils/configs/config_initializer.js:155` |
| `autoRollbackHA` | `JavaScript` | 3 | `ambari-web/classic/app/mixins/wizard/wizardProgressPageController.js:369`<br>`ambari-web/classic/app/routes/high_availability_routes.js:48`<br>`ambari-web/classic/app/routes/high_availability_routes.js:63` |
| `createAlerts` | `JavaScript` | 2 | `ambari-web/classic/app/controllers/main/alerts/alert_definitions_actions_controller.js:31`<br>`ambari-web/classic/app/controllers/main/alerts/alert_definitions_actions_controller.js:60` |
| `customizeAgentUserAccount` | `Handlebars`, `JavaScript` | 4 | `ambari-web/classic/app/controllers/wizard/step2_controller.js:160`<br>`ambari-web/classic/app/controllers/wizard/step3_controller.js:201`<br>`ambari-web/classic/app/controllers/wizard/step3_controller.js:381`<br>`ambari-web/classic/app/templates/wizard/step2.hbs:166` |
| `disableCredentialsAutocompleteForRepoUrls` | `JavaScript` | 1 | `ambari-web/classic/app/views/wizard/step1_view.js:327` |
| `displayOlderVersions` | `JavaScript` | 2 | `ambari-web/classic/app/mappers/hosts_mapper.js:183`<br>`ambari-web/classic/app/views/main/admin/stack_upgrade/versions_view.js:190` |
| `enableAddDeleteServices` | `Handlebars`, `JavaScript` | 4 | `ambari-web/classic/app/routes/add_service_routes.js:27`<br>`ambari-web/classic/app/views/main/admin/stack_upgrade/services_view.js:58`<br>`ambari-web/classic/app/views/main/service/item.js:373`<br>`ambari-web/classic/app/templates/main/service/all_services_actions.hbs:25` |
| `enabledWizardForHostOrderedUpgrade` | `JavaScript` | 1 | `ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:230` |
| `enableNewServiceRestartOptions` | `JavaScript` | 1 | `ambari-web/classic/app/views/main/service/item.js:191` |
| `enableToggleKerberos` | `Handlebars`, `JavaScript` | 6 | `ambari-web/classic/app/routes/main.js:475`<br>`ambari-web/classic/app/views/main/admin.js:42`<br>`ambari-web/classic/app/views/main/menu.js:119`<br>`ambari-web/classic/app/templates/main/admin/kerberos.hbs:23`<br>`ambari-web/classic/app/templates/main/admin/kerberos.hbs:42`<br>`ambari-web/classic/app/templates/main/admin/kerberos.hbs:64` |
| `installGanglia` | `JavaScript` | 3 | `ambari-web/classic/app/models/stack_service.js:229`<br>`ambari-web/classic/app/models/stack_version/service_simple.js:32`<br>`ambari-web/classic/app/views/main/admin/stack_upgrade/services_view.js:33` |
| `kerberosStackAdvisor` | `JavaScript` | 1 | `ambari-web/classic/app/controllers/main/admin/kerberos/step4_controller.js:44` |
| `logCountVizualization` | `Handlebars` | 1 | `ambari-web/classic/app/templates/main/host/summary.hbs:173` |
| `logSearch` | `Handlebars`, `JavaScript` | 6 | `ambari-web/classic/app/controllers/global/update_controller.js:252`<br>`ambari-web/classic/app/routes/main.js:333`<br>`ambari-web/classic/app/views/common/host_progress_popup_body_view.js:996`<br>`ambari-web/classic/app/views/main/host/menu.js:64`<br>`ambari-web/classic/app/views/main/host/menu.js:60`<br>`ambari-web/classic/app/templates/common/host_progress_popup.hbs:267` |
| `manageJournalNode` | `JavaScript` | 2 | `ambari-web/classic/app/models/host_component.js:422`<br>`ambari-web/classic/app/views/main/service/item.js:217` |
| `opsDuringRollingUpgrade` | `JavaScript` | 2 | `ambari-web/classic/app/app.js:165`<br>`ambari-web/classic/app/views/main/admin/stack_upgrade/services_view.js:27` |
| `preInstallChecks` | `Handlebars`, `JavaScript` | 4 | `ambari-web/classic/app/controllers/wizard/step7_controller.js:182`<br>`ambari-web/classic/app/controllers/wizard/step7_controller.js:181`<br>`ambari-web/classic/app/templates/wizard/step7.hbs:43`<br>`ambari-web/classic/app/templates/wizard/step7_with_category_tabs.hbs:61` |
| `preKerberizeCheck` | `JavaScript` | 1 | `ambari-web/classic/app/controllers/main/admin/kerberos.js:233` |
| `preUpgradeCheck` | `JavaScript` | 4 | `ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:1134`<br>`ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:1221`<br>`ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:1288`<br>`ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:1460` |
| `regenerateKeytabsOnSingleHost` | `JavaScript` | 1 | `ambari-web/classic/app/views/main/host/details.js:80` |
| `serviceAutoStart` | `JavaScript` | 2 | `ambari-web/classic/app/views/main/admin.js:52`<br>`ambari-web/classic/app/views/main/menu.js:130` |
| `showPageLoadTime` | `JavaScript` | 1 | `ambari-web/classic/app/utils/load_timer.js:31` |
| `skipComponentStartAfterInstall` | `JavaScript` | 5 | `ambari-web/classic/app/controllers/wizard/step9_controller.js:622`<br>`ambari-web/classic/app/controllers/wizard/step9_controller.js:696`<br>`ambari-web/classic/app/controllers/wizard/step9_controller.js:853`<br>`ambari-web/classic/app/controllers/wizard/step9_controller.js:934`<br>`ambari-web/classic/app/controllers/wizard/step9_controller.js:1166` |
