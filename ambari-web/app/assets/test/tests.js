/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

require('config');

require('messages');
require('utils/base64');
require('utils/db');
require('utils/helper');
require('utils/config');
require('mixins');
require('models');
require('controllers');
require('views');
require('router');
require('mappers');

require('utils/ajax/ajax');
require('utils/ajax/ajax_queue');

var files = ['test/init_model_test',
  'test/app_test',
  'test/data/secure_mapping_test',
  'test/data/HDP2/secure_mapping_test',
  'test/controllers/global/background_operations_test',
  'test/controllers/global/cluster_controller_test',
  'test/controllers/global/update_controller_test',
  'test/controllers/global/configuration_controller_test',
  'test/controllers/main/app_contoller_test',
  'test/controllers/main/admin/repositories_test',
  'test/controllers/main/admin/serviceAccounts_controller_test',
  'test/controllers/main/admin/highAvailability_controller_test',
  'test/controllers/main/admin/highAvailability/progress_controller_test',
  'test/controllers/main/admin/security_test',
  'test/controllers/main/admin/security/disable_test',
  'test/controllers/main/admin/security/security_progress_controller_test',
  'test/controllers/main/admin/security/add/addSecurity_controller_test',
  'test/controllers/main/admin/security/add/step1_test',
  'test/controllers/main/admin/security/add/step2_test',
  'test/controllers/main/admin/security/add/step3_test',
  'test/controllers/main/admin/security/add/step4_test',
  'test/controllers/main/dashboard/config_history_controller_test',
  'test/controllers/main/charts/heatmap_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_bytesread_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_byteswritten_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_cpuWaitIO_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_diskspaceused_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_memoryused_test',
  'test/controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_ResourceUsed_test',
  'test/controllers/main/host/add_controller_test',
  'test/controllers/main/host/configs_service_test',
  'test/controllers/main/host/details_test',
  'test/controllers/main/service/add_controller_test',
  'test/controllers/main/service/manage_config_groups_controller_test',
  'test/controllers/main/service/reassign_controller_test',
  'test/controllers/main/service/reassign/step2_controller_test',
  'test/controllers/main/service/reassign/step4_controller_test',
  'test/controllers/main/dashboard_test',
  'test/controllers/main/host_test',
  'test/controllers/main/service/item_test',
  'test/controllers/main/service/info/config_test',
  'test/controllers/main/service_test',
  'test/controllers/main/admin_test',
  'test/controllers/main/alerts_controller_test',
  'test/controllers/main/views_controller_test',
  'test/controllers/installer_test',
  'test/controllers/main_test',
  'test/controllers/wizard_test',
  'test/controllers/wizard/step0_test',
  'test/controllers/wizard/step2_test',
  'test/controllers/wizard/step3_test',
  'test/controllers/wizard/step4_test',
  'test/controllers/wizard/step5_test',
  'test/controllers/wizard/step6_test',
  'test/controllers/wizard/step7_test',
  'test/controllers/wizard/step8_test',
  'test/controllers/wizard/step9_test',
  'test/controllers/wizard/step10_test',
  'test/controllers/wizard/stack_upgrade/step3_controller_test',
  'test/login_test',
  'test/router_test',
  'test/mappers/server_data_mapper_test',
  'test/mappers/hosts_mapper_test',
  'test/mappers/service_mapper_test',
  'test/mappers/service_metrics_mapper_test',
  'test/mappers/status_mapper_test',
  'test/mappers/users_mapper_test',
  'test/mappers/stack_mapper_test',
  'test/mixins/common/chart/storm_linear_time_test',
  'test/mixins/common/localStorage_test',
  'test/mixins/main/host/details/host_components/decommissionable_test',
  'test/utils/configs/defaults_providers/yarn_defaults_provider_test',
  'test/utils/configs/defaults_providers/tez_defaults_provider_test',
  'test/utils/configs/defaults_providers/hive_defaults_provider_test',
  'test/utils/configs/validators/service_configs_validator_test',
  'test/utils/ajax/ajax_test',
  'test/utils/ajax/ajax_queue_test',
  'test/utils/batch_scheduled_requests_test',
  'test/utils/blueprint_test',
  'test/utils/config_test',
  'test/utils/date_test',
  'test/utils/config_test',
  'test/utils/form_field_test',
  'test/utils/host_progress_popup_test',
  'test/utils/misc_test',
  'test/utils/number_utils_test',
  'test/utils/validator_test',
  'test/utils/config_test',
  'test/utils/string_utils_test',
  'test/utils/helper_test',
  'test/utils/object_utils_test',
  'test/utils/ui_effects_test',
  'test/utils/updater_test',
  'test/views/common/chart/linear_time_test',
  'test/views/common/filter_combo_cleanable_test',
  'test/views/common/filter_view_test',
  'test/views/common/table_view_test',
  'test/views/common/quick_link_view_test',
  'test/views/common/rolling_restart_view_test',
  'test/views/common/configs/config_history_flow_test',
  'test/views/main/dashboard_test',
  'test/views/main/menu_test',
  'test/views/main/dashboard/config_history_view_test',
  'test/views/main/dashboard/widget_test',
  'test/views/main/dashboard/widgets_test',
  'test/views/main/dashboard/widgets/text_widget_test',
  'test/views/main/dashboard/widgets/uptime_text_widget_test',
  'test/views/main/dashboard/widgets/node_managers_live_test',
  'test/views/main/dashboard/widgets/datanode_live_test',
  'test/views/main/dashboard/widgets/tasktracker_live_test',
  'test/views/main/dashboard/widgets/hbase_average_load_test',
  'test/views/main/dashboard/widgets/hbase_regions_in_transition_test',
  'test/views/main/dashboard/widgets/jobtracker_rpc_test',
  'test/views/main/dashboard/widgets/namenode_rpc_test',
  'test/views/main/dashboard/widgets/hbase_master_uptime_test',
  'test/views/main/dashboard/widgets/jobtracker_uptime_test',
  'test/views/main/dashboard/widgets/namenode_uptime_test',
  'test/views/main/dashboard/widgets/resource_manager_uptime_test',
  'test/views/main/dashboard/widgets/links_widget_test',
  'test/views/main/dashboard/widgets/pie_chart_widget_test',
  'test/views/main/dashboard/widgets/namenode_cpu_test',
  'test/views/main/host/details_test',
  'test/views/main/host/summary_test',
  'test/views/main/host/details/host_component_view_test',
  'test/views/main/host/details/host_component_views/decommissionable_test',
  'test/views/main/charts/heatmap/heatmap_host_test',
  'test/views/main/service/item_test',
  'test/views/main/service/info/config_test',
  'test/views/main/service/info/summary_test',
  'test/views/main/mirroring/edit_dataset_view_test',
  'test/views/common/configs/overriddenProperty_view_test',
  'test/views/common/configs/services_config_test',
  'test/views/wizard/step3/hostLogPopupBody_view_test',
  'test/views/wizard/step3/hostWarningPopupBody_view_test',
  'test/views/wizard/step3/hostWarningPopupFooter_view_test',
  'test/views/wizard/step0_view_test',
  'test/views/wizard/step1_view_test',
  'test/views/wizard/step2_view_test',
  'test/views/wizard/step3_view_test',
  'test/views/wizard/step5_view_test',
  'test/views/wizard/step6_view_test',
  'test/views/wizard/step8_view_test',
  'test/views/wizard/step9_view_test',
  'test/views/wizard/step9/hostLogPopupBody_view_test',
  'test/views/wizard/step10_view_test',
  'test/views/application_test',
  'test/views/experimental_test',
  'test/views/installer_test',
  'test/views/login_test',
  'test/models/service/flume_test',
  'test/models/service/hdfs_test',
  'test/models/service/yarn_test',
  'test/models/alert_test',
  'test/models/authentication_test',
  'test/models/cluster_states_test',
  'test/models/config_group_test',
  'test/models/dataset_test',
  'test/models/dataset_job_test',
  'test/models/form_test',
  'test/models/host_test',
  'test/models/host_component_test',
  'test/models/hosts_test',
  'test/models/run_test',
  'test/models/service_config_test',
  'test/models/stack_service_component_test',
  'test/models/service_test',
  'test/models/stack_service_test',
  'test/models/user_test',
  //contains test with fake timers that affect Date
  'test/utils/lazy_loading_test'
];
App.initialize();
describe('Ambari Web Unit tests', function() {

  for (var i = 0; i < files.length; i++) {

    describe(files[i], function() {
      require(files[i]);
    });

  }

});
