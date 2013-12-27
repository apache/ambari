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

require('controllers/wizard/step5_controller');

App.HighAvailabilityWizardStep2Controller = App.WizardStep5Controller.extend({

  name:"highAvailabilityWizardStep2Controller",

  /**
   * Load services info to appropriate variable and return masterComponentHosts
   * @return Array
   */
  masterHostMapping:function () {
    var mapping = [], mappingObject, self = this, mappedHosts, hostObj, hostInfo;
    //get the unique assigned hosts and find the master services assigned to them


    mappedHosts = this.get("selectedServicesMasters").mapProperty("selectedHost").uniq();

    mappedHosts.forEach(function (item) {

      hostObj = self.get("hosts").findProperty("host_name", item);
      console.log("Name of the host is: " + hostObj.host_name);

      var masterServices = self.get("selectedServicesMasters").filterProperty("selectedHost", item);
      masterServices.forEach(function(item){
        if(item.component_name == "NAMENODE" || item.component_name == "JOURNALNODE"){
          item.set('color','green');
        }else{
          item.set('color','grey');
        }
      })

      mappingObject = Ember.Object.create({
        host_name:item,
        hostInfo:hostObj.host_info,
        masterServices:masterServices
      });

      mapping.pushObject(mappingObject);
    }, this);

    mapping.sort(this.sortHostsByName);

    return mapping;

  }.property("selectedServicesMasters.@each.selectedHost"),

  /**
   * Put master components to <code>selectedServicesMasters</code>, which will be automatically rendered in template
   * @param masterComponents
   */
  renderComponents:function (masterComponents) {
    var services = this.get('content.services')
      .filterProperty('isInstalled', true).filterProperty('isInstalled', true).mapProperty('serviceName'); //list of shown services

    var result = [];

    var curNameNode = masterComponents.findProperty('component_name',"NAMENODE");
    curNameNode.isCurNameNode = true;
    curNameNode.zId = 0;

    //Create JOURNALNODE
    for (var index = 0; index < 3; index++) {
      masterComponents.push(
        {
          availableHosts: [],
          component_name: "JOURNALNODE",
          display_name: "JournalNode",
          isHiveCoHost: false,
          isInstalled: false,
          selectedHost: this.get("hosts")[index].get("host_name"),
          serviceId: "HDFS",
          zId: index
        }
      )
    }
    //Create Additional NameNode
    masterComponents.push(
      {
        availableHosts: [],
        component_name: "NAMENODE",
        display_name: "NameNode",
        isHiveCoHost: false,
        isInstalled: false,
        selectedHost: this.get("hosts").mapProperty('host_name').without(curNameNode.selectedHost)[0],
        serviceId: "HDFS",
        isAddNameNode: true,
        zId: 1
      }
    )

    masterComponents.forEach(function (item) {
      var componentObj = Ember.Object.create(item);
      componentObj.set("availableHosts", this.get("hosts"));
      result.push(componentObj);
    }, this);

    this.set("selectedServicesMasters", result);

    var components = result.filterProperty('component_name',"NAMENODE");
    components.push.apply(components, result.filterProperty('component_name',"JOURNALNODE"));

    this.set('servicesMasters', components);
    this.rebalanceComponentHosts("NAMENODE");
    this.rebalanceComponentHosts("JOURNALNODE");
  }

});

