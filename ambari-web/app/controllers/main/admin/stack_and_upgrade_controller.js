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

App.MainAdminStackAndUpgradeController = Em.Controller.extend(App.LocalStorage, {
  name: 'mainAdminStackAndUpgradeController',

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {object}
   * @default null
   */
  upgradeData: null,

  /**
   * @type {number}
   * @default null
   */
  upgradeId: null,

  /**
   * @type {string}
   * @default null
   */
  upgradeVersion: null,

  /**
   * @type {boolean}
   * @default false
   */
  isDowngrade: false,

  /**
   * version that currently applied to server
   * should be plain object, because stored to localStorage
   * @type {object|null}
   */
  currentVersion: null,

  /**
   * versions to which cluster could be upgraded
   * @type {Array}
   */
  targetVersions: [],

  /**
   * @type {boolean} true if some request that should disable actions is in progress
   */
  requestInProgress: false,
  /**
   * @type {boolean} true while no updated upgrade info is loaded after retry
   */
  isRetryPending: false,
  /**
   * properties that stored to localStorage to resume wizard progress
   */
  wizardStorageProperties: ['upgradeId', 'upgradeVersion', 'currentVersion', 'isDowngrade'],

  /**
   * mutable properties of Upgrade Task
   * @type {Array}
   */
  taskDetailsProperties: ['status', 'stdout', 'stderr', 'error_log', 'host_name', 'output_log'],

  /**
   * Context for Finalize item
   * @type {string}
   */
  finalizeContext: 'Confirm Finalize',

  /**
   * Check if current item is Finalize
   * @type {boolean}
   */
  isFinalizeItem: false,

  isLoadUpgradeDataPending: false,

  /**
   * path to the mock json
   * @type {String}
   */
  mockRepoUrl: '/data/stack_versions/repo_versions_all.json',

  /**
   * api to get RepoVersions
   * @type {String}
   */
  realRepoUrl: function () {
    return App.get('apiPrefix') + App.get('stackVersionURL') +
      '/compatible_repository_versions?fields=*,operating_systems/*,operating_systems/repositories/*';
  }.property('App.stackVersionURL'),

  /**
   * path to the mock json
   * @type {String}
   */
  mockStackUrl: '/data/stack_versions/stack_version_all.json',

  /**
   * api to get ClusterStackVersions with repository_versions (use to init data load)
   * @type {String}
   */
  realStackUrl: function () {
    return App.get('apiPrefix') + '/clusters/' + App.get('clusterName') +
      '/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/repositories/*';
  }.property('App.clusterName'),

  /**
   * api to get ClusterStackVersions without repository_versions (use to update data)
   * @type {String}
   */
  realUpdateUrl: function () {
    return App.get('apiPrefix') + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=ClusterStackVersions/*';
  }.property('App.clusterName'),

  /**
   * Determines if list of services with checks that failed and were skipped by user during the upgrade is loaded
   * @type {boolean}
   */
  areSkippedServiceChecksLoaded: false,

  /**
   * List of services with checks that failed and were skipped by user during the upgrade
   * @type {array}
   */
  skippedServiceChecks: [],

  /**
   * status of tasks/items/groups which should be grayed out and disabled
   * @type {Array}
   */
  nonActiveStates: ['PENDING', 'ABORTED'],

  init: function () {
    this.initDBProperties();
  },

  /**
   * restore data from localStorage
   */
  initDBProperties: function () {
    var props = this.getDBProperties(this.get('wizardStorageProperties'));
    Em.keys(props).forEach(function (k) {
      if (props[k]) {
        this.set(k, props[k]);
      }
    }, this);
  },

  /**
   * load all data:
   * - upgrade data
   * - stack versions
   * - repo versions
   */
  load: function () {
    var dfd = $.Deferred();
    var self = this;

    this.loadUpgradeData(true).done(function() {
      self.loadStackVersionsToModel(true).done(function () {
        self.loadRepoVersionsToModel().done(function() {
          var currentVersion = App.StackVersion.find().findProperty('state', 'CURRENT');
          if (currentVersion) {
            self.set('currentVersion', {
              repository_version: currentVersion.get('repositoryVersion.repositoryVersion'),
              repository_name: currentVersion.get('repositoryVersion.displayName')
            });
          }
          dfd.resolve();
        });
      });
    });
    return dfd.promise();
  },

  /**
   * load upgrade tasks by upgrade id
   * @return {$.Deferred}
   * @param {boolean} onlyState
   */
  loadUpgradeData: function (onlyState) {
    var upgradeId = this.get('upgradeId'),
      deferred = $.Deferred(),
      self = this;

    if (Em.isNone(upgradeId)) {
      deferred.resolve();
      console.log('Upgrade in INIT state');
    } else {
      this.set('isLoadUpgradeDataPending', true);
      App.ajax.send({
        name: (onlyState) ? 'admin.upgrade.state' : 'admin.upgrade.data',
        sender: this,
        data: {
          id: upgradeId
        },
        success: 'loadUpgradeDataSuccessCallback'
      }).then(deferred.resolve).complete(function () {
          self.set('isLoadUpgradeDataPending', false);
        });
    }
    return deferred.promise();
  },

  /**
   * parse and push upgrade tasks to controller
   * @param data
   */
  loadUpgradeDataSuccessCallback: function (data) {
    if (Em.isNone(data)) return;
    App.set('upgradeState', data.Upgrade.request_status);
    this.setDBProperty('upgradeState', data.Upgrade.request_status);
    if (data.upgrade_groups) {
      this.updateUpgradeData(data);
    }
    if (this.get('isRetryPending') && data.Upgrade.request_status != 'ABORTED') {
      this.setProperties({
        requestInProgress: false,
        isRetryPending: false
      });
    }
  },

  /**
   * update data of Upgrade
   * @param {object} newData
   */
  updateUpgradeData: function (newData) {
    var oldData = this.get('upgradeData'),
      nonActiveStates = this.get('nonActiveStates'),
      groupsMap = {},
      itemsMap = {};

    if (Em.isNone(oldData) || (newData.upgrade_groups.length !== oldData.upgradeGroups.length)) {
      this.initUpgradeData(newData);
    } else {
      //create entities maps
      newData.upgrade_groups.forEach(function (newGroup) {
        groupsMap[newGroup.UpgradeGroup.group_id] = newGroup.UpgradeGroup;
        newGroup.upgrade_items.forEach(function (item) {
          itemsMap[item.UpgradeItem.stage_id] = item.UpgradeItem;
        })
      });

      //update existed entities with new data
      oldData.upgradeGroups.forEach(function (oldGroup) {
        oldGroup.set('status', groupsMap[oldGroup.get('group_id')].status);
        oldGroup.set('progress_percent', groupsMap[oldGroup.get('group_id')].progress_percent);
        oldGroup.set('completed_task_count', groupsMap[oldGroup.get('group_id')].completed_task_count);
        oldGroup.upgradeItems.forEach(function (item) {
          item.set('status', itemsMap[item.get('stage_id')].status);
          item.set('progress_percent', itemsMap[item.get('stage_id')].progress_percent);
        });
        var hasExpandableItems = oldGroup.upgradeItems.some(function (item) {
          return !nonActiveStates.contains(item.get('status'));
        });
        oldGroup.set('hasExpandableItems', hasExpandableItems);
      });
      oldData.set('Upgrade', newData.Upgrade);
    }
  },

  /**
   * change structure of Upgrade
   * In order to maintain nested views in template object should have direct link to its properties, for example
   * item.UpgradeItem.<properties> -> item.<properties>
   * @param {object} newData
   */
  initUpgradeData: function (newData) {
    var upgradeGroups = [],
      nonActiveStates = this.get('nonActiveStates');

    //wrap all entities into App.upgradeEntity
    newData.upgrade_groups.forEach(function (newGroup) {
      var hasExpandableItems = newGroup.upgrade_items.some(function (item) {
          return !nonActiveStates.contains(item.UpgradeItem.status);
        }),
        oldGroup = App.upgradeEntity.create({type: 'GROUP', hasExpandableItems: hasExpandableItems}, newGroup.UpgradeGroup),
        upgradeItems = [];
      newGroup.upgrade_items.forEach(function (item) {
        var oldItem = App.upgradeEntity.create({type: 'ITEM'}, item.UpgradeItem);
        oldItem.set('tasks', []);
        upgradeItems.pushObject(oldItem);
      });
      upgradeItems.reverse();
      oldGroup.set('upgradeItems', upgradeItems);
      upgradeGroups.pushObject(oldGroup);
    });
    upgradeGroups.reverse();
    this.set('upgradeData', Em.Object.create({
      upgradeGroups: upgradeGroups,
      Upgrade: newData.Upgrade
    }));
  },

  /**
   * request Upgrade Item and its tasks from server
   * @return {$.ajax}
   */
  getUpgradeItem: function (item) {
    return App.ajax.send({
      name: 'admin.upgrade.upgrade_item',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        groupId: item.get('group_id'),
        stageId: item.get('stage_id')
      },
      success: 'getUpgradeItemSuccessCallback'
    });
  },

  /**
   * success callback of <code>getTasks</code>
   * @param {object} data
   */
  getUpgradeItemSuccessCallback: function (data) {
    this.get('upgradeData.upgradeGroups').forEach(function (group) {
      if (group.get('group_id') === data.UpgradeItem.group_id) {
        group.get('upgradeItems').forEach(function (item) {
          if (item.get('stage_id') === data.UpgradeItem.stage_id) {
            if (item.get('tasks.length')) {
              item.set('isTasksLoaded', true);
              data.tasks.forEach(function (task) {
                var currentTask = item.get('tasks').findProperty('id', task.Tasks.id);
                this.get('taskDetailsProperties').forEach(function (property) {
                  currentTask.set(property, task.Tasks[property]);
                }, this);
              }, this);
            } else {
              var tasks = [];
              data.tasks.forEach(function (task) {
                tasks.pushObject(App.upgradeEntity.create({type: 'TASK'}, task.Tasks));
              });
              item.set('tasks', tasks);
            }
            item.set('isTasksLoaded', true);
          }
        }, this);
      }
    }, this);
  },

  /**
   * downgrade confirmation popup
   * @param {object} event
   */
  confirmDowngrade: function (event) {
    var self = this;
    var currentVersion = this.get('currentVersion');
    return App.showConfirmationPopup(
      function() {
        self.downgrade.call(self, currentVersion, event);
      },
      Em.I18n.t('admin.stackUpgrade.downgrade.body').format(currentVersion.repository_name),
      null,
      Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format(currentVersion.repository_name),
      Em.I18n.t('admin.stackUpgrade.downgrade.proceed')
    );
  },

  /**
   * make call to start downgrade process
   * @param {object} currentVersion
   * @param {object} event
   */
  downgrade: function (currentVersion, event) {
    this.set('requestInProgress', true);
    this.abortUpgrade();
    App.ajax.send({
      name: 'admin.downgrade.start',
      sender: this,
      data: {
        from: App.RepositoryVersion.find().findProperty('displayName', this.get('upgradeVersion')).get('repositoryVersion'),
        value: currentVersion.repository_version,
        label: currentVersion.repository_name,
        isDowngrade: true
      },
      success: 'upgradeSuccessCallback',
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    });
  },

  /**
   * abort upgrade (in order to start Downgrade)
   */
  abortUpgrade: function () {
    return App.ajax.send({
      name: 'admin.upgrade.abort',
      sender: this,
      data: {
        upgradeId: this.get('upgradeId')
      }
    });
  },

  retryUpgrade: function () {
    this.setProperties({
      requestInProgress: true,
      isRetryPending: true
    });
    return App.ajax.send({
      name: 'admin.upgrade.retry',
      sender: this,
      data: {
        upgradeId: this.get('upgradeId')
      }
    });
  },

  /**
   * make call to start upgrade process and show popup with current progress
   * @param {object} version
   */
  upgrade: function (version) {
    this.set('requestInProgress', true);
    App.ajax.send({
      name: 'admin.upgrade.start',
      sender: this,
      data: version,
      success: 'upgradeSuccessCallback',
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    });
    this.setDBProperty('currentVersion', this.get('currentVersion'));
  },

  /**
   * success callback of <code>upgrade()</code>
   * @param {object} data
   */
  upgradeSuccessCallback: function (data, opt, params) {
    this.set('upgradeData', null);
    this.set('upgradeId', data.resources[0].Upgrade.request_id);
    this.set('upgradeVersion', params.label);
    this.set('isDowngrade', !!params.isDowngrade);
    this.setDBProperties({
      upgradeVersion: params.label,
      upgradeId: data.resources[0].Upgrade.request_id,
      upgradeState: 'PENDING',
      isDowngrade: !!params.isDowngrade
    });
    App.set('upgradeState', 'PENDING');
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
    this.load();
    this.openUpgradeDialog();
  },

  /**
   * upgrade confirmation popup
   * @param {object} version
   * @return App.ModalPopup
   */
  confirmUpgrade: function (version) {
    var self = this;

    return App.showConfirmationPopup(
      function () {
        self.runPreUpgradeCheck.call(self, version);
      },
      Em.I18n.t('admin.stackUpgrade.upgrade.confirm.body').format(version.get('displayName')),
      null,
      Em.I18n.t('admin.stackUpgrade.dialog.header').format(version.get('displayName'))
    );
  },

  /**
   * send request for pre upgrade check
   * @param version
   */
  runPreUpgradeCheck: function(version) {
    var params = {
      value: version.get('repositoryVersion'),
      label: version.get('displayName')
    };

    if (App.get('supports.preUpgradeCheck')) {
      this.set('requestInProgress', true);
      App.ajax.send({
        name: "admin.rolling_upgrade.pre_upgrade_check",
        sender: this,
        data: params,
        success: "runPreUpgradeCheckSuccess",
        error: "runPreUpgradeCheckError"
      });
    } else {
      this.upgrade(params);
    }
  },

  /**
   * success callback of <code>runPreUpgradeCheckSuccess()</code>
   * if there are some fails - it shows popup else run upgrade
   * @param data {object}
   * @param opt {object}
   * @param params {object}
   * @returns {App.ModalPopup|undefined}
   */
  runPreUpgradeCheckSuccess: function (data, opt, params) {
    var self = this;
    if (data.items.someProperty('UpgradeChecks.status', 'FAIL') || data.items.someProperty('UpgradeChecks.status', 'WARNING')) {
      this.set('requestInProgress', false);
      var header = Em.I18n.t('popup.clusterCheck.Upgrade.header').format(params.label),
        failTitle = Em.I18n.t('popup.clusterCheck.Upgrade.fail.title'),
        failAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.fail.alert')),
        warningTitle = Em.I18n.t('popup.clusterCheck.Upgrade.warning.title'),
        warningAlert = new Em.Handlebars.SafeString(Em.I18n.t('popup.clusterCheck.Upgrade.warning.alert')),
        configsMergeWarning = data.items.findProperty('UpgradeChecks.id', "CONFIG_MERGE"),
        configs = [];
      if (configsMergeWarning && Em.get(configsMergeWarning, 'UpgradeChecks.status') === 'WARNING') {
        data.items = data.items.rejectProperty('UpgradeChecks.id', 'CONFIG_MERGE');
        var configsMergeCheckData = Em.get(configsMergeWarning, 'UpgradeChecks.failed_detail');
        if (configsMergeCheckData) {
          configs = configsMergeCheckData.map(function (item) {
            var isDeprecated = Em.isNone(item.new_stack_value),
              willBeRemoved = Em.isNone(item.result_value);
            return {
              type: item.type,
              name: item.property,
              currentValue: item.current,
              recommendedValue: isDeprecated ? Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated') : item.new_stack_value,
              isDeprecated: isDeprecated,
              resultingValue: willBeRemoved ? Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved') : item.result_value,
              willBeRemoved: willBeRemoved
            };
          });
        }
      }
      App.showClusterCheckPopup(data, header, failTitle, failAlert, warningTitle, warningAlert, function () {
        self.upgrade(params);
      }, configs, params.label);
    } else {
      this.upgrade(params);
    }
  },

  runPreUpgradeCheckError: function() {
    this.set('requestInProgress', false);
  },

  confirmRetryUpgrade: function (version) {
    var self = this;
    return App.showConfirmationPopup(
      function () {
        self.retryUpgrade();
      },
      Em.I18n.t('admin.stackUpgrade.upgrade.retry.confirm.body').format(version.get('displayName')),
      null,
      Em.I18n.t('admin.stackUpgrade.dialog.header').format(version.get('displayName'))
    );
  },

  confirmRetryDowngrade: function () {
    var self = this,
      currentVersion = this.get('currentVersion');
    return App.showConfirmationPopup(
      function() {
        self.retryUpgrade();
      },
      Em.I18n.t('admin.stackUpgrade.downgrade.retry.body').format(currentVersion.repository_name),
      null,
      Em.I18n.t('admin.stackUpgrade.dialog.downgrade.header').format(currentVersion.repository_name),
      Em.I18n.t('admin.stackUpgrade.downgrade.proceed')
    );
  },

  /**
   * confirmation popup before install repository version
   */
  installRepoVersionConfirmation: function (repo) {
    var self = this;
    return App.showConfirmationPopup(function () {
        self.installRepoVersion(repo);
      },
      Em.I18n.t('admin.stackVersions.version.install.confirm').format(repo.get('displayName'))
    );
  },

  /**
   * sends request to install repoVersion to the cluster
   * and create clusterStackVersion resourse
   * @param {Em.Object} repo
   * @return {$.ajax}
   * @method installRepoVersion
   */
  installRepoVersion: function (repo) {
    this.set('requestInProgress', true);
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
      success: 'installRepoVersionSuccess',
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    });
  },

  /**
   * transform repo data into json for
   * saving changes to repository version
   * @param {Em.Object} repo
   * @returns {{operating_systems: Array}}
   */
  prepareRepoForSaving: function(repo) {
    var repoVersion = { "operating_systems": [] };

    repo.get('operatingSystems').forEach(function (os, k) {
      repoVersion.operating_systems.push({
        "OperatingSystems": {
          "os_type": os.get("osType")
        },
        "repositories": []
      });
      os.get('repositories').forEach(function (repository) {
        repoVersion.operating_systems[k].repositories.push({
          "Repositories": {
            "base_url": repository.get('baseUrl'),
            "repo_id": repository.get('repoId'),
            "repo_name": repository.get('repoName')
          }
        });
      });
    });
    return repoVersion;
  },

  /**
   * Return stack version for the repo object
   * @param {Em.Object} repo
   * */
  getStackVersionNumber: function(repo){
    var stackVersionNumber = repo.get('stackVersion');
    if(null == stackVersionNumber)
      stackVersionNumber = App.get('currentStackVersion');
    return stackVersionNumber;
  },
  
  /**
   * perform validation if <code>skip<code> is  false and run save if
   * validation successfull or run save without validation is <code>skip<code> is true
   * @param {Em.Object} repo
   * @param {boolean} skip
   * @returns {$.Deferred}
   */
  saveRepoOS: function (repo, skip) {
    var self = this;
    var deferred = $.Deferred();
    this.validateRepoVersions(repo, skip).done(function(data) {
      if (data.length > 0) {
        deferred.resolve(data);
      } else {
        var repoVersion = self.prepareRepoForSaving(repo);
        var stackVersionNumber = self.getStackVersionNumber(repo);
        console.log("Repository stack version:"+stackVersionNumber);
        
        App.ajax.send({
          name: 'admin.stack_versions.edit.repo',
          sender: this,
          data: {
            stackName: App.get('currentStackName'),
            stackVersion: stackVersionNumber,
            repoVersionId: repo.get('repoVersionId'),
            repoVersion: repoVersion
          }
        }).success(function() {
          deferred.resolve([]);
        });
      }
    });
    return deferred.promise();
  },
  
  /**
   * send request for validation for each repository
   * @param {Em.Object} repo
   * @param {boolean} skip
   * @returns {*}
   */
  validateRepoVersions: function(repo, skip) {
    var deferred = $.Deferred(),
      totalCalls = 0,
      invalidUrls = [];
    
    if (skip) {
      deferred.resolve(invalidUrls);
    } else {
      var stackVersionNumber = this.getStackVersionNumber(repo);
      repo.get('operatingSystems').forEach(function (os) {
        if (os.get('isSelected')) {
          os.get('repositories').forEach(function (repo) {
            totalCalls++;
            App.ajax.send({
              name: 'admin.stack_versions.validate.repo',
              sender: this,
              data: {
                repo: repo,
                repoId: repo.get('repoId'),
                baseUrl: repo.get('baseUrl'),
                osType: os.get('osType'),
                stackName: App.get('currentStackName'),
                stackVersion: stackVersionNumber
              }
            })
              .success(function () {
                totalCalls--;
                if (totalCalls === 0) deferred.resolve(invalidUrls);
              })
              .error(function () {
                repo.set('hasError', true);
                invalidUrls.push(repo);
                totalCalls--;
                if (totalCalls === 0) deferred.resolve(invalidUrls);
              });
          });
        } else {
          return deferred.resolve(invalidUrls);
        }
      });
    }
    return deferred.promise();
  },

  /**
   * success callback for <code>installRepoVersion()<code>
   * saves request id to the db
   * @param data
   * @param opt
   * @param params
   * @method installStackVersionSuccess
   */
  installRepoVersionSuccess: function (data, opt, params) {
    var version = App.RepositoryVersion.find(params.id);
    App.db.set('repoVersionInstall', 'id', [data.Requests.id]);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
    version.set('defaultStatus', 'INSTALLING');
    if (version.get('stackVersion')) {
      version.set('stackVersion.state', 'INSTALLING');
    }
  },

  /**
   * opens a popup with installations state per host
   * @param {Em.Object} version
   * @method showProgressPopup
   */
  showProgressPopup: function(version) {
    var popupTitle = Em.I18n.t('admin.stackVersions.details.install.hosts.popup.title').format(version.get('displayName'));
    var requestIds = App.get('testMode') ? [1] : App.db.get('repoVersionInstall', 'id');
    var hostProgressPopupController = App.router.get('highAvailabilityProgressPopupController');
    hostProgressPopupController.initPopup(popupTitle, requestIds, this);
  },

  /**
   * reset upgradeState to INIT when upgrade is COMPLETED
   * and clean auxiliary data
   */
  finish: function () {
    if (App.get('upgradeState') === 'COMPLETED') {
      this.setDBProperties({
        upgradeId: undefined,
        upgradeState: 'INIT',
        upgradeVersion: undefined,
        currentVersion: undefined,
        isDowngrade: undefined
      });
      App.clusterStatus.setClusterStatus({
        localdb: App.db.data
      });
      App.set('upgradeState', 'INIT');
    }
  }.observes('App.upgradeState'),

  /**
   * Check <code>App.upgradeState</code> for HOLDING
   * If it is, send request to check if current item is Finalize
   * @method updateFinalize
   */
  updateFinalize: function () {
    var upgradeState = App.get('upgradeState');
    if (upgradeState === 'HOLDING') {
      return App.ajax.send({
        name: 'admin.upgrade.finalizeContext',
        sender: this,
        success: 'updateFinalizeSuccessCallback',
        error: 'updateFinalizeErrorCallback'
      })
    }
    else {
      this.set('isFinalizeItem', false);
    }
  }.observes('App.upgradeState'),

  /**
   *
   * @param {object|null} data
   * @method updateFinalizeSuccessCallback
   */
  updateFinalizeSuccessCallback: function (data) {
    var context = data ? Em.get(data, 'upgrade_groups.firstObject.upgrade_items.firstObject.UpgradeItem.context') : '';
    this.set('isFinalizeItem', context === this.get('finalizeContext'));
  },

  updateFinalizeErrorCallback: function() {
    this.set('isFinalizeItem', false);
  },

  /**
   * show dialog with tasks of upgrade
   * @return {App.ModalPopup}
   */
  openUpgradeDialog: function () {
    App.router.transitionTo('admin.stackUpgrade');
  },

  /**
   * returns url to get data for repoVersion or clusterStackVersion
   * @param {Boolean} stack true if load clusterStackVersion
   * @param {Boolean} fullLoad true if load all data
   * @returns {String}
   * @method getUrl
   */
  getUrl: function(stack, fullLoad) {
    if (App.get('testMode')) {
      return stack ? this.get('mockStackUrl') : this.get('mockRepoUrl')
    } else {
      if (fullLoad) {
        return stack ? this.get('realStackUrl') : this.get('realRepoUrl');
      } else {
        return this.get('realUpdateUrl');
      }
    }
  },

  /**
   * get stack versions from server and push it to model
   * @return {*}
   * @method loadStackVersionsToModel
   */
  loadStackVersionsToModel: function (fullLoad) {
    var dfd = $.Deferred();
    App.HttpClient.get(this.getUrl(true, fullLoad), App.stackVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * get repo versions from server and push it to model
   * @return {*}
   * @params {Boolean} isUpdate - if true loads part of data that need to be updated
   * @method loadRepoVersionsToModel()
   */
  loadRepoVersionsToModel: function () {
    var dfd = $.Deferred();
    App.HttpClient.get(this.getUrl(false, true), App.repoVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * set status to Upgrade item
   * @param item
   * @param status
   */
  setUpgradeItemStatus: function(item, status) {
    this.set('requestInProgress', true);
    return App.ajax.send({
      name: 'admin.upgrade.upgradeItem.setState',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        itemId: item.get('stage_id'),
        groupId: item.get('group_id'),
        status: status
      },
      callback: function() {
        this.sender.set('requestInProgress', false);
      }
    }).done(function () {
      item.set('status', status);
    });
  },

  currentVersionObserver: function () {
    var versionNumber = this.get('currentVersion.repository_version');
    var currentVersionObject = App.RepositoryVersion.find().findProperty('status', 'CURRENT');
    var versionName = currentVersionObject && currentVersionObject.get('stackVersionType');
    App.set('isStormMetricsSupported', versionName != 'HDP' || stringUtils.compareVersions(versionNumber, '2.2.2') > -1 || !versionNumber);
  }.observes('currentVersion.repository_version'),

  /**
   * get the installed repositories of HDP from server
   */
  loadRepositories: function () {
    if (App.router.get('clusterController.isLoaded')) {
      var nameVersionCombo = App.get('currentStackVersion');
      var stackName = nameVersionCombo.split('-')[0];
      var stackVersion = nameVersionCombo.split('-')[1];
      App.ajax.send({
        name: 'cluster.load_repositories',
        sender: this,
        data: {
          stackName: stackName,
          stackVersion: stackVersion
        },
        success: 'loadRepositoriesSuccessCallback',
        error: 'loadRepositoriesErrorCallback'
      });
    }
  }.observes('App.router.clusterController.isLoaded'),

  loadRepositoriesSuccessCallback: function (data) {
    var allRepos = [];
    data.items.forEach(function (os) {
      os.repositories.forEach(function (repository) {
        var osType = repository.Repositories.os_type;
        var repo = Em.Object.create({
          baseUrl: repository.Repositories.base_url,
          osType: osType,
          repoId: repository.Repositories.repo_id,
          repoName : repository.Repositories.repo_name,
          stackName : repository.Repositories.stack_name,
          stackVersion : repository.Repositories.stack_version,
          isFirst: false
        });
        var group = allRepos.findProperty('name', osType);
        if (!group) {
          group = {
            name: osType,
            repositories: []
          };
          repo.set('isFirst', true);
          allRepos.push(group);
        }
        group.repositories.push(repo);
      });
    }, this);
    allRepos.stackVersion = App.get('currentStackVersionNumber');
    this.set('allRepos', allRepos);
  },

  loadRepositoriesErrorCallback: function (request, ajaxOptions, error) {
    console.log('Error message is: ' + request.responseText);
  }
});
