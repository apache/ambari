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
    'stopHostComponentsInMaintenanceMode',
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
    'stopHostComponentsInMaintenanceMode',
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
        this.removeTasks(['stopHostComponentsInMaintenanceMode', 'deleteHostComponents', 'startRequiredServices']);
      } else {
        this.removeTasks(['startZooKeeperServers', 'startNameNode', 'stopHostComponentsInMaintenanceMode', 'deleteHostComponents', 'startRequiredServices']);
      }
    } else {
      this.removeTasks(['startZooKeeperServers', 'startNameNode']);
    }

    if (this.get('componentsWithoutReconfiguration').contains(componentName)) {
      this.removeTasks(['reconfigure']);
    }

    if (!this.get('content.reassignComponentsInMM.length')) {
      this.removeTasks(['stopHostComponentsInMaintenanceMode']);
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
    this.decrementProperty('multiTaskCounter');
    if (this.get('multiTaskCounter') <= 0) {
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
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.target');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      this.createComponent(hostComponents[i], hostName, this.get('content.reassign.service_id'));
    }
  },

  onCreateComponent: function () {
    this.onComponentsTasksSuccess();
  },

  putHostComponentsInMaintenanceMode: function () {
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    this.set('multiTaskCounter', hostComponents.length);
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
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.target');
    this.set('multiTaskCounter', hostComponents.length);
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

  /**
   * construct URL parameters for config call
   * @param componentName
   * @param data
   * @return {Array}
   */
  getConfigUrlParams: function (componentName, data) {
    var urlParams = [];
    switch (componentName) {
      case 'NAMENODE':
        urlParams.push('(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')');
        urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
        if (App.Service.find().someProperty('serviceName', 'HBASE')) {
          urlParams.push('(type=hbase-site&tag=' + data.Clusters.desired_configs['hbase-site'].tag + ')');
        }
        if (App.Service.find().someProperty('serviceName', 'ACCUMULO')) {
          urlParams.push('(type=accumulo-site&tag=' + data.Clusters.desired_configs['accumulo-site'].tag + ')');
        }
        break;
      case 'SECONDARY_NAMENODE':
        urlParams.push('(type=hdfs-site&tag=' + data.Clusters.desired_configs['hdfs-site'].tag + ')');
        urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
        break;
      case 'JOBTRACKER':
        urlParams.push('(type=mapred-site&tag=' + data.Clusters.desired_configs['mapred-site'].tag + ')');
        break;
      case 'RESOURCEMANAGER':
        urlParams.push('(type=yarn-site&tag=' + data.Clusters.desired_configs['yarn-site'].tag + ')');
        break;
      case 'WEBHCAT_SERVER':
        urlParams.push('(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')');
        break;
      case 'APP_TIMELINE_SERVER':
        urlParams.push('(type=yarn-site&tag=' + data.Clusters.desired_configs['yarn-site'].tag + ')');
        urlParams.push('(type=yarn-env&tag=' + data.Clusters.desired_configs['yarn-env'].tag + ')');
        break;
      case 'OOZIE_SERVER':
        urlParams.push('(type=oozie-site&tag=' + data.Clusters.desired_configs['oozie-site'].tag + ')');
        urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
        urlParams.push('(type=oozie-env&tag=' + data.Clusters.desired_configs['oozie-env'].tag + ')');
        break;
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
        urlParams.push('(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')');
        urlParams.push('(type=webhcat-site&tag=' + data.Clusters.desired_configs['webhcat-site'].tag + ')');
        urlParams.push('(type=hive-env&tag=' + data.Clusters.desired_configs['hive-env'].tag + ')');
        urlParams.push('(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')');
        break;
      case 'MYSQL_SERVER':
        urlParams.push('(type=hive-site&tag=' + data.Clusters.desired_configs['hive-site'].tag + ')');
        break;
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
        this.setSpecificNamenodeConfigs(configs, targetHostName);
        break;
      case 'RESOURCEMANAGER':
        this.setSpecificResourceMangerConfigs(configs, targetHostName);
        break;
      case 'HIVE_METASTORE':
      case 'HIVE_SERVER':
        this.setSpecificHiveConfigs(configs, targetHostName);
        break;
      case 'OOZIE_SERVER':
        this.setSpecificOozieConfigs(configs, targetHostName);
    }

    this.saveClusterStatus(secureConfigs, this.getComponentDir(configs, componentName));
    this.saveConfigsToServer(configs);
    this.saveServiceProperties(configs);
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
   * set specific configs which applies only to NameNode component
   * @param configs
   * @param targetHostName
   */
  setSpecificNamenodeConfigs: function (configs, targetHostName) {
    var sourceHostName = this.get('content.reassignHosts.source');

    if (App.get('isHaEnabled')) {
      var nameServices = configs['hdfs-site']['dfs.nameservices'];
      var suffix = (configs['hdfs-site']['dfs.namenode.http-address.' + nameServices + '.nn1'] === sourceHostName + ':50070') ? '.nn1' : '.nn2';
      configs['hdfs-site']['dfs.namenode.http-address.' + nameServices + suffix] = targetHostName + ':50070';
      configs['hdfs-site']['dfs.namenode.https-address.' + nameServices + suffix] = targetHostName + ':50470';
      configs['hdfs-site']['dfs.namenode.rpc-address.' + nameServices + suffix] = targetHostName + ':8020';
    }
    if (!App.get('isHaEnabled') && App.Service.find('HBASE').get('isLoaded')) {
      configs['hbase-site']['hbase.rootdir'] = configs['hbase-site']['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + targetHostName + ':8020');
    }
    if (!App.get('isHaEnabled') && App.Service.find('ACCUMULO').get('isLoaded')) {
      // Update the Namenode's hostname in instance.volumes
      configs['accumulo-site']['instance.volumes'] = configs['accumulo-site']['instance.volumes'].replace(/\/\/[^\/]*/, '//' + targetHostName + ':8020');
      // Add a replacement entry from the old hostname to the new hostname
      var target = 'hdfs://' + this.get('content.reassignHosts.target') + ':8020' + '/apps/accumulo/data';
      var source = 'hdfs://' + this.get('content.reassignHosts.source') + ':8020' + '/apps/accumulo/data';
      if (configs['accumulo-site']) {
        configs['accumulo-site']['instance.volumes.replacements'] = source + ' ' + target;
      }
    }
  },

  /**
   * set specific configs which applies only to ResourceManager component
   * @param configs
   * @param targetHostName
   */
  setSpecificResourceMangerConfigs: function (configs, targetHostName) {
    var sourceHostName = this.get('content.reassignHosts.source');

    if (App.get('isRMHaEnabled')) {
      if (configs['yarn-site']['yarn.resourcemanager.hostname.rm1'] === sourceHostName) {
        configs['yarn-site']['yarn.resourcemanager.hostname.rm1'] = targetHostName;

        var webAddressPort = this.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.address.rm1');
        if(webAddressPort != null)
          configs['yarn-site']['yarn.resourcemanager.webapp.address.rm1'] = targetHostName +":"+ webAddressPort;

        var httpsWebAddressPort = this.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.https.address.rm1');
        if(httpsWebAddressPort != null)
          configs['yarn-site']['yarn.resourcemanager.webapp.https.address.rm1'] = targetHostName +":"+ httpsWebAddressPort;
      } else {
        configs['yarn-site']['yarn.resourcemanager.hostname.rm2'] = targetHostName;

        var webAddressPort = this.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.address.rm2');
        if(webAddressPort != null)
          configs['yarn-site']['yarn.resourcemanager.webapp.address.rm2'] = targetHostName +":"+ webAddressPort;

        var httpsWebAddressPort = this.getWebAddressPort(configs, 'yarn.resourcemanager.webapp.https.address.rm2');
        if(httpsWebAddressPort != null)
          configs['yarn-site']['yarn.resourcemanager.webapp.https.address.rm2'] = targetHostName +":"+ httpsWebAddressPort;
      }
    }
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
   * set specific configs which applies only to Oozie related configs
   * @param configs
   * @param targetHostName
   */
  setSpecificOozieConfigs: function (configs, targetHostName) {
    var sourceHostName = this.get('content.reassignHosts.source'),
      oozieServerHosts = App.HostComponent.find().filterProperty('componentName', 'OOZIE_SERVER')
        .mapProperty('hostName').removeObject(sourceHostName).addObject(targetHostName).uniq().join(','),
      oozieUser = configs['oozie-env']['oozie_user'];

    configs['core-site']['hadoop.proxyuser.' + oozieUser + '.hosts'] = oozieServerHosts;
  },

  /**
   * set specific configs which applies only to Hive related configs
   * @param configs
   * @param targetHostName
   */
  setSpecificHiveConfigs: function (configs, targetHostName) {
    var sourceHostName = this.get('content.reassignHosts.source');
    var hiveMSHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_METASTORE').mapProperty('hostName');
    if (this.get('content.reassign.component_name') === 'HIVE_METASTORE') hiveMSHosts = hiveMSHosts.removeObject(sourceHostName).addObject(targetHostName);
    var hiveServerHosts = App.HostComponent.find().filterProperty('componentName', 'HIVE_SERVER').mapProperty('hostName');
    if (this.get('content.reassign.component_name') === 'HIVE_SERVER') hiveServerHosts = hiveServerHosts.removeObject(sourceHostName).addObject(targetHostName);
    var hiveMasterHosts = hiveMSHosts.concat(hiveServerHosts).uniq().join(',');
    var hiveUser = configs['hive-env']['hive_user'];
    var webhcatUser = configs['hive-env']['webhcat_user'];

    var port = configs['hive-site']['hive.metastore.uris'].match(/:[0-9]{2,4}/);
    port = port ? port[0].slice(1) : "9083";

    for (var i = 0; i < hiveMSHosts.length; i++) {
      hiveMSHosts[i] = "thrift://" + hiveMSHosts[i] + ":" + port;
    }

    configs['hive-site']['hive.metastore.uris'] = hiveMSHosts.join(',');
    configs['webhcat-site']['templeton.hive.properties'] = configs['webhcat-site']['templeton.hive.properties'].replace(/thrift.+[0-9]{2,},/i, hiveMSHosts.join('\\,') + ",");
    configs['core-site']['hadoop.proxyuser.' + hiveUser + '.hosts'] = hiveMasterHosts;
    configs['core-site']['hadoop.proxyuser.' + webhcatUser + '.hosts'] = hiveMasterHosts;
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
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    this.set('multiTaskCounter', hostComponents.length);
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
    var hostname = App.HostComponent.find().filterProperty('componentName', 'MYSQL_SERVER').get('firstObject.hostName');

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
    var hostname = App.HostComponent.find().filterProperty('componentName', 'MYSQL_SERVER').get('firstObject.hostName');

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
        hostName: App.HostComponent.find().filterProperty('componentName', 'MYSQL_SERVER').get('firstObject.hostName'),
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

  dbProperty: function() {
    var componentName = this.get('content.reassign.component_name');

    var property = null;
    switch(componentName) {
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
        property = 'javax.jdo.option.ConnectionDriverName';
        break;
      case 'OOZIE_SERVER':
        property = 'oozie.service.JPAService.jdbc.url';
        break;
    }

    return property;
  }.property(),

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
    var databaseProp = this.get('content.serviceProperties')[this.get('dbProperty')];

    return databaseProp.match(databaseTypes)[0];
  }.property('dbProperty'),

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
  },

  stopHostComponentsInMaintenanceMode: function () {
    var hostComponentsInMM = this.get('content.reassignComponentsInMM');
    var hostName = this.get('content.reassignHosts.source');
    var serviceName = this.get('content.reassign.service_id');
    hostComponentsInMM = hostComponentsInMM.map(function(componentName){
      return {
        hostName: hostName,
        serviceName: serviceName,
        componentName: componentName
      };
    });
    this.set('multiTaskCounter', hostComponentsInMM.length);
    this.updateComponentsState(hostComponentsInMM, 'INSTALLED');
  }

});
