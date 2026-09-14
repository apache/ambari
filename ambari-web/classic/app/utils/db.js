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
var InitialData = {
  'app': {
    'loginName': '',
    'authenticated': false,
    'configs': [],
    'tags': [],
    'tables': {
      'filterConditions': {},
      'displayLength': {},
      'startIndex': {},
      'sortingConditions': {},
      'selectedItems': {}
    }
  },

  'Installer': {},
  'AddHost': {},
  'AddService': {},
  'WidgetWizard': {},
  'KerberosWizard': {},
  'ReassignMaster': {},
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
  'NameNodeFederationWizard': {},
  'RouterFederationWizard': {},
  'ManageJournalNodeWizard': {},
  'RollbackHighAvailabilityWizard': {},
  'MainAdminStackAndUpgrade': {},
  'KerberosDisable': {},
  'tmp': {}

};

var workflowNamespaces = [
  'Installer',
  'AddHost',
  'AddService',
  'WidgetWizard',
  'KerberosWizard',
  'ReassignMaster',
  'HighAvailabilityWizard',
  'RMHighAvailabilityWizard',
  'AddHawqStandbyWizard',
  'RemoveHawqStandbyWizard',
  'ActivateHawqStandbyWizard',
  'RAHighAvailabilityWizard',
  'NameNodeFederationWizard',
  'RouterFederationWizard',
  'ManageJournalNodeWizard',
  'RollbackHighAvailabilityWizard',
  'MainAdminStackAndUpgrade'
];
var workflowStorageScope = null;
var workflowMemoryStorage = {};
var sensitiveKeyPattern = /(password|secret|private.?key|ssh.?key|credential|token|cookie|keytab)/i;
var sensitiveValueFields = [
  'value', 'property_value', 'propertyvalue', 'savedvalue', 'recommendedvalue',
  'initialvalue', 'defaultvalue', 'currentvalue', 'previousvalue', 'priorvalue',
  'originalvalue', 'uservalue', 'changedvalue', 'newvalue', 'oldvalue',
  'default_value', 'initial_value', 'previous_value', 'recommended_value',
  'new_value', 'old_value', 'confirmpassword', 'confirm_password'
];

function rootNamespace(namespace) {
  return String(namespace || '').split('.')[0];
}

function isWorkflowNamespace(namespace) {
  return workflowNamespaces.contains(rootNamespace(namespace));
}

function workflowStorageKey() {
  return workflowStorageScope ? 'ambari-workflow:' + encodeURIComponent(workflowStorageScope) : null;
}

function loadGlobalStorage() {
  return localStorage.getObject('ambari') || $.extend(true, {}, InitialData);
}

function loadWorkflowStorage() {
  var key = workflowStorageKey();
  return key ? (localStorage.getObject(key) || {}) : {};
}

function loadWorkflowMemory() {
  if (!workflowStorageScope) {
    return {};
  }
  if (!workflowMemoryStorage[workflowStorageScope]) {
    workflowMemoryStorage[workflowStorageScope] = $.extend(true, {}, loadWorkflowStorage());
  }
  return workflowMemoryStorage[workflowStorageScope];
}

function loadCurrentStorage() {
  var data = loadGlobalStorage();
  if (workflowStorageScope) {
    var scopedData = loadWorkflowMemory();
    workflowNamespaces.forEach(function (namespace) {
      data[namespace] = $.extend(true, {}, InitialData[namespace], scopedData[namespace]);
    });
  }
  App.db.data = data;
  return data;
}

function hasUrlCredentials(value) {
  return typeof value === 'string' && /[a-z][a-z0-9+.-]*:\/\/[^\s/@]+@/i.test(value);
}

