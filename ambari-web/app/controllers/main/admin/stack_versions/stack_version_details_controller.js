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
   * timeOut function to load updated progress
   * when install repo wersion is running
   */
  timeoutRef: null,

  /**
   * true if stack version install is in progress
   * @type {Boolean}
   */
  installFailed: function() {
    return this.get('content.stackVersion.state') == "INSTALL_FAILED";
  }.property('content.stackVersion.state'),
  /**
   * true if stack version install is in progress
   * @type {Boolean}
   */
  installInProgress: function() {
    return this.get('content.stackVersion.state') == "INSTALLING";
  }.property('content.stackVersion.state'),

  /**
   * true if repo version is installed on all hosts but not upgraded
   * @type {Boolean}
   */
  installComplete: function() {
    return this.get('content.stackVersion.state')
      && !["INSTALLING", "INSTALL_FAILED", "OUT_OF_SYNC"].contains(this.get('content.stackVersion.state'));
  }.property('content.stackVersion.state'),

  /**
   * true if repo version is not installed
   * this flag is used for install/reinstall button
   * we should show this button when there is no stackVersion (instead init state)
   * or when <code>INSTALL_FAILED<code> state
   * @type {Boolean}
   */
  notInstalled: function() {
    return !this.get('content.stackVersion.state') || ["INSTALL_FAILED", "OUT_OF_SYNC"].contains(this.get('content.stackVersion.state'));
  }.property('content.stackVersion.state'),

  /**
   * true if repo version is current
   * @type {Boolean}
   */
  current: function() {
    return this.get('content.stackVersion.state') == "CURRENT";
  }.property('content.stackVersion.state'),

  /**
   * counter that is shown on install button
   * @type {Number}
   */
  hostsToInstall: function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.notInstalledHosts.length') : App.get('allHostNames.length');
  }.property('content.stackVersion.notInstalledHosts.length'),

  /**
   * persentage of install progress
   * @type {Number}
   */
  progress: 0,

  /**
   * opens a popup with installations state per host
   * @method showProgressPopup
   */
  showProgressPopup: function() {
    var popupTitle = Em.I18n.t('admin.stackVersions.details.install.hosts.popup.title').format(this.get('content.displayName'));
    var requestIds = App.get('testMode') ? [1] : App.db.get('repoVersion', 'id');
    var hostProgressPopupController = App.router.get('highAvailabilityProgressPopupController');
    hostProgressPopupController.initPopup(popupTitle, requestIds, this);
  },

  /**
   * runs <code>updateProgress<code> method
   * to keep information up-to-date
   * @method doPolling
   */
  doPolling: function () {
    var self = this;
    self.updateProgress();
    this.set('timeoutRef', setTimeout(function () {
      if (self.get('installInProgress')) {
        self.doPolling();
      } else {
        clearTimeout(self.get('timeoutRef'));
      }
    }, 3000));
  },

  /**
   * runs ajax request to get current progress of
   * installing repo version to cluster
   * @returns {$.ajax}
   * @method updateProgress
   */
  updateProgress: function() {
    return App.ajax.send({
      'name': 'admin.high_availability.polling',
      'sender': this,
      'data': {
        requestId: App.db.get('repoVersion', 'id')
      },
      'success': 'updateProgressSuccess'
    });
  },

  /**
   * success calback for updateProgress
   * @param data
   * @method updateProgressSuccess
   */
  updateProgressSuccess: function(data) {
    if (Em.get(data, 'Requests.progress_percent')) {
      this.set('progress', parseInt(Em.get(data, 'Requests.progress_percent')));
      this.set('logs', data.tasks);
    }
  },

  /**
   * sends request to install repoVersion to the cluster
   * and create clusterStackVersion resourse
   * @param event
   * @return {$.ajax}
   * @method installRepoVersion
   */
  installRepoVersion: function (event) {
    var repo = event.context;
    var data = {
      ClusterStackVersions: {
        stack: repo.get('stackVersionType'),
        version: repo.get('stackVersionNumber'),
        repository_version: repo.get('repositoryVersion')
      },
      id: repo.get('id')
    };
    return App.ajax.send({
      name: 'admin.stack_version.install.repo_version',
      sender: this,
      data: data,
      success: 'installRepoVersionSuccess'
    });
  },

  /**
   * success callback for <code>installRepoVersion()<code>
   * saves request id to the db, and redirect user to the just
   * created clusterStackVersion.
   * @param data
   * @param opt
   * @param params
   * @method installStackVersionSuccess
   */
  installRepoVersionSuccess: function (data, opt, params) {
    var self = this;
    App.db.set('repoVersion', 'id', [data.Requests.id]);
    App.get('router.repoVersionsManagementController').loadStackVersionsToModel(true).done(function() {
      var repoVersion = App.RepositoryVersion.find(params.id);
      if (App.get('router.currentState.name') == "update") {
        App.router.transitionTo('main.admin.adminStackVersions.version', repoVersion);
      } else {
        self.set('content', repoVersion);
      }
    });
  }
});
