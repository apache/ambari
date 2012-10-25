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
require('controllers/installer');
require('controllers/installer/step1_controller');
require('controllers/installer/step2_controller');
require('controllers/installer/step3_controller');
require('controllers/installer/step4_controller');
require('controllers/installer/step6_controller');
require('controllers/installer/step5_controller');
require('controllers/installer/step6_controller');
require('controllers/installer/step7_controller');
require('controllers/installer/step8_controller');
require('controllers/installer/step9_controller');
require('controllers/installer/step10_controller');
require('controllers/main');
require('controllers/main/admin');
require('controllers/main/admin/item');
require('controllers/main/admin/user');
require('controllers/main/admin/user/edit');
require('controllers/main/admin/advanced');
require('controllers/main/admin/authentication');
require('controllers/main/service');
require('controllers/main/service/item');
require('controllers/main/service/info/summary');
require('controllers/main/service/info/metrics');
require('controllers/main/service/info/configs');
require('controllers/main/service/info/audit');
require('controllers/main/alert');
require('controllers/main/host');
require('controllers/main/host/details');
require('controllers/main/host/add_controller');
require('controllers/main/dashboard');
require('controllers/main/charts');
require('controllers/main/charts/heatmap');
require('controllers/main/charts/horizon_chart');
require('controllers/main/charts/horizon_chart');
require('controllers/main/rack');
require('controllers/main/apps_controller');
require('controllers/main/apps/item_controller');
require('controllers/wizard/step2_controller');
require('controllers/wizard/step3_controller');
require('controllers/wizard/step4_controller');
require('controllers/wizard/step5_controller');
require('controllers/wizard/step6_controller');