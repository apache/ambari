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

App.EnhancedConfigsMixin = Em.Mixin.create({

  /**
   * this value is used for observing
   * whether recommendations for dependent properties was received from server
   * @type {number}
   */
  recommendationTimeStamp: null,

  /**
   * flag is true when Ambari changes some of the dependent properties
   * @type {boolean}
   */
  hasChangedDependencies: function() {
    return App.get('supports.enhancedConfigs') && this.get('_dependentConfigValues.length') > 0;
  }.property('_dependentConfigValues.length'),

  /**
   * message fro alert box for dependent configs
   * @type {string}
   */
  dependenciesMessage: function() {
    var changedServices = this.get('changedProperties').mapProperty('serviceName').uniq();
    return Em.I18n.t('popup.dependent.configs.dependencies.info').format( this.get('changedProperties.length'), changedServices.length);
  }.property('changedProperties'),

  /**
   * values for dependent configs
   * @type {Object[]}
   * ex:
   * {
   *   saveRecommended: {boolean}, //by default is true (checkbox binding)
   *   saveRecommendedDefault: {boolean}, used for cancel operation to restore previous state
   *   fileName: {string}, //file name without '.xml'
   *   propertyName: {string},
   *   configGroup: {string},
   *   value: {string},
   *   serviceName: {string},
   *   recommendedValue: {string}
   * }
   * @private
   */
  _dependentConfigValues: [],

  /**
   * dependent properties that was changed by Ambari
   * @type {Object[]}
   */
  changedProperties: function() {
    return this.get('_dependentConfigValues').filterProperty('saveRecommended', true);
  }.property('_dependentConfigValues.@each.saveRecommended'),
  /**
   * dependent file names for configs
   * @type {string[]}
   */
  dependentFileNames: [],

  /**
   * dependent service names for configs
   * @type {string[]}
   */
  dependentServiceNames: [],

  /**
   * config groups for dependent services
   * @type {App.ConfigGroup[]}
   */
  dependentConfigGroups: [],

  /**
   * contains config group name that need to be saved
   * {
   *    serviceName: configGroupName
   * }
   * @type {Object}
   */
  groupsToSave: {},

  /******************************METHODS THAT WORKS WITH DEPENDENT CONFIGS *************************************/

  /**
   * clear values for dependent configs
   * @method clearDependentConfigs
   * @private
   */
  clearDependentConfigs: function() {
    this.set('groupsToSave', {});
    this.set('_dependentConfigValues', []);
  },

  onConfigGroupChangeForEnhanced: function() {
    this.clearDependentConfigs();
  }.observes('selectedConfigGroup'),


  /**
   * runs <code>setDependentServicesAndFileNames<code>
   * for stack properties for current service
   * @method loadDependentConfigs
   */
  setDependentServices: function(serviceName) {
    App.StackConfigProperty.find().forEach(function(stackProperty) {
      if (stackProperty.get('serviceName') === serviceName && stackProperty.get('propertyDependedBy.length') > 0) {
        this._setDependentServicesAndFileNames(stackProperty);
      }
    }, this);
  },

  /**
   * show popup to select config group for dependent services
   * to which dependent configs will ve saved
   * @method showSelectGroupsPopup
   */
  showSelectGroupsPopup: function(callback) {
    var servicesWithConfigGroups = [];
    this.get('dependentServiceNames').forEach(function(serviceName) {
      if (serviceName !== this.get('content.serviceName')) {
        if (!this.get('groupsToSave')[serviceName]) {
          var groups = this.get('dependentConfigGroups').filterProperty('service.serviceName', serviceName).mapProperty('name').uniq();
          servicesWithConfigGroups.push({
            serviceName: serviceName,
            configGroupNames: groups
          })
        }
      }
    }, this);
    if (servicesWithConfigGroups.length > 0) {
      App.showSelectGroupsPopup(servicesWithConfigGroups, this.get('groupsToSave'), callback);
    } else {
      callback();
    }
  },


  /**
   * sends request to get values for dependent configs
   * @param changedConfigs
   * @param initial
   * @param onComplete
   * @returns {$.ajax|null}
   */
  getRecommendationsForDependencies: function(changedConfigs, initial, onComplete) {
    if (Em.isArray(changedConfigs) && changedConfigs.length > 0 || initial) {
      var self = this;
      var recommendations = this.get('hostGroups');
      var configs = this._getConfigsByGroup(this.get('stepConfigs'));
      recommendations.blueprint.configurations = blueprintUtils.buildConfigsJSON(this.get('services'), configs);

      var dataToSend = {
        recommend: 'configurations',
        hosts: this.get('hostNames'),
        services: this.get('serviceNames'),
        recommendations: recommendations
      };
      if (App.get('supports.enhancedConfigs') && changedConfigs) {
        dataToSend.recommend = 'configuration-dependencies';
        dataToSend.changed_configurations = changedConfigs;
      }
      return App.ajax.send({
        name: 'config.recommendations',
        sender: this,
        data: {
          stackVersionUrl: App.get('stackVersionURL'),
          dataToSend: dataToSend,
          initial: initial
        },
        success: 'dependenciesSuccess',
        error: 'dependenciesError',
        callback: function() {
          self.onRecommendationsReceived();
          if (onComplete) {
            onComplete()
          }
        }
      });
    } else {
      return null;
    }
  },

  /**
   * complete callback on <code>getRecommendationsForDependencies<code>
   * @method onRecommendationsReceived
   */
  onRecommendationsReceived: function() {
    this.set('recommendationTimeStamp', (new Date).getTime());
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
  dependenciesSuccess: function (data, opt, params) {
    var self = this;
    if (!this.get('selectedConfigGroup.isDefault')) {
      self.showSelectGroupsPopup(function () {
        self._saveRecommendedValues(data, params.initial);
        self._updateDependentConfigs(self.get('selectedConfigGroup.isDefault'));
      });
    } else {
      self._saveRecommendedValues(data, params.initial);
      self._updateDependentConfigs(self.get('selectedConfigGroup.isDefault'));
    }
  },

  /**
   * method to show popup with dependent configs
   * @method showChangedDependentConfigs
   */
  showChangedDependentConfigs: function(event, callback) {
    var self = this;
    if (self.get('_dependentConfigValues.length') > 0) {
      App.showDependentConfigsPopup(this.get('_dependentConfigValues'), function() {
        self._updateDependentConfigs(self.get('selectedConfigGroup.isDefault'));
        if (callback) {
          callback();
        }
      });
    } else {
      if (callback) {
        callback();
      }
    }
  },

  /**
   *
   * @param jqXHR
   * @param ajaxOptions
   * @param error
   * @param opt
   */
  dependenciesError: function(jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.method, jqXHR.status);
  },


  /**
   * defines file names for configs and set them to <code>dependentFileNames<code> and
   * defines service names for configs and set them to <code>dependentServiceNames<code>
   * @param {App.StackConfigProperty} stackProperty
   * @private
   */
  _setDependentServicesAndFileNames: function(stackProperty) {
    if (stackProperty.get('propertyDependedBy.length') > 0) {
      stackProperty.get('propertyDependedBy').forEach(function(dependent) {
        var tag = App.config.getConfigTagFromFileName(dependent.type);
        /** setting dependent fileNames (without '.xml') **/
        if (!this.get('dependentFileNames').contains(tag)) {
          this.get('dependentFileNames').push(tag);
        }
        /** setting dependent serviceNames (without current serviceName) **/
        var dependentProperty = App.StackConfigProperty.find(dependent.name + "_" + tag);
        if (dependentProperty) {
          if (!this.get('dependentServiceNames').contains(dependentProperty.get('serviceName')) && dependentProperty.get('serviceName') !== this.get('content.serviceName')) {
            this.get('dependentServiceNames').push(dependentProperty.get('serviceName'));
          }
          this._setDependentServicesAndFileNames(dependentProperty);
        }
      }, this);
    }
  },

  /**
   * save values that are stored in <code>_dependentConfigValues<code>
   * to step configs
   * @param isDefaultConfigGroup
   * @private
   */
  _updateDependentConfigs: function(isDefaultConfigGroup) {
    var self = this;
    var dependentConfigs = this.get('_dependentConfigValues');
    this.get('stepConfigs').forEach(function(serviceConfigs) {
      var selectedGroup = self.getGroupForService(serviceConfigs.get('serviceName'));
      serviceConfigs.get('configs').forEach(function(cp) {
        var dependentConfig = dependentConfigs.filterProperty('propertyName', cp.get('name')).findProperty('fileName', App.config.getConfigTagFromFileName(cp.get('filename')));
        if (dependentConfig) {
          var valueToSave = dependentConfig.saveRecommended ? dependentConfig.recommendedValue : dependentConfig.value;
          if (isDefaultConfigGroup || selectedGroup.get('isDefault')) {
            cp.set('value', valueToSave);
          } else {
            var overridenConfig = cp.get('overrides') && cp.get('overrides').findProperty('group.name', selectedGroup.get('name'));
            if (overridenConfig) {
              overridenConfig.set('value', valueToSave);
            }
            if (dependentConfig.saveRecommended) {
              if (!overridenConfig) {
                self.addOverrideProperty(cp, selectedGroup, valueToSave, true);
              }
            } else {
              if (overridenConfig && overridenConfig.get('isNotSaved')) {
                cp.get('overrides').removeObject(overridenConfig);
              }
            }
          }
        }
      })
    });
  },

  /**
   * get config group object for current service
   * @param serviceName
   * @returns {*}
   */
  getGroupForService: function(serviceName) {
    if (this.get('content.serviceName') === serviceName) {
      return this.get('selectedConfigGroup')
    } else {
      if (this.get('selectedConfigGroup.isDefault')) {
        return this.get('dependentConfigGroups').filterProperty('service.serviceName', serviceName).findProperty('isDefault');
      } else {
        return this.get('dependentConfigGroups').findProperty('name', this.get('groupsToSave')[serviceName]);
      }
    }
  },

  /**
   * get array of config objects for current service depends on config group
   * for default group - it will be current stepConfigs
   * for not default group - overriden property in case there is such property in group
   * otherwise - property from default group
   * @param stepConfigs
   * @returns {Object[]}
   * @private
   */
  _getConfigsByGroup: function(stepConfigs) {
    if (this.get('selectedConfigGroup.isDefault') || this.get('controller.name') === 'wizardStep7Controller') {
      return stepConfigs;
    } else {
      var configsToSend = [];

      stepConfigs.forEach(function(serviceConfig) {
        var stepConfigToSend = [];
        var group = this.getGroupForService(serviceConfig.get('serviceName'));
        serviceConfig.get('configs').forEach(function(cp) {
          if (group && !group.get('isDefault') && cp.get('overrides')) {
            var conf = cp.get('overrides').findProperty('group.name', group.get('name'));
            stepConfigToSend.pushObject(conf ? conf : cp);
          } else {
            stepConfigToSend.pushObject(cp);
          }
        }, this);
        App.config.createServiceConfig(serviceConfig.get('serviceName'));
        var stepConfig =  App.config.createServiceConfig(serviceConfig.get('serviceName'));
        stepConfig.set('configs', stepConfigToSend);
        configsToSend.pushObject(stepConfig);
      }, this);

      return configsToSend;
    }
  },

  /**
   * saves values from response for dependent configs to <code>_dependentConfigValues<code>
   * @param data
   * @param updateOnlyBoundaries
   * @method saveRecommendedValues
   * @private
   */
  _saveRecommendedValues: function(data, updateOnlyBoundaries) {
    Em.assert('invalid data - `data.resources[0].recommendations.blueprint.configurations` not defined ', data && data.resources[0] && Em.get(data.resources[0], 'recommendations.blueprint.configurations'));
    var configs = data.resources[0].recommendations.blueprint.configurations;
    /** get all configs by config group **/
    var stepConfigsByGroup = this._getConfigsByGroup(this.get('stepConfigs'));
    for (var key in configs) {

      /**  defines main info for file name (service name, config group, config that belongs to filename) **/
      var service = App.config.getServiceByConfigType(key);
      var serviceName = service.get('serviceName');
      var stepConfig = stepConfigsByGroup.findProperty('serviceName', serviceName);
      var configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(key)) : [];

      var group = this.getGroupForService(serviceName);

      for (var propertyName in configs[key].properties) {
        /**  if property exists and has value **/
        var cp = configProperties.findProperty('name', propertyName);
        var value = cp && cp.get('value');

        if (!Em.isNone(value)) {
          if (!updateOnlyBoundaries) { //on first initial request we don't need to change values
            var dependentProperty = this.get('_dependentConfigValues').findProperty('propertyName', propertyName);
            if (dependentProperty) {
              if (value != configs[key].properties[propertyName]) {
                Em.set(dependentProperty, 'value', value);
                Em.set(dependentProperty, 'recommendedValue', configs[key].properties[propertyName]);
              }
            } else {
              if (value != configs[key].properties[propertyName]) {
                this.get('_dependentConfigValues').pushObject({
                  saveRecommended: true,
                  saveRecommendedDefault: true,
                  fileName: key,
                  propertyName: propertyName,
                  configGroup: group ? group.get('name') : service.get('displayName') + " Default",
                  value: value,
                  serviceName: serviceName,
                  recommendedValue: configs[key].properties[propertyName]
                });
              }
            }
          }
        }

        /**
         * saving new attribute values
         */
        if (configs[key].property_attributes && configs[key].property_attributes[propertyName]) {

          var stackProperty = App.StackConfigProperty.find(propertyName + '_' + key);
          if (stackProperty && stackProperty.get('valueAttributes')) {
            if (configs[key].property_attributes[propertyName].min) {
              stackProperty.set('valueAttributes.minimum', configs[key].property_attributes[propertyName].min);
            }
            if (configs[key].property_attributes[propertyName].max) {
              stackProperty.set('valueAttributes.maximum', configs[key].property_attributes[propertyName].max);
            }
            if (configs[key].property_attributes[propertyName].step) {
              stackProperty.set('valueAttributes.step', configs[key].property_attributes[propertyName].step);
            }
          }
        }
      }
    }
  }
});