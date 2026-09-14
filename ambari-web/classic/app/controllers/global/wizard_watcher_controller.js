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
var scopedWorkflowPersistence = require('utils/scoped_workflow_persistence');

App.WizardWatcherController = Em.Controller.extend(App.Persist, {
  name: 'wizardWatcherController',

  /**
   * @const
   */
  PREF_KEY: 'wizard-data',

  /**
   * name of user who working with wizard
   * @type {string|null}
   */
  wizardUser: null,

  /**
   * @type {string|null}
   */
  controllerName: null,

  lastPersistenceErrorCode: null,

  reentryNoticeShown: false,

  /**
   * define whether Wizard is running
   * @type {boolean}
   */
  isWizardRunning: Em.computed.bool('wizardUser'),

  /**
   * @type {string}
   */
  wizardDisplayName: function() {
    const controllerName = this.get('controllerName');
    return controllerName ? Em.I18n.t('wizard.inProgress').format(App.router.get(controllerName).get('displayName'), this.get('wizardUser')) : '';
  }.property('controllerName'),

  /**
   * define whether logged in user is the one who started wizard
   * @type {boolean}
   */
  isNonWizardUser: function() {
    return this.get('isWizardRunning') && this.get('wizardUser') !== App.router.get('loginName');
  }.property('App.router.loginName', 'wizardUser').volatile(),

  /**
   * set user who launched wizard
   * @returns {$.ajax}
   */
  setUser: function(controllerName) {
    var self = this;
    return scopedWorkflowPersistence.setUser(controllerName).done(function () {
      self.setProperties({
        wizardUser: App.router.get('loginName'),
        controllerName: controllerName
      });
    });
  },

  /**
   * reset user who launched wizard
   * @returns {$.ajax}
   */
  resetUser: function() {
    var self = this;
    return scopedWorkflowPersistence.release().done(function () {
      self.setProperties({wizardUser: null, controllerName: null});
    }).fail(function (error) {
      self.getUserPrefErrorCallback(error);
    });
  },

  retryPersistence: function () {
    var self = this;
    this.set('lastPersistenceErrorCode', null);
    return scopedWorkflowPersistence.retryLoad().then(function (state) {
      self.getUserPrefSuccessCallback(state && state.values && state.values.wizardData);
      return state;
    }, function (error) {
      self.getUserPrefErrorCallback(error);
      return $.Deferred().reject(error).promise();
    });
  },

  /**
   * get user who launched wizard
   * @returns {$.ajax}
   */
  getUser: function() {
    var self = this;
    var request = scopedWorkflowPersistence.loadCurrent().then(function (state) {
      self.getUserPrefSuccessCallback(state && state.values && state.values.wizardData);
      return state;
    }, function (error) {
      self.getUserPrefErrorCallback(error);
      return $.Deferred().reject(error).promise();
    });
    request.complete = request.always;
    return request;
  },

  getUserPrefSuccessCallback: function(data) {
    this.set('lastPersistenceErrorCode', null);
    if (scopedWorkflowPersistence.requiresReentry()) {
      if (!this.get('reentryNoticeShown')) {
        this.set('reentryNoticeShown', true);
        App.showAlertPopup(
          Em.I18n.t('common.warning'),
          Em.I18n.t('workflow.persistence.reentryRequired')
        );
      }
    } else {
      this.set('reentryNoticeShown', false);
    }
    if (Em.isNone(data)) {
      this.set('wizardUser', null);
      this.set('controllerName', null);
    } else {
      this.set('wizardUser', data.userName);
      this.set('controllerName', data.controllerName);
    }
  },

  getUserPrefErrorCallback: function (error) {
    var self = this;
    var errorCode = scopedWorkflowPersistence.errorCode(error) || 'WORKFLOW_STATE_UNAVAILABLE';
    if (this.get('lastPersistenceErrorCode') !== errorCode) {
      this.set('lastPersistenceErrorCode', errorCode);
      App.showConfirmationPopup(
        function () {
          self.retryPersistence();
        },
        error && error.message || Em.I18n.t('common.update.error'),
        null,
        Em.I18n.t('workflow.persistence.retryHeader'),
        Em.I18n.t('common.retry'),
        'warning'
      );
    }
  }
});