function isOpaqueCredential(value) {
  return typeof value === 'string' && (
    /-----BEGIN(?: [A-Z0-9]+)* PRIVATE KEY-----/i.test(value) ||
    /["'](?:password|secret|token|credential)["']\s*:/i.test(value)
  );
}

function isSafePathMetadata(property, value, key) {
  if (typeof value !== 'string' || value.charAt(0) !== '/' || /[\n\0]/.test(value)) {
    return false;
  }
  var name = String(property.name || property.propertyName || property.property_name || property.key || key || '').toLowerCase();
  return name.indexOf('keytab') !== -1 || name.indexOf('principal') !== -1;
}

function hasReenteredValue(value) {
  var propertyName = value.name || value.propertyName || value.property_name || value.key || '';
  var mapSensitive = sensitiveKeyPattern.test(String(propertyName));
  return Object.keys(value).some(function (key) {
    var item = value[key];
    var normalizedKey = key.toLowerCase();
    var reentryField = sensitiveValueFields.contains(normalizedKey) ||
      /^(baseurl|base_url|version_url|localrepovdfdata|versiondefinitionsource)$/i.test(key) ||
      sensitiveKeyPattern.test(key) || (mapSensitive && normalizedKey === 'value');
    return reentryField && item !== undefined && item !== null &&
      (typeof item !== 'string' || item.trim().length > 0);
  });
}

function clearResolvedReentryMarkers(value) {
  if (Array.isArray(value)) {
    return value.map(clearResolvedReentryMarkers);
  }
  if (!value || typeof value !== 'object') {
    return value;
  }
  var result = {};
  Object.keys(value).forEach(function (key) {
    if (key !== 'requires_reentry' || !hasReenteredValue(value)) {
      result[key] = clearResolvedReentryMarkers(value[key]);
    }
  });
  return result;
}

function containsReentryMarker(value) {
  if (!value || typeof value !== 'object') {
    return false;
  }
  if (!Array.isArray(value) && value.requires_reentry === true) {
    return true;
  }
  return Object.keys(value).some(function (key) {
    return containsReentryMarker(value[key]);
  });
}

function sanitizeWorkflowData(value, sensitiveContext, ancestors) {
  if (value === null || value === undefined) {
    return value;
  }
  if (typeof value !== 'object') {
    return sensitiveContext || isOpaqueCredential(value) ? undefined : value;
  }
  ancestors = ancestors || [];
  if (ancestors.contains(value)) {
    return undefined;
  }
  var nextAncestors = ancestors.concat([value]);
  if (Array.isArray(value)) {
    if (sensitiveContext) {
      return [];
    }
    return value.map(function (item) {
      return sanitizeWorkflowData(item, false, nextAncestors);
    }).filter(function (item) {
      return item !== undefined;
    });
  }

  var propertyName = value.name || value.propertyName || value.property_name || value.key || '';
  var attributes = value.propertyAttributes || value.property_attributes || value.property_value_attributes || {};
  var mapSensitive = sensitiveContext || sensitiveKeyPattern.test(String(propertyName)) || String(attributes.type || '').toLowerCase() === 'password';
  var result = {};
  var redacted = false;
  Object.keys(value).forEach(function (key) {
    var item = value[key];
    var normalizedKey = key.toLowerCase();
    var sensitiveKey = sensitiveKeyPattern.test(key);
    var opaqueSource = /^(versiondefinitionsource|localrepovdfdata)$/i.test(key);
    var safePath = isSafePathMetadata(value, item, key);
    var redact = (mapSensitive && sensitiveValueFields.contains(normalizedKey) && !safePath) ||
      (sensitiveKey && (item === null || typeof item !== 'object') && !safePath) ||
      (/(baseurl|defaulturl|version_url|base_url|default_base_url)/i.test(key) && hasUrlCredentials(item)) ||
      (opaqueSource && (typeof item === 'string' || (item && item.type === 'xml')));
    if (redact) {
      redacted = true;
      return;
    }
    var sanitized = sanitizeWorkflowData(item, sensitiveKey && !safePath, nextAncestors);
    if (sanitized === undefined) {
      redacted = true;
    } else {
      result[key] = sanitized;
    }
  });
  if (redacted) {
    result.requires_reentry = true;
  }
  return result;
}

function persistWorkflowNamespace(namespace) {
  var scopedData = loadWorkflowStorage();
  var memoryData = loadWorkflowMemory();
  var root = rootNamespace(namespace);
  memoryData[root] = $.extend(true, {}, clearResolvedReentryMarkers(App.db.data[root]));
  scopedData[root] = sanitizeWorkflowData(App.db.data[root], false);
  localStorage.setObject(workflowStorageKey(), scopedData);
}

function persistCurrentStorage(namespace) {
  if (workflowStorageScope && isWorkflowNamespace(namespace)) {
    persistWorkflowNamespace(namespace);
    return;
  }
  var globalData = loadGlobalStorage();
  if (namespace) {
    var root = rootNamespace(namespace);
    globalData[root] = App.db.data[root];
  } else {
    Object.keys(App.db.data).forEach(function (key) {
      if (!isWorkflowNamespace(key)) {
        globalData[key] = App.db.data[key];
      }
    });
  }
  localStorage.setObject('ambari', globalData);
}

function checkNamespace(namespace) {
  if (!namespace) {
    return false;
  }
  if (Em.isNone(Em.get(App.db.data, namespace))) {
    Em.setFullPath(App.db.data, namespace, {});
  }
  return true;
}

if (typeof Storage === 'undefined') {
  // stub for unit testing purposes
  window.localStorage = {};
  localStorage.setItem = function (key, val) {
    this[key] = val;
  };
  localStorage.getItem = function (key) {
    return this[key];
  };
  localStorage.removeItem = function (key) {
    delete this[key];
  };
  window.localStorage.setObject = function (key, value) {
    this[key] = value;
  };
  window.localStorage.getObject = function (key) {
    return this[key];
  };
}
else {
  Storage.prototype.setObject = function (key, value) {
    this.setItem(key, JSON.stringify(value));
  };

  Storage.prototype.getObject = function (key) {
    var value = this.getItem(key);
    return value && JSON.parse(value);
  };
}

App.db.cleanUp = function () {
  App.db.data = $.extend(true, {}, InitialData);
  localStorage.setObject('ambari', App.db.data);
  if (workflowStorageScope) {
    localStorage.removeItem(workflowStorageKey());
    delete workflowMemoryStorage[workflowStorageScope];
  }
};

App.db.cleanTmp = function () {
  App.db.data.tmp = {};
  persistCurrentStorage('tmp');
};

App.db.updateStorage = function () {
  loadCurrentStorage();
  if (Em.get(App, 'db.data.app.tables') && Em.get(App, 'db.data.app.configs')) {
    return true;
  }
  App.db.cleanUp();
  return false;
};

/*
 Initialize wizard namespaces if they are not initialized on login.
 This will be required during upgrade.
 */
App.db.mergeStorage = function () {
  if (localStorage.getObject('ambari') == null) {
    App.db.cleanUp();
  } else {
    App.db.data = $.extend(true, {}, InitialData, App.db.data);
    persistCurrentStorage();
  }
};

// called whenever user logs in
if (localStorage.getObject('ambari') == null) {
  App.db.cleanUp();
}

/**
 *
 * @param {string} namespace
 * @param {string} key
 * @returns {*}
 */
App.db.get = function (namespace, key) {
  loadCurrentStorage();
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  if (key.contains('user-pref')) {
    // username may contain "." which is the part of "user-pref-*" key so Em.set should be avoided
    return Em.get(App.db.data, namespace)[key];
  }
  return Em.get(Em.get(App.db.data, namespace), key);
};

/**
 *
 * @param {string} namespace
 * @param {string[]} listOfProperties
 * @returns {object}
 */
App.db.getProperties = function (namespace, listOfProperties) {
  loadCurrentStorage();
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  return Em.getProperties(Em.get(App.db.data, namespace), listOfProperties);
};

/**
 *
 * @param {string} namespace
 * @param {string} key
 * @param {*} value
 */
App.db.set = function (namespace, key, value) {
  loadCurrentStorage();
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  if (key.contains('user-pref')) {
    // username may contain "." which is the part of "user-pref-*" key so Em.set should be avoided
    Em.get(App.db.data, namespace)[key] = value;
  } else {
    Em.set(Em.get(App.db.data, namespace), key, value);
  }
  persistCurrentStorage(namespace);
};

/**
 *
 * @param {string} namespace
 * @param {{key: value}} hash
 */
App.db.setProperties = function (namespace, hash) {
  loadCurrentStorage();
  Em.assert('`namespace` should be defined', !!namespace);
  checkNamespace(namespace);
  Em.setProperties(Em.get(App.db.data, namespace), hash);
  persistCurrentStorage(namespace);
};

App.db.setLoginName = function (name) {
  App.db.set('app', 'loginName', name);
};

/**
 * Set user model to db
 * @param user
 */
App.db.setUser = function (user) {
  App.db.set('app', 'user', user);
};

App.db.setAuth = function (auth) {
  App.db.set('app', 'auth', auth);
};

App.db.setAuthenticated = function (authenticated) {
  App.db.set('app', 'authenticated', authenticated);
  loadCurrentStorage();
};

App.db.setFilterConditions = function (name, filterConditions) {
  App.db.set('app.tables.filterConditions', name, filterConditions);
};

App.db.setComboSearchQuery = function (name, query) {
  App.db.set('app.tables.comboSearchQuery', name, query);
};

App.db.setDisplayLength = function (name, displayLength) {
  App.db.set('app.tables.displayLength', name, displayLength);
};

App.db.setStartIndex = function (name, startIndex) {
  App.db.set('app.tables.startIndex', name, startIndex);
};

App.db.setSortingStatuses = function (name, sortingConditions) {
  App.db.set('app.tables.sortingConditions', name, sortingConditions);
};

App.db.setSelectedHosts = function (selectedHosts) {
  App.db.set('app.tables.selectedItems', 'mainHostController', selectedHosts);
};

App.db.unselectHosts = function (hostsToUnselect = []) {
  let selectedHosts = App.db.getSelectedHosts();
  selectedHosts = selectedHosts.filter(host => hostsToUnselect.indexOf(host) === -1);
  App.db.setSelectedHosts(selectedHosts);
};

App.db.setMasterComponentHosts = function (masterComponentHosts) {
  App.db.set('Installer', 'masterComponentHosts', masterComponentHosts);
};

App.db.setMasterToReassign = function (masterComponent) {
  App.db.set('ReassignMaster', 'masterComponent', masterComponent);
};

App.db.setReassignTasksStatuses = function (tasksStatuses) {
  App.db.set('ReassignMaster', 'tasksStatuses', tasksStatuses);
};

App.db.setReassignTasksRequestIds = function (requestIds) {
  App.db.set('ReassignMaster', 'tasksRequestIds', requestIds);
};

App.db.setStacks = function (stacks) {
  App.db.set('Installer', 'stacksVersions', stacks);
};

App.db.setOses = function (oses) {
  App.db.set('Installer', 'operatingSystems', oses);
};

App.db.setRepos = function (repos) {
  App.db.set('Installer', 'repositories', repos);
};

App.db.setLocalRepoVDFData = function (data) {
  App.db.set('Installer', 'localRepoVDFData', data);
};

App.db.setConfigs = function (configs) {
  App.db.set('app', 'configs', configs);
};

App.db.setTags = function (tags) {
  App.db.set('app', 'tags', tags);
};

/**
 * Set current step value for specified Wizard Type
 * @param wizardType
 * @param currentStep
 */
App.db.setWizardCurrentStep = function (wizardType, currentStep) {
  App.db.set(wizardType.capitalize(), 'currentStep', currentStep);
};

/**
 * Set localStorage with data from server
 */
App.db.setLocalStorage = function () {
  persistCurrentStorage();
  if (workflowStorageScope) {
    workflowNamespaces.forEach(persistWorkflowNamespace);
  }
};

App.db.sanitizeWorkflowData = function (value) {
  return sanitizeWorkflowData(value, false);
};

App.db.activateWorkflowScope = function (scope) {
  if (workflowStorageScope && workflowStorageScope !== scope) {
    delete workflowMemoryStorage[workflowStorageScope];
  }
  workflowStorageScope = scope;
  loadCurrentStorage();
};

App.db.deactivateWorkflowScope = function () {
  if (workflowStorageScope) {
    delete workflowMemoryStorage[workflowStorageScope];
  }
  workflowStorageScope = null;
  loadCurrentStorage();
};

App.db.getWorkflowStorageScope = function () {
  return workflowStorageScope;
};

App.db.getWorkflowSnapshot = function (namespaces) {
  loadCurrentStorage();
  return (namespaces || workflowNamespaces).reduce(function (snapshot, namespace) {
    if (App.db.data[namespace] && !$.isEmptyObject(App.db.data[namespace])) {
      snapshot[namespace] = sanitizeWorkflowData(App.db.data[namespace], false);
    }
    return snapshot;
  }, {});
};

App.db.hasUnresolvedWorkflowReentry = function (namespaces) {
  loadCurrentStorage();
  return (namespaces || workflowNamespaces).some(function (namespace) {
    return containsReentryMarker(App.db.data[namespace]);
  });
};

App.db.getLegacyWorkflowSnapshot = function (namespaces) {
  var legacyData = loadGlobalStorage();
  return (namespaces || workflowNamespaces).reduce(function (snapshot, namespace) {
    if (legacyData[namespace] && !$.isEmptyObject(legacyData[namespace])) {
      snapshot[namespace] = sanitizeWorkflowData(legacyData[namespace], false);
    }
    return snapshot;
  }, {});
};

App.db.restoreWorkflowSnapshot = function (snapshot) {
  if (!workflowStorageScope || !snapshot) {
    return;
  }
  var scopedData = loadWorkflowStorage();
  var memoryData = loadWorkflowMemory();
  workflowNamespaces.forEach(function (namespace) {
    if (snapshot[namespace]) {
      var sanitized = sanitizeWorkflowData(snapshot[namespace], false);
      scopedData[namespace] = sanitized;
      memoryData[namespace] = $.extend(true, {}, sanitized);
    }
  });
  localStorage.setObject(workflowStorageKey(), scopedData);
  loadCurrentStorage();
};

App.db.resetWorkflowNamespace = function (namespace) {
  loadCurrentStorage();
  App.db.data[rootNamespace(namespace)] = {};
  persistCurrentStorage(namespace);
};

App.db.setSecurityWizardStatus = function (status) {
  App.db.set('AddSecurity', 'status', status);
};

App.db.setDisableSecurityStatus = function (status) {
  App.db.set('AddSecurity', 'disableSecurityStatus', status);
};

App.db.setSecurityDeployCommands = function (commands) {
  App.db.set('AddSecurity', 'securityDeployCommands', commands);
};

App.db.setHighAvailabilityWizardConfigTag = function (tag) {
  App.db.set('HighAvailabilityWizard', tag.name, tag.value);
};

App.db.setHighAvailabilityWizardHdfsClientHosts = function (hostNames) {
  App.db.set('HighAvailabilityWizard', 'hdfsClientHostNames', hostNames);
};

App.db.setHighAvailabilityWizardTasksStatuses = function (tasksStatuses) {
  App.db.set('HighAvailabilityWizard', 'tasksStatuses', tasksStatuses);
};

App.db.setHighAvailabilityWizardTasksRequestIds = function (requestIds) {
  App.db.set('HighAvailabilityWizard', 'tasksRequestIds', requestIds);
};

App.db.setHighAvailabilityWizardHdfsUser = function (hdfsUser) {
  App.db.set('HighAvailabilityWizard', 'hdfsUser', hdfsUser);
};

App.db.setHighAvailabilityWizardRequestIds = function (requestIds) {
  App.db.set('HighAvailabilityWizard', 'requestIds', requestIds);
};

App.db.setHighAvailabilityWizardNameServiceId = function (nameServiceId) {
  App.db.set('HighAvailabilityWizard', 'nameServiceId', nameServiceId);
};

App.db.setRollBackHighAvailabilityWizardAddNNHost = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'addNNHost', host);
};

