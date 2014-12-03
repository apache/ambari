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


  notInstalledHostsCount: function() {
    return this.get('content.state') == 'INIT' ? this.get('totalHostCount.length') : 0;
  }.property('content.state', 'totalHostCount.length'),

  noInitHosts: function() {
    return this.get('notInstalledHostsCount.length') == 0;
  }.property('notInstalledHostsCount.length'),
  /**
   * true if stack version install is in progress
   * {Boolean}
   */
  installInProgress: function() {
    return this.get('content.state') == "INSTALLING";
  }.property('content.state'),

  /**
   * true if stack version upgrade is in progress
   * {Boolean}
   */
  upgradeInProgress: function() {
    return this.get('content.state') == "UPGRADING";
  }.property('content.state'),

  /**
   * true if repo version is installed on all hosts but not upgraded
   * {Boolean}
   */
  installedNotUpgraded: function() {
    return this.get("allInstalled") && !this.get("allUpgraded") ;
  }.property("allInstalled", "allUpgraded"),

  allInstalled: function() {
    return this.get('content.installedHosts.length') == this.get('totalHostCount') ||
      this.get('content.currentHosts.length') == this.get('totalHostCount');
  }.property('content.installedHosts.length', 'content.currentHosts.length'),

  allUpgraded: function() {
    return this.get('content.upgradedHosts.length') == this.get('totalHostCount') ||
      this.get('content.currentHosts.length') == this.get('totalHostCount');
  }.property('content.upgradedHosts.length', 'content.currentHosts.length'),

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
    var popupTitle = Em.I18n.t('admin.stackVersions.datails.install.hosts.popup.title').format(this.get('content.repositoryVersion.displayName'));
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
    //TODO add correct request
    return null;
    /*return App.ajax.send({
      name: 'admin.stack_version.install.repo_version',
      data: data,
      success: 'installStackVersionSuccess'
    })*/
  },

  installStackVersionSuccess: function(data) {
    App.db.set('stackUpgrade', 'id', [data.id]);
  }
});
