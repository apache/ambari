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
  dataIsLoaded: false,
  stepConfigs: [], //contains all field properties that are viewed in this service
  selectedService: null,
  serviceConfigTags: null,
  globalConfigs: [],
  uiConfigs: [],
  customConfig: [],
  isApplyingChanges: false,
  serviceConfigs: require('data/service_configs'),
  configs: require('data/config_properties').configProperties,
  configMapping: require('data/config_mapping'),
  customConfigs: require('data/custom_configs'),

  isSubmitDisabled: function () {
    return (!(this.stepConfigs.everyProperty('errorCount', 0)) || this.get('isApplyingChanges'));
  }.property('stepConfigs.@each.errorCount', 'isApplyingChanges'),

  slaveComponentGroups: null,

  clearStep: function () {
    this.set('dataIsLoaded', false);
    this.get('stepConfigs').clear();
    this.get('globalConfigs').clear();
    this.get('uiConfigs').clear();
    this.get('customConfig').clear();
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
    // this.set('serviceConfigs',require('data/service_configs'));
    //STEP 1: set the present state of the service Properties. State depends on array of: unique combination of type(ex. core-site) and tag (ex. version01) derived from serviceInfo desired_state
    this.loadMasterComponents();
    //this.loadSlaveComponentVersion();
  },

  /**
   * loads Master component properties
   */
  loadMasterComponents: function () {
    this.setServciceConfigs();
  },


  /**
   * loads slave Group Version from Ambari UI Database
   */
  loadSlaveComponentVersion: function () {
    var self = this;
    var url = App.apiPrefix + '/persist/current_version';
    $.ajax({
      type: 'GET',
      url: url,
      timeout: 10000,

      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the GET loadSlaveComponentGroup call");
        console.log("TRACE: The url is: " + url);
        self.loadSlaveComponentGroup(jsonData["current_version"]);
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
   * loads slave Group properties of currntly applid version from Ambari UI Database
   */
  loadSlaveComponentGroup: function (version) {
    var self = this;
    var url = App.apiPrefix + '/persist/' + version;
    $.ajax({
      type: 'GET',
      url: url,
      timeout: 10000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the GET loadSlaveComponentGroup call");
        console.log("TRACE: The url is: " + url);
        self.set('slaveComponentGroups', jsonData[version]);
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
   * Get the current applied slave configuration version from Ambari UI Database
   */
  getCurrentSlaveConfiguration: function () {

  },

  /**
   *  Loads the advanced configs fetched from the server metadata libarary
   */
  loadAdvancedConfig: function (serviceConfigs, advancedConfig) {
    advancedConfig.forEach(function (_config) {
      if (_config) {
        if (this.get('configMapping').someProperty('name', _config.name)) {
        } else if (!(serviceConfigs.someProperty('name', _config.name))) {
          _config.id = "site property";
          _config.category = 'Advanced';
          _config.displayName = _config.name;
          _config.defaultValue = _config.value;
          _config.isRequired = false;
          _config.isVisible = true;
          _config.displayType = 'advanced';
          _config.serviceName = this.get('content.serviceName');
          serviceConfigs.pushObject(_config);
        }
      }
    }, this);
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
      timeout: 10000,
      dataType: 'text',
      success: function (data) {
        console.log("TRACE: In success function for the GET getServciceConfigs call");
        console.log("TRACE: The url is: " + url);
        var jsonData = jQuery.parseJSON(data);
        self.set('serviceConfigTags', jsonData.ServiceInfo.desired_configs);
        //STEP 2: Create an array of objects defining tagnames to be polled and new tagnames to be set after submit
        self.setServiceTagNames();
        //STEP 5: Add the advanced configs to the serviceConfigs property
        var advancedConfig = App.router.get('installerController').loadAdvancedConfig(self.get('content.serviceName')) || [];
        //STEP 3: Set globalConfigs and Get an array of serviceProperty objects
        var serviceConfigs = self.getSitesConfigProperties(advancedConfig);
        self.loadAdvancedConfig(serviceConfigs, advancedConfig);
        self.loadCustomConfig(serviceConfigs);
        var serviceConfig = self.get('serviceConfigs').findProperty('serviceName', self.get('content.serviceName'));
        self.addHostNamesToGlobalConfig();
        serviceConfig.configs = self.get('globalConfigs').concat(serviceConfigs);

        self.renderServiceConfigs(serviceConfig);
        self.set('dataIsLoaded', true);
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
  loadCustomConfig: function (serviceConfigs) {
    if (this.get('customConfigs').findProperty('serviceName', this.get('content.serviceName'))) {
      var customConfigs = this.get('customConfigs').findProperty('serviceName', this.get('content.serviceName'));
      var customValue = '';
      var length = this.get('customConfig').length;
      this.get('customConfig').forEach(function (_config, index) {
        customValue += _config.name + '=' + _config.value;
        if (index !== length - 1) {
          customValue += '\n';
        }
      }, this);
      customConfigs.value = customValue;
      serviceConfigs.pushObject(customConfigs);
    }
  },


  /**
   * load the configs from the server
   */

  getSitesConfigProperties: function (advancedConfig) {
    var serviceConfigs = [];
    var globalConfigs = [];
    var localServiceConfigs = this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName'));

    this.get('serviceConfigTags').forEach(function (_tag) {
      if (_tag.siteName !== 'core-site') {
        var properties = this.getSiteConfigProperties(_tag.siteName, _tag.tagName);
        for (var index in properties) {
          var serviceConfigObj = {
            name: index,
            value: properties[index],
            defaultValue: properties[index],
            filename: _tag.siteName + ".xml"
          };
          if (this.get('configs').someProperty('name', index)) {
            var configProperty = this.get('configs').findProperty('name', index);
            if (this.get('configs').findProperty('name', index).isReconfigurable === false) {
            }
            serviceConfigObj.displayType = configProperty.displayType;
            serviceConfigObj.isRequired = configProperty.isRequired ? configProperty.isRequired : true;
            serviceConfigObj.isReconfigurable = (configProperty.isReconfigurable !== undefined) ? configProperty.isReconfigurable : true;
            serviceConfigObj.isVisible = (configProperty.isVisible !== undefined) ? configProperty.isVisible : true;

          }
          serviceConfigObj.displayType = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).displayType : null;

          serviceConfigObj.isRequired = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).isRequired : null;

          if (_tag.siteName === 'global') {
            if (this.get('configs').someProperty('name', index)) {
              var item = this.get('configs').findProperty('name', index);
              if (item.displayType === 'int') {
                if (/\d+m$/.test(properties[index])) {

                  serviceConfigObj.value = properties[index].slice(0, properties[index].length - 1);
                  serviceConfigObj.defaultValue = serviceConfigObj.value;
                }
              }
              if (item.displayType === 'checkbox') {
                switch (properties[index]) {
                  case 'true' :
                    serviceConfigObj.value = true;
                    serviceConfigObj.defaultValue = true;
                    break;
                  case 'false' :
                    serviceConfigObj.value = false;
                    serviceConfigObj.defaultValue = false;
                    break;
                }
              }
            }
            serviceConfigObj.id = 'puppet var';
            serviceConfigObj.serviceName = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).serviceName : null;
            serviceConfigObj.displayName = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).displayName : null;
            serviceConfigObj.category = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).category : null;
            serviceConfigObj.options = this.get('configs').someProperty('name', index) ? this.get('configs').findProperty('name', index).options : null;
            globalConfigs.pushObject(serviceConfigObj);
          } else if (!this.get('configMapping').someProperty('name', index)) {
            if (advancedConfig.someProperty('name', index)) {

              if (_tag.siteName !== this.get('serviceConfigs').findProperty('serviceName', this.get('content.serviceName')).filename) {
                serviceConfigObj.isVisible = false;
              }
              serviceConfigObj.id = 'site property';
              serviceConfigObj.serviceName = this.get('content.serviceName');
              serviceConfigObj.category = 'Advanced';
              serviceConfigObj.displayName = index;
              serviceConfigObj.displayType = 'advanced';
              // localServiceConfigs.configs.pushObject(serviceConfigObj);
              serviceConfigs.pushObject(serviceConfigObj);
            } else {
              serviceConfigObj.id = 'conf-site';
              serviceConfigObj.serviceName = this.get('content.serviceName');
              this.get('customConfig').pushObject(serviceConfigObj);
            }

          }

        }
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

    var serviceConfig = App.ServiceConfig.create({
      filename: serviceConfigs.filename,
      serviceName: serviceConfigs.serviceName,
      displayName: serviceConfigs.displayName,
      configCategories: serviceConfigs.configCategories,
      configs: []
    });

    if ((this.get('content.serviceName') && this.get('content.serviceName').toUpperCase() === serviceConfig.serviceName) || serviceConfig.serviceName === 'MISC') {

      this.loadComponentConfigs(serviceConfigs, serviceConfig);

      console.log('pushing ' + serviceConfig.serviceName);
      this.get('stepConfigs').pushObject(serviceConfig);

    } else {
      console.log('skipping ' + serviceConfig.serviceName);
    }

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
      if (!_serviceConfigProperty) return;
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      if (serviceConfigProperty.get('serviceName') === this.get('content.serviceName')) {
        // serviceConfigProperty.serviceConfig = componentConfig;
        if (App.db.getUser().admin) {
          serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
        } else {
          serviceConfigProperty.set('isEditable', false);
        }

        console.log("config result", serviceConfigProperty);
      } else {
        serviceConfigProperty.set('isVisible', false);
      }
      componentConfig.configs.pushObject(serviceConfigProperty);
      serviceConfigProperty.validate();
    }, this);
  },

  restartServicePopup: function (event) {
    console.log("Enered the entry pointttt");
    var self = this;
    var header;
    var message;
    var value;
    var flag;
    if (this.get('content.isStopped') === true) {
      var result = this.saveServiceConfigProperties();
      flag = result.flag;
      if (flag === true) {
        header = 'Start Service';
        message = 'Service configuration applied successfully';
      } else {
        header = 'Faliure';
        message = result.message;
        value = result.value;
      }

    } else {
      header = 'Stop Service';
      message = 'Stop the service and wait till it stops completely. Thereafter you can apply configuration changes';
    }
    App.ModalPopup.show({
      header: header,
      primary: 'OK',
      secondary: null,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        flag: flag,
        message: message,
        siteProperties: value,
        getDisplayMessage: function () {
          var displayMsg = [];
          var siteProperties = this.get('siteProperties');
          if (siteProperties) {
            siteProperties.forEach(function (_siteProperty) {
              var displayProperty = _siteProperty.siteProperty;
              var displayNames = _siteProperty.displayNames;
              /////////
              if (displayNames && displayNames.length) {
                if (displayNames.length === 1) {
                  displayMsg.push(displayProperty + ' as ' + displayNames[0]);
                } else {
                  var name;
                  displayNames.forEach(function (_name, index) {
                    if (index === 0) {
                      name = _name;
                    } else if (index === siteProperties.length - 1) {
                      name = name + ' and ' + _name;
                    } else {
                      name = name + ', ' + _name;
                    }
                  }, this);
                  displayMsg.push(displayProperty + ' as ' + name);

                }
              } else {
                displayMsg.push(displayProperty);
              }
            }, this);
          }
          return displayMsg;

        }.property('siteProperties'),
        template: Ember.Handlebars.compile([
          '<h5>{{view.message}}</h5>',
          '{{#unless view.flag}}',
          '<br/>',
          '<div class="pre-scrollable" style="max-height: 250px;">',
          '<ul>',
          '{{#each val in view.getDisplayMessage}}',
          '<li>',
          '{{val}}',
          '</li>',
          '{{/each}}',
          '</ul>',
          '</div>',
          '{{/unless}}'
        ].join('\n'))
      })
    });
  },

  /**
   * Save config properties
   */
  saveServiceConfigProperties: function () {
    var result = {
      flag: false,
      message: null,
      value: null
    };
    var configs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs');
    this.saveGlobalConfigs(configs);
    this.saveSiteConfigs(configs);
    var customConfigResult = this.setCustomConfigs();
    result.flag = customConfigResult.flag;
    result.value = customConfigResult.value;
    if (result.flag !== true) {
      result.message = 'Error in custom configuration. Some properties entered in the box are already exposed on this page';
      return result;
    }
    result.flag = result.flag && this.createConfigurations();
    if (result.flag === true) {
      result.flag = this.applyCreatedConfToService('new');
    } else {
      result.message = 'Faliure in applying service configuration';
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

    this.setHiveHostName(globalConfigs);
    this.set('globalConfigs', globalConfigs);
  },

  setHiveHostName: function(globals) {
    if (globals.someProperty('name', 'hive_database')) {
      //TODO: Hive host depends on the type of db selected. Change puppet variable name if postgres is not the default db
      var hiveDb = globals.findProperty('name', 'hive_database');
      if (hiveDb.value === 'New MySQL Database') {
        if (globals.someProperty('name', 'hive_ambari_host')) {
          globals.findProperty('name', 'hive_ambari_host').name = 'hive_mysql_hostname';
        }
        globals = globals.without(globals.findProperty('name', 'hive_existing_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_database'));
      } else {
        globals.findProperty('name', 'hive_existing_host').name = 'hive_mysql_hostname';
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
      }
    }
  },

  saveSiteConfigs: function (configs) {
    var storedConfigs = configs.filterProperty('id', 'site property').filterProperty('value');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('uiConfigs', storedConfigs.concat(uiConfigs));
  },

  loadUiSideConfigs: function () {
    var uiConfig = [];
    var configs = this.get('configMapping').filterProperty('foreignKey', null);
    configs.forEach(function (_config) {
      var value = this.getGlobConfigValue(_config.templateName, _config.value, _config.name);
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

  getGlobConfigValue: function (templateName, expression, name) {
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
        // Hack for templeton.zookeeper.hosts
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          if (name === "templeton.zookeeper.hosts" || name === 'hbase.zookeeper.quorum') {
            var zooKeeperPort = '2181';
            if (typeof globValue === 'string') {
              var temp = [];
              temp.push(globValue);
              globValue = temp;
            }
            if (name === "templeton.zookeeper.hosts") {
              globValue.forEach(function (_host, index) {
                globValue[index] = globValue[index] + ':' + zooKeeperPort;
              }, this);
            }
            value = value.replace(_express, globValue.toString());
          } else {
            value = value.replace(_express, globValue);
          }
        }
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
        } else if (this.get('globalConfigs').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('globalConfigs').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('globalConfigs').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('globalConfigs').findProperty('name', config.foreignKey[index]).value;
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
        } else if (this.get('globalConfigs').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('globalConfigs').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('globalConfigs').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('globalConfigs').findProperty('name', config.foreignKey[index]).value;
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
        } else {
          config.value = null;
        }
      }, this);
    }
  },
  createConfigurations: function () {
    var result = true;
    var serviceConfigTags = this.get('serviceConfigTags');
    serviceConfigTags.forEach(function (_serviceTags) {
      if (_serviceTags.siteName === 'global') {
        console.log("TRACE: Inside global");
        result = result && this.createConfigSite(this.createGlobalSiteObj(_serviceTags.newTagName));
      } else if (_serviceTags.siteName === 'core-site') {
        console.log("TRACE: Inside core-site");
        result = result && this.createConfigSite(this.createCoreSiteObj(_serviceTags.newTagName));
      } else {
        result = result && this.createConfigSite(this.createSiteObj(_serviceTags.siteName, _serviceTags.newTagName));
      }
    }, this);
    return result;
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
      if (_coreSiteObj.name != 'hadoop.proxyuser.oozie.hosts') {
        coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
      }
      //console.log("TRACE: name of the property is: " + _coreSiteObj.name);
      //console.log("TRACE: value of the property is: " + _coreSiteObj.value);
    }, this);
    return {"type": "core-site", "tag": tagName, "properties": coreSiteProperties};
  },

  createSiteObj: function (siteName, tagName) {
    var siteObj = this.get('uiConfigs').filterProperty('filename', siteName + '.xml');
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
    var site = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).get('configs').findProperty('id', 'conf-site');
    var siteProperties = [];
    var flag = true;
    var keyValue = site.value.split(/\n+/);
    if (keyValue) {
      keyValue.forEach(function (_keyValue) {
        console.log("The value of the keyValue is: " + _keyValue.trim());
        _keyValue = _keyValue.trim();
        var key = _keyValue.match(/(.+)=/);
        var value = _keyValue.match(/=(.*)/);
        if (key) {
          // Check dat entered config is allowed to reconfigure
          if (this.get('uiConfigs').someProperty('name', key[1])) {
            var property = {
              siteProperty: key[1],
              displayNames: []
            };
            if (this.get('configMapping').someProperty('name', key[1])) {
              this.setPropertyDisplayNames(property.displayNames, this.get('configMapping').findProperty('name', key[1]).templateName);
            }
            siteProperties.push(property);
            flag = false;
          } else if (flag) {
            this.setSiteProperty(key[1], value[1], site.name + '.xml');
          }
        }

      }, this);
    }
    var result = {
      flag: flag,
      value: siteProperties
    }
    return result;
  },

  /**
   * Set display names of the property tfrom he puppet/global names
   * @param: displayNames: a field to be set with displayNames
   * @param names: array of property puppet/global names
   */
  setPropertyDisplayNames: function (displayNames, names) {
    var stepConfigs = this.get('stepConfigs').findProperty('serviceName', this.get('content.serviceName')).configs;
    names.forEach(function (_name, index) {
      if (stepConfigs.someProperty('name', _name)) {
        displayNames.push(stepConfigs.findProperty('name', _name).displayName);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function (key, value, filename) {
    this.get('uiConfigs').pushObject({
      "id": "site property",
      "name": key,
      "value": value,
      "filename": filename
    });
  },

  getUrl: function (testUrl, url) {
    return (App.testMode) ? testUrl : App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;
  },

  /**
   * Adds host name of master component to global config;
   */
  addHostNamesToGlobalConfig: function () {
    var serviceName = this.get('content.serviceName');
    var globalConfigs = this.get('globalConfigs');
    var serviceConfigs = this.get('serviceConfigs').findProperty('serviceName', serviceName).configs;
    //namenode_host is required to derive "fs.default.name" a property of core-site
    var nameNodeHost = this.get('serviceConfigs').findProperty('serviceName', 'HDFS').configs.findProperty('name', 'namenode_host');
    nameNodeHost.defaultValue = App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'NAMENODE').get('host.hostName');
    globalConfigs.push(nameNodeHost);

    //zooKeeperserver_host
    var zooKeperHost = this.get('serviceConfigs').findProperty('serviceName', 'ZOOKEEPER').configs.findProperty('name', 'zookeeperserver_hosts');
    if (serviceName === 'ZOOKEEPER' || serviceName === 'HBASE' || serviceName === 'WEBHCAT') {
      zooKeperHost.defaultValue = App.Service.find('ZOOKEEPER').get('hostComponents').filterProperty('componentName', 'ZOOKEEPER_SERVER').mapProperty('host.hostName');
      globalConfigs.push(zooKeperHost);
    }

    switch (serviceName) {
      case 'HDFS':
        var sNameNodeHost = serviceConfigs.findProperty('name', 'snamenode_host');
        sNameNodeHost.defaultValue = this.get('content.components').findProperty('componentName', 'SECONDARY_NAMENODE').get('host.hostName');
        globalConfigs.push(sNameNodeHost);
        break;
      case 'MAPREDUCE':
        var jobTrackerHost = serviceConfigs.findProperty('name', 'jobtracker_host');
        jobTrackerHost.defaultValue = this.get('content.components').findProperty('componentName', 'JOBTRACKER').get('host.hostName');
        globalConfigs.push(jobTrackerHost);
        break;
      case 'HIVE':
        var hiveMetastoreHost = serviceConfigs.findProperty('name', 'hivemetastore_host');
        hiveMetastoreHost.defaultValue = this.get('content.components').findProperty('componentName', 'HIVE_SERVER').get('host.hostName');
        globalConfigs.push(hiveMetastoreHost);
        break;
      case 'OOZIE':
        var oozieServerHost = serviceConfigs.findProperty('name', 'oozieserver_host');
        oozieServerHost.defaultValue = this.get('content.components').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName');
        globalConfigs.push(oozieServerHost);
        break;
      case 'HBASE':
        var hbaseMasterHost = serviceConfigs.findProperty('name', 'hbasemaster_host');
        hbaseMasterHost.defaultValue = this.get('content.components').findProperty('componentName', 'HBASE_MASTER').get('host.hostName');
        globalConfigs.push(hbaseMasterHost);
        break;
    }
  }

})
;


App.MainServiceSlaveComponentGroupsController = App.SlaveComponentGroupsController.extend({
  name: 'mainServiceSlaveComponentGroupsController',
  contentBinding: 'App.router.mainServiceInfoConfigsController.slaveComponentGroups',
  stepConfigsBinding: 'App.router.mainServiceInfoConfigsController.stepConfigs',
  serviceBinding: 'App.router.mainServiceInfoConfigsController.selectedService'

});