App.db.setRollBackHighAvailabilityWizardSNNHost = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'sNNHost', host);
};

App.db.setRollBackHighAvailabilityWizardSelectedAddNN = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'selectedAddNN', host);
};

App.db.setRollBackHighAvailabilityWizardSelectedSNN = function (host) {
  App.db.set('RollbackHighAvailabilityWizard', 'selectedSNNH', host);
};

App.db.setRollbackHighAvailabilityWizardTasksStatuses = function (tasksStatuses) {
  App.db.set('RollbackHighAvailabilityWizard', 'tasksStatuses', tasksStatuses);
};

App.db.setRollbackHighAvailabilityWizardRequestIds = function (requestIds) {
  App.db.set('RollbackHighAvailabilityWizard', 'requestIds', requestIds);
};

App.db.setReassignMasterWizardRequestIds = function (requestIds) {
  App.db.set('ReassignMaster', 'requestIds', requestIds);
};

App.db.setReassignMasterWizardComponentDir = function (componentDir) {
  App.db.set('ReassignMaster', 'componentDir', componentDir);
};

App.db.setReassignMasterWizardReassignHosts = function (reassignHosts) {
  App.db.set('ReassignMaster', 'reassignHosts', reassignHosts);
};

App.db.setKerberosWizardConfigTag = function (tag) {
  App.db.set('KerberosWizard', tag.name, tag.value);
};

