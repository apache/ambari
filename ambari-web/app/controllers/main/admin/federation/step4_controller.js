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

App.NameNodeFederationWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "nameNodeFederationWizardStep4Controller",

  commands: ['stopAllServices', 'reconfigureHDFS', 'installNameNode', 'installZKFC', 'formatNameNode', 'formatZKFC', 'startZKFC', 'startNameNode', 'bootstrapNameNode', 'startZKFC2', 'startNameNode2', 'startAllServices'],

  tasksMessagesPrefix: 'admin.nameNodeFederation.wizard.step',

  newNameNodeHosts: function () {
    return this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').filterProperty('isInstalled', false).mapProperty('hostName');
  }.property('content.masterComponentHosts.@each.hostName'),

  reconfigureHDFS: function () {
    var data = this.get('content.serviceConfigProperties');
    var note = Em.I18n.t('admin.nameNodeFederation.wizard,step4.save.configuration.note');
    var configData = this.reconfigureSites(['hdfs-site'], data, note);
    return App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      error: 'onTaskError',
      success: 'installHDFSClients'
    });
  },

  installHDFSClients: function () {
    var nnHostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    var jnHostNames = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').mapProperty('hostName');
    var hostNames = nnHostNames.concat(jnHostNames).uniq();
    this.createInstallComponentTask('HDFS_CLIENT', hostNames, 'HDFS');
  },

  stopAllServices: function () {
    this.stopServices([], true, true);
  },

  installNameNode: function () {
    this.createInstallComponentTask('NAMENODE', this.get('newNameNodeHosts'), "HDFS");
  },

  installZKFC: function () {
    this.createInstallComponentTask('ZKFC', this.get('newNameNodeHosts'), "HDFS");
  },

  formatNameNode: function () {
    App.ajax.send({
      name: 'nameNode.federation.formatNameNode',
      sender: this,
      data: {
        host: this.get('newNameNodeHosts')[0]
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  formatZKFC: function () {
    App.ajax.send({
      name: 'nameNode.federation.formatZKFC',
      sender: this,
      data: {
        host: this.get('newNameNodeHosts')[0]
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startZKFC: function () {
    this.updateComponent('ZKFC', this.get('newNameNodeHosts')[0], "HDFS", "Start");
  },

  startNameNode: function () {
    this.updateComponent('NAMENODE', this.get('newNameNodeHosts')[0], "HDFS", "Start");
  },

  bootstrapNameNode: function () {
    App.ajax.send({
      name: 'nameNode.federation.bootstrapNameNode',
      sender: this,
      data: {
        host: this.get('newNameNodeHosts')[1]
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startZKFC2: function () {
    this.updateComponent('ZKFC', this.get('newNameNodeHosts')[1], "HDFS", "Start");
  },

  startNameNode2: function () {
    this.updateComponent('NAMENODE', this.get('newNameNodeHosts')[1], "HDFS", "Start");
  },

  startAllServices: function () {
    this.startServices(false);
  }

});
