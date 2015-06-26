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
  serviceId : 'HBASE',

  updateConfigClasses : function(configClasses, authEnabled, affectedProperties, addOldValue) {
    if (configClasses != null) {
      var xaAuthCoProcessorClass = App.get('isHadoop23Stack') ? "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
        : "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor";
      var nonXAClass = 'org.apache.hadoop.hbase.security.access.AccessController';
      var currentClassesList = configClasses.get('value').trim().length > 0 ? configClasses.get('value').trim().split(',') : [];
      var newClassesList = null, xaClassIndex, nonXaClassIndex;

      if (authEnabled) {
        var nonXaClassIndex = currentClassesList.indexOf(nonXAClass);
        if (nonXaClassIndex > -1) {
          currentClassesList.splice(nonXaClassIndex, 1);
          newClassesList = currentClassesList;
        }
        var xaClassIndex = currentClassesList.indexOf(xaAuthCoProcessorClass);
        if (xaClassIndex < 0) {
          currentClassesList.push(xaAuthCoProcessorClass);
          newClassesList = currentClassesList;
        }
      } else {
        var xaClassIndex = currentClassesList.indexOf(xaAuthCoProcessorClass);
        if (xaClassIndex > -1) {
          currentClassesList.splice(xaClassIndex, 1);
          newClassesList = currentClassesList;
        }
        if (addOldValue) {
          var nonXaClassIndex = currentClassesList.indexOf(nonXAClass);
          if (nonXaClassIndex < 0) {
            currentClassesList.push(nonXAClass);
            newClassesList = currentClassesList;
          }
        }
      }

      if (newClassesList != null) {
        affectedProperties.push({
          serviceName : "HBASE",
          sourceServiceName : "HBASE",
          propertyName : configClasses.get('name'),
          propertyDisplayName : configClasses.get('name'),
          newValue : newClassesList.join(','),
          curValue : configClasses.get('value'),
          changedPropertyName : 'ranger-hbase-plugin-enabled',
          removed : false,
          filename : 'hbase-site.xml'
        });
      }
    }
  },

  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [];
    var newValue = changedConfig.get("value");
    var hbaseAuthEnabledPropertyName = "ranger-hbase-plugin-enabled";
    var affectedPropertyName = changedConfig.get("name");
    if (affectedPropertyName == hbaseAuthEnabledPropertyName) {
      var configAuthEnabled = this.getConfig(allConfigs, 'hbase.security.authorization', 'hbase-site.xml', 'HBASE');
      var configMasterClasses = this.getConfig(allConfigs, 'hbase.coprocessor.master.classes', 'hbase-site.xml', 'HBASE');
      var configRegionClasses = this.getConfig(allConfigs, 'hbase.coprocessor.region.classes', 'hbase-site.xml', 'HBASE');

      var authEnabled = newValue == "Yes";
      var newAuthEnabledValue = authEnabled ? "true" : "false";
      var newRpcProtectionValue = authEnabled ? "privacy" : "authentication";

      // Add HBase-Ranger configs
      this.updateConfigClasses(configMasterClasses, authEnabled, affectedProperties, configAuthEnabled.get('value') == 'true');
      this.updateConfigClasses(configRegionClasses, authEnabled, affectedProperties, configAuthEnabled.get('value') == 'true');
      if (authEnabled && newAuthEnabledValue !== configAuthEnabled.get('value')) {
        affectedProperties.push({
          serviceName : "HBASE",
          sourceServiceName : "HBASE",
          propertyName : 'hbase.security.authorization',
          propertyDisplayName : 'hbase.security.authorization',
          newValue : newAuthEnabledValue,
          curValue : configAuthEnabled.get('value'),
          changedPropertyName : hbaseAuthEnabledPropertyName,
          removed : false,
          filename : 'hbase-site.xml'
        });
      }
    }
    return affectedProperties;
  }
});
