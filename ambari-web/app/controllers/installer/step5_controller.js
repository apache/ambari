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

//mock data

//input
App.selectedServices = [ 'HDFS', 'MapReduce', 'Ganglia', 'Nagios', 'HBase', 'Pig', 'Sqoop', 'Oozie', 'Hive', 'ZooKeeper'];

App.hosts = [
  {
    host_name: 'host1',
    cluster_name: "test",
    total_mem: 7,
    cpu_count: 2
  },
  {
    host_name: 'host2',
    cluster_name: "test",
    total_mem: 4,
    cpu_count: 2
  },
  {
    host_name: 'host3',
    cluster_name: "test",
    total_mem: 8,
    cpu_count: 2
  },
  {
    host_name: 'host4',
    cluster_name: "test",
    total_mem: 8,
    cpu_count: 2
  },
  {
    host_name: 'host5',
    cluster_name: "test",
    total_mem: 8,
    cpu_count: 2
  }
];

App.masterServices = [
  {
    component_name: "NameNode",
    selectedHost: 'host1',
    availableHosts: [] // filled dynAmically
  },
  {
    component_name: "ZooKeeper",
    selectedHost: 'host3',
    availableHosts: [] // filled dynAmically
  },
  {
    component_name: "JobTracker",
    selectedHost: 'host2',
    availableHosts: [] // filled dynAmically
  },
  {
    component_name: "HBase Master",
    selectedHost: 'host3',
    availableHosts: [] // filled dynAmically
  }
];

//mapping format
//masterHostMapping = [
//    {
//      host_name: 'host1',
//      masterServices: [{component_name:"NamedNode"}, {component_name:"Jobtracker"}]
//    },
//    {
//      host_name: 'host2',
//      masterServices: [{component_name:"NamedNode"}, {component_name:"Jobtracker"}]
//    }
//  ];

//end - mock data

