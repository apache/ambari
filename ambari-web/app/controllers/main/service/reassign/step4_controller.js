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

App.ReassignMasterWizardStep4Controller = App.HighAvailabilityProgressPageController.extend({

  isReassign: true,

  commands: ['stopServices', 'createHostComponents', 'putHostComponentsInMaintenanceMode', 'reconfigure', 'installHostComponents', 'startZooKeeperServers', 'startNameNode', 'deleteHostComponents', 'startServices'],

  clusterDeployState: 'REASSIGN_MASTER_INSTALLING',

  multiTaskCounter: 0,

  hostComponents: [],

  /**
   * Map with lists of unrelated services.
   * Used to define list of services to stop/start.
   */
  unrelatedServicesMap: {
    'JOBTRACKER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM'],
    'RESOURCEMANAGER': ['HDFS', 'ZOOKEEPER', 'HBASE', 'FLUME', 'SQOOP', 'STORM']
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
          'dfs.namenode.http-address': '<replace-value>:50070',
          'dfs.namenode.https-address': '<replace-value>:50470'
        },
        'core-site': {
          'fs.defaultFS': 'hdfs://<replace-value>:8020'
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
      componentName: 'JOBTRACKER',
      configs: [
        {
          site: 'mapred-site',
          keytab: 'mapreduce.jobtracker.keytab.file',
          principal: 'mapreduce.jobtracker.kerberos.principal'
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
    var isHadoop2Stack = App.get('isHadoop2Stack');
    var component = this.get('additionalConfigsMap').findProperty('componentName', componentName);

    if (Em.isNone(component)) return false;
    var additionalConfigs = (component.configs_Hadoop2 && isHadoop2Stack) ? component.configs_Hadoop2 : component.configs;

    for (var site in additionalConfigs) {
      if (additionalConfigs.hasOwnProperty(site)) {
        for (var property in additionalConfigs[site]) {
          if (additionalConfigs[site].hasOwnProperty(property)) {
            if (App.get('isHaEnabled') && componentName === 'NAMENODE' && property === 'fs.defaultFS') continue;

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
      hostComponentsNames += comp === 'ZKFC' ? comp : App.format.role(comp);
    }, this);
    return hostComponentsNames;
  },

  /**
   * remove unneeded tasks
   */
  removeUnneededTasks: function () {
    if (this.get('content.hasManualSteps')) {
      if (this.get('content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled')) {
        // Only for reassign NameNode with HA enabled
        this.get('tasks').splice(7, 2);
      } else {
        this.get('tasks').splice(5, 4);
      }
    } else {
      this.get('tasks').splice(5, 2);
    }
  },

  /**
   * initialize tasks
   */
  initializeTasks: function () {
    var commands = this.get('commands');
    var currentStep = App.router.get('reassignMasterController.currentStep');
    var hostComponentsNames = this.getHostComponentsNames();

    for (var i = 0; i < commands.length; i++) {
      var TaskLabel = i === 3 ? this.get('serviceName') : hostComponentsNames; //For Reconfigure task, show serviceName
      var title = Em.I18n.t('services.reassign.step4.task' + i + '.title').format(TaskLabel);
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
   * compute data for call to stop services
   */
  getStopServicesData: function () {
    var data = {
      "ServiceInfo": {
        "state": "INSTALLED"
      }
    };
    var unrelatedServices = this.get('unrelatedServicesMap')[this.get('content.reassign.component_name')];
    if (unrelatedServices) {
      var list = App.Service.find().mapProperty("serviceName").filter(function (s) {
        return !unrelatedServices.contains(s)
      }).join(',');
      data.context = "Stop required services";
      data.urlParams = "ServiceInfo/service_name.in(" + list + ")";
    } else {
      data.context = "Stop all services";
    }
    return data;
  },

  /**
   * make server call to stop services
   */
  stopServices: function () {
    App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: this.getStopServicesData(),
      success: 'startPolling',
      error: 'onTaskError'
    });
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

    if (componentName === 'NAMENODE') {
      this.setSpecificNamenodeConfigs(configs, targetHostName);
    }

    if (componentName === 'RESOURCEMANAGER') {
      this.setSpecificResourceMangerConfigs(configs, targetHostName);
    }

    this.saveClusterStatus(secureConfigs, this.getComponentDir(configs, componentName));
    this.saveConfigsToServer(configs);
  },

  /**
   * make PUT call to save configs to server
   * @param configs
   */
  saveConfigsToServer: function (configs) {
    var componentName = this.get('content.reassign.component_name');
    var tagName = 'version' + (new Date).getTime();
    var configData = Object.keys(configs).map(function (_siteName) {
      return {
        type: _siteName,
        tag: tagName,
        properties: configs[_siteName],
        service_config_version_note: Em.I18n.t('services.reassign.step4.save.configuration.note').format(App.format.role(componentName))
      }
    });

    var installedServices = App.Service.find();
    var allConfigData = [];
    installedServices.forEach(function (service) {
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

    App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + allConfigData.toString() + ']'
      },
      success: 'onSaveConfigs',
      error: 'onTaskError'
    });
  },

  /**
   * set specific configs which applies only to NameNode component
   * @param configs
   * @param targetHostName
   */
  setSpecificNamenodeConfigs: function (configs, targetHostName) {
    var sourceHostName = this.get('content.reassignHosts.source');

    if (App.get('isHadoop2Stack') && App.get('isHaEnabled')) {
      var nameServices = configs['hdfs-site']['dfs.nameservices'];
      if (configs['hdfs-site']['dfs.namenode.http-address.' + nameServices + '.nn1'] === sourceHostName + ':50070') {
        configs['hdfs-site']['dfs.namenode.http-address.' + nameServices + '.nn1'] = targetHostName + ':50070';
        configs['hdfs-site']['dfs.namenode.https-address.' + nameServices + '.nn1'] = targetHostName + ':50470';
        configs['hdfs-site']['dfs.namenode.rpc-address.' + nameServices + '.nn1'] = targetHostName + ':8020';
      } else {
        configs['hdfs-site']['dfs.namenode.http-address.' + nameServices + '.nn2'] = targetHostName + ':50070';
        configs['hdfs-site']['dfs.namenode.https-address.' + nameServices + '.nn2'] = targetHostName + ':50470';
        configs['hdfs-site']['dfs.namenode.rpc-address.' + nameServices + '.nn2'] = targetHostName + ':8020';
      }
    }
    if (!App.get('isHaEnabled') && App.Service.find('HBASE').get('isLoaded')) {
      configs['hbase-site']['hbase.rootdir'] = configs['hbase-site']['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + targetHostName + ':8020');
    }
  },

  /**
   * set specific configs which applies only to ResourceManager component
   * @param configs
   * @param targetHostName
   */
  setSpecificResourceMangerConfigs: function (configs, targetHostName) {
    var sourceHostName = this.get('content.reassignHosts.source');

    if (App.get('isHadoop2Stack') && App.get('isRMHaEnabled')) {
      if (configs['yarn-site']['yarn.resourcemanager.hostname.rm1'] === sourceHostName) {
        configs['yarn-site']['yarn.resourcemanager.hostname.rm1'] = targetHostName;
      } else {
        configs['yarn-site']['yarn.resourcemanager.hostname.rm2'] = targetHostName;
      }
    }

  },

  /**
   * set secure configs for component
   * @param secureConfigs
   * @param configs
   * @param componentName
   * @return {Boolean}
   */
  setSecureConfigs: function (secureConfigs, configs, componentName) {
    var securityEnabled = this.get('content.securityEnabled');
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
      return (App.get('isHadoop2Stack')) ? configs['hdfs-site']['dfs.namenode.name.dir'] : configs['hdfs-site']['dfs.name.dir'];
    }
    else if (componentName === 'SECONDARY_NAMENODE') {
      return (App.get('isHadoop2Stack')) ? configs['hdfs-site']['dfs.namenode.checkpoint.dir'] : configs['core-site']['fs.checkpoint.dir'];
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

  startServices: function () {
    var unrelatedServices = this.get('unrelatedServicesMap')[this.get('content.reassign.component_name')];
    if (unrelatedServices) {
      var list = App.Service.find().mapProperty("serviceName").filter(function (s) {
        return !unrelatedServices.contains(s)
      }).join(',');
      var conf = {
        name: 'common.services.update',
        sender: this,
        data: {
          "context": "Start required services",
          "ServiceInfo": {
            "state": "STARTED"
          },
          urlParams: "ServiceInfo/service_name.in(" + list + ")"},
        success: 'startPolling',
        error: 'onTaskError'
      };
      App.ajax.send(conf);
    } else {
      App.ajax.send({
        name: 'common.services.update',
        sender: this,
        data: {
          "context": "Start all services",
          "ServiceInfo": {
            "state": "STARTED"
          },
          urlParams: "params/run_smoke_test=true"
        },
        success: 'startPolling',
        error: 'onTaskError'
      });
    }
  },

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
  }
});
