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

App.ConfigOverridable = Em.Mixin.create({

  /**
   *
   * @method createOverrideProperty
   */
  createOverrideProperty: function (event) {
    var serviceConfigProperty = event.contexts[0];
    var serviceConfigController = this.get('controller');
    var selectedConfigGroup = serviceConfigController.get('selectedConfigGroup');
    var isInstaller = (this.get('controller.name') === 'wizardStep7Controller');
    var configGroups = (isInstaller) ? serviceConfigController.get('selectedService.configGroups') : serviceConfigController.get('configGroups');

    //user property is added, and it has not been saved, not allow override
    if (serviceConfigProperty.get('isUserProperty') && serviceConfigProperty.get('isNotSaved') && !isInstaller) {
      App.ModalPopup.show({
        header: Em.I18n.t('services.service.config.configOverride.head'),
        body: Em.I18n.t('services.service.config.configOverride.body'),
        secondary: false
      });
      return;
    }
    if (selectedConfigGroup.get('isDefault')) {
      // Launch dialog to pick/create Config-group
      App.config.launchConfigGroupSelectionCreationDialog(this.get('service.serviceName'),
        configGroups, serviceConfigProperty, function (selectedGroupInPopup) {
          console.log("launchConfigGroupSelectionCreationDialog(): Selected/Created:", selectedGroupInPopup);
          if (selectedGroupInPopup) {
            serviceConfigController.set('overrideToAdd', serviceConfigProperty);
            serviceConfigController.set('selectedConfigGroup', selectedGroupInPopup);
          }
        }, isInstaller);
    }
    else {
      serviceConfigController.addOverrideProperty(serviceConfigProperty, selectedConfigGroup);
    }
  }

});
