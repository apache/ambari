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
 * Additional data that is used in the `Move Component Initializers`
 *
 * @typedef {object} reassignComponentDependencies
 * @property {string} sourceHostName host where component was before moving
 * @property {string} targetHostName host where component will be after moving
 */

App.ReassignMasterWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "reassignMasterWizardStep4Controller",

  commands: [
    'stopRequiredServices',
    'cleanMySqlServer',
    'createHostComponents',
    'putHostComponentsInMaintenanceMode',
    'reconfigure',
    'installHostComponents',
    'startZooKeeperServers',
    'startNameNode',
    'deleteHostComponents',
    'configureMySqlServer',
    'startMySqlServer',
    'startNewMySqlServer',
    'startRequiredServices'
  ],

  // custom commands for Components with DB Configuration and Check
  commandsForDB: [
    'createHostComponents',
    'installHostComponents',
    'configureMySqlServer',
    'restartMySqlServer',
    'testDBConnection',
    'stopRequiredServices',
    'cleanMySqlServer',
    'putHostComponentsInMaintenanceMode',
    'reconfigure',
    'deleteHostComponents',
    'configureMySqlServer',
    'startRequiredServices'
  ],

  clusterDeployState: 'REASSIGN_MASTER_INSTALLING',

  multiTaskCounter: 0,

  hostComponents: [],

  /**
   * List of components, that do not need reconfiguration for moving to another host
   * Reconfigure command will be skipped
   */
  componentsWithoutReconfiguration: ['METRICS_COLLECTOR'],

  /**
   * Map with lists of unrelated services.
   * Used to define list of services to stop/start.
   */
  unrelatedServicesMap: {
    'JOBTRACKER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'RESOURCEMANAGER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'APP_TIMELINE_SERVER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'OOZIE_SERVER': ['ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM', 'HIVE'],
    'WEBHCAT_SERVER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'HIVE_SERVER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'HIVE_METASTORE': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'MYSQL_SERVER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM']
  },

  dbPropertyMap: {
    'HIVE_SERVER': 'javax.jdo.option.ConnectionDriverName',
    'HIVE_METASTORE': 'javax.jdo.option.ConnectionDriverName',
    'OOZIE_SERVER': 'oozie.service.JPAService.jdbc.url'
  },

  /**
   * additional configs with template values
   * Part of value to substitute has following format: "<replace-value>"
   */
  additionalConfigsMap: [
    {
      componentName: 'RESOURCEMANAGER',
      configs: {
        'yarn-site': {
          'yarn.resourcemanager.address': '<replace-value>:8050',
          'yarn.resourcemanager.admin.address': '<replace-value>:8141',
          'yarn.resourcemanager.resource-tracker.address': '<replace-value>:8025',
          'yarn.resourcemanager.scheduler.address': '<replace-value>:8030',
          'yarn.resourcemanager.webapp.address': '<replace-value>:8088',
          'yarn.resourcemanager.hostname': '<replace-value>'
        }
      }
    },
    {
      componentName: 'JOBTRACKER',
      configs: {
        'mapred-site': {
          'mapred.job.tracker.http.address': '<replace-value>:50030',
          'mapred.job.tracker': '<replace-value>:50300'
        }
      }
    },
    {
      componentName: 'SECONDARY_NAMENODE',
      configs: {
        'hdfs-site': {
          'dfs.secondary.http.address': '<replace-value>:50090'
        }
      },
      configs_Hadoop2: {
        'hdfs-site': {
          'dfs.namenode.secondary.http-address': '<replace-value>:50090'
        }
      }
    },
    {
      componentName: 'NAMENODE',
      configs: {
        'hdfs-site': {
          'dfs.http.address': '<replace-value>:50070',
          'dfs.https.address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.default.name': 'hdfs://<replace-value>:8020'
        }
      },
      configs_Hadoop2: {
        'hdfs-site': {
          'dfs.namenode.rpc-address': '<replace-value>:8020',
          'dfs.namenode.http-address': '<replace-value>:50070',
          'dfs.namenode.https-address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.defaultFS': 'hdfs://<replace-value>:8020'
        }
      }
    },
    {
      componentName: 'APP_TIMELINE_SERVER',
      configs: {
        'yarn-site': {
          'yarn.timeline-service.webapp.address': '<replace-value>:8188',
          'yarn.timeline-service.webapp.https.address': '<replace-value>:8190',
          'yarn.timeline-service.address': '<replace-value>:10200'
        }
      }
    },
    {
      componentName: 'OOZIE_SERVER',
        configs: {
          'oozie-site': {
            'oozie.base.url': 'http://<replace-value>:11000/oozie'
          },
          'core-site': {
            'hadoop.proxyuser.oozie.hosts': '<replace-value>'
          }
        }
    },
    {
      componentName: 'HIVE_METASTORE',
      configs: {
        'hive-site': {}
      }
    },
    {
      componentName: 'MYSQL_SERVER',
      configs: {
        'hive-site': {
          'javax.jdo.option.ConnectionURL': 'jdbc:mysql://<replace-value>/hive?createDatabaseIfNotExist=true'
        }
      }
    },
    {
      componentName: 'HISTORYSERVER',
      configs: {
        'mapred-site': {
          'mapreduce.jobhistory.webapp.address': '<replace-value>:19888',
          'mapreduce.jobhistory.address': '<replace-value>:10020'
        }
      }
    }
  ],

  secureConfigsMap: [
    {
      componentName: 'NAMENODE',
      configs: [
        {
          site: 'hdfs-site',
          keytab: 'dfs.namenode.keytab.file',
          principal: 'dfs.namenode.kerberos.principal'
        },
        {
          site: 'hdfs-site',
          keytab: 'dfs.web.authentication.kerberos.keytab',
          principal: 'dfs.web.authentication.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'SECONDARY_NAMENODE',
      configs: [
        {
          site: 'hdfs-site',
          keytab: 'dfs.secondary.namenode.keytab.file',
          principal: 'dfs.secondary.namenode.kerberos.principal'
        },
        {
          site: 'hdfs-site',
          keytab: 'dfs.web.authentication.kerberos.keytab',
          principal: 'dfs.web.authentication.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'RESOURCEMANAGER',
      configs: [
        {
          site: 'yarn-site',
          keytab: 'yarn.resourcemanager.keytab',
          principal: 'yarn.resourcemanager.principal'
        },
        {
          site: 'yarn-site',
          keytab: 'yarn.resourcemanager.webapp.spnego-keytab-file',
          principal: 'yarn.resourcemanager.webapp.spnego-principal'
        }
      ]
    },
    {
      componentName: 'OOZIE_SERVER',
      configs: [
        {
          site: 'oozie-site',
          keytab: 'oozie.authentication.kerberos.keytab',
          principal: 'oozie.authentication.kerberos.principal'
        },
        {
          site: 'oozie-site',
          keytab: 'oozie.service.HadoopAccessorService.keytab.file',
          principal: 'oozie.service.HadoopAccessorService.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'WEBHCAT_SERVER',
      configs: [
        {
          site: 'webhcat-site',
          keytab: 'templeton.kerberos.keytab',
          principal: 'templeton.kerberos.principal'
        }
      ]
    },
    {
      componentName: 'HIVE_SERVER',
      configs: [
        {
          site: 'hive-site',
          keytab: 'hive.server2.authentication.kerberos.keytab',
          principal: 'hive.server2.authentication.kerberos.principal'
        },
        {
          site: 'hive-site',
          keytab: 'hive.server2.authentication.spnego.keytab',
          principal: 'hive.server2.authentication.spnego.principal'
        }
      ]
    },
    {
      componentName: 'HIVE_METASTORE',
      configs: [
        {
          site: 'hive-site',
          keytab: 'hive.metastore.kerberos.keytab.file',
          principal: 'hive.metastore.kerberos.principal'
        }
      ]
    }

  ],

  /**
   * set additional configs
   * configs_Hadoop2 - configs which belongs to Hadoop 2 stack only
   * @param configs
   * @param componentName
   * @param replaceValue
   * @return {Boolean}
   */
  setAdditionalConfigs: function (configs, componentName, replaceValue) {
    var component = this.get('additionalConfigsMap').findProperty('componentName', componentName);

    if (Em.isNone(component)) return false;
    var additionalConfigs = (component.configs_Hadoop2) ? component.configs_Hadoop2 : component.configs;

    for (var site in additionalConfigs) {
      if (additionalConfigs.hasOwnProperty(site)) {
        for (var property in additionalConfigs[site]) {
          if (additionalConfigs[site].hasOwnProperty(property)) {
            if (App.get('isHaEnabled') && componentName === 'NAMENODE' && (property === 'fs.defaultFS' || property === 'dfs.namenode.rpc-address')) continue;

            configs[site][property] = additionalConfigs[site][property].replace('<replace-value>', replaceValue);
          }
        }
      }
    }
    return true;
  },

  /**
   * load step info
   */
  loadStep: function () {
    if (this.get('content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled')) {
      this.set('hostComponents', ['NAMENODE', 'ZKFC']);
    } else {
      this.set('hostComponents', [this.get('content.reassign.component_name')]);
    }
    this.set('serviceName', [this.get('content.reassign.service_id')]);
    this._super();
  },

  /**
   * concat host-component names into string
   * @return {String}
   */
  getHostComponentsNames: function () {
    var hostComponentsNames = '';
    this.get('hostComponents').forEach(function (comp, index) {
      hostComponentsNames += index ? '+' : '';
      hostComponentsNames += comp === 'ZKFC' ? comp : App.format.role(comp, false);
    }, this);
    return hostComponentsNames;
  },

  /**
   * remove unneeded tasks
   */
  removeUnneededTasks: function () {
    var componentName = this.get('content.reassign.component_name');
    if (this.isComponentWithDB()) {
      var db_type = this.get('content.databaseType');
      var is_remote_db = this.get('content.serviceProperties.is_remote_db');


      if (is_remote_db || db_type !== 'mysql') {
        this.removeTasks(['configureMySqlServer', 'startMySqlServer', 'restartMySqlServer', 'cleanMySqlServer', 'configureMySqlServer']);
      }

      if (db_type === 'derby') {
        this.removeTasks(['testDBConnection']);
      }
    }

    if (componentName !== 'MYSQL_SERVER' && !this.isComponentWithDB()) {
      this.removeTasks(['configureMySqlServer', 'startMySqlServer', 'restartMySqlServer', 'cleanMySqlServer', 'startNewMySqlServer', 'configureMySqlServer']);
    }

    if (componentName === 'MYSQL_SERVER') {
      this.removeTasks(['cleanMySqlServer']);
    }

    if (this.get('content.hasManualSteps')) {
      if (componentName === 'NAMENODE' && App.get('isHaEnabled')) {
        // Only for reassign NameNode with HA enabled
        this.removeTasks(['deleteHostComponents', 'startRequiredServices']);
      } else {
        this.removeTasks(['startZooKeeperServers', 'startNameNode', 'deleteHostComponents', 'startRequiredServices']);
      }
    } else {
      this.removeTasks(['startZooKeeperServers', 'startNameNode']);
    }

    if (this.get('componentsWithoutReconfiguration').contains(componentName)) {
      this.removeTasks(['reconfigure']);
    }
  },

  /**
   * remove tasks by command name
   */
  removeTasks: function(commands) {
    var tasks = this.get('tasks');

    commands.forEach(function(command) {
      var cmd = tasks.filterProperty('command', command);
      var index = null;

      if (cmd.length === 0) {
        return false;
      } else {
        index = tasks.indexOf( cmd[0] );
      }

      tasks.splice( index, 1 );
    });
  },

  /**
   * initialize tasks
   */
  initializeTasks: function () {
    var commands = this.get('commands');
    var currentStep = App.router.get('reassignMasterController.currentStep');
    var hostComponentsNames = this.getHostComponentsNames();

    if (this.isComponentWithDB()) {
      commands = this.get('commandsForDB');
    }

    for (var i = 0; i < commands.length; i++) {
      var TaskLabel = i === 3 ? this.get('serviceName') : hostComponentsNames; //For Reconfigure task, show serviceName
      var title = Em.I18n.t('services.reassign.step4.tasks.' + commands[i] + '.title').format(TaskLabel);
      this.get('tasks').pushObject(Ember.Object.create({
        title: title,
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        name: title,
        displayName: title,
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }
    this.removeUnneededTasks();
    this.set('isLoaded', true);
  },

  hideRollbackButton: function () {
    var failedTask = this.get('tasks').findProperty('showRollback');
    if (failedTask) {
      failedTask.set('showRollback', false);
    }
  }.observes('tasks.@each.showRollback'),

  onComponentsTasksSuccess: function () {
    this.incrementProperty('multiTaskCounter');
    if (this.get('multiTaskCounter') >= this.get('hostComponents').length) {
      this.onTaskCompleted();
    }
  },

  /**
   * make server call to stop services
   */
  stopRequiredServices: function () {
    this.stopServices(this.get('unrelatedServicesMap')[this.get('content.reassign.component_name')]);
  },

  createHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.target');
    for (var i = 0; i < hostComponents.length; i++) {
      this.createComponent(hostComponents[i], hostName, this.get('content.reassign.service_id'));
    }
  },

  onCreateComponent: function () {
    this.onComponentsTasksSuccess();
  },

  putHostComponentsInMaintenanceMode: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.host.host_component.passive',
        sender: this,
        data: {
          hostName: hostName,
          passive_state: "ON",
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onTaskError'
      });
    }
  },

  installHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.target');
    for (var i = 0; i < hostComponents.length; i++) {
      this.updateComponent(hostComponents[i], hostName, this.get('content.reassign.service_id'), "Install", hostComponents.length);
    }
  },

  reconfigure: function () {
    this.loadConfigsTags();
  },

  loadConfigsTags: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },

  serviceToConfigSiteMap: {
    'NAMENODE': ['hdfs-site', 'core-site'],
    'SECONDARY_NAMENODE': ['hdfs-site', 'core-site'],
    'JOBTRACKER': ['mapred-site'],
    'RESOURCEMANAGER': ['yarn-site'],
    'WEBHCAT_SERVER': ['webhcat-site'],
    'APP_TIMELINE_SERVER': ['yarn-site', 'yarn-env'],
    'OOZIE_SERVER': ['oozie-site', 'core-site', 'oozie-env'],
    'HIVE_SERVER': ['hive-site', 'webhcat-site', 'hive-env', 'core-site'],
    'HIVE_METASTORE': ['hive-site', 'webhcat-site', 'hive-env', 'core-site'],
    'MYSQL_SERVER': ['hive-site'],
    'HISTORYSERVER': ['mapred-site']
  },

  /**
   * construct URL parameters for config call
   * @param componentName
   * @param data
   * @return {Array}
   */
  getConfigUrlParams: function (componentName, data) {
    var urlParams = [];

    this.get('serviceToConfigSiteMap')[componentName].forEach(function(site){
      urlParams.push('(type=' + site + '&tag=' + data.Clusters.desired_configs[site].tag + ')');
    });

    // specific cases for NameNode component
    if (componentName === 'NAMENODE') {
        if (App.Service.find().someProperty('serviceName', 'HBASE')) {
          urlParams.push('(type=hbase-site&tag=' + data.Clusters.desired_configs['hbase-site'].tag + ')');
        }
        if (App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
          urlParams.push('(type=accumulo-site&tag=' + data.Clusters.desired_configs['accumulo-site'].tag + ')');
        }
    }

    return urlParams;
  },

  onLoadConfigsTags: function (data) {
    var urlParams = this.getConfigUrlParams(this.get('content.reassign.component_name'), data);

    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  /**
   *
   * @returns {extendedTopologyLocalDB}
   * @private
   * @method _prepareTopologyDB
   */
  _prepareTopologyDB: function () {
    var ret = this.get('content').getProperties(['masterComponentHosts', 'slaveComponentHosts', 'hosts']);
    ret.installedServices = App.Service.find().mapProperty('serviceName');
    return ret;
  },

  /**
   * Create dependencies for Config Initializers
   *
   * @param {object} additionalDependencies  some additional information that should be added
   * @returns {reassignComponentDependencies}
   * @private
   * @method _prepareDependencies
   */
  _prepareDependencies: function (additionalDependencies) {
    additionalDependencies = additionalDependencies || {};
    var ret = {};
    ret.sourceHostName = this.get('content.reassignHosts.source');
    ret.targetHostName = this.get('content.reassignHosts.target');
    return Em.merge(ret, additionalDependencies);
  },

  /**
   * Get additional dependencies-data for App.MoveRmConfigInitializer
   *
   * @param {object} configs
   * @returns {object}
   * @private
   * @method _getRmAdditionalDependencies
   */
  _getRmAdditionalDependencies: function (configs) {
    var ret = {};
    var cfg = configs['yarn-site']['yarn.resourcemanager.hostname.rm1'];
    if (cfg) {
      ret.rm1 = cfg;
    }
    return ret;
  },

  /**
   * Settings used to the App.MoveOSConfigInitializer setup
   *
   * @param {object} configs
   * @returns {object}
   * @private
   * @method _getOsInitializerSettings
   */
  _getOsInitializerSettings: function (configs) {
    var ret = {};
    var cfg = configs['oozie-env']['oozie_user'];
    if (cfg) {
      ret.oozieUser = cfg;
    }
    return ret;
  },

  /**
   * Get additional dependencies-data for App.MoveNameNodeConfigInitializer
   *
   * @param {object} configs
   * @returns {object}
   * @private
   * @method _getNnInitializerSettings
   */
  _getNnInitializerSettings: function (configs) {
    var ret = {};
    if (App.get('isHaEnabled')) {
      ret.namespaceId = configs['hdfs-site']['dfs.nameservices'];
      ret.suffix = (configs['hdfs-site']['dfs.namenode.http-address.' + ret.namespaceId + '.nn1'] === this.get('content.reassignHosts.source') + ':50070') ? 'nn1' : 'nn2';
    }
    return ret;
  },

  /**
   * Settings used to the App.MoveHsConfigInitializer and App.MoveHmConfigInitializer setup
   *
   * @param {object} configs
   * @returns {{hiveUser: string, webhcatUser: string}}
   * @private
   * @method _getHiveInitializerSettings
   */
  _getHiveInitializerSettings: function (configs) {
    return {
      hiveUser: configs['hive-env']['hive_user'],
      webhcatUser: configs['hive-env']['webhcat_user']
    };
  },

  /**
   * Settings used to the App.MoveRmConfigInitializer setup
   *
   * @param {object} configs
   * @returns {{suffix: string}}
   * @private
   * @method _getRmInitializerSettings
   */
  _getRmInitializerSettings: function (configs) {
    return {
      suffix: configs['yarn-site']['yarn.resourcemanager.hostname.rm1'] === this.get('content.reassignHosts.source') ? 'rm1': 'rm2'
    };
  },

  onLoadConfigs: function (data) {
    var componentName = this.get('content.reassign.component_name');
    var targetHostName = this.get('content.reassignHosts.target');
    var configs = {};
    var secureConfigs = [];

    data.items.forEach(function (item) {
      configs[item.type] = item.properties;
    }, this);

    this.setAdditionalConfigs(configs, componentName, targetHostName);
    this.setSecureConfigs(secureConfigs, configs, componentName);

    switch (componentName) {
      case 'NAMENODE':
        App.MoveNameNodeConfigInitializer.setup(this._getNnInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveNameNodeConfigInitializer);
        App.MoveNameNodeConfigInitializer.cleanup();
        break;
      case 'RESOURCEMANAGER':
        App.MoveRmConfigInitializer.setup(this._getRmInitializerSettings(configs));
        var additionalDependencies = this._getRmAdditionalDependencies(configs);
        configs = this.setDynamicConfigs(configs, App.MoveRmConfigInitializer, additionalDependencies);
        App.MoveRmConfigInitializer.cleanup();
        break;
      case 'HIVE_METASTORE':
        App.MoveHmConfigInitializer.setup(this._getHiveInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveHmConfigInitializer);
        App.MoveHmConfigInitializer.cleanup();
        break;
      case 'HIVE_SERVER':
        App.MoveHsConfigInitializer.setup(this._getHiveInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveHsConfigInitializer);
        App.MoveHsConfigInitializer.cleanup();
        break;
      case 'OOZIE_SERVER':
        App.MoveOSConfigInitializer.setup(this._getOsInitializerSettings(configs));
        configs = this.setDynamicConfigs(configs, App.MoveOSConfigInitializer);
        App.MoveOSConfigInitializer.cleanup();
    }

    this.saveClusterStatus(secureConfigs, this.getComponentDir(configs, componentName));
    this.saveConfigsToServer(configs);
    this.saveServiceProperties(configs);
  },

  /**
   * Set config values according to the new cluster topology
   *
   * @param {object} configs
   * @param {MoveComponentConfigInitializerClass} initializer
   * @param {object} [additionalDependencies={}]
   * @returns {object}
   * @method setDynamicConfigs
   */
  setDynamicConfigs: function (configs, initializer, additionalDependencies) {
    additionalDependencies = additionalDependencies || {};
    var topologyDB = this._prepareTopologyDB();
    var dependencies = this._prepareDependencies(additionalDependencies);
    Em.keys(configs).forEach(function (site) {
      Em.keys(configs[site]).forEach(function (config) {
        // temporary object for initializer
        var cfg = {
          name: config,
          filename: site,
          value: configs[site][config]
        };
        configs[site][config] = initializer.initialValue(cfg, topologyDB, dependencies).value;
      });
    });
    return configs;
  },

  /**
   * make PUT call to save configs to server
   * @param configs
   */
  saveConfigsToServer: function (configs) {
    App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + this.getServiceConfigData(configs).toString() + ']'
      },
      success: 'onSaveConfigs',
      error: 'onTaskError'
    });
  },
  /**
   * gather and format config data before sending to server
   * @param configs
   * @return {Array}
   * @method getServiceConfigData
   */
  getServiceConfigData: function (configs) {
    var componentName = this.get('content.reassign.component_name');
    var tagName = 'version' + (new Date).getTime();
    var configData = Object.keys(configs).map(function (_siteName) {
      return {
        type: _siteName,
        tag: tagName,
        properties: configs[_siteName],
        service_config_version_note: Em.I18n.t('services.reassign.step4.save.configuration.note').format(App.format.role(componentName, false))
      }
    });
    var allConfigData = [];

    App.Service.find().forEach(function (service) {
      var stackService = App.StackService.find().findProperty('serviceName', service.get('serviceName'));
      if (stackService) {
        var serviceConfigData = [];
        Object.keys(stackService.get('configTypesRendered')).forEach(function (type) {
          var serviceConfigTag = configData.findProperty('type', type);
          if (serviceConfigTag) {
            serviceConfigData.pushObject(serviceConfigTag);
          }
        }, this);
        allConfigData.pushObject(JSON.stringify({
          Clusters: {
            desired_config: serviceConfigData
          }
        }));
      }
    }, this);
    return allConfigData;
  },

  /**
   * Get the web address port when RM HA is enabled.
   * @param configs
   * @param webAddressKey (http vs https)
   * */
  getWebAddressPort: function (configs, webAddressKey){
    var result = null;
    var rmWebAddressValue = configs['yarn-site'][webAddressKey];
    if(rmWebAddressValue){
      var tokens = rmWebAddressValue.split(":");
      if(tokens.length > 1){
        result = tokens[1];
        result = result.replace(/^\s+|\s+$/g, '');
      }
    }

    if(result)  //only return non-empty result
      return result;
    else
      return null;
  },

  /**
   * set secure configs for component
   * @param secureConfigs
   * @param configs
   * @param componentName
   * @return {Boolean}
   */
  setSecureConfigs: function (secureConfigs, configs, componentName) {
    var securityEnabled = App.get('isKerberosEnabled');
    var component = this.get('secureConfigsMap').findProperty('componentName', componentName);
    if (Em.isNone(component) || !securityEnabled) return false;

    component.configs.forEach(function (config) {
      secureConfigs.push({
        keytab: configs[config.site][config.keytab],
        principal: configs[config.site][config.principal]
      });
    });
    return true;
  },

  /**
   * derive component directory from configurations
   * @param configs
   * @param componentName
   * @return {String}
   */
  getComponentDir: function (configs, componentName) {
    if (componentName === 'NAMENODE') {
      return configs['hdfs-site']['dfs.namenode.name.dir'];
    } else if (componentName === 'SECONDARY_NAMENODE') {
      return configs['hdfs-site']['dfs.namenode.checkpoint.dir'];
    }
    return '';
  },

  /**
   * save cluster status to server
   *
   * @param secureConfigs
   * @param componentDir
   * @return {Boolean}
   */
  saveClusterStatus: function (secureConfigs, componentDir) {
    if (componentDir || secureConfigs.length) {
      App.router.get(this.get('content.controllerName')).saveComponentDir(componentDir);
      App.router.get(this.get('content.controllerName')).saveSecureConfigs(secureConfigs);
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('content.cluster.name'),
        clusterState: this.get('clusterDeployState'),
        wizardControllerName: this.get('content.controllerName'),
        localdb: App.db.data
      });
      return true;
    }
    return false;
  },

  onSaveConfigs: function () {
    this.onTaskCompleted();
  },

  startZooKeeperServers: function () {
    var components = this.get('content.masterComponentHosts').filterProperty('component', 'ZOOKEEPER_SERVER');
    this.updateComponent('ZOOKEEPER_SERVER', components.mapProperty('hostName'), "ZOOKEEPER", "Start");
  },

  startNameNode: function () {
    var components = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE');
    this.updateComponent('NAMENODE', components.mapProperty('hostName').without(this.get('content.reassignHosts.source')), "HDFS", "Start");
  },

  /**
   * make server call to start services
   */
  startRequiredServices: function () {
    var unrelatedServices = this.get('unrelatedServicesMap')[this.get('content.reassign.component_name')];
    if (unrelatedServices) {
      this.startServices(false, unrelatedServices);
    } else {
      this.startServices(true);
    }
  },

  /**
   * make DELETE call for each host component on host
   */
  deleteHostComponents: function () {
    this.set('multiTaskCounter', 0);
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.delete.host_component',
        sender: this,
        data: {
          hostName: hostName,
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onDeleteHostComponentsError'
      });
    }
  },

  onDeleteHostComponentsError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.NoSuchResourceException') !== -1) {
      this.onComponentsTasksSuccess();
    } else {
      this.onTaskError();
    }
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      if (this.get('content.hasManualSteps')) {
        App.router.send('next');
      } else {
        App.router.send('complete');
      }
    }
  },

  /**
   * make server call to clean MYSQL
   */
  cleanMySqlServer: function () {
    var hostname = App.HostComponent.find().findProperty('componentName', 'MYSQL_SERVER').get('hostName');

    if (this.get('content.reassign.component_name') === 'MYSQL_SERVER') {
      hostname = this.get('content.reassignHosts.target');
    }

    App.ajax.send({
      name: 'service.mysql.clean',
      sender: this,
      data: {
        host: hostname
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  /**
   * make server call to configure MYSQL
   */
  configureMySqlServer : function () {
    var hostname = App.HostComponent.find().findProperty('componentName', 'MYSQL_SERVER').get('hostName');

    if (this.get('content.reassign.component_name') === 'MYSQL_SERVER') {
      hostname = this.get('content.reassignHosts.target');
    }

    App.ajax.send({
      name: 'service.mysql.configure',
      sender: this,
      data: {
        host: hostname
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startMySqlServer: function() {
    App.ajax.send({
      name: 'common.host.host_component.update',
      sender: this,
      data: {
        context: "Start MySQL Server",
        hostName: App.HostComponent.find().findProperty('componentName', 'MYSQL_SERVER').get('hostName'),
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        HostRoles: {
          state: "STARTED"
        }
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  restartMySqlServer: function() {
    var context = "Restart MySql Server";

    var resource_filters = {
      component_name: "MYSQL_SERVER",
      hosts: App.HostComponent.find().filterProperty('componentName', 'MYSQL_SERVER').get('firstObject.hostName'),
      service_name: "HIVE"
    };

    var operation_level = {
      level: "HOST_COMPONENT",
      cluster_name: this.get('content.cluster.name'),
      service_name: "HIVE",
      hostcomponent_name: "MYSQL_SERVER"
    };

    App.ajax.send({
      name: 'restart.hostComponents',
      sender: this,
      data: {
        context: context,
        resource_filters: [resource_filters],
        operation_level: operation_level
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startNewMySqlServer: function() {
    App.ajax.send({
      name: 'common.host.host_component.update',
      sender: this,
      data: {
        context: "Start MySQL Server",
        hostName: this.get('content.reassignHosts.target'),
        serviceName: "HIVE",
        componentName: "MYSQL_SERVER",
        HostRoles: {
          state: "STARTED"
        }
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  testDBConnection: function() {
    this.prepareDBCheckAction();
  },

  isComponentWithDB: function() {
    return ['HIVE_SERVER', 'HIVE_METASTORE', 'OOZIE_SERVER'].contains(this.get('content.reassign.component_name'));
  },

  /** @property {Object} propertiesPattern - check pattern according to type of connection properties **/
  propertiesPattern: function() {
    return {
      user_name: /(username|dblogin)$/ig,
      user_passwd: /(dbpassword|password)$/ig,
      db_connection_url: /jdbc\.url|connectionurl/ig,
      driver_class: /ConnectionDriverName|jdbc\.driver/ig,
      schema_name: /db\.schema\.name/ig
    };
  }.property(),

  /** @property {Object} connectionProperties - service specific config values mapped for custom action request **/
  connectionProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      propObj[key] = this.getConnectionProperty(this.get('propertiesPattern')[key]);
    }
    return propObj;
  }.property('propertiesPattern'),

  getConnectionProperty: function(regexp) {
    var propertyName = this.get('requiredProperties').filter(function(item) {
      return regexp.test(item);
    })[0];
    return this.get('content.serviceProperties')[propertyName];
  },

  /**
   * Properties that stores in local storage used for handling
   * last success connection.
   *
   * @property {Object} preparedDBProperties
   **/
  preparedDBProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      var propValue = this.getConnectionProperty(this.get('propertiesPattern')[key]);
      propObj[key] = propValue;
    }
    return propObj;
  }.property(),

  /** @property {object} requiredProperties - properties that necessary for database connection **/
  requiredProperties: function() {
    var propertiesMap = {
      OOZIE: ['oozie.db.schema.name','oozie.service.JPAService.jdbc.username','oozie.service.JPAService.jdbc.password','oozie.service.JPAService.jdbc.driver','oozie.service.JPAService.jdbc.url'],
      HIVE: ['ambari.hive.db.schema.name','javax.jdo.option.ConnectionUserName','javax.jdo.option.ConnectionPassword','javax.jdo.option.ConnectionDriverName','javax.jdo.option.ConnectionURL']
    };

    return propertiesMap[this.get('content.reassign.service_id')];
  }.property(),

  dbType: function() {
    var databaseTypes = /MySQL|PostgreS|Oracle|Derby|MSSQL|Anywhere/gi;
    var databaseProp = this.get('content.serviceProperties')[Em.getWithDefault(this.get('dbPropertyMap'), this.get('content.reassign.component_name'), null)];

    return databaseProp.match(databaseTypes)[0];
  }.property(),

  prepareDBCheckAction: function() {
    var params = this.get('preparedDBProperties');

    var ambariProperties = App.router.get('clusterController.ambariProperties');

    params['db_name'] = this.get('dbType');
    params['jdk_location'] = ambariProperties['jdk_location'];
    params['jdk_name'] = ambariProperties['jdk.name'];
    params['java_home'] = ambariProperties['java.home'];

    params['threshold'] = 60;
    params['ambari_server_host'] = location.hostname;
    params['check_execute_list'] = "db_connection_check";

    App.ajax.send({
      name: 'cluster.custom_action.create',
      sender: this,
      data: {
        requestInfo: {
          "context": "Check host",
          "action": "check_host",
          "parameters": params
        },
        filteredHosts: [this.get('content.reassignHosts.target')]
      },
      success: 'onCreateActionSuccess',
      error: 'onTaskError'
    });
  },

  onCreateActionSuccess: function(data) {
    this.set('checkDBRequestId', data.Requests.id);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('checkDBRequestId')
      },
      success: 'setCheckDBTaskId'
    });
  },

  setCheckDBTaskId: function(data) {
    this.set('checkDBTaskId', data.items[0].Tasks.id);
    this.startDBCheckPolling();
  },

  startDBCheckPolling: function() {
      this.getDBConnTaskInfo();
  },

  getDBConnTaskInfo: function() {
    this.setTaskStatus(this.get('currentTaskId'), 'IN_PROGRESS');
    this.get('tasks').findProperty('id', this.get('currentTaskId')).set('progress', 100);

    this.set('logs', []);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('checkDBRequestId'),
        taskId: this.get('checkDBTaskId')
      },
      success: 'getDBConnTaskInfoSuccess'
    });
  },

  getDBConnTaskInfoSuccess: function(data) {
    var task = data.Tasks;
    if (task.status === 'COMPLETED') {
      var structuredOut = task.structured_out.db_connection_check;
      if (structuredOut.exit_code != 0) {
        this.showConnectionErrorPopup(structuredOut.message);
        this.onTaskError();
      } else {
        this.onTaskCompleted();
      }
    }

    if (task.status === 'FAILED') {
      this.onTaskError();
    }

    if (/PENDING|QUEUED|IN_PROGRESS/.test(task.status)) {
      Em.run.later(this, function() {
        this.startDBCheckPolling();
      }, 3000);
    }
  },

  showConnectionErrorPopup: function(error) {
    var popup = App.showAlertPopup('Database Connection Error');
    popup.set('body', error);
  },

  testDBRetryTooltip: function() {
    var db_host = this.get('content.serviceProperties.database_hostname');
    var db_type = this.get('dbType');
    var db_props = this.get('preparedDBProperties');

    return Em.I18n.t('services.reassign.step4.tasks.testDBConnection.tooltip').format(
      db_host, db_type, db_props['schema_name'], db_props['user_name'],
      db_props['user_passwd'], db_props['driver_class'], db_props['db_connection_url']
    );
  }.property('dbProperties'),

  saveServiceProperties: function(configs) {
    App.router.get(this.get('content.controllerName')).saveServiceProperties(configs);
  }

});
