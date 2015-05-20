/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/modification_handlers/modification_handler');

module.exports = App.ServiceConfigModificationHandler.create({
  serviceId : 'STORM',

  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [];
    var newValue = changedConfig.get("value");
    var rangerPluginEnablePropertyName = "ranger-storm-plugin-enabled";
    var affectedPropertyName = changedConfig.get("name");
    if (affectedPropertyName == rangerPluginEnablePropertyName) {
      var authEnabled = newValue == "Yes";
      var configNimbusAuthorizer = this.getConfig(allConfigs, 'nimbus.authorizer', 'storm-site.xml', 'STORM');
      if (configNimbusAuthorizer != null) {
        // Only when configuration is already present, do we act on it.
        // Unsecured clusters do not have this config, and hence we skip any
        // updates
        var newNimbusAuthorizer = authEnabled ? (App.get('isHadoop23Stack') ? "org.apache.ranger.authorization.storm.authorizer.RangerStormAuthorizer"
              : "com.xasecure.authorization.storm.authorizer.XaSecureStormAuthorizer")
            : "backtype.storm.security.auth.authorizer.SimpleACLAuthorizer";

        // Add Storm-Ranger configs
        if (newNimbusAuthorizer !== configNimbusAuthorizer.get('value')) {
          affectedProperties.push({
            serviceName : "STORM",
            sourceServiceName : "STORM",
            propertyName : 'nimbus.authorizer',
            propertyDisplayName : 'nimbus.authorizer',
            newValue : newNimbusAuthorizer,
            curValue : configNimbusAuthorizer.get('value'),
            changedPropertyName : rangerPluginEnablePropertyName,
            removed : false,
            filename : 'storm-site.xml'
          });
        }
      }
      if (authEnabled && affectedProperties.length < 1 && !securityEnabled) {
        App.ModalPopup.show({
          header : Em.I18n.t('services.storm.configs.range-plugin-enable.dialog.title'),
          primary : Em.I18n.t('ok'),
          secondary : false,
          showCloseButton : false,
          onPrimary : function() {
            this.hide();
          },
          body : Em.I18n.t('services.storm.configs.range-plugin-enable.dialog.message')
        });
      }
    }
    return affectedProperties;
  }
});