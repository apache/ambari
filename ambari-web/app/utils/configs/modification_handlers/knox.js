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
  serviceId : 'KNOX',

  getDependentConfigChanges : function(changedConfig, selectedServices, allConfigs, securityEnabled) {
    var affectedProperties = [];
    var newValue = changedConfig.get("value");
    var rangerPluginEnablePropertyName = "ranger-knox-plugin-enabled";
    var affectedPropertyName = changedConfig.get("name");
    if (affectedPropertyName == rangerPluginEnablePropertyName) {
      var topologyXmlContent = this.getConfig(allConfigs, 'content', 'topology.xml', 'KNOX');
      if (topologyXmlContent != null) {
        var topologyXmlContentString = topologyXmlContent.get('value');
        var newTopologyXmlContentString = null;
        var authEnabled = newValue == "Yes";
        var authXml = /<provider>[\s]*<role>[\s]*authorization[\s]*<\/role>[\s\S]*?<\/provider>/.exec(topologyXmlContentString);
        if (authXml != null && authXml.length > 0) {
          var nameArray = /<name>\s*(.*?)\s*<\/name>/.exec(authXml[0]);
          if (nameArray != null && nameArray.length > 1) {
            if (authEnabled && 'AclsAuthz' == nameArray[1]) {
              var newName = nameArray[0].replace('AclsAuthz', 'XASecurePDPKnox');
              var newAuthXml = authXml[0].replace(nameArray[0], newName);
              newTopologyXmlContentString = topologyXmlContentString.replace(authXml[0], newAuthXml);
            } else if (!authEnabled && 'XASecurePDPKnox' == nameArray[1]) {
              var newName = nameArray[0].replace('XASecurePDPKnox', 'AclsAuthz');
              var newAuthXml = authXml[0].replace(nameArray[0], newName);
              newTopologyXmlContentString = topologyXmlContentString.replace(authXml[0], newAuthXml);
            }
          }
        }
        if (newTopologyXmlContentString != null) {
          affectedProperties.push({
            serviceName : "KNOX",
            sourceServiceName : "KNOX",
            propertyName : 'content',
            propertyDisplayName : 'content',
            newValue : newTopologyXmlContentString,
            curValue : topologyXmlContent.get('value'),
            changedPropertyName : rangerPluginEnablePropertyName,
            removed : false,
            filename : 'topology.xml'
          });
        }
      }
    }
    return affectedProperties;
  }
});