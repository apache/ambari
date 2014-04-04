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

require('test/app_test');
require('test/data/HDP2/site_properties_test');
require('test/controllers/global/background_operations_test');
require('test/controllers/global/cluster_controller_test');
require('test/controllers/main/app_contoller_test');
require('test/controllers/main/admin/cluster_test');
require('test/controllers/main/admin/security/add/addSecurity_controller_test');
require('test/controllers/main/admin/security/add/step2_test');
require('test/controllers/main/admin/security/add/step3_test');
require('test/controllers/main/admin/security/add/step4_test');
require('test/controllers/main/charts/heatmap_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_bytesread_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_byteswritten_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_cpuWaitIO_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_diskspaceused_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_memoryused_test');
require('test/controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_ResourceUsed_test');
require('test/controllers/main/service/add_controller_test');
require('test/controllers/main/service/reassign_controller_test');
require('test/controllers/main/dashboard_test');
require('test/controllers/main/host_test');
require('test/controllers/main/item_test');
require('test/controllers/main/service_test');
require('test/controllers/wizard/stack_upgrade/step3_controller_test');
require('test/controllers/installer_test');
require('test/controllers/wizard_test');
require('test/installer/step0_test');
require('test/installer/step2_test');
require('test/installer/step3_test');
require('test/installer/step4_test');
require('test/installer/step5_test');
require('test/installer/step6_test');
require('test/installer/step7_test');
require('test/installer/step8_test');
require('test/installer/step9_test');
require('test/installer/step10_test');
require('test/login_test');
require('test/mappers/server_data_mapper_test');
require('test/mappers/hosts_mapper_test');
require('test/mappers/jobs_mapper_test');
require('test/mappers/runs_mapper_test');
require('test/mappers/service_mapper_test');
require('test/mappers/status_mapper_test');
require('test/mappers/users_mapper_test');
require('test/utils/configs/defaults_providers/yarn_defaults_provider_test');
require('test/utils/configs/defaults_providers/tez_defaults_provider_test');
require('test/utils/configs/defaults_providers/hive_defaults_provider_test');
require('test/utils/configs/validators/service_configs_validator_test');
require('test/utils/ajax_test');
require('test/utils/batch_scheduled_requests_test');
require('test/utils/config_test');
require('test/utils/date_test');
require('test/utils/config_test');
require('test/utils/date_test');
require('test/utils/form_field_test');
require('test/utils/misc_test');
require('test/utils/number_utils_test');
require('test/utils/validator_test');
require('test/utils/config_test');
require('test/utils/string_utils_test');
require('test/utils/lazy_loading_test');
require('test/views/common/chart/linear_time_test');
require('test/views/common/filter_view_test');
require('test/views/common/table_view_test');
require('test/views/common/quick_link_view_test');
require('test/views/common/rolling_restart_view_test');
require('test/views/main/dashboard_test');
require('test/views/main/dashboard/widget_test');
require('test/views/main/dashboard/widgets/text_widget_test');
require('test/views/main/dashboard/widgets/uptime_text_widget_test');
require('test/views/main/dashboard/widgets/node_managers_live_test');
require('test/views/main/dashboard/widgets/datanode_live_test');
require('test/views/main/dashboard/widgets/tasktracker_live_test');
require('test/views/main/dashboard/widgets/hbase_average_load_test');
require('test/views/main/dashboard/widgets/hbase_regions_in_transition_test');
require('test/views/main/dashboard/widgets/jobtracker_rpc_test');
require('test/views/main/dashboard/widgets/namenode_rpc_test');
require('test/views/main/dashboard/widgets/hbase_master_uptime_test');
require('test/views/main/dashboard/widgets/jobtracker_uptime_test');
require('test/views/main/dashboard/widgets/namenode_uptime_test');
require('test/views/main/dashboard/widgets/resource_manager_uptime_test');
require('test/views/main/dashboard/widgets/links_widget_test');
require('test/views/main/dashboard/widgets/pie_chart_widget_test');
require('test/views/main/dashboard/widgets/namenode_cpu_test');
require('test/views/main/host/summary_test');
require('test/views/main/host/details/host_component_view_test');
require('test/views/main/host/details/host_component_views/decommissionable_test');
require('test/views/main/jobs/hive_job_details_tez_dag_view_test');
require('test/views/main/charts/heatmap/heatmap_host_test');
require('test/views/main/charts/heatmap/heatmap_rack_test');
require('test/views/main/service/info/config_test');
require('test/views/common/configs/services_config_test');
require('test/views/wizard/step1_view_test');
require('test/views/wizard/step3_view_test');
require('test/views/wizard/step9_view_test');
require('test/models/host_test');
require('test/models/host_component_test');
require('test/models/rack_test');
