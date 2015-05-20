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
  serviceId : 'HIVE',

  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [];
    var newValue = changedConfig.get("value");
    var rangerPluginEnabledName = "ranger-hive-plugin-enabled";
    var affectedPropertyName = changedConfig.get("name");
    if (affectedPropertyName == rangerPluginEnabledName) {
      var configAuthorizationEnabled = this.getConfig(allConfigs, 'hive.security.authorization.enabled', 'hive-site.xml', 'HIVE');
      var configAuthorizationManager = this.getConfig(allConfigs, 'hive.security.authorization.manager', 'hiveserver2-site.xml', 'HIVE');
      var configAuthenticatorManager = this.getConfig(allConfigs, 'hive.security.authenticator.manager', 'hiveserver2-site.xml', 'HIVE');
      var configRestrictedList = this.getConfig(allConfigs, 'hive.conf.restricted.list', 'hive-site.xml', 'HIVE');

      var rangerPluginEnabled = newValue == "Yes";
      var newConfigAuthorizationEnabledValue = rangerPluginEnabled ? "true" : "false";
      var newAuthorizationManagerValue = rangerPluginEnabled ? (App.get('isHadoop23Stack') ? "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory"
          : "com.xasecure.authorization.hive.authorizer.XaSecureHiveAuthorizerFactory")
         : "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory";
      var newAuthenticatorManagerValue = rangerPluginEnabled ? "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
          : "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator";
      var enabledRestrictedMap = {
        "hive.security.authorization.enabled" : "hive.security.authorization.enabled",
        "hive.security.authorization.manager" : "hive.security.authorization.manager",
        "hive.security.authenticator.manager" : "hive.security.authenticator.manager"
      }
      var enabledRestrictedList = Object.keys(enabledRestrictedMap);
      var newRestrictedListValue = rangerPluginEnabled ? enabledRestrictedList : [];

      // Add Hive-Ranger configs
      if (configAuthorizationEnabled != null && newConfigAuthorizationEnabledValue !== configAuthorizationEnabled.get('value')) {
        affectedProperties.push({
          serviceName : "HIVE",
          sourceServiceName : "HIVE",
          propertyName : 'hive.security.authorization.enabled',
          propertyDisplayName : 'hive.security.authorization.enabled',
          newValue : newConfigAuthorizationEnabledValue,
          curValue : configAuthorizationEnabled.get('value'),
          changedPropertyName : rangerPluginEnabledName,
          removed : false,
          filename : 'hive-site.xml'
        });
      }
      if (configAuthorizationManager != null && newAuthorizationManagerValue !== configAuthorizationManager.get('value')) {
        affectedProperties.push({
          serviceName : "HIVE",
          sourceServiceName : "HIVE",
          propertyName : 'hive.security.authorization.manager',
          propertyDisplayName : 'hive.security.authorization.manager',
          newValue : newAuthorizationManagerValue,
          curValue : configAuthorizationManager.get('value'),
          changedPropertyName : rangerPluginEnabledName,
          removed : false,
          filename : 'hiveserver2-site.xml'
        });
      }
      if (configAuthenticatorManager != null && newAuthenticatorManagerValue !== configAuthenticatorManager.get('value')) {
        affectedProperties.push({
          serviceName : "HIVE",
          sourceServiceName : "HIVE",
          propertyName : 'hive.security.authenticator.manager',
          propertyDisplayName : 'hive.security.authenticator.manager',
          newValue : newAuthenticatorManagerValue,
          curValue : configAuthenticatorManager.get('value'),
          changedPropertyName : rangerPluginEnabledName,
          removed : false,
          filename : 'hiveserver2-site.xml'
        });
      }
      if (configRestrictedList != null) {
        var currentValueList = configRestrictedList.get('value').split(',');
        // 'newRestrictedListValue' elements should be found in existing list
        var newValueList = [];
        currentValueList.forEach(function(s) {
          if (enabledRestrictedMap[s] == s) {
            return;
          }
          newValueList.push(s);
        });
        if (newRestrictedListValue.length > 0)
          newValueList = newValueList.concat(newRestrictedListValue);

        if (newValueList.length != currentValueList.length) {
          // One of the value was not found - set all of them in.
          var newValueListString = newValueList.join(',');
          affectedProperties.push({
            serviceName : "HIVE",
            sourceServiceName : "HIVE",
            propertyName : 'hive.conf.restricted.list',
            propertyDisplayName : 'hive.conf.restricted.list',
            newValue : newValueListString,
            curValue : configRestrictedList.get('value'),
            changedPropertyName : rangerPluginEnabledName,
            removed : false,
            filename : 'hive-site.xml'
          });
        }
      }
    }
    return affectedProperties;
  }
});