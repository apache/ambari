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

App.clusterStatus = Ember.Object.create({
  clusterName: '',
  validStates: ['CLUSTER_NOT_CREATED_1', 'CLUSTER_DEPLOY_PREP_2', 'CLUSTER_INSTALLING_3', 'SERVICE_STARTING_3', 'CLUSTER_INSTALLED_4',  'CLUSTER_STARTED_5',
    'ADD_HOSTS_DEPLOY_PREP_2', 'ADD_HOSTS_INSTALLING_3', 'ADD_HOSTS_INSTALLED_4', 'ADD_HOSTS_COMPLETED_5',
    'ADD_SERVICES_DEPLOY_PREP_2', 'ADD_SERVICES_INSTALLING_3', 'ADD_SERVICES_INSTALLED_4', 'ADD_SERVICES_COMPLETED_5',
    'STOPPING_SERVICES', 'STACK_UPGRADING', 'STACK_UPGRADE_FAILED', 'STACK_UPGRADED', 'STACK_UPGRADE_COMPLETED', 'ADD_SECURITY_STEP_1',
    'ADD_SECURITY_STEP_2', 'ADD_SECURITY_STEP_3', 'ADD_SECURITY_STEP_4', 'DISABLE_SECURITY', 'SECURITY_COMPLETED'],
  clusterState: 'CLUSTER_NOT_CREATED_1',
  wizardControllerName: null,
  localdb: null,
  key: 'CLUSTER_CURRENT_STATUS',
  /**
   * get cluster data from server and update cluster status
   * @param isAsync: set this to true if the call is to be made asynchronously.  if unspecified, false is assumed
   * @return promise object for the get call
   */
  updateFromServer: function(isAsync) {
    // if isAsync is undefined, set it to false
    isAsync = isAsync || false;
    var url = App.apiPrefix + '/persist/' + this.get('key');
    return jQuery.ajax(
      {
        url: url,
        context: this,
        async: isAsync,
        success: function (response) {
          if (response) {
            var newValue = jQuery.parseJSON(response);
            if (newValue.clusterState) {
              this.set('clusterState', newValue.clusterState);
            }
            if (newValue.clusterName) {
              this.set('clusterName', newValue.clusterName);
            }
            if (newValue.wizardControllerName) {
              this.set('wizardControllerName', newValue.wizardControllerName);
            }
            if (newValue.localdb) {
              this.set('localdb', newValue.localdb);
            }
          } else {
            // default status already set
          }
        },
        error: function (xhr) {
          if (xhr.status == 404) {
            // default status already set
            console.log('Persist API did NOT find the key CLUSTER_CURRENT_STATUS');
          }
        },
        statusCode: require('data/statusCodes')
      }
    );
  },
  /**
   * update cluster status and post it on server
   * @param newValue
   * @return {*}
   */
  setClusterStatus: function(newValue){
    if(App.testMode) return false;
    if (newValue) {
      //setter
      if (newValue.clusterState) {
        this.set('clusterState', newValue.clusterState);
      }
      if (newValue.clusterName) {
        this.set('clusterName', newValue.clusterName);
      }
      if (newValue.wizardControllerName) {
        this.set('wizardControllerName', newValue.wizardControllerName);
      }
      if (newValue.localdb) {
        this.set('localdb', newValue.localdb);
      }

      var url = App.apiPrefix + '/persist/';
      var keyValuePair = {};
      var val = {
        clusterName: this.get('clusterName'),
        clusterState: this.get('clusterState'),
        wizardControllerName: this.get('wizardControllerName'),
        localdb: this.get('localdb')
      };
      keyValuePair[this.get('key')] = JSON.stringify(val);


      jQuery.ajax({
        async: false,
        context: this,
        type: "POST",
        url: url,
        data: JSON.stringify(keyValuePair),
        beforeSend: function () {
          console.log('BeforeSend: persistKeyValues', keyValuePair);
        }
      });
      return newValue;
    }
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
