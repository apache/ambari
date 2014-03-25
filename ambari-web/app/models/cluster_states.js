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
require('mixins/common/userPref');

App.clusterStatus = Em.Object.create(App.UserPref, {
  clusterName: '',
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
  clusterState: 'CLUSTER_NOT_CREATED_1',
  wizardControllerName: null,
  localdb: null,
  key: 'CLUSTER_CURRENT_STATUS',
  isInstalled: function(){
    var notInstalledStates = ['CLUSTER_NOT_CREATED_1', 'CLUSTER_DEPLOY_PREP_2', 'CLUSTER_INSTALLING_3', 'SERVICE_STARTING_3'];
    return !notInstalledStates.contains(this.get('clusterState'));
  }.property('clusterState'),
  /**
   * get cluster data from server and update cluster status
   * @param {Boolean} isAsync set this to true if the call is to be made asynchronously.  if unspecified, false is assumed
   * @param {Boolean} overrideLocaldb
   * @return promise object for the get call
   */
  updateFromServer: function(isAsync, overrideLocaldb) {
    // if isAsync is undefined, set it to false
    this.set('makeRequestAsync', isAsync || false);
    // if overrideLocaldb is undefined, set it to true
    this.set('additionalData', {
      user: App.db.getUser(),
      login: App.db.getLoginName(),
      overrideLocaldb: overrideLocaldb || true
    });
    return this.getUserPref(this.get('key'));
  },

  getUserPrefSuccessCallback: function (response, opt, params) {
    if (response) {
      if (response.clusterState) {
        this.set('clusterState', response.clusterState);
      }
      if (response.clusterName) {
        this.set('clusterName', response.clusterName);
      }
      if (response.wizardControllerName) {
        this.set('wizardControllerName', response.wizardControllerName);
      }
      if (response.localdb) {
        this.set('localdb', response.localdb);
        // restore HAWizard data if process was started
        var isHAWizardStarted = App.get('isAdmin') && !App.isEmptyObject(response.localdb.HighAvailabilityWizard);
        if (params.overrideLocaldb || isHAWizardStarted) {
          App.db.data = response.localdb;
          App.db.setLocalStorage();
          App.db.setUser(params.user);
          App.db.setLoginName(params.login);
        }
      }
    }
    // this is to ensure that the local storage namespaces are initialized with all expected namespaces.
    // after upgrading ambari, loading local storage data from the "persist" data saved via an older version of
    // Ambari can result in missing namespaces that are defined in the new version of Ambari.
    App.db.mergeStorage();
  },

  getUserPrefErrorCallback: function (request, ajaxOptions, error) {
    if (request.status == 404) {
      // default status already set
      console.log('Persist API did NOT find the key CLUSTER_CURRENT_STATUS');
      return;
    }
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<p>{{t common.update.error}}</p>')
      })
    });
  },

  /**
   * update cluster status and post it on server
   * @param newValue
   * @return {*}
   */
  setClusterStatus: function(newValue){
    if(App.testMode) return false;
    var user = App.db.getUser();
    var login = App.db.getLoginName();
    var val = {clusterName: this.get('clusterName')};
    if (newValue) {
      //setter
      if (newValue.clusterName) {
        this.set('clusterName', newValue.clusterName);
        val.clusterName =  newValue.clusterName;
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
        if (newValue.localdb.app && newValue.localdb.app.user)
          delete newValue.localdb.app.user;
        if (newValue.localdb.app && newValue.localdb.app.loginName)
          delete newValue.localdb.app.loginName;
        this.set('localdb', newValue.localdb);
        val.localdb = newValue.localdb;
      } else {
        delete App.db.data.app.user;
        delete App.db.data.app.loginName;
          val.localdb = App.db.data;
        App.db.setUser(user);
        App.db.setLoginName(login);
      }

      this.set('makeRequestAsync', false);
      this.postUserPref(this.get('key'), val);
      return newValue;
    }
  },

  postUserPrefErrorCallback: function(request, ajaxOptions, error) {
    console.log("ERROR");
    var msg = '', doc;
    try {
      msg = 'Error ' + (request.status) + ' ';
      doc = $.parseXML(request.responseText);
      msg += $(doc).find("body p").text();
    } catch (e) {
      msg += JSON.parse(request.responseText).message;
    }

    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      response: msg,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<p>{{t common.persist.error}} {{response}}</p>')
      })
    });
  },

  /**
   * general info about cluster
   */
  value: function () {
      return {
        clusterName: this.get('clusterName'),
        clusterState: this.get('clusterState'),
        wizardControllerName: this.get('wizardControllerName'),
        localdb: this.get('localdb')
      };
  }.property('clusterName', 'clusterState', 'localdb', 'wizardControllerName')

});
