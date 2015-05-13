/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/modification_handlers/modification_handler');

module.exports = App.ServiceConfigModificationHandler.create({
  serviceId: 'YARN',

  getDependentConfigChanges: function (changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [],
      newValue = changedConfig.get('value'),
      rangerPluginEnabledName = 'ranger-yarn-plugin-enabled',
      affectedPropertyName = changedConfig.get('name');
    if (affectedPropertyName == rangerPluginEnabledName) {
      var configYarnAclEnable = this.getConfig(allConfigs, 'yarn.acl.enable', 'yarn-site.xml', 'YARN'),
        configAuthorizationProviderClass = this.getConfig(allConfigs, 'yarn.authorization-provider', 'yarn-site.xml', 'YARN'),
        isAuthorizationProviderClassNotSet = typeof configAuthorizationProviderClass === 'undefined',
        rangerPluginEnabled = newValue == 'Yes',
        newYarnAclEnable = 'true',
        newAuthorizationProviderClass = 'org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer';

      // Add YARN-Ranger configs
      if (rangerPluginEnabled) {
        if (configYarnAclEnable != null && newYarnAclEnable !== configYarnAclEnable.get('value')) {
          affectedProperties.push({
            serviceName: 'YARN',
            sourceServiceName: 'YARN',
            propertyName: 'yarn.acl.enable',
            propertyDisplayName: 'yarn.acl.enable',
            newValue: newYarnAclEnable,
            curValue: configYarnAclEnable.get('value'),
            changedPropertyName: rangerPluginEnabledName,
            removed: false,
            filename: 'yarn-site.xml'
          });
        }
        if (isAuthorizationProviderClassNotSet || newAuthorizationProviderClass !== configAuthorizationProviderClass.get('value')) {
          affectedProperties.push({
            serviceName: 'YARN',
            sourceServiceName: 'YARN',
            propertyName: 'yarn.authorization-provider',
            propertyDisplayName: 'yarn.authorization-provider',
            newValue: newAuthorizationProviderClass,
            curValue: isAuthorizationProviderClassNotSet ? '': configAuthorizationProviderClass.get('value'),
            changedPropertyName: rangerPluginEnabledName,
            removed: false,
            isNewProperty: isAuthorizationProviderClassNotSet,
            filename: 'yarn-site.xml',
            categoryName: 'Custom yarn-site'
          });
        }
      }
    }
    return affectedProperties;
  }
});
