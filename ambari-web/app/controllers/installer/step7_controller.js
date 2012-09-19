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

/**
 * Controller for Step 7 of the Installer Wizard.
 * By Step 7, we have the following information stored in App.db (localStorage) and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */
App.InstallerStep7Controller = Em.ArrayController.extend({

  name: 'installerStep7Controller',

  content: [],

  selectedService: null,

  slaveHostToGroup: null,

  isSubmitDisabled: function() {
    return !this.everyProperty('errorCount', 0);
  }.property('@each.errorCount'),

  // TODO: set attributes from localStorage in router
  // var selectedServiceNames = App.db.getSelectedServiceNames();
  selectedServiceNames: [ 'HDFS', 'MAPREDUCE', 'GANGLIA', 'NAGIOS', 'HBASE', 'PIG', 'SQOOP', 'OOZIE', 'HIVE', 'ZOOKEEPER'],
  masterComponentHosts: '',
  slaveComponentHosts: '',

  doInit: true,

  loadConfigs: function() {

    var selectedServiceNamesInDB = App.db.getSelectedServiceNames();
    if (selectedServiceNamesInDB !== undefined) {
      this.set('selectedServiceNames', selectedServiceNamesInDB);
    }
    // TODO: check App.db to see if configs have been saved already
    if (this.doInit) {
      var serviceConfigs = require('data/service_configs');

      var self = this;

      this.set('content', []);

      serviceConfigs.forEach(function(_serviceConfig) {
        var serviceConfig = App.ServiceConfig.create({
          serviceName: _serviceConfig.serviceName,
          displayName: _serviceConfig.displayName,
          configCategories: _serviceConfig.configCategories,
          configs: []
        });

        if (self.selectedServiceNames.contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {
          _serviceConfig.configs.forEach(function(_serviceConfigProperty) {
            var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
            serviceConfigProperty.serviceConfig = serviceConfig;
            serviceConfig.configs.pushObject(serviceConfigProperty);
            serviceConfigProperty.validate();
          });

          console.log('pushing ' + serviceConfig.serviceName);
          self.content.pushObject(serviceConfig);
        } else {
          console.log('skipping ' + serviceConfig.serviceName);
        }
      });

      this.set('selectedService', this.objectAt(0));
      this.doInit = false;
    }
  },

  submit: function() {
    if (!this.get('isSubmitDisabled')) {
      // TODO:
      // save service configs in App.db (localStorage)
      App.get('router').transitionTo('step8');
    }
  },

  showMasterHosts: function(event) {
    var serviceConfig = event.context;
    App.ModalPopup.show({
      header: serviceConfig.category + ' Hosts',
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/installer/master_hosts_popup')
      })
    });
  },

  showSlaveHosts: function(event) {
    var serviceConfig = event.context;
    App.ModalPopup.show({
      header: serviceConfig.category + ' Hosts',
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/installer/slave_hosts_popup')
      })
    });
  }

});

App.SlaveComponentGroupsController = Ember.ArrayController.extend({

  name: 'slaveComponentGroupsController',

  // TODO: Set up binding to use actual data
  //contentBinding: 'App.router.installerStep7Controller.slaveComponentHosts',
  content: require('data/mock/slave_component_hosts'),

  selectedComponentName: 'DataNode',

  showAddSlaveComponentGroup: function (event) {
    var componentName = event.context;
    this.set('selectedComponentName', componentName);
    App.ModalPopup.show({
      header: componentName + ' Groups',
      bodyClass: Ember.View.extend({
        controllerBinding: 'App.router.slaveComponentGroupsController',
        templateName: require('templates/installer/slave_hosts_popup')
      }),
      onPrimary: function() {
      }
    });
  },

  showEditSlaveComponentGroups: function(event) {
    this.showAddSlaveComponentGroup(event);
  },

  hosts: function() {
    return this.filterProperty('componentName', this.get('selectedComponentName'))[0].hosts;
  }.property('@each.hosts'),

  groups: function() {
    return this.filterProperty('componentName', this.get('selectedComponentName'))[0].hosts.mapProperty('group').uniq();
  }.property('@each.hosts')

});
