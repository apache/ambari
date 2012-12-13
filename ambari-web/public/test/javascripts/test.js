(function(/*! Brunch !*/) {
  'use strict';

  var globals = typeof window !== 'undefined' ? window : global;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};

  var has = function(object, name) {
    return ({}).hasOwnProperty.call(object, name);
  };

  var expand = function(root, name) {
    var results = [], parts, part;
    if (/^\.\.?(\/|$)/.test(name)) {
      parts = [root, name].join('/').split('/');
    } else {
      parts = name.split('/');
    }
    for (var i = 0, length = parts.length; i < length; i++) {
      part = parts[i];
      if (part === '..') {
        results.pop();
      } else if (part !== '.' && part !== '') {
        results.push(part);
      }
    }
    return results.join('/');
  };

  var dirname = function(path) {
    return path.split('/').slice(0, -1).join('/');
  };

  var localRequire = function(path) {
    return function(name) {
      var dir = dirname(path);
      var absolute = expand(dir, name);
      return globals.require(absolute);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    definition(module.exports, localRequire(name), module);
    var exports = cache[name] = module.exports;
    return exports;
  };

  var require = function(name) {
    var path = expand(name, '.');

    if (has(cache, path)) return cache[path];
    if (has(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has(cache, dirIndex)) return cache[dirIndex];
    if (has(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '"');
  };

  var define = function(bundle) {
    for (var key in bundle) {
      if (has(bundle, key)) {
        modules[key] = bundle[key];
      }
    }
  }

  globals.require = require;
  globals.require.define = define;
  globals.require.brunch = true;
})();

window.require.define({"test/installer/step1_test": function(exports, require, module) {
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
  require('controllers/wizard/step1_controller');

  /*
  describe('App.InstallerStep1Controller', function () {

    describe('#validateStep1()', function () {
      it('should return false and sets invalidClusterName to true if cluster name is empty', function () {
        var controller = App.InstallerStep1Controller.create();
        controller.set('clusterName', '');
        expect(controller.validateStep1()).to.equal(false);
        expect(controller.get('invalidClusterName')).to.equal(true);
      })
      it('should return false and sets invalidClusterName to true if cluster name has whitespaces', function () {
        var controller = App.InstallerStep1Controller.create();
        controller.set('clusterName', 'My Cluster');
        expect(controller.validateStep1()).to.equal(false);
        expect(controller.get('invalidClusterName')).to.equal(true);
      })
      it('should return false and sets invalidClusterName to true if cluster name has special characters', function () {
        var controller = App.InstallerStep1Controller.create();
        controller.set('clusterName', 'my-cluster');
        expect(controller.validateStep1()).to.equal(false);
        expect(controller.get('invalidClusterName')).to.equal(true);
      })
      it('should return true, sets invalidClusterName to false if cluster name is valid', function () {
        var controller = App.InstallerStep1Controller.create();
        var clusterName = 'mycluster1';
        controller.set('clusterName', clusterName);
        expect(controller.validateStep1()).to.equal(true);
        expect(controller.get('invalidClusterName')).to.equal(false);
      })
    })

  })*/
  
}});

window.require.define({"test/installer/step2_test": function(exports, require, module) {
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
  var Ember = require('ember');
  require('controllers/wizard/step2_controller');

  describe.skip('App.WizardStep2Controller', function () {

    /*describe('#hostsError()', function () {

      it('should return t(installer.step2.hostName.error.required) if manualInstall is false, hostNames is empty, and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', false);
        controller.set('hostNames', '');
        controller.set('hasSubmitted', true);
        expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.required'));
      })

      it('should return null if manualInstall is false, hostNames is not empty, and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', false);
        controller.set('hostNames', 'ambari');
        controller.set('hasSubmitted', true);
        expect(controller.get('hostsError')).to.equal(null);
      })

      it('should return t(installer.step2.hostName.error.invalid) if manualInstall is false and hostNames has an element ' +
        'that starts with a hyphen', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', false);
        controller.set('hostNames', "-apache");
        expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.invalid'));
      })

      it('should return t(installer.step2.hostName.error.invalid) if manualInstall is false and hostNames has an element ' +
        'that ends with a hyphen', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', false);
        controller.set('hostNames', 'apache-');
        expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.invalid'));
      })

      it('should return t(installer.step2.hostName.error.required) if manualInstall is true, hostNames is empty, and ' +
        'hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', true);
        controller.set('hostNames', '');
        controller.set('hasSubmitted', true);
        expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.required'));
      })

    })

    describe('#sshKeyError()', function () {
      it('should return t(installer.step2.sshKey.error.required) to true if manualInstall is false, sshKey is empty, ' +
        'and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', false);
        controller.set('sshKey', '');
        controller.set('hasSubmitted', true);
        expect(controller.get('sshKeyError')).to.equal(Ember.I18n.t('installer.step2.sshKey.error.required'));
      })

      it('should return null if manualInstall is true, sshKey is empty, and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('sshKey', '');
        controller.set('manualInstall', true);
        controller.set('hasSubmitted', true);
        expect(controller.get('sshKeyError')).to.equal(null);
      })

      it('should return null if sshKey is not null and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('sshKey', 'ambari');
        controller.set('hasSubmitted', true);
        expect(controller.get('sshKeyError')).to.equal(null);
      })

    })*/
      /* Passphrase has been disabled, so commenting out tests
      it('should set passphraseMatchErr to true if ' +
        'passphrase and confirmPassphrase doesn\'t match ', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', false);
        controller.set('passphrase', 'apache ambari');
        controller.set('confirmPassphrase', 'ambari');
        controller.validateStep2();
        expect(controller.get('passphraseMatchErr')).to.equal(true);
      })

      it('should set passphraseMatchErr to false if passphrase and ' +
        'confirmPassphrase doesn\'t match but manualInstall is true ', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('passphrase', 'apache ambari');
        controller.set('confirmPassphrase', 'ambari');
        controller.set('manualInstall', true);
        controller.validateStep2();
        expect(controller.get('passphraseMatchErr')).to.equal(false);
      })

      it('should set passphraseMatchErr to true if passphrase and ' +
        'confirmPassphrase matches', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('passphrase', 'apache ambari');
        controller.set('confirmPassphrase', 'apache ambari');
        controller.validateStep2();
        expect(controller.get('passphraseMatchErr')).to.equal(false);
      })
      */

    /*describe('#localRepoError()', function() {

      it('should return t(installer.step2.localRepo.error.required) localRepo is true, localRepoPath is empty, and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('localRepo', true);
        controller.set('localRepoPath', '');
        controller.set('hasSubmitted', true);
        expect(controller.get('localRepoError')).to.equal(Ember.I18n.t('installer.step2.localRepo.error.required'));
      })

      it('should return null if localRepo is true, localRepoPath is not empty, and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('localRepo', true);
        controller.set('localRepoPath', '/etc/');
        controller.set('hasSubmitted', true);
        expect(controller.get('localRepoError')).to.equal(null);
      })

      it('should return null if localRepo is false, localRepoPath is empty, and hasSubmitted is true', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('localRepo', false);
        controller.set('localRepoPath', '');
        controller.set('hasSubmitted', true);
        expect(controller.get('localRepoError')).to.equal(null);
      })
    })

    describe('#evaluateStep2(): On hitting step2 \"next\" button', function () {
      it('should return false if isSubmitDisabled is true ', function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('isSubmitDisabled', true);
        expect(controller.evaluateStep2()).to.equal(false);
      })
    })*/

  })
  
}});

window.require.define({"test/installer/step3_test": function(exports, require, module) {
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
  require('models/hosts');
  require('controllers/wizard/step3_controller');

  /*
  describe('App.InstallerStep3Controller', function () {
    //var controller = App.InstallerStep3Controller.create();

    describe('#parseHostInfo', function () {
      var controller = App.InstallerStep3Controller.create();
      it('should return true if there is no host with pending status in the data provided by REST bootstrap call.  It should also update the status on the client side', function () {
        var hostFromServer = [
          {
            name: '192.168.1.1',
            status: 'error'
          },
          {
            name: '192.168.1.2',
            status: 'success'
          },
          {
            name: '192.168.1.3',
            status: 'error'
          },
          {
            name: '192.168.1.4',
            status: 'success'
          }
        ];
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.1',
          status: 'error'
        }));
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.2',
          status: 'success'
        }));
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.3',
          status: 'pending'        //status should be overriden to 'error' after the parseHostInfo call
        }));
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.4',
          status: 'success'
        }));

        var result = controller.parseHostInfo(hostFromServer, controller.content);
        var host = controller.content.findProperty('name', '192.168.1.3');
        expect(result).to.equal(true);
        expect(host.bootStatus).to.equal('error');
      })
    })


    describe('#onAllChecked', function () {
      var controller = App.InstallerStep3Controller.create();
      it('should set all visible hosts\'s isChecked to true upon checking the "all" checkbox', function () {
        controller.set('category', 'All Hosts');
        controller.set('allChecked', true);
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.1',
          status: 'error',
          isChecked: false
        }));
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.2',
          status: 'success',
          isChecked: false
        }));
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.3',
          status: 'pending', //status should be overriden to 'error' after the parseHostInfo call
          isChecked: true
        }));
        controller.content.pushObject(App.HostInfo.create({
          name: '192.168.1.4',
          status: 'success',
          isChecked: false
        }));
        controller.onAllChecked();
        controller.content.forEach(function (host) {
          var result = host.get('isChecked');
          expect(result).to.equal(true);
        });

      })
    })
  })

  */
  
}});

window.require.define({"test/installer/step4_test": function(exports, require, module) {
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
  require('controllers/wizard/step4_controller');

  /*
  describe('App.InstallerStep4Controller', function () {

    var DEFAULT_SERVICES = ['HDFS'];
    var OPTIONAL_SERVICES = ['MAPREDUCE', 'NAGIOS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SQOOP', 'ZOOKEEPER', 'HCATALOG'];

    var controller = App.InstallerStep4Controller.create();
    controller.rawContent.forEach(function(item){
      item.isSelected = true;
      controller.pushObject(Ember.Object.create(item));
      });

    describe('#selectMinimum()', function () {
      it('should set isSelected is false on all non-default services and isSelected is true on all default services', function() {
        controller.selectMinimum();
        DEFAULT_SERVICES.forEach(function (serviceName) {
          expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(true);
        });
        OPTIONAL_SERVICES.forEach(function (serviceName) {
          expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(false);
        });
      })
    })

    describe('#selectAll()', function () {
      it('should set isSelected is true on all non-default services and isSelected is true on all default services', function() {
        controller.selectAll();
        DEFAULT_SERVICES.forEach(function (serviceName) {
          expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(true);
        });
        OPTIONAL_SERVICES.forEach(function (serviceName) {
          expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(true);
        });
      })
    })

    describe('#isAll()', function () {

      beforeEach(function() {
        DEFAULT_SERVICES.forEach(function(serviceName) {
          controller.findProperty('serviceName', serviceName).set('isSelected', true);
        });
        OPTIONAL_SERVICES.forEach(function(serviceName) {
          controller.findProperty('serviceName', serviceName).set('isSelected', true);
        });
      });

      it('should return true if isSelected is true for all services', function() {
        expect(controller.get('isAll')).to.equal(true);
      })

      it('should return false if isSelected is false for one of the services', function() {
        controller.findProperty('serviceName', 'HBASE').set('isSelected', false);
        expect(controller.get('isAll')).to.equal(false);
      })
    })

    describe('#isMinimum()', function () {

      beforeEach(function() {
        DEFAULT_SERVICES.forEach(function(serviceName) {
          controller.findProperty('serviceName', serviceName).set('isSelected', true);
        });
        OPTIONAL_SERVICES.forEach(function(serviceName) {
          controller.findProperty('serviceName', serviceName).set('isSelected', false);
        });
      });

      it('should return true if isSelected is true for all default services and isSelected is false for all optional services', function() {
        expect(controller.get('isMinimum')).to.equal(true);
      })

      it('should return false if isSelected is true for all default serices and isSelected is true for one of optional services', function() {
        controller.findProperty('serviceName', 'HBASE').set('isSelected', true);
        expect(controller.get('isMinimum')).to.equal(false);
      })

    })

    describe('#needToAddMapReduce', function() {

      describe('mapreduce not selected', function() {
        beforeEach(function() {
          controller.findProperty('serviceName', 'MAPREDUCE').set('isSelected', false);
        })

        it('should return true if Hive is selected and MapReduce is not selected', function() {
          controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
          expect(controller.needToAddMapReduce()).to.equal(true);
        })
        it('should return true if Pig is selected and MapReduce is not selected', function() {
          controller.findProperty('serviceName', 'PIG').set('isSelected', true);
          expect(controller.needToAddMapReduce()).to.equal(true);
        })
        it('should return true if Oozie is selected and MapReduce is not selected', function() {
          controller.findProperty('serviceName', 'OOZIE').set('isSelected', true);
          expect(controller.needToAddMapReduce()).to.equal(true);
        })
      })

      describe('mapreduce not selected', function() {
        beforeEach(function() {
          controller.findProperty('serviceName', 'MAPREDUCE').set('isSelected', true);
        })

        it('should return false if Hive is selected and MapReduce is selected', function() {
          controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
          expect(controller.needToAddMapReduce()).to.equal(false);
        })
        it('should return false if Pig is selected and MapReduce is not selected', function() {
          controller.findProperty('serviceName', 'PIG').set('isSelected', true);
          expect(controller.needToAddMapReduce()).to.equal(false);
        })
        it('should return false if Oozie is selected and MapReduce is not selected', function() {
          controller.findProperty('serviceName', 'OOZIE').set('isSelected', true);
          expect(controller.needToAddMapReduce()).to.equal(false);
        })
      })

    })

    describe('#saveSelectedServiceNamesToDB', function() {

      beforeEach(function() {
        DEFAULT_SERVICES.forEach(function(serviceName) {
          controller.findProperty('serviceName', serviceName).set('isSelected', true);
        });
        OPTIONAL_SERVICES.forEach(function(serviceName) {
          controller.findProperty('serviceName', serviceName).set('isSelected', true);
        });
      });

      it('should store the selected service names in App.db.selectedServiceNames', function() {
        App.db.setLoginName('tester');
        App.db.setClusterName('test');
        controller.saveSelectedServiceNamesToDB();
        // console.log('controller length=' + controller.get('length'));
        var selectedServiceNames = App.db.getSelectedServiceNames();
        // console.log('service length=' + selectedServiceNames.get('length'));
        expect(selectedServiceNames.length === DEFAULT_SERVICES.length + OPTIONAL_SERVICES.length).to.equal(true);
      })

    })

  })*/
  
}});

window.require.define({"test/installer/step5_test": function(exports, require, module) {
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
  
}});

window.require.define({"test/installer/step6_test": function(exports, require, module) {
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
  require('controllers/wizard/step6_controller');

  /*
  describe('App.InstallerStep6Controller', function () {

    var HOSTS = [ 'host1', 'host2', 'host3', 'host4' ];
    //App.InstallerStep6Controller.set.rawHosts = HOSTS;
    var controller = App.InstallerStep6Controller.create();
    controller.set('showHbase', false);
    HOSTS.forEach(function (_hostName) {
      controller.get('hosts').pushObject(Ember.Object.create({
        hostname: _hostName,
        isDataNode: true,
        isTaskTracker: true,
        isRegionServer: true
      }));
    });



  describe('#selectAllDataNodes()', function () {
    controller.get('hosts').setEach('isDataNode', false);

    it('should set isDataNode to true on all hosts', function () {
      controller.selectAllDataNodes();
      expect(controller.get('hosts').everyProperty('isDataNode', true)).to.equal(true);
    })
  })

  describe('#selectAllTaskTrackers()', function () {
    it('should set isTaskTracker to true on all hosts', function () {
      controller.selectAllTaskTrackers();
      expect(controller.get('hosts').everyProperty('isTaskTracker', true)).to.equal(true);
    })
  })

  describe('#selectAllRegionServers()', function () {
    it('should set isRegionServer to true on all hosts', function () {
      controller.selectAllRegionServers();
      expect(controller.get('hosts').everyProperty('isRegionServer', true)).to.equal(true);
    })
  })

  describe('#isAllDataNodes()', function () {

    beforeEach(function () {
      controller.get('hosts').setEach('isDataNode', true);
    })

    it('should return true if isDataNode is true for all services', function () {
      expect(controller.get('isAllDataNodes')).to.equal(true);
    })

    it('should return false if isDataNode is false for one host', function () {
      controller.get('hosts')[0].set('isDataNode', false);
      expect(controller.get('isAllDataNodes')).to.equal(false);
    })
  })

  describe('#isAllTaskTrackers()', function () {

    beforeEach(function () {
      controller.get('hosts').setEach('isTaskTracker', true);
    })

    it('should return true if isTaskTracker is true for all hosts', function () {
      expect(controller.get('isAllTaskTrackers')).to.equal(true);
    })

    it('should return false if isTaskTracker is false for one host', function () {
      controller.get('hosts')[0].set('isTaskTracker', false);
      expect(controller.get('isAllTaskTrackers')).to.equal(false);
    })

  })

  describe('#isAllRegionServers()', function () {

    beforeEach(function () {
      controller.get('hosts').setEach('isRegionServer', true);
    });

    it('should return true if isRegionServer is true for all hosts', function () {
      expect(controller.get('isAllRegionServers')).to.equal(true);
    })

    it('should return false if isRegionServer is false for one host', function () {
      controller.get('hosts')[0].set('isRegionServer', false);
      expect(controller.get('isAllRegionServers')).to.equal(false);
    })

  })

  describe('#validate()', function () {

    beforeEach(function () {
      controller.get('hosts').setEach('isDataNode', true);
      controller.get('hosts').setEach('isTaskTracker', true);
      controller.get('hosts').setEach('isRegionServer', true);
    });

    it('should return false if isDataNode is false for all hosts', function () {
      controller.get('hosts').setEach('isDataNode', false);
      expect(controller.validate()).to.equal(false);
    })

    it('should return false if isTaskTracker is false for all hosts', function () {
      controller.get('hosts').setEach('isTaskTracker', false);
      expect(controller.validate()).to.equal(false);
    })

    it('should return false if isRegionServer is false for all hosts', function () {
      controller.get('hosts').setEach('isRegionServer', false);
      expect(controller.validate()).to.equal(false);
    })

    it('should return true if isDataNode, isTaskTracker, and isRegionServer is true for all hosts', function () {
      expect(controller.validate()).to.equal(true);
    })

    it('should return true if isDataNode, isTaskTracker, and isRegionServer is true for only one host', function () {
      controller.get('hosts').setEach('isDataNode', false);
      controller.get('hosts').setEach('isTaskTracker', false);
      controller.get('hosts').setEach('isRegionServer', false);
      var host = controller.get('hosts')[0];
      host.set('isDataNode', true);
      host.set('isTaskTracker', true);
      host.set('isRegionServer', true);
      expect(controller.validate()).to.equal(true);
    })

  })

  })*/
  
}});

window.require.define({"test/installer/step7_test": function(exports, require, module) {
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
  require('controllers/wizard/step7_controller');

  /*
  describe('App.InstallerStep7Controller', function () {

  })*/
  
}});

window.require.define({"test/installer/step9_test": function(exports, require, module) {
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
  require('models/hosts');
  require('controllers/wizard/step9_controller');

  /*describe('App.InstallerStep9Controller', function () {
    //var controller = App.InstallerStep3Controller.create();

    describe('#isStepFailed', function () {
      var controller = App.InstallerStep9Controller.create();
      it('should return true if even a single action of a role with 100% success factor fails', function () {
        var polledData = new Ember.Set([
          {
            actionId: '1',
            name: '192.168.1.1',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 30%'
          },
          {
            actionId: '2',
            name: '192.168.1.2',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 20%'
          },
          {
            actionId: '3',
            name: '192.168.1.3',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 30%'
          },
          {
            actionId: '4',
            name: '192.168.1.4',
            status: 'failed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 40%'
          }
        ]);


        expect(controller.isStepFailed(polledData)).to.equal(true);

      })

      it('should return false if action of a role fails but with less percentage than success factor of the role', function () {
        var polledData = new Ember.Set([
          {
            actionId: '1',
            name: '192.168.1.1',
            status: 'failed',
            sf: '30',
            role: 'DataNode',
            message: 'completed 30%'
          },
          {
            actionId: '2',
            name: '192.168.1.2',
            status: 'failed',
            sf: '30',
            role: 'DataNode',
            message: 'completed 20%'
          },
          {
            actionId: '3',
            name: '192.168.1.3',
            status: 'completed',
            sf: '30',
            role: 'DataNode',
            message: 'completed 30%'
          },
          {
            actionId: '4',
            name: '192.168.1.4',
            status: 'completed',
            sf: '30',
            role: 'DataNode',
            message: 'completed 40%'
          }
        ]);

        expect(controller.isStepFailed(polledData)).to.equal(false);

      })

    })

    describe('#setHostsStatus', function () {
      var controller = App.InstallerStep9Controller.create();
      it('sets the status of all hosts in the content to the passed status value', function () {
        var mockData = new Ember.Set(
          {
            actionId: '1',
            name: '192.168.1.1',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 30%'
          },
          {
            actionId: '2',
            name: '192.168.1.2',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 20%'
          },
          {
            actionId: '3',
            name: '192.168.1.3',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 30%'
          },
          {
            actionId: '4',
            name: '192.168.1.4',
            status: 'completed',
            sf: '100',
            role: 'DataNode',
            message: 'completed 40%'
          }
        );
        mockData.forEach(function(_polledData){
          controller.content.pushObject(_polledData);
        });

        controller.setHostsStatus(mockData,'finish');
        var result = controller.content.everyProperty('status','finish');
        //console.log('value of pop is: '+ result.pop.actionId);
        expect(result).to.equal(true);

      })
    })


  })*/


  
}});

window.require.define({"test/login_test": function(exports, require, module) {
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

  require('controllers/login_controller');

  describe('App.LoginController', function () {

    var loginController = App.LoginController.create();

    describe('#validateCredentials()', function () {
      /*
      it('should return undefined if no username is present', function () {
        loginController.set('loginName', '');
        expect(loginController.validateCredentials()).to.equal(undefined);
      })
      it('should return undefined if no password is present', function () {
        loginController.set('password', '');
        expect(loginController.validateCredentials()).to.equal(undefined);
      })
      it('should return the user object with the specified username and password (dummy until actual integration)', function () {
        loginController.set('loginName', 'admin');
        loginController.set('password', 'admin');
        expect(loginController.validateCredentials().get('loginName'), 'admin');
      })
      */
    })
  })
  
}});

window.require.define({"test/main/dashboard_test": function(exports, require, module) {
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

  /*
  var App = require('app');

  require('models/alert'); 
  App.Alert.FIXTURES = [{ status: 'ok' }, { status: 'corrupt' }, { status: 'corrupt',}];
  require('controllers/main/dashboard');
   
  describe('MainDashboard', function () {
   
    var controller = App.MainDashboardController.create();
    
    describe('#alertsCount', function () {
      it('should return 2 if 2 alerts has status corrupt', function () {
          expect(controller.get('alertsCount')).to.equal(2);
      })
    })
  })
  */
}});

window.require.define({"test/main/host/details_test": function(exports, require, module) {
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

  /*
  var Ember = require('ember');
  var App = require('app');
   
  require('controllers/main/host/details');

  describe('MainHostdetails', function () {
    var controller = App.MainHostDetailsController.create();
    controller.content = Ember.Object.create({});
     
    describe('#setBack(value)', function () {
      it('should return true if value is true', function () {
        controller.setBack(true);
        expect(controller.get('isFromHosts')).to.equal(true);
      })
    })
    describe('#workStatus positive', function () {
      it('should return true if workstatus is true', function () {
        controller.content.set('workStatus',true);   
        expect(controller.get('isStarting')).to.equal(true);
        })
      it('should return false if workStatus is true', function () {
        expect(controller.get('isStopping')).to.equal(false);
      })
      it('should return false if workstatus is false', function () {
        controller.content.set('workStatus',false);   
        expect(controller.get('isStarting')).to.equal(false);
        })
      it('should return true if workStatus is false', function () {
        expect(controller.get('isStopping')).to.equal(true);
      })
    })
  })
  */
}});

window.require.define({"test/main/host_test": function(exports, require, module) {
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

  /*
  var App = require('app');
  require('models/cluster');
  require('models/service');
  require('models/pagination');
  require('controllers/main/host');

  describe('MainHostController', function () {
      describe('#sortByName()', function () {
          it('should change isSort value to true', function () {
              var mainHostController = App.MainHostController.create();
              mainHostController.set('isSort', false);
              mainHostController.sortByName();
              expect(mainHostController.get('isSort')).to.equal(true);
          });


          it('should inverse sortingAsc ', function () {
              var mainHostController = App.MainHostController.create();
              mainHostController.set('sortingAsc', false);
              mainHostController.sortByName();
              expect(mainHostController.get('sortingAsc')).to.equal(true);
              mainHostController.sortByName();
              expect(mainHostController.get('sortingAsc')).to.equal(false);
          })
      });


      describe('#showNextPage, #showPreviousPage()', function () {
          it('should change rangeStart according to page', function () {
              var mainHostController = App.MainHostController.create();
              mainHostController.set('pageSize', 3);
              mainHostController.showNextPage();
              expect(mainHostController.get('rangeStart')).to.equal(3);
              mainHostController.showPreviousPage();
              expect(mainHostController.get('rangeStart')).to.equal(0);
          })
      });


      describe('#sortClass()', function () {
          it('should return \'icon-arrow-down\' if sortingAsc is true', function () {
              var mainHostController = App.MainHostController.create({});
              mainHostController.set('sortingAsc', true);
              expect(mainHostController.get('sortClass')).to.equal('icon-arrow-down');
          });
          it('should return \'icon-arrow-up\' if sortingAsc is false', function () {
              var mainHostController = App.MainHostController.create({});
              mainHostController.set('sortingAsc', false);
              expect(mainHostController.get('sortClass')).to.equal('icon-arrow-up');
          })
      });


      describe('#allChecked', function () {
          it('should fill selectedhostsids array', function () {
              var mainHostController = App.MainHostController.create();
              mainHostController.set('allChecked', false);
              expect(mainHostController.get('selectedHostsIds').length).to.equal(0);
              mainHostController.set('allChecked', true);
              expect(!!(mainHostController.get('selectedHostsIds').length)).to.equal(true);
          })
      });


  });
  */
  
}});

window.require.define({"test/main/item_test": function(exports, require, module) {
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

  /*
  var App = require('app');
  require('views/common/modal_popup');
  require('controllers/main/service/item');

  describe('App.MainServiceItemController', function () {

      describe('#showRebalancer', function () {
          it('should return true if serviceName is hdfs', function () {
              var mainServiceItemController = App.MainServiceItemController.create({
              });
              mainServiceItemController.content.set('serviceName', 'hdfs');
              expect(mainServiceItemController.get('showRebalancer')).to.equal(true);
          })
      })
  })
  */
}});

window.require.define({"test/utils/form_field_test": function(exports, require, module) {
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
  require('models/form');


  /*
   * formField.isValid property doesn't update correctly, so I have to work with errorMessage property
   */
  describe('App.FormField', function () {

    describe('#validate()', function () {
      /*DIGITS TYPE*/
      it('123456789 is correct digits', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'digits');
        formField.set('value', 123456789);
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('"a33bc" is incorrect digits', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'digits');
        formField.set('value', 'a33bc');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      /*DIGITS TYPE END*/
      /*NUMBER TYPE*/
      it('+1234 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '+1234');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('-1234 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '-1234');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('-1.23.6 is incorrect number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '-1.23.6');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      it('+1.6 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', +1.6);
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('-1.6 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', -1.6);
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('1.6 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', 1.6);
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('-.356 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '-.356');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('+.356 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '+.356');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('-1. is incorrect number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '-1.');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      it('+1. is incorrect number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '+1.');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      it('1. is incorrect number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '1.');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      it('-1,23,6 is incorrect number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '-1,23,6');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      it('-1234567890 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '-1234567890');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('+1234567890 is correct number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '+1234567890');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(true);
      })
      it('123eed is incorrect number', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'number');
        formField.set('value', '123eed');
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      /*NUMBER TYPE END*/
      /*REQUIRE*/
      it('Required field shouldn\'t be empty', function () {
        var formField = App.FormField.create();
        formField.set('displayType', 'string');
        formField.set('value', '');
        formField.set('isRequired', true);
        formField.validate();
        expect(formField.get('errorMessage') === '').to.equal(false);
      })
      /*REQUIRE END*/

    })
  })
}});

window.require.define({"test/utils/validator_test": function(exports, require, module) {
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

  var validator = require('utils/validator');

  describe('validator', function () {

    describe('#isValidEmail(value)', function () {
      it('should return false if value is null', function () {
        expect(validator.isValidEmail(null)).to.equal(false);
      })
      it('should return false if value is ""', function () {
        expect(validator.isValidEmail('')).to.equal(false);
      })
      it('should return false if value is "a.com"', function () {
        expect(validator.isValidEmail('a.com')).to.equal(false);
      })
      it('should return false if value is "@a.com"', function () {
        expect(validator.isValidEmail('@a.com')).to.equal(false);
      })
      it('should return false if value is "a@.com"', function () {
        expect(validator.isValidEmail('a@.com')).to.equal(false);
      })
      it('should return true if value is "a@a.com"', function () {
        expect(validator.isValidEmail('a@a.com')).to.equal(true);
      })
      it('should return true if value is "user@a.b.com"', function () {
        expect(validator.isValidEmail('user@a.b.com')).to.equal(true);
      })
    })

    describe('#isValidInt(value)', function () {
      it('should return false if value is null', function () {
        expect(validator.isValidInt(null)).to.equal(false);
      })
      it('should return false if value is ""', function () {
        expect(validator.isValidInt('')).to.equal(false);
      })
      it('should return false if value is "abc"', function () {
        expect(validator.isValidInt('abc')).to.equal(false);
      })
      it('should return false if value is "0xff"', function () {
        expect(validator.isValidInt('0xff')).to.equal(false);
      })
      it('should return false if value is " 1""', function () {
        expect(validator.isValidInt(' 1')).to.equal(false);
      })
      it('should return false if value is "1 "', function () {
        expect(validator.isValidInt('1 ')).to.equal(false);
      })
      it('should return true if value is "10"', function () {
        expect(validator.isValidInt('10')).to.equal(true);
      })
      it('should return true if value is "-123"', function () {
        expect(validator.isValidInt('-123')).to.equal(true);
      })
      it('should return true if value is "0"', function () {
        expect(validator.isValidInt('0')).to.equal(true);
      })
      it('should return true if value is 10', function () {
        expect(validator.isValidInt(10)).to.equal(true);
      })
      it('should return true if value is -123', function () {
        expect(validator.isValidInt(10)).to.equal(true);
      })
      it('should return true if value is 0', function () {
        expect(validator.isValidInt(10)).to.equal(true);
      })
    })

    describe('#isValidFloat(value)', function () {
      it('should return false if value is null', function () {
        expect(validator.isValidFloat(null)).to.equal(false);
      })
      it('should return false if value is ""', function () {
        expect(validator.isValidFloat('')).to.equal(false);
      })
      it('should return false if value is "abc"', function () {
        expect(validator.isValidFloat('abc')).to.equal(false);
      })
      it('should return false if value is "0xff"', function () {
        expect(validator.isValidFloat('0xff')).to.equal(false);
      })
      it('should return false if value is " 1""', function () {
        expect(validator.isValidFloat(' 1')).to.equal(false);
      })
      it('should return false if value is "1 "', function () {
        expect(validator.isValidFloat('1 ')).to.equal(false);
      })
      it('should return true if value is "10"', function () {
        expect(validator.isValidFloat('10')).to.equal(true);
      })
      it('should return true if value is "-123"', function () {
        expect(validator.isValidFloat('-123')).to.equal(true);
      })
      it('should return true if value is "0"', function () {
        expect(validator.isValidFloat('0')).to.equal(true);
      })
      it('should return true if value is 10', function () {
        expect(validator.isValidFloat(10)).to.equal(true);
      })
      it('should return true if value is -123', function () {
        expect(validator.isValidFloat(10)).to.equal(true);
      })
      it('should return true if value is 0', function () {
        expect(validator.isValidFloat(10)).to.equal(true);
      })
      it('should return true if value is "0.0"', function () {
        expect(validator.isValidFloat("0.0")).to.equal(true);
      })
      it('should return true if value is "10.123"', function () {
        expect(validator.isValidFloat("10.123")).to.equal(true);
      })
      it('should return true if value is "-10.123"', function () {
        expect(validator.isValidFloat("-10.123")).to.equal(true);
      })
      it('should return true if value is 10.123', function () {
        expect(validator.isValidFloat(10.123)).to.equal(true);
      })
      it('should return true if value is -10.123', function () {
        expect(validator.isValidFloat(-10.123)).to.equal(true);
      })

    })
    /*describe('#isIpAddress(value)', function () {
      it('"127.0.0.1" - valid IP', function () {
        expect(validator.isIpAddress('127.0.0.1')).to.equal(true);
      })
      it('"227.3.67.196" - valid IP', function () {
        expect(validator.isIpAddress('227.3.67.196')).to.equal(true);
      })
      it('"327.0.0.0" - invalid IP', function () {
        expect(validator.isIpAddress('327.0.0.0')).to.equal(false);
      })
      it('"127.0.0." - invalid IP', function () {
        expect(validator.isIpAddress('127.0.0.')).to.equal(false);
      })
      it('"127.0." - invalid IP', function () {
        expect(validator.isIpAddress('127.0.')).to.equal(false);
      })
      it('"127" - invalid IP', function () {
        expect(validator.isIpAddress('127')).to.equal(false);
      })
      it('"127.333.0.1" - invalid IP', function () {
        expect(validator.isIpAddress('127.333.0.1')).to.equal(false);
      })
      it('"127.0.333.1" - invalid IP', function () {
        expect(validator.isIpAddress('127.0.333.1')).to.equal(false);
      })
      it('"127.0.1.333" - invalid IP', function () {
        expect(validator.isIpAddress('127.0.1.333')).to.equal(false);
      })
      it('"127.0.0.0:45555" - valid IP', function () {
        expect(validator.isIpAddress('127.0.0.0:45555')).to.equal(true);
      })
      it('"327.0.0.0:45555" - invalid IP', function () {
        expect(validator.isIpAddress('327.0.0.0:45555')).to.equal(false);
      })
      it('"0.0.0.0" - invalid IP', function () {
        expect(validator.isIpAddress('0.0.0.0')).to.equal(false);
      })
      it('"0.0.0.0:12" - invalid IP', function () {
        expect(validator.isIpAddress('0.0.0.0:12')).to.equal(false);
      })
      it('"1.0.0.0:0" - invalid IP', function () {
        expect(validator.isIpAddress('1.0.0.0:0')).to.equal(false);
      })
    })*/
    describe('#isDomainName(value)', function () {
      it('"google.com" - valid Domain Name', function () {
        expect(validator.isDomainName('google.com')).to.equal(true);
      })
      it('"google" - invalid Domain Name', function () {
        expect(validator.isDomainName('google')).to.equal(false);
      })
      it('"123.123" - invalid Domain Name', function () {
        expect(validator.isDomainName('123.123')).to.equal(false);
      })
      it('"4goog.le" - valid Domain Name', function () {
        expect(validator.isDomainName('4goog.le')).to.equal(true);
      })
      it('"55454" - invalid Domain Name', function () {
        expect(validator.isDomainName('55454')).to.equal(false);
      })
    })

  })
}});

window.require('test/installer/step1_test');
window.require('test/installer/step2_test');
window.require('test/installer/step3_test');
window.require('test/installer/step4_test');
window.require('test/installer/step5_test');
window.require('test/installer/step6_test');
window.require('test/installer/step7_test');
window.require('test/installer/step9_test');
window.require('test/login_test');
window.require('test/main/dashboard_test');
window.require('test/main/host/details_test');
window.require('test/main/host_test');
window.require('test/main/item_test');
window.require('test/utils/form_field_test');
window.require('test/utils/validator_test');
