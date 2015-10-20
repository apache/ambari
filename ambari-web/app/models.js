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


// load all models here

require('models/form'); // should be the 1st
require('models/authentication');
require('models/cluster');
require('models/cluster_states');
require('models/hosts');
require('models/stack');
require('models/stack_version/version');
require('models/stack_version/repository_version');
require('models/stack_version/os');
require('models/stack_version/repository');
require('models/operating_system');
require('models/repository');
require('models/stack_service');
require('models/stack_service_component');
require('models/quick_links');
require('models/service');
require('models/service_audit');
require('models/service/hdfs');
require('models/service/yarn');
require('models/service/mapreduce2');
require('models/service/hbase');
require('models/service/flume');
require('models/service/storm');
require('models/alerts/alert_definition');
require('models/alerts/alert_instance');
require('models/alerts/alert_instance_local');
require('models/alerts/alert_notification');
require('models/alerts/alert_config');
require('models/alerts/alert_group');
require('models/user');
require('models/host');
require('models/rack');
require('models/background_operation');
require('models/client_component');
require('models/host_component');
require('models/target_cluster');
require('models/slave_component');
require('models/master_component');
require('models/config_group');
require('models/host_stack_version');
require('models/root_service');
require('models/upgrade_entity');
require('models/configs/theme/theme_condition');
require('models/configs/service_config_version');
require('models/configs/stack_config_property');
require('models/configs/config_group');
require('models/configs/config_version');
require('models/configs/config_property');
require('models/configs/theme/tab');
require('models/configs/theme/section');
require('models/configs/theme/sub_section');
require('models/configs/theme/sub_section_tab');
require('models/configs/objects/service_config');
require('models/configs/objects/service_config_category');
require('models/configs/objects/service_config_property');
require('models/widget');
require('models/widget_property');
require('models/widget_layout');