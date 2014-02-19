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

  currentHostId: null,
  showCurrentHost: true,

  loadStep: function() {
    this._super();
    if(this.get('content.reassign.component_name') == "NAMENODE" && !this.get('content.masterComponentHosts').findProperty('component', "SECONDARY_NAMENODE")){
      this.set('showCurrentHost', false);
      this.set('componentToRebalance', 'NAMENODE');
      this.incrementProperty('rebalanceComponentHostsCounter');
    }else{
      this.set('showCurrentHost', true);
      this.rebalanceSingleComponentHosts(this.get('content.reassign.component_name'));
    }
  },

  loadComponents: function () {
    var components = this.get('components').filterProperty('isMaster', true);
    var masterComponents = this.get('content.masterComponentHosts');
    this.set('currentHostId', this.get('content').get('reassign').host_id);
    var componentNameToReassign = this.get('content').get('reassign').component_name;
    var result = [];
    masterComponents.forEach(function (master) {
      var color = "grey";
      if(master.component == componentNameToReassign){
        color = 'green';
      }
      result.push({
        component_name: master.component,
        display_name: App.format.role(master.component),
        selectedHost: master.hostName,
        isInstalled: true,
        serviceId: App.HostComponent.find().findProperty('componentName', master.component).get('serviceName'),
        isHiveCoHost: ['HIVE_METASTORE', 'WEBHCAT_SERVER'].contains(master.component),
        color: color
      });
    }, this);
    return result;
  },

  rebalanceSingleComponentHosts:function (componentName) {
    var currentComponents = this.get("selectedServicesMasters").filterProperty("component_name", componentName),
      componentHosts = currentComponents.mapProperty("selectedHost"),
      availableComponentHosts = [],
      preparedAvailableHosts = null;
    this.get("hosts").forEach(function (item) {
      if (this.get('currentHostId') !== item.get('host_name')) {
        availableComponentHosts.pushObject(item);
      }
    }, this);
    if (availableComponentHosts.length == 0) {
      return;
    }
    currentComponents.forEach(function (item) {
      preparedAvailableHosts = availableComponentHosts.slice(0);
      if (item.get('selectedHost') == this.get('currentHostId') && item.get('component_name') == this.get('content.reassign.component_name')) {
        item.set('selectedHost', preparedAvailableHosts.objectAt(0).host_name);
      }
      preparedAvailableHosts.sortBy('host_name');
      item.set("availableHosts", preparedAvailableHosts);
    }, this);
  }
});