App.db.setManageJournalNodeWizardConfigTag = function (tag) {
  App.db.set('ManageJournalNodeWizard', tag.name, tag.value);
};

/**
 * Get user model from db
 * @return {*}
 */
App.db.getUser = function () {
  return App.db.get('app', 'user');
};

App.db.getAuth = function () {
  return App.db.get('app', 'auth');
};

App.db.getLoginName = function () {
  return App.db.get('app', 'loginName');
};

App.db.getAuthenticated = function () {
  return Boolean(App.db.get('app', 'authenticated'));
};

App.db.getFilterConditions = function (name) {
  return name ? App.db.get('app.tables.filterConditions', name) : null;
};

App.db.getComboSearchQuery = function (name) {
  return name ? App.db.get('app.tables.comboSearchQuery', name) : null;
};

App.db.getDisplayLength = function (name) {
  return name ? App.db.get('app.tables.displayLength', name) : null;
};

App.db.getStartIndex = function (name) {
  return name ? App.db.get('app.tables.startIndex', name): null;
};

App.db.getSortingStatuses = function (name) {
  return name ? App.db.get('app.tables.sortingConditions', name): null;
};

App.db.getSelectedHosts = function () {
  return App.db.get('app.tables.selectedItems', 'mainHostController') || [];
};

