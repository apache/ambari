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

  /**
   * amount of all hosts installed on cluster
   * @type {Number}
   */
  totalHostCount: function() {
    return App.get('allHostNames.length');
  }.property('App.allHostNames.length'),

  /**
   * true if stack version install is in progress
   * @type {Boolean}
   */
  installInProgress: function() {
    return this.get('content.state') == "INSTALLING";
  }.property('content.state'),

  /**
   * true if repo version is installed on all hosts but not upgraded
   * @type {Boolean}
   */
  installedNotUpgraded: function() {
    return this.get('content.state') == "INSTALLED";
  }.property('content.state'),


  /**
   * depending on state run or install repo request
   * or show the installation process popup
   * @method installStackVersion
   */
  installStackVersion: function() {
    if (this.get('installInProgress')) {
      this.showProgressPopup();
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
    hostProgressPopupController.initPopup(popupTitle, requestIds, this);
  }
});
