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
  installedServices: function(){
    return App.Service.find().mapProperty('serviceName');
  }.property(),
  currentVersion: function(){
    return App.get('currentStackVersion');
  }.property('App.currentStackVersion'),
  upgradeVersion: '',
  /**
   * get the newest version of HDP from server
   * and update if it newer than current
   */
  updateUpgradeVersion: function(){
    var url = App.formatUrl(
      App.apiPrefix + "/stacks",
      {},
      '/data/wizard/stack/stacks.json'
    );
    var result = this.get('upgradeVersion') || App.defaultStackVersion;
    $.ajax({
      type: "GET",
      url: url,
      async: false,
      dataType: 'json',
      timeout: App.timeout,
      success: function (data) {
        result = result.replace(/HDP-/, '');
        data.filterProperty('name', 'HDP').mapProperty('version').forEach(function(version){
          result = (result < version) ? version : result;
        });
        result = 'HDP-' + result;
      },
      error: function (request, ajaxOptions, error) {
        console.log('Error message is: ' + request.responseText);
      },
      statusCode: require('data/statusCodes')
    });
    if(result && result !== this.get('upgradeVersion')){
      this.set('upgradeVersion', result);
    }
  },
  /**
   * load services info(versions, description)
   */
  loadServicesFromServer: function () {
    var displayOrderConfig = require('data/services');
    var result = [];
    var method = 'GET';
    var url = App.formatUrl(
      App.apiPrefix + App.get('stackVersionURL'),
      {},
      '/data/wizard/stack/hdp/version/1.2.0.json'
    );
    var self = this;
    $.ajax({
      type: method,
      url: url,
      async: false,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);

        // loop through all the service components
        for (var i = 0; i < displayOrderConfig.length; i++) {
          var entry = jsonData.services.findProperty("name", displayOrderConfig[i].serviceName);
          if(self.get('installedServices').contains(entry.name)){
            var myService = Em.Object.create({
              serviceName: entry.name,
              displayName: displayOrderConfig[i].displayName,
              isDisabled: i === 0,
              isSelected: true,
              isInstalled: false,
              isHidden: displayOrderConfig[i].isHidden,
              description: entry.comment,
              version: entry.version,
              newVersion: '1.3.0'
            });
            result.push(myService);
          }
        }
      },
      error: function (request, ajaxOptions, error) {
        console.log('Error message is: ' + request.responseText);
      },
      statusCode: require('data/statusCodes')
    });
    this.set('services', result);
  }.observes('upgradeVersion')
});