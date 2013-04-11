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

App.testMode = false;
if(location.port == '3333'){
  App.testMode = true;
}
App.testModeDelayForActions = 10000;
App.skipBootstrap = false;
App.alwaysGoToInstaller = false;
App.testEnableSecurity = true;                          // By default enable security is tested
App.apiPrefix = '/api/v1';
App.defaultStackVersion = 'HDP-1.2.1';
App.defaultLocalStackVersion = 'HDPLocal-1.2.1';
App.defaultJavaHome = '/usr/jdk/jdk1.6.0_31';
App.addServicesEnabled = false;
// default AJAX timeout
App.timeout = 180000;
// max number of retries for certain AJAX calls
App.maxRetries = 3;
App.bgOperationsUpdateInterval = 6000;
App.componentsUpdateInterval = 6000;
App.contentUpdateInterval = 15000;
App.maxRunsForAppBrowser = 500;
App.pageReloadTime=3600000;

// this is to make sure that IE does not cache data when making AJAX calls to the server
$.ajaxSetup({
  cache: false
});

/**
 * Test Mode values
 */
App.test_hostname = 'hostname';