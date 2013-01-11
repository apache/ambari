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

App.WizardStep8Controller = Em.Controller.extend({
  name: 'wizardStep8Controller',
  rawContent: require('data/review_configs'),
  totalHosts: [],
  clusterInfo: [],
  services: [],
  configs: [],
  globals: [],
  configMapping: require('data/config_mapping'),
  slaveComponentConfig: null,
  isSubmitDisabled: false,
  hasErrorOccurred: false,

  selectedServices: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false);
  }.property('content.services').cacheable(),

  clearStep: function () {
    this.get('services').clear();
    this.get('configs').clear();
    this.get('globals').clear();
    this.get('clusterInfo').clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step8: Review Page");
    this.clearStep();
    this.loadGlobals();
    this.loadConfigs();
    this.setCustomConfigs();
    //this.loadSlaveConfiguration();
    this.loadClusterInfo();
    this.loadServices();
    this.set('isSubmitDisabled', false);
  },

  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
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
    this.set('globals', globals);
  },

  loadConfigs: function () {
    var storedConfigs = this.get('content.serviceConfigProperties').filterProperty('id', 'site property').filterProperty('value');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('configs', storedConfigs.concat(uiConfigs));
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
      if (this.get('globals').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globValue = this.get('globals').findProperty('name', templateName[index]).value;
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
    if (config.value == null) {
      return;
    }
    var fkValue = config.value.match(/<(foreignKey.*?)>/g);
    if (fkValue) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.value = config.value.replace(_fkValue, globalValue);
        } else if (this.get('content.serviceConfigProperties').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value;
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
        } else if (this.get('content.serviceConfigProperties').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value;
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
        if (this.get('globals').someProperty('name', config.templateName[index])) {
          var globalValue = this.get('globals').findProperty('name', config.templateName[index]).value;
          config.value = config.value.replace(_value, globalValue);
        } else {
          config.value = null;
        }
      }, this);
    }
  },

  /**
   * override site properties with the entered key-value pair in *-site.xml
   */
  setCustomConfigs: function () {
    var site = this.get('content.serviceConfigProperties').filterProperty('id', 'conf-site');
    site.forEach(function (_site) {
      var keyValue = _site.value.split(/\n+/);
      if (keyValue) {
        keyValue.forEach(function (_keyValue) {
          console.log("The value of the keyValue is: " + _keyValue.trim());
          _keyValue = _keyValue.trim();
          var key = _keyValue.match(/(.+)=/);
          var value = _keyValue.match(/=(.*)/);
          if (key) {
            this.setSiteProperty(key[1], value[1], _site.name + '.xml');
          }

        }, this);
      }
    }, this);
  },

  /**
   * Set property of the site variable
   */
  setSiteProperty: function (key, value, filename) {
    if (this.get('configs').someProperty('name', key)) {
      this.get('configs').findProperty('name', key).value = value;
    } else {
      this.get('configs').pushObject({
        "id": "site property",
        "name": key,
        "value": value,
        "filename": filename
      });
    }
  },

  loadSlaveConfiguration: function () {

    var slaveComponentConfig = this.convertSlaveConfig(this.get('content.slaveGroupProperties'));
    this.set("slaveComponentConfig", slaveComponentConfig);
  },

  convertSlaveConfig: function (slaveContent) {
    var dest = {
      "version": "1.0",
      "components": [
      ],
      "slaveHostComponents": []
    };

    slaveContent.forEach(function (_slaveContent) {
      var newComponent = {};
      newComponent.componentName = _slaveContent.componentName;
      newComponent.serviceName = this.getServiceInfo(newComponent.componentName).name;
      newComponent.groups = [];
      var index = 2;
      if (_slaveContent.groups) {
        _slaveContent.groups.forEach(function (_group) {
          var newGroup = {};
          newGroup.groupName = _group.name;
          newGroup.configVersion = {config: {'global': 'version1', 'core-site': 'version1'}}; // TODO : every time a new version should be generated
          if (this.getServiceInfo(_slaveContent.componentName)) {
            newGroup.configVersion.config[this.getServiceInfo(_slaveContent.componentName).domain] = 'version' + index;
            newGroup.configVersion.config[this.getServiceInfo(_slaveContent.componentName).siteName] = 'version' + index;
          }
          newGroup.siteVersion = 'version' + index;
          newGroup.hostNames = _slaveContent.hosts.filterProperty("group", newGroup.groupName).mapProperty('hostName');
          newGroup.properties = _group.properties;
          if (!Ember.empty(newGroup.hostNames)) {
            newComponent.groups.push(newGroup);
          }
          index++;
        }, this);
      }
      dest.components.push(newComponent);
    }, this);
    var hostsInfo = this.get('content.hosts');

    for (var index in hostsInfo) {
      var hostIndex = 2;
      var slaveHost = {name: null, configVersion: null, slaveComponents: []};
      dest.components.forEach(function (_component) {
        _component.groups.forEach(function (_group) {
          if (_group.hostNames.contains(hostsInfo[index].name)) {
            var slaveComponent = {};
            slaveHost.name = hostsInfo[index].name;
            slaveComponent.componentName = _component.componentName;
            slaveComponent.groupName = _group.groupName;
            slaveComponent.properties = _group.properties;
            slaveHost.slaveComponents.pushObject(slaveComponent);
          }
        }, this);
      }, this);
      hostIndex++;
      if (!Ember.none(slaveHost.name)) {
        dest.slaveHostComponents.pushObject(slaveHost);
      }

    }
    return dest;
  },

  getServiceInfo: function (componentName) {
    var serviceConfig;
    switch (componentName) {
      case 'DATANODE':
        serviceConfig = {
          name: 'HDFS',
          siteName: 'hdfs-site',
          domain: 'datanode-global'
        };
        break;
      case 'TASKTRACKER':
        serviceConfig = {
          name: 'MAPREDUCE',
          siteName: 'mapred-site',
          domain: 'tasktracker-global'
        };
        break;
      case 'HBASE_REGIONSERVER':
        serviceConfig = {
          name: 'HBASE',
          siteName: 'hbase-site',
          domain: 'regionserver-global'
        };
        break;
      default:
        serviceConfig = {};
    }
    return serviceConfig;
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
    var repoOption = this.get('content.installOption.localRepo');
    var repoObj = this.rawContent.findProperty('config_name', 'Repo');
    if (repoOption) {
      repoObj.config_value = 'Yes';
    } else {
      repoObj.config_value = 'No';
    }
    this.get('clusterInfo').pushObject(Ember.Object.create(repoObj));
  },


  /**
   * Load all info about services to <code>services</code> variable
   */
  loadServices: function () {
    var selectedServices = this.get('selectedServices');
    this.set('services', selectedServices.mapProperty('serviceName'));

    selectedServices.forEach(function (_service) {
      console.log('INFO: step8: Name of the service from getService function: ' + _service.serviceName);
      var reviewService = this.rawContent.findProperty('config_name', 'services');
      var serviceObj = reviewService.config_value.findProperty('service_name', _service.serviceName);

      if (serviceObj) {
        switch (serviceObj.service_name) {
          case 'HDFS':
            this.loadHDFS(serviceObj);
            break;
          case 'MAPREDUCE':
            this.loadMapReduce(serviceObj);
            break;
          case 'HIVE':
            this.loadHive(serviceObj);
            break;
          case 'HBASE':
            this.loadHbase(serviceObj);
            break;
          case 'ZOOKEEPER':
            this.loadZk(serviceObj);
            break;
          case 'OOZIE':
            this.loadOozie(serviceObj);
            break;
          case 'NAGIOS':
            this.loadNagios(serviceObj);
            break;
          case 'GANGLIA':
            this.loadGanglia(serviceObj);
            break;
          /* case 'PIG':
           this.loadPig(serviceObj);
           break;
           case 'SQOOP':
           this.loadSqoop(serviceObj);
           break;
           */
          case 'HCATALOG':
            break;
          default:
        }
      }
    }, this);
  },

  /**
   * load all info about HDFS service
   * @param hdfsObj
   */
  loadHDFS: function (hdfsObj) {
    hdfsObj.get('service_components').forEach(function (_component) {
      switch (_component.get('display_name')) {
        case 'NameNode':
          this.loadNnValue(_component);
          break;
        case 'SecondaryNameNode':
          this.loadSnnValue(_component);
          break;
        case 'DataNodes':
          this.loadDnValue(_component);
          break;
        default:
      }
    }, this);
    //var
    this.get('services').pushObject(hdfsObj);
  },

  loadNnValue: function (nnComponent) {
    var nnHostName = this.get('content.masterComponentHosts').findProperty('display_name', nnComponent.display_name);
    nnComponent.set('component_value', nnHostName.hostName);
  },

  loadSnnValue: function (snnComponent) {
    var snnHostName = this.get('content.masterComponentHosts').findProperty('display_name', 'SNameNode');
    snnComponent.set('component_value', snnHostName.hostName);
  },

  loadDnValue: function (dnComponent) {
    var dnHosts = this.get('content.slaveComponentHosts').findProperty('displayName', 'DataNode');
    var totalDnHosts = dnHosts.hosts.length;
    /* var totalGroups = this.get('slaveComponentConfig.components').findProperty('componentName', 'DATANODE').groups.length;
     var groupLabel;
     if (totalGroups == 1) {
     groupLabel = 'group';
     } else {
     groupLabel = 'groups';
     }
     */
    dnComponent.set('component_value', totalDnHosts + ' hosts');
  },


  /**
   * Load all info about mapReduce service
   * @param mrObj
   */
  loadMapReduce: function (mrObj) {
    mrObj.get('service_components').forEach(function (_component) {
      switch (_component.get('display_name')) {
        case 'JobTracker':
          this.loadJtValue(_component);
          break;
        case 'TaskTrackers':
          this.loadTtValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(mrObj);
  },

  loadJtValue: function (jtComponent) {
    var jtHostName = this.get('content.masterComponentHosts').findProperty('display_name', jtComponent.display_name);
    jtComponent.set('component_value', jtHostName.hostName);
  },

  loadTtValue: function (ttComponent) {
    var ttHosts = this.get('content.slaveComponentHosts').findProperty('displayName', 'TaskTracker');
    var totalTtHosts = ttHosts.hosts.length;
    /* var totalGroups = this.get('slaveComponentConfig.components').findProperty('componentName', 'TASKTRACKER').groups.length;
     var groupLabel;
     if (totalGroups == 1) {
     groupLabel = 'group';
     } else {
     groupLabel = 'groups';
     }
     */
    ttComponent.set('component_value', totalTtHosts + ' hosts');
  },

  /**
   * Load all info about Hive service
   * @param hiveObj
   */
  loadHive: function (hiveObj) {
    hiveObj.get('service_components').forEach(function (_component) {
      switch (_component.get('display_name')) {
        case 'Hive Metastore':
          this.loadHiveMetaStoreValue(_component);
          break;
        case 'Database':
          this.loadHiveDbValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(hiveObj);

  },

  loadHiveMetaStoreValue: function (metaStoreComponent) {
    var hiveHostName = this.get('content.masterComponentHosts').findProperty('display_name', 'Hive Server');
    metaStoreComponent.set('component_value', hiveHostName.hostName);
  },

  loadHiveDbValue: function (dbComponent) {
    var hiveDb = App.db.getServiceConfigProperties().findProperty('name', 'hive_database');

    if (hiveDb.value === 'New MySQL Database') {

      dbComponent.set('component_value', 'MySQL (New Database)');

    } else {

      var db = App.db.getServiceConfigProperties().findProperty('name', 'hive_existing_database');

      dbComponent.set('component_value', db.value + ' (' + hiveDb.value + ')');

    }
  },

  /**
   * Load all info about Hbase
   * @param hbaseObj
   */
  loadHbase: function (hbaseObj) {
    hbaseObj.service_components.forEach(function (_component) {
      switch (_component.display_name) {
        case 'Master':
          this.loadMasterValue(_component);
          break;
        case 'Region Servers':
          this.loadRegionServerValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(hbaseObj);
  },

  loadMasterValue: function (hbaseMaster) {
    var hbaseHostName = this.get('content.masterComponentHosts').findProperty('display_name', 'HBase Master');
    hbaseMaster.set('component_value', hbaseHostName.hostName);
  },

  loadRegionServerValue: function (rsComponent) {
    var rsHosts = this.get('content.slaveComponentHosts').findProperty('displayName', 'RegionServer');
    var totalRsHosts = rsHosts.hosts.length;
    /* var totalGroups = this.get('slaveComponentConfig.components').findProperty('componentName', 'HBASE_REGIONSERVER').groups.length;
     var groupLabel;
     if (totalGroups == 1) {
     groupLabel = 'group';
     } else {
     groupLabel = 'groups';
     } */
    rsComponent.set('component_value', totalRsHosts + ' hosts');
  },

  /**
   * Load all info about ZooKeeper service
   * @param zkObj
   */
  loadZk: function (zkObj) {
    zkObj.get('service_components').forEach(function (_component) {
      switch (_component.get('display_name')) {
        case 'Servers':
          this.loadZkServerValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(zkObj);
  },

  loadZkServerValue: function (serverComponent) {
    var zkHostNames = this.get('content.masterComponentHosts').filterProperty('display_name', 'ZooKeeper').length;
    var hostSuffix;
    if (zkHostNames === 1) {
      hostSuffix = 'host';
    } else {
      hostSuffix = 'hosts';
    }
    serverComponent.set('component_value', zkHostNames + ' ' + hostSuffix);
  },

  /**
   * Load all info about Oozie services
   * @param oozieObj
   */
  loadOozie: function (oozieObj) {
    oozieObj.get('service_components').forEach(function (_component) {
      switch (_component.get('display_name')) {
        case 'Server':
          this.loadOozieServerValue(_component);
          break;
        case 'Database':
          // TODO: uncomment when ready to integrate with Oozie Database other than Derby
          // this.loadOozieDbValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(oozieObj);
  },

  loadOozieServerValue: function (oozieServer) {
    var oozieServerName = this.get('content.masterComponentHosts').findProperty('display_name', 'Oozie Server');
    oozieServer.set('component_value', oozieServerName.hostName);
  },

  loadOozieDbValue: function (dbComponent) {
    var oozieDb = App.db.getServiceConfigProperties().findProperty('name', 'oozie_database');
    if (oozieDb.value === 'New PostgreSQL Database') {
      dbComponent.set('component_value', 'PostgreSQL (New Database)');
    } else {
      var db = App.db.getServiceConfigProperties().findProperty('name', 'oozie_existing_database');
      dbComponent.set('component_value', db.value + ' (' + oozieDb.value + ')');
    }
  },


  /**
   * Load all info about Nagios service
   * @param nagiosObj
   */
  loadNagios: function (nagiosObj) {
    nagiosObj.service_components.forEach(function (_component) {
      switch (_component.display_name) {
        case 'Server':
          this.loadNagiosServerValue(_component);
          break;
        case 'Administrator':
          this.loadNagiosAdminValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(nagiosObj);
  },

  loadNagiosServerValue: function (nagiosServer) {
    var nagiosServerName = this.get('content.masterComponentHosts').findProperty('display_name', 'Nagios Server');
    nagiosServer.set('component_value', nagiosServerName.hostName);
  },

  loadNagiosAdminValue: function (nagiosAdmin) {
    var config = this.get('content.serviceConfigProperties');
    var adminLoginName = config.findProperty('name', 'nagios_web_login');
    var adminEmail = config.findProperty('name', 'nagios_contact');
    nagiosAdmin.set('component_value', adminLoginName.value + ' / (' + adminEmail.value + ')');
  },

  /**
   * Load all info about ganglia
   * @param gangliaObj
   */
  loadGanglia: function (gangliaObj) {
    gangliaObj.get('service_components').forEach(function (_component) {
      switch (_component.get('display_name')) {
        case 'Server':
          this.loadGangliaServerValue(_component);
          break;
        default:
      }
    }, this);
    this.get('services').pushObject(gangliaObj);
  },

  loadGangliaServerValue: function (gangliaServer) {
    var gangliaServerName = this.get('content.masterComponentHosts').findProperty('display_name', 'Ganglia Collector');
    gangliaServer.set('component_value', gangliaServerName.hostName);
  },

  loadSqoop: function (sqoopObj) {
    this.get('services').pushObject(sqoopObj);
  },

  loadPig: function (pigObj) {
    this.get('services').pushObject(pigObj);
  },

  /**
   * Onclick handler for <code>next</code> button
   */
  submit: function () {

    if (this.get('isSubmitDisabled')) {
      return;
    }

    this.set('isSubmitDisabled', true);

    if (App.testMode || !this.get('content.cluster.requestId')) {
      // For recovery : set the cluster status

      // We need to do recovery based on whether we are in Add Host or Installer wizard
      switch (this.get('content.controllerName')) {
        case 'installerController' :
          App.clusterStatus.set('value', {
            clusterName: this.get('clusterName'),
            clusterState: 'CLUSTER_DEPLOY_PREP_2',
            wizardControllerName: this.get('content.controllerName'),
            localdb: App.db.data
          });
          break;

        case 'addHostController' :
          App.clusterStatus.set('value', {
            clusterName: this.get('clusterName'),
            clusterState: 'ADD_HOSTS_DEPLOY_PREP_2',
            wizardControllerName: this.get('content.controllerName'),
            localdb: App.db.data
          });
          break;
        default :
          break;
      }

      this.createCluster();
      this.createSelectedServices();
      //this.setAmbariUIDb();
      this.createConfigurations();
      this.applyCreatedConfToServices();
      this.createComponents();
      this.registerHostsToCluster();
      this.createAllHostComponents();
      //this.applyCreatedConfToSlaveGroups();
      this.ajaxQueueFinished = function () {
        console.log('everything is loaded')
        App.router.send('next');
      };
      this.doNextAjaxCall();
    } else {
      App.router.send('next');
    }
  },

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

  /**
   *  The following create* functions are called upon submitting Step 8.
   */

  createCluster: function () {

    if (this.get('content.isWizard')) {
      return false;
    }

    var clusterName = this.get('clusterName');
    var url = App.apiPrefix + '/clusters/' + clusterName;

    var stackVersion = (this.get('content.installOptions.localRepo')) ? App.defaultLocalStackVersion : App.defaultStackVersion;

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
      }

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
    var clientHosts = slaveHosts.filterProperty('componentName', "CLIENT").objectAt(0).hosts;

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
        this.get('content.clients').forEach(function (_client) {
          if (!_client.isInstalled) {
            var hostNames = clientHosts.mapProperty('hostName').splice(0);
            switch (_client.component_name) {
              case 'HDFS_CLIENT':
                // install HDFS_CLIENT on HBASE_MASTER, HBASE_REGIONSERVER, and WEBHCAT_SERVER hosts
                masterHosts.filterProperty('component', 'HBASE_MASTER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                  hostNames.pushObject(_masterHost.hostName);
                }, this);
                masterHosts.filterProperty('component', 'HBASE_REGIONSERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                  hostNames.pushObject(_masterHost.hostName);
                }, this);
                masterHosts.filterProperty('component', 'WEBHCAT_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
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
              case 'HCAT':
                // install HCAT (client) on NAGIOS_SERVER host
                masterHosts.filterProperty('component', 'NAGIOS_SERVER').filterProperty('isInstalled', false).forEach(function (_masterHost) {
                  hostNames.pushObject(_masterHost.hostName);
                }, this);
                break;
            }
            hostNames = hostNames.uniq();
            this.registerHostsToComponent(hostNames, _client.component_name);
          }
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
      this.registerHostsToComponent(masterHosts.filterProperty('component', 'HIVE_SERVER').mapProperty('hostName'), 'MYSQL_SERVER');
    }
  },

  registerHostsToComponent: function (hostNames, componentName) {

    if (hostNames.length == 0) {
      return;
    }
    console.log('registering ' + componentName + ' to ' + JSON.stringify(hostNames));

    // currently we are specifying the predicate as a query string.
    // this can hit a ~4000-character limit in Jetty server.
    // chunk to multiple calls if needed
    // var hostsPredicate = hostNames.map(function (hostName) {
    //   return 'Hosts/host_name=' + hostName;
    // }).join('|');

    var queryStrArr = [];
    var queryStr = '';
    hostNames.forEach(function (hostName) {
      queryStr += 'Hosts/host_name=' + hostName + '|';
      if (queryStr.length > 3500) {
        queryStrArr.push(queryStr.slice(0, -1));
        queryStr = '';
      }
    });

    if (queryStr.length > 0) {
      queryStrArr.push(queryStr.slice(0, -1));
    }

    queryStrArr.forEach(function (queryStr) {
      // console.log('creating host components for ' + queryStr);
      var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/hosts?' + queryStr;
      var data = {
        "host_components": [
          {
            "HostRoles": {
              "component_name": componentName
            }
          }
        ]
      };

      this.ajax({
        type: 'POST',
        url: url,
        data: JSON.stringify(data),
        beforeSend: function () {
          console.log('BeforeSend: registerHostsToComponent for ' + queryStr + ' and component ' + componentName);
        }
      });
    }, this);
  },

  createConfigurations: function () {
    var selectedServices = this.get('selectedServices');
    if (!this.get('content.isWizard')) {
      this.createConfigSiteForService(this.createGlobalSiteObj());
      // this.createGlobalSitePerSlaveGroup();
      this.createConfigSiteForService(this.createCoreSiteObj());
      this.createConfigSiteForService(this.createHdfsSiteObj());
      //this.createHdfsSitePerSlaveGroup('HDFS');
    }
    if (selectedServices.someProperty('serviceName', 'MAPREDUCE')) {
      this.createConfigSiteForService(this.createMrSiteObj());
      //this.createMrSitePerSlaveGroup('MAPREDUCE');
    }
    if (selectedServices.someProperty('serviceName', 'HBASE')) {
      this.createConfigSiteForService(this.createHbaseSiteObj());
      //this.createHbaseSitePerSlaveGroup('HBASE');
    }
    if (selectedServices.someProperty('serviceName', 'OOZIE')) {
      this.createConfigSiteForService(this.createOozieSiteObj('OOZIE'));
    }
    if (selectedServices.someProperty('serviceName', 'HIVE')) {
      this.createConfigSiteForService(this.createHiveSiteObj('HIVE'));
    }
    if (selectedServices.someProperty('serviceName', 'WEBHCAT')) {
      this.createConfigSiteForService(this.createWebHCatSiteObj('WEBHCAT'));
    }
  },

  createConfigSiteForService: function (data) {
    console.log("Inside createConfigSiteForService");

    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/configurations';

    this.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data),
      beforeSend: function () {
        console.log("BeforeSend: createConfigSiteForService for " + data.type);
      }
    });
  },

  createGlobalSiteObj: function () {
    var globalSiteProperties = {};
    //this.get('globals').filterProperty('domain', 'global').forEach(function (_globalSiteObj) {
    this.get('globals').forEach(function (_globalSiteObj) {
      // do not pass any globals whose name ends with _host or _hosts
      if (!/_hosts?$/.test(_globalSiteObj.name)) {
        // append "m" to JVM memory options
        var value = null;
        if (/_heapsize|_newsize|_maxnewsize$/.test(_globalSiteObj.name)) {
          value = _globalSiteObj.value + "m";
          globalSiteProperties[_globalSiteObj.name] = value;
        } else {
          globalSiteProperties[_globalSiteObj.name] = _globalSiteObj.value;
        }
        console.log("STEP8: name of the global property is: " + _globalSiteObj.name);
        console.log("STEP8: value of the global property is: " + _globalSiteObj.value);
      }
      if (_globalSiteObj.name == 'java64_home') {
        globalSiteProperties['java64_home'] = this.get('content.installOptions.javaHome');
      }
    }, this);
    return {"type": "global", "tag": "version1", "properties": globalSiteProperties};
  },

  createGlobalSitePerSlaveGroup: function () {
    this.get('slaveComponentConfig.components').forEach(function (_component) {
      _component.groups.forEach(function (_group) {
        var globalSiteProperties = {};
        var properties = _group.properties;
        properties.forEach(function (_property) {
          if (!/_hosts?$/.test(_property.name)) {
            // append "m" to JVM memory options
            var value = null;
            if (/_heapsize|_newsize|_maxnewsize$/.test(_property.name)) {
              value = _property.value + "m";
              globalSiteProperties[_property.name] = value;
            } else {
              globalSiteProperties[_property.name] = _property.storeValue;
            }
            console.log("STEP8: name of the global property is: " + _property.name);
            console.log("STEP8: value of the global property is: " + _property.storeValue);
          }
        }, this);
        var config = _group.configVersion.config;
        for (var index in config) {
          if (index === 'datanode-global' || index === 'tasktracker-global' || index === 'regionserver-global') {
            var data = {"type": index, "tag": config[index], "properties": globalSiteProperties};
            this.createConfigSiteForService(data);
          }
        }
      }, this);
    }, this);
  },

  createCoreSiteObj: function () {
    var coreSiteObj = this.get('configs').filterProperty('filename', 'core-site.xml');
    var coreSiteProperties = {};
    // hadoop.proxyuser.oozie.hosts needs to be skipped if oozie is not selected
    var isOozieSelected = this.get('selectedServices').someProperty('serviceName', 'OOZIE');
    coreSiteObj.forEach(function (_coreSiteObj) {
      if (isOozieSelected || _coreSiteObj.name != 'hadoop.proxyuser.oozie.hosts') {
        coreSiteProperties[_coreSiteObj.name] = _coreSiteObj.value;
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
      hdfsProperties[_configProperty.name] = _configProperty.value;
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {"type": "hdfs-site", "tag": "version1", "properties": hdfsProperties };
  },

  createHdfsSitePerSlaveGroup: function (serviceName) {
    var hdfsSite = this.createHdfsSiteObj();
    var component = this.get('slaveComponentConfig.components').findProperty('serviceName', serviceName);
    component.groups.forEach(function (_group) {
      var siteProperties = hdfsSite.properties;
      _group.properties.forEach(function (_property) {
        this.get('configMapping').forEach(function (_config) {
          if (_config.templateName.contains(_property.name)) {
            this.get('globals').findProperty('name', _property.name).value = _property.storeValue;
            var value = this.getGlobConfigValue(_config.templateName, _config.value);
            if (siteProperties[_config.name]) {
              siteProperties[_config.name] = value;
            }
          }
        }, this);
      }, this);
      var data = {"type": hdfsSite.type, "tag": _group.siteVersion, "properties": siteProperties};
      console.log("The value of globalConfig is: " + JSON.stringify(siteProperties));
      this.createConfigSiteForService(data);
    }, this);
  },

  createMrSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'mapred-site.xml');
    var mrProperties = {};
    configs.forEach(function (_configProperty) {
      mrProperties[_configProperty.name] = _configProperty.value;
      console.log("STEP*: name of the property is: " + _configProperty.name);
      console.log("STEP8: value of the property is: " + _configProperty.value);
    }, this);
    return {type: 'mapred-site', tag: 'version1', properties: mrProperties};
  },

  createMrSitePerSlaveGroup: function (serviceName) {
    var mrSite = this.createMrSiteObj();
    var component = this.get('slaveComponentConfig.components').findProperty('serviceName', serviceName);
    component.groups.forEach(function (_group) {
      var siteProperties = mrSite.properties;
      _group.properties.forEach(function (_property) {
        this.get('configMapping').forEach(function (_config) {
          if (_config.templateName.contains(_property.name)) {
            this.get('globals').findProperty('name', _property.name).value = _property.storeValue;
            var value = this.getGlobConfigValue(_config.templateName, _config.value);
            if (siteProperties[_config.name]) {
              siteProperties[_config.name] = value;
            }
          }
        }, this);
      }, this);
      var data = {"type": mrSite.type, "tag": _group.siteVersion, "properties": siteProperties};
      this.createConfigSiteForService(data);
    }, this);
  },

  createHbaseSiteObj: function () {
    var configs = this.get('configs').filterProperty('filename', 'hbase-site.xml');
    var hbaseProperties = {};
    configs.forEach(function (_configProperty) {
      hbaseProperties[_configProperty.name] = _configProperty.value;
    }, this);
    var masterHosts = App.db.getMasterComponentHosts();
    // TODO: should filter on "component" but that gives unexpected results
    var zkServers = masterHosts.filterProperty('display_name', 'ZooKeeper').mapProperty('hostName');
    hbaseProperties['hbase.zookeeper.quorum'] = zkServers.join(',');
    return {type: 'hbase-site', tag: 'version1', properties: hbaseProperties};
  },

  createHbaseSitePerSlaveGroup: function (serviceName) {
    var hbaseSite = this.createHbaseSiteObj();
    var component = this.get('slaveComponentConfig.components').findProperty('serviceName', serviceName);
    component.groups.forEach(function (_group) {
      var siteProperties = hbaseSite.properties;
      _group.properties.forEach(function (_property) {
        this.get('configMapping').forEach(function (_config) {
          if (_config.templateName.contains(_property.name)) {
            this.get('globals').findProperty('name', _property.name).value = _property.storeValue;
            var value = this.getGlobConfigValue(_config.templateName, _config.value);
            if (siteProperties[_config.name]) {
              siteProperties[_config.name] = value;
            }
          }
        }, this);
      }, this);
      var data = {"type": hbaseSite.type, "tag": _group.siteVersion, "properties": siteProperties};
      this.createConfigSiteForService(data);
    }, this);
  },

  createOozieSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'oozie-site.xml');
    var oozieProperties = {};
    configs.forEach(function (_configProperty) {
      oozieProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'oozie-site', tag: 'version1', properties: oozieProperties};
  },

  createHiveSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'hive-site.xml');
    var hiveProperties = {};
    configs.forEach(function (_configProperty) {
      hiveProperties[_configProperty.name] = _configProperty.value;
    }, this);
    hiveProperties['hive.metastore.uris'] = 'thrift://' + this.get('globals').findProperty('name', 'hivemetastore_host').value + ':9083';
    hiveProperties['javax.jdo.option.ConnectionURL'] =
      'jdbc:mysql://' + this.get('globals').findProperty('name', 'hive_mysql_hostname').value +
        '/' + this.get('globals').findProperty('name', 'hive_database_name').value + '?createDatabaseIfNotExist=true';
    return {type: 'hive-site', tag: 'version1', properties: hiveProperties};
  },

  createWebHCatSiteObj: function (serviceName) {
    var configs = this.get('configs').filterProperty('filename', 'webhcat-site.xml');
    var webHCatProperties = {};
    configs.forEach(function (_configProperty) {
      webHCatProperties[_configProperty.name] = _configProperty.value;
    }, this);
    return {type: 'webhcat-site', tag: 'version1', properties: webHCatProperties};
  },

  applyCreatedConfToServices: function () {
    var services = this.get('selectedServices').mapProperty('serviceName');
    services.forEach(function (_service) {
      var data = this.getConfigForService(_service);
      this.applyCreatedConfToService(_service, 'PUT', data);
    }, this);
  },

  applyCreatedConfToService: function (service, httpMethod, data) {
    console.log("Inside applyCreatedConfToService");

    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/services/' + service;

    this.ajax({
      type: httpMethod,
      url: url,
      data: JSON.stringify(data),
      beforeSend: function () {
        console.log("BeforeSend: applyCreatedConfToService for " + service);
      }
    });
  },

  applyCreatedConfToSlaveGroups: function () {
    this.get('slaveComponentConfig.components').forEach(function (_component) {
      _component.groups.forEach(function (_group) {
        var aggregatedHostNames = '';
        _group.hostNames.forEach(function (_hostName, index) {
          aggregatedHostNames += 'HostRoles/host_name=' + _hostName;
          if (index !== _group.hostNames.length - 1) {
            aggregatedHostNames += '|';
          }
        }, this);
        console.log("The aggregated hostNames value is: " + aggregatedHostNames);
        this.applyCreatedConfToSlaveGroup(aggregatedHostNames, 'PUT', _group.configVersion, _group.groupName);
      }, this);
    }, this);
  },

  applyCreatedConfToSlaveGroup: function (aggregatedHostNames, httpMethod, data, groupName) {
    console.log("Inside applyCreatedConfToHost");
    var url = App.apiPrefix + '/clusters/' + this.get('clusterName') + '/host_components?' + aggregatedHostNames;

    this.ajax({
      type: httpMethod,
      url: url,
      data: JSON.stringify(data),
      beforeSend: function () {
        console.log("BeforeSend: applyCreatedConfToSlaveGroup for group: " + groupName);
      }
    });
  },

  getConfigForService: function (serviceName) {
    switch (serviceName) {
      case 'HDFS':
        return {config: {'global': 'version1', 'core-site': 'version1', 'hdfs-site': 'version1'}};
      case 'MAPREDUCE':
        return {config: {'global': 'version1', 'core-site': 'version1', 'mapred-site': 'version1'}};
      case 'HBASE':
        return {config: {'global': 'version1', 'hbase-site': 'version1'}};
      case 'OOZIE':
        return {config: {'global': 'version1', 'oozie-site': 'version1'}};
      case 'HIVE':
        return {config: {'global': 'version1', 'hive-site': 'version1'}};
      case 'WEBHCAT':
        return {config: {'global': 'version1', 'webhcat-site': 'version1'}};
      default:
        return {config: {'global': 'version1'}};
    }
  },

  ajaxQueue: [],

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

    this.set('ajaxBusy', true);
    console.log('AJAX send ' + first.url);
    $.ajax(first);

  },

  registerErrPopup: function (header, message) {

    App.ModalPopup.show({
      header: header,
      secondary: false,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['<p>{{view.message}}</p>'].join('\n')),
        message: message
      })
    });
  },

  /**
   * We need to do a lot of ajax calls(about 10 or more) async in special order.
   * To do this i generate array of ajax objects and then send requests step by step.
   * All ajax objects are stored in <code>ajaxQueue</code>
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
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: STep8 -> In success function");
        console.log("TRACE: STep8 -> value of the url is: " + params.url);
        console.log("TRACE: STep8 -> value of the received data is: " + jsonData);
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
    }

    params.error = function (xhr, status, error) {
      var responseText = JSON.parse(xhr.responseText);
      self.registerErrPopup("Error", responseText.message);
      self.set('isSubmitDisabled', true);
      self.set('hasErrorOccurred', true);
    }
    this.get('ajaxQueue').pushObject(params);
  }

})





  
  
