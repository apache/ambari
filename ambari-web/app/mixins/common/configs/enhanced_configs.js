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
  hasChangedDependencies: function() {
    return App.get('isClusterSupportsEnhancedConfigs') && this.get('isControllerSupportsEnhancedConfigs') && this.get('changedProperties.length') > 0;
  }.property('changedProperties.length'),

  /**
   * defines is block with changed dependent configs should be shown
   * rely on controller
   * @type {boolean}
   */
  isControllerSupportsEnhancedConfigs: function() {
    return ['wizardStep7Controller','mainServiceInfoConfigsController'].contains(this.get('name'));
  }.property('name'),

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
    var changedServices = this.get('changedProperties').filterProperty('saveRecommended').mapProperty('serviceName').uniq();
    var changedProperties = this.get('changedProperties').filterProperty('saveRecommended');
    var cfgLen = changedProperties.length === 1 ? 'singular' : 'plural';
    var sLen = changedServices.length === 1 ? 'singular' : 'plural';
    return Em.I18n.t('popup.dependent.configs.dependencies.config.' + cfgLen).format(changedProperties.length)
      + Em.I18n.t('popup.dependent.configs.dependencies.service.' + sLen).format(changedServices.length);
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
  showSelectGroupsPopup: function() {
    return !this.get('selectedConfigGroup.isDefault') && this.get('selectedService.dependentServiceNames.length');
  }.property('selectedConfigGroup.isDefault'),

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
    // Do not clear the configs that are in excluded list
    var excludedConfigs = ['hadoop.proxyuser', 'dfs.allow.truncate'];
    var cleanDependencies = this.get('_dependentConfigValues').reject(function(item) {
      for (var i = 0; i < excludedConfigs.length; i++) {
        if (Em.get(item, 'propertyName').contains(excludedConfigs[i])) return false;
      }
      if (installedServices.contains(Em.get(item, 'serviceName'))) {
        var stackProperty = App.StackConfigProperty.find(App.config.configId(item.propertyName, item.fileName));
        var parentConfigs = stackProperty && stackProperty.get('propertyDependsOn');
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
   * @param {App.ConfigGroup|null} [configGroup=null]
   * @returns {$.ajax|null}
   */
  getRecommendationsForDependencies: function(changedConfigs, initial, onComplete, configGroup) {
    if (Em.isArray(changedConfigs) && changedConfigs.length > 0 || initial) {
      if (!configGroup) {
        configGroup = this.get('selectedConfigGroup');
      }
      var recommendations = this.get('hostGroups');
      recommendations.blueprint.configurations = blueprintUtils.buildConfigsJSON(this.get('services'), this.get('stepConfigs'));
      delete recommendations.config_groups;

      var dataToSend = {
        recommend: 'configurations',
        hosts: this.get('hostNames'),
        services: this.get('serviceNames')
      };
      if (changedConfigs) {
        dataToSend.recommend = 'configuration-dependencies';
        dataToSend.changed_configurations = changedConfigs;
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
          selectedConfigGroup: configGroup.get('isDefault') ? null : configGroup.get('name'),
          initial: initial
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
    this._saveRecommendedValues(data, params.initial, params.dataToSend.changed_configurations, params.selectedConfigGroup);
    this.set("recommendationsConfigs", Em.get(data.resources[0] , "recommendations.blueprint.configurations"));
    if (!params.initial) {
      this.updateDependentConfigs();
    }
  },

  /**
   * method to show popup with dependent configs
   * @method showChangedDependentConfigs
   */
  showChangedDependentConfigs: function(event, callback, secondary) {
    var self = this;
    if (this.get('_dependentConfigValues.length') > 0) {
      App.showDependentConfigsPopup(this.get('changedProperties'), function() {
        self.updateDependentConfigs();
        if (callback) {
          callback();
        }
      }, secondary);
    } else {
      if (callback) {
        callback();
      }
    }
  },

  /**
   *
   */
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
    console.error("ERROR: Unable to determine recommendations for configs: ", jqXHR, ajaxOptions, error, opt);
  },

  /**
   * saves values from response for dependent config properties to <code>_dependentConfigValues<code>
   * @param data
   * @param [updateOnlyBoundaries=false]
   * @param [changedConfigs=null]
   * @method saveRecommendedValues
   * @private
   */
  _saveRecommendedValues: function(data, updateOnlyBoundaries, changedConfigs, selectedConfigGroup) {
    Em.assert('invalid data - `data.resources[0].recommendations.blueprint.configurations` not defined ', data && data.resources[0] && Em.get(data.resources[0], 'recommendations.blueprint.configurations'));
    var configObject = data.resources[0].recommendations.blueprint.configurations;
    if (!selectedConfigGroup) {
      this.parseConfigsByTag(configObject, updateOnlyBoundaries, changedConfigs, selectedConfigGroup);
    } else if (data.resources[0].recommendations['config-groups']){
      var configFroGroup = data.resources[0].recommendations['config-groups'][0];
      this.parseConfigsByTag(configFroGroup.configurations, updateOnlyBoundaries, changedConfigs, selectedConfigGroup);
      this.parseConfigsByTag(configFroGroup.dependent_configurations, updateOnlyBoundaries, changedConfigs, selectedConfigGroup);
    }
  },

  /**
   * saves values from response for dependent configs to <code>_dependentConfigValues<code>
   * @param configObject - JSON response from `recommendations` endpoint
   * @param updateOnlyBoundaries
   * @param selectedConfigGroup
   * @param {App.ServiceConfigProperty[]} parentConfigs - config properties for which recommendations were received
   * @method saveRecommendedValues
   * @private
   */
  parseConfigsByTag: function(configObject, updateOnlyBoundaries, parentConfigs, selectedConfigGroup) {
    var wizardController = this.get('wizardController');
    if (wizardController) {
      var fileNamesToUpdate = wizardController.getDBProperty('fileNamesToUpdate') || [];
      this.set('_fileNamesToUpdate', fileNamesToUpdate);
    }
    var notDefaultGroup = !!selectedConfigGroup;
    var parentPropertiesNames = parentConfigs ? parentConfigs.map(function(p) { return App.config.configId(Em.get(p, 'name'), Em.get(p, 'type'))}) : [];
    /** get all configs by config group **/
    for (var key in configObject) {

      /**  defines main info for file name (service name, config group, config that belongs to filename) **/
      var service = App.config.getServiceByConfigType(key);
      var serviceName = service.get('serviceName');
      var stepConfig = this.get('stepConfigs').findProperty('serviceName', serviceName);
      if (stepConfig) {
        var initialValue;
        var configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(key)) : [];

        var group = this.getGroupForService(serviceName);

        for (var propertyName in configObject[key].properties) {

          var dependentProperty = this.get('_dependentConfigValues').filterProperty('propertyName', propertyName).filterProperty('fileName', key).findProperty('configGroup', group && Em.get(group,'name'));
          var cp = configProperties.findProperty('name', propertyName);
          var override = (notDefaultGroup && group && cp && cp.get('overrides')) ? cp.get('overrides').findProperty('group.name', group.get('name')) : null;

          var value = override ? override.get('value') : cp && cp.get('value');

          if (this.useInitialValue(serviceName)) {
            initialValue = override ? override.get('initialValue') : cp && cp.get('initialValue');
          } else {
            initialValue = override ? override.get('savedValue') : cp && cp.get('savedValue');
          }

          var recommendedValue = configObject[key].properties[propertyName];

          var isNewProperty = (!notDefaultGroup && Em.isNone(cp)) || (notDefaultGroup && group && Em.isNone(override));

          initialValue = validator.isValidFloat(initialValue) ? parseFloat(initialValue).toString() : initialValue;
          recommendedValue = validator.isValidFloat(recommendedValue) ? parseFloat(recommendedValue).toString() : recommendedValue;

          var groupName = group && Em.get(group, 'name');
          var dependentProperty = this.get('_dependentConfigValues').find(function (dcv) {
            return dcv.propertyName === propertyName && dcv.fileName === key && dcv.configGroup === groupName;
          });

          if (!updateOnlyBoundaries && !parentPropertiesNames.contains(App.config.configId(propertyName, key)) && initialValue != recommendedValue) { //on first initial request we don't need to change values
            if (dependentProperty) {
              Em.set(dependentProperty, 'value', initialValue);
              Em.set(dependentProperty, 'recommendedValue', recommendedValue);
              Em.set(dependentProperty, 'toDelete', false); // handled in <code>saveRecommendedAttributes</code>
              Em.set(dependentProperty, 'toAdd', isNewProperty);
              Em.set(dependentProperty, 'parentConfigs', dependentProperty.parentConfigs.concat(parentPropertiesNames).uniq());
            } else {
              this.get('_dependentConfigValues').pushObject({
                saveRecommended: true,
                saveRecommendedDefault: true,
                toDelete: false,
                isDeleted: false, // handled in <code>saveRecommendedAttributes</code>
                toAdd: isNewProperty,
                fileName: key,
                propertyName: propertyName,
                configGroup: group ? group.get('name') : "",
                value: initialValue,
                parentConfigs: parentPropertiesNames,
                serviceName: serviceName,
                allowChangeGroup: !this.get('selectedService.isDefault') && service.get('serviceName') != stepConfig.get('serviceName') && stepConfig.get('configGroups.length') > 1,
                serviceDisplayName: service.get('displayName'),
                recommendedValue: recommendedValue
              });
            }
          }

          /**
           * saving recommended value to service config properties
           * this value can be used as marker on slider widget
           */
          if (notDefaultGroup) {
            override && override.set('recommendedValue', recommendedValue);
          } else {
            cp && cp.set('recommendedValue', recommendedValue);
          }
          if (!updateOnlyBoundaries) {
            /**
             * clear _dependentPropertyValues from
             * properties that wasn't changed while recommendations
             */

            if ((initialValue == recommendedValue) || (Em.isNone(initialValue) && Em.isNone(recommendedValue))) {
              /** if recommended value same as default we shouldn't show it in popup **/
              if (notDefaultGroup) {
                if (override) {
                  if (override.get('isNotSaved')) {
                    cp.get('overrides').removeObject(override);
                  } else {
                    override.set('value', initialValue);
                  }
                  if (dependentProperty) {
                    this.get('_dependentConfigValues').removeObject(dependentProperty);
                  }
                }
              } else {
                cp.set('value', initialValue);
                if (!this.useInitialValue(serviceName)) {
                  cp.set('savedValue', initialValue);
                }
                if (dependentProperty) {
                  this.get('_dependentConfigValues').removeObject(dependentProperty);
                }
              }
            }
          }
        }
      }
      this._saveRecommendedAttributes(configObject, parentPropertiesNames, updateOnlyBoundaries, selectedConfigGroup);
    }
    if (wizardController) {
      wizardController.setDBProperty('fileNamesToUpdate', this.get('_fileNamesToUpdate').uniq());
    }
  },

  /**
   * Save property attributes received from recommendations. These attributes are minimum, maximum,
   * increment_step. Attributes are stored in <code>App.StackConfigProperty</code> model.
   *
   * @param {Object[]} configs
   * @param parentPropertiesNames
   * @param updateOnlyBoundaries
   * @private
   */
  _saveRecommendedAttributes: function(configs, parentPropertiesNames, updateOnlyBoundaries, selectedConfigGroup) {
    var self = this;
    var wizardController = self.get('wizardController');
    var fileNamesToUpdate = wizardController ? this.get('_fileNamesToUpdate') : [];
    var stackConfigsMap = {};
    App.StackConfigProperty.find().forEach(function (c) {
      stackConfigsMap[c.get('id')] = c;
    });
    Em.keys(configs).forEach(function (siteName) {
      var service = App.config.getServiceByConfigType(siteName);
      var serviceName = service.get('serviceName');
      var group = self.getGroupForService(serviceName);
      var stepConfig = self.get('stepConfigs').findProperty('serviceName', serviceName);
      var configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(siteName)) : [];
      var properties = configs[siteName].property_attributes || {};
      Em.keys(properties).forEach(function (propertyName) {
        var cp = configProperties.findProperty('name', propertyName);
        var stackProperty = stackConfigsMap[App.config.configId(propertyName, siteName)];
        var attributes = properties[propertyName] || {};
        var fileName = App.config.getOriginalFileName(siteName);
        Em.keys(attributes).forEach(function (attributeName) {
          if (attributeName == 'delete' && cp) {
            if (!updateOnlyBoundaries) {
              var modifiedFileNames = self.get('modifiedFileNames');
              var dependentProperty = self.get('_dependentConfigValues').filterProperty('propertyName', propertyName).filterProperty('fileName', siteName).findProperty('configGroup', group && Em.get(group,'name'));
              if (dependentProperty) {
                Em.set(dependentProperty, 'toDelete', true);
                Em.set(dependentProperty, 'toAdd', false);
                Em.set(dependentProperty, 'recommendedValue', null);
              } else {
                self.get('_dependentConfigValues').pushObject({
                  saveRecommended: true,
                  saveRecommendedDefault: true,
                  propertyValue: cp && (self.useInitialValue(serviceName) ? cp.get('initialValue') : cp.get('savedValue')),
                  toDelete: true,
                  toAdd: false,
                  isDeleted: true,
                  fileName: siteName,
                  propertyName: propertyName,
                  configGroup: group ? group.get('name') : "",
                  parentConfigs: parentPropertiesNames,
                  serviceName: service.get('serviceName'),
                  allowChangeGroup: !self.get('selectedService.isDefault') && service.get('serviceName') != stepConfig.get('serviceName') && stepConfig.get('configGroups.length') > 1,
                  serviceDisplayName: service.get('displayName'),
                  recommendedValue: null
                });
              }
              if (modifiedFileNames && !modifiedFileNames.contains(fileName)) {
               modifiedFileNames.push(fileName);
              } else if (wizardController && App.StackService.find(service.get('serviceName')).get('isInstalled')) {
                if (!fileNamesToUpdate.contains(fileName)) {
                  fileNamesToUpdate.push(fileName);
                }
              }
            }
          } else if (stackProperty) {
            if (selectedConfigGroup) {
              if (!stackProperty.get('valueAttributes')[selectedConfigGroup]) {
                /** create not default group object for updating such values as min/max **/
                Em.set(stackProperty.get('valueAttributes'), selectedConfigGroup, {});
              }
              if (stackProperty.get('valueAttributes')[selectedConfigGroup][attributeName] != attributes[attributeName]) {
                Em.set(stackProperty.get('valueAttributes')[selectedConfigGroup], attributeName, attributes[attributeName]);
                self.toggleProperty('forceUpdateBoundaries');
              }
            } else {
              Em.set(stackProperty.get('valueAttributes'), attributeName, attributes[attributeName]);
              if (attributeName === 'visible') {
                var p = self.findConfigProperty(propertyName, fileName);
                if (p) {
                  p.set('isVisible', attributes[attributeName]);
                }
              }
            }
          }
        });
      });
    });
    this.set('_fileNamesToUpdate', fileNamesToUpdate);
  },

  /**
   * save values that are stored in <code>_dependentConfigValues<code>
   * to step configs
   */
  updateDependentConfigs: function() {
    var self = this;
    this.get('stepConfigs').forEach(function(serviceConfigs) {
      var selectedGroup = self.getGroupForService(serviceConfigs.get('serviceName'));
      if (selectedGroup) {
        self._updateRecommendedValues(serviceConfigs, selectedGroup);

        self._addRecommendedProperties(serviceConfigs, selectedGroup);

        self._removeUnRecommendedProperties(serviceConfigs, selectedGroup);
      }
    });
    this.set('recommendationTimeStamp', (new Date).getTime());
  },

  /**
   * Add and remove dependencies based on recommendations
   *
   * @param {String[]} [serviceNames=undefined] - list of services to apply changes
   */
  addRemoveDependentConfigs: function(serviceNames) {
    var self = this;
    this.get('stepConfigs').forEach(function(serviceConfigs) {
      if (serviceNames && !serviceNames.contains(serviceConfigs.get('serviceName'))) {
        return;
      }
      var selectedGroup = self.getGroupForService(serviceConfigs.get('serviceName'));
      if (selectedGroup) {
        self._addRecommendedProperties(serviceConfigs, selectedGroup);
        self._removeUnRecommendedProperties(serviceConfigs, selectedGroup);
      }
    });
  },


  /**
   * add configs that was recommended and wasn't present in stepConfigs
   * @param stepConfigs
   * @param selectedGroup
   * @private
   */
  _addRecommendedProperties: function(stepConfigs, selectedGroup) {
    var propertiesToAdd = this.get('_dependentConfigValues').filterProperty('toAdd').filterProperty('serviceName', stepConfigs.get('serviceName')).filterProperty('configGroup', selectedGroup.get('name'));
    if (propertiesToAdd.length > 0) {
      propertiesToAdd.forEach(function(propertyToAdd) {
        if (!selectedGroup || selectedGroup.get('isDefault')) {
          if (Em.get(propertyToAdd, 'isDeleted')) {
            this.get('_dependentConfigValues').removeObject(propertyToAdd);
          }
          var originalFileName = App.config.getOriginalFileName(Em.get(propertyToAdd, 'fileName'));
          var predefinedProperty = App.config.get('preDefinedSiteProperties')
                .filterProperty('filename', originalFileName)
                .findProperty('name', Em.get(propertyToAdd, 'propertyName'));
          var addedProperty = App.ServiceConfigProperty.create({
            name: Em.get(propertyToAdd, 'propertyName'),
            displayName: Em.get(propertyToAdd, 'propertyName'),
            value: Em.get(propertyToAdd, 'recommendedValue'),
            recommendedValue: Em.get(propertyToAdd, 'recommendedValue'),
            savedValue: null,
            category: 'Advanced ' + Em.get(propertyToAdd, 'fileName'),
            serviceName: stepConfigs.get('serviceName'),
            filename: originalFileName,
            isNotSaved: !Em.get(propertyToAdd, 'isDeleted'),
            isRequired:  predefinedProperty ? Em.getWithDefault(predefinedProperty, 'isRequired', true) : true
          });
          stepConfigs.get('configs').pushObject(addedProperty);
          addedProperty.validate();
        } else {
          var cp = stepConfigs.get('configs').filterProperty('name', Em.get(propertyToAdd, 'propertyName')).findProperty('filename', App.config.getOriginalFileName(Em.get(propertyToAdd, 'fileName')));
          if (Em.get(propertyToAdd, 'isDeleted')) {
            this.get('_dependentConfigValues').removeObject(propertyToAdd);
          }
          var overriddenProperty = cp.get('overrides') && cp.get('overrides').findProperty('group.name', selectedGroup.get('name'));
          if (overriddenProperty) {
            overriddenProperty.set('value', Em.get(propertyToAdd, 'recommendedValue'));
            overriddenProperty.set('recommendedValue', Em.get(propertyToAdd, 'recommendedValue'));
          } else {
            var overridePlainObject = {
              "value": Em.get(propertyToAdd, 'recommendedValue'),
              "recommendedValue": Em.get(propertyToAdd, 'recommendedValue'),
              "isNotSaved": !Em.get(propertyToAdd, 'isDeleted'),
              "isEditable": true
            };
            App.config.createOverride(cp, overridePlainObject, selectedGroup);
          }
        }
        Em.setProperties(propertyToAdd, {
          isDeleted: Em.get(propertyToAdd, 'isDeleted'),
          toAdd: false,
          toDelete: false
        });
      }, this);
    }
  },

  /**
   * remove configs that was recommended to delete from stepConfigs
   * @param stepConfigs
   * @param selectedGroup
   * @private
   */
  _removeUnRecommendedProperties: function(stepConfigs, selectedGroup) {
    var propertiesToDelete = this.get('_dependentConfigValues').filterProperty('toDelete').filterProperty('serviceName', stepConfigs.get('serviceName')).filterProperty('configGroup', selectedGroup.get('name'));
    if (propertiesToDelete.length > 0) {

      propertiesToDelete.forEach(function(propertyToDelete) {
        var cp = stepConfigs.get('configs').filterProperty('name', Em.get(propertyToDelete, 'propertyName')).findProperty('filename', App.config.getOriginalFileName(Em.get(propertyToDelete, 'fileName')));
        if (cp) {
          if (!selectedGroup || selectedGroup.get('isDefault')) {
            if (cp.get('isNotSaved')) {
              this.get('_dependentConfigValues').removeObject(propertyToDelete);
            }
            stepConfigs.get('configs').removeObject(cp);
            if (!cp.get('isNotSaved')) {
              Em.set(propertyToDelete, 'isDeleted', true);
            }
          } else {
            var overriddenConfig = cp.get('overrides') && cp.get('overrides').findProperty('group.name', selectedGroup.get('name'));
            if (overriddenConfig) {
              if (overriddenConfig.get('isNotSaved')) {
                this.get('_dependentConfigValues').removeObject(propertyToDelete);
              }
              cp.removeObject(overriddenConfig);
              if (!overriddenConfig.get('isNotSaved')) {
                Em.set(propertyToDelete, 'isDeleted', true);
              }
            }
          }
          Em.setProperties(propertyToDelete, {
            toAdd: false,
            toDelete: false
          });
        } else {
          this.get('_dependentConfigValues').removeObject(propertyToDelete);
        }
      }, this);
    }
  },

  /**
   * update config to their recommended values
   * @param stepConfigs
   * @param selectedGroup
   * @private
   */
  _updateRecommendedValues: function(stepConfigs, selectedGroup) {
    var propertiesToUpdate = this.get('_dependentConfigValues').filter(function(p) {
      return !Em.get(p, 'toDelete') && !Em.get(p, 'toAdd') && Em.get(p, 'serviceName') == stepConfigs.get('serviceName') && Em.get(p, 'configGroup') == selectedGroup.get('name');
    });
    if (propertiesToUpdate.length > 0) {
      stepConfigs.get('configs').forEach(function (cp) {
        var propertyToUpdate = propertiesToUpdate.filterProperty('propertyName', cp.get('name')).findProperty('fileName', App.config.getConfigTagFromFileName(cp.get('filename')));
        if (propertyToUpdate) {
          var valueToSave = propertyToUpdate.saveRecommended ? propertyToUpdate.recommendedValue : propertyToUpdate.value;
          if (!selectedGroup || selectedGroup.get('isDefault')) {
            if (propertyToUpdate.saveRecommended || Em.get(propertyToUpdate, 'forceUpdate')) {
              cp.set('value', valueToSave);
            }
            Em.set(propertyToUpdate, 'forceUpdate', false);
            cp.set('recommendedValue', propertyToUpdate.recommendedValue);
          } else {
            var overriddenConfig = cp.get('overrides') && cp.get('overrides').findProperty('group.name', selectedGroup.get('name'));
            if (overriddenConfig) {
              if (propertyToUpdate.saveRecommended || overriddenConfig.get('value') == propertyToUpdate.recommendedValue) {
                overriddenConfig.set('value', valueToSave);
              }
              overriddenConfig.set('recommendedValue', propertyToUpdate.recommendedValue);
            }
          }
        }
      }, this);
    }
  },

  /**
   * On first load on installer and add service <code>initialValue<code> of <code>serviceConfigProperty<code> object
   * that contains value from stack should be overriden by dynamic recommendation.
   * Do this only for not installed services as in this case <code>initialValue<code> is not used.
   * @param configObject
   */
  updateInitialValue: function(configObject) {
    for (var key in configObject) {
      /**  defines main info for file name (service name, config group, config that belongs to filename) **/
      var service = App.config.getServiceByConfigType(key);
      if (App.Service.find().filterProperty('serviceName', service.get('serviceName'))) {
        var stepConfig = this.get('stepConfigs').findProperty('serviceName', service.get('serviceName'));
        if (stepConfig) {
          var configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(key)) : [];

          for (var propertyName in configObject[key].properties) {
            var configProperty = configProperties.findProperty('name', propertyName);
            if (configProperty) {
              configProperty.set('initialValue', configObject[key].properties[propertyName]);
            }
          }
        }
      }
    }
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
