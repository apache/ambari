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

App.version = ''; // filled out by set-ambari-version.sh script
App.testMode = (location.port == '3333'); // test mode is automatically enabled if running on brunch server
App.testModeDelayForActions = 10000;
App.skipBootstrap = false;
App.alwaysGoToInstaller = false;
App.testEnableSecurity = true; // By default enable security is tested; turning it false tests disable security
App.testNameNodeHA = true;
App.apiPrefix = '/api/v1';
App.defaultStackVersion = 'HDP-2.3';
App.defaultWindowsStackVersion = 'HDPWIN-2.1';

App.defaultJavaHome = '/usr/jdk/jdk1.6.0_31';
App.timeout = 180000; // default AJAX timeout
App.maxRetries = 3; // max number of retries for certain AJAX calls
App.sessionKeepAliveInterval  = 60000;
App.bgOperationsUpdateInterval = 6000;
App.componentsUpdateInterval = 6000;
App.contentUpdateInterval = 15000;
App.hostStatusCountersUpdateInterval = 10000;
App.alertDefinitionsUpdateInterval = 10000;
App.alertInstancesUpdateInterval = 10000;
App.alertGroupsUpdateInterval = 10000;
App.clusterEnvUpdateInterval = 10000;
App.pageReloadTime = 3600000;
App.nnCheckpointAgeAlertThreshold = 12; // in hours
App.singleNodeInstall = false;
App.singleNodeAlias = document.location.hostname;
App.minDiskSpace = 2.0; // minimum disk space required for '/' for each host before install, unit GB
App.minDiskSpaceUsrLib = 1.0; // minimum disk space for '/usr/lib' for each host before install, unit GB
App.healthIconClassGreen = 'icon-ok-sign'; // bootstrap icon class for healthy/started service/host/host-component
App.healthIconClassRed = 'icon-warning-sign'; // bootstrap icon class for master down/stopped service/host/host-component
App.healthIconClassOrange = 'icon-minus-sign'; // bootstrap icon class for slave down/decommissioned host/host-component
App.healthIconClassYellow = 'icon-question-sign'; // bootstrap icon class for heartbeat lost service/host/host-component
App.isManagedMySQLForHiveEnabled = false;
App.isStormMetricsSupported = true;
App.healthStatusRed = '#ff0000';
App.healthStatusGreen = '#5AB400';
App.healthStatusOrange = '#FF8E00';
App.inactivityRemainTime = 60; // in seconds

App.stackVersionsAvailable = true;

// experimental features are automatically enabled if running on brunch server
App.enableExperimental = false;

App.supports = {
  preUpgradeCheck: true,
  displayOlderVersions: false,
  autoRollbackHA: false,
  alwaysEnableManagedMySQLForHive: false,
  preKerberizeCheck: false,
  customizeAgentUserAccount: false,
  installGanglia: false,
  opsDuringRollingUpgrade: false,
  customizedWidgetLayout: false,
  showPageLoadTime: false,
  skipComponentStartAfterInstall: false,
  preInstallChecks: false,
  hostComboSearchBox: true,
  serviceAutoStart: false,
  logSearch: true,
  redhatSatellite: false,
  enableIpa: false,
  addingNewRepository: false,
  kerberosStackAdvisor: true,
  logCountVizualization: false
};

if (App.enableExperimental) {
  for (var support in App.supports) {
    App.supports[support] = true;
  }
}

// this is to make sure that IE does not cache data when making AJAX calls to the server
if (!$.mocho) {
  $.ajaxSetup({
    cache: false,
    headers: {"X-Requested-By": "X-Requested-By"}
  });
}

/**
 * Test Mode values
 */
App.test_hostname = 'hostname';
