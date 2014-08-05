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

App.RMHighAvailabilityWizardStep2Controller = App.WizardStep5Controller.extend({
  name: "rMHighAvailabilityWizardStep2Controller",

  loadStepCallback: function (components, self) {
    this._super(components, self);
    self.hideUnusedComponents();
  },

  renderComponents: function (masterComponents) {
    var existedRM = masterComponents.findProperty('component_name', 'RESOURCEMANAGER');
    existedRM.isAdditional = false;
    var additionalRMSelectedHost = this.get('content.rmHosts.additionalRM') ||
        this.get('hosts').mapProperty('host_name').without(existedRM.selectedHost)[0];
    var additionalRM = $.extend({}, existedRM, {
      isInstalled: false,
      isAdditional: true,
      selectedHost: additionalRMSelectedHost
    });
    masterComponents.push(additionalRM);
    this._super(masterComponents);
  },

  /**
   * Remove service masters, that should be hidden in this wizard
   */
  hideUnusedComponents: function () {
    var servicesMasters = this.get('servicesMasters');
    servicesMasters = servicesMasters.filterProperty('component_name', 'RESOURCEMANAGER');
    this.set('servicesMasters', servicesMasters);
  }
});

