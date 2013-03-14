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

  /**
   * return true if security status is loaded and false otherwise
   */
  securityStatusLoading: function () {
    var dfd = $.Deferred();
    this.connectOutlet('loading');
    if (App.testMode) {
      window.setTimeout(function () {
        dfd.resolve();
      }, 50);
    } else {
      this.getHDFSDetailsFromServer(dfd);
    }
    return dfd.promise();
  },

  /**
   * return true if security status is loaded and false otherwise
   */
  getHDFSDetailsFromServer: function (dfd) { //TODO: this should be obtain from cluster level config rather than HDFS global config
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services/HDFS';
    $.ajax({
      type: 'GET',
      url: url,
      async: false,    // we are retrieving user information that is used ahead in addSecurity/apply stage
      timeout: 10000,
      dataType: 'text',
      success: function (data) {
        console.log("TRACE: The url is: " + url);
        var jsonData = jQuery.parseJSON(data);
        var configs = jsonData.ServiceInfo.desired_configs;
        if ('global' in configs) {
          self.getServiceConfigsFromServer(dfd, 'global', configs['global']);
        } else {
          if (dfd) {
            dfd.reject();
          }
        }
      },

      error: function (request, ajaxOptions, error) {
        if (dfd) {
          dfd.reject();
        }
      },

      statusCode: require('data/statusCodes')
    });
  },

  getServiceConfigsFromServer: function (dfd, siteName, tagName) {
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/configurations/?type=' + siteName + '&tag=' + tagName;
    $.ajax({
      type: 'GET',
      url: url,
      async: false, // we are retrieving user information that is used ahead in addSecurity/apply stage
      timeout: 10000,
      dataType: 'json',
      success: function (data) {
        console.log("TRACE: In success function for the GET getServiceConfigsFromServer call");
        console.log("TRACE: The url is: " + url);
        var configs = data.items.findProperty('tag', tagName).properties;
        if (configs && configs['security_enabled'] === 'true') {
          self.set('securityEnabled', true);
        } else {
          self.loadUsers(configs);
          self.set('securityEnabled', false);
        }
        if (dfd) {
          dfd.resolve();
        }
      },

      error: function (request, ajaxOptions, error) {
        if (dfd) {
          dfd.reject();
        }
      },

      statusCode: require('data/statusCodes')
    });
  },

  loadUsers: function (configs) {
    var serviceUsers = this.get('serviceUsers');
    if (configs['hdfs_user']) {
      serviceUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: configs['hdfs_user']});
    } else {
      serviceUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
    }
    if (configs['mapred_user']) {
      serviceUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: configs['mapred_user']});
    } else {
      serviceUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
    }
    if (configs['hbase_user']) {
      serviceUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: configs['hbase_user']});
    } else {
      serviceUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
    }
    if (configs['hive_user']) {
      serviceUsers.pushObject({id: 'puppet var', name: 'hive_user', value: configs['hive_user']});
    } else {
      serviceUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
    }
  }

});