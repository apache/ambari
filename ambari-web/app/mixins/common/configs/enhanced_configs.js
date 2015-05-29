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
    return App.get('isClusterSupportsEnhancedConfigs') && this.get('isControllerSupportsEnhancedConfigs') && this.get('_dependentConfigValues.length') > 0;
  }.property('_dependentConfigValues.length'),

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


  /**
   * message fro alert box for dependent configs
   * @type {string}
   */
  dependenciesMessage: function() {
    var changedServices = this.get('changedProperties').mapProperty('serviceName').uniq();
    var cfgLen = this.get('changedProperties.length') === 1 ? 'singular' : 'plural';
    var sLen = changedServices.length === 1 ? 'singular' : 'plural';
    return Em.I18n.t('popup.dependent.configs.dependencies.config.' + cfgLen).format(this.get('changedProperties.length'))
      + Em.I18n.t('popup.dependent.configs.dependencies.service.' + sLen).format(changedServices.length);
  }.property('changedProperties'),

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
   *    serviceName: {String} configGroupName
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
    this.setProperties({
      groupsToSave: {},
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
    this.set('_dependentConfigValues', cleanDependencies);
  },

  onConfigGroupChangeForEnhanced: function() {
    if (this.get('name') === 'mainServiceInfoConfigsController') {
      this.clearDependentConfigs();
    } else {
      this.set('groupsToSave', {});
    }
    this.get('dependentServiceNames').forEach(function(serviceName) {
      var defaultGroup = this.get('dependentConfigGroups').filterProperty('service.serviceName', serviceName).findProperty('isDefault');
      this.get('groupsToSave')[serviceName] = defaultGroup.get('name');
    }, this);
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
   * get config group object for current service
   * @param serviceName
   * @returns {App.ConfigGroup|null}
   */
  getGroupForService: function(serviceName) {
    if (this.get('stepConfigs.length') === 0) return null;
    if (this.get('name') === 'wizardStep7Controller') {
      return this.get('stepConfigs').findProperty('serviceName', serviceName).get('selectedConfigGroup');
    } else {
      if (this.get('content.serviceName') === serviceName) {
        return this.get('selectedConfigGroup')
      } else {
        if (this.get('selectedConfigGroup.isDefault')) {
          return this.get('dependentConfigGroups').filterProperty('service.serviceName', serviceName).findProperty('isDefault');
        } else {
          return this.get('dependentConfigGroups').findProperty('name', this.get('groupsToSave')[serviceName]);
        }
      }
    }
  },

  /**
   * show popup to select config group for dependent services
   * to which dependent configs will ve saved
   * @method showSelectGroupsPopup
   */
  showSelectGroupPopup: function(event) {
    var serviceName = event.context;
    if (serviceName !== this.get('content.serviceName')) {
      var groups = this.get('dependentConfigGroups').filterProperty('service.serviceName', serviceName).mapProperty('name').uniq();
      if (groups.length) {
        var configs = this.get('_dependentConfigValues').filterProperty('serviceName', serviceName);
        App.showSelectGroupsPopup(serviceName, groups, this.get('groupsToSave'), configs);
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
      return Em.get(dependentConfig, 'propertyName') == config.get('name') && Em.get(dependentConfig, 'fileName') == App.config.getConfigTagFromFileName(config.get('filename'))
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
      var recommendations = this.get('hostGroups');
      recommendations.blueprint.configurations = blueprintUtils.buildConfigsJSON(this.get('services'), this.get('stepConfigs'));
      delete recommendations.config_groups;

      var dataToSend = {
        recommend: 'configurations',
        hosts: this.get('hostNames'),
        services: this.get('serviceNames')
      };
      if (App.get('isClusterSupportsEnhancedConfigs')) {
        if (changedConfigs) {
          dataToSend.recommend = 'configuration-dependencies';
          dataToSend.changed_configurations = changedConfigs;
        }
        if (!this.get('selectedConfigGroup.isDefault') && this.get('selectedConfigGroup.hosts.length') > 0) {
          var configGroups = this.buildConfigGroupJSON(this.get('selectedService.configs'), this.get('selectedConfigGroup'));
          recommendations.config_groups = [configGroups];
        }
      }
      dataToSend.recommendations = recommendations;
      return App.ajax.send({
        name: 'config.recommendations',
        sender: this,
        data: {
          stackVersionUrl: App.get('stackVersionURL'),
          dataToSend: dataToSend,
          selectedConfigGroup: this.get('selectedConfigGroup.isDefault') ? null : this.get('selectedConfigGroup.name'),
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
      App.showDependentConfigsPopup(this.get('_dependentConfigValues'), function() {
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
   * @param jqXHR
   * @param ajaxOptions
   * @param error
   * @param opt
   */
  dependenciesError: function(jqXHR, ajaxOptions, error, opt) {
    this.set('recommendationTimeStamp', (new Date).getTime());
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
          if (dependentProperty.get('serviceName') && !this.get('dependentServiceNames').contains(dependentProperty.get('serviceName')) && dependentProperty.get('serviceName') !== this.get('content.serviceName')) {
            this.get('dependentServiceNames').push(dependentProperty.get('serviceName'));
          }
          this._setDependentServicesAndFileNames(dependentProperty);
        }
      }, this);
    }
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
    this.parseConfigsByTag(configObject, updateOnlyBoundaries, changedConfigs, selectedConfigGroup);
    if (!this.get('selectedConfigGroup.isDefault') && data.resources[0].recommendations['config-groups']) {
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
    var notDefaultGroup = !!selectedConfigGroup;
    var parentPropertiesNames = parentConfigs ? parentConfigs.mapProperty('name') : [];
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

          var dependentProperty = this.get('_dependentConfigValues').filterProperty('propertyName', propertyName).findProperty('fileName', key);
          var cp = configProperties.findProperty('name', propertyName);
          var override = (notDefaultGroup && group && cp && cp.get('overrides')) ? cp.get('overrides').findProperty('group.name', group.get('name')) : null;

          var value = override ? override.get('value') : cp && cp.get('value');

          if (this.useInitialValue(serviceName)) {
            initialValue = override ? override.get('initialValue') : cp && cp.get('initialValue');
          } else {
            initialValue = override ? override.get('savedValue') : cp && cp.get('savedValue');
          }


          initialValue = Em.isNone(initialValue) ? value : initialValue;
          var recommendedValue = configObject[key].properties[propertyName];

          var isNewProperty = (!notDefaultGroup && Em.isNone(cp)) || (notDefaultGroup && group && Em.isNone(override));

          var parsedInit = parseFloat(initialValue);
          var parsedRecommended = parseFloat(recommendedValue);
          if (!isNaN(parsedInit) && !isNaN(parsedRecommended)) {
            initialValue = parsedInit.toString();
            recommendedValue = parsedRecommended.toString();
          }
          if (!updateOnlyBoundaries && !parentPropertiesNames.contains(propertyName) && initialValue != recommendedValue) { //on first initial request we don't need to change values
            if (dependentProperty) {
              Em.set(dependentProperty, 'value', initialValue);
              Em.set(dependentProperty, 'recommendedValue', recommendedValue);
              Em.set(dependentProperty, 'toDelete', false);
              Em.set(dependentProperty, 'toAdd', isNewProperty);
              Em.set(dependentProperty, 'parentConfigs', dependentProperty.parentConfigs.concat(parentPropertiesNames).uniq());
            } else {
              this.get('_dependentConfigValues').pushObject({
                saveRecommended: true,
                saveRecommendedDefault: true,
                toDelete: false,
                isDeleted: false,
                toAdd: isNewProperty,
                fileName: key,
                propertyName: propertyName,
                configGroup: group ? group.get('name') : service.get('displayName') + " Default",
                value: initialValue,
                parentConfigs: parentPropertiesNames,
                serviceName: serviceName,
                allowChangeGroup: serviceName != this.get('content.serviceName') && !this.get('selectedConfigGroup.isDefault'),
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
              if (dependentProperty) {
                this.get('_dependentConfigValues').removeObject(dependentProperty);
              }
            }
          }
        }
      }
      this._saveRecommendedAttributes(configObject, parentPropertiesNames, updateOnlyBoundaries, selectedConfigGroup);
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
    Em.keys(configs).forEach(function (siteName) {
      var service = App.config.getServiceByConfigType(siteName);
      var serviceName = service.get('serviceName');
      var group = self.getGroupForService(serviceName);
      var stepConfig = self.get('stepConfigs').findProperty('serviceName', serviceName);
      var configProperties = stepConfig ? stepConfig.get('configs').filterProperty('filename', App.config.getOriginalFileName(siteName)) : [];
      var properties = configs[siteName].property_attributes || {};
      Em.keys(properties).forEach(function (propertyName) {
        var cp = configProperties.findProperty('name', propertyName);
        var stackProperty = App.StackConfigProperty.find().findProperty('id', propertyName + '_' + siteName);
        var attributes = properties[propertyName] || {};
        Em.keys(attributes).forEach(function (attributeName) {
          if (attributeName == 'delete') {
            if (!updateOnlyBoundaries) {
              var dependentProperty = self.get('_dependentConfigValues').filterProperty('propertyName', propertyName).findProperty('fileName', siteName);
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
                  configGroup: group ? group.get('name') : service.get('displayName') + " Default",
                  parentConfigs: parentPropertiesNames,
                  serviceName: service.get('serviceName'),
                  allowChangeGroup: service.get('serviceName') != self.get('content.serviceName') && !self.get('selectedConfigGroup.isDefault'),
                  serviceDisplayName: service.get('displayName'),
                  recommendedValue: null
                });
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
            }
          }
        });
      });
    });
  },

  /**
   * save values that are stored in <code>_dependentConfigValues<code>
   * to step configs
   */
  updateDependentConfigs: function() {
    var self = this;
    this.get('stepConfigs').forEach(function(serviceConfigs) {
      var selectedGroup = self.getGroupForService(serviceConfigs.get('serviceName'));

      self._updateRecommendedValues(serviceConfigs, selectedGroup);

      self._addRecommendedProperties(serviceConfigs, selectedGroup);

      self._removeUnRecommendedProperties(serviceConfigs, selectedGroup);
    });
    this.set('recommendationTimeStamp', (new Date).getTime());
  },


  /**
   * add configs that was recommended and wasn't present in stepConfigs
   * @param stepConfigs
   * @param selectedGroup
   * @private
   */
  _addRecommendedProperties: function(stepConfigs, selectedGroup) {
    var propertiesToAdd = this.get('_dependentConfigValues').filterProperty('toAdd').filterProperty('serviceName', stepConfigs.get('serviceName'));
    if (propertiesToAdd.length > 0) {
      propertiesToAdd.forEach(function(propertyToAdd) {
        if (!selectedGroup || selectedGroup.get('isDefault')) {
          if (Em.get(propertyToAdd, 'isDeleted')) {
            this.get('_dependentConfigValues').removeObject(propertyToAdd);
          }
          var addedProperty = App.ServiceConfigProperty.create({
            name: Em.get(propertyToAdd, 'propertyName'),
            displayName: Em.get(propertyToAdd, 'propertyName'),
            value: Em.get(propertyToAdd, 'recommendedValue'),
            recommendedValue: Em.get(propertyToAdd, 'recommendedValue'),
            savedValue: null,
            category: 'Advanced ' + Em.get(propertyToAdd, 'fileName'),
            serviceName: stepConfigs.get('serviceName'),
            filename: App.config.getOriginalFileName(Em.get(propertyToAdd, 'fileName')),
            isNotSaved: !Em.get(propertyToAdd, 'isDeleted'),
            isRequired: true
          });
          stepConfigs.get('configs').pushObject(addedProperty);
          addedProperty.validate();
        } else {
          var cp = stepConfigs.get('configs').filterProperty('name', Em.get(propertyToAdd, 'propertyName')).findProperty('filename', App.config.getOriginalFileName(Em.get(propertyToAdd, 'fileName')));
          if (Em.get(propertyToAdd, 'isDeleted')) {
            this.get('_dependentConfigValues').removeObject(propertyToAdd);
          }
          this.addOverrideProperty(cp, selectedGroup, Em.get(propertyToAdd, 'recommendedValue'), !Em.get(propertyToAdd, 'isDeleted'));
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
    var propertiesToDelete = this.get('_dependentConfigValues').filterProperty('toDelete').filterProperty('serviceName', stepConfigs.get('serviceName'));
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
      return !Em.get(p, 'toDelete') && !Em.get(p, 'toAdd') && Em.get(p, 'serviceName') == stepConfigs.get('serviceName');
    });
    if (propertiesToUpdate.length > 0) {
      stepConfigs.get('configs').forEach(function (cp) {
        var propertyToUpdate = propertiesToUpdate.filterProperty('propertyName', cp.get('name')).findProperty('fileName', App.config.getConfigTagFromFileName(cp.get('filename')));
        if (propertyToUpdate) {
          var valueToSave = propertyToUpdate.saveRecommended ? propertyToUpdate.recommendedValue : propertyToUpdate.value;
          if (!selectedGroup || selectedGroup.get('isDefault')) {
            if (propertyToUpdate.saveRecommended || cp.get('value') == propertyToUpdate.recommendedValue) {
              cp.set('value', valueToSave);
            }
            cp.set('recommendedValue', propertyToUpdate.recommendedValue);
          } else {
            if (stepConfigs.get('serviceName') !== this.get('content.serviceName')) {
              if (propertyToUpdate.saveRecommended || cp.get('value') == propertyToUpdate.recommendedValue) {
                cp.set('value', this.useInitialValue(stepConfigs.get('serviceName')) ? cp.get('initialValue') : cp.get('savedValue'));
              }
              cp.set('recommendedValue', propertyToUpdate.recommendedValue);
            }
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
  }

});
