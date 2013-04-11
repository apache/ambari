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

App.MainAdminController = Em.Controller.extend({
  name: 'mainAdminController',
  category: 'user',
  securityEnabled: false,
  serviceUsers: [],

  deferred: null,

  tag: null,

  /**
   * return true if security status is loaded and false otherwise
   */
  securityStatusLoading: function () {
    var self = this;
    this.set('deferred', $.Deferred());
    if (App.testMode) {
      window.setTimeout(function () {
        self.get('deferred').resolve();
      }, 50);
    }
    else {
      //get Security Status From Server
      App.ajax.send({
        name: 'admin.security_status',
        sender: this,
        success: 'getSecurityStatusFromServerSuccessCallback',
        error: 'errorCallback'
      });
    }
    return this.get('deferred').promise();
  },

  errorCallback: function() {
    this.get('deferred').reject();
  },

  getSecurityStatusFromServerSuccessCallback: function (data) {
    var configs = data.Clusters.desired_configs;
    if ('global' in configs) {
      this.set('tag', configs['global'].tag);
      this.getServiceConfigsFromServer();
    }
    else {
      this.get('deferred').reject();
    }
  },

  getServiceConfigsFromServer: function () {
    App.ajax.send({
      name: 'admin.service_config',
      sender: this,
      data: {
        siteName: 'global',
        tagName: this.get('tag')
      },
      success: 'getServiceConfigsFromServerSuccessCallback',
      error: 'errorCallback'
    });
  },

  getServiceConfigsFromServerSuccessCallback: function (data) {
    console.log("TRACE: In success function for the GET getServiceConfigsFromServer call");
    var configs = data.items.findProperty('tag', this.get('tag')).properties;
    if (configs && configs['security_enabled'] === 'true') {
      this.set('securityEnabled', true);
    }
    else {
      this.loadUsers(configs);
      this.set('securityEnabled', false);
    }
    this.get('deferred').resolve();
  },

  loadUsers: function (configs) {
    var serviceUsers = this.get('serviceUsers');
    serviceUsers.pushObject({
      id: 'puppet var',
      name: 'hdfs_user',
      value: configs['hdfs_user'] ? configs['hdfs_user'] : 'hdfs'
    });
    serviceUsers.pushObject({
      id: 'puppet var',
      name: 'mapred_user',
      value: configs['mapred_user'] ? configs['mapred_user'] : 'mapred'
    });
    serviceUsers.pushObject({
      id: 'puppet var',
      name: 'hbase_user',
      value: configs['hbase_user'] ? configs['hbase_user'] : 'hbase'
    });
    serviceUsers.pushObject({
      id: 'puppet var',
      name: 'hive_user',
      value: configs['hive_user'] ? configs['hive_user'] : 'hive'
    });
  }

});