/**
 * Return current step for specified Wizard Type
 * @param wizardType
 * @return {*}
 */
App.db.getWizardCurrentStep = function (wizardType) {
  return App.db.get(wizardType.capitalize(), 'currentStep') || 0;
};

App.db.getAllHostNames = function () {
  return App.db.get('Installer', 'hostNames');
};

App.db.getMasterToReassign = function () {
  return App.db.get('ReassignMaster', 'masterComponent');
};

App.db.getReassignTasksStatuses = function () {
  return App.db.get('ReassignMaster', 'tasksStatuses');
};

App.db.getReassignTasksRequestIds = function () {
  return App.db.get('ReassignMaster', 'tasksRequestIds');
};

App.db.getSecurityWizardStatus = function () {
  return App.db.get('AddSecurity', 'status');
};

App.db.getDisableSecurityStatus = function () {
  return App.db.get('AddSecurity', 'disableSecurityStatus');
};

App.db.getStacks = function () {
  return App.db.get('Installer', 'stacksVersions');
};

App.db.getOses = function () {
  return App.db.get('Installer', 'operatingSystems');
};

App.db.getRepos = function () {
  return App.db.get('Installer', 'repositories');
};

App.db.getLocalRepoVDFData = function () {
  return App.db.get('Installer', 'localRepoVDFData');
};

