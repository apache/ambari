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
var LZString = require('utils/lz-string');

var CONTROLLERS = {
  installerController: {
    workflow: 'CLUSTER_CREATE',
    namespaces: ['Installer'],
    clusterState: 'CLUSTER_NOT_CREATED_1',
    reentryStep: 2
  },
  addHostController: {
    workflow: 'ADD_HOST',
    namespaces: ['AddHost'],
    clusterState: 'ADD_HOSTS_DEPLOY_PREP_2',
    reentryStep: 2
  },
  addServiceController: {
    workflow: 'ADD_SERVICE',
    namespaces: ['AddService'],
    clusterState: 'ADD_SERVICES_DEPLOY_PREP_2',
    reentryStep: 7
  },
  reassignMasterController: {
    workflow: 'REASSIGN_COMPONENT',
    namespaces: ['ReassignMaster'],
    clusterState: 'DEFAULT',
    reentryStep: 3
  },
  kerberosWizardController: {
    workflow: 'ENABLING_KERBEROS',
    namespaces: ['KerberosWizard'],
    clusterState: 'ADD_SECURITY_STEP_1',
    reentryStep: 2
  },
  highAvailabilityWizardController: {
    workflow: 'HIGH_AVAILIBILITY_NAMENODE',
    namespaces: ['HighAvailabilityWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 2
  },
  rollbackHighAvailabilityWizardController: {
    workflow: 'ROLLBACK_HIGH_AVAILABILITY',
    namespaces: ['RollbackHighAvailabilityWizard'],
    clusterState: 'ROLLBACK_HIGH_AVAILABILITY',
    reentryStep: 1
  },
  rMHighAvailabilityWizardController: {
    workflow: 'HIGH_AVAILIBILITY_RM_HA',
    namespaces: ['RMHighAvailabilityWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 2
  },
  rAHighAvailabilityWizardController: {
    workflow: 'HIGH_AVAILIBILITY_RANGER_HA',
    namespaces: ['RAHighAvailabilityWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 2
  },
  manageJournalNodeWizardController: {
    workflow: 'MANAGE_JOURNALNODES',
    namespaces: ['ManageJournalNodeWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 1
  },
  nameNodeFederationWizardController: {
    workflow: 'NAMENODE_FEDERATION',
    namespaces: ['NameNodeFederationWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 2
  },
  routerFederationWizardController: {
    workflow: 'ROUTER_FEDERATION',
    namespaces: ['RouterFederationWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 2
  },
  addHawqStandbyWizardController: {
    workflow: 'ADD_HAWQ_STANDBY',
    namespaces: ['AddHawqStandbyWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 1
  },
  removeHawqStandbyWizardController: {
    workflow: 'REMOVE_HAWQ_STANDBY',
    namespaces: ['RemoveHawqStandbyWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 1
  },
  activateHawqStandbyWizardController: {
    workflow: 'ACTIVATE_HAWQ_STANDBY',
    namespaces: ['ActivateHawqStandbyWizard'],
    clusterState: 'HIGH_AVAILABILITY_DEPLOY',
    reentryStep: 1
  },
  mainAdminStackAndUpgradeController: {
    workflow: 'UPGRADE',
    namespaces: ['MainAdminStackAndUpgrade'],
    clusterState: 'STACK_UPGRADING'
  },
  widgetWizardController: {
    browserOnly: true,
    namespaces: ['WidgetWizard']
  },
  widgetEditController: {
    browserOnly: true,
    namespaces: ['WidgetWizard']
  }
};

var TAB_ID_KEY = 'ambari.workflow.tab-id';
var DRAFT_ID_PREFIX = 'ambari.workflow.cluster-create.';

function resolved(value) {
  return $.Deferred().resolve(value).promise();
}

function rejected(error) {
  return $.Deferred().reject(error).promise();
}

function legacyPromise(promise) {
  promise.complete = promise.always;
  return promise;
}

function workflowError(code, message) {
  var error = new Error(Em.I18n.t(message));
  error.code = code;
  return error;
}

function responseCode(request) {
  return request && request.responseJSON && request.responseJSON.code;
}

function randomUuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID();
  }
  if (!window.crypto || typeof window.crypto.getRandomValues !== 'function') {
    throw workflowError('WORKFLOW_ID_UNAVAILABLE', 'workflow.persistence.idUnavailable');
  }
  var bytes = new Uint8Array(16);
  window.crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  return Array.prototype.map.call(bytes, function (value, index) {
    var hex = value.toString(16);
    if (hex.length < 2) {
      hex = '0' + hex;
    }
    return ([4, 6, 8, 10].contains(index) ? '-' : '') + hex;
  }).join('');
}

function currentPrincipal() {
  var loginName = App.router && App.router.get && App.router.get('loginName');
  if (loginName) {
    return loginName;
  }
  var data = localStorage.getObject('ambari');
  return data && data.app && data.app.loginName;
}

function tabId() {
  var value = sessionStorage.getItem(TAB_ID_KEY);
  if (!value) {
    value = randomUuid();
    sessionStorage.setItem(TAB_ID_KEY, value);
  }
  return value;
}

function draftId(principal) {
  var key = DRAFT_ID_PREFIX + principal;
  var value = sessionStorage.getItem(key);
  if (!value) {
    value = randomUuid();
    sessionStorage.setItem(key, value);
  }
  return value;
}

function parseLegacyValue(value) {
  if (value === null || value === undefined || typeof value === 'object') {
    return value;
  }
  try {
    return JSON.parse(value);
  } catch (ignore) {
    try {
      return JSON.parse(LZString.decompressFromBase64(value));
    } catch (ignored) {
      return value;
    }
  }
}

function hasMeaningfulSnapshot(snapshot) {
  return Object.keys(snapshot || {}).some(function (namespace) {
    return snapshot[namespace] && !$.isEmptyObject(snapshot[namespace]);
  });
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

function browserScopeKey(principal, scope) {
  return JSON.stringify([principal, scope.type, scope.id, tabId()]);
}

function requestState(scope) {
  return App.ajax.send({
    name: 'persist.scoped.get',
    data: {
      scopeType: scope.type,
      scopeId: scope.id
    }
  });
}

function updateState(scope, data) {
  return App.ajax.send({
    name: 'persist.scoped.put',
    data: {
      scopeType: scope.type,
      scopeId: scope.id,
      workflowState: data
    }
  });
}

function ScopedWorkflowSession(principal, scope, clusterName, controllerName) {
  this.principal = principal;
  this.scope = scope;
  this.clusterName = clusterName || null;
  this.controllerName = controllerName || null;
  this.browserScope = browserScopeKey(principal, scope);
  this.revision = 0;
  this.workflow = 'IDLE';
  this.phase = 'IDLE';
  this.values = {};
  this.loaded = false;
  this.paused = false;
  this.reentryRequired = false;
  this.active = true;
  this.generation = 0;
  this.tail = resolved();
  this.pendingOutputs = [];
}

ScopedWorkflowSession.prototype.isCurrent = function (generation) {
  return this.active && manager.session === this && generation === this.generation &&
    currentPrincipal() === this.principal && App.db.getWorkflowStorageScope() === this.browserScope;
};

ScopedWorkflowSession.prototype.settleOutput = function (output, resolvedState, value) {
  this.pendingOutputs.removeObject(output);
  if (output.state() === 'pending') {
    output[resolvedState ? 'resolve' : 'reject'](value);
  }
};

ScopedWorkflowSession.prototype.invalidateOperations = function () {
  var error = workflowError('WORKFLOW_SCOPE_CHANGED', 'workflow.persistence.scopeChanged');
  this.pendingOutputs.slice().forEach(function (output) {
    if (output.state() === 'pending') {
      output.reject(error);
    }
  });
  this.pendingOutputs = [];
  this.generation++;
  this.tail = resolved();
};

ScopedWorkflowSession.prototype.invalidate = function () {
  this.active = false;
  this.invalidateOperations();
};

ScopedWorkflowSession.prototype.enqueue = function (operation) {
  var self = this;
  var output = $.Deferred();
  var generation = this.generation;
  if (this.paused || !this.active) {
    return rejected(workflowError('WORKFLOW_QUEUE_PAUSED', 'workflow.persistence.queuePaused'));
  }
  this.pendingOutputs.push(output);
  var queued = this.tail.then(function () {
    if (!self.isCurrent(generation)) {
      var staleError = workflowError('WORKFLOW_SCOPE_CHANGED', 'workflow.persistence.scopeChanged');
      self.settleOutput(output, false, staleError);
      return rejected(staleError);
    }
    if (self.paused) {
      var pausedError = workflowError('WORKFLOW_QUEUE_PAUSED', 'workflow.persistence.queuePaused');
      self.settleOutput(output, false, pausedError);
      return rejected(pausedError);
    }
    var request;
    try {
      request = operation(generation);
    } catch (error) {
      request = rejected(error);
    }
    return $.when(request).then(function (value) {
      if (!self.isCurrent(generation)) {
        var staleError = workflowError('WORKFLOW_SCOPE_CHANGED', 'workflow.persistence.scopeChanged');
        self.settleOutput(output, false, staleError);
        return rejected(staleError);
      }
      self.settleOutput(output, true, value);
      return value;
    }, function (error) {
      if (self.isCurrent(generation)) {
        self.paused = true;
      }
      self.settleOutput(output, false, error);
      return rejected(error);
    });
  });
  this.tail = queued.then(function () {
    return undefined;
  }, function () {
    return undefined;
  });
  return output.promise();
};

ScopedWorkflowSession.prototype.applyState = function (state, restoreSnapshot) {
  this.revision = Number(state.revision || 0);
  this.workflow = state.workflow || 'IDLE';
  this.phase = state.phase || 'IDLE';
  this.values = state.values || {};
  this.loaded = true;
  var status = this.values.CLUSTER_CURRENT_STATUS;
  if (!this.controllerName && status && CONTROLLERS[status.wizardControllerName]) {
    this.controllerName = status.wizardControllerName;
  }
  var config = CONTROLLERS[this.controllerName];
  if (restoreSnapshot !== false && status && status.localdb && config) {
    var activeSnapshot = {};
    config.namespaces.forEach(function (namespace) {
      if (status.localdb[namespace]) {
        activeSnapshot[namespace] = $.extend(true, {}, status.localdb[namespace]);
      }
    });
    this.reentryRequired = config.namespaces.some(function (namespace) {
      return containsReentryMarker(activeSnapshot[namespace]);
    });
    if (this.reentryRequired && config.reentryStep) {
      config.namespaces.forEach(function (namespace) {
        if (activeSnapshot[namespace] && containsReentryMarker(activeSnapshot[namespace])) {
          activeSnapshot[namespace].currentStep = config.reentryStep;
        }
      });
    }
    status.localdb = activeSnapshot;
    App.db.restoreWorkflowSnapshot(activeSnapshot);
  }
  return state;
};

ScopedWorkflowSession.prototype.applyStateIfCurrent = function (state, generation, restoreSnapshot) {
  if (!this.isCurrent(generation)) {
    throw workflowError('WORKFLOW_SCOPE_CHANGED', 'workflow.persistence.scopeChanged');
  }
  return this.applyState(state, restoreSnapshot);
};

ScopedWorkflowSession.prototype.load = function () {
  var self = this;
  return this.enqueue(function (generation) {
    return requestState(self.scope).then(function (state) {
      return self.applyStateIfCurrent(state, generation);
    }).then(function (state) {
      if (state.revision === 0 && state.workflow === 'IDLE' && self.scope.type === 'clusters') {
        return self.importLegacyState(generation);
      }
      return state;
    });
  });
};

ScopedWorkflowSession.prototype.ensureLoaded = function (generation) {
  var self = this;
  return this.loaded ? resolved() : requestState(this.scope).then(function (state) {
    return self.applyStateIfCurrent(state, generation);
  });
};

ScopedWorkflowSession.prototype.importLegacyState = function (generation) {
  var self = this;
  return App.ajax.send({
    name: 'persist.get',
    data: {key: 'wizard-data'}
  }).then(function (rawOwner) {
    var owner = parseLegacyValue(rawOwner);
    if (!owner || !owner.userName || !owner.controllerName) {
      return self.rejectUnownedLegacyState();
    }
    var config = CONTROLLERS[owner.controllerName];
    var snapshot = config ? App.db.getLegacyWorkflowSnapshot(config.namespaces) : {};
    if (owner.userName !== self.principal || !config || !hasMeaningfulSnapshot(snapshot)) {
      throw workflowError(
        'CLASSIC_LEGACY_RECOVERY_UNVERIFIED',
        'workflow.persistence.legacyUnverified'
      );
    }
    var status = {
      clusterName: self.clusterName,
      clusterState: config.clusterState,
      wizardControllerName: owner.controllerName,
      localdb: snapshot
    };
    return updateState(self.scope, {
      expected_revision: 0,
      workflow: config.workflow,
      phase: 'LEGACY_RECOVERY',
      values: {
        CLUSTER_CURRENT_STATUS: status,
        wizardData: owner
      }
    }).then(function (state) {
      return self.applyStateIfCurrent(state, generation);
    });
  }, function (request) {
    if (request && request.status === 404) {
      return self.rejectUnownedLegacyState();
    }
    throw request;
  });
};

ScopedWorkflowSession.prototype.rejectUnownedLegacyState = function () {
  if (hasMeaningfulSnapshot(App.db.getLegacyWorkflowSnapshot())) {
    return rejected(workflowError(
      'CLASSIC_LEGACY_RECOVERY_UNVERIFIED',
      'workflow.persistence.legacyOwnerMissing'
    ));
  }
  return {
    revision: this.revision,
    workflow: this.workflow,
    phase: this.phase,
    values: this.values
  };
};

ScopedWorkflowSession.prototype.save = function (controllerName, phase, updates, requireReentry) {
  var self = this;
  var config = CONTROLLERS[controllerName];
  if (!config) {
    return rejected(workflowError('CLASSIC_WORKFLOW_UNSUPPORTED', 'workflow.persistence.unsupported'));
  }
  if (requireReentry && this.reentryRequired) {
    if (App.db.hasUnresolvedWorkflowReentry(config.namespaces)) {
      return rejected(workflowError('WORKFLOW_REENTRY_REQUIRED', 'workflow.persistence.reentryRequired'));
    }
    this.reentryRequired = false;
  }
  return this.enqueue(function (generation) {
    return self.ensureLoaded(generation).then(function () {
      if (self.workflow !== 'IDLE' && self.workflow !== config.workflow) {
        throw workflowError('WORKFLOW_ACTIVE', 'workflow.persistence.active');
      }
      var values = $.extend(true, {}, self.values, updates);
      return updateState(self.scope, {
        expected_revision: self.revision,
        workflow: config.workflow,
        phase: phase || config.workflow,
        values: App.db.sanitizeWorkflowData(values)
      }).then(function (state) {
        return self.applyStateIfCurrent(state, generation, false);
      });
    });
  });
};

ScopedWorkflowSession.prototype.release = function () {
  var self = this;
  return this.enqueue(function (generation) {
    return self.ensureLoaded(generation).then(function () {
      if (self.workflow === 'IDLE') {
        return self;
      }
      return updateState(self.scope, {
        expected_revision: self.revision,
        workflow: 'IDLE',
        phase: 'IDLE',
        values: {}
      }).then(function (state) {
        return self.applyStateIfCurrent(state, generation, false);
      });
    });
  });
};

ScopedWorkflowSession.prototype.retryLoad = function () {
  this.invalidateOperations();
  this.paused = false;
  this.loaded = false;
  return this.load();
};

var manager = {
  session: null,
  resolving: null,
  resolvingPrincipal: null,
  resolvingControllerName: null,
  resolutionGeneration: 0,
  blockedInstallerError: null,

  activateSession: function (session) {
    if (this.session && this.session !== session) {
      this.session.invalidate();
    }
    this.session = session;
    App.db.activateWorkflowScope(session.browserScope);
    return session;
  },

  invalidateSession: function (session) {
    if (!session) {
      return;
    }
    session.invalidate();
    if (this.session === session) {
      this.session = null;
      if (App.db.getWorkflowStorageScope() === session.browserScope) {
        App.db.deactivateWorkflowScope();
      }
    }
  },

  controllerConfig: function (controllerName) {
    return CONTROLLERS[controllerName] || null;
  },

  prepareInstallerScope: function () {
    var principal = currentPrincipal();
    if (!principal) {
      return rejected(workflowError('WORKFLOW_PRINCIPAL_REQUIRED', 'workflow.persistence.signInCreate'));
    }
    if (hasMeaningfulSnapshot(App.db.getLegacyWorkflowSnapshot(['Installer']))) {
      this.blockedInstallerError = workflowError(
        'CLASSIC_LEGACY_RECOVERY_UNVERIFIED',
        'workflow.persistence.installerLegacyBlocked'
      );
      return rejected(this.blockedInstallerError);
    }
    var scope = {type: 'drafts', id: draftId(principal)};
    return resolved(this.activateSession(new ScopedWorkflowSession(principal, scope, null, 'installerController')));
  },

  resolveSession: function (controllerName) {
    var self = this;
    var principal = currentPrincipal();
    if (!principal) {
      return rejected(workflowError('WORKFLOW_PRINCIPAL_REQUIRED', 'workflow.persistence.signInLoad'));
    }
    if (this.blockedInstallerError && controllerName === 'installerController') {
      return rejected(this.blockedInstallerError);
    }
    if (controllerName === 'installerController') {
      if (this.session && this.session.active && this.session.principal === principal &&
          this.session.scope.type === 'drafts' && this.session.controllerName === controllerName) {
        return resolved(this.session);
      }
      if (this.session) {
        this.invalidateSession(this.session);
      }
      this.resolutionGeneration++;
      this.resolving = null;
      this.resolvingPrincipal = null;
      this.resolvingControllerName = null;
      return this.prepareInstallerScope();
    }
    if (this.session && this.session.active && this.session.principal === principal &&
        this.session.scope.type === 'clusters' && (!controllerName || this.session.controllerName === controllerName)) {
      return resolved(this.session);
    }
    if (this.session) {
      this.invalidateSession(this.session);
    }
    if (this.resolving && this.resolvingPrincipal === principal && this.resolvingControllerName === controllerName) {
      return this.resolving;
    }
    var resolutionGeneration = ++this.resolutionGeneration;
    var resolution = $.Deferred();
    this.resolving = resolution.promise();
    this.resolvingPrincipal = principal;
    this.resolvingControllerName = controllerName;
    App.ajax.send({name: 'cluster.load_cluster_name'}).then(function (data) {
      if (resolutionGeneration !== self.resolutionGeneration || currentPrincipal() !== principal) {
        throw workflowError('WORKFLOW_SCOPE_CHANGED', 'workflow.persistence.principalChanged');
      }
      var items = data.items || [];
      if (items.length !== 1) {
        throw workflowError(
          'CLASSIC_SCOPED_TARGET_REQUIRED',
          'workflow.persistence.targetRequired'
        );
      }
      var cluster = items[0].Clusters;
      var session = new ScopedWorkflowSession(principal, {
        type: 'clusters',
        id: String(cluster.cluster_id)
      }, cluster.cluster_name, controllerName);
      self.activateSession(session);
      resolution.resolve(session);
    }, function (error) {
      resolution.reject(error);
    }).fail(function (error) {
      resolution.reject(error);
    }).always(function () {
      if (resolutionGeneration === self.resolutionGeneration) {
        self.resolving = null;
        self.resolvingPrincipal = null;
        self.resolvingControllerName = null;
      }
    });
    return resolution.promise();
  },

  loadCurrent: function (controllerName) {
    return legacyPromise(this.resolveSession(controllerName).then(function (session) {
      return session.load().then(function (state) {
        var savedController = state && state.values && state.values.wizardData && state.values.wizardData.controllerName;
        if (!session.controllerName && savedController && CONTROLLERS[savedController]) {
          session.controllerName = savedController;
        }
        if (controllerName === 'installerController' && state.workflow === 'IDLE') {
          return session.save(controllerName, 'START', {
            wizardData: {
              userName: session.principal,
              controllerName: controllerName
            }
          });
        }
        return state;
      });
    }));
  },

  saveStatus: function (status) {
    var controllerName = status.wizardControllerName || (this.session && this.session.values.wizardData && this.session.values.wizardData.controllerName);
    if (status.clusterState === 'DEFAULT') {
      return this.release();
    }
    return this.resolveSession(controllerName).then(function (session) {
      var config = CONTROLLERS[controllerName];
      var nextStatus = $.extend(true, {}, status, {
        localdb: App.db.getWorkflowSnapshot(config && config.namespaces)
      });
      if (config && config.browserOnly) {
        session.controllerName = controllerName;
        session.loaded = true;
        session.workflow = 'BROWSER_LOCAL';
        session.values = {
          CLUSTER_CURRENT_STATUS: nextStatus,
          wizardData: {
            userName: session.principal,
            controllerName: controllerName
          }
        };
        return session;
      }
      return session.save(controllerName, status.clusterState || 'SAVED', {
        CLUSTER_CURRENT_STATUS: nextStatus,
        wizardData: {
          userName: session.principal,
          controllerName: controllerName
        }
      }, true);
    });
  },

  setUser: function (controllerName) {
    return this.resolveSession(controllerName).then(function (session) {
      var config = CONTROLLERS[controllerName];
      if (config && config.browserOnly) {
        session.controllerName = controllerName;
        session.loaded = true;
        session.workflow = 'BROWSER_LOCAL';
        session.values = {
          wizardData: {
            userName: session.principal,
            controllerName: controllerName
          }
        };
        return session;
      }
      return session.save(controllerName, 'START', {
        wizardData: {
          userName: session.principal,
          controllerName: controllerName
        }
      });
    });
  },

  release: function () {
    var self = this;
    var session = this.session;
    if (!session) {
      return resolved();
    }
    var config = CONTROLLERS[session.controllerName];
    if (config && config.browserOnly) {
      self.invalidateSession(session);
      return resolved(session);
    }
    return session.release().then(function (state) {
      if (session.scope.type === 'drafts') {
        sessionStorage.removeItem(DRAFT_ID_PREFIX + session.principal);
      }
      self.invalidateSession(session);
      return state;
    });
  },

  retryLoad: function () {
    return this.session && this.session.active
      ? legacyPromise(this.session.retryLoad())
      : rejected(workflowError('WORKFLOW_STATE_UNAVAILABLE', 'workflow.persistence.stateUnavailable'));
  },

  getCreationDraftId: function () {
    return this.session && this.session.scope.type === 'drafts' && this.session.workflow === 'CLUSTER_CREATE'
      ? this.session.scope.id
      : null;
  },

  requiresReentry: function () {
    return Boolean(this.session && this.session.active && this.session.reentryRequired);
  },

  clearForTests: function () {
    this.invalidateSession(this.session);
    this.resolving = null;
    this.resolvingPrincipal = null;
    this.resolvingControllerName = null;
    this.resolutionGeneration++;
    this.blockedInstallerError = null;
    App.db.deactivateWorkflowScope();
  },

  errorCode: function (error) {
    return error && (error.code || responseCode(error));
  }
};

App.scopedWorkflowPersistence = manager;
module.exports = manager;
