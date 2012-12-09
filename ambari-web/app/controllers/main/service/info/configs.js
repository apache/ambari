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
require('controllers/wizard/slave_component_groups_controller');

App.MainServiceInfoConfigsController = Em.Controller.extend({
  name: 'mainServiceInfoConfigsController',
  stepConfigs: [], //contains all field properties that are viewed in this service
  selectedService: null,
  serviceConfigs: require('data/service_configs'),

  isSubmitDisabled: function () {
    return !this.stepConfigs.everyProperty('errorCount', 0);
  }.property('stepConfigs.@each.errorCount'),

  slaveComponentHosts : function(){
    console.log('slaveComponentHosts');
    console.log(App.db.getSlaveComponentHosts());
    return App.db.getSlaveComponentHosts();
  }.property('content'),

  clearStep: function () {
    this.get('stepConfigs').clear();
  },

  serviceConfigProperties: function() {
    console.log('serviceConfigProperties');
    console.log(App.db.getServiceConfigProperties());
    return App.db.getServiceConfigProperties();
  }.property('content'),

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading configure for service");

    this.clearStep();
    this.renderServiceConfigs(this.serviceConfigs);

    var storedServices = this.get('serviceConfigProperties');
    if (storedServices) {
      var configs = new Ember.Set();

      // for all services`
      this.get('stepConfigs').forEach(function (_content) {
        //for all components
        _content.get('configs').forEach(function (_config) {

          var componentVal = storedServices.findProperty('name', _config.get('name'));
          //if we have config for specified component
          if(componentVal){

            //set it
            _config.set('value', componentVal.value)
          }

        }, this);
      }, this);
    }

    console.log('---------------------------------------');
    console.log(this.get('stepConfigs'));

  },

  /**
   * Render configs for active services
   * @param serviceConfigs
   */
  renderServiceConfigs: function (serviceConfigs) {
    serviceConfigs.forEach(function (_serviceConfig) {
      var serviceConfig = App.ServiceConfig.create({
        serviceName: _serviceConfig.serviceName,
        displayName: _serviceConfig.displayName,
        configCategories: _serviceConfig.configCategories,
        configs: []
      });

      if (this.get('content.serviceName') && this.get('content.serviceName').toUpperCase() === serviceConfig.serviceName) {
        this.loadComponentConfigs(_serviceConfig, serviceConfig);

        console.log('pushing ' + serviceConfig.serviceName);
        this.get('stepConfigs').pushObject(serviceConfig);

      } else {
        console.log('skipping ' + serviceConfig.serviceName);
      }
    }, this);

    this.set('selectedService', this.get('stepConfigs').objectAt(0));
  },

  /**
   * Load child components to service config object
   * @param _componentConfig
   * @param componentConfig
   */
  loadComponentConfigs: function (_componentConfig, componentConfig) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      serviceConfigProperty.serviceConfig = componentConfig;
      serviceConfigProperty.initialValue();
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
  },

  /**
   * Save config properties
   */
  saveServiceConfigProperties: function () {
    var serviceConfigProperties = [];
    this.get('stepConfigs').forEach(function (_content) {
      _content.get('configs').forEach(function (_configProperties) {
        var configProperty = {
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          service: _configProperties.get('serviceName')
        };
        serviceConfigProperties.push(configProperty);
      }, this);

    }, this);

    App.db.setServiceConfigProperties(serviceConfigProperties);
    alert('Data saved successfully');
//    this.set('content.serviceConfigProperties', serviceConfigProperties);
  }
});

App.MainServiceSlaveComponentGroupsController = App.SlaveComponentGroupsController.extend({
  name: 'mainServiceSlaveComponentGroupsController',
  contentBinding: 'App.router.mainServiceInfoConfigsController.slaveComponentHosts',
  serviceBinding: 'App.router.mainServiceInfoConfigsController.selectedService',

  selectedComponentName: function () {
    switch (App.router.get('mainServiceInfoConfigsController.selectedService.serviceName')) {
      case 'HDFS':
        return 'DATANODE';
      case 'MAPREDUCE':
        return 'TASKTRACKER';
      case 'HBASE':
        return 'HBASE_REGIONSERVER';
      default:
        return null;
    }
  }.property('App.router.mainServiceInfoConfigsController.selectedService')
});