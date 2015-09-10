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

App.UpgradeVersionBoxView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_version_box'),
  classNames: ['span4', 'version-box'],
  classNameBindings: ['versionClass'],

  /**
   * @type {string}
   * @constant
   */
  PROGRESS_STATUS: 'IN_PROGRESS',

  /**
   * flag that verify Upgrade availability when version in INSTALL_FAILED state
   * @type {boolean}
   */
  isUpgradeAvailable: false,

  /**
   * @type {boolean}
   */
  upgradeCheckInProgress: false,

  /**
   * progress of version installation
   * @type {number}
   */
  installProgress: function() {
    var requestId = App.get('testMode') ? 1 : App.db.get('repoVersionInstall', 'id')[0];
    var installRequest = App.router.get('backgroundOperationsController.services').findProperty('id', requestId);
    return (installRequest) ? installRequest.get('progress') : 0;
  }.property('App.router.backgroundOperationsController.serviceTimestamp'),

  /**
   * version is upgrading
   * @type {boolean}
   */
  isUpgrading: function () {
    return (this.get('controller.upgradeVersion') === this.get('content.displayName') && App.get('upgradeState') !== 'INIT');
  }.property('App.upgradeState', 'content.displayName', 'controller.upgradeVersion'),

  isRepoUrlsEditDisabled: function () {
    return ['INSTALLING', 'UPGRADING'].contains(this.get('content.status')) || this.get('isUpgrading');
  }.property('content.status', 'isUpgrading'),

  /**
   * @type {string}
   */
  versionClass: function () {
    return this.get('content.status') === 'CURRENT' ? 'current-version-box' : '';
  }.property('content.status'),

  /**
   * @type {boolean}
   */
  isOutOfSync: function () {
    return this.get('content.status') === 'OUT_OF_SYNC';
  }.property('content.status'),

  /**
   * map containing version (id, label)
   * this is used as param for <code>showHosts<code> method
   * @type {Object}
   */
  versionStateMap: {
    'current': {
      'id': 'current',
      'property': 'currentHosts',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.current')
    },
    'installed': {
      'id': 'installed',
      'property': 'installedHosts',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.installed')
    },
    'not_installed': {
      'id': 'installing',
      'property': 'notInstalledHosts',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.not_installed')
    }
  },

  /**
   * @type {object}
   * @default null
   */
  content: null,

  /**
   * object that describes how content should be displayed
   * @type {Em.Object}
   * TODO remove <code>isUpgrading</code> condition when transition of version states in API fixed
   */
  stateElement: function () {
    var currentVersion = this.get('controller.currentVersion');
    var status = this.get('content.status');
    var element = Em.Object.create({
      status: status,
      isInstalling: function () {
        return this.get('status') === 'INSTALLING';
      }.property('status'),
      buttons: [],
      isDisabled: false
    });
    var isInstalling = this.get('parentView.repoVersions').someProperty('status', 'INSTALLING');
    var isAborted = App.get('upgradeState') === 'ABORTED';

    if (status === 'CURRENT') {
      element.set('isLabel', true);
      element.set('text', Em.I18n.t('common.current'));
      element.set('class', 'label label-success');
    }
    else if (['INIT', 'INSTALL_FAILED', 'OUT_OF_SYNC'].contains(status) && !this.get('isUpgradeAvailable')) {
      element.set('isButton', true);
      element.set('text', Em.I18n.t('admin.stackVersions.version.installNow'));
      element.set('action', 'installRepoVersionConfirmation');
      element.set('isDisabled', !App.isAccessible('ADMIN') || this.get('controller.requestInProgress') || isInstalling);
    }
    else if (status === 'INSTALLING') {
      element.set('iconClass', 'icon-cog');
      element.set('isLink', true);
      element.set('text', Em.I18n.t('hosts.host.stackVersions.status.installing'));
      element.set('action', 'showProgressPopup');
    }
    else if ((status === 'INSTALLED' && !this.get('isUpgrading')) ||
             (status === 'INSTALL_FAILED' && this.get('isUpgradeAvailable'))) {
      if (stringUtils.compareVersions(this.get('content.repositoryVersion'), Em.get(currentVersion, 'repository_version')) === 1) {
        var isDisabled = !App.isAccessible('ADMIN') || this.get('controller.requestInProgress') || isInstalling;
        element.set('isButtonGroup', true);
        element.set('text', Em.I18n.t('admin.stackVersions.version.performUpgrade'));
        element.set('action', 'confirmUpgrade');
        element.get('buttons').pushObject({
          text: Em.I18n.t('admin.stackVersions.version.reinstall'),
          action: 'installRepoVersionConfirmation',
          isDisabled: isDisabled
        });
        element.set('isDisabled', isDisabled);
      }
      else {
        element.set('iconClass', 'icon-ok');
        element.set('isLink', true);
        element.set('text', Em.I18n.t('common.installed'));
        element.set('action', null);
      }
    }
    else if ((['UPGRADING', 'UPGRADE_FAILED', 'UPGRADED'].contains(status) || this.get('isUpgrading')) && !isAborted) {
      element.set('isLink', true);
      element.set('action', 'openUpgradeDialog');
      if (['HOLDING', 'HOLDING_FAILED', 'HOLDING_TIMEDOUT'].contains(App.get('upgradeState'))) {
        element.set('iconClass', 'icon-pause');
        if (this.get('controller.isDowngrade')) {
          element.set('text', Em.I18n.t('admin.stackVersions.version.downgrade.pause'));
        }
        else {
          element.set('text', Em.I18n.t('admin.stackVersions.version.upgrade.pause'));
        }
      }
      else {
        element.set('iconClass', 'icon-cog');
        if (this.get('controller.isDowngrade')) {
          element.set('text', Em.I18n.t('admin.stackVersions.version.downgrade.running'));
        }
        else {
          element.set('text', Em.I18n.t('admin.stackVersions.version.upgrade.running'));
        }
      }
    }
    else if (isAborted) {
      element.set('isButton', true);
      element.set('text', this.get('controller.isDowngrade') ? Em.I18n.t('common.reDowngrade') : Em.I18n.t('common.reUpgrade'));
      element.set('action', this.get('controller.isDowngrade') ? 'confirmRetryDowngrade' : 'confirmRetryUpgrade');
      element.set('isDisabled', this.get('controller.requestInProgress'));
    }
    return element;
  }.property(
    'content.status',
    'controller.isDowngrade',
    'isUpgrading',
    'controller.requestInProgress',
    'parentView.repoVersions.@each.status',
    'isUpgradeAvailable'),

  didInsertElement: function () {
    this.checkUpgradeAvailability();
    App.tooltip($('.link-tooltip'), {title: Em.I18n.t('admin.stackVersions.version.linkTooltip')});
    App.tooltip($('.hosts-tooltip'));
    App.tooltip($('.out-of-sync-badge'), {title: Em.I18n.t('hosts.host.stackVersions.status.out_of_sync')});
  },

  /**
   * run custom action of controller
   */
  runAction: function (event) {
    var target = event && event.target,
      action = event && event.context || this.get('stateElement.action');
    if (target && ($(target).hasClass('disabled') || $(target).parent().hasClass('disabled'))) {
      return;
    }
    if (action) {
      this.get('controller')[action](this.get('content'));
    }
  },

  /**
   * @param App.RepositoryVersion
   * */
  getStackVersionNumber: function(repository){
    var stackVersion = null; 
    var systems = repository.get('operatingSystems');

    systems.forEach(function (os) {
      os.get('repositories').forEach(function (repo) {
        stackVersion = repo.get('stackVersion');
        if (null != stackVersion)
          return stackVersion;
      });
    });

    return stackVersion; 
  },
  
  /**
   * show popup with repositories to edit
   * @return {App.ModalPopup}
   */
  editRepositories: function () {
    var self = this;
    var repoRecord = App.RepositoryVersion.find(this.get('content.id'));
    //make deep copy of repoRecord
    var repo = Em.Object.create({
      repoVersionId: repoRecord.get('id'),
      displayName: repoRecord.get('displayName'),
      repositoryVersion: repoRecord.get('displayName'),
      stackVersion: self.getStackVersionNumber(repoRecord),
      operatingSystems: repoRecord.get('operatingSystems').map(function (os) {
        return Em.Object.create({
          osType: os.get('osType'),
          isSelected: true,
          repositories: os.get('repositories').map(function (repository) {
            return Em.Object.create({
              repoName: repository.get('repoName'),
              repoId: repository.get('repoId'),
              baseUrl: repository.get('baseUrl'),
              hasError: false
            });
          })
        });
      })
    });

    return this.get('isRepoUrlsEditDisabled') ? null : App.ModalPopup.show({
      classNames: ['repository-list', 'sixty-percent-width-modal'],
      skipValidation: false,
      autoHeight: false,
      /**
       * @type {boolean}
       */
      serverValidationFailed: false,
      bodyClass: Ember.View.extend({
        content: repo,
        skipCheckBox: Ember.Checkbox.extend({
          classNames: ["align-checkbox"],
          change: function() {
            this.get('parentView.content.operatingSystems').forEach(function(os) {
              os.get('repositories').forEach(function(repo) {
                repo.set('skipValidation', this.get('checked'));
              }, this);
            }, this);
          }
        }),

        /**
         * set <code>disablePrimary</code> of popup depending on base URL validation
         */
        uiValidation: function () {
          var disablePrimary = !(App.get('isAdmin') && !App.get('isOperator'));

          this.get('content.operatingSystems').forEach(function (os) {
            os.get('repositories').forEach(function (repo) {
              disablePrimary = (!disablePrimary) ? repo.get('hasError') : disablePrimary;
            }, this);
          }, this);
          this.set('parentView.disablePrimary', disablePrimary);
        },
        templateName: require('templates/main/admin/stack_upgrade/edit_repositories'),
        didInsertElement: function () {
          App.tooltip($("[rel=skip-validation-tooltip]"), {placement: 'right'});
        }
      }),
      header: Em.I18n.t('common.repositories'),
      primary: Em.I18n.t('common.save'),
      disablePrimary: false,
      onPrimary: function () {
        var self = this;
        App.get('router.mainAdminStackAndUpgradeController').saveRepoOS(repo, this.get('skipValidation')).done(function(data){
          if (data.length > 0) {
            self.set('serverValidationFailed', true);
            self.set('disablePrimary', true);
          } else {
            self.hide();
          }
        })
      }
    });
  },

  /**
   * shows popup with listed hosts wich has current state of hostStackVersion
   * @param {object} event
   * @returns {App.ModalPopup}
   * @method showHostsListPopup
   */
  showHosts: function (event) {
    var status = event.contexts[0];
    var displayName = this.get('content.displayName');
    var hosts = this.get('content').get(status['property']);
    var self = this;
    hosts.sort();
    if (hosts.length) {
      return App.ModalPopup.show({
        bodyClass: Ember.View.extend({
          title: Em.I18n.t('admin.stackVersions.hosts.popup.title').format(displayName, status.label, hosts.length),
          hosts: hosts,
          template: Em.Handlebars.compile('<h4>{{view.title}}</h4><div class="limited-height-2">{{#each view.hosts}}<div>{{this}}</div>{{/each}}</div>')
        }),
        header: Em.I18n.t('admin.stackVersions.hosts.popup.header').format(status.label),
        primary: Em.I18n.t('admin.stackVersions.hosts.popup.primary'),
        secondary: Em.I18n.t('common.close'),
        onPrimary: function () {
          this.hide();
          self.filterHostsByStack(displayName, status.id);
        }
      });
    }
  },

  /**
   * goes to the hosts page with content filtered by repo_version_name and repo_version_state
   * @param displayName
   * @param state
   * @method filterHostsByStack
   */
  filterHostsByStack: function (displayName, state) {
    if (!displayName || !state) return;
    App.router.get('mainHostController').filterByStack(displayName, state);
    App.router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    App.router.transitionTo('hosts.index');
  },

  /**
   * when version in INSTALL_FAILED state it still could be upgraded if check passed
   */
  checkUpgradeAvailability: function () {
    if (this.get('content.status') === 'INSTALL_FAILED') {
      this.runUpgradeCheck();
    }
  }.observes('content.status'),

  /**
   * run Upgrade Check
   */
  runUpgradeCheck: function() {
    if (App.get('supports.preUpgradeCheck') && !this.get('upgradeCheckInProgress')) {
      App.ajax.send({
        name: "admin.rolling_upgrade.pre_upgrade_check",
        sender: this,
        data: {
          value: this.get('content.repositoryVersion'),
          label: this.get('content.displayName')
        },
        success: "runUpgradeCheckSuccess",
        error: "runUpgradeCheckError"
      });
      this.set('upgradeCheckInProgress', true);
    }
  },

  runUpgradeCheckSuccess: function (data) {
    this.set('isUpgradeAvailable', !data.items.someProperty('UpgradeChecks.status', 'FAIL'));
    this.set('upgradeCheckInProgress', false);
  },

  runUpgradeCheckError: function() {
    this.set('isUpgradeAvailable', false);
    this.set('upgradeCheckInProgress', false);
  }
});
