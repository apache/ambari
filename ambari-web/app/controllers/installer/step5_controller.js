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

App.InstallerStep5Controller = Em.Controller.extend({
  //properties
  name: "installerStep5Controller",
  hosts: [],
  selectedServices: [],
  selectedServicesMasters: [],
  zId: 0,
  components: require('data/service_components'),

  /*
   Below function retrieves host information from local storage
   */

  clearStep: function () {
    this.set('hosts', []);
    this.set('selectedServices', []);
    this.set('selectedServicesMasters', []);
    this.set('zId', 0);
  },

  loadStep: function () {
    console.log("TRACE: Loading step5: Assign Masters");
    this.clearStep();
    this.renderHostInfo(this.loadHostInfo());
    this.renderComponents(this.loadComponents(this.loadServices()));
  },

  loadHostInfo: function () {
    var hostInfo = [];
    hostInfo = App.db.getHosts();
    var hosts = new Ember.Set();
    for (var index in hostInfo) {
      hosts.add(hostInfo[index]);
      console.log("TRACE: host name is: " + hostInfo[index].name);
    }
    return hosts.filterProperty('bootStatus', 'success');
  },

  renderHostInfo: function (hostsInfo) {

    //wrap the model data into

    hostsInfo.forEach(function (_host) {
      var hostObj = Ember.Object.create({
        host_name: _host.name,
        cpu: _host.cpu,
        memory: _host.memory
      });
      console.log('pushing ' + hostObj.host_name);
      hostObj.set("host_info", "" + hostObj.get("host_name") + " ( " + hostObj.get("memory") + "GB" + " " + hostObj.get("cpu") + "cores )");
      this.get("hosts").pushObject(hostObj);
    }, this);

  },

  loadServices: function () {
    var serviceInfo = App.db.getService();
    var services = serviceInfo.filterProperty('isSelected', true).mapProperty('serviceName');
    services.forEach(function (item) {
      console.log("TRACE: service name is: " + item);
      this.get("selectedServices").pushObject(Ember.Object.create({service_name: item}));
    }, this);

    return services;

  },

  loadComponents: function (services) {
    var components = new Ember.Set();
    if (App.db.getMasterComponentHosts() === undefined) {
      var masterComponents = this.components.filterProperty('isMaster', true);
      for (var index in services) {
        var componentInfo = masterComponents.filterProperty('service_name', services[index]);
        componentInfo.forEach(function (_componentInfo) {
          console.log("TRACE: master component name is: " + _componentInfo.display_name);
          var componentObj = {};
          componentObj.component_name = _componentInfo.display_name;
          componentObj.selectedHost = this.selectHost(_componentInfo.component_name);   // call the method that plays selectNode algorithm or fetches from server
          componentObj.availableHosts = [];
          components.add(componentObj);
        }, this);
      }
    } else {
      var masterComponentHosts = App.db.getMasterComponentHosts();
      masterComponentHosts.forEach(function (_masterComponentHost) {
        var componentObj = {};
        componentObj.component_name = _masterComponentHost.component;
        componentObj.selectedHost = _masterComponentHost.hostName;   // call the method that plays selectNode algorithm or fetches from server
        componentObj.availableHosts = [];
        components.add(componentObj);
      }, this);
    }
    return components;
  },

  getMasterComponents: function () {
    return (this.get('selectedServicesMasters').slice(0));
  },

  renderComponents: function (masterComponents) {
    var zookeeperComponent = null, componentObj = null;
    var services = [];
    services = this.getMasterComponents();
    if (services.length) {
      this.set('selectedServicesMasters', []);
    }

    masterComponents.forEach(function (item) {
      //add the zookeeper component at the end if exists
      if (item.component_name === "ZooKeeper") {
        if (services.length) {
          services.forEach(function (_service) {
            this.get('selectedServicesMasters').pushObject(_service);
          }, this);
        }
        this.set('zId', parseInt(this.get('zId')) + 1);
        zookeeperComponent = Ember.Object.create(item);
        zookeeperComponent.set('zId', this.get('zId'));
        zookeeperComponent.set("showRemoveControl", true);
        zookeeperComponent.set("availableHosts", this.get("hosts").slice(0));
        this.get("selectedServicesMasters").pushObject(Ember.Object.create(zookeeperComponent));

      } else {
        componentObj = Ember.Object.create(item);
        componentObj.set("availableHosts", this.get("hosts").slice(0));
        this.get("selectedServicesMasters").pushObject(componentObj);
      }
    }, this);
  },

  getKerberosServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[1];
    } else if (noOfHosts <= 5) {
      return hosts[1];
    } else if (noOfHosts <= 30) {
      return hosts[3];
    } else {
      return hosts[5];
    }
  },

  getNameNode: function (noOfHosts) {
    var hosts = this.get('hosts');
    return hosts[0];
  },

  getSNameNode: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else {
      return hosts[1];
    }
  },

  getJobTracker: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[1];
    } else if (noOfHosts <= 5) {
      return hosts[1];
    } else if (noOfHosts <= 30) {
      return hosts[1];
    } else {
      return hosts[2];
    }
  },

  getHBaseMaster: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[0];
    } else if (noOfHosts <= 5) {
      return hosts[0];
    } else if (noOfHosts <= 30) {
      return hosts[2];
    } else {
      return hosts[3];
    }
  },

  getOozieServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[1];
    } else if (noOfHosts <= 5) {
      return hosts[1];
    } else if (noOfHosts <= 30) {
      return hosts[2];
    } else {
      return hosts[3];
    }
  },

  getOozieServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[1];
    } else if (noOfHosts <= 5) {
      return hosts[1];
    } else if (noOfHosts <= 30) {
      return hosts[2];
    } else {
      return hosts[3];
    }
  },

  getHiveServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[1];
    } else if (noOfHosts <= 5) {
      return hosts[1];
    } else if (noOfHosts <= 30) {
      return hosts[2];
    } else {
      return hosts[4];
    }
  },

  getTempletonServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else if (noOfHosts < 3) {
      return hosts[1];
    } else if (noOfHosts <= 5) {
      return hosts[1];
    } else if (noOfHosts <= 30) {
      return hosts[2];
    } else {
      return hosts[4];
    }
  },

  getZooKeeperServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts < 3) {
      return [hosts[0].host_name];
    } else {
      return [hosts[0].host_name, hosts[1].host_name, hosts[2].host_name];
    }
  },

  getGangliaServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    var hostnames = [];
    var inc = 0;
    hosts.forEach(function (_hostname) {
      hostnames[inc] = _hostname.host_name;
      inc++;
    });
    var hostExcAmbari = hostnames.without(location.hostname);
    if (hostExcAmbari !== null || hostExcAmbari !== undefined || hostExcAmbari.length !== 0) {
      return hostExcAmbari[0];
    } else {
      return hostnames[0];
    }
  },

  getNagiosServer: function (noOfHosts) {
    var hosts = this.get('hosts');
    var hostnames = [];
    var inc = 0;
    hosts.forEach(function (_hostname) {
      hostnames[inc] = _hostname.host_name;
      inc++;
    });
    var hostExcAmbari = hostnames.without(location.hostname);
    if (hostExcAmbari !== null || hostExcAmbari !== undefined || hostExcAmbari.length !== 0) {
      return hostExcAmbari[0];
    } else {
      return hostnames[0];
    }
  },

  selectHost: function (componentName) {
    var noOfHosts = this.get('hosts').length;
    if (componentName === 'KERBEROS_SERVER') {
      return this.getKerberosServer(noOfHosts).host_name;
    } else if (componentName === 'NAMENODE') {
      return this.getNameNode(noOfHosts).host_name;
    } else if (componentName === 'SNAMENODE') {
      return this.getSNameNode(noOfHosts).host_name;
    } else if (componentName === 'JOBTRACKER') {
      return this.getJobTracker(noOfHosts).host_name;
    } else if (componentName === 'HBASE_MASTER') {
      return this.getHBaseMaster(noOfHosts).host_name;
    } else if (componentName === 'OOZIE_SERVER') {
      return this.getOozieServer(noOfHosts).host_name;
    } else if (componentName === 'HIVE_SERVER') {
      return this.getHiveServer(noOfHosts).host_name;
    } else if (componentName === 'TEMPLETON_SERVER') {
      return this.getTempletonServer(noOfHosts).host_name;
    } else if (componentName === 'ZOOKEEPER_SERVER') {
      var zhosts = this.getZooKeeperServer(noOfHosts);
      var extraHosts = zhosts.slice(0, zhosts.length - 1);
      var zooKeeperHosts = new Ember.Set();
      extraHosts.forEach(function (_host) {
        var zooKeeperHost = {};
        zooKeeperHost.component_name = 'ZooKeeper';
        zooKeeperHost.selectedHost = _host;
        zooKeeperHost.availableHosts = [];
        zooKeeperHosts.add(zooKeeperHost);
      });
      this.renderComponents(zooKeeperHosts);
      var lastHost = zhosts[zhosts.length - 1];
      return lastHost;
    } else if (componentName === 'GANGLIA_MONITOR_SERVER') {
      return this.getGangliaServer(noOfHosts);
    } else if (componentName === 'NAGIOS_SERVER') {
      return this.getNagiosServer(noOfHosts);
    }
  },

  masterHostMapping: function () {
    var mapping = [], mappingObject, self = this, mappedHosts, hostObj, hostInfo;
    //get the unique assigned hosts and find the master services assigned to them

    mappedHosts = this.get("selectedServicesMasters").mapProperty("selectedHost").uniq();

    mappedHosts.forEach(function (item) {
      hostObj = self.get("hosts").findProperty("host_name", item);
      console.log("Name of the host is: " + hostObj.host_name);
      hostInfo = " ( " + hostObj.get("memory") + "GB" + " " + hostObj.get("cpu") + "cores )";

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

  lastZooKeeper: function () {
    var currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper");
    if (currentZooKeepers) {
      var lastZooKeeper = currentZooKeepers.get("lastObject");
      return lastZooKeeper;
    } else {
      return null;
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
    console.log('hosts legth is: ' + maxNumZooKeepers);
    //work only if the Zookeeper service is selected in previous step
    if (!this.get("selectedServices").mapProperty("service_name").contains("ZOOKEEPER")) {
      console.log('ALERT: Zookeeper service was not selected');
      return false;
    }

    if (currentZooKeepers.get("length") < maxNumZooKeepers) {
      console.log('currentZookeeper length less than maximum. Its: ' + currentZooKeepers.get("length"))
      currentZooKeepers.set("lastObject.showAddControl", false);
      if (currentZooKeepers.get("length") >= 1) {
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
      this.set('zId', parseInt(this.get('zId')) + 1);

      this.get("selectedServicesMasters").pushObject(newZookeeper);

      this.rebalanceZookeeperHosts();

      return true;
    }
    return false;//if no more zookeepers can be added
  },

  removeZookeepers: function (zId) {
    var currentZooKeepers;

    //work only if the Zookeeper service is selected in previous step
    if (!this.get("selectedServices").mapProperty("service_name").contains("ZOOKEEPER")) {
      return false;
    }

    currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper");

    if (currentZooKeepers.get("length") > 1) {
      this.get("selectedServicesMasters").removeAt(this.get("selectedServicesMasters").indexOf(this.get("selectedServicesMasters").findProperty("zId", zId)));

      currentZooKeepers = this.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper");
      if (currentZooKeepers.get("length") < this.get("hosts.length")) {
        currentZooKeepers.set("lastObject.showAddControl", true);
      }

      if (currentZooKeepers.get("length") === 1) {
        currentZooKeepers.set("lastObject.showRemoveControl", false);
      }
      this.set('zId', parseInt(this.get('zId')) - 1);
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
    if (a.memory < b.memory) {
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

  saveComponentHostsToDb: function () {
    var obj = this.get('selectedServicesMasters');
    var masterComponentHosts = [];
    var inc = 0;
    var array = [];
    obj.forEach(function (_component) {
      var hostArr = [];
      masterComponentHosts.push({
        component: _component.component_name,
        hostName: _component.selectedHost
      });
    });

    App.db.setMasterComponentHosts(masterComponentHosts);
    this.saveHostToMasterComponents();

  },

  saveHostToMasterComponents: function () {
    var hostMasterComponents = App.db.getMasterComponentHosts();
    var hosts = hostMasterComponents.mapProperty('hostName').uniq();
    var hostsMasterServicesMapping = [];
    hosts.forEach(function (_host) {
      var componentsOnHost = hostMasterComponents.filterProperty('hostName', _host).mapProperty('component');
      hostsMasterServicesMapping.push({
        hostname: _host,
        components: componentsOnHost
      });
    }, this);
    App.db.setHostToMasterComponent(hostsMasterServicesMapping);
    App.db.getHostToMasterComponent().forEach(function (_hostcomponent) {
      console.log("INFO: the name of this thimg is: " + _hostcomponent.hostname);
      console.log("INFO: the name of this thimg is: " + _hostcomponent.components);
    }, this);
  },

  submit: function () {
    this.saveComponentHostsToDb();
    App.router.send('next');
  }

});



