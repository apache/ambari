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


// load all controllers here

require('controllers/application');
require('controllers/login_controller');
require('controllers/wizard');
require('controllers/installer');
require('controllers/global/background_operations_controller');
require('controllers/main');
require('controllers/main/dashboard');
require('controllers/main/dashboard/config_history_controller');
require('controllers/main/admin');
require('controllers/main/admin/highAvailability_controller');
require('controllers/main/admin/highAvailability/nameNode/wizard_controller');
require('controllers/main/admin/highAvailability/progress_controller');
require('controllers/main/admin/highAvailability/progress_popup_controller');
require('controllers/main/admin/highAvailability/nameNode/rollback_controller');
require('controllers/main/admin/highAvailability/nameNode/step1_controller');
require('controllers/main/admin/highAvailability/nameNode/step2_controller');
require('controllers/main/admin/highAvailability/nameNode/step3_controller');
require('controllers/main/admin/highAvailability/nameNode/step4_controller');
require('controllers/main/admin/highAvailability/nameNode/step5_controller');
require('controllers/main/admin/highAvailability/nameNode/step6_controller');
require('controllers/main/admin/highAvailability/nameNode/step7_controller');
require('controllers/main/admin/highAvailability/nameNode/step8_controller');
require('controllers/main/admin/highAvailability/nameNode/step9_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/step1_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/step2_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/step3_controller');
require('controllers/main/admin/highAvailability/nameNode/rollbackHA/rollback_wizard_controller');
require('controllers/main/admin/highAvailability/resourceManager/wizard_controller');
require('controllers/main/admin/highAvailability/resourceManager/step1_controller');
require('controllers/main/admin/highAvailability/resourceManager/step2_controller');
require('controllers/main/admin/highAvailability/resourceManager/step3_controller');
require('controllers/main/admin/highAvailability/resourceManager/step4_controller');
require('controllers/main/admin/repositories');
require('controllers/main/admin/stack_upgrade_controller');
require('controllers/main/admin/serviceAccounts_controller');
require('controllers/main/admin/advanced');
require('utils/polling');
require('controllers/main/admin/security');
require('controllers/main/admin/security/security_progress_controller');
require('controllers/main/admin/security/disable');
require('controllers/main/admin/security/add/addSecurity_controller');
require('controllers/main/admin/security/add/step1');
require('controllers/main/admin/security/add/step2');
require('controllers/main/admin/security/add/step3');
require('controllers/main/admin/security/add/step4');
require('controllers/main/admin/authentication');
require('controllers/main/alerts_controller');
require('controllers/main/service');
require('controllers/main/service/item');
require('controllers/main/service/info/summary');
require('controllers/main/service/info/configs');
require('controllers/main/service/info/audit');
require('controllers/main/service/add_controller');
require('controllers/main/service/reassign_controller');
require('controllers/main/service/reassign/step1_controller');
require('controllers/main/service/reassign/step2_controller');
require('controllers/main/service/reassign/step3_controller');
require('controllers/main/service/reassign/step4_controller');
require('controllers/main/service/reassign/step5_controller');
require('controllers/main/service/reassign/step6_controller');
require('controllers/main/service/manage_config_groups_controller');
require('controllers/main/host');
require('controllers/main/host/details');
require('controllers/main/host/configs_service');
require('controllers/main/host/add_controller');
require('controllers/main/host/addHost/step4_controller');
require('controllers/main/charts');
require('controllers/main/charts/heatmap_metrics/heatmap_metric');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_processrun');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_diskspaceused');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_cpuWaitIO');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_memoryused');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_bytesread');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_byteswritten');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_gctime');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_dfs_memHeapUsed');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_mapreduce');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_mapreduce_gctime');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_mapreduce_mapsRunning');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_mapreduce_reducesRunning');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_mapreduce_memHeapUsed');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_yarn');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_gctime');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_memHeapUsed');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_yarn_ResourceUsed');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_hbase');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_readrequest');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_writerequest');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_compactionqueue');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_regions');
require('controllers/main/charts/heatmap_metrics/heatmap_metric_hbase_memstoresize');
require('controllers/main/charts/heatmap');
require('controllers/main/apps_controller');
require('controllers/main/apps/item_controller');
require('controllers/main/mirroring_controller');
require('controllers/main/mirroring/edit_dataset_controller');
require('controllers/main/mirroring/datasets_controller');
require('controllers/main/mirroring/jobs_controller');
require('controllers/main/mirroring/manage_clusters_controller');
require('controllers/main/views_controller');
require('controllers/main/views/details_controller');
require('controllers/wizard/slave_component_groups_controller');
require('controllers/wizard/step0_controller');
require('controllers/wizard/step1_controller');
require('controllers/wizard/step2_controller');
require('controllers/wizard/step3_controller');
require('controllers/wizard/step4_controller');
require('controllers/wizard/step5_controller');
require('controllers/wizard/step6_controller');
require('controllers/wizard/step7_controller');
require('controllers/wizard/step8_controller');
require('controllers/wizard/step9_controller');
require('controllers/wizard/step10_controller');
require('controllers/wizard/stack_upgrade/step1_controller');
require('controllers/wizard/stack_upgrade/step2_controller');
require('controllers/wizard/stack_upgrade/step3_controller');
require('controllers/global/cluster_controller');
require('controllers/global/update_controller');
require('controllers/global/configuration_controller');
