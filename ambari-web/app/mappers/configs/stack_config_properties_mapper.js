/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.stackConfigPropertiesMapper = App.QuickDataMapper.create({
  model: App.StackConfigProperty,
  config: {
    id: 'id',
    name: 'StackConfigurations.property_name',
    display_name: 'StackConfigurations.property_display_name',
    file_name: 'StackConfigurations.type',
    description: 'StackConfigurations.property_description',
    value: 'StackConfigurations.property_value',
    recommended_value: 'StackConfigurations.property_value',
    type: 'StackConfigurations.property_type',
    service_name: 'StackConfigurations.service_name',
    stack_name: 'StackConfigurations.stack_name',
    stack_version: 'StackConfigurations.stack_version',
    property_depended_by: 'StackConfigurations.property_depended_by',
    property_depends_on: 'StackConfigurations.property_depends_on',
    value_attributes: 'StackConfigurations.property_value_attributes',
    is_final: 'default_is_final',
    recommended_is_final: 'default_is_final',
    supports_final: 'supports_final',
    widget: 'widget',
    /**** ui properties ***/
    display_type: 'display_type',
    category: 'category'
  },

  map: function (json) {
    console.time('stackConfigMapper execution time');
    if (json && json.items) {
      var configs = [];
      json.items.forEach(function(stackItem) {
        var configTypeInfo = Em.get(stackItem, 'StackServices.config_types');

        stackItem.configurations.forEach(function(config) {
          var configType = App.config.getConfigTagFromFileName(config.StackConfigurations.type);
          config.id = config.StackConfigurations.property_name + '_' + configType;
          config.recommended_is_final = config.StackConfigurations.final === "true";
          config.supports_final = !!configTypeInfo[configType] && configTypeInfo[configType].supports.final === "true";
          // Map from /dependencies to property_depended_by
          config.StackConfigurations.property_depended_by = [];
          if (config.dependencies && config.dependencies.length > 0) {
            config.dependencies.forEach(function(dep) {
              config.StackConfigurations.property_depended_by.push({
                type : dep.StackConfigurationDependency.dependency_type,
                name : dep.StackConfigurationDependency.dependency_name
              });
            });
          }
          /**
           * merging stack info with that is stored on UI
           * for now is not used; uncomment in will be needed
           * this.mergeWithUI(config);
           */
          this.mergeWithUI(config);
          configs.push(this.parseIt(config, this.get('config')));
        }, this);
      }, this);
      App.store.loadMany(this.get('model'), configs);

      App.StackService.find().filterProperty('id').forEach(function(service) {
        this.setDependentServices(service);
      }, this);
    }
    console.timeEnd('stackConfigMapper execution time');
  },

  /******************* METHODS TO MERGE STACK PROPERTIES WITH STORED ON UI (NOT USED FOR NOW)*********************************/

  /**
   * find UI config with current name and fileName
   * if there is such property - adds some info to config object
   * @param {Object} config
   * @method mergeWithUI
   */
  mergeWithUI: function(config) {
    var uiConfigProperty = this.getUIConfig(config.StackConfigurations.property_name, config.StackConfigurations.type);
    var displayType = App.permit(App.config.advancedConfigIdentityData(config.StackConfigurations), 'displayType').displayType || 'string';
    if (!config.StackConfigurations.property_display_name) {
      config.StackConfigurations.property_display_name = uiConfigProperty && uiConfigProperty.displayName ? uiConfigProperty.displayName : config.StackConfigurations.property_name;
    }
    config.category = uiConfigProperty ? uiConfigProperty.category : 'Advanced ' + App.config.getConfigTagFromFileName(config.StackConfigurations.type);
    config.display_type = uiConfigProperty ? uiConfigProperty.displayType || displayType : displayType;
  },

  /**
   * returns config with such name and fileName if there is such on UI
   * otherwise returns null
   * @param propertyName
   * @param siteName
   * @returns {Object|null}
   * @method getUIConfig
   */
  getUIConfig: function(propertyName, siteName) {
    return App.config.get('preDefinedSiteProperties').filterProperty('filename', siteName).findProperty('name', propertyName);
  },

  /**
   * runs <code>setDependentServicesAndFileNames<code>
   * for stack properties for current service
   * @method loadDependentConfigs
   */
  setDependentServices: function(service) {
    App.StackConfigProperty.find().filterProperty('serviceName', service.get('serviceName')).forEach(function(stackProperty) {
      if (stackProperty.get('propertyDependedBy.length')) {
        this._setDependentServices(stackProperty, 'propertyDependedBy', service);
      }
      if (stackProperty.get('propertyDependsOn.length')) {
        this._setDependentServices(stackProperty, 'propertyDependsOn', service);
      }
    }, this);
  },
  /**
   * defines service names for configs and set them to <code>dependentServiceNames<code>
   * @param {App.StackConfigProperty} stackProperty
   * @param {String} [key='propertyDependedBy'] - attribute to check dependent configs
   * @param service
   * @private
   */
  _setDependentServices: function(stackProperty, key, service) {
    key = key || 'propertyDependedBy';
    if (stackProperty.get(key + '.length') > 0) {
      stackProperty.get(key).forEach(function(dependent) {
        var tag = App.config.getConfigTagFromFileName(dependent.type);
        /** setting dependent serviceNames (without current serviceName) **/
        var dependentProperty = App.StackConfigProperty.find(dependent.name + "_" + tag);
        if (dependentProperty) {
          if (dependentProperty.get('serviceName') && dependentProperty.get('serviceName') != service.get('serviceName') && !service.get('dependentServiceNames').contains(dependentProperty.get('serviceName'))) {
            service.set('dependentServiceNames', service.get('dependentServiceNames').concat([dependentProperty.get('serviceName')]));
          }
          this._setDependentServices(dependentProperty, key, service);
        }
      }, this);
    }
  }
});
