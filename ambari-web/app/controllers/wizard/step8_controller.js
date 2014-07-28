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
var stringUtils = require('utils/string_utils');

App.WizardStep8Controller = Em.Controller.extend({

  name: 'wizardStep8Controller',

  /**
   * List of raw data about cluster that should be displayed
   * @type {Array}
   */
  rawContent: [
    {
      config_name: 'Admin',
      display_name: 'Admin Name',
      config_value: ''
    },
    {
      config_name: 'cluster',
      display_name: 'Cluster Name',
      config_value: ''
    },
    {
      config_name: 'hosts',
      display_name: 'Total Hosts',
      config_value: ''
    },
    {
      config_name: 'Repo',
      display_name: 'Local Repository',
      config_value: ''
    }
  ],

  /**
   * List of data about cluster (based on formatted <code>rawContent</code>)
   * @type {Object[]}
   */
  clusterInfo: [],

  /**
   * List of services with components assigned to hosts
   * @type {Object[]}
   */
  services: [],

  /**
   * @type {Object[]}
   */
  configs: [],

  /**
   * All configs
   * @type {Array}
   */
  configMapping: function () {
    return App.config.get('configMapping').all(true);
  }.property('App.config.configMapping'),

  /**
   *
   */
  slaveComponentConfig: null,

  /**
   * Should Submit button be disabled
   * @type {bool}
   */
  isSubmitDisabled: false,

  /**
   * Should Back button be disabled
   * @type {bool}
   */
  isBackBtnDisabled: false,

  /**
   * Is error appears while <code>ajaxQueue</code> executes
   * @type {bool}
   */
  hasErrorOccurred: false,

  /**
   * Are services installed
   * Used to hide Deploy Progress Bar
   * @type {bool}
   */
  servicesInstalled: false,

  /**
   * List of service config tags
   * @type {Object[]}
   */
  serviceConfigTags: [],

  /**
   * Ajax-requests queue
   * @type {App.ajaxQueue}
   */
  ajaxRequestsQueue: null,

  /**
   * Is cluster security enabled
   * @type {bool}
   */
  securityEnabled: function () {
    return App.router.get('mainAdminSecurityController.securityEnabled');
  }.property('App.router.mainAdminSecurityController.securityEnabled'),

  /**
   * Selected config group
   * @type {Object}
   */
  selectedConfigGroup: null,

  /**
   * List of config groups
   * @type {Object[]}
   */
  configGroups: [],

  /**
   * List of selected but not installed services
   * @type {Object[]}
   */
  selectedServices: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false);
  }.property('content.services').cacheable(),

  /**
   * List of installed and selected services
   * @type {Object[]}
   */
  installedServices: function () {
    return this.get('content.services').filterProperty('isSelected').filterProperty('isInstalled');
  }.property('content.services').cacheable(),

  /**
   * Ajax-requests count
   * @type {number}
   */
  ajaxQueueLength: 0,

  /**
   * Current cluster name
   * @type {string}
   */
  clusterName: function () {
    return this.get('content.cluster.name');
  }.property('content.cluster.name'),

  /**
   * List of existing cluster names
   * @type {string[]}
   */
  clusterNames: [],

  /**
   * Clear current step data
   * @method clearStep
   */
  clearStep: function () {
    this.get('services').clear();
    this.get('configs').clear();
    this.get('clusterInfo').clear();
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
    this.set('ajaxQueueLength', 0);
    this.set('ajaxRequestsQueue', App.ajaxQueue.create());
    this.set('ajaxRequestsQueue.finishedCallback', this.ajaxQueueFinished);
  },

  /**
   * Load current step data
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading step8: Review Page");
    if (this.get('content.controllerName') != 'installerController') {
      App.router.get('mainAdminSecurityController').setSecurityStatus();
    }
    this.clearStep();
    if (this.get('content.serviceConfigProperties')) {
      this.formatProperties();
      this.loadConfigs();
    }
    this.loadClusterInfo();
    this.loadServices();
    this.set('isSubmitDisabled', false);
    this.set('isBackBtnDisabled', false);
  },

  /**
   * replace whitespace character with coma between directories
   * @method formatProperties
   */
  formatProperties: function () {
    this.get('content.serviceConfigProperties').forEach(function (_configProperty) {
      _configProperty.value = App.config.trimProperty(_configProperty, false);
    });
  },

  /**
   * Remove unused Hive configs
   * @param {Ember.Enumerable} configs
   * @returns {Ember.Enumerable}
   * @method removeHiveConfigs
   */
  removeHiveConfigs: function (configs) {
    var hiveDb = configs.findProperty('name', 'hive_database');
    var hiveDbType = {name: 'hive_database_type', value: 'mysql'};

    var hive_properties = Em.A([]);

    if (hiveDb.value === 'New MySQL Database') {
      if (configs.someProperty('name', 'hive_ambari_host')) {
        configs.findProperty('name', 'hive_hostname').value = configs.findProperty('name', 'hive_ambari_host').value;
        hiveDbType.value = 'mysql';
      }
      hive_properties = Em.A(['hive_existing_mysql_host', 'hive_existing_mysql_database', 'hive_existing_oracle_host',
        'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database']);
    }
    else {
      if (hiveDb.value === 'Existing MySQL Database') {
        configs.findProperty('name', 'hive_hostname').value = configs.findProperty('name', 'hive_existing_mysql_host').value;
        hiveDbType.value = 'mysql';
        hive_properties = Em.A(['hive_ambari_host', 'hive_ambari_database', 'hive_existing_oracle_host',
          'hive_existing_oracle_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database']);
      }
      else {
        if (hiveDb.value === Em.I18n.t('services.service.config.hive.oozie.postgresql')) {
          configs.findProperty('name', 'hive_hostname').value = configs.findProperty('name', 'hive_existing_postgresql_host').value;
          hiveDbType.value = 'postgres';
          hive_properties = Em.A(['hive_ambari_host', 'hive_ambari_database', 'hive_existing_oracle_host',
            'hive_existing_oracle_database', 'hive_existing_mysql_host', 'hive_existing_mysql_database']);
        }
        else { //existing oracle database
          configs.findProperty('name', 'hive_hostname').value = configs.findProperty('name', 'hive_existing_oracle_host').value;
          hiveDbType.value = 'oracle';
          hive_properties = Em.A(['hive_ambari_host', 'hive_ambari_database', 'hive_existing_mysql_host',
            'hive_existing_mysql_database', 'hive_existing_postgresql_host', 'hive_existing_postgresql_database']);
        }
      }
    }

    hive_properties.forEach(function (property) {
      configs = configs.without(configs.findProperty('name', property));
    });

    configs.pushObject(hiveDbType);
    return configs;
  },

  /**
   * Remove unused Oozie configs
   * @param {Ember.Enumerable} configs
   * @returns {Ember.Enumerable}
   * @method removeOozieConfigs
   */
  removeOozieConfigs: function (configs) {
    var oozieDb = configs.findProperty('name', 'oozie_database');
    var oozieDbType = {name: 'oozie_database_type'};

    var oozie_properties = Em.A(['oozie_ambari_host', 'oozie_ambari_database']);

    if (oozieDb.value === 'New Derby Database') {
      configs.findProperty('name', 'oozie_hostname').value = configs.findProperty('name', 'oozie_ambari_host').value;
      oozieDbType.value = 'derby';
      oozie_properties = Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_mysql_host',
        'oozie_existing_mysql_database', 'oozie_existing_oracle_host', 'oozie_existing_oracle_database',
        'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database']);
    }
    else {
      if (oozieDb.value === 'Existing MySQL Database') {
        configs.findProperty('name', 'oozie_hostname').value = configs.findProperty('name', 'oozie_existing_mysql_host').value;
        oozieDbType.value = 'mysql';
        oozie_properties = Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_oracle_host',
          'oozie_existing_oracle_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database']);
      }
      else {
        if (oozieDb.value === Em.I18n.t('services.service.config.hive.oozie.postgresql')) {
          configs.findProperty('name', 'oozie_hostname').value = configs.findProperty('name', 'oozie_existing_postgresql_host').value;
          oozieDbType.value = 'postgresql';
          oozie_properties = Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_oracle_host',
            'oozie_existing_oracle_database', 'oozie_existing_mysql_host', 'oozie_existing_mysql_database']);
        }
        else { // existing oracle database
          configs.findProperty('name', 'oozie_hostname').value = configs.findProperty('name', 'oozie_existing_oracle_host').value;
          oozieDbType.value = 'oracle';
          oozie_properties = Em.A(['oozie_ambari_host', 'oozie_ambari_database', 'oozie_existing_mysql_host',
            'oozie_existing_mysql_database', 'oozie_derby_database', 'oozie_existing_postgresql_host', 'oozie_existing_postgresql_database']);
        }
      }
    }
    oozie_properties.forEach(function (property) {
      configs = configs.without(configs.findProperty('name', property));
    });
    configs.pushObject(oozieDbType);
    return configs;
  },

  /**
   * Load all site properties
   * @method loadConfigs
   */
  loadConfigs: function () {
    //storedConfigs contains custom configs as well
    var configs = this.get('content.serviceConfigProperties');
    if (configs.someProperty('name', 'hive_database')) {
      configs = this.removeHiveConfigs(configs);
    }
    if (configs.someProperty('name', 'oozie_database')) {
      configs = this.removeOozieConfigs(configs);
    }
    configs.forEach(function (_config) {
      _config.value = (typeof _config.value === "boolean") ? _config.value.toString() : _config.value;
    });
    var mappedConfigs = App.config.excludeUnsupportedConfigs(this.get('configMapping'), this.get('selectedServices').mapProperty('serviceName'));
    var uiConfigs = this.loadUiSideConfigs(mappedConfigs);
    this.set('configs', configs.concat(uiConfigs));
  },

  /**
   * Load UI configs
   * @param {Array} configMapping
   * @return {Array}
   * @method loadUiSideConfigs
   */
  loadUiSideConfigs: function (configMapping) {
    var uiConfig = [];
    var configs = configMapping.filterProperty('foreignKey', null);
    this.addDynamicProperties(configs);
    configs.forEach(function (_config) {
      var valueWithOverrides = this.getGlobConfigValueWithOverrides(_config.templateName, _config.value, _config.name);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": valueWithOverrides.value,
        "filename": _config.filename,
        "overrides": valueWithOverrides.overrides
      });
    }, this);
    var dependentConfig = $.extend(true, [], configMapping.filterProperty('foreignKey'));
    dependentConfig.forEach(function (_config) {
      App.config.setConfigValue(uiConfig, this.get('content.serviceConfigProperties'), _config);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config._name || _config.name,
        "value": _config.value,
        "filename": _config.filename
      });
    }, this);
    return uiConfig;
  },

  /**
   * Add dynamic properties to configs
   * @param {Array} configs
   * @method addDynamicProperties
   */
  addDynamicProperties: function (configs) {
    var templetonHiveProperty = this.get('content.serviceConfigProperties').someProperty('name', 'templeton.hive.properties');
    if (!templetonHiveProperty) {
      configs.pushObject({
        "name": "templeton.hive.properties",
        "templateName": ["hivemetastore_host"],
        "foreignKey": null,
        "value": "hive.metastore.local=false,hive.metastore.uris=thrift://<templateName[0]>:9083,hive.metastore.sasl.enabled=yes,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse",
        "filename": "webhcat-site.xml"
      });
    }
  },

  /**
   * Format <code>content.hosts</code> from Object to Array
   * @returns {Array}
   * @method getRegisteredHosts
   */
  getRegisteredHosts: function () {
    var allHosts = this.get('content.hosts');
    var hosts = [];
    for (var hostName in allHosts) {
      if (allHosts.hasOwnProperty(hostName)) {
        if (allHosts[hostName].bootStatus == 'REGISTERED') {
          allHosts[hostName].hostName = allHosts[hostName].name;
          hosts.pushObject(allHosts[hostName]);
        }
      }
    }
    return hosts;
  },

  /**
   * Set all site property that are derived from other puppet-variable
   * @param {String} templateName
   * @param {String} expression
   * @param {String} name
   * @return {Object}
   * example: <code>{
   *   value: '...',
   *   overrides: [
   *    {
   *      value: 'v1',
   *      hosts: ['h1', 'h2']
   *    },
   *    {
   *      value: 'v2',
   *      hosts: ['h2', 'h3']
   *    },
   *    ....
   *   ]
   * }</code>
   * @method getGlobConfigValueWithOverrides
   */
  getGlobConfigValueWithOverrides: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return { value: expression, overrides: []};      // if site property do not map any global property then return the value
    }
    var overrideHostToValue = {};
    express.forEach(function (_express) {
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('configs').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globalObj = this.get('configs').findProperty('name', templateName[index]);
        var globValue = globalObj.value;
        // Hack for templeton.zookeeper.hosts
        var preReplaceValue = null;
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          preReplaceValue = value;
          value = this._replaceConfigValues(name, _express, value, globValue);
        }
        if (globalObj.overrides != null) {
          globalObj.overrides.forEach(function (override) {
            var ov = override.value;
            var hostsArray = override.hosts;
            hostsArray.forEach(function (host) {
              if (!(host in overrideHostToValue)) {
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, preReplaceValue, ov);
              } else {
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, overrideHostToValue[host], ov);
              }
            }, this);
          }, this);
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

    var valueWithOverrides = {
      value: value,
      overrides: []
    };
    var overrideValueToHostMap = {};
    if (!jQuery.isEmptyObject(overrideHostToValue)) {
      for (var host in overrideHostToValue) {
        var hostVal = overrideHostToValue[host];
        if (!(hostVal in overrideValueToHostMap)) {
          overrideValueToHostMap[hostVal] = [];
        }
        overrideValueToHostMap[hostVal].push(host);
      }
    }
    for (var val in overrideValueToHostMap) {
      valueWithOverrides.overrides.push({
        value: val,
        hosts: overrideValueToHostMap[val]
      });
    }
    return valueWithOverrides;
  },

  /**
   * replace some values in config property
   * @param {string} name
   * @param {string} express
   * @param {string} value
   * @param {string} globValue
   * @return {string}
   * @private
   * @method _replaceConfigValues
   */
  _replaceConfigValues: function (name, express, value, globValue) {
    return value.replace(express, globValue);
  },

  /**
   * Load all info about cluster to <code>clusterInfo</code> variable
   * @method loadClusterInfo
   */
  loadClusterInfo: function () {

    //Admin name
    var admin = this.rawContent.findProperty('config_name', 'Admin');
    admin.config_value = App.db.getLoginName();
    console.log("STEP8: the value of content cluster name: " + App.db.getLoginName());
    if (admin.config_value) {
      this.get('clusterInfo').pushObject(Ember.Object.create(admin));
    }

    // cluster name
    var cluster = this.rawContent.findProperty('config_name', 'cluster');
    cluster.config_value = this.get('content.cluster.name');
    console.log("STEP8: the value of content cluster name: " + this.get('content.cluster.name'));
    this.get('clusterInfo').pushObject(Ember.Object.create(cluster));

    //hosts
    var newHostsCount = 0;
    var totalHostsCount = 0;
    var hosts = this.get('content.hosts');
    for (var hostName in hosts) {
      newHostsCount += ~~(!hosts[hostName].isInstalled);
      totalHostsCount++;
    }

    var totalHostsObj = this.rawContent.findProperty('config_name', 'hosts');
    totalHostsObj.config_value = totalHostsCount + ' (' + newHostsCount + ' new)';
    this.get('clusterInfo').pushObject(Em.Object.create(totalHostsObj));

    //repo
    if (['addHostController', 'addServiceController'].contains(this.get('content.controllerName'))) {
      this.loadRepoInfo();
    } else {
      // from install wizard
      var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
      var allRepos = [];
      if (selectedStack && selectedStack.operatingSystems) {
        selectedStack.operatingSystems.forEach(function (os) {
          if (os.selected) {
            allRepos.push(Em.Object.create({
              base_url: os.baseUrl,
              os_type: os.osType,
              repo_id: os.repoId
            }));
          }
        }, this);
      }
      allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
      this.get('clusterInfo').set('repoInfo', allRepos);
    }
  },

  /**
   * Load repo info for add Service/Host wizard review page
   * @return {$.ajax|null}
   * @method loadRepoInfo
   */
  loadRepoInfo: function () {
    var nameVersionCombo = App.get('currentStackVersion').split('-');
    return App.ajax.send({
      name: 'cluster.load_repositories',
      sender: this,
      data: {
        stackName: nameVersionCombo[0],
        stackVersion: nameVersionCombo[1]
      },
      success: 'loadRepoInfoSuccessCallback',
      error: 'loadRepoInfoErrorCallback'
    });
  },

  /**
   * Save all repo base URL of all OS type to <code>repoInfo<code>
   * @param {object} data
   * @method loadRepoInfoSuccessCallback
   */
  loadRepoInfoSuccessCallback: function (data) {
    var allRepos = [];
    data.items.forEach(function (os) {
      if (!App.get('supports.ubuntu') && os.OperatingSystems.os_type == 'debian12') return; // @todo: remove after Ubuntu support confirmation
      os.repositories.forEach(function (repository) {
        allRepos.push(Em.Object.create({
          base_url: repository.Repositories.base_url,
          os_type: repository.Repositories.os_type,
          repo_id: repository.Repositories.repo_id
        }));
      });
    }, this);
    allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
    this.get('clusterInfo').set('repoInfo', allRepos);
  },

  /**
   * @param {object} request
   * @method loadRepoInfoErrorCallback
   */
  loadRepoInfoErrorCallback: function (request) {
    console.log('Error message is: ' + request.responseText);
    var allRepos = [];
    allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
    this.get('clusterInfo').set('repoInfo', allRepos);
  },

  /**
   * Load all info about services to <code>services</code> variable
   * @method loadServices
   */
  loadServices: function () {
    this.get('selectedServices').filterProperty('isHiddenOnSelectServicePage', false).forEach(function (service) {
      console.log('INFO: step8: Name of the service from getService function: ' + service.get('serviceName'));
      var serviceObj = Em.Object.create({
        service_name: service.get('serviceName'),
        display_name: service.get('displayNameOnSelectServicePage'),
        service_components: Em.A([])
      });
      service.get('serviceComponents').forEach(function (component) {
        // show clients for services that have only clients components
        if ((component.get('isClient') || component.get('isClientBehavior')) && !service.get('isClientOnlyService')) return;
        // skip components that was hide on assign master page
        if (component.get('isMaster') && !component.get('isShownOnInstallerAssignMasterPage')) return;
        // no HA component
        if (App.get('isHaEnabled') && component.get('isHAComponentOnly')) return;
        var displayName;
        if (component.get('isClient')) {
          displayName = Em.I18n.t('common.clients')
        } else {
          // remove service name from component display name
          displayName = component.get('displayName').replace(new RegExp('^' + service.get('serviceName') + '\\s', 'i'), '');
        }
        serviceObj.get('service_components').pushObject(Em.Object.create({
          component_name: component.get('isClient') ? Em.I18n.t('common.client').toUpperCase() : component.get('componentName'),
          display_name: displayName,
          component_value: this.assignComponentHosts(component)
        }));
      }, this);
      if (service.get('customReviewHandler')) {
        for (var displayName in service.get('customReviewHandler')) {
          serviceObj.get('service_components').pushObject(Em.Object.create({
            display_name: displayName,
            component_value: this.assignComponentHosts(Em.Object.create({
              customHandler: service.get('customReviewHandler.' + displayName)
            }))
          }));
        }
      }
      this.get('services').pushObject(serviceObj);
    }, this);
  },

  /**
   * Set <code>component_value</code> property to <code>component</code>
   * @param {Em.Object} component
   * @return {String}
   * @method assignComponentHosts
   */
  assignComponentHosts: function (component) {
    var componentValue;
    if (component.get('customHandler')) {
      componentValue = this[component.get('customHandler')].call(this, component);
    }
    else {
      if (component.get('isMaster') || component.get('isMasterBehavior')) {
        componentValue = this.getMasterComponentValue(component.get('componentName'));
      }
      else {
        console.log(' --- ---INFO: step8: NOT component isMaster');
        var hostsLength = this.get('content.slaveComponentHosts')
          .findProperty('componentName', component.get('isClient') ? Em.I18n.t('common.client').toUpperCase() : component.get('componentName'))
          .hosts.length;
        componentValue = hostsLength + Em.I18n.t('installer.step8.host' + ((hostsLength > 1) ? 's' : ''));
      }
    }
    return componentValue;
  },

  getMasterComponentValue: function (componentName) {
    var masterComponents = this.get('content.masterComponentHosts');
    var hostsCount = masterComponents.filterProperty('component', componentName).length;
    return stringUtils.pluralize(hostsCount,
      masterComponents.findProperty('component', componentName).hostName,
      hostsCount + ' ' + Em.I18n.t('installer.step8.hosts'));
  },

  /**
   * Set dispalyed Hive DB value based on DB type
   * @param {Ember.Object} dbComponent
   * @method loadHiveDbValue
   */
  loadHiveDbValue: function () {
    var db,
      serviceConfigPreoprties = this.get('wizardController').getDBProperty('serviceConfigProperties'),
      hiveDb = serviceConfigPreoprties.findProperty('name', 'hive_database');
    if (hiveDb.value === 'New MySQL Database') {
      return 'MySQL (New Database)';
    }
    else {
      if (hiveDb.value === 'Existing MySQL Database') {
        db = serviceConfigPreoprties.findProperty('name', 'hive_existing_mysql_database');
        return db.value + ' (' + hiveDb.value + ')';
      }
      else {
        if (hiveDb.value === Em.I18n.t('services.service.config.hive.oozie.postgresql')) {
          db = serviceConfigPreoprties.findProperty('name', 'hive_existing_postgresql_database');
          return db.value + ' (' + hiveDb.value + ')';
        }
        else { // existing oracle database
          db = serviceConfigPreoprties.findProperty('name', 'hive_existing_oracle_database');
          return db.value + ' (' + hiveDb.value + ')';
        }
      }
    }
  },

  /**
   * Set displayed HBase master value
   * @param {Object} hbaseMaster
   * @method loadHbaseMasterValue
   */
  loadHbaseMasterValue: function (hbaseMaster) {
    var hbaseHostName = this.get('content.masterComponentHosts').filterProperty('component', hbaseMaster.component_name);
    if (hbaseHostName.length == 1) {
      hbaseMaster.set('component_value', hbaseHostName[0].hostName);
    } else {
      hbaseMaster.set('component_value', hbaseHostName[0].hostName + " " + Em.I18n.t('installer.step8.other').format(hbaseHostName.length - 1));
    }
  },

  /**
   * Set displayed ZooKeeper Server value
   * @param {Object} serverComponent
   * @method loadZkServerValue
   */
  loadZkServerValue: function (serverComponent) {
    var zkHostNames = this.get('content.masterComponentHosts').filterProperty('component', serverComponent.component_name).length;
    var hostSuffix;
    if (zkHostNames === 1) {
      hostSuffix = Em.I18n.t('installer.step8.host');
    } else {
      hostSuffix = Em.I18n.t('installer.step8.hosts');
    }
    serverComponent.set('component_value', zkHostNames + hostSuffix);
  },

  /**
   * Set displayed Oozie DB value based on DB type
   * @param {Object} dbComponent
   * @method loadOozieDbValue
   */
  loadOozieDbValue: function () {
    var db, oozieDb = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_database');
    if (oozieDb.value === 'New Derby Database') {
      db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_derby_database');
      return db.value + ' (' + oozieDb.value + ')';
    }
    else {
      if (oozieDb.value === 'Existing MySQL Database') {
        db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_existing_mysql_database');
        return db.value + ' (' + oozieDb.value + ')';
      }
      else {
        if (oozieDb.value === Em.I18n.t('services.service.config.hive.oozie.postgresql')) {
          db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_existing_postgresql_database');
          return db.value + ' (' + oozieDb.value + ')';
        }
        else { // existing oracle database
          db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_existing_oracle_database');
          return db.value + ' (' + oozieDb.value + ')';
        }
      }
    }
  },

  /**
   * Set displayed Nagion Admin value
   * @param {Object} nagiosAdmin
   * @method loadNagiosAdminValue
   */
  loadNagiosAdminValue: function () {
    var config = this.get('content.serviceConfigProperties');
    var adminLoginName = config.findProperty('name', 'nagios_web_login');
    var adminEmail = config.findProperty('name', 'nagios_contact');
    return adminLoginName.value + ' / (' + adminEmail.value + ')';
  },

  /**
   * Onclick handler for <code>next</code> button
   * @method submit
   * @return {App.ModalPopup|null}
   */
  submit: function () {
    if (this.get('isSubmitDisabled')) return null;
    if ((this.get('content.controllerName') == 'addHostController') && this.get('securityEnabled')) {
      var self = this;
      return App.showConfirmationPopup(function () {
        self.submitProceed();
      }, Em.I18n.t('installer.step8.securityConfirmationPopupBody'));
    }
    else {
      return this.submitProceed();
    }
  },
  /**
   * Update configurations for installed services.
   * Do separated PUT-request for each siteName for each service
   *
   * @param {Array} fileNamesToUpdate - file names that should be updated
   * @method updateConfigurations
   */
  updateConfigurations: function (fileNamesToUpdate) {
    var configurationController = App.router.get('mainServiceInfoConfigsController');
    var configs = this.get('configs').slice(0);
    var configsMap = [];

    fileNamesToUpdate.forEach(function (fileName) {
      if (!fileName || /^(core)/.test(fileName)) return;
      var tagName = 'version' + (new Date).getTime();
      var configsToSave = configs.filterProperty('filename', fileName);
      configsToSave.forEach(function (item) {
        item.value = App.config.trimProperty(item, false);
      });
      configsMap.push(configurationController.createSiteObj(fileName.replace(".xml", ""), tagName, configsToSave));
    }, this);

    if (!configsMap.length) return;
    var configData = configsMap.map(function (siteConfigObject) {
      return JSON.stringify({
        Clusters: {
          desired_config: {
            type: siteConfigObject.type,
            tag: siteConfigObject.tag,
            properties: siteConfigObject.properties,
            properties_attributes: siteConfigObject.properties_attributes
          }
        }
      });
    }, this).toString();
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.apply_configuration_to_cluster',
      data: {
        data: '[' + configData + ']'
      }
    });
  },
  /**
   * Prepare <code>ajaxQueue</code> and start to execute it
   * @method submitProceed
   */
  submitProceed: function () {
    var self = this;
    this.set('isSubmitDisabled', true);
    this.set('isBackBtnDisabled', true);
    if (this.get('content.controllerName') == 'addHostController') {
      App.router.get('addHostController').setLowerStepsDisable(4);
    }

    // checkpoint the cluster status on the server so that the user can resume from where they left off
    switch (this.get('content.controllerName')) {
      case 'installerController':
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'CLUSTER_DEPLOY_PREP_2',
          wizardControllerName: this.get('content.controllerName'),
          localdb: App.db.data
        });
        break;
      case 'addHostController':
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_HOSTS_DEPLOY_PREP_2',
          wizardControllerName: this.get('content.controllerName'),
          localdb: App.db.data
        });
        break;
      case 'addServiceController':
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SERVICES_DEPLOY_PREP_2',
          wizardControllerName: this.get('content.controllerName'),
          localdb: App.db.data
        });
        break;
      default:
        break;
    }
    // delete any existing clusters to start from a clean slate
    // before creating a new cluster in install wizard
    // TODO: modify for multi-cluster support
    this.getExistingClusterNames().complete(function () {
      var clusterNames = self.get('clusterNames');
      if (self.get('content.controllerName') == 'installerController' && (!App.get('testMode')) && clusterNames.length) {
        self.deleteClusters(clusterNames);
      } else {
        self.deleteClustersCallback(null, null, {isLast: true});
      }
    });
  },

  /**
   * Get list of existing cluster names
   * @returns {object|null}
   * returns an array of existing cluster names.
   * returns an empty array if there are no existing clusters.
   * @method getExistingClusterNames
   */
  getExistingClusterNames: function () {
    return App.ajax.send({
      name: 'wizard.step8.existing_cluster_names',
      sender: this,
      success: 'getExistingClusterNamesSuccessCallBack',
      error: 'getExistingClusterNamesErrorCallback'
    });
  },

  /**
   * Save received list to <code>clusterNames</code>
   * @param {Object} data
   * @method getExistingClusterNamesSuccessCallBack
   */
  getExistingClusterNamesSuccessCallBack: function (data) {
    var clusterNames = data.items.mapProperty('Clusters.cluster_name');
    console.log("Got existing cluster names: " + clusterNames);
    this.set('clusterNames', clusterNames);
  },

  /**
   * If error appears, set <code>clusterNames</code> to <code>[]</code>
   * @method getExistingClusterNamesErrorCallback
   */
  getExistingClusterNamesErrorCallback: function () {
    console.log("Failed to get existing cluster names");
    this.set('clusterNames', []);
  },

  /**
   * Delete cluster by name
   * One request for one cluster!
   * @param {string[]} clusterNames
   * @method deleteClusters
   */
  deleteClusters: function (clusterNames) {
    clusterNames.forEach(function (clusterName, index) {
      App.ajax.send({
        name: 'common.delete.cluster',
        sender: this,
        data: {
          name: clusterName,
          isLast: index == clusterNames.length - 1
        },
        success: 'deleteClustersCallback',
        error: 'deleteClustersCallback'
      });
    }, this);

  },

  deleteClustersCallback: function (response, request, data) {
    if (data.isLast) {
      this.setLocalRepositories();
      this.createCluster();
      this.createSelectedServices();
      if (this.get('content.controllerName') !== 'addHostController') {
        if (this.get('wizardController').getDBProperty('fileNamesToUpdate') && this.get('wizardController').getDBProperty('fileNamesToUpdate').length) {
          this.updateConfigurations(this.get('wizardController').getDBProperty('fileNamesToUpdate'));
        }
        this.createConfigurations();
        this.applyConfigurationsToCluster();
      }
      this.createComponents();
      this.registerHostsToCluster();
      if (App.get('supports.hostOverridesInstaller')) {
        this.createConfigurationGroups();
      }
      this.createMasterHostComponents();
      this.createSlaveAndClientsHostComponents();
      this.createAdditionalHostComponents();

      this.set('ajaxQueueLength', this.get('ajaxRequestsQueue.queue.length'));
      this.get('ajaxRequestsQueue').start();
    }
  },

  /**
   * Updates local repositories for the Ambari server.
   * @method setLocalRepositories
   * @return {bool} true - requests are sent, false - requests not sent
   */
  setLocalRepositories: function () {
    if (this.get('content.controllerName') !== 'installerController' || !App.get('supports.localRepositories')) return false;
    var self = this,
      stack = this.get('content.stacks').findProperty('isSelected', true);
    stack.operatingSystems.forEach(function (os) {
      if (os.baseUrl !== os.originalBaseUrl) {
        console.log("Updating local repository URL from " + os.originalBaseUrl + " -> " + os.baseUrl + ". ", os);
        self.addRequestToAjaxQueue({
          name: 'wizard.step8.set_local_repos',
          data: {
            osType: os.osType,
            repoId: os.repoId,
            stackVersionURL: App.get('stackVersionURL'),
            data: JSON.stringify({
              "Repositories": {
                "base_url": os.baseUrl,
                "verify_base_url": false
              }
            })
          }
        });
      }
    });
    return true;
  },


  /**
   * *******************************************************************
   * The following create* functions are called upon submitting Step 8.
   * *******************************************************************
   */

  /**
   * Create cluster using selected stack version
   * Queued request
   * @method createCluster
   */
  createCluster: function () {
    if (this.get('content.controllerName') !== 'installerController') return;
    var stackVersion = (this.get('content.installOptions.localRepo')) ? App.currentStackVersion.replace(/(-\d+(\.\d)*)/ig, "Local$&") : App.currentStackVersion;
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_cluster',
      data: {
        data: JSON.stringify({ "Clusters": {"version": stackVersion }})
      },
      success: 'createClusterSuccess'
    });
  },

  createClusterSuccess: function (data, xhr, params) {
    App.set('clusterName', params.cluster)
  },

  /**
   * Create selected to install services
   * Queued request
   * Skipped if no services where selected!
   * @method createSelectedServices
   */
  createSelectedServices: function () {
    var data = this.createSelectedServicesData();
    if (!data.length) return;
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_selected_services',
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * Format data for <code>createSelectedServices</code> request
   * @returns {Object[]}
   * @method createSelectedServicesData
   */
  createSelectedServicesData: function () {
    return this.get('selectedServices').map(function (_service) {
      return {"ServiceInfo": { "service_name": _service.get('serviceName') }};
    });
  },

  /**
   * Create components for selected services
   * Queued requests
   * One request for each service!
   * @method createComponents
   */
  createComponents: function () {
    var serviceComponents = App.StackServiceComponent.find();
    this.get('selectedServices').forEach(function (_service) {
      var serviceName = _service.get('serviceName');
      var componentsData = serviceComponents.filterProperty('serviceName', serviceName).map(function (_component) {
        return { "ServiceComponentInfo": { "component_name": _component.get('componentName') } };
      });

      // Service must be specified in terms of a query for creating multiple components at the same time.
      // See AMBARI-1018.
      this.addRequestToAjaxQueue({
        name: 'wizard.step8.create_components',
        data: {
          data: JSON.stringify({"components": componentsData}),
          serviceName: serviceName
        }
      });
    }, this);
  },

  /**
   * Error callback for new service component request
   * So, if component doesn't exist we should create it
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method newServiceComponentErrorCallback
   */
  newServiceComponentErrorCallback: function (request, ajaxOptions, error, opt, params) {
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_components',
      data: {
        serviceName: params.serviceName,
        data: JSON.stringify({
          "components": [
            {
              "ServiceComponentInfo": {
                "component_name": params.componentName
              }
            }
          ]
        })
      }
    });
  },

  /**
   * Register hosts
   * Queued request
   * @method registerHostsToCluster
   */
  registerHostsToCluster: function () {
    var data = this.createRegisterHostData();
    if (!data.length) return;
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.register_host_to_cluster',
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * Format request-data for <code>registerHostsToCluster</code>
   * @returns {Object}
   * @method createRegisterHostData
   */
  createRegisterHostData: function () {
    return this.getRegisteredHosts().filterProperty('isInstalled', false).map(function (host) {
      return {"Hosts": { "host_name": host.hostName}};
    });
  },

  /**
   * Register new master components
   * @uses registerHostsToComponent
   * @method createMasterHostComponents
   */
  createMasterHostComponents: function () {
    // create master components for only selected services.
    var selectedMasterComponents = this.get('content.masterComponentHosts').filter(function (_component) {
      return this.get('selectedServices').mapProperty('serviceName').contains(_component.serviceId)
    }, this);
    selectedMasterComponents.mapProperty('component').uniq().forEach(function (component) {
      var hostNames = selectedMasterComponents.filterProperty('component', component).filterProperty('isInstalled', false).mapProperty('hostName');
      this.registerHostsToComponent(hostNames, component);
    }, this);
  },

  getClientsToMasterMap: function () {
    var clientNames = App.StackServiceComponent.find().filterProperty('isClient').mapProperty('componentName'),
      clientsMap = {},
      dependedComponents = App.StackServiceComponent.find().filterProperty('isMaster');
    clientNames.forEach(function (clientName) {
      clientsMap[clientName] = Em.A([]);
      dependedComponents.forEach(function (component) {
        if (component.get('dependencies').contains(clientName)) clientsMap[clientName].push(component.get('componentName'));
      });
      if (!clientsMap[clientName].length) delete clientsMap[clientName];
    });
    return clientsMap;
  },
  /**
   * Register slave components and clients
   * @uses registerHostsToComponent
   * @method createSlaveAndClientsHostComponents
   */
  createSlaveAndClientsHostComponents: function () {
    var masterHosts = this.get('content.masterComponentHosts'),
      slaveHosts = this.get('content.slaveComponentHosts'),
      clients = this.get('content.clients');

    /**
     * Determines on which hosts client should be installed (based on availability of master components on hosts)
     * @type {Object}
     * Format:
     * <code>
     *  {
     *    CLIENT1: Em.A([MASTER1, MASTER2, ...]),
     *    CLIENT2: Em.A([MASTER3, MASTER1, ...])
     *    ...
     *  }
     * </code>
     */
    var clientsToMasterMap = this.getClientsToMasterMap();

    slaveHosts.forEach(function (_slave) {
      if (_slave.componentName !== 'CLIENT') {
        var hostNames = _slave.hosts.filterProperty('isInstalled', false).mapProperty('hostName');
        this.registerHostsToComponent(hostNames, _slave.componentName);
      }
      else {
        clients.forEach(function (_client) {

          var hostNames = _slave.hosts.mapProperty('hostName');
          if (clientsToMasterMap[_client.component_name]) {
            clientsToMasterMap[_client.component_name].forEach(function (componentName) {
              masterHosts.filterProperty('component', componentName).filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              });
            });
          }
          hostNames = hostNames.uniq();

          if (_client.isInstalled) {
            /**
             * check whether clients are already installed on selected master hosts!!!
             */
            var clientHosts = [];
            var installedHosts = this.get('content.hosts');
            for (var hostName in installedHosts) {
              if (installedHosts[hostName].isInstalled &&
                installedHosts[hostName].hostComponents.filterProperty('HostRoles.state', 'INSTALLED').mapProperty('HostRoles.component_name').contains(_client.component_name)) {
                clientHosts.push(hostName);
              }
            }

            if (clientHosts.length > 0) {
              clientHosts.forEach(function (hostName) {
                if (hostNames.contains(hostName)) {
                  hostNames.splice(hostNames.indexOf(hostName), 1);
                }
              }, this);
            }
            /**
             * For Add Service Only
             * if client is not added to host or is not installed add Object
             * {
             *    componentName: {String},
             *    hostName: {String}
             * }
             * to content.additionalClients
             * later it will be used to install client on host before installing new services
             */
            if (this.get('content.controllerName') === 'addServiceController' && hostNames.length > 0) {
              hostNames.forEach(function (hostName) {
                this.get('content.additionalClients').push(Em.Object.create({
                  componentName: _client.component_name, hostName: hostName
                }))
              }, this)

            }
          }

          this.registerHostsToComponent(hostNames, _client.component_name);

        }, this);
      }
    }, this);
  },

  /**
   * Register additional components
   * Based on availability of some services
   * @uses registerHostsToComponent
   * @method createAdditionalHostComponents
   */
  createAdditionalHostComponents: function () {
    var masterHosts = this.get('content.masterComponentHosts');
    // add MySQL Server if Hive is selected
    // add Ganglia Monitor (Slave) to all hosts if Ganglia service is selected
    var gangliaService = this.get('content.services').filterProperty('isSelected').findProperty('serviceName', 'GANGLIA');
    if (gangliaService) {
      var hosts = this.getRegisteredHosts();
      if (gangliaService.get('isInstalled')) {
        hosts = hosts.filterProperty('isInstalled', false);
      }
      if (hosts.length) {
        this.registerHostsToComponent(hosts.mapProperty('hostName'), 'GANGLIA_MONITOR');
      }
    }
    var hiveService = this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).findProperty('serviceName', 'HIVE');
    if (hiveService) {
      var hiveDb = this.get('content.serviceConfigProperties').findProperty('name', 'hive_database');
      if (hiveDb.value == "New MySQL Database") {
        this.registerHostsToComponent(masterHosts.filterProperty('component', 'HIVE_SERVER').mapProperty('hostName'), 'MYSQL_SERVER');
      }
    }
  },

  /**
   * Register component to hosts
   * Queued request
   * @param {String[]} hostNames
   * @param {String} componentName
   * @method registerHostsToComponent
   */
  registerHostsToComponent: function (hostNames, componentName) {
    if (!hostNames.length) return;

    var queryStr = '';
    hostNames.forEach(function (hostName) {
      queryStr += 'Hosts/host_name=' + hostName + '|';
    });
    //slice off last symbol '|'
    queryStr = queryStr.slice(0, -1);

    var data = {
      "RequestInfo": {
        "query": queryStr
      },
      "Body": {
        "host_components": [
          {
            "HostRoles": {
              "component_name": componentName
            }
          }
        ]
      }
    };

    this.addRequestToAjaxQueue({
      name: 'wizard.step8.register_host_to_component',
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * Compare generated config object with current configs that were filled
   * on "Customize Services" page.
   *
   * @param {Object} properties - generated by createSiteObj|createCoreSiteObj
   * @param {Array} configs - current configs to compare
   * @return {Boolean}
   * @method isConfigsChanged
   **/
  isConfigsChanged: function (properties, configs) {
    var isChanged = false;
    for (var property in properties) {
      var config = configs.findProperty('name', property);
      // if config not found then it's looks like a new config
      if (!config) {
        isChanged = true;
      } else {
        if (!config.hasInitialValue || config.isNotDefaultValue) {
          isChanged = true;
        }
      }
    }
    return isChanged;
  },
  /**
   * Create config objects for cluster and services
   * @method createConfigurations
   */
  createConfigurations: function () {
    var selectedServices = this.get('selectedServices');
    var coreSiteObject = this.createCoreSiteObj();
    var tag = 'version1';

    if (this.get('content.controllerName') == 'addServiceController') {
      tag = 'version' + (new Date).getTime();
      coreSiteObject.tag = tag;
      var coreSiteConfigs = this.get('configs').filterProperty('filename', 'core-site.xml');
      if (this.isConfigsChanged(coreSiteObject.properties, coreSiteConfigs))
        this.get('serviceConfigTags').pushObject(coreSiteObject);
    }

    selectedServices.forEach(function (service) {
      Object.keys(service.get('configTypes')).forEach(function (type) {
        if (!this.get('serviceConfigTags').someProperty('type', type)) {
          if (!App.supports.capacitySchedulerUi && service.get('serviceName') === 'MAPREDUCE' && (type === 'capacity-scheduler' || type === 'mapred-queue-acls')) {
            return;
          } else if (type === 'core-site') {
            this.get('serviceConfigTags').pushObject(coreSiteObject);
          } else if (type === 'storm-site') {
            this.get('serviceConfigTags').pushObject(this.createStormSiteObj(tag));
          } else if (type === 'zoo.cfg') {
            this.get('serviceConfigTags').pushObject(this.createZooCfgObj(tag));
          } else {
            var isNonXmlFile = type.endsWith('log4j') || type.endsWith('env') || type.endsWith('properties') || type.endsWith('conf');
            this.get('serviceConfigTags').pushObject(this.createSiteObj(type, isNonXmlFile, tag));
          }
        }
      }, this);
    }, this);
  },

  /**
   * Send <code>serviceConfigTags</code> to server
   * Queued request
   * One request for each service config tag
   * @method applyConfigurationsToCluster
   */
  applyConfigurationsToCluster: function () {
    var configData = this.get('serviceConfigTags').map(function (_serviceConfig) {
      return JSON.stringify({
        Clusters: {
          desired_config: {
            type: _serviceConfig.type,
            tag: _serviceConfig.tag,
            properties: _serviceConfig.properties,
            properties_attributes: _serviceConfig.properties_attributes
          }
        }
      });
    }, this).toString();
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.apply_configuration_to_cluster',
      data: {
        data: '[' + configData + ']'
      }
    });
  },

  /**
   * Create and update config groups
   * @method createConfigurationGroups
   */
  createConfigurationGroups: function () {
    var configGroups = this.get('content.configGroups').filterProperty('isDefault', false);
    var clusterName = this.get('clusterName');
    var sendData = [];
    var updateData = [];
    var serviceConfigController = App.router.get('mainServiceInfoConfigsController');
    var timeTag = (new Date).getTime();
    var groupsToDelete = App.router.get(this.get('content.controllerName')).getDBProperty('groupsToDelete');
    if (groupsToDelete && groupsToDelete.length > 0) {
      this.removeInstalledServicesConfigurationGroups(groupsToDelete);
    }
    configGroups.forEach(function (configGroup) {
      var groupConfigs = [];
      var groupData = {
        "cluster_name": clusterName,
        "group_name": configGroup.name,
        "tag": configGroup.service.id,
        "description": configGroup.description,
        "hosts": [],
        "desired_configs": []
      };
      configGroup.hosts.forEach(function (hostName) {
        groupData.hosts.push({"host_name": hostName});
      });
      //wrap properties into Em.Object to make them compatible with buildGroupDesiredConfigs method
      configGroup.properties.forEach(function (property) {
        groupConfigs.push(Em.Object.create(property));
      });
      groupData.desired_configs = serviceConfigController.buildGroupDesiredConfigs.call(serviceConfigController, groupConfigs, timeTag);
      // check for group from installed service
      if (configGroup.isForUpdate === true) {
        // if group is a new one, create it
        if (!configGroup.id) {
          sendData.push({"ConfigGroup": groupData});
        } else {
          // update an existing group
          groupData.id = configGroup.id;
          updateData.push({"ConfigGroup": groupData});
        }
      } else {
        sendData.push({"ConfigGroup": groupData});
      }
      //each group should have unique tag to prevent overriding configs from common sites
      timeTag++;
    }, this);
    if (sendData.length > 0) {
      this.applyConfigurationGroups(sendData);
    }
    if (updateData.length > 0) {
      this.applyInstalledServicesConfigurationGroup(updateData);
    }
  },

  /**
   * Create new config groups request
   * Queued request
   * @param {Object[]} sendData
   * @method applyConfigurationGroups
   */
  applyConfigurationGroups: function (sendData) {
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.apply_configuration_groups',
      data: {
        data: JSON.stringify(sendData)
      }
    });
  },

  /**
   * Update existed config groups
   * Separated request for each group
   * @param {Object[]} updateData
   * @method applyInstalledServicesConfigurationGroup
   */
  applyInstalledServicesConfigurationGroup: function (updateData) {
    updateData.forEach(function (item) {
      App.router.get('mainServiceInfoConfigsController').putConfigGroupChanges(item);
    });
  },

  /**
   * Delete selected config groups
   * @param {Object[]} groupsToDelete
   * @method removeInstalledServicesConfigurationGroups
   */
  removeInstalledServicesConfigurationGroups: function (groupsToDelete) {
    groupsToDelete.forEach(function (item) {
      App.config.deleteConfigGroup(Em.Object.create(item));
    });
  },

  /**
   * Create Core Site object
   * @returns {{type: string, tag: string, properties: {}}}
   * @method createCoreSiteObj
   */
  createCoreSiteObj: function () {
    var installedAndSelectedServices = Em.A([]);
    installedAndSelectedServices.pushObjects(this.get('installedServices'));
    installedAndSelectedServices.pushObjects(this.get('selectedServices'));
    var coreSiteObj = this.get('configs').filterProperty('filename', 'core-site.xml'),
      coreSiteProperties = {},
    // some configs needs to be skipped if services are not selected
      isOozieSelected = installedAndSelectedServices.someProperty('serviceName', 'OOZIE'),
      oozieUser = this.get('configs').someProperty('name', 'oozie_user') ? this.get('configs').findProperty('name', 'oozie_user').value : null,
      isHiveSelected = installedAndSelectedServices.someProperty('serviceName', 'HIVE'),
      hiveUser = this.get('configs').someProperty('name', 'hive_user') ? this.get('configs').findProperty('name', 'hive_user').value : null,
      isHcatSelected = installedAndSelectedServices.someProperty('serviceName', 'WEBHCAT'),
      hcatUser = this.get('configs').someProperty('name', 'hcat_user') ? this.get('configs').findProperty('name', 'hcat_user').value : null,
      isGLUSTERFSSelected = installedAndSelectedServices.someProperty('serviceName', 'GLUSTERFS');

    // screen out the GLUSTERFS-specific core-site.xml entries when they are not needed
    if (!isGLUSTERFSSelected) {
      coreSiteObj = coreSiteObj.filter(function (_config) {
        return !_config.name.contains("fs.glusterfs");
      });
    }

    coreSiteObj.forEach(function (_coreSiteObj) {
      // exclude some configs if service wasn't selected
      if (
        (isOozieSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.groups')) &&
          (isHiveSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.groups')) &&
          (isHcatSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.groups'))) {
        coreSiteProperties[_coreSiteObj.name] = App.config.escapeXMLCharacters(_coreSiteObj.value);
      }
      if (isGLUSTERFSSelected && _coreSiteObj.name == "fs.default.name") {
        coreSiteProperties[_coreSiteObj.name] =
          this.get('configs').someProperty('name', 'fs_glusterfs_default_name') ?
            App.config.escapeXMLCharacters(this.get('configs').findProperty('name', 'fs_glusterfs_default_name').value) : null;
      }
      if (isGLUSTERFSSelected && _coreSiteObj.name == "fs.defaultFS") {
        coreSiteProperties[_coreSiteObj.name] =
          this.get('configs').someProperty('name', 'glusterfs_defaultFS_name') ?
            App.config.escapeXMLCharacters(this.get('configs').findProperty('name', 'glusterfs_defaultFS_name').value) : null;
      }
    }, this);
    var attributes = App.router.get('mainServiceInfoConfigsController').getConfigAttributes(coreSiteObj);
    var configObj = {"type": "core-site", "tag": "version1", "properties": coreSiteProperties};
    if (attributes) {
      configObj['properties_attributes'] = attributes;
    }
    return  configObj;
  },

  /**
   * Create siteObj for custom service with it own configs
   * @param {string} site
   * @param {bool} isNonXmlFile
   * @param tag
   * @returns {{type: string, tag: string, properties: {}}}
   * @method createSiteObj
   */
  createSiteObj: function (site, isNonXmlFile, tag) {
    var properties = {};
    var configs = this.get('configs').filterProperty('filename', site + '.xml');
    var attributes = App.router.get('mainServiceInfoConfigsController').getConfigAttributes(configs);
    configs.forEach(function (_configProperty) {
      if (isNonXmlFile) {
        var heapsizeExceptions = ['hadoop_heapsize', 'yarn_heapsize', 'nodemanager_heapsize', 'resourcemanager_heapsize', 'apptimelineserver_heapsize', 'jobhistory_heapsize'];
        // do not pass any globals whose name ends with _host or _hosts
        if (_configProperty.isRequiredByAgent !== false) {
          // append "m" to JVM memory options except for heapsizeExtensions
          if (/_heapsize|_newsize|_maxnewsize$/.test(_configProperty.name) && !heapsizeExceptions.contains(_configProperty.name)) {
            properties[_configProperty.name] = _configProperty.value + "m";
          } else {
            properties[_configProperty.name] = _configProperty.value;
          }
        }
      } else {
        properties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      }
    }, this);
    var configObj = {"type": site, "tag": tag, "properties": properties };
    if (attributes) {
      configObj['properties_attributes'] = attributes;
    }
    return configObj;
  },

  /**
   * Create ZooKeeper Cfg Object
   * @param tag
   * @returns {{type: string, tag: string, properties: {}}}
   * @method createZooCfgObj
   */
  createZooCfgObj: function (tag) {
    var configs = this.get('configs').filterProperty('filename', 'zoo.cfg');
    var csProperties = {};
    configs.forEach(function (_configProperty) {
      csProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'zoo.cfg', tag: tag, properties: csProperties};
  },
  /**
   * Create site obj for Storm
   * Some config-properties should be modified in custom way
   * @param tag
   * @returns {{type: string, tag: string, properties: {}}}
   * @method createStormSiteObj
   */
  createStormSiteObj: function (tag) {
    var configs = this.get('configs').filterProperty('filename', 'storm-site.xml');
    var stormProperties = {};
    var specialProperties = ["storm.zookeeper.servers", "nimbus.childopts", "supervisor.childopts", "worker.childopts"];
    configs.forEach(function (_configProperty) {
      if (specialProperties.contains(_configProperty.name)) {
        if (_configProperty.name == "storm.zookeeper.servers") {
          stormProperties[_configProperty.name] = JSON.stringify(_configProperty.value).replace(/"/g, "'");
        } else {
          stormProperties[_configProperty.name] = JSON.stringify(_configProperty.value).replace(/"/g, "");
        }
      } else {
        stormProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      }
    }, this);
    return {type: 'storm-site', tag: tag, properties: stormProperties};
  },

  /**
   * Navigate to next step after all requests are sent
   * @method ajaxQueueFinished
   */
  ajaxQueueFinished: function () {
    console.log('everything is loaded');
    App.router.send('next');
  },

  /**
   * We need to do a lot of ajax calls async in special order. To do this,
   * generate array of ajax objects and then send requests step by step. All
   * ajax objects are stored in <code>ajaxRequestsQueue</code>
   *
   * @param {Object} params object with ajax-request parameters like url, type, data etc
   * @method addRequestToAjaxQueue
   */
  addRequestToAjaxQueue: function (params) {
    if (App.get('testMode')) return;

    params = jQuery.extend({
      sender: this,
      error: 'ajaxQueueRequestErrorCallback'
    }, params);
    params.data['cluster'] = this.get('clusterName');

    this.get('ajaxRequestsQueue').addRequest(params);
  },

  /**
   * Error callback for each queued ajax-request
   * @param {object} xhr
   * @param {string} status
   * @param {string} error
   * @method ajaxQueueRequestErrorCallback
   */
  ajaxQueueRequestErrorCallback: function (xhr, status, error) {
    var responseText = JSON.parse(xhr.responseText);
    var controller = App.router.get(App.clusterStatus.wizardControllerName);
    controller.registerErrPopup(Em.I18n.t('common.error'), responseText.message);
    this.set('hasErrorOccurred', true);
    // an error will break the ajax call chain and allow submission again
    this.set('isSubmitDisabled', false);
    this.set('isBackBtnDisabled', false);
    App.router.get(this.get('content.controllerName')).setStepsEnable();
  }

});
