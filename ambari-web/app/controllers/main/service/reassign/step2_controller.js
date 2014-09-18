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
  useServerValidation: false,

  loadStep: function() {
    // If High Availability is enabled NameNode became a multiple component
    if (App.get('isHaEnabled')) {
      this.get('multipleComponents').push('NAMENODE');
    }
    this.clearStep();
    this.renderHostInfo();
    this.loadStepCallback(this.loadComponents(), this);

    // if moving NameNode with HA enabled
    if (this.get('content.reassign.component_name') === "NAMENODE" && App.get('isHaEnabled')) {
      this.set('showCurrentHost', false);
      this.set('componentToRebalance', 'NAMENODE');
      this.incrementProperty('rebalanceComponentHostsCounter');

    // if moving ResourceManager with HA enabled
    } else if (this.get('content.reassign.component_name') === "RESOURCEMANAGER" && App.get('isRMHaEnabled')) {
      this.set('showCurrentHost', false);
      this.set('componentToRebalance', 'RESOURCEMANAGER');
      this.incrementProperty('rebalanceComponentHostsCounter');
    } else {
      this.set('showCurrentHost', true);
      this.rebalanceSingleComponentHosts(this.get('content.reassign.component_name'));
    }
  },

  /**
   * load master components
   * @return {Array}
   */
  loadComponents: function () {
    var masterComponents = this.get('content.masterComponentHosts');
    this.set('currentHostId', this.get('content').get('reassign').host_id);
    var componentNameToReassign = this.get('content').get('reassign').component_name;
    var result = [];

    masterComponents.forEach(function (master) {
      var color = "grey";
      if (master.component == componentNameToReassign) {
        color = 'green';
      }
      result.push({
        component_name: master.component,
        display_name: App.format.role(master.component),
        selectedHost: master.hostName,
        isInstalled: true,
        serviceId: App.HostComponent.find().findProperty('componentName', master.component).get('serviceName'),
        isServiceCoHost: ['HIVE_METASTORE', 'WEBHCAT_SERVER'].contains(master.component),
        color: color
      });
    }, this);
    return result;
  },

  /**
   * rebalance single component among available hosts
   * @param componentName
   * @return {Boolean}
   */
  rebalanceSingleComponentHosts: function (componentName) {
    var currentComponents = this.get("selectedServicesMasters").filterProperty("component_name", componentName),
      availableComponentHosts = [];

    this.get("hosts").forEach(function (item) {
      if (this.get('currentHostId') !== item.get('host_name')) {
        availableComponentHosts.pushObject(item);
      }
    }, this);

    if (availableComponentHosts.length > 0) {
      currentComponents.forEach(function (item) {
        var preparedAvailableHosts = availableComponentHosts.slice(0);

        if (item.get('selectedHost') === this.get('currentHostId') && item.get('component_name') === this.get('content.reassign.component_name')) {
          item.set('selectedHost', preparedAvailableHosts.objectAt(0).host_name);
        }
        item.set("availableHosts", preparedAvailableHosts.sortProperty('host_name'));
      }, this);
      return true;
    }
    return false;
  },

  updateIsSubmitDisabled: function () {
    var isSubmitDisabled = this._super();
    if (!isSubmitDisabled) {
      var reassigned = 0;
      var existedComponents = App.HostComponent.find().filterProperty('componentName', this.get('content.reassign.component_name')).mapProperty('hostName');
      var newComponents = this.get('servicesMasters').mapProperty('selectedHost');
      existedComponents.forEach(function (host) {
        if (!newComponents.contains(host)) {
          reassigned++;
        }
      }, this);
      isSubmitDisabled = reassigned !== 1;
    }
    this.set('submitDisabled', isSubmitDisabled);
    return isSubmitDisabled;
  }.observes('servicesMasters.@each.selectedHost', 'servicesMasters.@each.isHostNameValid')
});

