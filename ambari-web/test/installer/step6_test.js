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

describe('App.WizardStep6Controller', function () {

  var controller = App.WizardStep6Controller.create();

  controller.set('content', {
    hosts: {},
    masterComponentHosts: {},
    services: [
      Em.Object.create({
        serviceName: 'MAPREDUCE',
        isSelected: true
      }),
      Em.Object.create({
        serviceName: 'YARN',
        isSelected: true
      }),
      Em.Object.create({
        serviceName: 'HBASE',
        isSelected: true
      }),
      Em.Object.create({
        serviceName: 'HDFS',
        isSelected: true
      })
    ]
  });

  var HOSTS = Em.A([ 'host0', 'host1', 'host2', 'host3' ]);

  var h = {};
  var m = [];
  HOSTS.forEach(function (hostName) {
    var obj = Em.Object.create({
      name: hostName,
      hostName: hostName,
      bootStatus: 'REGISTERED'
    });
    h[hostName] = obj;
    m.push(obj);
  });

  controller.set('content.hosts', h);
  controller.set('content.masterComponentHosts', m);
  controller.set('isMasters', false);


  describe('#loadStep', function() {
    controller.loadStep();
    it('Hosts are loaded', function() {
      expect(controller.get('hosts').length).to.equal(HOSTS.length);
    });

    it('Headers are loaded', function() {
      expect(controller.get('headers').length).not.to.equal(0);
    });
  });

  describe('#isAddHostWizard', function() {
    it('true if content.controllerName is addHostController', function() {
      controller.set('content.controllerName', 'addHostController');
      expect(controller.get('isAddHostWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addHostController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isAddHostWizard')).to.equal(false);
    });
  });

  describe('#isInstallerWizard', function() {
    it('true if content.controllerName is addHostController', function() {
      controller.set('content.controllerName', 'installerController');
      expect(controller.get('isInstallerWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addHostController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isInstallerWizard')).to.equal(false);
    });
  });

  describe('#isAddServiceWizard', function() {
    it('true if content.controllerName is addServiceController', function() {
      controller.set('content.controllerName', 'addServiceController');
      expect(controller.get('isAddServiceWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addServiceController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isAddServiceWizard')).to.equal(false);
    });
  });

  describe('#setAllNodes', function() {

    var test_config = Em.A([
      {
        title: 'DataNode',
        name: 'DATANODE',
        state: false
      },
      {
        title: 'DataNode',
        name: 'DATANODE',
        state: true
      },
      {
        title: 'TaskTracker',
        name: 'TASKTRACKER',
        state: false
      },
      {
        title: 'TaskTracker',
        name: 'TASKTRACKER',
        state: true
      }
    ]);

    test_config.forEach(function(test) {
      it((test.state?'Select':'Deselect') + ' all ' + test.title, function() {
        controller.setAllNodes(test.name, test.state);
        var hosts = controller.get('hosts');
        hosts.forEach(function(host) {
          var cb = host.get('checkboxes').filterProperty('isInstalled', false).findProperty('component', test.name);
          if (cb) {
            expect(cb.get('checked')).to.equal(test.state);
          }
        });
      });
    });


  });

  describe('#isServiceSelected', function() {
    controller.get('content.services').forEach(function(service) {
      it(service.serviceName + ' is selected', function() {
        expect(controller.isServiceSelected(service.serviceName)).to.equal(true);
      });
    });
    var unselectedService = 'FAKESERVICE';
    it(unselectedService + ' is not selected', function() {
      expect(controller.isServiceSelected(unselectedService)).to.equal(false);
    });
  });

  describe('#validateEachComponent', function() {
    it('Nothing checked', function() {
      controller.get('hosts').forEach(function(host) {
        host.get('checkboxes').setEach('checked', false);
      });
      expect(controller.validateEachComponent('')).to.equal(false);
    });
    it('One slave is not selected for no one host', function() {
      controller.get('hosts').forEach(function(host) {
        host.get('checkboxes').forEach(function(checkbox, index) {
          checkbox.set('checked', index === 0);
        });
      });
      expect(controller.validateEachComponent('')).to.equal(false);
    });
    it('All checked', function() {
      controller.get('hosts').forEach(function(host) {
        host.get('checkboxes').forEach(function(checkbox) {
          checkbox.set('checked', true);
        });
      });
      expect(controller.validateEachComponent('')).to.equal(true);
    });
  });

  describe('#validateEachHost', function() {
    it('Nothing checked', function() {
      controller.get('hosts').forEach(function(host) {
        host.get('checkboxes').setEach('checked', false);
      });
      expect(controller.validateEachHost('')).to.equal(false);
    });
    it('One host doesn\'t have assigned slaves', function() {
      controller.get('hosts').forEach(function(host, index) {
        host.get('checkboxes').setEach('checked', index === 0);
      });
      expect(controller.validateEachHost('')).to.equal(false);
    });
    it('All checked', function() {
      controller.get('hosts').forEach(function(host) {
        host.get('checkboxes').setEach('checked', true);
      });
      expect(controller.validateEachHost('')).to.equal(true);
    });
  });

});
