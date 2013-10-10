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

App.ReassignMasterWizardStep2Controller = App.WizardStep5Controller.extend({

  loadStep: function() {
    this._super();
    this.rebalanceComponentHosts('NAMENODE');
  },

  loadComponents: function () {
    var components = this.get('components').filterProperty('isMaster', true);
    var masterComponents = this.get('content.masterComponentHosts');
    var result = [];
    masterComponents.forEach(function (master) {
      result.push({
        component_name: master.component,
        display_name: App.format.role(master.component),
        selectedHost: master.hostName,
        isInstalled: true,
        serviceId: App.HostComponent.find().findProperty('componentName', master.component).get('serviceName'),
        availableHosts: [],
        isHiveCoHost: ['HIVE_METASTORE', 'WEBHCAT_SERVER'].contains(master.component)
      });
    }, this);
    return result;
  }
});

