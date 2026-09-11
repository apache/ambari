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
var scopedWorkflowPersistence = require('utils/scoped_workflow_persistence');
App.clusterStatus = Em.Object.create(App.Persist, {

  /**
   * Cluster name
   * @type {string}
   */
  clusterName: '',

  /**
   * List valid cluster states
   * @type {string[]}
   */
  validStates: [
    'DEFAULT',
    'CLUSTER_NOT_CREATED_1',
    'CLUSTER_DEPLOY_PREP_2',
    'CLUSTER_INSTALLING_3',
    'SERVICE_STARTING_3',
    'CLUSTER_INSTALLED_4',
    'ADD_HOSTS_DEPLOY_PREP_2',
    'ADD_HOSTS_INSTALLING_3',
    'ADD_HOSTS_INSTALLED_4',
    'ADD_SERVICES_DEPLOY_PREP_2',
    'ADD_SERVICES_INSTALLING_3',
    'ADD_SERVICES_INSTALLED_4',
    'STOPPING_SERVICES',
    'STACK_UPGRADING',
    'STACK_UPGRADE_FAILED',
    'STACK_UPGRADED',
    'ADD_SECURITY_STEP_1',
    'ADD_SECURITY_STEP_2',
    'ADD_SECURITY_STEP_3',
    'ADD_SECURITY_STEP_4',
    'DISABLE_SECURITY',
    'HIGH_AVAILABILITY_DEPLOY',
    'ROLLBACK_HIGH_AVAILABILITY'],

  /**
   * Default cluster state
   * @type {string}
   */
  clusterState: 'CLUSTER_NOT_CREATED_1',

  /**
   * Current used wizard <code>controller.name</code>
   * @type {string|null}
   */
  wizardControllerName: null,

  /**
   * Local DB
   * @type {object|null}
   */
  localdb: null,

  /**
   * Persist key
   * @type {string}
   */
  key: 'CLUSTER_CURRENT_STATUS',

  /**
   * Is cluster installed
   * @type {bool}
   */
  isInstalled: Em.computed.notExistsIn('clusterState', ['CLUSTER_NOT_CREATED_1', 'CLUSTER_DEPLOY_PREP_2', 'CLUSTER_INSTALLING_3', 'SERVICE_STARTING_3']),

  /**
   * Stores instance of <code>App.ModalPopup</code> created by <code>postUserPrefErrorCallback</code>
   * @property {App.ModalPopup|null}
   */
  persistErrorModal: null,

  /**
   * General info about cluster
   * @type {{clusterName: string, clusterState: string, wizardControllerName: string, localdb: object}}
   */
  value: function () {
    return {
      clusterName: this.get('clusterName'),
      clusterState: this.get('clusterState'),
      wizardControllerName: this.get('wizardControllerName'),
      localdb: this.get('localdb')
    };
  }.property('clusterName', 'clusterState', 'localdb', 'wizardControllerName'),

  /**
   * get cluster data from server and update cluster status
   * @param {bool} overrideLocaldb
   * @return promise object for the get call
   * @method updateFromServer
   */
  updateFromServer: function (overrideLocaldb) {
    var self = this;
    this.setProperties({
      clusterName: App.get('clusterName') || '',
      clusterState: this.get('wizardControllerName') === 'installerController' ? 'CLUSTER_NOT_CREATED_1' : 'DEFAULT',
      localdb: null
    });
    var request = scopedWorkflowPersistence.loadCurrent(this.get('wizardControllerName')).then(function (state) {
      var response = state && state.values && state.values.CLUSTER_CURRENT_STATUS;
      self.getUserPrefSuccessCallback(response, null, {
        data: {
          user: App.db.getUser(),
          login: App.db.getLoginName(),
          auth: App.db.getAuth(),
          overrideLocaldb: !overrideLocaldb
        }
      });
      return response;
    }, function (error) {
      self.getUserPrefErrorCallback(error || {}, null, error && error.message);
      return $.Deferred().reject(error).promise();
    });
    request.complete = request.always;
    return request;
  },

  /**
   * Success callback for get-persist request
   * @param {object} response
   * @param {object} opt
   * @param {object} params
   * @method getUserPrefSuccessCallback
   */
  getUserPrefSuccessCallback: function (response, opt, params) {
    if (response) {
      // decompress response
      if (typeof response != 'object') {
        response = JSON.parse(LZString.decompressFromBase64(response));
      }
      if (response.clusterState) {
        this.set('clusterState', response.clusterState);
      }
      if (response.clusterName) {
        this.set('clusterName', response.clusterName);
      }
      if (response.wizardControllerName) {
        this.set('wizardControllerName', response.wizardControllerName);
      }
      if (response.localdb && !$.isEmptyObject(response.localdb)) {
        this.set('localdb', response.localdb);
        if (App.db.getWorkflowStorageScope()) {
          App.db.restoreWorkflowSnapshot(response.localdb);
          return;
        }
        // restore HAWizard data if process was started
        var isHAWizardStarted = App.isAuthorized('SERVICE.ENABLE_HA') && !App.isEmptyObject(response.localdb.HighAvailabilityWizard);
        // restore Kerberos Wizard is started
        var isKerberosWizardStarted = App.isAuthorized('CLUSTER.TOGGLE_KERBEROS') && !App.isEmptyObject(response.localdb.KerberosWizard);
        if (params.data.overrideLocaldb || isHAWizardStarted || isKerberosWizardStarted) {
          var localdbTables = (App.db.data.app && App.db.data.app.tables) ? App.db.data.app.tables : {};
          var authenticated = Em.get(App, 'db.data.app.authenticated') || false;
          App.db.data = response.localdb;
          App.db.setLocalStorage();
          App.db.setUser(params.data.user);
          App.db.setLoginName(params.data.login);
          App.db.setAuth(params.data.auth);
          App.db.setAuthenticated(authenticated);
          App.db.data.app.tables = localdbTables;
        }
      }
    }
    // this is to ensure that the local storage namespaces are initialized with all expected namespaces.
    // after upgrading ambari, loading local storage data from the "persist" data saved via an older version of
    // Ambari can result in missing namespaces that are defined in the new version of Ambari.
    App.db.mergeStorage();
  },

  /**
   * Error callback for get-persist request
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @method getUserPrefErrorCallback
   */
  getUserPrefErrorCallback: function (request, ajaxOptions, error) {
    if (request.status == 404) {
      // default status already set
      return;
    }
    var self = this;
    App.showConfirmationPopup(
      function () {
        self.retryPersistence();
      },
      request.message || error || Em.I18n.t('common.update.error'),
      null,
      Em.I18n.t('workflow.persistence.retryHeader'),
      Em.I18n.t('common.retry'),
      'warning'
    );
  },

  retryPersistence: function () {
    var self = this;
    var request = scopedWorkflowPersistence.retryLoad().then(function (state) {
      self.getUserPrefSuccessCallback(state && state.values && state.values.CLUSTER_CURRENT_STATUS, null, {
        data: {
          user: App.db.getUser(),
          login: App.db.getLoginName(),
          auth: App.db.getAuth(),
          overrideLocaldb: false
        }
      });
      return state;
    }, function (error) {
      self.getUserPrefErrorCallback(error || {}, null, error && error.message);
      return $.Deferred().reject(error).promise();
    });
    request.complete = request.always;
    return request;
  },

  /**
   * update cluster status and post it on server.
   * This function should always be called by admin user
   * @param {object} newValue
   * @param {object} opt - Can have additional params for ajax callBacks and sender
   *                 opt.successCallback
   *                 opt.successCallbackData
   *                 opt.errorCallback
   *                 opt.errorCallbackData
   *                 opt.alwaysCallback
   *                 opt.alwaysCallbackData
   *                 opt.sender
   * @method setClusterStatus
   * @return {*}
   */
  setClusterStatus: function (newValue, opt) {
    if (App.get('testMode')) return false;
    var val = {clusterName: this.get('clusterName')};
    if (newValue) {
      App.db.cleanTmp();
      //setter
      if (newValue.clusterName) {
        this.set('clusterName', newValue.clusterName);
        val.clusterName = newValue.clusterName;
      }

      if (newValue.clusterState) {
        this.set('clusterState', newValue.clusterState);
        val.clusterState = newValue.clusterState;
      }
      if (newValue.wizardControllerName) {
        this.set('wizardControllerName', newValue.wizardControllerName);
        val.wizardControllerName = newValue.wizardControllerName;
      }

      if (newValue.localdb) {
        this.set('localdb', newValue.localdb);
      }
      val.localdb = App.db.getWorkflowSnapshot();
      if (!$.mocho) {
        scopedWorkflowPersistence.saveStatus(val)
            .done(function () {
              !!opt && Em.typeOf(opt.successCallback) === 'function' && opt.successCallback.call(opt.sender || this, opt.successCallbackData);
            })
            .fail(function (error) {
              this.postUserPrefErrorCallback(error || {}, null, error && error.message);
              !!opt && Em.typeOf(opt.errorCallback) === 'function' && opt.errorCallback.call(opt.sender || this, opt.errorCallbackData);
            }.bind(this))
            .always(function () {
              !!opt && Em.typeOf(opt.alwaysCallback) === 'function' && opt.alwaysCallback.call(opt.sender || this, opt.alwaysCallbackData);
            });
      }
      return newValue;
    }
  },

  /**
   * Error callback for post-persist request
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @method postUserPrefErrorCallback
   */
  postUserPrefErrorCallback: function (request, ajaxOptions, error) {
    var self = this;
    var msg = error || request.message || Em.get(request, 'responseJSON.message') || '';
    var doc;
    try {
      msg = msg || 'Error ' + (request.status) + ' ';
      doc = $.parseXML(request.responseText);
      msg += $(doc).find("body p").text();
    } catch (e) {
      if (!msg && request.responseText) {
        try {
          msg = JSON.parse(request.responseText).message;
        } catch (ignore) {
          msg = request.responseText;
        }
      }
    }

    if (this.get('persistErrorModal')) {
      if (this.get('persistErrorModal').get('state') === 'destroyed') {
        this.set('persistErrorModal', null);
      } else {
        this.get('persistErrorModal').onPrimary();
        this.set('persistErrorModal', null);
      }
    }

    var modal = App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      primary: Em.I18n.t('common.retry'),
      secondary: Em.I18n.t('common.cancel'),
      response: msg,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<p>{{t common.persist.error}} {{response}}</p>')
      }),
      onPrimary: function () {
        this.hide();
        self.set('persistErrorModal', null);
        self.retryPersistence();
      },
      onSecondary: function () {
        this.hide();
        self.set('persistErrorModal', null);
      }
    });
    this.set('persistErrorModal', modal);
  }

});
