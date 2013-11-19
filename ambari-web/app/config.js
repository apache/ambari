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

App.testMode = (location.port == '3333'); // test mode is automatically enabled if running on brunch server
App.testModeDelayForActions = 10000;
App.skipBootstrap = false;
App.alwaysGoToInstaller = false;
App.testEnableSecurity = true; // By default enable security is tested; turning it false tests disable security
App.testNameNodeHA = true;
App.apiPrefix = '/api/v1';
App.defaultStackVersion = 'HDP-2.0.5';
App.defaultLocalStackVersion = 'HDPLocal-2.0.5';
App.defaultJavaHome = '/usr/jdk/jdk1.6.0_31';
App.timeout = 180000; // default AJAX timeout
App.maxRetries = 3; // max number of retries for certain AJAX calls
App.bgOperationsUpdateInterval = 6000;
App.componentsUpdateInterval = 6000;
App.contentUpdateInterval = 15000;
App.maxRunsForAppBrowser = 500;
App.pageReloadTime=3600000;
App.singleNodeInstall = false;
App.singleNodeAlias = document.location.hostname;
App.reassignableComponents = ['NAMENODE', 'SECONDARY_NAMENODE', 'JOBTRACKER', 'RESOURCEMANAGER'];

// experimental features are automatically enabled if running on brunch server
App.enableExperimental = false;

App.supports = {
  addServices: false,
  hostOverrides: false,
  hostOverridesInstaller: false,
  hostOverridesHost: false,
  mirroring: false,
  secureCluster: true,
  secureClusterProceedPopup: false,
  reassignMaster: true,
  stackUpgrade: false,
  capacitySchedulerUi: false,
  startStopAllServices: true,
  hiveOozieExtraDatabases: true,
  multipleHBaseMasters: true,
  addMasters: false,
  customizeSmokeTestUser: true,
  hue: false,
  ldapGroupMapping: false,
  localRepositories: true,
  highAvailability: true,
  deleteHost: true,
  autoRollbackHA: false
};

if (App.enableExperimental) {
  for (var support in App.supports) {
    App.supports[support] = true;
  }
}

// this is to make sure that IE does not cache data when making AJAX calls to the server
$.ajaxSetup({
  cache: false,
  headers: {"X-Requested-By": "X-Requested-By"}
});

/**
 * Test Mode values
 */
App.test_hostname = 'hostname';
