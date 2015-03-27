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
   * values for dependent configs
   * @type {Object[]}
   * ex:
   * {
   *   saveRecommended: {boolean}, //by default is true (checkbox binding)
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

  /***********************************METHODS THAT WORKS WITH MODEL ********************************************/

  /**
   * generates desired_config objects for default config group
   * @returns {Object}
   * @method getDependentConfigObject
   */
  getDependentConfigObject: function(serviceName) {

    var fileNamesToSave = this._getFileNamesToSave(serviceName);

    var configsToSave = this._getConfigsToSave(fileNamesToSave);

    return this.generateDesiredConfigsJSON(configsToSave, fileNamesToSave, this.get('serviceConfigNote'));
  },

  /**
   * generates data and save configs for not default groups only
   * that uses configs from model App.ConfigProperty
   * @param serviceName
   * @param configGroup
   * @method saveEnhancedConfigsAndGroup
   */
  saveModelConfigsWithGroup: function(serviceName, configGroup) {
    /**
     * for now we are saving configs from model only for dependent services
     * so excluding situation in current service is trying to be saved
     * this is temporary solution
     */
    if (this.get('content.serviceName') !== serviceName) {

      var configsToSave = App.ConfigProperty.find().filter(function(cp) {
        return cp.get('configVersion.groupName') == configGroup.get('name') || cp.get('isNotSaved');
      });
      if (configsToSave.length > 0) {
        var hostNames = configGroup.get('hosts').map(function(hostName) {
          return  {
            "host_name": hostName
          }
        });

        var fileNamesToSave = configsToSave.mapProperty('fileName').uniq();

        this.putConfigGroupChanges({
          ConfigGroup: {
            "id": configGroup.get('id'),
            "cluster_name": App.get('clusterName'),
            "group_name": configGroup.get('name'),
            "tag": configGroup.get('service.id'),
            "description": configGroup.get('description'),
            "hosts": hostNames,
            "service_config_version_note": this.get('serviceConfigNote'),
            "desired_configs": this.generateDesiredConfigsJSON(configsToSave, fileNamesToSave, null, true)
          }
        })
      }
    }
  },

  /**
   * save configs from model to default config group
   * @param serviceName
   */
  saveModelConfigs: function(serviceName) {
    var desired_configs = this.getDependentConfigObject(serviceName);
    if (desired_configs.length > 0) {
      var data = [JSON.stringify({
        Clusters: {
          desired_config: desired_configs
        }
      })];
      this.doPUTClusterConfigurationSites(data);
    }
  },

  /********************************METHODS THAT GENERATES JSON TO SAVE *****************************************/

  /**
   * generating common JSON object for desired configs
   * @param configsToSave
   * @param fileNamesToSave
   * @param serviceConfigNote
   * @param {boolean} [isNotDefaultGroup=false]
   * @returns {Array}
   */
  generateDesiredConfigsJSON: function(configsToSave, fileNamesToSave, serviceConfigNote, isNotDefaultGroup) {
    var desired_config = [];
    if (Em.isArray(configsToSave) && Em.isArray(fileNamesToSave) && fileNamesToSave.length && configsToSave.length) {
      serviceConfigNote = serviceConfigNote || "";
      var tagVersion = "version" + (new Date).getTime();

      fileNamesToSave.forEach(function(fName) {
        if (this.allowSaveSite(fName)) {
          var properties = configsToSave.filterProperty('fileName', fName);
          var type = App.config.getConfigTagFromFileName(fName);
          desired_config.push(this.createDesiredConfig(type, tagVersion, properties, serviceConfigNote, isNotDefaultGroup));
        }
      }, this);
    }
    return desired_config;
  },

  /**
   * for some file names we have a restriction
   * and can't save them, in this this method will return false
   * @param fName
   * @returns {boolean}
   */
  allowSaveSite: function(fName) {
    switch (fName) {
      case 'mapred-queue-acls.xml':
        return false;
      case 'core-site.xml':
        return ['HDFS', 'GLUSTERFS'].contains(this.get('content.serviceName'));
      default :
        return true;
    }
  },

  /**
   * generating common JSON object for desired config
   * @param {string} type - file name without '.xml'
   * @param {string} tagVersion - version + timestamp
   * @param {App.ConfigProperty[]} properties - array of properties from model
   * @param {string} serviceConfigNote
   * @param {boolean} [isNotDefaultGroup=false]
   * @returns {{type: string, tag: string, properties: {}, properties_attributes: {}|undefined, service_config_version_note: string|undefined}}
   */
  createDesiredConfig: function(type, tagVersion, properties, serviceConfigNote, isNotDefaultGroup) {
    Em.assert('type and tagVersion should be defined', type && tagVersion);
    var desired_config = {
      "type": type,
      "tag": tagVersion,
      "properties": {}
    };
    if (!isNotDefaultGroup) {
      desired_config.service_config_version_note = serviceConfigNote || "";
    }
    var attributes = { final: {} };
    if (Em.isArray(properties)) {
      properties.forEach(function(property) {

        if (property.get('isRequiredByAgent')) {
          desired_config.properties[property.get('name')] = this.formatValueBeforeSave(property);
          /**
           * add is final value
           */
          if (property.get('isFinal')) {
            attributes.final[property.get('name')] = "true";
          }
        }
      }, this);
    }

    if (Object.keys(attributes.final).length) {
      desired_config.properties_attributes = attributes;
    }
    return desired_config;
  },

  /**
   * format value before save performs some changing of values
   * according to the rules that includes heapsizeException trimming and some custom rules
   * @param {App.ConfigProperty} property
   * @returns {string}
   */
  formatValueBeforeSave: function(property) {
    var name = property.get('name');
    var value = property.get('value');
    //TODO check for core-site
    if (this.get('heapsizeRegExp').test(name) && !this.get('heapsizeException').contains(name) && !(value).endsWith("m")) {
      return value += "m";
    }
    if (typeof property.get('value') === "boolean") {
      return property.get('value').toString();
    }
    switch (name) {
      case 'storm.zookeeper.servers':
        if (Object.prototype.toString.call(value) === '[object Array]' ) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return App.config.trimProperty(property, true);
    }
  },

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
   * saves properties for dependent service to config group based on <code>groupsToSave<code>
   */
  saveDependentGroups: function() {
    if (App.get('supports.enhancedConfigs') && this.get('dependentServiceNames.length') && Object.keys(this.get('groupsToSave')).length > 0) {

      this.get('dependentServiceNames').forEach(function(serviceName) {
        if (this.get('groupsToSave')[serviceName]) {
          if (this.get('groupsToSave')[serviceName].contains('Default')) {
            this.saveModelConfigs(serviceName);
          } else {
            this.saveModelConfigsWithGroup(serviceName, this.get('dependentConfigGroups').findProperty('name', this.get('groupsToSave')[serviceName]));
          }
        }
      }, this);

    }
  },


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
   * get service for current config type
   * @param {String} configType - config fileName without xml
   * @return App.StackService
   */
  getServiceByConfigType: function(configType) {
    return App.StackService.find().find(function(s) {
      return Object.keys(s.get('configTypes')).contains(configType);
    });
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
   * @returns {$.ajax|null}
   */
  getRecommendationsForDependencies: function(changedConfigs) {
    if (Em.isArray(changedConfigs) && changedConfigs.length > 0) {
      var recommendations = this.get('hostGroups');
      var configs = this._getConfigsByGroup(this.get('stepConfigs'));
      recommendations.blueprint.configurations = blueprintUtils.buildConfigsJSON(this.get('services'), configs);

      var dataToSend = {
        recommend: 'configurations',
        hosts: this.get('hostNames'),
        services: this.get('serviceNames'),
        recommendations: recommendations
      };
      /** TODO uncomment when be will be ready
       if (App.get('supports.enhancedConfigs')) {
        dataToSend.recommend = 'configuration-dependencies';
        dataToSend.changed_configurations = changedConfigs;
      }
       **/
      return App.ajax.send({
        name: 'config.recommendations',
        sender: this,
        data: {
          stackVersionUrl: App.get('stackVersionURL'),
          dataToSend: dataToSend
        },
        success: 'dependenciesSuccess',
        error: 'dependenciesError'
      });
    } else {
      return null;
    }
  },

  /**
   * shows popup with results for recommended value
   * if case properties that was changes belongs to not default group
   * user should pick to what config group from dependent service dependent properties will be saved
   * @param data
   * @method dependenciesSuccess
   */
  dependenciesSuccess: function (data) {
    var self = this;
    if (!this.get('selectedConfigGroup.isDefault')) {
      self.showSelectGroupsPopup(function () {
        self._saveRecommendedValues(data);
        if (self.get('_dependentConfigValues.length') > 0) {
          App.showDependentConfigsPopup(self.get('_dependentConfigValues'), function () {
            self._saveDependentConfigs();
          });
        }
      });
    } else {
      self._saveRecommendedValues(data);
      if (self.get('_dependentConfigValues.length') > 0) {
        App.showDependentConfigsPopup(self.get('_dependentConfigValues'), function () {
          self._saveDependentConfigs();
        });
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
   * get file names that need to be saved
   * used for default config group
   * @param {String} serviceName
   * @returns {Ember.Enumerable}
   * @private
   */
  _getFileNamesToSave: function(serviceName) {
    return App.ConfigProperty.find().filter(function(cp) {
      return cp.get('isNotDefaultValue') && cp.get('stackConfigProperty.serviceName') === serviceName;
    }, this).mapProperty('fileName').uniq();
  },

  /**
   * get configs that need to be saved, for default group
   * @param fileNamesToSave
   * @returns {App.ConfigProperty[]}
   * @private
   */
  _getConfigsToSave: function(fileNamesToSave) {
    if (Em.isArray(fileNamesToSave) && fileNamesToSave.length) {
      return App.ConfigProperty.find().filter(function(cp) {
        return fileNamesToSave.contains(cp.get('fileName')) && cp.get('configVersion.isCurrent');
      });
    } else {
      return Em.A([]);
    }
  },

  /**
   * save values that are stored in <code>_dependentConfigValues<code>
   * for current service to step configs
   * for dependent services to model
   * @private
   */
  _saveDependentConfigs: function() {
    var self = this;
    this.get('_dependentConfigValues').forEach(function(dependentConfig) {
      if (Em.get(dependentConfig, 'saveRecommended')) { // if saveRecommended is false leave properties as is
        if (Em.get(dependentConfig, 'serviceName') === self.get('content.serviceName')) { //for current service save dependent properties to step configs
          self.get('stepConfigs').objectAt(0).get('configs').forEach(function(stepConfig) {
            if (stepConfig.get('filename') === App.config.getOriginalFileName(Em.get(dependentConfig, 'fileName'))
              && stepConfig.get('name') === Em.get(dependentConfig, 'propertyName')) {
              if (self.get('selectedConfigGroup.isDefault')) {
                stepConfig.set('value', Em.get(dependentConfig, 'recommendedValue'))
              } else {
                if (!stepConfig.get('overrides')) {
                  stepConfig.set('overrides', Em.A([]));
                }
                var overridenConfig = stepConfig.get('overrides').findProperty('isEditable');
                if (overridenConfig) {
                  overridenConfig.set('value', Em.get(dependentConfig, 'recommendedValue'));
                } else {
                  self.addOverrideProperty(stepConfig, self.get('selectedConfigGroup'), Em.get(dependentConfig, 'recommendedValue'));
                }
              }
            }
          })
        } else { //for not current service save dependent properties to model

          App.ConfigProperty.find().forEach(function(cp) {
            if (cp.get('name') === Em.get(dependentConfig, 'propertyName')
              && cp.get('fileName') === App.config.getOriginalFileName(Em.get(dependentConfig, 'fileName'))) {

              if (self.get('selectedConfigGroup.isDefault') || Em.get(dependentConfig, 'configGroup').contains('Default')) {
                if (cp.get('isOriginalSCP')) {
                  cp.set('value', Em.get(dependentConfig, 'recommendedValue'))
                }
              } else {
                if (cp.get('configVersion.groupName') === self.get('groupsToSave')[dependentConfig.serviceName]) {
                  cp.set('value', Em.get(dependentConfig, 'recommendedValue'));
                } else {
                  App.store.load(App.ConfigProperty, {
                    id: Em.get(dependentConfig, 'propertyName') + '_' + Em.get(dependentConfig, 'fileName') + '_',
                    name: Em.get(dependentConfig, 'propertyName'),
                    value: Em.get(dependentConfig, 'recommendedValue'),
                    file_name: App.config.getOriginalFileName(Em.get(dependentConfig, 'fileName')),
                    is_not_saved: true
                  })
                }
              }

            }
          });
        }
      }
    });
  },

  /**
   * get array of config objects for current service depends on config group
   * for default group - it will be current stepConfigs
   * for not default group - overriden property in case there is such property in group
   * otherwise - property from default group
   * @param stepConfigs
   * @returns {App.ServiceConfigProperty[]}
   * @private
   */
  _getConfigsByGroup: function(stepConfigs) {
    var configsToSend = [];
    if (this.get('selectedConfigGroup.isDefault')) {
      return stepConfigs;
    } else {
      stepConfigs.forEach(function(stepConfig) {
        if (stepConfig.get('overrides')) {
          var conf = stepConfig.get('overrides').findProperty('group.name', this.get('selectedConfigGroup.name'));
          if (conf) {
            configsToSend.pushObject(conf);
          } else {
            configsToSend.pushObject(stepConfig);
          }
        } else {
          configsToSend.pushObject(stepConfig);
        }
      }, this)
    }
    return configsToSend;
  },

  /**
   * saves values from response for dependent configs to <code>_dependentConfigValues<code>
   * @param data
   * @method saveRecommendedValues
   * @private
   */
  _saveRecommendedValues: function(data) {
    Em.assert('invalid data', data && data.resources[0] && Em.get(data.resources[0], 'recommendations.blueprint.configurations'));
    var configs = data.resources[0].recommendations.blueprint.configurations;
    for (var key in configs) {
      for (var propertyName in configs[key].properties) {
        var service = this.getServiceByConfigType(key);
        var value = this._getCurrentValue(service.get('serviceName'), key, propertyName, this.get('selectedConfigGroup'));
        if (!Em.isNone(value)) {
          var dependentProperty = this.get('_dependentConfigValues').findProperty('propertyName', propertyName);
          if (dependentProperty) {
            if (value != configs[key].properties[propertyName]) {
              Em.set(dependentProperty, 'value', value);
              Em.set(dependentProperty, 'recommendedValue', configs[key].properties[propertyName]);
            } else {
              this.get('_dependentConfigValues').removeObject(dependentProperty);
            }
          } else {
            var configGroup = this.get('selectedConfigGroup.isDefault') ?
              service.get('serviceName') + ' Default' : this.get('groupsToSave')[service.get('serviceName')] || this.get('selectedConfigGroup.name');
            if (value != configs[key].properties[propertyName]) {
              this.get('_dependentConfigValues').pushObject({
                saveRecommended: true,
                fileName: key,
                propertyName: propertyName,
                configGroup: configGroup,
                value: value,
                serviceName: service.get('serviceName'),
                recommendedValue: configs[key].properties[propertyName]
              });
            }
          }
        }
      }
    }
  },

  /**
   * get current value for property by serviceName, tag and ConfigGroup
   * @param serviceName
   * @param tag
   * @param propertyName
   * @param configGroup
   * @returns {null|Object}
   * @private
   */
  _getCurrentValue: function(serviceName, tag, propertyName, configGroup) {
    if (serviceName == this.get('content.serviceName')) {
      var stepConfig = this.get('stepConfigs').objectAt(0).get('configs').find(function(stepConfig) {
        return (stepConfig.get('filename') === App.config.getOriginalFileName(tag) && stepConfig.get('name') === propertyName);
      });
      if (stepConfig) {
        if (configGroup.get('isDefault') || Em.isNone(stepConfig.get('overrides'))) {
          return stepConfig.get('value');
        } else {
          var overridenConfig = stepConfig.get('overrides').findProperty('isEditable');
          if (overridenConfig) {
            return overridenConfig.get('value');
          } else {
            return stepConfig.get('value');
          }
        }
      }
    } else {
      var currentDefaultProperties = App.ConfigProperty.find().filter(function(cp) {
        return cp.get('configVersion.isCurrent') && cp.get('configVersion.isDefault');
      });
      if (!this.get('selectedConfigGroup.isDefault') && (!this.get('groupsToSave')[serviceName] || !this.get('groupsToSave')[serviceName].contains('Default'))) {
        var currentProperties = App.ConfigProperty.find().filter(function(cp) {
          return cp.get('configVersion.isCurrent') && cp.get('configVersion.groupName') === this.get('groupsToSave')[serviceName];
        }, this);
        var modelConfig = currentProperties.findProperty('name', propertyName);
        if (modelConfig) {
          return modelConfig.get('value');
        }
      }

      var modelDefaultConfig = currentDefaultProperties.findProperty('name', propertyName);
      if (modelDefaultConfig) {
        return modelDefaultConfig.get('value');
      }
    }
    return null;
  }
});