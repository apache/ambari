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
App.db = {};

if (typeof Storage !== 'undefined') {
  Storage.prototype.setObject = function (key, value) {
    this.setItem(key, JSON.stringify(value));
  }

  Storage.prototype.getObject = function (key) {
    var value = this.getItem(key);
    return value && JSON.parse(value);
  }
} else {
  // stub for unit testing purposes
  window.localStorage = {};
  localStorage.setItem = function (key, val) {
    this[key] = val;
  }
  localStorage.getItem = function (key) {
    return this[key];
  }
  window.localStorage.setObject = function (key, value) {
    this[key] = value;
  };
  window.localStorage.getObject = function (key, value) {
    return this[key];
  };
}

App.db.cleanUp = function () {
  console.log('TRACE: Entering db:cleanup function');
  App.db.data = {
    'app': {
      'loginName': '',
      'authenticated': false
    },
    'Installer' : {},
    'AddHost' : {},
    'AddService' : {}
  };
  console.log("In cleanup./..");
  localStorage.setObject('ambari', App.db.data);
};

// called whenever user logs in
if (localStorage.getObject('ambari') == null) {
  console.log('doing a cleanup');
  App.db.cleanUp();
}

/*
 * setter methods
 */

App.db.setLoginName = function (name) {
  console.log('TRACE: Entering db:setLoginName function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.loginName = name;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setAmbariStacks = function (stacks) {
  console.log('TRACE: Entering db:setAmbariStacks function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.stacks = stacks;
  localStorage.setObject('ambari', App.db.data);
};

/**
 * Set user model to db
 * @param user
 */
App.db.setUser = function (user) {
  console.log('TRACE: Entering db:setUser function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.user = user;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setAuthenticated = function (authenticated) {
  console.log('TRACE: Entering db:setAuthenticated function');

  App.db.data = localStorage.getObject('ambari');
  console.log('present value of authentication is: ' + App.db.data.app.authenticated);
  console.log('desired value of authentication is: ' + authenticated);
  App.db.data.app.authenticated = authenticated;
  localStorage.setObject('ambari', App.db.data);
  App.db.data = localStorage.getObject('ambari');
  console.log('Now present value of authentication is: ' + App.db.data.app.authenticated);
};

App.db.setAllHostNames = function (hostNames) {
  console.log('TRACE: Entering db:setAllHostNames function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.hostNames = hostNames;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setAllHostNamesPattern = function (hostNames) {
  console.log('TRACE: Entering db:setAllHostNamesPattern function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.hostNamesPattern = hostNames;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHosts = function (hostInfo) {
  console.log('TRACE: Entering db:setHosts function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.hostInfo = hostInfo;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setInstallOptions = function(installOptions){
  console.log('TRACE: Entering db:setInstallOptions function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.installOptions = installOptions;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setBootStatus = function (status) {
  console.log('TRACE: Entering db:setBootStatus function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.bootStatus = status;
  localStorage.setObject('ambari', App.db.data);
};

App.db.removeHosts = function (hostInfo) {
  console.log('TRACE: Entering db:setSoftRepo function');
  var hostList = App.db.getHosts();
  hostInfo.forEach(function (_hostInfo) {
    var host = _hostInfo.hostName;
    delete hostList[host];
  });
  App.db.setHosts(hostList);
};

App.db.setService = function (serviceInfo) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.serviceInfo = serviceInfo;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSelectedServiceNames = function (serviceNames) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.selectedServiceNames = serviceNames;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setClientsForSelectedServices = function (clientInfo) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.clientInfo = clientInfo;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setMasterComponentHosts = function (masterComponentHosts) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.masterComponentHosts = masterComponentHosts;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSlaveComponentHosts = function (slaveComponentHosts) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.slaveComponentHosts = slaveComponentHosts;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSlaveProperties = function (slaveProperties) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.slaveProperties = slaveProperties;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setServiceConfigs = function (serviceConfigs) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.serviceConfigs = serviceConfigs;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setAdvancedServiceConfig = function(serviceConfigs) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.advanceServiceConfigs = serviceConfigs;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setServiceConfigProperties = function (configProperties) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.configProperties = configProperties;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setCluster = function (status) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.clusterStatus = status;
  console.log('db.setClusterStatus called: ' + JSON.stringify(status));
  localStorage.setObject('ambari', App.db.data);
};

/**
 * Set current step value for specified Wizard Type
 * @param wizardType
 * @param currentStep
 */
App.db.setWizardCurrentStep = function (wizardType, currentStep) {
  console.log('TRACE: Entering db:setWizardCurrentStep function');

  App.db.data[wizardType.capitalize()].currentStep = currentStep;

  localStorage.setObject('ambari', App.db.data);
};


/*
 *  getter methods
 */

/**
 * Get user model from db
 * @return {*}
 */
App.db.getUser = function () {
  console.log('TRACE: Entering db:getUser function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.user;
};

App.db.getLoginName = function () {
  console.log('Trace: Entering db:getLoginName function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.loginName;
};

App.db.getAuthenticated = function () {
  console.log('Trace: Entering db:getAuthenticated function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.authenticated;
};

App.db.getAmbariStacks = function () {
  console.log('TRACE: Entering db:setAmbariStacks function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.stacks;
};

/**
 * Return current step for specified Wizard Type
 * @param wizardType
 * @return {*}
 */
App.db.getWizardCurrentStep = function (wizardType) {
  console.log('Trace: Entering db:getWizardCurrentStep function for ', wizardType);
  if (App.db.data[wizardType.capitalize()]) {
    return App.db.data[wizardType.capitalize()].currentStep;
  }
  return 0;
};

App.db.getAllHostNames = function () {
  console.log('TRACE: Entering db:getHostNames function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.hostNames;
};

App.db.getAllHostNamesPattern = function () {
  console.log('TRACE: Entering db:getHostNamesPattern function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.hostNamesPattern;
};
App.db.getInstallOptions = function () {
  console.log('TRACE: Entering db:getInstallOptions function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.installOptions;
};

App.db.isCompleted = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.completed;
};

App.db.getHosts = function () {
  console.log('TRACE: Entering db:getHosts function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.hostInfo;
};

App.db.getBootStatus = function () {
  console.log('TRACE: Entering db:getBootStatus function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.bootStatus;
};

App.db.getService = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.serviceInfo;
};

App.db.getSelectedServiceNames = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.selectedServiceNames;
};

App.db.getClientsForSelectedServices = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.clientInfo;
};

App.db.getMasterComponentHosts = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.masterComponentHosts;
};

App.db.getSlaveComponentHosts = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.slaveComponentHosts;
};

App.db.getServiceConfigs = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.serviceConfigs;
};

App.db.getAdvancedServiceConfig = function() {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.advanceServiceConfigs;
};

App.db.getServiceConfigProperties = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.configProperties;
};

App.db.getSlaveProperties = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.slaveProperties;
};

App.db.getCluster = function () {
  console.log('TRACE: Entering db:getClusterStatus function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.clusterStatus;
};

module.exports = App.db;
