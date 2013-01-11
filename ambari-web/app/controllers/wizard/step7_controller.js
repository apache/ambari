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
 * By Step 7, we have the following information stored in App.db and set on this
 * controller by the router.
 *
 *   selectedServices: App.db.selectedServices (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */

App.WizardStep7Controller = Em.Controller.extend({

  name: 'wizardStep7Controller',

  stepConfigs: [], //contains all field properties that are viewed in this step

  selectedService: null,

  slaveHostToGroup: null,

  isSubmitDisabled: function () {
    return !this.stepConfigs.filterProperty('showConfig', true).everyProperty('errorCount', 0);
  }.property('stepConfigs.@each.errorCount'),

  selectedServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
  }.property('content.services').cacheable(),

  allInstalledServiceNames: function () {
    return this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName');
  }.property('content.services').cacheable(),

  masterComponentHosts: function () {
    return this.get('content.masterComponentHosts');
  }.property('content.masterComponentHosts'),

  slaveComponentHosts: function () {
    return this.get('content.slaveGroupProperties');
  }.property('content.slaveGroupProperties', 'content.slaveComponentHosts'),

  serviceConfigs: require('data/service_configs'),
  configMapping: require('data/config_mapping'),
  customConfigs: require('data/custom_configs'),
  customData: [],

  clearStep: function () {
    this.get('stepConfigs').clear();
  },

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    this.clearStep();
    var serviceConfigs = this.get('serviceConfigs');
    var advancedConfig = this.get('content.advancedServiceConfig') || [];
    this.loadAdvancedConfig(serviceConfigs, advancedConfig);
    this.loadCustomConfig();
    this.renderServiceConfigs(serviceConfigs);
    var storedServices = this.get('content.serviceConfigProperties');
    if (storedServices) {
      var configs = new Ember.Set();

      // for all services`
      this.get('stepConfigs').forEach(function (_content) {
        //for all components
        _content.get('configs').forEach(function (_config) {

          var componentVal = storedServices.findProperty('name', _config.get('name'));
          //if we have config for specified component
          if (componentVal) {
            //set it
            _config.set('value', componentVal.value)
          }

        }, this);
      }, this);

    }
  },

  /*
   Loads the advanced configs fetched from the server metadata libarary
   */

  loadAdvancedConfig: function (serviceConfigs, advancedConfig) {
    advancedConfig.forEach(function (_config) {
      if (_config) {
        var service = serviceConfigs.findProperty('serviceName', _config.serviceName);
        if (service) {
          if (this.get('configMapping').someProperty('name', _config.name)) {
          } else if (!(service.configs.someProperty('name', _config.name))) {
            _config.id = "site property";
            _config.category = 'Advanced';
            _config.displayName = _config.name;
            _config.defaultValue = _config.value;
            // make all advanced configs optional and populated by default
            /*
             if (/\${.*}/.test(_config.value) || (service.serviceName !== 'OOZIE' && service.serviceName !== 'HBASE')) {
             _config.isRequired = false;
             _config.value = '';
             } else if (/^\s+$/.test(_config.value)) {
             _config.isRequired = false;
             }
             */
            _config.isRequired = false;
            _config.isVisible = true;
            _config.displayType = 'advanced';
            service.configs.pushObject(_config);
          }
        }
      }
    }, this);
  },


  /**
   * Render a custom conf-site box for entering properties that will be written in *-site.xml files of the services
   */
  loadCustomConfig: function () {
    var serviceConfigs = this.get('serviceConfigs');
    this.get('customConfigs').forEach(function (_config) {
      var service = serviceConfigs.findProperty('serviceName', _config.serviceName);
      if (service) {
        if (!(service.configs.someProperty('name', _config.name))) {
          service.configs.pushObject(_config);
        }
      }
    }, this);
  },

  /**
   * Render configs for active services
   * @param serviceConfigs
   */
  renderServiceConfigs: function (serviceConfigs) {
    serviceConfigs.forEach(function (_serviceConfig) {

      var serviceConfig = App.ServiceConfig.create({
        filename: _serviceConfig.filename,
        serviceName: _serviceConfig.serviceName,
        displayName: _serviceConfig.displayName,
        configCategories: _serviceConfig.configCategories,
        showConfig: false,
        configs: []
      });

      if (this.get('allInstalledServiceNames').contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {

        this.loadComponentConfigs(_serviceConfig, serviceConfig);

        console.log('pushing ' + serviceConfig.serviceName, serviceConfig);

        if (this.get('selectedServiceNames').contains(serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {
          serviceConfig.showConfig = true;
        }

        this.get('stepConfigs').pushObject(serviceConfig);

      } else {
        console.log('skipping ' + serviceConfig.serviceName);
      }
    }, this);

    var miscConfigs = this.get('stepConfigs').findProperty('serviceName', 'MISC').configs;
    var showProxyGroup = this.get('selectedServiceNames').contains('HIVE') ||
      this.get('selectedServiceNames').contains('HCATALOG') ||
      this.get('selectedServiceNames').contains('OOZIE');
    miscConfigs.findProperty('name', 'proxyuser_group').set('isVisible', showProxyGroup);
    miscConfigs.findProperty('name', 'hbase_user').set('isVisible', this.get('selectedServiceNames').contains('HBASE'));
    miscConfigs.findProperty('name', 'mapred_user').set('isVisible', this.get('selectedServiceNames').contains('MAPREDUCE'));
    miscConfigs.findProperty('name', 'hive_user').set('isVisible', this.get('selectedServiceNames').contains('HIVE'));
    miscConfigs.findProperty('name', 'hcat_user').set('isVisible', this.get('selectedServiceNames').contains('HCATALOG'));
    miscConfigs.findProperty('name', 'webhcat_user').set('isVisible', this.get('selectedServiceNames').contains('WEBHCAT'));
    miscConfigs.findProperty('name', 'oozie_user').set('isVisible', this.get('selectedServiceNames').contains('OOZIE'));
    miscConfigs.findProperty('name', 'pig_user').set('isVisible', this.get('selectedServiceNames').contains('PIG'));
    miscConfigs.findProperty('name', 'sqoop_user').set('isVisible', this.get('selectedServiceNames').contains('SQOOP'));
    miscConfigs.findProperty('name', 'zk_user').set('isVisible', this.get('selectedServiceNames').contains('ZOOKEEPER'));

    this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig', true).objectAt(0));
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

  validateCustomConfig: function () {
    var flag = true;
    var serviceProperties = [];
    this.get('stepConfigs').forEach(function (_serviceContent) {
      var configProperties = _serviceContent.get('configs');
      if (configProperties.someProperty('id', 'conf-site')) {
        var serviceProperty = {};
        serviceProperty.serviceName = _serviceContent.get("serviceName");
        serviceProperty.siteProperties = [];
        var customSite = configProperties.findProperty('id', 'conf-site');
        var keyValue = customSite.value.split(/\n+/);
        if (keyValue) {
          keyValue.forEach(function (_keyValue) {
            console.log("The value of the keyValue is: " + _keyValue.trim());
            _keyValue = _keyValue.trim();
            var key = _keyValue.match(/(.+)=/);
            var value = _keyValue.match(/=(.*)/);
            if (key) {
              // Check that entered config is allowed to reconfigure
              if (configProperties.someProperty('name', key[1]) || this.get('configMapping').someProperty('name', key[1])) {
                var property = {
                  siteProperty: key[1],
                  displayNames: [],
                  displayMsg: null
                };
                if (this.get('configMapping').someProperty('name', key[1])) {
                  this.setPropertyDisplayNames(property.displayNames, this.get('configMapping').findProperty('name', key[1]).templateName, configProperties);
                }
                property.displayMsg = this.setDisplayMessage(property.siteProperty, property.displayNames);
                serviceProperty.siteProperties.push(property);
                flag = false;
              }
            }
          }, this);
        }
        serviceProperties.push(serviceProperty);
      }
    }, this);
    var result = {
      flag: flag,
      value: serviceProperties
    };
    return result;
  },

  /**
   * @param: An array of display names
   */
  setDisplayMessage: function (siteProperty, displayNames) {
    var displayMsg = null;
    if (displayNames && displayNames.length) {
      if (displayNames.length === 1) {
        displayMsg = siteProperty + ' as ' + displayNames[0];
      } else {
        var name = null;
        displayNames.forEach(function (_name, index) {
          if (index === 0) {
            name = _name;
          } else if (index === displayNames.length - 1) {
            name = name + ' and ' + _name;
          } else {
            name = name + ', ' + _name;
          }
        }, this);
        displayMsg = siteProperty + ' as ' + name;
      }
    } else {
      displayMsg = siteProperty;
    }
    return displayMsg;
  },

  /**
   * Set display names of the property tfrom he puppet/global names
   * @param: displayNames: a field to be set with displayNames
   * @param names: array of property puppet/global names
   * @param configProperties: array of config properties of the respective service to the name param
   */
  setPropertyDisplayNames: function (displayNames, names, configProperties) {
    names.forEach(function (_name, index) {
      if (configProperties.someProperty('name', _name)) {
        displayNames.push(configProperties.findProperty('name', _name).displayName);
      }
    }, this);
  },

  /**
   * Display Error Message with service name, its custom configuration name and displaynames on the page
   * @param: customConfig: array with custom configuration, serviceName and displayNames relative to custom configuration
   */
  showCustomConfigErrMsg: function (customConfig) {

    App.ModalPopup.show({
      header: 'Custom configuration error: ',
      primary: 'OK',
      secondary: null,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        message: 'Error in custom configuration. Some properties entered in the box are already exposed on this page',
        siteProperties: customConfig,
        getDisplayMessage: function () {

        }.property('customConfig.@each.siteProperties.@each.siteProperty'),
        customConfig: customConfig,
        template: Ember.Handlebars.compile([
          '<h5>{{view.message}}</h5>',
          '<br/>',
          '<div class="pre-scrollable" style="max-height: 250px;">',
          '<ul>',
          '{{#each val in view.customConfig}}',
          '{{#if val.siteProperties}}',
          '<li>',
          '{{val.serviceName}}',
          '<ul>',
          '{{#each item in  val.siteProperties}}',
          '<li>',
          '{{item.displayMsg}}',
          '</li>',
          '{{/each}}',
          '</ul>',
          '</li>',
          '{{/if}}',
          '{{/each}}',
          '</ul>',
          '</div>'
        ].join('\n'))
      })
    });
  },

  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      var result = {};
      result = this.validateCustomConfig();
      if (result.flag === true) {
        App.router.send('next');
      } else {
        this.showCustomConfigErrMsg(result.value);
      }
    }
  }

});
