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
  serviceId : 'HDFS',

  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [];
    var newValue = changedConfig.get("value");
    var rangerPluginEnabledName = "ranger-hdfs-plugin-enabled";
    var affectedPropertyName = changedConfig.get("name");
    if (App.get('isHadoop23Stack') && affectedPropertyName == rangerPluginEnabledName) {
      var configAttributesProviderClass = this.getConfig(allConfigs, 'dfs.namenode.inode.attributes.provider.class', 'hdfs-site.xml', 'HDFS');
      var isAttributesProviderClassSet = typeof configAttributesProviderClass !== 'undefined';

      var rangerPluginEnabled = newValue == "Yes";
      var newDfsPermissionsEnabled = rangerPluginEnabled ? "true" : "false";
      var newAttributesProviderClass = 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer';

      if (rangerPluginEnabled && (!isAttributesProviderClassSet || newAttributesProviderClass != configAttributesProviderClass.get('value'))) {
        affectedProperties.push({
          serviceName : "HDFS",
          sourceServiceName : "HDFS",
          propertyName : 'dfs.namenode.inode.attributes.provider.class',
          propertyDisplayName : 'dfs.namenode.inode.attributes.provider.class',
          newValue : newAttributesProviderClass,
          curValue : isAttributesProviderClassSet ? configAttributesProviderClass.get('value') : '',
          changedPropertyName : rangerPluginEnabledName,
          removed : false,
          isNewProperty : !isAttributesProviderClassSet,
          filename : 'hdfs-site.xml',
          categoryName: 'Custom hdfs-site'
        });
      }
    }
    return affectedProperties;
  }
});