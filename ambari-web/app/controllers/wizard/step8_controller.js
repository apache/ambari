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
  rawContent: require('data/review_configs'),
  totalHosts: [],
  clusterInfo: [],
  services: [],
  configs: [],
  globals: [],
  ajaxQueue: [],
  configMapping: function(){
    return App.config.get('configMapping').all(true);
  }.property('App.config.configMapping'),

  slaveComponentConfig: null,
  isSubmitDisabled: false,
  isBackBtnDisabled: false,
  hasErrorOccurred: false,
  servicesInstalled: false,
  serviceConfigTags: [],
  securityEnabled: function() {
    return App.router.get('mainAdminSecurityController.securityEnabled');
  }.property('App.router.mainAdminSecurityController.securityEnabled'),
  /**
   * During page save time, we set the host overrides to the server.
   * The new host -> site:tag map is stored below. This will be
   * useful during save, to update the host's host components. Also,
   * it will be useful in deletion of overrides.
   *
   * Example:
   * {
   *  'hostname1': {
   *    'global': {
   *      'tagName': 'tag3187261938_hostname1',
   *      'map': {
   *        'hadoop_heapsize': '2048m'
   *      }
   *    }
   *  }
   * }
   *
   * @see loadedHostToOverrideSiteToTagMap
   */
  savedHostToOverrideSiteToTagMap: {},
  selectedConfigGroup: null,
  configGroups: [],

  selectedServices: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false);
  }.property('content.services').cacheable(),

  clearStep: function () {
    this.get('services').clear();
    this.get('configs').clear();
    this.get('globals').clear();
    this.get('clusterInfo').clear();
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
  },

  loadStep: function () {
    console.log("TRACE: Loading step8: Review Page");
    if (this.get('content.controllerName') != 'installerController') {
      App.router.get('mainAdminSecurityController').setSecurityStatus();
    }
    this.clearStep();
    if (this.get('content.serviceConfigProperties')) {
      this.formatProperties();
      this.loadGlobals();
      this.loadConfigs();
    }
    this.loadClusterInfo();
    this.loadServices();
    this.set('isSubmitDisabled', false);
    this.set('isBackBtnDisabled', false);
  },
  /**
   * replace whitespace character with coma between directories
   */
  formatProperties: function(){
    this.get('content.serviceConfigProperties').forEach(function(_configProperty){
        _configProperty.value = App.config.trimProperty(_configProperty,false);
    });
  },

  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
    if (globals.someProperty('name', 'hive_database')) {
      var hiveDb = globals.findProperty('name', 'hive_database');
      var hiveDbType = {name: 'hive_database_type', value: 'mysql'};
      var hiveJdbcDriver = {name: 'hive_jdbc_driver'};

      if (hiveDb.value === 'New MySQL Database') {
        if (globals.someProperty('name', 'hive_ambari_host')) {
          globals.findProperty('name', 'hive_hostname').value = globals.findProperty('name', 'hive_ambari_host').value;
          hiveDbType.value = 'mysql';
          hiveJdbcDriver.value = 'com.mysql.jdbc.Driver';
        }
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
      } else if (hiveDb.value === 'Existing MySQL Database'){
        globals.findProperty('name', 'hive_hostname').value = globals.findProperty('name', 'hive_existing_mysql_host').value;
        hiveDbType.value = 'mysql';
        hiveJdbcDriver.value = 'com.mysql.jdbc.Driver';
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_oracle_database'));
      } else { //existing oracle database
        globals.findProperty('name', 'hive_hostname').value = globals.findProperty('name', 'hive_existing_oracle_host').value;
        hiveDbType.value = 'oracle';
        hiveJdbcDriver.value = 'oracle.jdbc.driver.OracleDriver';
        globals = globals.without(globals.findProperty('name', 'hive_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'hive_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'hive_existing_mysql_database'));
      }
      globals.push(hiveDbType);
      globals.push(hiveJdbcDriver);
    }

    if (globals.someProperty('name', 'oozie_database')) {
      var oozieDb = globals.findProperty('name', 'oozie_database');
      var oozieDbType = {name:'oozie_database_type'};
      var oozieJdbcDriver = {name: 'oozie_jdbc_driver'};

      if (oozieDb.value === 'New Derby Database') {
        globals.findProperty('name', 'oozie_hostname').value = globals.findProperty('name', 'oozie_ambari_host').value;
        oozieDbType.value = 'derby';
        oozieJdbcDriver.value = 'org.apache.derby.jdbc.EmbeddedDriver';
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
      } else if (oozieDb.value === 'Existing MySQL Database') {
        globals.findProperty('name', 'oozie_hostname').value = globals.findProperty('name', 'oozie_existing_mysql_host').value;
        oozieDbType.value = 'mysql';
        oozieJdbcDriver.value = 'com.mysql.jdbc.Driver';
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_oracle_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
      } else { // existing oracle database
        globals.findProperty('name', 'oozie_hostname').value = globals.findProperty('name', 'oozie_existing_oracle_host').value;
        oozieDbType.value = 'oracle';
        oozieJdbcDriver.value = 'oracle.jdbc.driver.OracleDriver';
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_ambari_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_host'));
        globals = globals.without(globals.findProperty('name', 'oozie_existing_mysql_database'));
        globals = globals.without(globals.findProperty('name', 'oozie_derby_database'));
      }
      globals.push(oozieDbType);
      globals.push(oozieJdbcDriver);
    }

    this.set('globals', globals);
  },

  loadConfigs: function () {
    //storedConfigs contains custom configs as well
    var serviceConfigProperties = this.get('content.serviceConfigProperties').filterProperty('id', 'site property');
    serviceConfigProperties.forEach(function(_config){
      _config.value = (typeof _config.value === "boolean") ? _config.value.toString() : _config.value;
    });
    var storedConfigs = serviceConfigProperties.filterProperty('value');
    var mappedConfigs = App.config.excludeUnsupportedConfigs(this.get('configMapping'), this.get('selectedServices').mapProperty('serviceName'));
    var uiConfigs = this.loadUiSideConfigs(mappedConfigs);
    this.set('configs', storedConfigs.concat(uiConfigs));
  },

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
      App.config.setConfigValue(uiConfig, this.get('content.serviceConfigProperties'), _config, this.get('globals'));
      uiConfig.pushObject({
        "id": "site property",
        "name": _config._name || _config.name,
        "value": _config.value,
        "filename": _config.filename
      });
    }, this);
    return uiConfig;
  },

  addDynamicProperties: function(configs) {
    var templetonHiveProperty =  this.get('content.serviceConfigProperties').someProperty('name', 'templeton.hive.properties');
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

  getRegisteredHosts: function () {
    var allHosts = this.get('content.hosts');
    var hosts = [];
    for (var hostName in allHosts) {
      if (allHosts[hostName].bootStatus == 'REGISTERED') {
        allHosts[hostName].hostName = allHosts[hostName].name;
        hosts.pushObject(allHosts[hostName]);
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
   *   overrides: {
   *    'value1': [h1, h2],
   *    'value2': [h3]
   *   }
   * }</code>
   */

  getGlobConfigValueWithOverrides: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return { value : expression, overrides: []};      // if site property do not map any global property then return the value
    }
    var overrideHostToValue = {};
    express.forEach(function (_express) {
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globals').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globalObj = this.get('globals').findProperty('name', templateName[index]);
        var globValue = globalObj.value;
        // Hack for templeton.zookeeper.hosts
        var preReplaceValue = null;
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          preReplaceValue = value;
          value = this._replaceConfigValues(name, _express, value, globValue);
        }
        if(globalObj.overrides!=null){
          globalObj.overrides.forEach(function(override){
            var ov = override.value;
            var hostsArray = override.hosts;
            hostsArray.forEach(function(host){
              if(!(host in overrideHostToValue)){
                overrideHostToValue[host] = this._replaceConfigValues(name, _express, preReplaceValue, ov);
              }else{
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
    if(!jQuery.isEmptyObject(overrideHostToValue)){
      for(var host in overrideHostToValue){
        var hostVal = overrideHostToValue[host];
        if(!(hostVal in overrideValueToHostMap)){
          overrideValueToHostMap[hostVal] = [];
        }
        overrideValueToHostMap[hostVal].push(host);
      }
    }
    for(var val in overrideValueToHostMap){
      valueWithOverrides.overrides.push({
        value: val,
        hosts: overrideValueToHostMap[val]
      });
    }
    return valueWithOverrides;
  },

  _replaceConfigValues: function (name, express, value, globValue) {
    return value.replace(express, globValue);
  },

  /**
   * Load all info about cluster to <code>clusterInfo</code> variable
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
    var masterHosts = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();
    var slaveHosts = this.get('content.slaveComponentHosts');

    var hostObj = [];
    slaveHosts.forEach(function (_hosts) {
      hostObj = hostObj.concat(_hosts.hosts);
    }, this);

    slaveHosts = hostObj.mapProperty('hostName').uniq();

    var componentHosts = masterHosts.concat(slaveHosts).uniq();
    var totalHosts = App.Host.find().mapProperty('hostName').concat(componentHosts).uniq();
    var newHostsCount = totalHosts.length - App.Host.find().content.length;
    this.set('totalHosts', totalHosts);
    var totalHostsObj = this.rawContent.findProperty('config_name', 'hosts');
    totalHostsObj.config_value = totalHosts.length + ' (' + newHostsCount + ' new)';
    this.get('clusterInfo').pushObject(Ember.Object.create(totalHostsObj));

    //repo
    if (['addHostController', 'addServiceController'].contains(this.get('content.controllerName'))) {
      this.loadRepoInfo();
    } else { // from install wizard
      var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
      var allRepos = [];
      var supportedOs = ['redhat5', 'redhat6', 'sles11'];
      if (selectedStack && selectedStack.operatingSystems) {
        selectedStack.operatingSystems.forEach(function (os) {
          if (os.selected && supportedOs.contains(os.osType)) {
            allRepos.push(Em.Object.create({
              base_url: os.baseUrl,
              os_type: Em.I18n.t("installer.step8.repoInfo.osType." + os.osType)
            }));
          }
        }, this);
      }
      allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
      this.get('clusterInfo').set('repoInfo', allRepos);
    }
  },

  /**
   * get the repositories info of HDP from server. Used only in addHost controller.
   */
  loadRepoInfo: function(){
    var nameVersionCombo = App.get('currentStackVersion');
    var stackName = nameVersionCombo.split('-')[0];
    var stackVersion = nameVersionCombo.split('-')[1];
    App.ajax.send({
      name: 'cluster.load_repositories',
      sender: this,
      data: {
        stackName: stackName,
        stackVersion: stackVersion
      },
      success: 'loadRepoInfoSuccessCallback',
      error: 'loadRepositoriesErrorCallback'
    });
  },

  loadRepoInfoSuccessCallback: function (data) {
    var allRepos = [];
    var supportedOs = ['redhat5', 'redhat6', 'sles11'];
    data.items.forEach(function (item) {
      var os = item.repositories[0].Repositories;
      if (supportedOs.contains(os.os_type)) {
        allRepos.push(Em.Object.create({
          base_url: os.base_url,
          os_type: Em.I18n.t("installer.step8.repoInfo.osType." + os.os_type)
        }));
      }
    }, this);
    allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
    this.get('clusterInfo').set('repoInfo', allRepos);
  },

  loadRepoInfoErrorCallback: function(request, ajaxOptions, error) {
    console.log('Error message is: ' + request.responseText);
    var allRepos = [];
    allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
    this.get('clusterInfo').set('repoInfo', allRepos);
  },

  /**
   * Load all info about services to <code>services</code> variable
   */
  loadServices: function () {
    var reviewService = this.rawContent.findProperty('config_name', 'services');

    this.get('selectedServices').forEach(function (_service) {
      console.log('INFO: step8: Name of the service from getService function: ' + _service.serviceName);
      var serviceObj = reviewService.config_value.findProperty('service_name', _service.serviceName);
      if (serviceObj) {
        serviceObj.get('service_components').forEach(function (_component) {
          this.assignComponentHosts(_component);
        }, this);
        this.get('services').pushObject(serviceObj);
      }
    }, this);
  },

  assignComponentHosts: function (component) {
    var componentValue;
    if (component.get('customHandler')) {
      this[component.get('customHandler')].call(this, component);
    } else {
      if (component.get('isMaster')) {
        componentValue = this.get('content.masterComponentHosts')
          .findProperty('component', component.component_name).hostName;
      } else {
        var hostsLength = this.get('content.slaveComponentHosts')
          .findProperty('componentName', component.component_name)
          .hosts.length;
        componentValue = hostsLength + Em.I18n.t('installer.step8.host' + ((hostsLength > 1) ? 's' : ''));
      }
      component.set('component_value', componentValue);
    }
  },
  loadHiveDbValue: function (dbComponent) {
    var hiveDb = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'hive_database');
    if (hiveDb.value === 'New MySQL Database') {
      dbComponent.set('component_value', 'MySQL (New Database)');
    } else if(hiveDb.value === 'Existing MySQL Database'){
      var db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'hive_existing_mysql_database');
      dbComponent.set('component_value', db.value + ' (' + hiveDb.value + ')');
    } else { // existing oracle database
      var db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'hive_existing_oracle_database');
      dbComponent.set('component_value', db.value + ' (' + hiveDb.value + ')');
    }
  },
  loadHbaseMasterValue: function (hbaseMaster) {
    var hbaseHostName = this.get('content.masterComponentHosts').filterProperty('component', hbaseMaster.component_name);
    if (hbaseHostName.length == 1) {
      hbaseMaster.set('component_value', hbaseHostName[0].hostName);
    } else {
      hbaseMaster.set('component_value', hbaseHostName[0].hostName + Em.I18n.t('installer.step8.other').format(hbaseHostName.length - 1));
    }
  },
  loadZkServerValue: function (serverComponent) {
    var zkHostNames = this.get('content.masterComponentHosts').filterProperty('component', serverComponent.component_name).length;
    var hostSuffix;
    if (zkHostNames === 1) {
      hostSuffix = Em.I18n.t('installer.step8.host');
    } else {
      hostSuffix = Em.I18n.t('installer.step8.hosts');
    }
    serverComponent.set('component_value', zkHostNames + ' ' + hostSuffix);
  },
  loadOozieDbValue: function (dbComponent) {
    var oozieDb = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_database');
    if (oozieDb.value === 'New Derby Database'){
      var db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_derby_database');
      dbComponent.set('component_value', db.value + ' (' + oozieDb.value + ')');
    }/* else if (oozieDb.value === 'New MySQL Database') {
      dbComponent.set('component_value', 'MySQL (New Database)');
    } */else if(oozieDb.value === 'Existing MySQL Database'){
      var db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_existing_mysql_database');
      dbComponent.set('component_value', db.value + ' (' + oozieDb.value + ')');
    } else { // existing oracle database
      var db = this.get('wizardController').getDBProperty('serviceConfigProperties').findProperty('name', 'oozie_existing_oracle_database');
      dbComponent.set('component_value', db.value + ' (' + oozieDb.value + ')');
    }

  },
  loadNagiosAdminValue: function (nagiosAdmin) {
    var config = this.get('content.serviceConfigProperties');
    var adminLoginName = config.findProperty('name', 'nagios_web_login');
    var adminEmail = config.findProperty('name', 'nagios_contact');
    nagiosAdmin.set('component_value', adminLoginName.value + ' / (' + adminEmail.value + ')');
  },
  /**
   * Onclick handler for <code>next</code> button
   */
  submit: function () {
    if (this.get('isSubmitDisabled')) {
      return;
    }
    if ((this.get('content.controllerName') == 'addHostController') && this.get('securityEnabled')) {
      var self = this;
      App.showConfirmationPopup(function() {
        self.submitProceed();
      }, Em.I18n.t('installer.step8.securityConfirmationPopupBody'));
    }
    else {
      this.submitProceed();
    }
  },
  /**
   * Update configurations for installed services.
   *
   * @param {Array} configsToUpdate - configs need to update
   * @return {*}
   */
  updateConfigurations: function (configsToUpdate) {
    var configurationController = App.router.get('mainServiceInfoConfigsController');
    var serviceNames = configsToUpdate.mapProperty('serviceName').uniq();
    serviceNames.forEach(function(serviceName) {
      var configs = configsToUpdate.filterProperty('serviceName', serviceName);
      configurationController.setNewTagNames(configs);
      var tagName = configs.objectAt(0).newTagName;
      var siteConfigs = configs.filterProperty('id', 'site property');
      siteConfigs.mapProperty('filename').uniq().forEach(function(siteName) {
        var formattedConfigs = configurationController.createSiteObj(siteName.replace(".xml", ""), tagName, configs.filterProperty('filename', siteName));
        configurationController.doPUTClusterConfigurationSite(formattedConfigs);
      });
    });
  },

  submitProceed: function() {
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
    if (this.get('content.controllerName') == 'installerController' && (!App.testMode)) {
      var clusterNames = this.getExistingClusterNames();
      this.deleteClusters(clusterNames);
    }
    if (this.get('wizardController').getDBProperty('configsToUpdate')) {
      this.updateConfigurations(this.get('wizardController').getDBProperty('configsToUpdate'));
    }
    this.setLocalRepositories();
    this.createCluster();
    this.createSelectedServices();
    this.createConfigurations();
    this.createComponents();
    this.registerHostsToCluster();
    if (App.supports.hostOverridesInstaller) {
      this.createConfigurationGroups();
    }
    this.createAllHostComponents();

    this.ajaxQueueFinished = function () {
      console.log('everything is loaded');
      App.router.send('next');
    };
    this.doNextAjaxCall();
  },

  /**
   * Used in progress bar
   */
  ajaxQueueLength: function () {
    return this.get('ajaxQueue').length;
  }.property('ajaxQueue.length'),

  /**
   * Used in progress bar
   */
  ajaxQueueLeft: 0,

  setAmbariUIDb: function () {
    var dbContent = this.get('content.slaveGroupProperties');
    var slaveComponentConfig = this.get("slaveComponentConfig");
    this.persistKeyValues(slaveComponentConfig.version, dbContent);
    this.persistKeyValues('current_version', slaveComponentConfig.version);
  },

  persistKeyValues: function (key, value) {

    var str = "{ '" + key + "' : '" + JSON.stringify(value) + "'}";
    var obj = eval("(" + str + ")");

    this.ajax({
      type: "POST",
      url: App.apiPrefix + '/persist',
      data: JSON.stringify(obj),
      beforeSend: function () {
        console.log('BeforeSend: persistKeyValues', obj);
      }
    });
  },

  clusterName: function () {
    return this.get('content.cluster.name');
  }.property('content.cluster.name'),

  clusterNames: [],

  // returns an array of existing cluster names.
  // returns an empty array if there are no existing clusters.
  getExistingClusterNames: function () {
    var url = App.apiPrefix + '/clusters';

    App.ajax.send({
      name: 'wizard.step8.existing_cluster_names',
      sender: this,
      success: 'getExistingClusterNamesSuccessCallBack',
      error: 'getExistingClusterNamesErrorCallback'
    });

    return this.get('clusterNames');
  },

  getExistingClusterNamesSuccessCallBack: function (data) {
    var clusterNames = data.items.mapProperty('Clusters.cluster_name');
    console.log("Got existing cluster names: " + clusterNames);
    this.set('clusterNames', clusterNames);
  },

  getExistingClusterNamesErrorCallback: function () {
    console.log("Failed to get existing cluster names");
    this.set('clusterNames', []);
  },

  deleteClusters: function (clusterNames) {
    clusterNames.forEach(function (clusterName) {
      App.ajax.send({
        name: 'wizard.step8.delete_cluster',
        sender: this,
        data: {
          name: clusterName
        },
        success: 'deleteClustersSuccessCallback',
        error: 'deleteClustersErrorCallback'
      });
    }, this);
  },

  deleteClustersSuccessCallback: function(data, opt, params) {
    console.log('DELETE cluster ' + params.name + ' succeeded');
  },
  deleteClustersErrorCallback: function(request, ajaxOptions, error, opt) {
    console.log('DELETE cluster failed');
  },
  

  /**
   * Updates local repositories for the Ambari server.
   */
  setLocalRepositories: function () {
    if (this.get('content.controllerName') !== 'installerController' || !App.supports.localRepositories) {
      return false;
    }
    var self = this;
    var apiUrl = App.get('stack2VersionURL');
    var stacks = this.get('content.stacks');
    stacks.forEach(function (stack) {
      stack.operatingSystems.forEach(function (os) {
        if (os.baseUrl !== os.originalBaseUrl) {
          console.log("Updating local repository URL from " + os.originalBaseUrl + " -> " + os.baseUrl + ". ", os);
          var url = App.apiPrefix + apiUrl + "/operatingSystems/" + os.osType + "/repositories/" + stack.name;
          self.ajax({
            type: 'PUT',
            url: url,
            data: JSON.stringify({
              "Repositories": {
                "base_url": os.baseUrl
              }
            }),
            beforeSend: function () {
              console.log("BeforeSend: setLocalRepositories PUT to ", url);
            }
          });
        }
      });
    });
  },


  /**
   *  The following create* functions are called upon submitting Step 8.
   */

  createCluster: function () {

    if (this.get('content.controllerName') !== 'installerController') {
      return false;
    }

    var clusterName = this.get('clusterName');
    var url = App.apiPrefix + '/clusters/' + clusterName;

    var stackVersion = (this.get('content.installOptions.localRepo')) ? App.currentStackVersion.replace(/(-\d+(\.\d)*)/ig, "Local$&") : App.currentStackVersion;

    this.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify({ "Clusters": {"version": stackVersion }}),
      beforeSend: function () {
        console.log("BeforeSend: createCluster for " + clusterName);
      }
    });

  },

  createSelectedServices: function () {

    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/services';
    var data = this.createServiceData();
    var httpMethod = 'POST';

    if (!data.length) {
      return;
    }

    this.ajax({
      type: httpMethod,
      url: url,
      data: JSON.stringify(data),
      beforeSend: function () {
        console.log('BeforeSend: createSelectedServices ', data);
      }
    });
  },

  createServiceData: function () {
    var services = this.get('selectedServices').mapProperty('serviceName');
    var data = [];
    services.forEach(function (_service) {
      data.pushObject({"ServiceInfo": { "service_name": _service }});
    }, this);
    return data;
  },

  createComponents: function () {

    var serviceComponents = require('data/service_components');
    var services = this.get('selectedServices').mapProperty('serviceName');
    services.forEach(function (_service) {
      var components = serviceComponents.filterProperty('service_name', _service);
      var componentsData = components.map(function (_component) {
        return { "ServiceComponentInfo": { "component_name": _component.component_name } };
      });

      // Service must be specified in terms of a query for creating multiple components at the same time.
      // See AMBARI-1018.
      var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/services?ServiceInfo/service_name=' + _service;
      var data = {
        "components": componentsData
      };

      this.ajax({
        type: 'POST',
        url: url,
        data: JSON.stringify(data),
        beforeSend: function () {
          console.log('BeforeSend: createComponents for ' + _service, componentsData);
        }
      });
    }, this);

  },

  registerHostsToCluster: function () {

    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/hosts';
    var data = this.createRegisterHostData();

    if (data.length == 0) {
      return;
    }

    this.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data),
      beforeSend: function () {
        console.log('BeforeSend: registerHostsToCluster', data);
      }
    });
  },

  createRegisterHostData: function () {
    var hosts = this.getRegisteredHosts().filterProperty('isInstalled', false);
    if (!hosts.length) {
      return [];
    }
    return hosts.map(function (host) {
      return {"Hosts": { "host_name": host.hostName}};
    });
  },

  // TODO: review the code for add hosts / add services scenarios...
  createAllHostComponents: function () {

    var masterHosts = this.get('content.masterComponentHosts');
    var slaveHosts = this.get('content.slaveComponentHosts');
    var clients = this.get('content.clients');

    // note: masterHosts has 'component' vs slaveHosts has 'componentName'
    var masterComponents = masterHosts.mapProperty('component').uniq();

    masterComponents.forEach(function (component) {
      var hostNames = masterHosts.filterProperty('component', component).filterProperty('isInstalled', false).mapProperty('hostName');
      this.registerHostsToComponent(hostNames, component);
    }, this);

    slaveHosts.forEach(function (_slave) {
      if (_slave.componentName !== 'CLIENT') {
        var hostNames = _slave.hosts.filterProperty('isInstalled', false).mapProperty('hostName');
        this.registerHostsToComponent(hostNames, _slave.componentName);
      } else {
        clients.forEach(function (_client) {

          var hostNames = _slave.hosts.mapProperty('hostName');
          switch (_client.component_name) {
            case 'HDFS_CLIENT':
              // install HDFS_CLIENT on HBASE_MASTER, HBASE_REGIONSERVER, WEBHCAT_SERVER, HISTORYSERVER and OOZIE_SERVER hosts
              masterHosts.filterProperty('component', 'HBASE_MASTER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'HBASE_REGIONSERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'WEBHCAT_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'HISTORYSERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'OOZIE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'MAPREDUCE_CLIENT':
              // install MAPREDUCE_CLIENT on HIVE_SERVER, OOZIE_SERVER, NAGIOS_SERVER, and WEBHCAT_SERVER hosts
              masterHosts.filterProperty('component', 'HIVE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'OOZIE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'WEBHCAT_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'OOZIE_CLIENT':
              // install OOZIE_CLIENT on NAGIOS_SERVER host
              masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'ZOOKEEPER_CLIENT':
              // install ZOOKEEPER_CLIENT on WEBHCAT_SERVER host
              masterHosts.filterProperty('component', 'WEBHCAT_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'HIVE_CLIENT':
              //install HIVE client on NAGIOS_SERVER host
              masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'HIVE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'HCAT':
              // install HCAT (client) on NAGIOS_SERVER host
              masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'YARN_CLIENT':
              // install YARN_CLIENT on NAGIOS_SERVER,HIVE_SERVER,OOZIE_SERVER,WEBHCAT_SERVER host
              masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'HIVE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'OOZIE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'WEBHCAT_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
            case 'TEZ_CLIENT':
              //install TEZ client on HIVE_SERVER, HIVE_CLIENT hosts.
              //HIVE_CLIENT always installed on NAGIOS_SERVER host, so it is the same host with NAGIOS_SERVER and HIVE_CLIENT
              masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              masterHosts.filterProperty('component', 'HIVE_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                hostNames.pushObject(_masterHost.hostName);
              }, this);
              break;
          }
          hostNames = hostNames.uniq();

          if (_client.isInstalled) {
            //check whether clients are already installed on selected master hosts!!!
            var installedHosts = _slave.hosts.filterProperty('isInstalled', true).mapProperty('hostName');
            installedHosts.forEach(function (host) {
              if (hostNames.contains(host)) {
                hostNames.splice(hostNames.indexOf(host), 1);
              }
            }, this);
          }

          this.registerHostsToComponent(hostNames, _client.component_name);

        }, this);
      }
    }, this);

    // add Ganglia Monitor (Slave) to all hosts if Ganglia service is selected
    var gangliaService = this.get('content.services').filterProperty('isSelected', true).findProperty('serviceName', 'GANGLIA');
    if (gangliaService) {
      var hosts = this.getRegisteredHosts();
      if (gangliaService.get('isInstalled')) {
        hosts = hosts.filterProperty('isInstalled', false);
      }
      if (hosts.length) {
        this.registerHostsToComponent(hosts.mapProperty('hostName'), 'GANGLIA_MONITOR');
      }
    }
    // add MySQL Server if Hive is selected
    var hiveService = this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).findProperty('serviceName', 'HIVE');
    if (hiveService) {
      var hiveDb = this.get('content.serviceConfigProperties').findProperty('name', 'hive_database');
        if(hiveDb.value == "New MySQL Database") {
      this.registerHostsToComponent(masterHosts.filterProperty('component', 'HIVE_SERVER').mapProperty('hostName'), 'MYSQL_SERVER');
        }
    }
  },

  registerHostsToComponent: function (hostNames, componentName) {

    if (hostNames.length == 0) {
      return;
    }

    var queryStr = '';
    hostNames.forEach(function (hostName) {
      queryStr += 'Hosts/host_name=' + hostName + '|';
    });
    //slice off last symbol '|'
    queryStr = queryStr.slice(0, -1);

    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/hosts';
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

    this.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data)
    });
  },

  createConfigurations: function () {
    var selectedServices = this.get('selectedServices');
    if (this.get('content.controllerName') == 'installerController') {
      this.get('serviceConfigTags').pushObject(this.createCoreSiteObj());
      this.get('serviceConfigTags').pushObject(this.createHdfsSiteObj());
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('HDFS'));
    }
    var globalSiteObj = this.createGlobalSiteObj();
    if (this.get('content.controllerName') == 'addServiceController') {
      globalSiteObj.tag = 'version' + (new Date).getTime();
    }
    this.get('serviceConfigTags').pushObject(globalSiteObj);
    if (selectedServices.someProperty('serviceName', 'MAPREDUCE')) {
      this.get('serviceConfigTags').pushObject(this.createMrSiteObj());
      if (App.supports.capacitySchedulerUi) {
        this.get('serviceConfigTags').pushObject(this.createCapacityScheduler());
        this.get('serviceConfigTags').pushObject(this.createMapredQueueAcls());
      }
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('MAPREDUCE'));
    }
    if (selectedServices.someProperty('serviceName', 'MAPREDUCE2')) {
      this.get('serviceConfigTags').pushObject(this.createMrSiteObj());
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('MAPREDUCE2'));
    }
    if (selectedServices.someProperty('serviceName', 'YARN')) {
      this.get('serviceConfigTags').pushObject(this.createYarnSiteObj());
      this.get('serviceConfigTags').pushObject(this.createCapacityScheduler());
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('YARN'));
    }
    if (selectedServices.someProperty('serviceName', 'HBASE')) {
      this.get('serviceConfigTags').pushObject(this.createHbaseSiteObj());
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('HBASE'));
    }
    if (selectedServices.someProperty('serviceName', 'OOZIE')) {
      this.get('serviceConfigTags').pushObject(this.createOozieSiteObj('OOZIE'));
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('OOZIE'));
    }
    if (selectedServices.someProperty('serviceName', 'HIVE')) {
      this.get('serviceConfigTags').pushObject(this.createHiveSiteObj('HIVE'));
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('HIVE'));
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('HIVE-EXEC'));
    }
    if (selectedServices.someProperty('serviceName', 'WEBHCAT')) {
      this.get('serviceConfigTags').pushObject(this.createWebHCatSiteObj('WEBHCAT'));
    }
    if (selectedServices.someProperty('serviceName', 'HUE')) {
      this.get('serviceConfigTags').pushObject(this.createHueSiteObj('HUE'));
    }
    if (selectedServices.someProperty('serviceName', 'PIG')) {
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('PIG'));
    }
    if (selectedServices.someProperty('serviceName', 'FALCON')) {
      this.get('serviceConfigTags').pushObject(this.createFalconSiteObj('FALCON'));
    }
    if (selectedServices.someProperty('serviceName', 'STORM')) {
      this.get('serviceConfigTags').pushObject(this.createStormSiteObj());
    }
    if (selectedServices.someProperty('serviceName', 'TEZ')) {
      this.get('serviceConfigTags').pushObject(this.createTezSiteObj());
    }
    if (selectedServices.someProperty('serviceName', 'ZOOKEEPER')) {
      this.get('serviceConfigTags').pushObject(this.createZooCfgObj());
      this.get('serviceConfigTags').pushObject(this.createLog4jObj('ZOOKEEPER'));
    }

    this.applyConfigurationsToCluster();
  },

  applyConfigurationsToCluster: function() {
    var clusterUrl = App.apiPrefix + '/clusters/' + this.get('clusterName');
    var configData = [];
    this.get('serviceConfigTags').forEach(function (_serviceConfig) {
      var Clusters = {
        Clusters: {
          desired_config: {
            type: _serviceConfig.type,
            tag: _serviceConfig.tag,
            properties: _serviceConfig.properties
          }
        }
      };
      configData.pushObject(JSON.stringify(Clusters));
    }, this);

    var data = {
      configData: '[' + configData.toString() + ']'
    };

    console.debug("applyConfigurationsToCluster(Step8): Applying to URL", clusterUrl, " Data:", data.configData);
    this.ajax({
      type: 'PUT',
      url: clusterUrl,
      data: data.configData,
      beforeSend: function () {
        console.log("BeforeSend: Updating cluster config");
      }
    });
  },

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

  applyConfigurationGroups: function (sendData) {
    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/config_groups';
    this.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(sendData)
    });
  },

  applyInstalledServicesConfigurationGroup: function (updateData) {
    updateData.forEach(function(item) {
      App.router.get('mainServiceInfoConfigsController').putConfigGroupChanges(item);
    });
  },

  removeInstalledServicesConfigurationGroups: function(groupsToDelete) {
    groupsToDelete.forEach(function(item) {
      App.config.deleteConfigGroup(Em.Object.create(item));
    });
  },

  createGlobalSiteObj: function () {
    var globalSiteProperties = {};
    var globalSiteObj = this.get('globals');
    var isGLUSTERFSSelected = this.get('selectedServices').someProperty('serviceName', 'GLUSTERFS');
    
    // screen out the GLUSTERFS-specific global config entries when they are not required
    if (!isGLUSTERFSSelected) {
      globalSiteObj = globalSiteObj.filter(function(_config) {
        return _config.name.indexOf("fs_glusterfs") < 0;
      });
    }
    
    globalSiteObj.forEach(function (_globalSiteObj) {
      var heapsizeException =  ['hadoop_heapsize','yarn_heapsize','nodemanager_heapsize','resourcemanager_heapsize', 'apptimelineserver_heapsize'];
      // do not pass any globals whose name ends with _host or _hosts
      if (_globalSiteObj.isRequiredByAgent !== false) {
        // append "m" to JVM memory options except for hadoop_heapsize
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name) && !heapsizeException.contains(_globalSiteObj.name)) {
          globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value + "m";
        } else {
          globalSiteProperties[_globalSiteObj.name] = App.config.escapeXMLCharacters(_globalSiteObj.value);
        }
      }
    }, this);
    // we don't expose gmond_user to the user; it needs to be the same as gmetad_user
    globalSiteProperties['gmond_user'] = globalSiteProperties['gmetad_user'];
    return {"type": "global", "tag": "version1", "properties": globalSiteProperties};
  },

  createCoreSiteObj: function () {
    var coreSiteObj = this.get('configs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    // hadoop.proxyuser.oozie.hosts needs to be skipped if oozie is not selected
    var isOozieSelected = this.get('selectedServices').someProperty('serviceName', 'OOZIE');
    var oozieUser = this.get('globals').someProperty('name', 'oozie_user') ? this.get('globals').findProperty('name', 'oozie_user').value : null;
    var isHiveSelected = this.get('selectedServices').someProperty('serviceName', 'HIVE');
    var hiveUser = this.get('globals').someProperty('name', 'hive_user') ? this.get('globals').findProperty('name', 'hive_user').value : null;
    var isHcatSelected = this.get('selectedServices').someProperty('serviceName', 'WEBHCAT');
    var hcatUser = this.get('globals').someProperty('name', 'hcat_user') ? this.get('globals').findProperty('name', 'hcat_user').value : null;
    var isGLUSTERFSSelected = this.get('selectedServices').someProperty('serviceName', 'GLUSTERFS');

    // screen out the GLUSTERFS-specific core-site.xml entries when they are not needed
    if (!isGLUSTERFSSelected) {
      coreSiteObj = coreSiteObj.filter(function(_config) {
        return _config.name.indexOf("fs.glusterfs") < 0;
      });
    }

    coreSiteObj.forEach(function (_coreSiteObj) {
      if ((isOozieSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + oozieUser + '.groups')) && (isHiveSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hiveUser + '.groups')) && (isHcatSelected || (_coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.hosts' && _coreSiteObj.name != 'hadoop.proxyuser.' + hcatUser + '.groups'))) {
        coreSiteProperties[_coreSiteObj.name] = App.config.escapeXMLCharacters(_coreSiteObj.value);
      }
      if (isGLUSTERFSSelected && _coreSiteObj.name == "fs.default.name") {
        coreSiteProperties[_coreSiteObj.name] = this.get('globals').someProperty('name', 'fs_glusterfs_default_name') ? App.config.escapeXMLCharacters(this.get('globals').findProperty('name', 'fs_glusterfs_default_name').value) : null;
      }
      if (isGLUSTERFSSelected && _coreSiteObj.name == "fs.defaultFS") {
        coreSiteProperties[_coreSiteObj.name] = this.get('globals').someProperty('name', 'glusterfs_defaultFS_name') ? App.config.escapeXMLCharacters(this.get('globals').findProperty('name', 'glusterfs_defaultFS_name').value) : null;
      }
      console.log("STEP*: name of the property is: " + _coreSiteObj.name);
      console.log("STEP8: value of the property is: " + _coreSiteObj.value);
    }, this);
    return {"type": "core-site", "tag": "version1", "properties": coreSiteProperties};
  },

  createHdfsSiteObj: function () {
    var hdfsSiteObj = this.get('configs').filterProperty('filename', 'hdfs-site.xml');
    var hdfsProperties = {};
    hdfsSiteObj.forEach(function (_configProperty) {
      hdfsProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {"type": "hdfs-site", "tag": "version1", "properties": hdfsProperties };
  },

  createLog4jObj: function (fileName) {
    fileName = fileName.toLowerCase();
    var Log4jObj = this.get('configs').filterProperty('filename', fileName + '-log4j.xml');
    var Log4jProperties = {};
    Log4jObj.forEach(function (_configProperty) {
      Log4jProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {"type": fileName + "-log4j", "tag": "version1", "properties": Log4jProperties };
  },

  createHueSiteObj: function () {
    var hueSiteObj = this.get('configs').filterProperty('filename', 'hue-site.xml');
    var hueProperties = {};
    hueSiteObj.forEach(function (_configProperty) {
      hueProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {"type": "hue-site", "tag": "version1", "properties": hueProperties };
  },

  createMrSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'mapred-site.xml');
    var mrProperties = {};
    configs.forEach(function (_configProperty) {
      mrProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'mapred-site', tag: 'version1', properties: mrProperties};
  },

  createYarnSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'yarn-site.xml');
    var mrProperties = {};
    configs.forEach(function (_configProperty) {
      mrProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'yarn-site', tag: 'version1', properties: mrProperties};
  },

  createCapacityScheduler: function () {
    var configs = this.get('configs').filterProperty('filename', 'capacity-scheduler.xml');
    var csProperties = {};
    configs.forEach(function (_configProperty) {
      csProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'capacity-scheduler', tag: 'version1', properties: csProperties};
  },

  createMapredQueueAcls: function () {
    var configs = this.get('configs').filterProperty('filename', 'mapred-queue-acls.xml');
    var mqProperties = {};
    configs.forEach(function (_configProperty) {
     mqProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'mapred-queue-acls', tag: 'version1', properties: mqProperties};
  },

  createHbaseSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'hbase-site.xml');
    var hbaseProperties = {};
    configs.forEach(function (_configProperty) {
      hbaseProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'hbase-site', tag: 'version1', properties: hbaseProperties};
  },

  createOozieSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'oozie-site.xml');
    var oozieProperties = {};
    configs.forEach(function (_configProperty) {
      oozieProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'oozie-site', tag: 'version1', properties: oozieProperties};
  },

  createHiveSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'hive-site.xml');
    var hiveProperties = {};
    configs.forEach(function (_configProperty) {
      hiveProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'hive-site', tag: 'version1', properties: hiveProperties};
  },

  createWebHCatSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'webhcat-site.xml');
    var webHCatProperties = {};
    configs.forEach(function (_configProperty) {
      webHCatProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'webhcat-site', tag: 'version1', properties: webHCatProperties};
  },

  createZooCfgObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'zoo.cfg');
    var csProperties = {};
    configs.forEach(function (_configProperty) {
      csProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'zoo.cfg', tag: 'version1', properties: csProperties};
  },

  createStormSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'storm-site.xml');
    var stormProperties = {};
    var specialProperties = ["storm.zookeeper.servers", "nimbus.childopts", "supervisor.childopts", "worker.childopts"];
    configs.forEach(function (_configProperty) {
      if (specialProperties.contains(_configProperty.name)) {
        if (_configProperty.name == "storm.zookeeper.servers") {
          stormProperties[_configProperty.name] = JSON.stringify(_configProperty.value).replace(/"/g, "'");
        } else {
          stormProperties[_configProperty.name] = JSON.stringify(_configProperty.value).replace(/"/g,"");
        }
      } else {
        stormProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
      }
    }, this);
    return {type: 'storm-site', tag: 'version1', properties: stormProperties};
  },

  createTezSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'tez-site.xml');
    var tezProperty = {};
    configs.forEach(function (_configProperty) {
      tezProperty[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'tez-site', tag: 'version1', properties: tezProperty};
  },


  createFalconSiteObj: function (s) {
    var configs = this.get('configs').filterProperty('filename', 'oozie-site.xml');
    var falconProperties = {};
    configs.forEach(function (_configProperty) {
      falconProperties[_configProperty.name] = App.config.escapeXMLCharacters(_configProperty.value);
    }, this);
    return {type: 'oozie-site', tag: 'version1', properties: falconProperties};
  },

  ajaxQueueFinished: function () {
    //do something
  },

  doNextAjaxCall: function () {

    if (this.get('ajaxBusy')) {
      return;
    }

    var queue = this.get('ajaxQueue');
    if (!queue.length) {
      this.ajaxQueueFinished();
      return;
    }

    var first = queue[0];
    this.set('ajaxQueue', queue.slice(1));
    this.set('ajaxQueueLeft', this.get('ajaxQueue').length);

    this.set('ajaxBusy', true);
    $.ajax(first);

  },

  /**
   * We need to do a lot of ajax calls async in special order. To do this,
   * generate array of ajax objects and then send requests step by step. All
   * ajax objects are stored in <code>ajaxQueue</code>
   *
   * @param params
   */

  ajax: function (params) {
    if (App.testMode) return;

    var self = this;
    params = jQuery.extend({
      async: true,
      dataType: 'text',
      statusCode: require('data/statusCodes'),
      timeout: App.timeout,
      error: function (request, ajaxOptions, error) {
        console.log('Step8: In Error ');
        // console.log('Step8: Error message is: ' + request.responseText);
      },
      success: function (data) {
        console.log("TRACE: STep8 -> In success function");
      }
    }, params);

    var success = params.success;
    var error = params.error;

    params.success = function () {
      if (success) {
        success();
      }

      self.set('ajaxBusy', false);
      self.doNextAjaxCall();
    };

    params.error = function (xhr, status, error) {
      var responseText = JSON.parse(xhr.responseText);
      var controller = App.router.get(App.clusterStatus.wizardControllerName);
      controller.registerErrPopup(Em.I18n.t('common.error'), responseText.message);
      self.set('hasErrorOccurred', true);
      // an error will break the ajax call chain and allow submission again
      self.set('isSubmitDisabled', false);
      self.set('isBackBtnDisabled', false);
      App.router.get(self.get('content.controllerName')).setStepsEnable();
      self.get('ajaxQueue').clear();
      self.set('ajaxBusy', false);
    };
    this.get('ajaxQueue').pushObject(params);
  }

});
