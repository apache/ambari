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

App.MainAdminSecurityAddStep2Controller = Em.Controller.extend({

  name: 'mainAdminSecurityAddStep2Controller',
  isRecommendedLoaded: true,
  stepConfigs: [],
  installedServices: [],
  selectedService: null,
  securityUsers: [],
  filter: '',
  filterColumns: [],

  /**
   * map which depict connection between config and slave component
   * in order to set component hosts to config value
   */
  slaveComponentMap: [
    {
      serviceName: 'HDFS',
      configName: 'datanode_hosts',
      component: 'DATANODE'
    },
    {
      serviceName: 'YARN',
      configName: 'nodemanager_host',
      component: 'NODEMANAGER'
    },
    {
      serviceName: 'HBASE',
      configName: 'regionserver_hosts',
      component: 'HBASE_REGIONSERVER'
    },
    {
      serviceName: 'STORM',
      configName: 'supervisor_hosts',
      component: 'SUPERVISOR'
    }
  ],
  /**
   * map which depict connection between config and master component
   * in order to set component hosts to config value
   */
  masterComponentMap: [
    {
      serviceName: 'OOZIE',
      configName: 'oozie_servername',
      components: ['OOZIE_SERVER']
    },
    {
      serviceName: 'HIVE',
      configName: 'hive_metastore',
      components: ['HIVE_METASTORE', 'HIVE_SERVER']
    },
    {
      serviceName: 'HIVE',
      configName: 'webhcatserver_host',
      components: ['WEBHCAT_SERVER']
    },
    {
      serviceName: 'HDFS',
      configName: 'namenode_host',
      components: ['NAMENODE']
    },
    {
      serviceName: 'HDFS',
      configName: 'snamenode_host',
      components: ['SECONDARY_NAMENODE']
    },
    {
      serviceName: 'HDFS',
      configName: 'journalnode_hosts',
      components: ['JOURNALNODE']
    },
    {
      serviceName: 'MAPREDUCE2',
      configName: 'jobhistoryserver_host',
      components: ['HISTORYSERVER']
    },
    {
      serviceName: 'YARN',
      configName: 'resourcemanager_host',
      components: ['RESOURCEMANAGER']
    },
    {
      serviceName: 'YARN',
      configName: 'apptimelineserver_host',
      components: ['APP_TIMELINE_SERVER']
    },
    {
      serviceName: 'HBASE',
      configName: 'hbasemaster_host',
      components: ['HBASE_MASTER']
    },
    {
      serviceName: 'ZOOKEEPER',
      configName: 'zookeeperserver_hosts',
      components: ['ZOOKEEPER_SERVER']
    },
    {
      serviceName: 'FALCON',
      configName: 'falcon_server_host',
      components: ['FALCON_SERVER']
    },
    {
      serviceName: 'STORM',
      configName: 'storm_host',
      components: ['STORM_UI_SERVER', 'NIMBUS', 'SUPERVISOR']
    },
    {
      serviceName: 'STORM',
      configName: 'nimbus_host',
      components: ['NIMBUS']
    },
    {
      serviceName: 'KNOX',
      configName: 'knox_gateway_hosts',
      components: ['KNOX_GATEWAY']
    }
  ],

  hostToPrincipalMap: [
    {
      serviceName: 'OOZIE',
      configName: 'oozie_servername',
      principalName: 'oozie_principal_name',
      primaryName: 'oozie/'
    },
    {
      serviceName: 'OOZIE',
      configName: 'oozie_servername',
      principalName: 'oozie_http_principal_name',
      primaryName: 'HTTP/'
    },
    {
      serviceName: 'FALCON',
      configName: 'falcon_server_host',
      principalName: 'falcon_principal_name',
      primaryName: 'falcon/'
    },
    {
      serviceName: 'FALCON',
      configName: 'falcon_server_host',
      principalName: 'falcon_http_principal_name',
      primaryName: 'HTTP/'
    },
    {
      serviceName: 'HIVE',
      configName: 'webhcatserver_host',
      principalName: 'webHCat_http_principal_name',
      primaryName: 'HTTP/'
    }
  ],

  isSubmitDisabled: function () {
    return !this.get('stepConfigs').filterProperty('showConfig').everyProperty('errorCount', 0);
  }.property('stepConfigs.@each.errorCount'),

  /**
   * clear info of step
   */
  clearStep: function () {
    this.get('stepConfigs').clear();
    this.get('securityUsers').clear();
  },

  /**
   *  Function is called whenever the step is loaded
   */
  loadStep: function () {
    console.log("TRACE: Loading addSecurity step2: Configure Services");
    var versionNumber = App.get('currentStackVersionNumber');
    if( stringUtils.compareVersions(versionNumber, "2.2") >= 0){
      // Add Nimbus config options
      var masterComponentMap = this.get('masterComponentMap');
      masterComponentMap.filterProperty('configName', 'storm_host').components = ["SUPERVISOR", "STORM_UI_SERVER", "DRPC_SERVER", "STORM_REST_API"];
      masterComponentMap.pushObject({
        serviceName: 'STORM',
        configName: 'nimbus_host',
        components: ['NIMBUS']
      });
      this.get('hostToPrincipalMap').pushObject({
        serviceName: 'STORM',
        configName: 'nimbus_host',
        principalName: 'storm_principal_name',
        primaryName: 'storm'
      });
    }
    this.clearStep();
    this.loadUsers();
    this.addUserPrincipals(this.get('content.services'), this.get('securityUsers'));
    this.addMasterHostToConfigs();
    this.addHostPrincipals();
    this.addSlaveHostToConfigs();
    this.renderServiceConfigs(this.get('content.services'));
    this.changeCategoryOnHa(this.get('content.services'), this.get('stepConfigs'));
    this.setStoredConfigsValue(this.get('content.serviceConfigProperties'));
    this.set('installedServices', App.Service.find().mapProperty('serviceName'));
  },

  /**
   * set stored values to service configs
   * @param storedConfigProperties
   * @return {Boolean}
   */
  setStoredConfigsValue: function (storedConfigProperties) {
    if (!storedConfigProperties) return false;

    // for all services`
    this.get('stepConfigs').forEach(function (_content) {
      _content.get('configs').forEach(function (_config) {
        var configProperty = storedConfigProperties.findProperty('name', _config.get('name'));

        if (configProperty) {
          _config.set('value', configProperty.value);
        }
      }, this);
    }, this);
    return true;
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
        showConfig: true,
        configs: this.wrapConfigProperties(_serviceConfig)
      });

      this.get('stepConfigs').pushObject(serviceConfig);
    }, this);
    this.set('selectedService', this.get('stepConfigs').filterProperty('showConfig').objectAt(0));
  },

  /**
   * wrap configs into App.ServiceConfigProperty objects
   * @param _componentConfig
   */
  wrapConfigProperties: function (_componentConfig) {
    var configs = [];
    _componentConfig.configs.forEach(function (_serviceConfigProperty) {
      var serviceConfigProperty = App.ServiceConfigProperty.create(_serviceConfigProperty);
      serviceConfigProperty.set('isEditable', serviceConfigProperty.get('isReconfigurable'));
      serviceConfigProperty.validate();
      configs.pushObject(serviceConfigProperty);
    }, this);
    return configs;
  },

  /**
   * fill config with hosts of component
   * @param serviceName
   * @param configName
   * @param componentNames
   * @return {Boolean}
   */
  setHostsToConfig: function (serviceName, configName, componentNames) {
    var service = this.get('content.services').findProperty('serviceName', serviceName);
    if (service) {
      var hosts = service.configs.findProperty('name', configName);
      if (hosts) {
        hosts.defaultValue = App.Service.find(service.serviceName)
          .get('hostComponents')
          .filter(function (component) {
            return componentNames.contains(component.get('componentName'));
          })
          .mapProperty('hostName')
          .uniq();
        return true;
      }
      return false;
    }
    return false;
  },

  /**
   * set principal default value based on config host and default primary name
   * @param serviceName
   * @param hostConfigName
   * @param principalConfigName
   * @param defaultPrimaryName
   * @return {Boolean}
   */
  setHostToPrincipal: function (serviceName, hostConfigName, principalConfigName, defaultPrimaryName) {
    var service = this.get('content.services').findProperty('serviceName', serviceName);
    if (service) {
      var host = service.configs.findProperty('name', hostConfigName);
      var principal = service.configs.findProperty('name', principalConfigName);
      var versionNumber = App.get('currentStackVersionNumber');
      var special22ConfigsMap = {
        storm_principal_name: defaultPrimaryName,
        oozie_http_principal_name: defaultPrimaryName + '_HOST'
      };
      if (stringUtils.compareVersions(versionNumber, "2.2") >= 0 && special22ConfigsMap[principalConfigName]) {
        principal.defaultValue = special22ConfigsMap[principalConfigName];
        return true;
      }
      if (host && principal) {
        var host_defaultValue = Array.isArray(host.defaultValue) ? host.defaultValue[0] : host.defaultValue;
        principal.defaultValue = defaultPrimaryName + host_defaultValue;
        return true;
       }
      return false;
    }
    return false;
  },

  /**
   * load services users
   */
  loadUsers: function () {
    var securityUsers = App.router.get('mainAdminSecurityController').get('serviceUsers');
    if (Em.isNone(securityUsers) || securityUsers.length === 0) {
      if (App.get('testMode')) {
        securityUsers = securityUsers || [];
        securityUsers.pushObject({ name: 'hdfs_user', value: 'hdfs'});
        securityUsers.pushObject({ name: 'mapred_user', value: 'mapred'});
        securityUsers.pushObject({ name: 'hbase_user', value: 'hbase'});
        securityUsers.pushObject({ name: 'hive_user', value: 'hive'});
        securityUsers.pushObject({ name: 'smokeuser', value: 'ambari-qa'});
      } else {
        securityUsers = App.db.getSecureUserInfo();
      }
    }
    this.set('securityUsers', securityUsers);
  },

  /**
   * set default values to user principals and control their visibility
   * @param serviceConfigs
   * @param securityUsers
   */
  addUserPrincipals: function (serviceConfigs, securityUsers) {
    var generalService = serviceConfigs.findProperty('serviceName', 'GENERAL').configs;
    this.setUserPrincipalValue(securityUsers.findProperty('name', 'smokeuser'), generalService.findProperty('name', 'smokeuser_principal_name'));
    var servicesWithUserPrincipals = ['HDFS', 'HBASE'];

    servicesWithUserPrincipals.forEach(function (serviceName) {
      var isServiceInstalled = serviceConfigs.someProperty('serviceName', serviceName);
      var userPrincipal = generalService.findProperty('name', serviceName.toLowerCase() + '_principal_name');
      var userKeytab = generalService.findProperty('name', serviceName.toLowerCase() + '_user_keytab');
      var userName = securityUsers.findProperty('name', serviceName.toLowerCase() + '_user');
      if (isServiceInstalled && this.setUserPrincipalValue(userName, userPrincipal)) {
        userPrincipal.isVisible = true;
        userKeytab.isVisible = true;
      }
    }, this);
  },
  /**
   * set default value of user principal
   * @param user
   * @param userPrincipal
   */
  setUserPrincipalValue: function (user, userPrincipal) {
    if (user && userPrincipal) {
      userPrincipal.defaultValue = user.value;
      return true;
    }
    return false;
  },

  /**
   * put hosts of slave component into defaultValue of configs
   */
  addSlaveHostToConfigs: function () {
    this.get('slaveComponentMap').forEach(function (service) {
      this.setHostsToConfig(service.serviceName, service.configName, [service.component]);
    }, this);
  },

  /**
   * put hosts of master component into defaultValue of configs
   */
  addMasterHostToConfigs: function () {
    this.get('masterComponentMap').forEach(function (item) {
      this.setHostsToConfig(item.serviceName, item.configName, item.components);
    }, this);
  },

  /**
   * put hosts to principal default values
   */
  addHostPrincipals: function () {
    this.get('hostToPrincipalMap').forEach(function (item) {
      this.setHostToPrincipal(item.serviceName, item.configName, item.principalName, item.primaryName);
    }, this);
  },

  /**
   * modify config categories depending on whether HA is enabled or not
   * @param serviceConfigs
   * @param stepConfigs
   */
  changeCategoryOnHa: function (serviceConfigs, stepConfigs) {
    var hdfsService = serviceConfigs.findProperty('serviceName', 'HDFS');
    if (hdfsService) {
      var properties = stepConfigs.findProperty('serviceName', 'HDFS').get('configs');
      var configCategories = hdfsService.configCategories;
      if ((App.get('testMode') && App.get('testNameNodeHA')) || (this.get('content.isNnHa') === 'true')) {
        this.removeConfigCategory(properties, configCategories, 'SNameNode');
      } else {
        this.removeConfigCategory(properties, configCategories, 'JournalNode');
      }
      return true;
    }
    return false;
  },
  /**
   * remove config category that belong to component and hide category configs
   * @param properties
   * @param configCategories
   * @param component
   */
  removeConfigCategory: function (properties, configCategories, component) {
    properties.filterProperty('category', component).forEach(function (_snConfig) {
      _snConfig.set('isVisible', false);
    }, this);
    var category = configCategories.findProperty('name', component);
    if (category) {
      configCategories.removeObject(category);
    }
  }
});
