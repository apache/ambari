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
var validator = require('utils/validator');

App.EnhancedConfigsMixin = Em.Mixin.create({

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
   * flag is true when Ambari changes some of the dependent properties
   * @type {boolean}
   */
  hasChangedDependencies: Em.computed.and('App.isClusterSupportsEnhancedConfigs', 'isControllerSupportsEnhancedConfigs', 'changedProperties.length'),

  /**
   * defines is block with changed dependent configs should be shown
   * rely on controller
   * @type {boolean}
   */
  isControllerSupportsEnhancedConfigs: Em.computed.existsIn('name', ['wizardStep7Controller','mainServiceInfoConfigsController']),

  /**
   * defines if initialValue of config can be used on current controller
   * if not savedValue is used instead
   * @param {String} serviceName
   * @return {boolean}
   * @method useInitialValue
   */
  useInitialValue: function(serviceName) {
    return ['wizardStep7Controller'].contains(this.get('name')) && !App.Service.find().findProperty('serviceName', serviceName);
  },

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
   * values for dependent configs
   * @type {Object[]}
   * ex:
   * {
   *   saveRecommended: {boolean}, //by default is true (checkbox binding)
   *   saveRecommendedDefault: {boolean}, used for cancel operation to restore previous state
   *   toDelete: {boolean} [true], // defines if property should be deleted
   *   toAdd: {boolean} [false], // defines if property should be added
   *   isDeleted: {boolean} [true], // defines if property was deleted, but was present in initial configs
   *   fileName: {string}, //file name without '.xml'
   *   propertyName: {string},
   *   parentConfigs: {string[]} // name of the parent configs
   *   configGroup: {string},
   *   value: {string},
   *   serviceName: {string},
   *   allowChangeGroup: {boolean}, //used to disable group link for current service
   *   serviceDisplayName: {string},
   *   recommendedValue: {string}
   * }
   * @private
   */
  _dependentConfigValues: Em.A([]),

  /**
   * dependent properties that was changed by Ambari
   * @type {Object[]}
   */
  changedProperties: function() {
    return this.get('_dependentConfigValues').filter(function(dp) {
      return (this.get('selectedConfigGroup.isDefault') && Em.get(dp, 'configGroup').contains('Default'))
        || [this.get('selectedConfigGroup.name'), this.get('selectedConfigGroup.dependentConfigGroups') && this.get('selectedConfigGroup.dependentConfigGroups')[Em.get(dp, 'serviceName')]].contains(Em.get(dp, 'configGroup'));
    }, this);
  }.property('_dependentConfigValues.@each.saveRecommended', 'selectedConfigGroup'),

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
    if (this.get('selectedConfigGroup') && this.get('isControllerSupportsEnhancedConfigs') && !this.get('selectedConfigGroup.isDefault') && this.get('selectedService.dependentServiceNames.length')) {
      this.get('selectedService.dependentServiceNames').forEach(function (serviceName) {
        if (!this.get('selectedConfigGroup.dependentConfigGroups')[serviceName]) {
          var stepConfig = this.get('stepConfigs').findProperty('serviceName', serviceName);
          if (stepConfig) {
            stepConfig.get('configGroups').filterProperty('isDefault', false).forEach(function (configGroup) {
              this.get('selectedService.configGroups').filterProperty('isDefault', false).forEach(function (currentServiceGroup) {
                if (currentServiceGroup.get('dependentConfigGroups')[serviceName] != configGroup.get('name')) {
                  var dependentGroups = $.extend({},this.get('selectedConfigGroup.dependentConfigGroups'));
                  dependentGroups[serviceName] = configGroup.get('name');
                  this.set('selectedConfigGroup.dependentConfigGroups', dependentGroups);
                }
              }, this);
            }, this);
          }
        }
      }, this);
    }
  }.observes('selectedConfigGroup'),

  /******************************METHODS THAT WORKS WITH DEPENDENT CONFIGS *************************************/

  /**
   * clear values for dependent configs
   * @method clearDependentConfigs
   * @private
   */
  clearDependentConfigs: function() {
    this.setProperties({
      _dependentConfigValues: []
    });
  },

  /**
   * clear values for dependent configs for given services
   * @method clearDependentConfigs
   * @private
   */
  clearDependentConfigsByService: function(serviceNames) {
    var cleanDependencies = this.get('_dependentConfigValues').reject(function(c) {
      return serviceNames.contains(c.serviceName);
    }, this);
    this.get('stepConfigs').filter(function(s) {
      return serviceNames.contains(s.get('serviceName'));
    }).forEach(function(s) {
      s.get('configs').setEach('isNotSaved', false);
    });
    this.set('_dependentConfigValues', cleanDependencies);
  },

  /**
   * Remove configs from <code>_dependentConfigValues</code> which depends between installed services only.
   *
   * @param {String[]} installedServices
   * @param {App.ServiceConfig[]} stepConfigs
   */
  clearDependenciesForInstalledServices: function(installedServices, stepConfigs) {
    var allConfigs = stepConfigs.mapProperty('configs').filterProperty('length').reduce(function(p, c) {
      return p && p.concat(c);
    });
    var cleanDependencies = this.get('_dependentConfigValues').reject(function(item) {
      if (Em.get(item, 'propertyName').contains('hadoop.proxyuser')) return false;
      if (installedServices.contains(Em.get(item, 'serviceName'))) {
        var stackProperty = App.configsCollection.getConfigByName(item.propertyName, item.fileName);
        var parentConfigs = stackProperty && stackProperty.propertyDependsOn;
        if (!parentConfigs || !parentConfigs.length) {
          return true;
        }
        // check that all parent properties from installed service
        return !parentConfigs.reject(function(parentConfig) {
          var property = allConfigs.filterProperty('filename', App.config.getOriginalFileName(parentConfig.type))
                                   .findProperty('name', parentConfig.name);
          if (!property) {
            return false;
          }
          return installedServices.contains(Em.get(property, 'serviceName'));
        }).length;
      }
      return false;
    });
    this.set('_dependentConfigValues', cleanDependencies);
  },

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
          return groups.length ? groups.findProperty('isDefault', true) : null;
        } else {
          return groups.length ? groups.findProperty('name', this.get('selectedConfigGroup.dependentConfigGroups')[serviceName]) : null;
        }
      } else {
        return null;
      }
    }
  },

  /**
   * disable saving recommended value for current config
   * @param config
   * @param {boolean} saveRecommended
   * @method removeCurrentFromDependentList
   */
  removeCurrentFromDependentList: function (config, saveRecommended) {
    var current = this.get('_dependentConfigValues').find(function(dependentConfig) {
      return Em.get(dependentConfig, 'propertyName') == config.get('name') && Em.get(dependentConfig, 'fileName') == App.config.getConfigTagFromFileName(config.get('filename'));
    });
    if (current) {
      Em.setProperties(current, {
          'saveRecommended': !!saveRecommended,
          'saveRecommendedDefault': !!saveRecommended
        });
    }
  },

  /**
   * sends request to get values for dependent configs
   * @param {{type: string, name: string}[]} changedConfigs - list of changed configs to track recommendations
   * @param {Boolean} initial
   * @param {Function} onComplete
   * @returns {$.ajax|null}
   */
  getRecommendationsForDependencies: function(changedConfigs, initial, onComplete) {
    if (Em.isArray(changedConfigs) && changedConfigs.length > 0 || initial) {
      var configGroup = this.get('selectedConfigGroup');
      var recommendations = this.get('hostGroups');
      delete recommendations.config_groups;

      var dataToSend = {
        recommend: 'configurations',
        hosts: this.get('hostNames'),
        services: this.get('serviceNames')
      };
      var clearConfigsOnAddService = this.isConfigHasInitialState();
      if (clearConfigsOnAddService) {
        recommendations.blueprint.configurations = this.get('initialConfigValues');
      } else {
        recommendations.blueprint.configurations = blueprintUtils.buildConfigsJSON(this.get('services'), this.get('stepConfigs'));
        if (changedConfigs) {
          dataToSend.recommend = 'configuration-dependencies';
          dataToSend.changed_configurations = changedConfigs;
        }
      }
      if (!configGroup.get('isDefault') && configGroup.get('hosts.length') > 0) {
        var configGroups = this.buildConfigGroupJSON(this.get('selectedService.configs'), configGroup);
        recommendations.config_groups = [configGroups];
      }
      dataToSend.recommendations = recommendations;
      return App.ajax.send({
        name: 'config.recommendations',
        sender: this,
        data: {
          stackVersionUrl: App.get('stackVersionURL'),
          dataToSend: dataToSend,
          notDefaultGroup: configGroup && !configGroup.get('isDefault'),
          initial: initial,
          clearConfigsOnAddService: clearConfigsOnAddService
        },
        success: 'dependenciesSuccess',
        error: 'dependenciesError',
        callback: function() {
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
   * Defines if there is any changes made by user.
   * Check all properties except recommended properties from popup
   *
   * @returns {boolean}
   */
  isConfigHasInitialState: function() {
    return !this.get('stepConfigs').filter(function(stepConfig) {
      return stepConfig.get('changedConfigProperties').filter(function(c) {
        return !this.get('changedProperties').map(function(changed) {
          return App.config.configId(changed.propertyName, changed.fileName);
        }).contains(App.config.configId(c.get('name'), c.get('filename')));
      }, this).length;
    }, this).length;
  },


  /**
   * Set all config values to their default (initialValue)
   */
  clearConfigValues: function() {
    this.get('stepConfigs').forEach(function(stepConfig) {
      stepConfig.get('changedConfigProperties').forEach(function(c) {
        var recommendedProperty = this.get('_dependentConfigValues').find(function(d) {
          return App.config.configId(d.propertyName, d.fileName) == App.config.configId(c.get('name'), c.get('filename'));
        });
        if (recommendedProperty) {
          var initialValue = recommendedProperty.value;
          if (Em.isNone(initialValue)) {
            stepConfig.get('configs').removeObject(c);
          } else {
            c.set('value', initialValue);
            c.set('recommendedValue', initialValue);
          }
          this.get('_dependentConfigValues').removeObject(recommendedProperty);
        }
      }, this)
    }, this);
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
  dependenciesSuccess: function (data, opt, params) {
    this._saveRecommendedValues(data, params.initial, params.dataToSend.changed_configurations, params.notDefaultGroup);
    this.set("recommendationsConfigs", Em.get(data.resources[0] , "recommendations.blueprint.configurations"));
    if (params.clearConfigsOnAddService) {
      if (this.get('wizardController.name') == 'addServiceController') {
        this.clearDependenciesForInstalledServices(this.get('installedServiceNames'), this.get('stepConfigs'));
      }
      this.clearConfigValues();
    }
    this.set('recommendationTimeStamp', (new Date).getTime());
  },

  /**
   * method to show popup with dependent configs
   * @method showChangedDependentConfigs
   */
  showChangedDependentConfigs: function(event, callback, secondary) {
    if (this.get('_dependentConfigValues.length') > 0) {
      App.showDependentConfigsPopup(this.get('changedProperties'), this.onSaveRecommendedPopup.bind(this), secondary);
    } else {
      if (callback) {
        callback();
      }
    }
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
      var initial = redo ? Em.get(p, 'value') : Em.get(p, 'recommendedValue');
      var recommended = redo ? Em.get(p, 'recommendedValue') : Em.get(p, 'value');
      var stepConfig = this.get('stepConfigs').findProperty('serviceName', Em.get(p, 'serviceName'));
      var config = stepConfig.get('configs').find(function(scp) {
        return scp.get('name') == Em.get(p, 'propertyName') && scp.get('filename') == Em.get(p, 'fileName');
      });
      var selectedGroup = App.ServiceConfigGroup.find().filterProperty('serviceName', Em.get(p, 'serviceName')).findProperty('name', Em.get(p, 'configGroup'));
      if (Em.isNone(recommended)) {
        if (selectedGroup.get('isDefault')) {
          stepConfig.get('configs').removeObject(config);
        } else {
          config.get('overrides').removeObject(this._getOverride(config, selectedGroup));
        }
      } else if (Em.isNone(initial)) {
        this._addConfigByRecommendation(stepConfig, selectedGroup, Em.get(p, 'propertyName'), Em.get(p, 'fileName'), Em.get(p, 'serviceName'), recommended, initial, config);
      } else {
        Em.set(config, 'value', recommended);
      }
    }, this);
  },

  /**
   * update configs when toggle checkbox on dependent configs popup
   * @param propertiesToUndo
   * @param propertiesToRedo
   */
  onSaveRecommendedPopup: function(propertiesToUndo, propertiesToRedo) {
    this.undoRedoRecommended(propertiesToUndo, false);
    this.undoRedoRecommended(propertiesToRedo, true);
    this.set('recommendationTimeStamp', (new Date).getTime());
  },

  changedDependentGroup: function() {
    var dependentServices = this.get('stepConfigs').filter(function(stepConfig) {
      return this.get('selectedService.dependentServiceNames').contains(stepConfig.get('serviceName'));
    }, this);
    App.showSelectGroupsPopup(this.get('selectedService.serviceName'),
      this.get('selectedService.configGroups').findProperty('name', this.get('selectedConfigGroup.name')),
      dependentServices, this.get('_dependentConfigValues'))
  },

  /**
   *
   * @param jqXHR
   * @param ajaxOptions
   * @param error
   * @param opt
   */
  dependenciesError: function(jqXHR, ajaxOptions, error, opt) {
    this.set('recommendationTimeStamp', (new Date).getTime());
    // We do not want to show user dialogs of failed recommendations
  },

  /**
   * saves values from response for dependent config properties to <code>_dependentConfigValues<code>
   * @param data
   * @param [updateOnlyBoundaries=false]
   * @param [changedConfigs=null]
   * @param notDefaultGroup
   * @param updateInitial
   * @method saveRecommendedValues
   * @private
   */
  _saveRecommendedValues: function(data, updateOnlyBoundaries, changedConfigs, notDefaultGroup, updateInitial) {
    Em.assert('invalid data - `data.resources[0].recommendations.blueprint.configurations` not defined ', data && data.resources[0] && Em.get(data.resources[0], 'recommendations.blueprint.configurations'));
    var configObject = data.resources[0].recommendations.blueprint.configurations;
    if (!notDefaultGroup) {
      this.parseConfigsByTag(configObject, changedConfigs, updateInitial, updateOnlyBoundaries);
    } else if (data.resources[0].recommendations['config-groups']){
      var configFroGroup = data.resources[0].recommendations['config-groups'][0];
      this.parseConfigsByTag(configFroGroup.configurations, changedConfigs, updateInitial, updateOnlyBoundaries);
      this.parseConfigsByTag(configFroGroup.dependent_configurations, changedConfigs, updateInitial, updateOnlyBoundaries);
    }
    this._cleanUpPopupProperties();
  },

  /**
   * saves values from response for dependent configs to <code>_dependentConfigValues<code>
   * @param configObject - JSON response from `recommendations` endpoint
   * @param {App.ServiceConfigProperty[]} parentConfigs - config properties for which recommendations were received
   * @param updateInitial
   * @param updateOnlyBoundaries
   * @method saveRecommendedValues
   * @private
   */
  parseConfigsByTag: function(configObject, parentConfigs, updateInitial, updateOnlyBoundaries) {
    var parentPropertiesNames = parentConfigs ? parentConfigs.map(function(p) { return App.config.configId(Em.get(p, 'name'), Em.get(p, 'type'))}) : [];
    /** get all configs by config group **/
    for (var key in configObject) {

      /**  defines main info for file name (service name, config group, config that belongs to filename) **/
      var service = App.config.get('serviceByConfigTypeMap')[key];
      var serviceName = service.get('serviceName');
      var stepConfig = this.get('stepConfigs').findProperty('serviceName', serviceName);
      if (stepConfig) {
        var configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(key)) : [];

        var group = this.getGroupForService(serviceName);

        for (var propertyName in configObject[key].properties) {

          var cp = configProperties.findProperty('name', propertyName);

          var configPropertyObject = (!group || group.get('isDefault')) ? cp : this._getOverride(cp, group);

          var recommendedValue = this.getFormattedValue(configObject[key].properties[propertyName]);
          var popupProperty = this.getPopupProperty(propertyName, key, Em.get(group || {}, 'name'));
          var initialValue = this._getInitialValue(configObject, popupProperty, serviceName, recommendedValue, updateInitial);
          if (configPropertyObject) {
            this._updateConfigByRecommendation(configPropertyObject, recommendedValue, updateInitial, updateOnlyBoundaries);
          } else if (!updateOnlyBoundaries) {
            this._addConfigByRecommendation(stepConfig, group, propertyName, key, serviceName, recommendedValue, initialValue, cp);
          }
          if (!updateOnlyBoundaries) {
            this._updatePopup(popupProperty, propertyName, key, recommendedValue, Em.get(configPropertyObject || {}, 'initialValue'), service, Em.get(group || {},'name') || "Default", parentPropertiesNames);
          }
        }
      }
    }
    this.parseConfigAttributes(configObject, parentPropertiesNames, updateOnlyBoundaries);
  },

  installedServices: function () {
    return App.StackService.find().toArray().toMapByCallback('serviceName', function (item) {
      return Em.get(item, 'isInstalled');
    });
  }.property(),

  /**
   * Save property attributes received from recommendations. These attributes are minimum, maximum,
   * increment_step. Attributes are stored in <code>App.StackConfigProperty</code> model.
   *
   * @param {Object[]} configs
   * @param parentPropertiesNames
   * @param updateOnlyBoundaries
   * @private
   */
  parseConfigAttributes: function(configs, parentPropertiesNames, updateOnlyBoundaries) {
    var self = this;
    Em.keys(configs).forEach(function (siteName) {
      var fileName = App.config.getOriginalFileName(siteName),
        service = App.config.get('serviceByConfigTypeMap')[siteName];
      var serviceName = service && service.get('serviceName'),
        stepConfig = self.get('stepConfigs').findProperty('serviceName', serviceName);
          if (stepConfig) {
            var group = self.getGroupForService(serviceName),
                configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(siteName)) : [],
                properties = configs[siteName].property_attributes || {};
            Em.keys(properties).forEach(function (propertyName) {
              var cp = configProperties.findProperty('name', propertyName);
              var stackProperty = App.configsCollection.getConfigByName(propertyName, siteName);
              var configObject = (!group || group.get('isDefault')) ? cp : self._getOverride(cp, group);
              var configsCollection = !group || group.get('isDefault') ? stepConfig.get('configs') : Em.getWithDefault(cp, 'overrides', []);
              var dependentProperty = self.getPopupProperty(propertyName, fileName, Em.get(group || {},'name'));
              var attributes = properties[propertyName] || {};
              Em.keys(attributes).forEach(function (attributeName) {
                if (attributeName == 'delete' && configObject) {
                  if (!updateOnlyBoundaries) {
                    self._removeConfigByRecommendation(configObject, configsCollection);
                    self._updatePopup(dependentProperty, propertyName, siteName, null, Em.get(configObject, 'initialValue'), service, Em.get(group || {},'name') || "Default", parentPropertiesNames);
                  }
                } else if (stackProperty) {
                  var selectedConfigGroup = group && !group.get('isDefault') ? group.get('name') : null;
                  if (selectedConfigGroup) {
                    if (!stackProperty.valueAttributes[selectedConfigGroup]) {
                      /** create not default group object for updating such values as min/max **/
                      Em.set(stackProperty.valueAttributes, selectedConfigGroup, {});
                    }
                    if (stackProperty.valueAttributes[selectedConfigGroup][attributeName] != attributes[attributeName]) {
                      Em.set(stackProperty.valueAttributes[selectedConfigGroup], attributeName, attributes[attributeName]);
                      self.toggleProperty('forceUpdateBoundaries');
                    }
                  } else {
                    Em.set(stackProperty.valueAttributes, attributeName, attributes[attributeName]);
                  }
                }
              });
            });
      }
    });
  },

  /**
   * update config based on recommendations
   * @param config
   * @param recommendedValue
   * @param updateInitial
   * @param updateOnlyBoundaries
   * @private
   */
  _updateConfigByRecommendation: function(config, recommendedValue, updateInitial, updateOnlyBoundaries) {
    Em.assert('config should be defined', config);
    Em.set(config, 'recommendedValue', recommendedValue);
    if (!updateOnlyBoundaries) Em.set(config, 'value', recommendedValue);
    if (updateInitial && Em.isNone(Em.get(config, 'savedValue'))) Em.set(config, 'initialValue', recommendedValue);
  },

  /**
   * remove config based on recommendations
   * @param config
   * @param configsCollection
   * @private
   */
  _removeConfigByRecommendation: function(config, configsCollection) {
    Em.assert('config and configsCollection should be defined', config && configsCollection);
    configsCollection.removeObject(config);
    /**
     * need to update wizard info when removing configs for installed services;
     */
    var installedServices = this.get('installedServices'), wizardController = this.get('wizardController'),
        fileNamesToUpdate = wizardController ? wizardController.getDBProperty('fileNamesToUpdate') || [] : [],
        fileName = Em.get(config, 'filename'), serviceName = Em.get(config, 'serviceName');
    var modifiedFileNames = this.get('modifiedFileNames');
    if (modifiedFileNames && !modifiedFileNames.contains(fileName)) {
      modifiedFileNames.push(fileName);
    } else if (wizardController && installedServices[serviceName]) {
      if (!fileNamesToUpdate.contains(fileName)) {
        fileNamesToUpdate.push(fileName);
      }
    }
    if (wizardController) {
      wizardController.setDBProperty('fileNamesToUpdate', fileNamesToUpdate.uniq());
    }
  },

  /**
   * add config based on recommendations
   * @param stepConfigs
   * @param selectedGroup
   * @param name
   * @param fileName
   * @param serviceName
   * @param recommendedValue
   * @param initialValue
   * @param cp
   * @private
   */
  _addConfigByRecommendation: function(stepConfigs, selectedGroup, name, fileName, serviceName, recommendedValue, initialValue, cp) {
    fileName = App.config.getOriginalFileName(fileName);
    var coreObject = {
      "value": recommendedValue,
      "recommendedValue": recommendedValue,
      "initialValue": initialValue,
      "savedValue": !this.useInitialValue(serviceName) && !Em.isNone(initialValue) ? initialValue : null,
      "isEditable": true
    };
    if (!selectedGroup || selectedGroup.get('isDefault')) {
      var addedProperty = App.configsCollection.getConfigByName(name, fileName) || App.config.createDefaultConfig(name, serviceName, fileName, false, coreObject);
      var addedPropertyObject = App.ServiceConfigProperty.create(addedProperty);
      stepConfigs.get('configs').pushObject(addedPropertyObject);
      addedPropertyObject.validate();
    } else {
      if (cp) {
        var newOverride = App.config.createOverride(cp, coreObject, selectedGroup);
        selectedGroup.get('properties').pushObject(newOverride);
      } else {
        stepConfigs.get('configs').push(App.config.createCustomGroupConfig(name, fileName, recommendedValue, selectedGroup, true, true));
      }
    }
  },

  /**
   * @param configProperty
   * @param popupProperty
   * @param serviceName
   * @param recommendedValue
   * @param updateInitial
   * @returns {*}
   * @private
   */
  _getInitialValue: function(configProperty, popupProperty, serviceName, recommendedValue, updateInitial) {
    if (!this.useInitialValue(serviceName)) {
      return configProperty ? Em.get(configProperty, 'savedValue') : null;
    } else if (updateInitial) {
      return recommendedValue;
    } else {
      return popupProperty ? popupProperty.value : configProperty ? Em.get(configProperty, 'initialValue') : null;
    }
  },

  /**
   * format value for float values
   * @param value
   * @returns {*}
   */
  getFormattedValue: function(value) {
    return validator.isValidFloat(value) ? parseFloat(value).toString() : value;
  },

  /**
   * just get config override
   * @param cp
   * @param selectedGroup
   * @returns {*|Object}
   * @private
   */
  _getOverride: function(cp, selectedGroup) {
    return Em.get(cp, 'overrides.length') && Em.get(cp, 'overrides').findProperty('group.name', Em.get(selectedGroup, 'name'));
  },

  /**
   * get property form popup
   * @param name
   * @param fileName
   * @param groupName
   * @returns {Object}
   */
  getPopupProperty: function(name, fileName, groupName) {
    return this.get('_dependentConfigValues').find(function (dcv) {
      return dcv.propertyName === name && dcv.fileName === App.config.getOriginalFileName(fileName) && dcv.configGroup === (groupName || "Default");
    });
  },

  /**
   * add or update proeprty in popup
   * @param popupProperty
   * @param name
   * @param fileName
   * @param recommendedValue
   * @param initialValue
   * @param service
   * @param groupName
   * @param parentPropertiesNames
   * @private
   */
  _updatePopup: function(popupProperty, name, fileName, recommendedValue, initialValue, service, groupName, parentPropertiesNames) {
    if (popupProperty) {
      Em.set(popupProperty, 'recommendedValue', recommendedValue);
      Em.set(popupProperty, 'isDeleted', Em.isNone(recommendedValue));
    } else {
      var popupPropertyObject = {
        saveRecommended: true,
        saveRecommendedDefault: true,
        fileName: App.config.getOriginalFileName(fileName),
        propertyName: name,

        isDeleted: Em.isNone(recommendedValue),
        notDefined: Em.isNone(initialValue),

        configGroup: groupName,
        value: initialValue,
        parentConfigs: parentPropertiesNames,
        serviceName: service.get('serviceName'),
        allowChangeGroup: groupName!= "Default" && (service.get('serviceName') != this.get('selectedService.serviceName'))
          && (App.ServiceConfigGroup.find().filterProperty('serviceName', service.get('serviceName')).length > 1),
        serviceDisplayName: service.get('displayName'),
        recommendedValue: recommendedValue
      };
      this.get('_dependentConfigValues').pushObject(popupPropertyObject);
    }
  },

  /**
   * clean properties that have same current and recommended values
   * @private
   */
  _cleanUpPopupProperties: function() {
    var cleanDependentList = this.get('_dependentConfigValues').filter(function(d) {
      return !((Em.isNone(d.value) && Em.isNone(d.recommendedValue)) || d.value == d.recommendedValue);
    }, this);
    this.set('_dependentConfigValues', cleanDependentList);
  },

  /**
   * Helper method to get property from the <code>stepConfigs</code>
   *
   * @param {String} name - config property name
   * @param {String} fileName - config property filename
   * @return {App.ServiceConfigProperty|Boolean} - App.ServiceConfigProperty instance or <code>false</code> when property not found
   */
  findConfigProperty: function(name, fileName) {
    if (!name && !fileName) return false;
    if (this.get('stepConfigs') && this.get('stepConfigs.length')) {
      return this.get('stepConfigs').mapProperty('configs').filter(function(item) {
        return item.length;
      }).reduce(function(p, c) {
        if (p) {
          return p.concat(c);
        }
      }).filterProperty('filename', fileName).findProperty('name', name);
    }
    return false;
  }

});
