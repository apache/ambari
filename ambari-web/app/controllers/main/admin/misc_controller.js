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

require('controllers/main/service/info/configs');

App.MainAdminMiscController = App.MainServiceInfoConfigsController.extend({
  name:'mainAdminMiscController',
  users: null,
  content: {
    serviceName: 'MISC'
  },
  loadUsers: function() {
    this.set('selectedService', this.get('content.serviceName'));
    this.loadServiceConfig();
  },
  loadServiceConfig: function() {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        serviceName: this.get('content.serviceName'),
        serviceConfigsDef: this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'))
      },
      success: 'loadServiceTagSuccess'
    });
  },
  loadServiceTagSuccess: function(data, opt, params) {
    var installedServices = App.Service.find().mapProperty("serviceName");
    var serviceConfigsDef = params.serviceConfigsDef;
    var serviceName = this.get('content.serviceName');
    var loadedClusterSiteToTagMap = {};

    for ( var site in data.Clusters.desired_configs) {
      if (serviceConfigsDef.sites.indexOf(site) > -1) {
        loadedClusterSiteToTagMap[site] = data.Clusters.desired_configs[site]['tag'];
      }
    }
    this.setServiceConfigTags(loadedClusterSiteToTagMap);
    var configGroups = App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags'));
    var configSet = App.config.mergePreDefinedWithLoaded(configGroups, [], this.get('serviceConfigTags'), serviceName);

    var misc_configs = configSet.globalConfigs.filterProperty('serviceName', this.get('selectedService')).filterProperty('category', 'Users and Groups').filterProperty('isVisible', true);

    misc_configs = App.config.miscConfigVisibleProperty(misc_configs, installedServices);

    var sortOrder = this.get('configs').filterProperty('serviceName', this.get('selectedService')).filterProperty('category', 'Users and Groups').filterProperty('isVisible', true).mapProperty('name');

    var sorted = [];

    if(sortOrder) {
      sortOrder.forEach(function(name) {
        sorted.push(misc_configs.findProperty('name', name));
      });
      this.set('users', sorted);
    }
    else {
      this.set('users', misc_configs);
    }
    if(this.get("content.hdfsUser")){
      this.get('content').set('hdfsUser', misc_configs.findProperty('name','hdfs_user').get("value"));
    }
    if (this.get("content.group")) {
      this.get('content').set('group', misc_configs.findProperty('name','user_group').get("value"));
    }
    this.set('dataIsLoaded', true);
  }
});
