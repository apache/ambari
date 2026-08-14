export default [
  {
    source: 'ambari-web/classic/app/controllers/global/background_operations_controller.js',
    line: 178,
    requestExpression: 'queryParams.name',
    dispatchKind: 'HELPER_RETURN_PROPERTY',
    candidateRequestNames: [
      'background_operations.get_most_recent',
      'background_operations.get_by_task',
      'background_operations.get_by_request'
    ],
    dispatchCondition: "getQueryParams() defaults to get_most_recent; TASK_DETAILS selects get_by_task; TASKS_LIST or HOSTS_LIST selects get_by_request.",
    boundaryNotes: 'The current production set is closed. Unknown levelInfo.name values retain the get_most_recent default.',
    evidence: [
      'ambari-web/classic/app/controllers/global/background_operations_controller.js:204-228',
      'ambari-web/classic/app/utils/ajax/ajax.js:562,569,574'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/installer.js',
    line: 590,
    requestExpression: 'name',
    dispatchKind: 'LOCAL_TERNARY',
    candidateRequestNames: [
      'wizard.step1.post_version_definition_file.xml',
      'wizard.step1.post_version_definition_file.url'
    ],
    dispatchCondition: 'A truthy isXMLdata selects the XML dry-run request; otherwise the URL/JSON dry-run request is used.',
    boundaryNotes: 'The current production set is closed.',
    evidence: [
      'ambari-web/classic/app/controllers/installer.js:586-600',
      'ambari-web/classic/app/utils/ajax/ajax.js:2025,2039'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/installer.js',
    line: 656,
    requestExpression: 'name',
    dispatchKind: 'LOCAL_TERNARY',
    candidateRequestNames: [
      'wizard.step8.post_version_definition_file.xml',
      'wizard.step8.post_version_definition_file'
    ],
    dispatchCondition: 'isXMLdata == true selects the XML request; otherwise the JSON request is used.',
    boundaryNotes: 'The current production set is closed. The source intentionally uses loose equality.',
    evidence: [
      'ambari-web/classic/app/controllers/installer.js:653-666',
      'ambari-web/classic/app/utils/ajax/ajax.js:2049,2063'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/admin/highAvailability/progress_popup_controller.js',
    line: 122,
    requestExpression: 'name',
    dispatchKind: 'LOCAL_BRANCH',
    candidateRequestNames: [
      'background_operations.get_by_request',
      'common.request.polling'
    ],
    dispatchCondition: 'A null or undefined stageId selects get_by_request; every other value selects common.request.polling. Numeric zero is normalized to string "0".',
    boundaryNotes: 'The current production set is closed. One request is sent for every requestId with the same selected name.',
    evidence: [
      'ambari-web/classic/app/controllers/main/admin/highAvailability/progress_popup_controller.js:108-131',
      'ambari-web/classic/test/controllers/main/admin/highAvailability/progress_popup_controller_test.js:166-224',
      'ambari-web/classic/app/utils/ajax/ajax.js:234,569'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js',
    line: 461,
    requestExpression: "(onlyState) ? 'admin.upgrade.state' : 'admin.upgrade.data'",
    dispatchKind: 'INLINE_TERNARY',
    candidateRequestNames: [
      'admin.upgrade.state',
      'admin.upgrade.data'
    ],
    dispatchCondition: 'A truthy onlyState loads only Upgrade fields; otherwise the full upgrade group and item data is loaded.',
    boundaryNotes: 'The current production set is closed.',
    evidence: [
      'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:452-471',
      'ambari-web/classic/app/utils/ajax/ajax.js:1723,1740'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/alerts/alert_instances_controller.js',
    line: 118,
    requestExpression: 'ajaxData',
    dispatchKind: 'OPTIONS_OBJECT_SWITCH',
    candidateRequestNames: [
      'alerts.instances.by_host',
      'alerts.instances.by_definition',
      'alerts.instances'
    ],
    dispatchCondition: "sourceType HOST selects by_host; ALERT_DEFINITION selects by_definition; all other values select all alert instances.",
    boundaryNotes: 'The current production set is closed. Scheduled refreshes reuse the previously selected sourceType and sourceName.',
    evidence: [
      'ambari-web/classic/app/controllers/main/alerts/alert_instances_controller.js:83-118,126-172',
      'ambari-web/classic/app/utils/ajax/ajax.js:492,500,504'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js',
    line: 493,
    requestExpression: 'sendData',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'alert_groups.create'
    ],
    dispatchCondition: 'postNewAlertGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static; it is classified as dynamic only because the complete options object is passed by variable.',
    evidence: [
      'ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:464-493',
      'ambari-web/classic/app/utils/ajax/ajax.js:443'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js',
    line: 530,
    requestExpression: 'sendData',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'alert_groups.update'
    ],
    dispatchCondition: 'updateAlertGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static; it is classified as dynamic only because the complete options object is passed by variable.',
    evidence: [
      'ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:506-530',
      'ambari-web/classic/app/utils/ajax/ajax.js:459'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js',
    line: 561,
    requestExpression: 'sendData',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'alert_groups.delete'
    ],
    dispatchCondition: 'removeAlertGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static; it is classified as dynamic only because the complete options object is passed by variable.',
    evidence: [
      'ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:540-561',
      'ambari-web/classic/app/utils/ajax/ajax.js:475'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/host/details.js',
    line: 337,
    requestExpression: "(Array.isArray(component)) ? 'common.host.host_components.update' : 'common.host.host_component.update'",
    dispatchKind: 'INLINE_TERNARY',
    candidateRequestNames: [
      'common.host.host_components.update',
      'common.host.host_component.update'
    ],
    dispatchCondition: 'An array selects the multi-component update on one host; a non-array component selects the single host-component update.',
    boundaryNotes: 'The current production set is closed. The non-array branch expects an Ember component object.',
    evidence: [
      'ambari-web/classic/app/controllers/main/host/details.js:322-344',
      'ambari-web/classic/app/utils/ajax/ajax.js:143,167'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/host/details.js',
    line: 606,
    requestExpression: "(Em.isNone(componentName)) ? 'common.delete.host' : 'common.delete.host_component'",
    dispatchKind: 'INLINE_TERNARY',
    candidateRequestNames: [
      'common.delete.host',
      'common.delete.host_component'
    ],
    dispatchCondition: 'A null or undefined componentName deletes the host; every other value deletes a host component.',
    boundaryNotes: 'The current production set is closed. An empty string is not Em.isNone and therefore selects the host-component endpoint.',
    evidence: [
      'ambari-web/classic/app/controllers/main/host/details.js:598-616',
      'ambari-web/classic/app/utils/ajax/ajax.js:402,406'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/main/service/add_controller.js',
    line: 468,
    requestExpression: 'name',
    dispatchKind: 'PARAMETER_WRAPPER',
    candidateRequestNames: [
      'common.services.update'
    ],
    dispatchCondition: 'The only production caller invokes installServicesRequest with common.services.update.',
    boundaryNotes: 'The wrapper can accept any future request name. A test injects wizard.step3.host_info, but that is not a production call path.',
    evidence: [
      'ambari-web/classic/app/controllers/main/service/add_controller.js:459-474',
      'ambari-web/classic/app/controllers/main/service/add_controller.js:463',
      'ambari-web/classic/test/controllers/main/service/add_controller_test.js:863',
      'ambari-web/classic/app/utils/ajax/ajax.js:46'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/wizard.js',
    line: 426,
    requestExpression: "isRetry ? 'common.host_components.update' : 'common.services.update'",
    dispatchKind: 'INLINE_TERNARY',
    candidateRequestNames: [
      'common.host_components.update',
      'common.services.update'
    ],
    dispatchCondition: 'A retry installs unfinished host components; an initial run installs services in INIT state.',
    boundaryNotes: 'The current production set is closed.',
    evidence: [
      'ambari-web/classic/app/controllers/wizard.js:401-432',
      'ambari-web/classic/app/utils/ajax/ajax.js:46,272'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/wizard/step8_controller.js',
    line: 906,
    requestExpression: 'ajaxOpts',
    dispatchKind: 'OPTIONS_OBJECT_TERNARY',
    candidateRequestNames: [
      'admin.kerberos.cluster.artifact.update',
      'admin.kerberos.cluster.artifact.create'
    ],
    dispatchCondition: 'descriptorExists === true selects update; otherwise create. A truthy instant sends here, while a falsy instant enqueues the same options object.',
    boundaryNotes: 'The current production set is closed. The same pair also appears in the upstream set for queue.shift().',
    evidence: [
      'ambari-web/classic/app/controllers/wizard/step8_controller.js:890-909',
      'ambari-web/classic/app/utils/ajax/ajax.js:1655,1664'
    ]
  },
  {
    source: 'ambari-web/classic/app/controllers/wizard/step9_controller.js',
    line: 511,
    requestExpression: 'name',
    dispatchKind: 'LOCAL_SWITCH',
    candidateRequestNames: [
      'common.host_components.update',
      'common.services.update'
    ],
    dispatchCondition: 'addHostController selects host_components; addServiceController and the default branch select services.',
    boundaryNotes: 'The current production set is closed. Unknown controllerName values retain the common.services.update default.',
    evidence: [
      'ambari-web/classic/app/controllers/wizard/step9_controller.js:466-517',
      'ambari-web/classic/app/utils/ajax/ajax.js:46,272'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/common/configs/configs_saver.js',
    line: 595,
    requestExpression: 'ajaxOptions',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'wizard.step8.apply_configuration_groups'
    ],
    dispatchCondition: 'createConfigGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static. An optional success callback does not change the request name.',
    evidence: [
      'ambari-web/classic/app/mixins/common/configs/configs_saver.js:584-595',
      'ambari-web/classic/app/utils/ajax/ajax.js:2185'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/common/configs/configs_saver.js',
    line: 617,
    requestExpression: 'ajaxOptions',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'config_groups.update_config_group'
    ],
    dispatchCondition: 'updateConfigGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static. An optional success callback does not change the request name.',
    evidence: [
      'ambari-web/classic/app/mixins/common/configs/configs_saver.js:605-617',
      'ambari-web/classic/app/utils/ajax/ajax.js:915'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/common/configs/configs_saver.js',
    line: 644,
    requestExpression: 'ajaxData',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'common.across.services.configurations'
    ],
    dispatchCondition: 'putChangedConfigurations constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static. Optional success and always callbacks do not change the request name.',
    evidence: [
      'ambari-web/classic/app/mixins/common/configs/configs_saver.js:629-644',
      'ambari-web/classic/app/utils/ajax/ajax.js:222'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/main/service/configs/component_actions_by_configs.js',
    line: 112,
    requestExpression: "config_action.get('popupProperties').primaryButton.metaData.name",
    dispatchKind: 'MODEL_METADATA_LOOKUP',
    candidateRequestNames: [
      'service.item.refreshQueueYarnRequest'
    ],
    dispatchCondition: 'The code selects ConfigAction records with actionType showPopup. The only current showPopup fixture supplies service.item.refreshQueueYarnRequest.',
    boundaryNotes: 'The current data set is closed, but ConfigAction is an extension boundary. A future fixture or runtime record can add a name, and this sender does not validate registration.',
    evidence: [
      'ambari-web/classic/app/mixins/main/service/configs/component_actions_by_configs.js:54-94,109-124',
      'ambari-web/classic/app/models/configs/theme/config_action.js:65-120',
      'ambari-web/classic/app/utils/ajax/ajax.js:657'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/main/service/configs/config_overridable.js',
    line: 329,
    requestExpression: 'sendData',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'config_groups.update'
    ],
    dispatchCondition: 'updateConfigurationGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static; callbacks stored on the options object do not change the request name.',
    evidence: [
      'ambari-web/classic/app/mixins/main/service/configs/config_overridable.js:308-329',
      'ambari-web/classic/app/utils/ajax/ajax.js:2476'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/main/service/configs/config_overridable.js',
    line: 439,
    requestExpression: 'sendData',
    dispatchKind: 'STATIC_OPTIONS_OBJECT',
    candidateRequestNames: [
      'common.delete.config_group'
    ],
    dispatchCondition: 'deleteConfigurationGroup constructs an options object with a fixed request name.',
    boundaryNotes: 'This is semantically static; callbacks stored on the options object do not change the request name.',
    evidence: [
      'ambari-web/classic/app/mixins/main/service/configs/config_overridable.js:416-439',
      'ambari-web/classic/app/utils/ajax/ajax.js:414'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/wizard/assign_master_components.js',
    line: 604,
    requestExpression: "isInstaller ? 'hosts.info.install' : 'hosts.high_availability.wizard'",
    dispatchKind: 'LOCAL_TERNARY',
    candidateRequestNames: [
      'hosts.info.install',
      'hosts.high_availability.wizard'
    ],
    dispatchCondition: 'installerController in wizardController.name or content.controllerName selects install host information; all other wizards select existing-cluster HA host information.',
    boundaryNotes: 'The current production set is closed.',
    evidence: [
      'ambari-web/classic/app/mixins/wizard/assign_master_components.js:601-612',
      'ambari-web/classic/app/utils/ajax/ajax.js:2873,2897'
    ]
  },
  {
    source: 'ambari-web/classic/app/mixins/wizard/wizardProgressPageController.js',
    line: 129,
    requestExpression: "this.get('request.ajaxName')",
    dispatchKind: 'MIXIN_REQUEST_PROPERTY',
    candidateRequestNames: [
      'admin.kerberize.cluster',
      'admin.kerberize.cluster.force'
    ],
    dispatchCondition: 'submitRequest is reached only on an isSingleRequestPage path. Kerberos Step 7 is the only current production controller setting that flag; initial execution selects the normal request and retry selects force.',
    boundaryNotes: 'The base mixin remains an extension boundary because a future single-request subclass can supply another request.ajaxName. Other current HA, federation, JournalNode, and Kerberos subclasses use command/task mode and do not reach this sender.',
    evidence: [
      'ambari-web/classic/app/mixins/wizard/wizardProgressPageController.js:60-75,127-140',
      'ambari-web/classic/app/controllers/main/admin/kerberos/step7_controller.js:21-61,82-88',
      'ambari-web/classic/app/routes/add_kerberos_routes.js:321-333',
      'ambari-web/classic/app/utils/ajax/ajax.js:1581,1605'
    ]
  },
  {
    source: 'ambari-web/classic/app/utils/ajax/ajax_queue.js',
    line: 151,
    requestExpression: 'queue.shift()',
    dispatchKind: 'FIFO_OPTIONS_QUEUE',
    candidateRequestNames: [
      'wizard.step8.create_cluster',
      'wizard.step8.create_selected_services',
      'wizard.step8.create_components',
      'wizard.step8.register_host_to_cluster',
      'wizard.step8.register_host_to_component',
      'common.across.services.configurations',
      'wizard.step8.apply_configuration_groups',
      'config_groups.update_config_group',
      'alerts.create_alert_notification',
      'admin.kerberos.cluster.artifact.update',
      'admin.kerberos.cluster.artifact.create',
      'common.host_component.update'
    ],
    dispatchCondition: 'FIFO dispatch from two current production queue instances. Wizard Step 8 conditionally contributes the first eleven request types; Add Service additional-client installation contributes common.host_component.update.',
    boundaryNotes: 'The current production set contains twelve unique names. The queue class is open: addRequest checks only a nonblank name and sender shape, not ajax.js registration. Production code has no addRequests call or direct queue push.',
    evidence: [
      'ambari-web/classic/app/utils/ajax/ajax_queue.js:107-151',
      'ambari-web/classic/app/mixins/wizard/wizardDeployProgressController.js:54-64',
      'ambari-web/classic/app/controllers/wizard/step8_controller.js:201-202,890-909,945-973,991-1018,1087-1093,1132-1137,1462-1467,1548-1553,1581-1604,1705-1711',
      'ambari-web/classic/app/controllers/main/service/add_controller.js:128,499-527',
      'ambari-web/classic/app/utils/ajax/ajax.js:106,222,533,915,1655,1664,2125,2137,2149,2161,2173,2185'
    ]
  },
  {
    source: 'ambari-web/classic/app/utils/config.js',
    line: 1063,
    requestExpression: 'name',
    dispatchKind: 'LOCAL_TERNARY',
    candidateRequestNames: [
      'configs.stack_configs.load.services',
      'configs.stack_configs.load.all'
    ],
    dispatchCondition: 'serviceNames is normalized to an array; a nonempty array selects specified services and an empty array selects all services.',
    boundaryNotes: 'The current production set is closed.',
    evidence: [
      'ambari-web/classic/app/utils/config.js:1060-1071',
      'ambari-web/classic/app/utils/ajax/ajax.js:814,819'
    ]
  },
  {
    source: 'ambari-web/classic/app/views/common/configs/widgets/test_db_connection_widget_view.js',
    line: 226,
    requestExpression: "(isServiceInstalled) ? 'cluster.custom_action.create' : 'custom_action.create'",
    dispatchKind: 'INLINE_TERNARY',
    candidateRequestNames: [
      'cluster.custom_action.create',
      'custom_action.create'
    ],
    dispatchCondition: 'A loaded service model selects the cluster-scoped custom action; otherwise the top-level pre-cluster custom action is used.',
    boundaryNotes: 'The current production set is closed. The runtime boundary is the service model isLoaded state.',
    evidence: [
      'ambari-web/classic/app/views/common/configs/widgets/test_db_connection_widget_view.js:215-237',
      'ambari-web/classic/app/utils/ajax/ajax.js:2819,2841'
    ]
  },
  {
    source: 'ambari-web/classic/app/views/common/controls_view.js',
    line: 1368,
    requestExpression: "(isServiceInstalled) ? 'cluster.custom_action.create' : 'custom_action.create'",
    dispatchKind: 'INLINE_TERNARY',
    candidateRequestNames: [
      'cluster.custom_action.create',
      'custom_action.create'
    ],
    dispatchCondition: 'A loaded parent service model selects the cluster-scoped custom action; otherwise the top-level pre-cluster custom action is used.',
    boundaryNotes: 'The current production set is closed. This is a separate legacy database-control implementation from the dedicated widget view.',
    evidence: [
      'ambari-web/classic/app/views/common/controls_view.js:1365-1379',
      'ambari-web/classic/app/utils/ajax/ajax.js:2819,2841'
    ]
  }
];
