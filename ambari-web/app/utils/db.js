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
var InitialData =  {
  'app': {
    'loginName': '',
    'authenticated': false,
    'configs': [],
    'tables': {
      'filterConditions': {},
      'displayLength': {},
      'startIndex': {},
      'sortingConditions': {},
      'selectedItems': {}
    }
  },

  'Installer' : {},
  'AddHost' : {},
  'AddService' : {},
  'WidgetWizard' : {},
  'KerberosWizard': {},
  'ReassignMaster' : {},
  'AddSecurity': {},
  'AddAlertDefinition': {
    content: {}
  },
  'HighAvailabilityWizard': {},
  'RMHighAvailabilityWizard': {},
  'AddHawqStandbyWizard': {},
  'RemoveHawqStandbyWizard': {},
  'ActivateHawqStandbyWizard': {},
  'RAHighAvailabilityWizard': {},
  'RollbackHighAvailabilityWizard': {},
  'MainAdminStackAndUpgrade': {},
  'KerberosDisable': {},
  'tmp': {}

};

if (typeof Storage !== 'undefined') {
  Storage.prototype.setObject = function (key, value) {
    this.setItem(key, JSON.stringify(value));
  };

  Storage.prototype.getObject = function (key) {
    var value = this.getItem(key);
    return value && JSON.parse(value);
  };
} else {
  // stub for unit testing purposes
  window.localStorage = {};
  localStorage.setItem = function (key, val) {
    this[key] = val;
  };
  localStorage.getItem = function (key) {
    return this[key];
  };
  window.localStorage.setObject = function (key, value) {
    this[key] = value;
  };
  window.localStorage.getObject = function (key, value) {
    return this[key];
  };
}

App.db.cleanUp = function () {
  console.log('TRACE: Entering db:cleanup function');
  App.db.data = InitialData;
  console.log("In cleanup./..");
  localStorage.setObject('ambari', App.db.data);
};

App.db.cleanTmp = function() {
  App.db.data.tmp = {};
  localStorage.setObject('ambari', App.db.data);
};

App.db.cleanAddHost = function() {
  App.db.data.AddHost = {};
  localStorage.setObject('ambari', App.db.data);
};

App.db.updateStorage = function() {
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data && App.db.data.app && App.db.data.app.tables && App.db.data.app.configs) {
    return true;
  }
  console.warn("local storage is deprecated!");
  App.db.cleanUp();
  return false;
};

/*
  Initialize wizard namespaces if they are not initialized on login.
  This will be required during upgrade.
 */
App.db.mergeStorage = function() {
  if (localStorage.getObject('ambari') == null) {
    console.log('doing a cleanup');
    App.db.cleanUp();
  } else {
    localStorage.setObject('ambari', $.extend(true,{}, InitialData, App.db.data));
  }
};

// called whenever user logs in
if (localStorage.getObject('ambari') == null) {
  console.log('doing a cleanup');
  App.db.cleanUp();
}

App.db.get = function (namespace, key) {
  console.log('TRACE: Entering db:get' + key);
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data[namespace]) {
    App.db.data[namespace] = {};
  }
  return App.db.data[namespace][key];
};

/**
 *
 * @param {string} namespace
 * @param {string[]} listOfProperties
 * @returns {object}
 */
App.db.getProperties = function (namespace, listOfProperties) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data[namespace]) {
    App.db.data[namespace] = {};
  }
  var ret = {};
  listOfProperties.forEach(function (k) {
    ret[k] = App.db.data[namespace][k];
  });
  return ret;
};

App.db.set = function (namespace, key, value) {
  console.log('TRACE: Entering db:set' + key + ';value: ', value);
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data[namespace]) {
    App.db.data[namespace] = {};
  }
  App.db.data[namespace][key] = value;
  localStorage.setObject('ambari', App.db.data);
};

/**
 *
 * @param {string} namespace
 * @param {{key: value}} hash
 */
App.db.setProperties = function (namespace, hash) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data[namespace]) {
    App.db.data[namespace] = {};
  }
  for (var k in hash) {
    if (hash.hasOwnProperty(k)) {
      App.db.data[namespace][k] = hash[k];
    }
  }
  localStorage.setObject('ambari', App.db.data);
};
/*
 * setter methods
 */