App.db.getHighAvailabilityWizardHdfsUser = function () {
  return App.db.get('HighAvailabilityWizard', 'hdfsUser');
};

App.db.getHighAvailabilityWizardTasksStatuses = function () {
  return App.db.get('HighAvailabilityWizard', 'tasksStatuses');
};

App.db.getHighAvailabilityWizardTasksRequestIds = function () {
  return App.db.get('HighAvailabilityWizard', 'tasksRequestIds');
};

App.db.getHighAvailabilityWizardFailedTask = function () {
  return App.db.get('HighAvailabilityWizard', 'failedTask');
};

App.db.getHighAvailabilityWizardHdfsClientHosts = function () {
  return App.db.get('HighAvailabilityWizard', 'hdfsClientHostNames');
};

App.db.getHighAvailabilityWizardConfigTag = function (tag) {
  return App.db.get('HighAvailabilityWizard', tag);
};

App.db.getHighAvailabilityWizardRequestIds = function () {
  return App.db.get('HighAvailabilityWizard', 'requestIds');
};

App.db.getHighAvailabilityWizardNameServiceId = function () {
  return App.db.get('HighAvailabilityWizard', 'nameServiceId');
};

App.db.getRollbackHighAvailabilityWizardTasksStatuses = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'tasksStatuses');
};

App.db.getRollbackHighAvailabilityWizardRequestIds = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'requestIds');
};

App.db.getRollBackHighAvailabilityWizardAddNNHost = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'addNNHost');
};

App.db.getRollBackHighAvailabilityWizardSNNHost = function () {
  return App.db.get('RollbackHighAvailabilityWizard', 'sNNHost');
};

App.db.getReassignMasterWizardRequestIds = function () {
  return App.db.get('ReassignMaster', 'requestIds');
};

App.db.getReassignMasterWizardComponentDir = function () {
  return App.db.get('ReassignMaster', 'componentDir');
};

App.db.getManageJournalNodeWizardConfigTag = function (tag) {
  return App.db.get('ManageJournalNodeWizard', tag);
};

App.db.getConfigs = function () {
  return App.db.get('app', 'configs');
};

App.db.getTags = function () {
  return App.db.get('app', 'tags');
};

App.db.getReassignMasterWizardReassignHosts = function () {
  return App.db.get('ReassignMaster', 'reassignHosts');
};

module.exports = App.db;
