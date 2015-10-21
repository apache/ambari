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
    is_final: 'recommended_is_final',
    recommended_is_final: 'recommended_is_final',
    supports_final: 'supports_final',
    widget: 'widget',
    /**** ui properties ***/
    display_type: 'display_type',
    category: 'category',
    index: 'index'
  },

  map: function (json) {
    console.time('App.stackConfigPropertiesMapper execution time');
    if (json && json.Versions) {
      //hack for cluster versions
      json = {items: [json]};
      var clusterConfigs = true;
    }
    if (json && json.items) {
      var configs = [];
      json.items.forEach(function(stackItem) {
        var configTypeInfo = clusterConfigs ? Em.get(stackItem, 'Versions.config_types') : Em.get(stackItem, 'StackServices.config_types');

        stackItem.configurations.forEach(function(config) {
          if (clusterConfigs) {
            config.StackConfigurations = config.StackLevelConfigurations;
          }
          var configType = App.config.getConfigTagFromFileName(config.StackConfigurations.type);
          config.id = App.config.configId(config.StackConfigurations.property_name, configType);
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
              var service = App.StackService.find(config.StackConfigurations.service_name);
              var dependentService = App.config.getServiceByConfigType(dep.StackConfigurationDependency.dependency_type);
              if (dependentService && service && dependentService.get('serviceName') != service.get('serviceName') && !service.get('dependentServiceNames').contains(dependentService.get('serviceName'))) {
                service.set('dependentServiceNames', service.get('dependentServiceNames').concat(dependentService.get('serviceName')));
              }
            });
          }
          if (Em.get(config, 'StackConfigurations.property_depends_on.length') > 0) {
            config.StackConfigurations.property_depends_on.forEach(function(dep) {
              var service = App.StackService.find(config.StackConfigurations.service_name);
              var dependentService = App.config.getServiceByConfigType(dep.type);
              if (dependentService && service && dependentService.get('serviceName') != service.get('serviceName') && !service.get('dependentServiceNames').contains(dependentService.get('serviceName'))) {
                service.set('dependentServiceNames', service.get('dependentServiceNames').concat(dependentService.get('serviceName')));
              }
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
    }
    console.timeEnd('App.stackConfigPropertiesMapper execution time');
  },

  /******************* METHODS TO MERGE STACK PROPERTIES WITH STORED ON UI *********************************/

  /**
   * find UI config with current name and fileName
   * if there is such property - adds some info to config object
   * @param {Object} config
   * @method mergeWithUI
   */
  mergeWithUI: function(config) {
    var c = config.StackConfigurations;
    var uiConfigProperty = this.getUIConfig(c.property_name, c.type);
    var advancedData = App.config.advancedConfigIdentityData(c);

    if (!c.property_display_name) {
      c.property_display_name = App.config.getPropertyIfExists('displayName', App.config.getDefaultDisplayName(c.property_name, c.type), advancedData, uiConfigProperty);
    }
    c.service_name = App.config.getPropertyIfExists('serviceName', c.service_name, advancedData, uiConfigProperty);

    config.category = App.config.getPropertyIfExists('category', App.config.getDefaultCategory(true, c.type), advancedData, uiConfigProperty);
    config.display_type = App.config.getPropertyIfExists('displayType', Em.get(c, 'property_value_attributes.type') || App.config.getDefaultDisplayType(c.property_name, c.type, c.property_value), advancedData, uiConfigProperty);
    config.index = App.config.getPropertyIfExists('index', null, advancedData, uiConfigProperty);
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
    return App.config.get('preDefinedSitePropertiesMap')[App.config.configId(propertyName, siteName)];
  }
});