App.db.setLoginName = function (name) {
  console.log('TRACE: Entering db:setLoginName function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.loginName = name;
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

App.db.setFilterConditions = function(name, filterConditions) {
  console.log('TRACE: Entering db:setFilterConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.filterConditions) {
    App.db.data.app.tables.filterConditions = {};
  }
  App.db.data.app.tables.filterConditions[name] = filterConditions;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setDisplayLength = function(name, displayLength) {
  console.log('TRACE: Entering db:setDisplayLength function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.displayLength) {
    App.db.data.app.tables.displayLength = {};
  }
  App.db.data.app.tables.displayLength[name] = displayLength;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setStartIndex = function(name, startIndex) {
  console.log('TRACE: Entering db:setStartIndex function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.startIndex) {
    App.db.data.app.tables.startIndex = {};
  }
  App.db.data.app.tables.startIndex[name] = startIndex;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSortingStatuses = function(name, sortingConditions) {
  console.log('TRACE: Entering db:setSortingConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.sortingConditions) {
    App.db.data.app.tables.sortingConditions = {};
  }
  App.db.data.app.tables.sortingConditions[name] = sortingConditions;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSelectedHosts = function(name, selectedHosts) {
  console.log('TRACE: Entering db:setSelectedHosts function');
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.app.tables.selectedItems) {
    App.db.data.app.tables.selectedItems = {};
  }
  App.db.data.app.tables.selectedItems[name] = selectedHosts;
  localStorage.setObject('ambari', App.db.data);
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

App.db.setInstallOptions = function (installOptions) {
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

App.db.setServiceConfigs = function (serviceConfigs) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.serviceConfigs = serviceConfigs;
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

App.db.setUpgradeOptions = function (upgradeOptions) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.StackUpgrade.upgradeOptions = upgradeOptions;
  console.log('db.setUpgradeOptions called: ' + JSON.stringify(upgradeOptions));
  localStorage.setObject('ambari', App.db.data);
};

App.db.setMasterToReassign = function (masterComponent) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.ReassignMaster.masterComponent = masterComponent;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setReassignTasksStatuses = function (tasksStatuses) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.ReassignMaster.tasksStatuses = tasksStatuses;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setReassignTasksRequestIds = function (requestIds) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.ReassignMaster.tasksRequestIds = requestIds;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setStacks = function (stacks) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.Installer.stacksVersions = stacks;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setServices = function (services) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.services = services;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setConfigs = function (configs) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.configs = configs;
  localStorage.setObject('ambari', App.db.data);
};

/**
 * Set current step value for specified Wizard Type
 * @param wizardType
 * @param currentStep
 */
App.db.setWizardCurrentStep = function (wizardType, currentStep) {
  console.log('TRACE: Entering db:setWizardCurrentStep function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data[wizardType.capitalize()].currentStep = currentStep;
  localStorage.setObject('ambari', App.db.data);
};

/**
 * Set localStorage with data from server
 */
App.db.setLocalStorage = function () {
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSecurityWizardStatus = function (status) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  App.db.data.AddSecurity.status = status;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setDisableSecurityStatus = function (status) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  App.db.data.AddSecurity.disableSecurityStatus = status;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSecurityDeployCommands = function (commands) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  App.db.data.AddSecurity.securityDeployCommands = commands;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSecureConfigProperties  = function (secureConfigs) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  App.db.data.AddSecurity.secureConfigProperties = secureConfigs;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setSecureUserInfo  = function (userInfo) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  App.db.data.AddSecurity.secureUserInfo = userInfo;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setIsNameNodeHa = function (haStatus) {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  App.db.data.AddSecurity.haStatus = haStatus;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardConfigTag = function (tag) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard[tag.name] = tag.value;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardHdfsClientHosts = function (hostNames) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard.hdfsClientHostNames = hostNames;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardTasksStatuses = function (tasksStatuses) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard.tasksStatuses = tasksStatuses;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardTasksRequestIds = function (requestIds) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard.tasksRequestIds = requestIds;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardHdfsUser = function (hdfsUser) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard.hdfsUser = hdfsUser;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardRequestIds = function (requestIds) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard.requestIds = requestIds;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setHighAvailabilityWizardNameServiceId = function (nameServiceId) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.HighAvailabilityWizard.nameServiceId = nameServiceId;
  localStorage.setObject('ambari', App.db.data);
};


App.db.setRollBackHighAvailabilityWizardAddNNHost = function (host) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.addNNHost = host;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setRollBackHighAvailabilityWizardSNNHost = function (host) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.sNNHost = host;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setRollBackHighAvailabilityWizardSelectedAddNN = function (host) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.selectedAddNN = host;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setRollBackHighAvailabilityWizardSelectedSNN = function (host) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.selectedSNNH = host;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setRollbackHighAvailabilityWizardTasksStatuses = function (tasksStatuses) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.tasksStatuses = tasksStatuses;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setRollbackHighAvailabilityWizardTasksRequestIds = function (requestIds) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.tasksRequestIds = requestIds;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setRollbackHighAvailabilityWizardRequestIds = function (requestIds) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.RollbackHighAvailabilityWizard.requestIds = requestIds;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setReassignMasterWizardRequestIds = function (requestIds) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.ReassignMaster.requestIds = requestIds;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setReassignMasterWizardComponentDir = function (componentDir) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.ReassignMaster.componentDir = componentDir;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setReassignMasterWizardReassignHosts = function (reassignHosts) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.ReassignMaster.reassignHosts = reassignHosts;
  localStorage.setObject('ambari', App.db.data);
};

App.db.setKerberosWizardConfigTag = function (tag) {
  App.db.data = localStorage.getObject('ambari');
  App.db.data.KerberosWizard[tag.name] = tag.value;
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
  return Boolean(App.db.data.app.authenticated);
};

App.db.getFilterConditions = function(name) {
  console.log('TRACE: Entering db:getFilterConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.filterConditions[name]) {
    return App.db.data.app.tables.filterConditions[name];
  }
  return null;
};

App.db.getDisplayLength = function(name) {
  console.log('TRACE: Entering db:getDisplayLength function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.displayLength[name]) {
    return App.db.data.app.tables.displayLength[name];
  }
  return null;
};

App.db.getStartIndex = function(name) {
  console.log('TRACE: Entering db:getStartIndex function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.startIndex[name]) {
    return App.db.data.app.tables.startIndex[name];
  }
  return null;
};

App.db.getSortingStatuses = function(name) {
  console.log('TRACE: Entering db:getSortingConditions function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.sortingConditions[name]) {
    return App.db.data.app.tables.sortingConditions[name];
  }
  return null;
};

App.db.getSelectedHosts = function(name) {
  console.log('TRACE: Entering db:getSelectedHosts function');
  App.db.data = localStorage.getObject('ambari');
  if (App.db.data.app.tables.selectedItems[name]) {
    return App.db.data.app.tables.selectedItems[name];
  }
  return [];
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

App.db.getServiceConfigProperties = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.configProperties;
};

App.db.getCluster = function () {
  console.log('TRACE: Entering db:getClusterStatus function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.clusterStatus;
};

App.db.getMasterToReassign = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.masterComponent;
};

App.db.getReassignTasksStatuses = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.tasksStatuses;
};

App.db.getReassignTasksRequestIds = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.tasksRequestIds;
};

App.db.getUpgradeOptions = function () {
  console.log('TRACE: Entering db:getUpgradeOptions function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.StackUpgrade.upgradeOptions;
};

App.db.getSecurityWizardStatus = function () {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  return App.db.data.AddSecurity.status;
};

App.db.getDisableSecurityStatus = function () {
  App.db.data = localStorage.getObject('ambari');
  if (!App.db.data.AddSecurity) {
    App.db.data.AddSecurity = {};
  }
  return App.db.data.AddSecurity.disableSecurityStatus;
};

App.db.getSecurityDeployCommands = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.AddSecurity.securityDeployCommands;
};

App.db.getSecureConfigProperties = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.AddSecurity.secureConfigProperties;
};

App.db.getSecureUserInfo  = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.AddSecurity.secureUserInfo;
};

App.db.getIsNameNodeHa = function (haStatus) {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.AddSecurity.haStatus;
};

App.db.getStacks = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.Installer.stacksVersions;
};

App.db.getServices = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.services;
};

