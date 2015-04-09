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

require('controllers/main/admin/serviceAccounts_controller');

App.HighAvailabilityWizardStep4Controller = Em.Controller.extend({

  name:"highAvailabilityWizardStep4Controller",

  POLL_INTERVAL: 1000,

  isNextEnabled: false,

  isNameNodeStarted: true,

  pullCheckPointStatus: function () {
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', true).hostName;
    App.ajax.send({
      name: 'admin.high_availability.getNnCheckPointStatus',
      sender: this,
      data: {
        hostName: hostName
      },
      success: 'checkNnCheckPointStatus'
    });
  },

  checkNnCheckPointStatus: function (data) {
    this.set('isNameNodeStarted', data.HostRoles.desired_state === 'STARTED');
    var self = this;
    var journalTransactionInfo = $.parseJSON(Em.get(data, 'metrics.dfs.namenode.JournalTransactionInfo'));
    var isInSafeMode = !Em.isEmpty(Em.get(data, 'metrics.dfs.namenode.Safemode'));
    // in case when transaction info absent or invalid return 2 which will return false in next `if` statement
    journalTransactionInfo = !!journalTransactionInfo ? (parseInt(journalTransactionInfo.LastAppliedOrWrittenTxId) - parseInt(journalTransactionInfo.MostRecentCheckpointTxId)) : 2;
    if (journalTransactionInfo <= 1 && isInSafeMode) {
      this.set("isNextEnabled", true);
      return;
    }
    
    window.setTimeout(function () {
      self.pullCheckPointStatus();
    }, self.POLL_INTERVAL);
  },

  done: function () {
    if (this.get('isNextEnabled')) {
      App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
        App.router.send("next");
      });
    }
  }

});

