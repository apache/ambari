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

App.MainAdminClusterController = Em.Controller.extend({
  name:'mainAdminClusterController',
  services: [],
  upgradeVersion: '',
  /**
   * get the newest version of HDP from server
   */
  updateUpgradeVersion: function(){
    if(App.router.get('clusterController.isLoaded')){
      var url = App.formatUrl(
        App.apiPrefix + "/stacks",
        {},
        '/data/wizard/stack/stacks.json'
      );
      var upgradeVersion = this.get('upgradeVersion') || App.defaultStackVersion;
      var installedServices = {};
      var newServices = {};
      $.ajax({
        type: "GET",
        url: url,
        async: false,
        dataType: 'json',
        timeout: App.timeout,
        success: function (data) {
          var currentVersion = App.currentStackVersion.replace(/HDP-/, '');
          var minUpgradeVersion = currentVersion;
          upgradeVersion = upgradeVersion.replace(/HDP-/, '');
          data = data.filterProperty('name', 'HDP');
          data.mapProperty('version').forEach(function(version){
            upgradeVersion = (upgradeVersion < version) ? version : upgradeVersion;
          });
          //TODO remove hardcoded upgrade version
          upgradeVersion = (App.testMode)?'1.3.0': upgradeVersion;
          minUpgradeVersion = data.findProperty('version', upgradeVersion).minUpgradeVersion;
          minUpgradeVersion = (minUpgradeVersion) ? minUpgradeVersion : currentVersion;
          upgradeVersion = (minUpgradeVersion <= currentVersion) ? upgradeVersion : currentVersion;
          installedServices = data.findProperty('version', currentVersion);
          newServices = data.findProperty('version', upgradeVersion);
          upgradeVersion = 'HDP-' + upgradeVersion;
        },
        error: function (request, ajaxOptions, error) {
          console.log('Error message is: ' + request.responseText);
        },
        statusCode: require('data/statusCodes')
      });
      this.set('upgradeVersion', upgradeVersion);
      if(installedServices && newServices){
        this.parseServicesInfo(installedServices, newServices);
      } else {
        console.log('HDP stack doesn\'t have services with defaultStackVersion');
      }
    }
  }.observes('App.router.clusterController.isLoaded', 'App.currentStackVersion'),
  /**
   * parse services info(versions, description) by version
   */
  parseServicesInfo: function (oldServices, newServices) {
    var result = [];
    var installedServices = App.Service.find().mapProperty('serviceName');
    var displayOrderConfig = require('data/services');
    if(oldServices.services && newServices.services){
      // loop through all the service components
      for (var i = 0; i < displayOrderConfig.length; i++) {
        var entry = oldServices.services.findProperty("name", displayOrderConfig[i].serviceName);
        if (entry) {
          if (installedServices.contains(entry.name)) {
            var myService = Em.Object.create({
              serviceName: entry.name,
              displayName: displayOrderConfig[i].displayName,
              isDisabled: i === 0,
              isSelected: true,
              isInstalled: false,
              isHidden: displayOrderConfig[i].isHidden,
              description: entry.comment,
              version: entry.version,
              newVersion: newServices.services.findProperty("name", displayOrderConfig[i].serviceName).version
            });
            //From 1.3.0 for Hive we display only "Hive" (but it installes HCat and WebHCat as well)
            if (this.get('upgradeVersion').replace(/HDP-/, '') >= '1.3.0' && displayOrderConfig[i].serviceName == 'HIVE') {
              myService.set('displayName', 'Hive');
            }
            result.push(myService);
          }
        }
        else {
          console.warn('Service not found - ', displayOrderConfig[i].serviceName);
        }
      }
    }
    this.set('services', result);
  }
});