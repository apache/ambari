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
App.apiPrefix = '/api/v1';
App.defaultStackVersion = 'HDP-1.3.0';
App.defaultLocalStackVersion = 'HDPLocal-1.3.0';
App.defaultJavaHome = '/usr/jdk/jdk1.6.0_31';
App.timeout = 180000; // default AJAX timeout
App.maxRetries = 3; // max number of retries for certain AJAX calls
App.bgOperationsUpdateInterval = 6000;
App.componentsUpdateInterval = 6000;
App.contentUpdateInterval = 15000;
App.maxRunsForAppBrowser = 500;
App.pageReloadTime=3600000;

// experimental features are automatically enabled if running on brunch server
App.enableExperimental = false;

App.supports = {
  addServices: false,
  hostOverrides: false,
  mirroring: false,
  secureCluster: false,
  reassignMaster: false,
  stackUpgrade: false,
  capacitySchedulerUi: false,
  startStopAllServices: false,
  hiveOozieExtraDatabases: true,
  multipleHBaseMasters: false,
  addMasters: false,
  customizeSmokeTestUser: true,
  hue: false,
  ldapGroupMapping: false
};

if (App.enableExperimental) {
  for (var support in App.supports) {
    App.supports[support] = true;
  }
}

// this is to make sure that IE does not cache data when making AJAX calls to the server
$.ajaxSetup({
  cache: false
});

/**
 * Test Mode values
 */
App.test_hostname = 'hostname';
