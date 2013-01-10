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

App.WizardStep5Controller = Em.Controller.extend({

  name:"wizardStep5Controller",

  hosts:[],

  selectedServices:[],
  selectedServicesMasters:[],
  zId:0,

  hasHiveServer: function () {
    return this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_SERVER');
  }.property('selectedServicesMasters'),

  updateHiveCoHosts: function () {
    var hiveServer =  this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_SERVER');
    var hiveMetastore = this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_METASTORE');
    var webHCatServer = this.get('selectedServicesMasters').findProperty('component_name', 'WEBHCAT_SERVER');
    if (hiveServer && hiveMetastore && webHCatServer) {
      this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_METASTORE').set('selectedHost', hiveServer.get('selectedHost'));
      this.get('selectedServicesMasters').findProperty('component_name', 'WEBHCAT_SERVER').set('selectedHost', hiveServer.get('selectedHost'));
    }
  }.observes('selectedServicesMasters.@each.selectedHost'),

  components:require('data/service_components'),

  clearStep:function () {
    this.set('hosts', []);
    this.set('selectedServices', []);
    this.set('selectedServicesMasters', []);
    this.set('zId', 0);
  },

  loadStep:function () {
    console.log("WizardStep5Controller: Loading step5: Assign Masters");
    this.clearStep();
    this.renderHostInfo();
    this.renderComponents(this.loadComponents());

    if (!this.get("selectedServicesMasters").filterProperty('isInstalled', false).length) {
      console.log('no master components to add');
      App.router.send('next');
    }
  },

  /**
   * Load active host list to <code>hosts</code> variable
   */
  renderHostInfo:function () {

    var hostInfo = this.get('content.hosts');

    for (var index in hostInfo) {
      var _host = hostInfo[index];
      if (_host.bootStatus === 'REGISTERED') {
        var hostObj = Ember.Object.create({
          host_name:_host.name,

          cpu:_host.cpu,
          memory:_host.memory,
          disk_info:_host.disk_info,
          host_info:"%@ (%@, %@ cores)".fmt(_host.name, (_host.memory * 1024).bytesToSize(1, 'parseFloat'), _host.cpu)

//          Uncomment to test sorting with random cpu, memory, host_info
//          cpu:function () {
//            return parseInt(2 + Math.random() * 4);
//          }.property(),
//          memory:function () {
//            return parseInt((Math.random() * 4000000000) + 4000000000);
//          }.property(),
//
//          host_info:function () {
//            return "%@ (%@, %@ cores)".fmt(this.get('host_name'), (this.get('memory') * 1024).bytesToSize(1, 'parseFloat'), this.get('cpu'));
//          }.property('cpu', 'memory')

        });

        this.get("hosts").pushObject(hostObj);
      }
    }
  },

  /**
   * Load services info to appropriate variable and return masterComponentHosts
   * @return {Ember.Set}
   */
  loadComponents:function () {

    var services = this.get('content.services')
      .filterProperty('isSelected', true).mapProperty('serviceName'); //list of shown services

    services.forEach(function (item) {
      this.get("selectedServices").pushObject(Ember.Object.create({service_name:item}));
    }, this);

    var masterHosts = this.get('content.masterComponentHosts'); //saved to local storadge info

    var resultComponents = new Ember.Set();

    var masterComponents = this.get('components').filterProperty('isMaster', true); //get full list from mock data

    var servicesLength = services.length;
    for (var index = 0; index < servicesLength; index++) {
      var componentInfo = masterComponents.filterProperty('service_name', services[index]);

      componentInfo.forEach(function (_componentInfo) {
        if (_componentInfo.component_name == 'ZOOKEEPER_SERVER') {
          var savedComponents = masterHosts.filterProperty('component', _componentInfo.component_name);
          if (savedComponents.length) {

            savedComponents.forEach(function (item) {
              var zooKeeperHost = {};
              zooKeeperHost.display_name = _componentInfo.display_name;
              zooKeeperHost.component_name = _componentInfo.component_name;
              zooKeeperHost.selectedHost = item.hostName;
              zooKeeperHost.availableHosts = [];
              zooKeeperHost.serviceId = services[index];
              zooKeeperHost.isInstalled = item.isInstalled;
              resultComponents.add(zooKeeperHost);
            })

          } else {

            var zooHosts = this.selectHost(_componentInfo.component_name);
            zooHosts.forEach(function (_host) {
              var zooKeeperHost = {};
              zooKeeperHost.display_name = _componentInfo.display_name;
              zooKeeperHost.component_name = _componentInfo.component_name;
              zooKeeperHost.selectedHost = _host;
              zooKeeperHost.availableHosts = [];
              zooKeeperHost.serviceId = services[index];
              zooKeeperHost.isInstalled = false;
              zooKeeperHost.isHiveCoHost = false;
              resultComponents.add(zooKeeperHost);
            });

          }
        } else {
          var savedComponent = masterHosts.findProperty('component', _componentInfo.component_name);
          var componentObj = {};
          componentObj.component_name = _componentInfo.component_name;
          componentObj.display_name = _componentInfo.display_name;
          componentObj.selectedHost = savedComponent ? savedComponent.hostName : this.selectHost(_componentInfo.component_name);   // call the method that plays selectNode algorithm or fetches from server
          componentObj.isInstalled = savedComponent ? savedComponent.isInstalled : App.Component.find().someProperty('componentName', _componentInfo.component_name);
          componentObj.serviceId = services[index];
          componentObj.availableHosts = [];
          componentObj.isHiveCoHost = ['HIVE_METASTORE', 'WEBHCAT_SERVER'].contains(_componentInfo.component_name);
          resultComponents.add(componentObj);
        }
      }, this);
    }

    return resultComponents;
  },

  /**
   * Put master components to <code>selectedServicesMasters</code>, which will be automatically rendered in template
   * @param masterComponents
   */
  renderComponents:function (masterComponents) {
    var zookeeperComponent = null, componentObj = null;
    var services = this.get('selectedServicesMasters').slice(0);
    if (services.length) {
      this.set('selectedServicesMasters', []);
    }

    var countZookeeper = masterComponents.filterProperty('display_name', 'ZooKeeper').length;

    masterComponents.forEach(function (item) {
      //add the zookeeper component at the end if exists
      console.log("TRACE: render master component name is: " + item.component_name);
      if (item.display_name === "ZooKeeper") {
        if (services.length) {
          services.forEach(function (_service) {
            this.get('selectedServicesMasters').pushObject(_service);
          }, this);
        }
        this.set('zId', parseInt(this.get('zId')) + 1);
        zookeeperComponent = Ember.Object.create(item);
        zookeeperComponent.set('zId', this.get('zId'));
        zookeeperComponent.set("showRemoveControl", countZookeeper > 1);
        zookeeperComponent.set("availableHosts", this.get("hosts").slice(0));
        this.get("selectedServicesMasters").pushObject(zookeeperComponent);

      } else {
        componentObj = Ember.Object.create(item);
        componentObj.set("availableHosts", this.get("hosts").slice(0));
        this.get("selectedServicesMasters").pushObject(componentObj);
      }
    }, this);

  },

  getKerberosServer:function (noOfHosts) {
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

  getNameNode:function (noOfHosts) {
    var hosts = this.get('hosts');
    return hosts[0];
  },

  getSNameNode:function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts === 1) {
      return hosts[0];
    } else {
      return hosts[1];
    }
  },

  getJobTracker:function (noOfHosts) {
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

  getHBaseMaster:function (noOfHosts) {
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

  getOozieServer:function (noOfHosts) {
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

  getOozieServer:function (noOfHosts) {
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

  getHiveServer:function (noOfHosts) {
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

  getHiveMetastore:function (noOfHosts) {
    return this.getHiveServer(noOfHosts);
  },

  getWebHCatServer:function (noOfHosts) {
    return this.getHiveServer(noOfHosts);
  },

  getZooKeeperServer:function (noOfHosts) {
    var hosts = this.get('hosts');
    if (noOfHosts < 3) {
      return [hosts[0].host_name];
    } else {
      return [hosts[0].host_name, hosts[1].host_name, hosts[2].host_name];
    }
  },

  getGangliaServer:function (noOfHosts) {
    var hosts = this.get('hosts');
    var hostnames = [];
    var inc = 0;
    hosts.forEach(function (_hostname) {
      hostnames[inc] = _hostname.host_name;
      inc++;
    });
    var hostExcAmbari = hostnames.without(location.hostname);
    if (noOfHosts > 1) {
      return hostExcAmbari[0];
    } else {
      return hostnames[0];
    }
  },

  getNagiosServer:function (noOfHosts) {
    var hosts = this.get('hosts');
    var hostnames = [];
    var inc = 0;
    hosts.forEach(function (_hostname) {
      hostnames[inc] = _hostname.host_name;
      inc++;
    });
    var hostExcAmbari = hostnames.without(location.hostname);
    if (noOfHosts > 1) {
      return hostExcAmbari[0];
    } else {
      return hostnames[0];
    }
  },


  /**
   * Return hostName of masterNode for specified service
   * @param componentName
   * @return {*}
   */
  selectHost:function (componentName) {
    var noOfHosts = this.get('hosts').length;
    switch (componentName) {
      case 'KERBEROS_SERVER':
        return this.getKerberosServer(noOfHosts).host_name;
      case 'NAMENODE':
        return this.getNameNode(noOfHosts).host_name;
      case 'SECONDARY_NAMENODE':
        return this.getSNameNode(noOfHosts).host_name;
      case 'JOBTRACKER':
        return this.getJobTracker(noOfHosts).host_name;
      case 'HBASE_MASTER':
        return this.getHBaseMaster(noOfHosts).host_name;
      case 'OOZIE_SERVER':
        return this.getOozieServer(noOfHosts).host_name;
      case 'HIVE_SERVER':
        return this.getHiveServer(noOfHosts).host_name;
      case 'HIVE_METASTORE':
        return this.getHiveMetastore(noOfHosts).host_name;
      case 'WEBHCAT_SERVER':
        return this.getWebHCatServer(noOfHosts).host_name;
      case 'ZOOKEEPER_SERVER':
        return this.getZooKeeperServer(noOfHosts);
      case 'GANGLIA_SERVER':
        return this.getGangliaServer(noOfHosts);
      case 'NAGIOS_SERVER':
        return this.getNagiosServer(noOfHosts);
    }
  },

  masterHostMapping:function () {
    var mapping = [], mappingObject, self = this, mappedHosts, hostObj, hostInfo;
    //get the unique assigned hosts and find the master services assigned to them

    mappedHosts = this.get("selectedServicesMasters").mapProperty("selectedHost").uniq();

    mappedHosts.forEach(function (item) {
      hostObj = self.get("hosts").findProperty("host_name", item);
      console.log("Name of the host is: " + hostObj.host_name);

      mappingObject = Ember.Object.create({
        host_name:item,
        hostInfo:hostObj.host_info,
        masterServices:self.get("selectedServicesMasters").filterProperty("selectedHost", item)
      });

      mapping.pushObject(mappingObject);
    }, this);

    mapping.sort(this.sortHostsByName);

    return mapping;

  }.property("selectedServicesMasters.@each.selectedHost"),

  remainingHosts:function () {
    return (this.get("hosts.length") - this.get("masterHostMapping.length"));
  }.property("selectedServicesMasters.@each.selectedHost"),

  hasZookeeper:function () {
    return this.selectedServices.findProperty("service_name", "ZooKeeper");
  }.property("selectedServices"),

  //methods
  getAvailableHosts:function (componentName) {
    var assignableHosts = [],
      zookeeperHosts = null;

    if (componentName === "ZooKeeper") {
      zookeeperHosts = this.get("selectedServicesMasters").filterProperty("display_name", "ZooKeeper").mapProperty("selectedHost").uniq();
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

  assignHostToMaster:function (masterService, selectedHost, zId) {
    if (selectedHost && masterService) {
      if ((masterService === "ZooKeeper") && zId) {
        this.get('selectedServicesMasters').findProperty("zId", zId).set("selectedHost", selectedHost);
        this.rebalanceZookeeperHosts();
      }
      else {
        this.get('selectedServicesMasters').findProperty("display_name", masterService).set("selectedHost", selectedHost);
      }

    }
  },

  lastZooKeeper:function () {
    var currentZooKeepers = this.get("selectedServicesMasters").filterProperty("display_name", "ZooKeeper");
    if (currentZooKeepers) {
      return currentZooKeepers.get("lastObject");
    }

    return null;
  },

  addZookeepers:function () {
    /*
     *Logic: If ZooKeeper service is selected then there can be
     * minimum 1 ZooKeeper master in total, and
     * maximum 1 ZooKeeper on every host
     */

    var maxNumZooKeepers = this.get("hosts.length"),
      currentZooKeepers = this.get("selectedServicesMasters").filterProperty("display_name", "ZooKeeper"),
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
      newZookeeper.set("display_name", lastZoo.get("display_name"));
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

  removeZookeepers:function (zId) {
    var currentZooKeepers;

    //work only if the Zookeeper service is selected in previous step
    if (!this.get("selectedServices").mapProperty("service_name").contains("ZOOKEEPER")) {
      return false;
    }

    currentZooKeepers = this.get("selectedServicesMasters").filterProperty("display_name", "ZooKeeper");

    if (currentZooKeepers.get("length") > 1) {
      this.get("selectedServicesMasters").removeAt(this.get("selectedServicesMasters").indexOf(this.get("selectedServicesMasters").findProperty("zId", zId)));

      currentZooKeepers = this.get("selectedServicesMasters").filterProperty("display_name", "ZooKeeper");
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

  rebalanceZookeeperHosts:function () {
    //for a zookeeper update the available hosts for the other zookeepers

    var currentZooKeepers = this.get("selectedServicesMasters").filterProperty("display_name", "ZooKeeper"),
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

  sortHostsByConfig:function (a, b) {
    //currently handling only total memory on the host
    if (a.memory < b.memory) {
      return 1;
    }
    else {
      return -1;
    }
  },

  sortHostsByName:function (a, b) {
    if (a.host_name > b.host_name) {
      return 1;
    }
    else {
      return -1;
    }
  }
});



