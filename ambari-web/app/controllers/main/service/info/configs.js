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
  serviceConfigTags: null,
  globalConfigs: [],
  uiConfigs: [],
  isApplyingChanges: false,
  serviceConfigs: require('data/service_configs'),
  configs: require('data/config_properties').configProperties,
  configMapping: require('data/config_mapping'),
  customConfigs: require('data/custom_configs'),

  isSubmitDisabled: function () {
    return (!(this.stepConfigs.everyProperty('errorCount', 0)) || this.get('isApplyingChanges'));
  }.property('stepConfigs.@each.errorCount', 'isApplyingChanges'),

  slaveComponentHosts: function () {
    if (!this.get('content')) {
      return;
    }
    console.log('slaveComponentHosts', App.db.getSlaveComponentHosts());
    return App.db.getSlaveComponentHosts();
  }.property('content'),

  clearStep: function () {
    this.get('stepConfigs').clear();
    this.get('globalConfigs').clear();
    if (this.get('serviceConfigTags')) {
      this.set('serviceConfigTags', null);
    }
  },

  serviceConfigProperties: function () {
    return App.db.getServiceConfigProperties();
  }.property('content'),

  /**
   * On load function
   */
  loadStep: function () {
    console.log("TRACE: Loading configure for service");
    this.clearStep();
    //STEP 1: set the present state of the service Properties. State depends on array of: unique combination of type(ex. core-site) and tag (ex. version01) derived from serviceInfo desired_state
    this.setServciceConfigs();
    //STEP 2: Create an array of objects defining tagnames to be polled and new tagnames to be set after submit
    this.setServiceTagNames();
    //STEP 3: Set globalConfigs and Get an array of serviceProperty objects
    var serviceConfigs = this.getSitesConfigProperties();
    //STEP 4: Remove properties mentioned in configMapping from serviceConfig
    this.get('configMapping').forEach(function (_configs) {

    }, this)
    //STEP 5: Add the advanced configs to the serviceConfigs property

    var advancedConfig = App.router.get('installerController').loadAdvancedConfig(this.get('content.serviceName')) || [];
    var service = this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'));
    advancedConfig.forEach(function (_config) {
      if (service) {
        if (!this.get('configMapping').someProperty('name', _config.name)) {
          if (service.configs.someProperty('name', _config.name)) {
            service.configs.findProperty('name', _config.name).description = _config.description;
          } else {
            _config.id = "site property";
            _config.category = 'Advanced';
            _config.displayName = _config.name;
            _config.defaultValue = _config.value;
            if (/\${.*}/.test(_config.value) || (service.serviceName !== 'OOZIE' && service.serviceName !== 'HBASE')) {
              _config.isRequired = false;
              _config.value = '';
            } else if (/^\s+$/.test(_config.value)) {
              _config.isRequired = false;
            }
            _config.isVisible = true;
            _config.displayType = 'advanced';
            service.configs.pushObject(_config);
          }
        }
      }
    }, this);

    this.loadCustomConfig();

    this.renderServiceConfigs(this.get('serviceConfigs'));

    console.log('---------------------------------------');


  },

  /**
   * Get configuration for the *-site.xml
   */
  setServciceConfigs: function () {
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services/' + this.get('content.serviceName');
    $.ajax({
      type: 'GET',
      url: url,
      async: false,
      timeout: 10000,
      dataType: 'text',
      success: function (data) {
        console.log("TRACE: In success function for the GET getServciceConfigs call");
        console.log("TRACE: The url is: " + url);
        var jsonData = jQuery.parseJSON(data);
        self.set('serviceConfigTags', jQuery.parseJSON(jsonData.ServiceInfo.desired_configs));
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: In error function for the getServciceConfigs call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);

      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * set tagnames for configuration of the *-site.xml
   */
  setServiceTagNames: function () {
    console.log("TRACE: In setServiceTagNames function:");
    var newServiceConfigTags = [];
    var serviceConfigTags = this.get('serviceConfigTags');
    var time = new Date().getMilliseconds();
    console.log("The value of time is: " + time);
    for (var index in serviceConfigTags) {
      console.log("The value of serviceConfigTags[index]: " + serviceConfigTags[index]);
      newServiceConfigTags.pushObject({
        siteName: index,
        tagName: serviceConfigTags[index],
        newTagName: serviceConfigTags[index] + time
      }, this);
    }
    this.set('serviceConfigTags', newServiceConfigTags);
  },

  /**
   * Render a custom conf-site box for entering properties that will be written in *-site.xml files of the services
   */
  loadCustomConfig: function () {
    var serviceConfig = this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'));
    var customConfig = this.get('customConfigs').findProperty('serviceName', this.get('content.serviceName'));
    serviceConfig.configs.pushObject(customConfig);
  },

  /**
   * load the configs from the server
   */

  getSitesConfigProperties: function () {
    var serviceConfigs = [];
    var globalConfigs = [];
    var localServiceConfigs = this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'));

    this.get('serviceConfigTags').forEach(function (_tag) {
      var properties = this.getSiteConfigProperties(_tag.siteName, _tag.tagName);
      for (var index in properties) {
        var serviceConfigObj = {
          name: index,
          value: properties[index],
          defaultValue: properties[index],
          filename: _tag.siteName + ".xml",
          isVisible: true,
          isRequired: true
        };
        if (_tag.siteName === 'global') {
          if (localServiceConfigs.configs.someProperty('name', index)) {
            var item = localServiceConfigs.configs.findProperty('name', index);
            item.value = properties[index];
            item.defaultValue = properties[index];
            if (item.displayType === 'int') {
              if (/\d+m$/.test(item.value)) {
                item.value = item.value.slice(0, item.value.length - 1);
                item.defaultValue = item.value;
              }
            }
            if (item.displayType === 'checkbox') {
              switch (item.value) {
                case 'true' :
                  item.value = true;
                  break;
                case 'false' :
                  item.value = false;
                  break;
              }
            }
          }
          serviceConfigObj.id = 'puppet var';
          serviceConfigObj.serviceName = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).serviceName : null;
          serviceConfigObj.category = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).category : null;
          globalConfigs.pushObject(serviceConfigObj);
        } else if (!this.get('configMapping').someProperty('name', index)) {
          if (_tag.siteName !== localServiceConfigs.filename) {
            serviceConfigObj.isVisible = false;
          }
          serviceConfigObj.id = 'site property';
          serviceConfigObj.serviceName = this.get('content.serviceName');
          serviceConfigObj.category = 'Advanced';
          serviceConfigObj.displayName = index;
          serviceConfigObj.displayType = 'advanced';
          localServiceConfigs.configs.pushObject(serviceConfigObj);
        }
        serviceConfigs.pushObject(serviceConfigObj);
      }
    }, this);
    this.set('globalConfigs', globalConfigs);
    return serviceConfigs;
  },

  getSiteConfigProperties: function (sitename, tagname) {
    var self = this;
    var properties = {};
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/configurations/?type=' + sitename + '&tag=' + tagname;
    $.ajax({
      type: 'GET',
      url: url,
      async: false,
      timeout: 10000,
      dataType: 'json',
      success: function (data) {
        console.log("TRACE: In success function for the GET getSiteConfigProperties call");
        console.log("TRACE: The url is: " + url);
        properties = data.items.findProperty('tag', tagname).properties;

        console.log("The value of config properties is: " + properties);
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: In error function for the getServciceConfigs call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);

      },

      statusCode: require('data/statusCodes')
    });
    return properties;
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
      console.log("config", _serviceConfigProperty);
      if(!_serviceConfigProperty) return;
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      serviceConfigProperty.serviceConfig = componentConfig;
      this.initialValue(serviceConfigProperty);
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
      console.log("config result", serviceConfigProperty);
    }, this);
  },

  restartServicePopup: function (event) {
    console.log("Enered the entry pointttt");
    var self = this;
    var result;
    console.log('I am over hererererere: ' + this.get('content.healthStatus'));
    if (this.get('isApplyingChanges') === true) {
      return;
    }
    App.ModalPopup.show({
      header: 'Restart ' + self.get('content.serviceName'),
      primary: 'Restart',
      onPrimary: function () {
        self.restartService();
        this.hide();
      },
      onSecondary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['<p>Restart the service</p>'].join('\n'))
      })
    });
  },

  restartService: function () {
    console.log("In restart servicesssss");
    this.set('isApplyingChanges', true);
    this.get('serviceConfigTags').forEach(function (_tag) {
      _tag.newTagName = _tag.newTagName + new Date().getMilliseconds();
    }, this);

    //Step 1: Stop the service

    if (this.stopService()) {
      this.doPolling('stop', function () {
        this.applyConfigurations();
      }, function () {
        this.failuresInStop();
      });
    } else {
      this.failuresInStop();
    }
  },

  failuresInStop: function () {
    console.log("Step 1 faliure");
    this.msgPopup('Restart ' + this.get('content.serviceName'), 'Failed to stop the service');
    this.set('isApplyingChanges', false);
  },

  applyConfigurations: function () {
    if (App.testMode === true) {
      result = true
    } else {
      var result = this.saveServiceConfigProperties();
    }

    if (result === false) {
      console.log("Step2 failure");
      this.msgPopup('Restart ' + this.get('content.serviceName'), 'Failed to apply configs. Start the service again with last configurations', this.startServiceWrapper);
    } else {
      if (this.startService()) {
        this.doPolling('start', function () {
          this.msgPopup('Restart ' + this.get('content.serviceName'), 'Restarted the service successfully with new configurations');
          this.set('isApplyingChanges', false);
        }, function () {
          // this.rollBackPopup('Configs applied but failures encountered during starting/checking service. Do you want to rollback to the last service configuration and restart the service.');
          console.log("Configs applied but failures encountered during starting/checking service.");
        });
      } else {
        this.msgPopup('Restart ' + this.get('content.serviceName'), 'Failed to start the service');
        this.set('isApplyingChanges', false);
      }
      console.log("Error in start service API");
    }
  },

  startServiceWrapper: function () {
    if (this.startService()) {
      this.doPolling('start', function () {
        this.msgPopup('Restart ' + this.get('content.serviceName'), 'Started the service with the last known configuration.');
        this.set('isApplyingChanges', false);
      }, function () {
        this.msgPopup('Restart ' + this.get('content.serviceName'), 'Started the service with the last known configuration.');
        this.set('isApplyingChanges', false);
      });
    } else {
      this.msgPopup('Restart ' + this.get('content.serviceName'), 'Started the service with the last known configuration.');
      this.set('isApplyingChanges', false);
    }
  },


  rollBack: function () {
    var result;
    //STEP 1: Apply the last known configuration
    result = this.applyCreatedConfToService('previous');
    //CASE 1: failure for rollback
    if (result === false) {
      console.log("rollback1 faliure");
      this.msgPopup('Restart ' + this.get('content.serviceName'), 'Failed to rolled back to the last known configuration');
    } else {
      //STEP 2: start the service
      if (this.startService()) {
        this.doPolling('start', function () {
          this.msgPopup('Restart ' + this.get('content.serviceName'), 'Successfully rolled back to the last known configuration');
          this.set('isApplyingChanges', false);
        }, function () {
          this.msgPopup('Restart ' + this.get('content.serviceName'), 'Rolled back to the last configuration but failed to start the service with the rolled back configuration');
          this.set('isApplyingChanges', false);
        });
      } else {
        this.msgPopup('Restart ' + this.get('content.serviceName'), 'Rolled back to the last configuration but failed to start the service with the rolled back configuration');
        this.set('isApplyingChanges', false);
      }
    }
  },

  startService: function () {
    var self = this;
    var clusterName = App.router.getClusterName();
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + this.get('content.serviceName');
    var method = (App.testMode) ? 'GET' : 'PUT';
    var data = '{"ServiceInfo": {"state": "STARTED"}}';
    var result;
    $.ajax({
      type: method,
      url: url,
      data: data,
      async: false,
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the startService call");
        console.log("TRACE: value of the url is: " + url);
        result = true;
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: In error function for the startService call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);

        result = (App.testMode) ? true : false;

      },

      statusCode: require('data/statusCodes')
    });
    return result;
  },

  msgPopup: function (header, message, callback) {
    var self = this;
    var result;

    App.ModalPopup.show({
      header: 'Restart ' + self.get('content.serviceName'),
      secondary: false,
      onPrimary: function () {
        if (callback !== undefined) {
          callback();
        }
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['<p>{{view.message}}</p>'].join('\n')),
        message: message
      })
    });
  },

  rollBackPopup: function (message) {
    var self = this;
    var result;

    App.ModalPopup.show({
      header: 'Restart ' + self.get('content.serviceName'),
      primary: 'Yes',
      secondary: 'No',
      onPrimary: function () {
        self.rollBack();
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['<p>{{view.message}}</p>'].join('\n')),
        message: message
      })
    });
  },

  stopService: function () {
    var self = this;
    var clusterName = App.router.getClusterName();
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + this.get('content.serviceName');
    var method = (App.testMode) ? 'GET' : 'PUT';
    var data = '{"ServiceInfo": {"state": "INSTALLED"}}';
    var result;
    $.ajax({
      type: method,
      url: url,
      data: data,
      async: false,
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the stopService call");
        console.log("TRACE: value of the url is: " + url);
        result = true;
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: STep8 -> In error function for the stopService call");
        console.log("TRACE: STep8 -> value of the url is: " + url);
        console.log("TRACE: STep8 -> error code status is: " + request.status);
        result = (App.testMode) ? true : false;
      },

      statusCode: require('data/statusCodes')
    });
    return result;
  },

  /**
   * Save config properties
   */
  saveServiceConfigProperties: function () {
    var result = false;
    var configs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    this.saveGlobalConfigs(configs);
    this.saveSiteConfigs(configs);
    this.setCustomConfigs();
    var result = this.createConfigurations();
    if (result === true) {
      result = this.applyCreatedConfToService('new');
    }
    console.log("The result from applyCreatdConfToService is: " + result);
    return result;
  },

  saveGlobalConfigs: function (configs) {
    var globalConfigs = this.get('globalConfigs');
    configs.filterProperty('id', 'puppet var').forEach(function (_config) {
      if (globalConfigs.someProperty('name', _config.name)) {
        globalConfigs.findProperty('name', _config.name).value = _config.value;
      } else {
        globalConfigs.pushObject({
          name: _config.name,
          value: _config.value
        });
      }
    }, this);
    this.set('globalConfigs', globalConfigs);
  },

  saveSiteConfigs: function (configs) {
    var storedConfigs = this.get('stepConfigs').filterProperty('id', 'site property').filterProperty('value');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('uiConfigs', storedConfigs.concat(uiConfigs));
  },

  loadUiSideConfigs: function () {
    var uiConfig = [];
    var configs = this.get('configMapping').filterProperty('foreignKey', null);
    configs.forEach(function (_config) {
      var value = this.getGlobConfigValue(_config.templateName, _config.value);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('configMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      this.setConfigValue(uiConfig, _config);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": _config.value,
        "filename": _config.filename
      });
    }, this);
    return uiConfig;
  },
  /**
   * Set all site property that are derived from other puppet-variable
   */

  getGlobConfigValue: function (templateName, expression) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return expression;
    }
    express.forEach(function (_express) {
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globalConfigs').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globValue = this.get('globalConfigs').findProperty('name', templateName[index]).value;
        value = value.replace(_express, globValue);
      } else {
        /*
         console.log("ERROR: The variable name is: " + templateName[index]);
         console.log("ERROR: mapped config from configMapping file has no corresponding variable in " +
         "content.serviceConfigProperties. Two possible reasons for the error could be: 1) The service is not selected. " +
         "and/OR 2) The service_config metadata file has no corresponding global var for the site property variable");
         */
        value = null;
      }
    }, this);
    return value;
  },
  /**
   * Set all site property that are derived from other site-properties
   */
  setConfigValue: function (uiConfig, config) {
    var fkValue = config.value.match(/<(foreignKey.*?)>/g);
    if (fkValue) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.value = config.value.replace(_fkValue, globalValue);
        } else if (this.get('stepConfigs').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('stepConfigs').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('stepConfigs').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('stepConfigs').findProperty('name', config.foreignKey[index]).value;
          }
          config.value = config.value.replace(_fkValue, globalValue);
        }
      }, this);
    }
    if (fkValue = config.name.match(/<(foreignKey.*?)>/g)) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.name = config.name.replace(_fkValue, globalValue);
        } else if (this.get('stepConfigs').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('stepConfigs').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('stepConfigs').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('stepConfigs').findProperty('name', config.foreignKey[index]).value;
          }
          config.name = config.name.replace(_fkValue, globalValue);
        }
      }, this);
    }
    //For properties in the configMapping file having foreignKey and templateName properties.
    var templateValue = config.value.match(/<(templateName.*?)>/g);
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        if (this.get('globalConfigs').someProperty('name', config.templateName[index])) {
          var globalValue = this.get('globalConfigs').findProperty('name', config.templateName[index]).value;
          config.value = config.value.replace(_value, globalValue);
        }
      }, this);
    }
  },
  createConfigurations: function () {
    var result = true;
    var serviceConfigTags = this.get('serviceConfigTags');
    serviceConfigTags.forEach(function (_serviceTags) {
      if (_serviceTags.siteName === 'global') {
        console.log("TRACE: Inside globalssss");
        result = this.createConfigSite(this.createGlobalSiteObj(_serviceTags.newTagName));
      } else if (_serviceTags.siteName === 'core-site') {
        console.log("TRACE: Inside core-site");
        result = this.createConfigSite(this.createCoreSiteObj(_serviceTags.newTagName));
      } else {
        result = this.createConfigSite(this.createSiteObj(_serviceTags.siteName, _serviceTags.newTagName));
      }
      if (result === false) {
        return false;
      }
    }, this);
    return true;
  },

  createConfigSite: function (data) {
    var result;
    var realData = data;
    console.log("Inside createConfigSite");
    var clusterName = App.router.getClusterName();
    var url = App.apiPrefix + '/clusters/' + clusterName + '/configurations';
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data),
      async: false,
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        result = true;
        console.log("TRACE: In success function for the createConfigSite");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: value of the received data is: " + jsonData);
      },

      error: function (request, ajaxOptions, error) {
        result = false;
        console.log('TRACE: In Error ');
        console.log("The original data was: " + JSON.stringify(realData));
        console.log('TRACE: Error message is: ' + request.responseText);
        console.log("TRACE: value of the url is: " + url);
      },

      statusCode: require('data/statusCodes')
    });
    console.log("Exiting createConfigSite");
    console.log("Value of result is: " + result);
    return result;
  },

  createGlobalSiteObj: function (tagName) {
    var globalSiteProperties = {};
    this.get('globalConfigs').forEach(function (_globalSiteObj) {
      // do not pass any globalConfigs whose name ends with _host or _hosts
      if (!/_hosts?$/.test(_globalSiteObj.name)) {
        // append "m" to JVM memory options
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name)) {
          _globalSiteObj.value += "m";
        }
        globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value;
        //console.log("TRACE: name of the global property is: " + _globalSiteObj.name);
        //console.log("TRACE: value of the global property is: " + _globalSiteObj.value);
      }
    }, this);
    return {"type": "global", "tag": tagName, "properties": globalSiteProperties};
  },

  createCoreSiteObj: function (tagName) {
    var coreSiteObj = this.get('uiConfigs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    // hadoop.proxyuser.oozie.hosts needs to be skipped if oozie is not selected
    var isOozieSelected = (this.get('content.serviceName') === 'OOZIE');
    coreSiteObj.forEach(function (_coreSiteObj) {
      if (isOozieSelected || _coreSiteObj.name != 'hadoop.proxyuser.oozie.hosts') {
        coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
      }
      //console.log("TRACE: name of the property is: " + _coreSiteObj.name);
      //console.log("TRACE: value of the property is: " + _coreSiteObj.value);
    }, this);
    return {"type": "core-site", "tag": tagName, "properties": coreSiteProperties};
  },

  createSiteObj: function (siteName, tagName) {
    var siteObj = this.get('uiConfigs').filterProperty('filename', siteName + ".xml");
    var siteProperties = {};
    siteObj.forEach(function (_siteObj) {
      siteProperties[_siteObj.name] = _siteObj.value;
    }, this);
    return {"type": siteName, "tag": tagName, "properties": siteProperties};
  },

  applyCreatedConfToService: function (configStatus) {
    var result;
    var clusterName = App.router.getClusterName();
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + this.get('content.serviceName');
    var data = this.getConfigForService(configStatus);
    var realData = data;
    $.ajax({
      type: 'PUT',
      url: url,
      async: false,
      dataType: 'text',
      data: JSON.stringify(data),
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the applyCreatedConfToService call");
        console.log("TRACE: value of the url is: " + url);
        result = true;
      },

      error: function (request, ajaxOptions, error) {
        console.log('Error: In Error of apply');
        console.log("The original data was: " + JSON.stringify(realData));
        console.log('Error: Error message is: ' + request.responseText);
        result = false;
      },

      statusCode: require('data/statusCodes')
    });
    console.log("Exiting applyCreatedConfToService");
    console.log("Value of result is: " + result);
    return result;
  },

  getConfigForService: function (config) {
    var data = {config: {}};
    this.get('serviceConfigTags').forEach(function (_serviceTag) {
      if (config === 'new')
        data.config[_serviceTag.siteName] = _serviceTag.newTagName;
      else if (config = 'previous') {
        data.config[_serviceTag.siteName] = _serviceTag.tagName;
      }
    }, this);
    return data;
  },

  setCustomConfigs: function () {
    var site = this.get('stepConfigs').filterProperty('id', 'conf-site');
    site.forEach(function (_site) {
      var keyValue = _site.value.split(/\n+/);
      if (keyValue) {
        keyValue.forEach(function (_keyValue) {
          console.log("The value of the keyValue is: " + _keyValue.trim());
          _keyValue = _keyValue.trim();
          var key = _keyValue.match(/(.+)=/);
          var value = _keyValue.match(/=(.*)/);
          if (key) {
            this.setSiteProperty(key[1], value[1], _site.filename);
          }

        }, this);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function (key, value, filename) {
    if (this.get('uiConfigs').someProperty('name', key)) {
      this.get('uiConfigs').findProperty('name', key).value = value;
    } else {
      this.get('uiConfigs').pushObject({
        "id": "site property",
        "name": key,
        "value": value,
        "filename": filename
      });
    }
  },

  getUrl: function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;
  },

  doPolling: function (desiredStatus, successCallback, errCallback) {
    var POLL_INTERVAL = 4000;
    var self = this;
    var result = true;
    var servicesUrl1;
    if (desiredStatus === 'stop') {
      servicesUrl1 = this.getUrl('/data/dashboard/mapreduce/mapreduce_stop.json', '/services?ServiceInfo/service_name=' + this.get('content.serviceName') + '&fields=components/host_components/*');
    } else if (desiredStatus === 'start') {
      servicesUrl1 = this.getUrl('/data/dashboard/mapreduce/mapreduce_start.json', '/services?ServiceInfo/service_name=' + this.get('content.serviceName') + '&fields=components/host_components/*');
    }

    App.HttpClient.get(servicesUrl1, App.servicesMapper, {
      complete: function () {
        var status;
        if (result === false) {
          return;
        }
        if (desiredStatus === 'stop') {
          status = self.get('content.isStopped');
          if (self.get('content.components').someProperty('workStatus', 'STOP_FAILED')) {
           // if (!self.stopService()) {
             // return;
            //}
          }
        } else if (desiredStatus === 'start') {
          status = self.get('content.isStarted');
          if (self.get('content.components').someProperty('workStatus', 'START_FAILED')) {
            //if (!self.startService()) {
             // return;
            //}
          }
        }
        if (status !== true) {
          window.setTimeout(function () {
            self.doPolling(desiredStatus, successCallback, errCallback);
          }, POLL_INTERVAL);
        } else if (status === true) {
          successCallback.apply(self);
        }
      },
      error: function (jqXHR, textStatus) {
        errCallback.apply(self);
        result = false;
      }
    });
    return result;
  },

  initialValue: function (config) {
    switch (config.name) {
      case 'namenode_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'NAMENODE').get('host.hostName'));
        break;
      case 'snamenode_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'SECONDARY_NAMENODE').get('host.hostName'));
        break;
      case 'jobtracker_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'JOBTRACKER').get('host.hostName'));
        break;
      case 'hbasemaster_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'HBASE_MASTER').get('host.hostName'));
        break;
      case 'hivemetastore_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'HIVE_SERVER').get('host.hostName'));
        break;
      case 'hive_ambari_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'HIVE_SERVER').get('host.hostName'));
        break;
      case 'oozieserver_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName'));
        break;
      case 'oozie_ambari_host':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName'));
        break;
      case 'zookeeperserver_hosts':
        config.set('id', 'puppet var');
        config.set('value', this.get('content.components').findProperty('componentName', 'ZOOKEEPER_SERVER').get('host.hostName'));
        break;
    }
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