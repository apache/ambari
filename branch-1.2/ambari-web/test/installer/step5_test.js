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

var Ember = require('ember');
var App = require('app');
require('controllers/wizard/step5_controller');

/*
describe('App.InstallerStep5Controller', function () {
  var controller = App.InstallerStep5Controller.create();
  controller.get("selectedServices").pushObject({service_name: 'ZOOKEEPER'});
  var cpu = 2, memory = 4;
  var HOST = ['host1', 'host2', 'host3', 'host4', 'host5'];
  var hosts = [];
  HOST.forEach(function (_host) {
    controller.get('hosts').pushObject(Ember.Object.create({
      host_name: _host,
      cpu: cpu,
      memory: memory
    }));
  });

  var componentObj = Ember.Object.create({
    component_name: 'ZooKeeper',
    selectedHost: 'host2', // call the method that plays selectNode algorithm or fetches from server
    availableHosts: []
  });
  componentObj.set('availableHosts', controller.get('hosts').slice(0));
  componentObj.set('zId', 1);
  componentObj.set("showAddControl", true);
  componentObj.set("showRemoveControl", false);
  controller.get("selectedServicesMasters").pushObject(componentObj);


  describe('#getAvailableHosts()', function () {
    it('should generate available hosts for a new zookeeper service', function () {
      var hostsForNewZookeepers = controller.getAvailableHosts("ZooKeeper"),
        ok = true, i = 0, masters = null;

      //test that the hosts found, do not have Zookeeper master assigned to them
      for (i = 0; i < hostsForNewZookeepers.get("length"); i++) {
        masters = controller.get("selectedServicesMasters").filterProperty(hostsForNewZookeepers[i].get("host_name"));
        if (masters.findProperty("component_name", "ZooKeeper")) {
          ok = false;
          break;
        }
      }

      expect(ok).to.equal(true);
    })

    it('should return all hosts for services other than ZooKeeper', function () {
      var hostsForNewZookeepers = controller.getAvailableHosts("");

      expect(hostsForNewZookeepers.get("length")).to.equal(controller.get("hosts.length"));
    })
  })

  describe('#assignHostToMaster()', function () {
    it('should assign the selected host to the non-ZooKeeper master service', function () {
      //test non-zookeeper master
      var SERVICE_MASTER = "NameNode",
        HOST = "host4", ZID, status;
      var nonZookeeperObj = Ember.Object.create({
        component_name: SERVICE_MASTER,
        selectedHost: HOST, // call the method that plays selectNode algorithm or fetches from server
        availableHosts: []
      });
      controller.get("selectedServicesMasters").pushObject(nonZookeeperObj);
      controller.assignHostToMaster(SERVICE_MASTER, HOST);
      expect(controller.get("selectedServicesMasters").findProperty("component_name", SERVICE_MASTER).get("selectedHost")).to.equal(HOST);
    })

    it('should assign the selected host to the ZooKeeper master service', function () {
      //test non-zookeeper master
      var SERVICE_MASTER = "ZooKeeper",
        HOST = "host4", ZID = 2;

      //test zookeeper master assignment with
      if (controller.addZookeepers()) {
        controller.assignHostToMaster(SERVICE_MASTER, HOST, ZID);
        expect(controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").findProperty("zId", ZID).get("selectedHost")).to.equal(HOST);
      }
    })
  })

  describe('#addZookeepers()', function () {

    it('should add a new ZooKeeper', function () {
      var newLength = 0;
      if (controller.get("selectedServices").mapProperty("service_name").contains("ZOOKEEPER")
        && controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("length") < controller.get("hosts.length")) {
        newLength = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("length");
        controller.addZookeepers();
        expect(controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("length")).to.equal(newLength + 1);
      }
    })

    it('should add ZooKeepers up to the number of hosts', function () {

      var currentZooKeepers = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").length,
        success = true;

      //add ZooKeepers as long as possible
      if (currentZooKeepers) {

        while (success) {
          success = controller.addZookeepers();
        }
        var services = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper");
        var length = services.length;
        expect(controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").length).to.equal(controller.get("hosts.length"));
      }
    })
  })

  describe('#removeZookeepers()', function () {
    it('should remove a ZooKeeper', function () {
      if (controller.get("selectedServices").mapProperty("service_name").contains("ZOOKEEPER")) {
        if (controller.addZookeepers()) {
          newLength = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("length");
          controller.removeZookeepers(2);
          expect(controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("length")).to.equal(newLength - 1);
        }
      }
    })

    it('should fail to remove a ZooKeeper if there is only 1', function () {
      var currentZooKeepers = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").length,
        success = true;
      //remove ZooKeepers as long as possible

      if (currentZooKeepers) {
        while (success) {
          success = controller.removeZookeepers(controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("lastObject.zId"));
        }
        expect(controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").get("length")).to.equal(1);
      }
    })

  })

  describe('#rebalanceZookeeperHosts()', function () {

    it('should rebalance hosts for ZooKeeper', function () {
      //assign a host to a zookeeper and then rebalance the available hosts for the other zookeepers
      var zookeepers = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper"),
        aZookeeper = null, aHost = null, i = 0, ok = true;

      if (zookeepers.get("length") > 1) {
        aZookeeper = controller.get("selectedServicesMasters").filterProperty("component_name", "ZooKeeper").findProperty("zId", 1);
        aHost = aZookeeper.get("availableHosts")[0];
        aZookeeper.set("selectedHost", aHost.get("host_name"));

        controller.rebalanceZookeeperHosts();

        for (i = 0; i < zookeepers.get("length"); i++) {
          if (zookeepers[i].get("availableHosts").mapProperty("host_name").contains(aHost)) {
            ok = false;
            break;
          }
        }

        expect(ok).to.equal(true);
      }
    })
  })

})*/