App.db.getHighAvailabilityWizardHdfsUser = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.hdfsUser;
};

App.db.getHighAvailabilityWizardTasksStatuses = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.tasksStatuses;
};

App.db.getHighAvailabilityWizardTasksRequestIds = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.tasksRequestIds;
};

App.db.getHighAvailabilityWizardFailedTask = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.failedTask;
};

App.db.getHighAvailabilityWizardHdfsClientHosts = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.hdfsClientHostNames;
};

App.db.getHighAvailabilityWizardConfigTag = function (tag) {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard[tag];
};

App.db.getHighAvailabilityWizardRequestIds = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.requestIds;
};

App.db.getHighAvailabilityWizardNameServiceId = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.HighAvailabilityWizard.nameServiceId;
};

App.db.getRollbackHighAvailabilityWizardTasksStatuses = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.RollbackHighAvailabilityWizard.tasksStatuses;
};

App.db.getRollbackHighAvailabilityWizardTasksRequestIds = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.tasksRequestIds;
};

App.db.getRollbackHighAvailabilityWizardRequestIds = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.RollbackHighAvailabilityWizard.requestIds;
};

App.db.getRollBackHighAvailabilityWizardAddNNHost = function (host) {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.RollbackHighAvailabilityWizard.addNNHost;
};

App.db.getRollBackHighAvailabilityWizardSNNHost = function (host) {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.RollbackHighAvailabilityWizard.sNNHost;
};

App.db.getReassignMasterWizardRequestIds = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.requestIds;
};

App.db.getReassignMasterWizardComponentDir = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.componentDir;
};

App.db.getConfigs = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.configs;
};

App.db.getReassignMasterWizardReassignHosts = function () {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.ReassignMaster.reassignHosts;
};

App.db.getKerberosWizardConfigTag = function (tag) {
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.KerberosWizard[tag];
};

module.exports = App.db;
