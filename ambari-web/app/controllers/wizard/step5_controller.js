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
var numberUtils = require('utils/number_utils');

App.WizardStep5Controller = Em.Controller.extend({

  name:"wizardStep5Controller",
  title: function () {
    if (this.get('content.controllerName') == 'reassignMasterController') {
      return Em.I18n.t('installer.step5.reassign.header');
    }
    return Em.I18n.t('installer.step5.header');
  }.property('content.controllerName'),

  isReassignWizard: function () {
    return this.get('content.controllerName') == 'reassignMasterController';
  }.property('content.controllerName'),

  isAddServiceWizard: function() {
    return this.get('content.controllerName') == 'addServiceController';
  }.property('content.controllerName'),

  isReassignHive: function () {
    return this.get('servicesMasters').objectAt(0) && this.get('servicesMasters').objectAt(0).component_name == 'HIVE_SERVER' && this.get('isReassignWizard');
  }.property('isReassignWizard', 'servicesMasters'),
  /**
   * master components which could be assigned to multiple hosts
   */
  multipleComponents: ['ZOOKEEPER_SERVER', 'HBASE_MASTER'],
  /**
   * Define state for submit button. Return true only for Reassign Master Wizard and if more than one master component was reassigned.
   */
  isSubmitDisabled: function () {
    if (!this.get('isReassignWizard')) {
      return false;
    }
    var reassigned = 0;
    var arr1 = App.HostComponent.find().filterProperty('componentName', this.get('content.reassign.component_name')).mapProperty('host.hostName');
    var arr2 = this.get('servicesMasters').mapProperty('selectedHost');
    arr1.forEach(function (host) {
      if (!arr2.contains(host)) {
        reassigned++;
      }
    }, this);
    return reassigned !== 1;
  }.property('servicesMasters.@each.selectedHost'),

  hosts:[],

  componentToRebalance: null,
  rebalanceComponentHostsCounter: 0,

  servicesMasters:[],
  selectedServicesMasters:[],

  components:require('data/service_components'),

  clearStep:function () {
    this.set('hosts', []);
    this.set('selectedServicesMasters', []);
    this.set('servicesMasters', []);
  },

  loadStep:function () {
    console.log("WizardStep5Controller: Loading step5: Assign Masters");
    this.clearStep();
    this.renderHostInfo();
    this.renderComponents(this.loadComponents());

    this.updateComponent('ZOOKEEPER_SERVER');
    if(App.supports.multipleHBaseMasters){
      this.updateComponent('HBASE_MASTER');
    }

    if (!this.get("selectedServicesMasters").filterProperty('isInstalled', false).length) {
      console.log('no master components to add');
      App.router.send('next');
    }
  },

  /**
   * Used to set showAddControl flag for ZOOKEEPER_SERVER and HBASE_SERVER
   */
  updateComponent: function(componentName){
    var component = this.last(componentName);

    var services = this.get('content.services').filterProperty('isInstalled', true).mapProperty('serviceName');
    var currentService = componentName.split('_')[0];
    var showControl = !services.contains(currentService);

    if (component) {
      if(showControl){
        if (this.get("selectedServicesMasters").filterProperty("component_name", componentName).length < this.get("hosts.length") && !this.get('isReassignWizard')) {
          component.set('showAddControl', true);
        } else {
          component.set('showRemoveControl', false);
        }
      }
    }
  },

  /**
   * Load active host list to <code>hosts</code> variable
   */
  renderHostInfo:function () {

    var hostInfo = this.get('content.hosts');
    var result = [];

    for (var index in hostInfo) {
      var _host = hostInfo[index];
      if (_host.bootStatus === 'REGISTERED') {
        result.push(Ember.Object.create({
          host_name:_host.name,

          cpu:_host.cpu,
          memory:_host.memory,
          disk_info:_host.disk_info,
          host_info: Em.I18n.t('installer.step5.hostInfo').fmt(_host.name, numberUtils.bytesToSize(_host.memory, 1, 'parseFloat', 1024), _host.cpu)
        }));
      }
    }
    this.set("hosts", result);
    this.sortHosts(this.get('hosts'));
  },

  sortHosts: function (hosts) {
    hosts.sort(function (a, b) {
      if (a.get('memory') == b.get('memory')) {
        if (a.get('cpu') == b.get('cpu')) {
          return a.get('host_name').localeCompare(b.get('host_name')); // hostname asc
        }
        return b.get('cpu') - a.get('cpu'); // cores desc
      }
      return b.get('memory') - a.get('memory'); // ram desc
    });
  },

  /**
   * Load services info to appropriate variable and return masterComponentHosts
   * @return Array
   */
  loadComponents:function () {

    var services = this.get('content.services')
      .filterProperty('isSelected', true).mapProperty('serviceName'); //list of shown services

    var masterComponents = this.get('components').filterProperty('isMaster', true); //get full list from mock data
    var masterHosts = this.get('content.masterComponentHosts'); //saved to local storage info

    var resultComponents = [];

    var servicesLength = services.length;
    for (var index = 0; index < servicesLength; index++) {
      var componentInfo = masterComponents.filterProperty('service_name', services[index]);

      componentInfo.forEach(function (_componentInfo) {
        if (_componentInfo.component_name == 'ZOOKEEPER_SERVER' || _componentInfo.component_name == 'HBASE_MASTER') {
          var savedComponents = masterHosts.filterProperty('component', _componentInfo.component_name);
          if (savedComponents.length) {

            savedComponents.forEach(function (item) {
              var zooKeeperHost = {};
              zooKeeperHost.display_name = _componentInfo.display_name;
              zooKeeperHost.component_name = _componentInfo.component_name;
              zooKeeperHost.selectedHost = item.hostName;
              zooKeeperHost.serviceId = services[index];
              zooKeeperHost.isInstalled = item.isInstalled;
              zooKeeperHost.isHiveCoHost = false;
              resultComponents.push(zooKeeperHost);
            })

          } else {

            var zooHosts = this.selectHost(_componentInfo.component_name);
            zooHosts.forEach(function (_host) {
              var zooKeeperHost = {};
              zooKeeperHost.display_name = _componentInfo.display_name;
              zooKeeperHost.component_name = _componentInfo.component_name;
              zooKeeperHost.selectedHost = _host;
              zooKeeperHost.serviceId = services[index];
              zooKeeperHost.isInstalled = false;
              zooKeeperHost.isHiveCoHost = false;
              resultComponents.push(zooKeeperHost);
            });

          }
        } else {
          var savedComponent = masterHosts.findProperty('component', _componentInfo.component_name);
          var componentObj = {};
          componentObj.component_name = _componentInfo.component_name;
          componentObj.display_name = _componentInfo.display_name;
          componentObj.selectedHost = savedComponent ? savedComponent.hostName : this.selectHost(_componentInfo.component_name);   // call the method that plays selectNode algorithm or fetches from server
          componentObj.isInstalled = savedComponent ? savedComponent.isInstalled : false;
          componentObj.serviceId = services[index];
          componentObj.isHiveCoHost = ['HIVE_METASTORE', 'WEBHCAT_SERVER'].contains(_componentInfo.component_name) && !this.get('isReassignWizard');
          resultComponents.push(componentObj);
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
    var self = this;
    var services = this.get('content.services')
      .filterProperty('isInstalled', true).mapProperty('serviceName'); //list of shown services
    var showRemoveControlZk = !services.contains('ZOOKEEPER') && masterComponents.filterProperty('display_name', 'ZooKeeper').length > 1;
    var showRemoveControlHb = !services.contains('HBASE') && masterComponents.filterProperty('component_name', 'HBASE_MASTER').length > 1;
    var zid = 1;
    var hid = 1;
    var nid = 1;
    var result = [];

    masterComponents.forEach(function (item) {

      if (item.component_name == 'SECONDARY_NAMENODE') {
        if (self.get('isAddServiceWizard')) {
          if (App.get('isHaEnabled')) {
            return;
          }
        }
      }

      var componentObj = Ember.Object.create(item);
      console.log("TRACE: render master component name is: " + item.component_name);

      if (item.display_name === "ZooKeeper") {
        componentObj.set('zId', zid++);
        componentObj.set("showRemoveControl", showRemoveControlZk);
      } else if (App.supports.multipleHBaseMasters && item.component_name === "HBASE_MASTER") {
        componentObj.set('zId', hid++);
        componentObj.set("showRemoveControl", showRemoveControlHb);
      }  else if (item.component_name === "NAMENODE") {
        componentObj.set('zId', nid++);
      }
      result.push(componentObj);
    }, this);

    this.set("selectedServicesMasters", result);
    if (this.get('isReassignWizard')) {
      var components = result.filterProperty('component_name', this.get('content.reassign.component_name'));
      components.setEach('isInstalled', false);
      this.set('servicesMasters', components);
    } else {
      this.set('servicesMasters', result);
    }
  },

  hasHiveServer: function () {
    return !!this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_SERVER') && !this.get('isReassignWizard');
  }.property('selectedServicesMasters'),

  updateHiveCoHosts: function () {
    var hiveServer =  this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_SERVER');
    var hiveMetastore = this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_METASTORE');
    var webHCatServer = this.get('selectedServicesMasters').findProperty('component_name', 'WEBHCAT_SERVER');
    if (hiveServer && hiveMetastore && webHCatServer) {
      if (!this.get('isReassignHive') && this.get('servicesMasters').objectAt(0) && !(this.get('servicesMasters').objectAt(0).component_name == 'HIVE_METASTORE')) {
        this.get('selectedServicesMasters').findProperty('component_name', 'HIVE_METASTORE').set('selectedHost', hiveServer.get('selectedHost'))
      }
      this.get('selectedServicesMasters').findProperty('component_name', 'WEBHCAT_SERVER').set('selectedHost', hiveServer.get('selectedHost'));
    }
  }.observes('selectedServicesMasters.@each.selectedHost'),

  /**
   * select and return host for component by scheme
   * Scheme is an object that has keys which compared to number of hosts,
   * if key more that number of hosts, then return value of that key.
   * Value is index of host in hosts array.
   *
   * @param noOfHosts
   * @param selectionScheme
   * @return {*}
   */
  getHostForComponent: function(noOfHosts, selectionScheme){
    var hosts = this.get('hosts');
    if(hosts.length === 1 || $.isEmptyObject(selectionScheme)){
      return hosts[0];
    } else {
      for(var i in selectionScheme){
        if(window.isFinite(i)){
          if(noOfHosts < window.parseInt(i)){
            return hosts[selectionScheme[i]];
          }
        }
      }
      return hosts[selectionScheme['else']]
    }
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
    var hostNames = this.get('hosts').mapProperty('host_name');
    var hostExcAmbari = hostNames.without(location.hostname);
    if (noOfHosts > 1) {
      return hostExcAmbari[0];
    } else {
      return hostNames[0];
    }
  },

  getNagiosServer:function (noOfHosts) {
    return this.getGangliaServer(noOfHosts);
  },

  getHueServer:function (noOfHosts) {
    return this.getGangliaServer(noOfHosts);
  },

  getOozieServer:function(){
    return this.selectHost('OOZIE_SERVER');
  },

  getNimbusServer: function(noOfHosts) {
    return this.getGangliaServer(noOfHosts);
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
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 3,
          "else" : 5
        }).host_name;
      case 'NAMENODE':
        return this.getHostForComponent(noOfHosts, {
          "else" : 0
        }).host_name;
      case 'SECONDARY_NAMENODE':
        return this.getHostForComponent(noOfHosts, {
          "else" : 1
        }).host_name;
      case 'JOBTRACKER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 1,
          "else" : 2
        }).host_name;
      case 'HISTORYSERVER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 1,
          "else" : 2
        }).host_name;
      case 'RESOURCEMANAGER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 1,
          "else" : 2
        }).host_name;
      case 'HBASE_MASTER':
        return [this.getHostForComponent(noOfHosts, {
          "3" : 0,
          "6" : 0,
          "31" : 2,
          "else" : 3
        }).host_name];
      case 'OOZIE_SERVER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 2,
          "else" : 3
        }).host_name;
      case 'HIVE_SERVER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 2,
          "else" : 4
        }).host_name;
      case 'HIVE_METASTORE':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 2,
          "else" : 4
        }).host_name;
      case 'WEBHCAT_SERVER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 2,
          "else" : 4
        }).host_name;
      case 'ZOOKEEPER_SERVER':
        return this.getZooKeeperServer(noOfHosts);
      case 'GANGLIA_SERVER':
        return this.getGangliaServer(noOfHosts);
      case 'NAGIOS_SERVER':
        return this.getNagiosServer(noOfHosts);
      case 'HUE_SERVER':
        return this.getHueServer(noOfHosts);
      case 'APP_TIMELINE_SERVER':
        return this.getHostForComponent(noOfHosts, {
          "3" : 1,
          "6" : 1,
          "31" : 1,
          "else" : 2
        }).host_name;
      case 'FALCON_SERVER':
        return this.getOozieServer(noOfHosts);
      case 'STORM_UI_SERVER':
      case 'DRPC_SERVER':
      case 'STORM_REST_API':
      case 'NIMBUS':
        return this.getNimbusServer(noOfHosts);
      default:
    }
  },

  masterHostMapping: function () {
    var mapping = [], mappingObject, mappedHosts, hostObj;
    //get the unique assigned hosts and find the master services assigned to them
    mappedHosts = this.get("selectedServicesMasters").mapProperty("selectedHost").uniq();

    mappedHosts.forEach(function (item) {
      hostObj = this.get("hosts").findProperty("host_name", item);

      mappingObject = Ember.Object.create({
        host_name: item,
        hostInfo: hostObj.host_info,
        masterServices: this.get("selectedServicesMasters").filterProperty("selectedHost", item)
      });

      mapping.pushObject(mappingObject);
    }, this);

    return mapping.sortProperty('host_name');
  }.property("selectedServicesMasters.@each.selectedHost"),

  remainingHosts:function () {
    return (this.get("hosts.length") - this.get("masterHostMapping.length"));
  }.property("selectedServicesMasters.@each.selectedHost"),


  /**
   * On change callback for selects
   * @param componentName
   * @param selectedHost
   * @param zId
   */
  assignHostToMaster:function (componentName, selectedHost, zId) {
    if (selectedHost && componentName) {
      if (zId) {
        this.get('selectedServicesMasters').filterProperty('component_name', componentName).findProperty("zId", zId).set("selectedHost", selectedHost);
      } else {
        this.get('selectedServicesMasters').findProperty("component_name", componentName).set("selectedHost", selectedHost);
      }
    }
  },

  /**
   * Returns last component of selected type
   * @param componentName
   * @return {*}
   */
  last: function(componentName){
    return this.get("selectedServicesMasters").filterProperty("component_name", componentName).get("lastObject");
  },

  /**
   * Add new component to ZooKeeper Server and Hbase master
   * @param componentName
   * @return {Boolean}
   */
  addComponent:function (componentName) {
    /*
     * Logic: If ZooKeeper or Hbase service is selected then there can be
     * minimum 1 ZooKeeper or Hbase master in total, and
     * maximum 1 ZooKeeper or Hbase on every host
     */

    var maxNumMasters = this.get("hosts.length"),
      currentMasters = this.get("selectedServicesMasters").filterProperty("component_name", componentName),
      newMaster = null,
      masterHosts = null,
      suggestedHost = null,
      i = 0,
      lastMaster = null;

    if (!currentMasters.length) {
      console.log('ALERT: Zookeeper service was not selected');
      return false;
    }

    if (currentMasters.get("length") < maxNumMasters) {

      currentMasters.set("lastObject.showAddControl", false);
      currentMasters.set("lastObject.showRemoveControl", true);

      //create a new zookeeper based on an existing one
      newMaster = Ember.Object.create({});
      lastMaster = currentMasters.get("lastObject");
      newMaster.set("display_name", lastMaster.get("display_name"));
      newMaster.set("component_name", lastMaster.get("component_name"));
      newMaster.set("selectedHost", lastMaster.get("selectedHost"));
      newMaster.set("isInstalled", false);

      if (currentMasters.get("length") === (maxNumMasters - 1)) {
        newMaster.set("showAddControl", false);
      } else {
        newMaster.set("showAddControl", true);
      }
      newMaster.set("showRemoveControl", true);

      //get recommended host for the new Zookeeper server
      masterHosts = currentMasters.mapProperty("selectedHost").uniq();

      for (i = 0; i < this.get("hosts.length"); i++) {
        if (!(masterHosts.contains(this.get("hosts")[i].get("host_name")))) {
          suggestedHost = this.get("hosts")[i].get("host_name");
          break;
        }
      }

      newMaster.set("selectedHost", suggestedHost);
      newMaster.set("zId", (currentMasters.get("lastObject.zId") + 1));

      this.get("selectedServicesMasters").insertAt(this.get("selectedServicesMasters").indexOf(lastMaster) + 1, newMaster);

      this.set('componentToRebalance', componentName);
      this.incrementProperty('rebalanceComponentHostsCounter');

      return true;
    }
    return false;//if no more zookeepers can be added
  },

  /**
   * Remove component from ZooKeeper server or Hbase Master
   * @param componentName
   * @param zId
   * @return {Boolean}
   */
  removeComponent:function (componentName, zId) {
    var currentMasters = this.get("selectedServicesMasters").filterProperty("component_name", componentName);

    //work only if the Zookeeper service is selected in previous step
    if (!currentMasters.length) {
      return false;
    }

    if (currentMasters.get("length") > 1) {
      this.get("selectedServicesMasters").removeAt(this.get("selectedServicesMasters").indexOf(currentMasters.findProperty("zId", zId)));

      currentMasters = this.get("selectedServicesMasters").filterProperty("component_name", componentName);
      if (currentMasters.get("length") < this.get("hosts.length")) {
        currentMasters.set("lastObject.showAddControl", true);
      }

      if (currentMasters.get("length") === 1) {
        currentMasters.set("lastObject.showRemoveControl", false);
      }

      this.set('componentToRebalance', componentName);
      this.incrementProperty('rebalanceComponentHostsCounter');

      return true;
    }

    return false;

  },

  submit: function () {
    if (!this.get('isSubmitDisabled')){
      App.router.send('next');
    }
  }
});



