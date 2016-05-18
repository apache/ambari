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
var stringUtils = require('utils/string_utils');

require('controllers/main/service/info/configs');

App.MainAdminServiceAccountsController = App.MainServiceInfoConfigsController.extend({
  name: 'mainAdminServiceAccountsController',
  users: null,
  serviceConfigTags: [],
  content: Em.Object.create({
    serviceName: 'MISC'
  }),
  loadUsers: function () {
    this.set('selectedService', this.get('content.serviceName') ? this.get('content.serviceName') : "MISC");
    this.loadServiceConfig();
  },
  loadServiceConfig: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      data: {
        serviceName: this.get('selectedService'),
        serviceConfigsDef: this.get('serviceConfigs').findProperty('serviceName', this.get('selectedService'))
      },
      success: 'loadServiceTagSuccess'
    });
  },
  loadServiceTagSuccess: function (data, opt, params) {
    var self = this;
    var serviceConfigsDef = params.serviceConfigsDef;
    var loadedClusterSiteToTagMap = {};

    for (var site in Em.get(data, 'Clusters.desired_configs')) {
      if (serviceConfigsDef.get('configTypes').hasOwnProperty(site)) {
        loadedClusterSiteToTagMap[site] = data.Clusters.desired_configs[site]['tag'];
      }
    }
    this.setServiceConfigTags(loadedClusterSiteToTagMap);
    // load server stored configurations
    App.router.get('configurationController').getConfigsByTags(this.get('serviceConfigTags')).done(function (serverConfigs) {
      self.createConfigObject(serverConfigs);
    });
  },


  /**
   * Changes format from Object to Array
   *
   * {
   *  'core-site': 'version1',
   *  'hdfs-site': 'version1',
   *  ...
   * }
   *
   * to
   *
   * [
   *  {
   *    siteName: 'core-site',
   *    tagName: 'version1',
   *    newTageName: null
   *  },
   *  ...
   * ]
   *
   * set tagnames for configuration of the *-site.xml
   * @private
   * @method setServiceConfigTags
   */
  setServiceConfigTags: function (desiredConfigsSiteTags) {
    var newServiceConfigTags = [];
    for (var index in desiredConfigsSiteTags) {
      newServiceConfigTags.pushObject({
        siteName: index,
        tagName: desiredConfigsSiteTags[index],
        newTagName: null
      }, this);
    }
    this.set('serviceConfigTags', newServiceConfigTags);
  },

  /**
   * Generate configuration object that will be rendered
   *
   * @param {Object[]} serverConfigs
   */
  createConfigObject: function(serverConfigs) {
    var configs = [];
    serverConfigs.forEach(function(configObject) {
      configs = configs.concat(App.config.getConfigsFromJSON(configObject, true));
    });
    var miscConfigs = configs.filterProperty('displayType', 'user').filterProperty('category', 'Users and Groups');
    miscConfigs.setEach('isVisible', true);
    this.set('users', miscConfigs);
    this.set('dataIsLoaded', true);
  },

  /**
   * sort miscellaneous configs by specific order
   * @param sortOrder
   * @param arrayToSort
   * @return {Array}
   */
  sortByOrder: function (sortOrder, arrayToSort) {
    var sorted = [];
    if (sortOrder && sortOrder.length > 0) {
      sortOrder.forEach(function (name) {
        var user = arrayToSort.findProperty('name', name);
        if (user) {
          sorted.push({
            isVisible: user.get('isVisible'),
            displayName: user.get('displayName'),
            value: user.get('value')
          });
        }
      });
      return sorted;
    } else {
      return arrayToSort;
    }
  }
});
