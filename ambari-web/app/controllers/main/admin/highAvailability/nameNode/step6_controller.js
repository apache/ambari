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

App.HighAvailabilityWizardStep6Controller = Em.Controller.extend({

  name:"highAvailabilityWizardStep6Controller",

  POLL_INTERVAL: 1000,

  isNextEnabled: function(){
    //only 3 JournalNodes could be installed
    return (this.get('initJnCounter') === 3);
  }.property('initJnCounter'),

  initJnCounter: 0,

  pullCheckPointStatus: function () {
    this.set('initJnCounter', 0);
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', "JOURNALNODE").mapProperty('hostName');
    hostNames.forEach(function (hostName) {
      this.pullEachJnStatus(hostName);
    }, this);
  },

  pullEachJnStatus: function(hostName){
    App.ajax.send({
      name: 'admin.high_availability.getJnCheckPointStatus',
      sender: this,
      data: {
        hostName: hostName
      },
      success: 'checkJnCheckPointStatus'
    });
  },

  checkJnCheckPointStatus: function (data) {
    var self = this;
    var journalStatusInfo;
    if (data.metrics && data.metrics.dfs) {
      journalStatusInfo = $.parseJSON(data.metrics.dfs.journalnode.journalsStatus);
      if (journalStatusInfo[this.get('content.nameServiceId')] && journalStatusInfo[this.get('content.nameServiceId')].Formatted === "true") {
        this.set("initJnCounter", (this.get('initJnCounter') + 1));
        return;
      }
    }

    window.setTimeout(function () {
      self.pullEachJnStatus(data.HostRoles.host_name);
    }, self.POLL_INTERVAL);
  },

  done: function () {
    if (this.get('isNextEnabled')) {
      App.router.send('next');
    }
  }

});