App.InstallerStep5Controller = Em.Controller.extend({
  //properties
  name: "installerStep5Controller",

  hosts: [],
  selectedServices: [],
  selectedServicesMasters: [],

  masterHostMapping: function () {
    var mapping = [], mappingObject, self = this, mappedHosts, hostObj, hostInfo;
    //get the unique assigned hosts and find the master services assigned to them

    mappedHosts = this.get("selectedServicesMasters").mapProperty("selectedHost").uniq();

    mappedHosts.forEach(function (item) {
      hostObj = self.get("hosts").findProperty("host_name", item);
      hostInfo = " ( " + hostObj.get("total_mem") + "GB" + " " + hostObj.get("cpu_count") + "cores )";

      mappingObject = Ember.Object.create({
        host_name: item,
        hostInfo: hostInfo,
        masterServices: self.get("selectedServicesMasters").filterProperty("selectedHost", item)
      });

      mapping.pushObject(mappingObject);
    }, this);

    mapping.sort(this.sortHostsByName);

    return mapping;

  }.property("selectedServicesMasters.@each.selectedHost"),

  remainingHosts: function () {
    return (this.get("hosts.length") - this.get("masterHostMapping.length"));
  }.property("selectedServicesMasters.@each.selectedHost"),

  hasZookeeper: function () {
    return this.selectedServices.findProperty("service_name", "ZooKeeper");
  }.property("selectedServices"),

  //methods
  getAvailableHosts: function (componentName) {
    var assignableHosts = [],
      zookeeperHosts = null;

    if (componentName === "ZooKeeper") {
      zookeeperHosts = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").mapProperty("selectedHost").uniq();

      this.get("hosts").forEach(function (item) {
        if (!(zookeeperHosts.contains(item.get("host_name")))) {
          assignableHosts.pushObject(item);
        }
      }, this);

      return assignableHosts;

    } else {
      return this.get("hosts");
    }
  },

  assignHostToMaster: function (masterService, selectedHost, zId) {
    if (selectedHost && masterService) {

      if ((masterService === "ZooKeeper") && zId) {
        this.get('selectedServicesMasters').findProperty("zId", zId).set("selectedHost", selectedHost);
        this.rebalanceZookeeperHosts();
      }
      else {
        this.get('selectedServicesMasters').findProperty("component_name", masterService).set("selectedHost", selectedHost);
      }

    }
  },

  addZookeepers: function () {
    /*
     *Logic: If ZooKeeper service is selected then there can be
     * minimum 1 ZooKeeper master in total, and
     * maximum 1 ZooKeeper on every host
     */
    var maxNumZooKeepers = this.get("hosts.length"),
      currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper"),
      newZookeeper = null,
      zookeeperHosts = null,
      suggestedHost = null,
      i = 0,
      lastZoo = null;

    //work only if the Zookeeper service is selected in previous step
    if (!this.get("selectedServices").mapProperty("service_name").contains("ZooKeeper")) {
      return false;
    }

    if (currentZooKeepers.get("length") < maxNumZooKeepers) {
      currentZooKeepers.set("lastObject.showAddControl", false);
      if (currentZooKeepers.get("length") > 1) {
        currentZooKeepers.set("lastObject.showRemoveControl", true);
      }

      //create a new zookeeper based on an existing one
      newZookeeper = Ember.Object.create({});
      lastZoo = currentZooKeepers.get("lastObject");
      newZookeeper.set("component_name", lastZoo.get("component_name"));
      newZookeeper.set("selectedHost", lastZoo.get("selectedHost"));
      newZookeeper.set("availableHosts", this.getAvailableHosts("ZooKeeper"));

      if (currentZooKeepers.get("length") === (maxNumZooKeepers - 1)) {
        newZookeeper.set("showAddControl", false);
      } else {
        newZookeeper.set("showAddControl", true);
      }
      newZookeeper.set("showRemoveControl", true);

      //get recommended host for the new Zookeeper server
      zookeeperHosts = currentZooKeepers.mapProperty("selectedHost").uniq();

      for (i = 0; i < this.get("hosts.length"); i++) {
        if (!(zookeeperHosts.contains(this.get("hosts")[i].get("host_name")))) {
          suggestedHost = this.get("hosts")[i].get("host_name");
          break;
        }
      }

      newZookeeper.set("selectedHost", suggestedHost);
      newZookeeper.set("zId", (currentZooKeepers.get("lastObject.zId") + 1));

      this.get("selectedServicesMasters").pushObject(newZookeeper);

      this.rebalanceZookeeperHosts();

      return true;
    }
    return false;//if no more zookeepers can be added
  },

  removeZookeepers: function (zId) {
    var currentZooKeepers;

    //work only if the Zookeeper service is selected in previous step
    if (!this.get("selectedServices").mapProperty("service_name").contains("ZooKeeper")) {
      return false;
    }

    currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper");

    if (currentZooKeepers.get("length") > 1) {
      this.get("selectedServicesMasters").removeAt(this.get("selectedServicesMasters").indexOf(this.get("selectedServicesMasters").findProperty("zId", zId)));

      currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper");
      if (currentZooKeepers.get("length") < this.get("hosts.length")) {
        currentZooKeepers.set("lastObject.showAddControl", true);
      }

      this.rebalanceZookeeperHosts();

      return true;
    }

    return false;

  },

  rebalanceZookeeperHosts: function () {
    //for a zookeeper update the available hosts for the other zookeepers

    var currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper"),
      zooHosts = currentZooKeepers.mapProperty("selectedHost"),
      availableZooHosts = [],
      preparedAvailableHosts = null;

    //get all hosts available for zookeepers
    this.get("hosts").forEach(function (item) {
      if (!zooHosts.contains(item.get("host_name"))) {
        availableZooHosts.pushObject(item);
      }
    }, this);

    currentZooKeepers.forEach(function (item) {
      preparedAvailableHosts = availableZooHosts.slice(0);
      preparedAvailableHosts.pushObject(this.get("hosts").findProperty("host_name", item.get("selectedHost")))
      preparedAvailableHosts.sort(this.sortHostsByConfig, this);
      item.set("availableHosts", preparedAvailableHosts);
    }, this);

  },

  sortHostsByConfig: function (a, b) {
    //currently handling only total memory on the host
    if (a.total_mem < b.total_mem) {
      return 1;
    }
    else {
      return -1;
    }
  },

  sortHostsByName: function (a, b) {
    if (a.host_name > b.host_name) {
      return 1;
    }
    else {
      return -1;
    }
  },

  /*
   * Initialize the model data
   */
  init: function () {
    var zookeeperComponent = null, componentObj = null, hostObj = null;
    this._super();

    //wrap the model data into

    App.hosts.forEach(function (item) {
      hostObj = Ember.Object.create(item);
      hostObj.set("host_info", "" + hostObj.get("host_name") + " ( " + hostObj.get("total_mem") + "GB" + " " + hostObj.get("cpu_count") + "cores )");
      this.get("hosts").pushObject(hostObj);
    }, this);

    //sort the hosts
    this.get("hosts").sort(this.sortHostsByConfig);

    //todo: build masters from config instead
    App.masterServices.forEach(function (item) {
      //add the zookeeper component at the end if exists
      if (item.component_name === "ZooKeeper") {
        zookeeperComponent = Ember.Object.create(item);
      } else {
        componentObj = Ember.Object.create(item);
        componentObj.set("availableHosts", this.get("hosts").slice(0));
        this.get("selectedServicesMasters").pushObject(componentObj);
      }
    }, this);

    //while initialization of the controller there will be only 1 zookeeper server

    if (zookeeperComponent) {
      zookeeperComponent.set("showAddControl", true);
      zookeeperComponent.set("showRemoveControl", false);
      zookeeperComponent.set("zId", 1);
      zookeeperComponent.set("availableHosts", this.get("hosts").slice(0));
      this.get("selectedServicesMasters").pushObject(Ember.Object.create(zookeeperComponent));
    }

    App.selectedServices.forEach(function (item) {
      this.get("selectedServices").pushObject(Ember.Object.create({service_name: item}));
    }, this);

  }

});



