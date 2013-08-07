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
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.installComponent('HDFS_CLIENT', hostNames);
  }
});

