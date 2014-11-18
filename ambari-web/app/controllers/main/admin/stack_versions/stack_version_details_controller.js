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

App.MainStackVersionsDetailsController = Em.Controller.extend({
  name: 'mainStackVersionsDetailsController',

  content: null,

  totalHostCount: function() {
    return App.get('allHostNames.length');
  }.property('App.allHostNames.length'),

  /**
   * true if install stack version is in progress at least on 1 host
   * {Boolean}
   */
  installInProgress: function() {
    return this.get('content.upgradingHostStacks.length');
  }.property('content.upgradingHostStacks.length'),

  /**
   * true if repo version is installed on all hosts
   * {Boolean}
   */
  allInstalled: function() {
    return this.get('content.notInstalledHostStacks.length') == 0;
  }.property('content.notInstalledHostStacks.length'),

  /**
   * depending on state run or install repo request
   * or show the installation process popup
   * @param event
   * @method installStackVersion
   */
  installStackVersion: function(event) {
    if (this.get('installInProgress')) {
      this.showProgressPopup();
    } else if (!this.get('allInstalled')) {
      this.doInstallStackVersion(event.context);
    }
  },

  /**
   * opens a popup with installations state per host
   * @method showProgressPopup
   */
  showProgressPopup: function() {
    var popupTitle = Em.I18n.t('admin.stackVersions.datails.install.hosts.popup.title').format(this.get('content.version'));
    var requestIds = App.get('testMode') ? [1] : App.db.get('stackUpgrade', 'id');
    var hostProgressPopupController = App.router.get('highAvailabilityProgressPopupController');
    hostProgressPopupController.initPopup(popupTitle, requestIds, this, true);
  },

  /**
   * runs request to install repo version
   * @param stackVersion
   * @returns {*|$.ajax}
   * @method doInstallStackVersion
   */
  doInstallStackVersion: function(stackVersion) {
    var services = App.Service.find();
    var data = this.generateDataForInstall(stackVersion, services);
    return App.ajax.send({
      name: 'admin.stack_version.install.repo_version',
      data: data,
      success: 'installStackVersionSuccess'
    })
  },

  /**
   * generates the request data for installing repoversions
   * @param {Ember.object} stackVersion
   * @param {Array} services - array of service objects
   * @returns {JSON}
   * {
   *  RequestInfo:
   *   {
   *     context: string,
   *     action: string,
   *     parameters:
   *       {
   *         name: string,
   *         name_list: string,
   *         base_url_list: string,
   *         packages: string
   *       }
   *    },
   *  Requests/resource_filters: [{hosts:string}]
   * }
   */
  generateDataForInstall: function(stackVersion, services) {
    var hostNames = stackVersion.get('hostStackVersions').mapProperty('host.hostName');
    var base_urls = stackVersion.get('operatingSystems').map(function(os) {
      return os.get('repositories').mapProperty('baseurl').join(",");
    });
    var serviceNames = services.mapProperty('serviceName');
    return {
      "RequestInfo": {
        "context":"Install Repo Version" + stackVersion.get('version'),
        "action":"ru_install_repo",
        "parameters": {
          "name": stackVersion.get('version'),
          "name_list": stackVersion.get('name'),
          "base_url_list": base_urls.join(","),
          "packages": serviceNames.join(",")
        }
      },
      "Requests/resource_filters": [
        {
          "hosts": hostNames.join(",")
        }
      ]
    };
  },

  installStackVersionSuccess: function(data) {
    App.db.set('stackUpgrade', 'id', [data.id]);
  }
});
