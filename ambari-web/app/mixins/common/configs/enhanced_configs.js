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

App.EnhancedConfigsMixin = Em.Mixin.create({

  modifiedFileNames: [],

  /**
   * merge step configs from model
   * for default config group properties should be list
   * of changed properties
   * @param properties
   * @param currentVersionNumber
   */
  loadConfigsToModel: function(properties, currentVersionNumber) {
    var serviceName = this.get('content.serviceName');
    if (properties && properties.length) {
      properties.forEach(function(p) {
        var configFromModel = App.ConfigProperty.find(p.get('name') + '_' + App.config.getConfigTagFromFileName(p.get('filename')) + '_' + currentVersionNumber);
        if (configFromModel && configFromModel.get('name')) {
          configFromModel.setProperties({
            'value': p.get('value'),
            'isFinal': p.get('isFinal'),
            'defaultValue': p.get('defaultValue'),
            'defaultIsFinal': p.get('defaultIsFinal'),
            'isRequiredByAgent': p.get('isRequiredByAgent'),
            'isNotSaved': p.get('isNotSaved')
          });
        } else {
          App.store.load(App.ConfigProperty, {
            id: p.get('name') + '_' + App.config.getConfigTagFromFileName(p.get('filename')) + '_' + currentVersionNumber,
            name: p.get('name'),
            file_name: p.get('filename'),
            value: p.get('value'),
            is_final: p.get('isFinal'),
            default_value: p.get('defaultValue'),
            default_is_final: p.get('defaultIsFinal'),
            is_required_by_agent: p.get('isRequiredByAgent'),
            is_not_saved: p.get('isNotSaved'),
            is_required: false,
            config_version_id: serviceName + '_' + currentVersionNumber
          })
        }
      });
    }
  },

  /**
   * generates data and save configs for default group
   * @method saveEnhancedConfigs
   */
  saveEnhancedConfigs: function() {

    var fileNamesToSave = this.getFileNamesToSave(this.get('modifiedFileNames'));

    var configsToSave = this.getConfigsToSave(fileNamesToSave);

    var desired_configs = this.generateDesiredConfigsJSON(configsToSave, fileNamesToSave, this.get('serviceConfigNote'));

    this.doPUTClusterConfigurationSites(desired_configs);
  },

  /**
   * generates data and save configs for not default group
   * @param selectedConfigGroup
   * @method saveEnhancedConfigsAndGroup
   */
  saveEnhancedConfigsAndGroup: function(selectedConfigGroup) {
    //TODO update for dependent configs
    var serviceConfigVersion = App.ConfigVersion.find().findProperty('groupName', selectedConfigGroup.get('name'));

    var overridenConfigs = App.ConfigProperty.find().filter(function(cp) {
      return cp.get('configVersion.groupId') === selectedConfigGroup.get('id') || cp.get('isNotDefaultValue');
    });

    var hostNames = serviceConfigVersion.get('hosts').map(function(hostName) {
      return  {
        "host_name": hostName
      }
    });

    var fileNamesToSave = overridenConfigs.mapProperty('fileName').uniq();

    this.putConfigGroupChanges({
      ConfigGroup: {
        "id": selectedConfigGroup.get('id'),
        "cluster_name": App.get('clusterName'),
        "group_name": selectedConfigGroup.get('name'),
        "tag": selectedConfigGroup.get('service.id'),
        "description": selectedConfigGroup.get('description'),
        "hosts": hostNames,
        "service_config_version_note": this.get('serviceConfigNote'),
        "desired_configs": this.generateDesiredConfigsJSON(overridenConfigs, fileNamesToSave, null, true)
      }
    }, true);
  },

  /**
   * get file names that need to be saved
   * @param {Array} modifiedFileNames
   * @returns {Ember.Enumerable}
   */
  getFileNamesToSave: function(modifiedFileNames) {
    return App.ConfigProperty.find().filter(function(cp) {
      return cp.get('isNotDefaultValue') || cp.get('isNotSaved');
    }, this).mapProperty('fileName').concat(modifiedFileNames).uniq();
  },

  /**
   * get configs that need to be saved, for default group
   * @param fileNamesToSave
   * @returns {App.ConfigProperty[]}
   */
  getConfigsToSave: function(fileNamesToSave) {
    if (Em.isArray(fileNamesToSave) && fileNamesToSave.length) {
      return App.ConfigProperty.find().filter(function(cp) {
        return (fileNamesToSave.contains(cp.get('fileName')) && cp.get('isOriginalSCP')) || cp.get('isNotSaved');
      });
    } else {
      return Em.A([]);
    }
  },

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

  /**
   * overriden in controller
   */
  doPUTClusterConfigurationSites: Em.K,

  /**
   * overriden in controller
   */
  putConfigGroupChanges: Em.K
});