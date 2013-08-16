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

App.HighAvailabilityWizardStep5Controller = App.HighAvailabilityProgressPageController.extend({

  commands: ['stopAllServices', 'installNameNode', 'installJournalNodes', 'startJournalNodes', 'disableSNameNode', 'reconfigureHDFS'],

  stopAllServices: function () {
    App.ajax.send({
      name: 'admin.high_availability.stop_all_services',
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  installNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('isAddNameNode').hostName;
    this.createComponent('NAMENODE', hostName);
  },

  installJournalNodes: function () {
    App.ajax.send({
      name: 'admin.high_availability.create_journalnode',
      sender: this,
      success: 'onJournalNodeCreate',
      error: 'onJournalNodeCreate'
    });
  },

  onJournalNodeCreate: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.createComponent('JOURNALNODE', hostNames);
  },

  startJournalNodes: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.startComponent('JOURNALNODE', hostNames);
  },

  disableSNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').findProperty('component', 'SECONDARY_NAMENODE').hostName;
    App.ajax.send({
      name: 'admin.high_availability.maintenance_mode',
      sender: this,
      data: {
        hostName: hostName,
        componentName: 'SECONDARY_NAMENODE'
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  },

  reconfigureHDFS: function () {
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

  onLoadConfigsTags: function (data) {
    var hdfsSiteTag = data.Clusters.desired_configs['hdfs-site'].tag;
    var coreSiteTag = data.Clusters.desired_configs['core-site'].tag;
    App.ajax.send({
      name: 'admin.high_availability.load_configs',
      sender: this,
      data: {
        hdfsSiteTag: hdfsSiteTag,
        coreSiteTag: coreSiteTag
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    var hdfsSiteProperties = data.items.findProperty('type', 'hdfs-site').properties;
    var coreSiteProperties = data.items.findProperty('type', 'core-site').properties;

    var currentNameNodeHost = this.get('content.masterComponentHosts').findProperty('isCurNameNode').hostName;
    var newNameNodeHost = this.get('content.masterComponentHosts').findProperty('isAddNameNode').hostName;
    var journalNodeHosts = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    var zooKeeperHosts = this.get('content.masterComponentHosts').filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');

    //hdfs-site configs changes
    hdfsSiteProperties['dfs.nameservices'] = 'mycluster';
    hdfsSiteProperties['dfs.ha.namenodes.mycluster'] = 'nn1,nn2';
    hdfsSiteProperties['dfs.namenode.rpc-address.mycluster.nn1'] = currentNameNodeHost + ':8020';
    hdfsSiteProperties['dfs.namenode.rpc-address.mycluster.nn2'] = newNameNodeHost + ':8020';
    hdfsSiteProperties['dfs.namenode.http-address.mycluster.nn1'] = currentNameNodeHost + ':50070';
    hdfsSiteProperties['dfs.namenode.http-address.mycluster.nn2'] = newNameNodeHost + ':50070';
    hdfsSiteProperties['dfs.namenode.shared.edits.dir'] = 'qjournal://' + journalNodeHosts[0] + ':8485;' + journalNodeHosts[1] + ':8485;' + journalNodeHosts[2] + ':8485/mycluster';
    hdfsSiteProperties['dfs.client.failover.proxy.provider.mycluster'] = 'org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider';
    hdfsSiteProperties['dfs.ha.fencing.methods'] = 'shell(/bin/true)';
    hdfsSiteProperties['dfs.journalnode.edits.dir'] = '/grid/0/hdfs/journal';
    hdfsSiteProperties['dfs.ha.automatic-failover.enabled'] = 'true';

    //core-site configs changes
    coreSiteProperties['ha.zookeeper.quorum'] = zooKeeperHosts[0] + ':2181,' + zooKeeperHosts[1] + ':2181,' + zooKeeperHosts[2] + ':2181';
    coreSiteProperties['fs.defaultFS'] = 'hdfs://mycluster';
    this.set('configsSaved', false);
    App.ajax.send({
      name: 'admin.high_availability.save_configs',
      sender: this,
      data: {
        siteName: 'hdfs-site',
        properties: hdfsSiteProperties
      },
      success: 'installHDFSClients',
      error: 'onTaskError'
    });
    App.ajax.send({
      name: 'admin.high_availability.save_configs',
      sender: this,
      data: {
        siteName: 'core-site',
        properties: coreSiteProperties
      },
      success: 'installHDFSClients',
      error: 'onTaskError'
    });
  },

  configsSaved: false,

  installHDFSClients: function () {
    if (!this.get('configsSaved')) {
      this.set('configsSaved', true);
      return;
    }
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.createComponent('HDFS_CLIENT', hostNames);
  }
});

