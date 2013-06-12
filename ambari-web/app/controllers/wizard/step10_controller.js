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

App.WizardStep10Controller = Em.Controller.extend({
  clusterInfo: [],

  isNagiosRestartRequired: function() {
    return this.get('content.controllerName') !== 'installerController' && App.Service.find('NAGIOS').get('isLoaded');
  }.property(),

  clearStep: function () {
    this.get('clusterInfo').clear();
  },

  loadStep: function () {
    console.log("TRACE: Loading step10: Summary Page");
    this.clearStep();
    this.loadInstalledHosts(this.loadRegisteredHosts());
    var installFlag = true;
    var startFlag = true;
    if (this.get('content.controllerName') == 'installerController') {
      installFlag = this.loadMasterComponents();
      startFlag = this.loadStartedServices();
    }
    if (installFlag && startFlag) {
      this.loadInstallTime();
    }
  },

  loadRegisteredHosts: function () {
    var masterHosts = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();
    var slaveHosts = this.get('content.slaveComponentHosts');
    var hostObj = [];
    slaveHosts.forEach(function (_hosts) {
      hostObj = hostObj.concat(_hosts.hosts);
    }, this);
    slaveHosts = hostObj.mapProperty('hostName').uniq();
    var registeredHosts = App.Host.find().mapProperty('hostName').concat(masterHosts.concat(slaveHosts)).uniq();
    var registerHostsStatement = Em.I18n.t('installer.step10.hostsSummary').format(registeredHosts.length);
    var registerHostsObj = Ember.Object.create({
      id: 1,
      color: 'text-info',
      displayStatement: registerHostsStatement,
      status: []
    });
    this.get('clusterInfo').pushObject(registerHostsObj);

    return registerHostsObj;
  },

  loadInstalledHosts: function (host) {
    var hosts = this.get('content.hosts');
    var hostsInfo = [];
    for (var index in hosts) {
      hostsInfo.pushObject(hosts[index]);
      console.log('Step10 SUMMARY: value of hosts is: ' + hosts[index].status);
    }
    var succeededHosts = hostsInfo.filterProperty('status', 'success');
    var warnedHosts = hostsInfo.filterProperty('status', 'warning').concat(hostsInfo.filterProperty('status', 'failed'));
    if (succeededHosts.length) {
      var successStatement = Em.I18n.t('installer.step10.servicesSummary').format(succeededHosts.length) + ((succeededHosts.length > 1) ? Em.I18n.t('installer.step8.hosts') : Em.I18n.t('installer.step8.host'));
      this.get('clusterInfo').findProperty('id', 1).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-success',
        displayStatement: successStatement
      }));
    }

    if (warnedHosts.length) {
      var warnStatement = warnedHosts.length + Em.I18n.t('installer.step10.warnings');
      this.get('clusterInfo').findProperty('id', 1).get('status').pushObject(Ember.Object.create({
        id: 2,
        color: 'text-warning',
        displayStatement: warnStatement,
        statements: []
      }));

      warnedHosts.forEach(function (_host) {
        var clusterState;
        console.log("Content.cluster.status is: " + this.get('content.cluster.status'));
        if (this.get('content.cluster.status') === 'INSTALL FAILED') {
          clusterState = Em.I18n.t('installer.step10.clusterState.installing');
        } else if (this.get('content.cluster.status') === 'START FAILED') {
          clusterState = Em.I18n.t('installer.step10.clusterState.starting');
        }
        console.log('host value is: ' + JSON.stringify(_host));
        var failedTasks = _host.tasks.filterProperty('Tasks.status', 'FAILED');
        failedTasks.forEach(function (_task) {
          var taskStatement = clusterState + App.format.role(_task.Tasks.role) + Em.I18n.t('installer.step10.taskStatus.failed') + _host.name;
          this.get('clusterInfo').findProperty('id', 1).get('status').findProperty('id', 2).get('statements').pushObject(Ember.Object.create({
            status: 'failed',
            color: 'text-info',
            displayStatement: taskStatement
          }));
        }, this);

        var abortedTasks = _host.tasks.filterProperty('Tasks.status', 'ABORTED');
        abortedTasks.forEach(function (_task) {
          var abortStatement = clusterState + App.format.role(_task.Tasks.role) + Em.I18n.t('installer.step10.taskStatus.aborted') + _host.name;
          this.get('clusterInfo').findProperty('id', 1).get('status').findProperty('id', 2).get('statements').pushObject(Ember.Object.create({
            status: 'aborted',
            color: 'text-info',
            displayStatement: abortStatement
          }));
        }, this);

        var timedOutTasks = _host.tasks.filterProperty('Tasks.status', 'TIMEDOUT');
        timedOutTasks.forEach(function (_task) {
          var timedOutStatement = clusterState + App.format.role(_task.Tasks.role) + Em.I18n.t('installer.step10.taskStatus.timedOut') + _host.name;
          this.get('clusterInfo').findProperty('id', 1).get('status').findProperty('id', 2).get('statements').pushObject(Ember.Object.create({
            status: 'timedout',
            color: 'text-info',
            displayStatement: timedOutStatement
          }));
        }, this);
      }, this);
    }
  },

  loadMasterComponents: function () {
    var components = this.get('content.masterComponentHosts');
    var statement;
    if (this.get('content.cluster.status') === 'INSTALL FAILED') {
      this.get('clusterInfo').pushObject(Ember.Object.create({
        id: 2,
        displayStatement: Em.I18n.t('installer.step10.installStatus.failed'),
        color: 'text-error',
        status: []
      }));
      return false;
    } else {
      this.get('clusterInfo').pushObject(Ember.Object.create({
        id: 2,
        displayStatement: Em.I18n.t('installer.step10.installStatus.installed'),
        color: 'text-success',
        status: []
      }));
    }

    console.log('STEP10 master components:  ' + JSON.stringify(components));
    components.forEach(function (_component) {
      var component = Ember.Object.create(_component);
      switch (component.component) {
        case 'NAMENODE':
          this.loadNn(component);
          break;
        case 'SECONDARY_NAMENODE':
          this.loadSnn(component);
          break;
        case 'JOBTRACKER' :
          this.loadJt(component);
          break;
        case 'HISTORYSERVER':
          this.loadHS(component);
          break;
        case 'RESOURCEMANAGER':
          this.loadRM(component);
          break;
        case 'ZOOKEEPER_SERVER' :
          // TODO: Fix this; redundant entries and wrong number
          //this.loadZk(component);
          break;
        case 'HBASE_MASTER':
          this.loadHb(component);
          break;
        case 'HIVE_SERVER':
          this.loadHiveServer(component);
          break;
        case 'OOZIE_SERVER':
          this.loadOozieServer(component);
          break;
        case 'GANGLIA_SERVER':
          this.loadGanglia(component)
          break;
        case 'NAGIOS_SERVER':
          this.loadNagios(component);
          break;
      }
    }, this);
    return true;
  },

  loadHS: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.historyServer') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to HistoryServer component');
    }
  },

  loadRM: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.resourceManager') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to ResourceManager component');
    }
  },

  loadNn: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.nameNode') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to NameNode component');
    }
  },

  loadSnn: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.secondaryNameNode') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to SecondaryNameNode component');
    }
  },

  loadJt: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.jobTracker') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to JobTracker component');
    }
  },

  loadZk: function (component) {
    var hostLength = component.get('hostName').length;
    if (hostLength) {
      var hostVal;
      if (hostLength === 1) {
        hostVal = Em.I18n.t('installer.step8.host');
      } else {
        hostVal = Em.I18n.t('installer.step8.hosts');
      }
      var statement = Em.I18n.t('installer.step10.master.zooKeeper') + component.get('hostName').length + ' ' + hostVal;
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to Zookeeper component');
    }
  },

  loadHb: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.hbase') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to HBase Master component');
    }
  },

  loadHiveServer: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.hiveMetastore') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to Hive server component');
    }
  },

  loadOozieServer: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.oozie') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to Oozie server component');
    }
  },

  loadGanglia: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.ganglia') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to Ganglia server component');
    }
  },

  loadNagios: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.nagios') + component.get('hostName');
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Ember.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    } else {
      console.log('ERROR: no host name assigned to Nagios server component');
    }
  },

  loadStartedServices: function (component) {
    if (this.get('content.cluster.status') === 'STARTED') {
      var statement = Em.I18n.t('installer.step10.startStatus.started');
      this.get('clusterInfo').pushObject(Ember.Object.create({
        id: 3,
        color: 'text-success',
        displayStatement: Em.I18n.t('installer.step10.startStatus.started'),
        status: []
      }));
      this.get('clusterInfo').pushObject(Ember.Object.create({
        id: 4,
        color: 'text-success',
        displayStatement: Em.I18n.t('installer.step10.startStatus.passed'),
        status: []
      }));
      return true;
    } else {
      this.get('clusterInfo').pushObject(Ember.Object.create({
        id: 3,
        color: 'text-error',
        displayStatement: Em.I18n.t('installer.step10.startStatus.failed'),
        status: []
      }));
      return false;
    }
  },

  loadInstallTime: function () {
    var statement;
    var time;
    if (this.get('content.cluster.installTime')) {
      time = this.calculateInstallTime(this.get('content.cluster.installTime'));
      if (time.minutes !== 0) {
        statement = Em.I18n.t('installer.step10.installTime.minutes').format(time.minutes, time.seconds);
      } else {
        statement = Em.I18n.t('installer.step10.installTime.seconds').format(time.seconds);
      }
      this.get('clusterInfo').pushObject(Ember.Object.create({
        id: 5,
        color: 'text-info',
        displayStatement: statement,
        status: []
      }));
    }
  },
  calculateInstallTime: function(installTime){
    var secondsPerMinute = 60;
    var minutes = Math.floor(installTime);
    var seconds = Math.floor((installTime - minutes) * secondsPerMinute);
    return {
      minutes: minutes,
      seconds: seconds
    }
  }
});

