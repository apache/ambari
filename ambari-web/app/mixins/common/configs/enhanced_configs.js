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
var blueprintUtils = require('utils/blueprint');

App.EnhancedConfigsMixin = Em.Mixin.create(App.ConfigWithOverrideRecommendationParser, {

  /**
   * this value is used for observing
   * whether recommendations for dependent properties was received from server
   * @type {number}
   */
  recommendationTimeStamp: null,

  /**
   * this property is used to force update min/max values
   * for not default config groups
   * @type {boolean}
   */
  forceUpdateBoundaries: false,

  /**
   * object with loaded recommendations
   *
   * @type {?Object}
   */
  recommendationsConfigs: null,

  /**
   * list of recommendations that should be applied without any config changes
   *
   * @type {Object[]}
   */
  initialRecommendations: [],

  /**
   * flag is true when Ambari changes some of the dependent properties
   * @type {boolean}
   */
  hasChangedDependencies: Em.computed.and('isControllerSupportsEnhancedConfigs', 'changedProperties.length'),

  /**
   * defines is block with changed dependent configs should be shown
   * rely on controller
   * @type {boolean}
   */
  isControllerSupportsEnhancedConfigs: Em.computed.existsIn('name', ['wizardStep7Controller','mainServiceInfoConfigsController']),

  /**
   * Stores name and file name of changed config
   * This used only for capacity-scheduler
   *
   * @property {object}
   */
  currentlyChangedConfig: null,

  dependenciesGroupMessage: Em.I18n.t('popup.dependent.configs.dependencies.for.groups'),
  /**
   * message fro alert box for dependent configs
   * @type {string}
   */
  dependenciesMessage: function() {
    var changedProperties = this.get('changedProperties').filterProperty('saveRecommended');
    var changedServices = changedProperties.mapProperty('serviceName').uniq();
    var cfgLenSuffix = changedProperties.length === 1 ? 'singular' : 'plural';
    var sLenSuffix = changedServices.length === 1 ? 'singular' : 'plural';
    return Em.I18n.t('popup.dependent.configs.dependencies.config.' + cfgLenSuffix).format(changedProperties.length)
      + Em.I18n.t('popup.dependent.configs.dependencies.service.' + sLenSuffix).format(changedServices.length);
  }.property('changedProperties.length'),

  /**
   * dependent properties that was changed by Ambari
   * @type {Object[]}
   */
  changedProperties: function() {
    return this.get('recommendations').filter(function(dp) {
      return (this.get('selectedConfigGroup.isDefault') && Em.get(dp, 'configGroup').contains('Default'))
        || [this.get('selectedConfigGroup.name'), this.get('selectedConfigGroup.dependentConfigGroups') && this.get('selectedConfigGroup.dependentConfigGroups')[Em.get(dp, 'serviceName')]].contains(Em.get(dp, 'configGroup'));
    }, this);
  }.property('recommendations.@each.saveRecommended', 'recommendations.@each.recommendedValue', 'selectedConfigGroup'),

  /**
   * defines if change dependent group message should be shown
   * @type {boolean}
   */
  showSelectGroupsPopup: Em.computed.and('!selectedConfigGroup.isDefault', 'selectedService.dependentServiceNames.length'),

  /**
    * set default values for dependentGroups
   * @method setDependentGroups
   */
  setDependentGroups: function () {
    if (this.shouldSetDependentConfigs()) {
      this.get('selectedService.dependentServiceNames').forEach(function (serviceName) {
        if (!this.get('selectedConfigGroup.dependentConfigGroups')[serviceName]) {
          var stepConfig = this.get('stepConfigs').findProperty('serviceName', serviceName);
          if (stepConfig) {
            this.setDependentConfigGroups(stepConfig, serviceName);
          }
        }
      }, this);
    }
  }.observes('selectedConfigGroup'),

  /**
   *
   * @param {Em.Object} stepConfig
   * @param {string} serviceName
   */
  setDependentConfigGroups: function(stepConfig, serviceName) {
    stepConfig.get('configGroups').filterProperty('isDefault', false).forEach(function (configGroup) {
      this.get('selectedService.configGroups').filterProperty('isDefault', false).forEach(function (currentServiceGroup) {
        if (currentServiceGroup.get('dependentConfigGroups')[serviceName] !== configGroup.get('name')) {
          var dependentGroups = $.extend({}, this.get('selectedConfigGroup.dependentConfigGroups'));
          dependentGroups[serviceName] = configGroup.get('name');
          this.set('selectedConfigGroup.dependentConfigGroups', dependentGroups);
        }
      }, this);
    }, this);
  },

  /**
   *
   * @returns {boolean}
   */
  shouldSetDependentConfigs: function() {
    return Boolean(this.get('selectedConfigGroup') &&
           this.get('isControllerSupportsEnhancedConfigs') &&
           !this.get('selectedConfigGroup.isDefault') &&
           this.get('selectedService.dependentServiceNames.length'));
  },

  /******************************METHODS THAT WORKS WITH DEPENDENT CONFIGS *************************************/

  /**
   * get config group object for current service
   * @param serviceName
   * @returns {App.ConfigGroup|null}
   */
  getGroupForService: function(serviceName) {
    if (!this.get('stepConfigs') || this.get('stepConfigs.length') === 0) {
      return null;
    }
    if (this.get('selectedService.serviceName') === serviceName) {
      return this.get('selectedConfigGroup');
    } else {
      var stepConfig = this.get('stepConfigs').findProperty('serviceName', serviceName);
      if (stepConfig) {
        var groups = stepConfig.get('configGroups');
        if (this.get('selectedConfigGroup.isDefault')) {
          return groups.length ? groups.findProperty('isDefault') : null;
        } else {
          return groups.length ? groups.findProperty('name', this.get('selectedConfigGroup.dependentConfigGroups')[serviceName]) : null;
        }
      } else {
        return null;
      }
    }
  },

  clearRecommendationsInfo: function() {
    this.set('recommendationsConfigs', null);
  },

  clearRecommendations: function () {
    this.clearRecommendationsInfo();
    this.clearAllRecommendations();
  },

  /**
   * sends request to get values for dependent configs
   * @param {{type: string, name: string}[]} changedConfigs - list of changed configs to track recommendations
   * @param {Function} [onComplete]
   * @returns {$.ajax|null}
   */
  loadConfigRecommendations: function (changedConfigs, onComplete) {
    var updateDependencies = Em.isArray(changedConfigs) && changedConfigs.length > 0;
    var stepConfigs = this.get('stepConfigs');
    var requiredTags = [];

    if (updateDependencies || Em.isNone(this.get('recommendationsConfigs'))) {
      var recommendations = this.get('hostGroups');
      var dataToSend = this.getConfigRecommendationsParams(updateDependencies, changedConfigs);
      this.modifyRecommendationConfigGroups(recommendations);

      if (!stepConfigs.someProperty('serviceName', 'MISC')) {
        requiredTags.push({site: 'cluster-env', serviceName: 'MISC'});
      }
      if (Em.isArray(changedConfigs) && changedConfigs.mapProperty('type').uniq()[0] === 'capacity-scheduler') {
        this.set('currentlyChangedConfig', {
          name: 'capacity-scheduler',
          fileName: 'capacity-scheduler.xml'
        });
      } else {
        this.set('currentlyChangedConfig', null);
      }

      if (App.Service.find().someProperty('serviceName', 'HDFS') && !stepConfigs.someProperty('serviceName', 'HDFS')) {
        requiredTags.push({site: 'core-site', serviceName: 'HDFS'});
      }
      
      this.setUserContext(dataToSend);
      if (requiredTags.length) {
        this.loadAdditionalSites(requiredTags, stepConfigs, recommendations, dataToSend, onComplete);
      } else {
        this.addRecommendationRequestParams(recommendations, dataToSend, stepConfigs);
        return this.getRecommendationsRequest(dataToSend, onComplete);
      }
    } else {
      if (onComplete) {
        onComplete()
      }
    }
    return $.Deferred().resolve().promise();
  },

  /**
   *
   * @param recommendations
   * @param dataToSend
   * @param stepConfigs
   */
  addRecommendationRequestParams: function(recommendations, dataToSend, stepConfigs) {
    recommendations.blueprint.configurations = blueprintUtils.buildConfigsJSON(stepConfigs);
    dataToSend.recommendations = recommendations;
  },

  /**
   *
   * @param {Array} stepConfigs
   * @param {object} recommendations
   * @param {object} dataToSend
   * @param {Function} onComplete
   */
  loadAdditionalSites: function(sites, stepConfigs, recommendations, dataToSend, onComplete) {
    var self = this;
    App.config.getConfigsByTypes(sites).done(function (configs) {
      stepConfigs = stepConfigs.concat(configs);

      self.addRecommendationRequestParams(recommendations, dataToSend, stepConfigs);
      self.getRecommendationsRequest(dataToSend, onComplete);
    });
  },

  /**
   *
   * @param {object} recommendations
   */
  modifyRecommendationConfigGroups: function(recommendations) {
    var configGroup = this.get('selectedConfigGroup');

    if (configGroup && !configGroup.get('isDefault') && configGroup.get('hosts.length') > 0) {
      recommendations.config_groups = [this.buildConfigGroupJSON(this.get('selectedService.configs'), configGroup)];
    } else {
      delete recommendations.config_groups;
    }
  },

  /**
   *
   * @param {boolean} updateDependencies
   * @param {Array} changedConfigs
   * @returns {{recommend: string, hosts: *, services: *, changed_configurations: *}}
   */
  getConfigRecommendationsParams: function(updateDependencies, changedConfigs) {
    return {
      recommend: updateDependencies ? 'configuration-dependencies' : 'configurations',
      hosts: this.get('hostNames'),
      services: this.get('serviceNames'),
      changed_configurations: updateDependencies ? changedConfigs : undefined
    };
  },

  getRecommendationsRequest: function (dataToSend, callback) {
    var self = this;
    this.set('recommendationsInProgress', true);
    return App.ajax.send({
      name: 'config.recommendations',
      sender: self,
      data: {
        stackVersionUrl: App.get('stackVersionURL'),
        dataToSend: dataToSend
      },
      success: 'loadRecommendationsSuccess',
      error: 'loadRecommendationsError',
      callback: function () {
        self.set('recommendationsInProgress', false);
        self.set('recommendationTimeStamp', (new Date).getTime());
        if (callback) {
          callback()
        }
      }
    });
  },

  setUserContext: function(dataToSend) {
    var controllerName = this.get('content.controllerName');
    var changes = dataToSend.changed_configurations;
    if (changes) {
      dataToSend['user_context'] = {"operation" : "EditConfig"};
    } else {
      if (!controllerName) {
        dataToSend['user_context'] = {"operation" : "RecommendAttribute"};
      } else if (controllerName == 'addServiceController') {
        dataToSend['user_context'] = {
          "operation" : "AddService",
          "operation_details" : (this.get('content.services')|| []).filterProperty('isSelected').filterProperty('isInstalled', false).mapProperty('serviceName').join(',')
        };
      } else if (controllerName == 'installerController'){
        dataToSend['user_context'] = {"operation" : "ClusterCreate"};
      }
    }
  },

  /**
   * Defines if there is any changes made by user.
   * Check all properties except recommended properties from popup
   *
   * @returns {boolean}
   */
  isConfigHasInitialState: function() {
    return Boolean(this.get('selectedConfigGroup.isDefault') && !Em.isNone(this.get('recommendationsConfigs'))
      && !this.get('stepConfigs').filter(function(stepConfig) {
      return stepConfig.get('changedConfigProperties').filter(function(c) {
        var changedConfigIds = this.get('changedProperties').map(function(changed) {
          return App.config.configId(changed.propertyName, changed.propertyFileName);
        });
        return !changedConfigIds.contains(App.config.configId(c.get('name'), c.get('filename')));
      }, this).length;
    }, this).length);
  },

  /**
   * generates JSON with config group info to send it for recommendations
   * @param configs
   * @param configGroup
   * @returns {{configurations: Object[], hosts: string[]}}
   */
  buildConfigGroupJSON: function(configs, configGroup) {
    Em.assert('configGroup can\'t be null', configGroup);
    var hosts = configGroup.get('hosts');
    var configurations = {};
    var overrides = configs.forEach(function(cp) {
      var override = cp.get('overrides') && cp.get('overrides').findProperty('group.name', configGroup.get('name'));
      if (override) {
        var tag = App.config.getConfigTagFromFileName(cp.get('filename'));
        if (!configurations[tag]) {
          configurations[tag] = { properties: {} };
        }
        configurations[tag].properties[cp.get('name')] = override.get('value');
      }
    });
    return {
      configurations: [configurations],
      hosts: hosts
    }
  },

  /**
   * shows popup with results for recommended value
   * if case properties that was changes belongs to not default group
   * user should pick to what config group from dependent service dependent properties will be saved
   * @param data
   * @param opt
   * @param params
   * @method dependenciesSuccess
   */
  loadRecommendationsSuccess: function (data, opt, params) {
    this._saveRecommendedValues(data, params.dataToSend.changed_configurations);
    if (this.isConfigHasInitialState()) {
      /** clearing all recommendations info **/
      this.undoRedoRecommended(this.get('recommendations'), false);
      this.clearAllRecommendations();

      /**
       * resetting recommendations to initial state
       * this case can be present when installed services depends on new added service on ADW
       **/
      this.undoRedoRecommended(this.get('initialRecommendations'), true);
      this.get('initialRecommendations').forEach(function (r) {
        this.get('recommendations').pushObject(r);
      }, this);
    }
    this.set("recommendationsConfigs", Em.get(data, "resources.0.recommendations.blueprint.configurations"));
  },

  loadRecommendationsError: Em.K,

  changedDependentGroup: function () {
    var dependentServices = this.get('selectedService.dependentServiceNames');
    var isInstallWizard = this.get('content.controllerName') === 'installerController';
    var installedServices = App.Service.find().mapProperty('serviceName');
    var services = this.get('stepConfigs').filter(function (stepConfig) {
      if (dependentServices.contains(stepConfig.get('serviceName'))) {
        return isInstallWizard ? true : installedServices.contains(stepConfig.get('serviceName'));
      }
      return false;
    }, this);
    App.showSelectGroupsPopup(this.get('selectedService.serviceName'),
        this.get('selectedService.configGroups').findProperty('name', this.get('selectedConfigGroup.name')),
        services, this.get('recommendations'));
  },

  /**
   * saves values from response for dependent config properties to <code>recommendations<code>
   * @param data
   * @param [changedConfigs=null]
   * @method saveRecommendedValues
   * @private
   */
  _saveRecommendedValues: function(data, changedConfigs) {
    Em.assert('invalid data - `data.resources[0].recommendations.blueprint.configurations` not defined ', data && data.resources[0] && Em.get(data.resources[0], 'recommendations.blueprint.configurations'));
    var recommendations = data.resources[0].recommendations;
    if (recommendations['config-groups'] && this.get('selectedConfigGroup') && !this.get('selectedConfigGroup.isDefault')) {
      this.saveConfigGroupsRecommendations(recommendations, changedConfigs);
    } else {
      var configObject = recommendations.blueprint.configurations;
      this.get('stepConfigs').forEach(function(stepConfig) {
        this.updateConfigsByRecommendations(configObject, stepConfig.get('configs'), changedConfigs);
      }, this);
      this.addByRecommendations(configObject, changedConfigs);
    }
    this.cleanUpRecommendations();
  },

  /**
   *
   * @param {object} recommendations
   * @param {?Array} changedConfigs
   */
  saveConfigGroupsRecommendations: function(recommendations, changedConfigs) {
    var configForGroup = recommendations['config-groups'][0];
    this.get('stepConfigs').forEach(function(stepConfig) {
      var configGroup = this.getGroupForService(stepConfig.get('serviceName'));
      if (configGroup && this.isConfigGroupAffected(configForGroup.hosts, configGroup.get('hosts'))) {
        this.updateOverridesByRecommendations(configForGroup.configurations, stepConfig.get('configs'), changedConfigs, configGroup);
        this.updateOverridesByRecommendations(configForGroup.dependent_configurations, stepConfig.get('configs'), changedConfigs, configGroup);
        this.toggleProperty('forceUpdateBoundaries');
      }
    }, this);
  },


  /**
   * determine whether hosts of group affected by config modifications
   * @param {Array} affectedHosts
   * @param {Array} groupHosts
   * @returns {boolean}
   */
  isConfigGroupAffected: function(affectedHosts, groupHosts) {
    return _.intersection(affectedHosts, groupHosts).length > 0;
  },

  /**
   * method to show popup with dependent configs
   * @method showChangedDependentConfigs
   */
  showChangedDependentConfigs: function(event, callback, secondary) {
    var self = this;
    var recommendations = event ? this.get('changedProperties') : this.get('recommendations'),
      recommendedChanges = recommendations.filterProperty('isEditable'),
      requiredChanges = this.filterRequiredChanges(recommendations);
    if (recommendedChanges.length > 0 || requiredChanges.length > 0) {
      App.showDependentConfigsPopup(recommendedChanges, requiredChanges, function() {
        self.onSaveRecommendedPopup(recommendedChanges.concat(requiredChanges));
        if (callback) callback();
      }, secondary);
    } else {
      if (callback) callback();
    }
  },

  /**
   *
   * @param {Array} recommendations
   * @returns {Array}
   */
  filterRequiredChanges: function(recommendations) {
    return recommendations.filter(function(recommendation) {
      if (recommendation.isEditable === false) {
        if (!this.get('selectedConfigGroup.isDefault')) {
          return App.ServiceConfigGroup.defaultGroupName !== recommendation.configGroup
        } else {
          return true;
        }
      }
    }, this);
  },

  /**
   * update configs when toggle checkbox on dependent configs popup
   */
  onSaveRecommendedPopup: function(recommendations) {
    var propertiesToUpdate = recommendations.filter(function(c) {
        return Em.get(c, 'saveRecommendedDefault') !== Em.get(c, 'saveRecommended');
      }),
      propertiesToUndo = propertiesToUpdate.filterProperty('saveRecommended', false),
      propertiesToRedo = propertiesToUpdate.filterProperty('saveRecommended', true);

    this.undoRedoRecommended(propertiesToUndo, false);
    this.undoRedoRecommended(propertiesToRedo, true);
    this.set('recommendationTimeStamp', (new Date).getTime());
  },

  /**
   * run through config properties list (form dependent popup)
   * and set value to default (undo) or recommended (redo)
   * this happens when toggle checkbox in popup
   * @param {Object[]} propertiesToUpdate
   * @param {boolean} redo
   */
  undoRedoRecommended: function(propertiesToUpdate, redo) {
    propertiesToUpdate.forEach(function(p) {
      var initial = redo ? Em.get(p, 'initialValue') : Em.get(p, 'recommendedValue');
      var recommended = redo ? Em.get(p, 'recommendedValue') : Em.get(p, 'initialValue');
      var stepConfig = this.get('stepConfigs').findProperty('serviceName', Em.get(p, 'serviceName'));
      var config = stepConfig.get('configs').find(function(scp) {
        return scp.get('name') === Em.get(p, 'propertyName') &&
               scp.get('filename') === App.config.getOriginalFileName(Em.get(p, 'propertyFileName'));
      });
      var selectedGroup = App.ServiceConfigGroup.find()
        .filterProperty('serviceName', Em.get(p, 'serviceName'))
        .findProperty('name', Em.get(p, 'configGroup'));

      if (selectedGroup.get('isDefault')) {
        this.setRecommendedForDefaultGroup(recommended, stepConfig, p, initial, config);
      } else {
        this.setRecommendedForGroup(recommended, selectedGroup, config, initial);
      }
    }, this);
  },

  /**
   *
   * @param {string} recommended
   * @param {Em.Object} selectedGroup
   * @param {object} config
   * @param {string}initial
   */
  setRecommendedForGroup: function(recommended, selectedGroup, config, initial) {
    if (Em.isNone(recommended)) {
      config.get('overrides').removeObject(config.getOverride(selectedGroup.get('name')));
    } else if (Em.isNone(initial)) {
      this._addConfigOverrideRecommendation(config, recommended, null, selectedGroup);
    } else {
      var override = config.getOverride(selectedGroup.get('name'));
      if (override) {
        override.set('value', recommended);
      }
    }
  },

  /**
   *
   * @param {string} recommended
   * @param {Em.Object} stepConfig
   * @param {object|Em.Object} prop
   * @param {string} initial
   * @param {object} config
   */
  setRecommendedForDefaultGroup: function(recommended, stepConfig, prop, initial, config) {
    var name = Em.get(prop, 'propertyName'),
        filename = Em.get(prop, 'propertyFileName');

    if (Em.isNone(recommended)) {
      stepConfig.get('configs').removeObject(config);
    } else if (Em.isNone(initial)) {
      var stackConfigProperty = App.configsCollection.getConfigByName(name, filename);
      stepConfig.get('configs').pushObject(this._createNewProperty(
        name,
        filename,
        Em.get(prop, 'serviceName'),
        recommended,
        stackConfigProperty? stackConfigProperty.propertyDependsOn : []));
    } else {
      Em.set(config, 'value', recommended);
    }
  },

  saveInitialRecommendations: function() {
    this.get('recommendations').forEach(function (r) {
      this.get('initialRecommendations').pushObject(r);
    }, this);
  },

  /**
   * disable saving recommended value for current config
   * @param config
   * @param {boolean} saveRecommended
   * @method removeCurrentFromDependentList
   */
  removeCurrentFromDependentList: function (config, saveRecommended) {
    var recommendation = this.getRecommendation(config.get('name'), config.get('filename'), config.get('group.name'));
    if (recommendation) this.saveRecommendation(recommendation, saveRecommended);
  }
});
