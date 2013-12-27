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
var components = require('data/service_components');

describe('App.WizardStep5Controller', function () {
  var controller = App.WizardStep5Controller.create();
  controller.set('content', {});
  var cpu = 2, memory = 4;
  var schemes = [
    {'description': 'empty condition'},
    {
      'description': 'second host if amount more than 1',
      "else": 1
    },
    {
      'description': 'first host if amount less than 3, third host if amount less than 6, fourth host if amount more than 5',
      "3": 0,
      "6": 2,
      "else": 3
    },
    {
      'description': 'second host if amount less than 3, second host if amount less than 6, third host if amount less than 31, sixth host if amount more than 30',
      "3": 1,
      "6": 1,
      "31": 2,
      "else": 5
    }
  ];
  var test_config = [
    {
      title: '1 host',
      hosts: ['host0'],
      equals: [0, 0, 0, 0]
    },
    {
      title: '2 hosts',
      hosts: ['host0', 'host1'],
      equals: [0, 1, 0, 1]
    },
    {
      title: '3 hosts',
      hosts: ['host0', 'host1', 'host2'],
      equals: [0, 1, 2, 1]
    },
    {
      title: '5 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4'],
      equals: [0, 1, 2, 1]
    },
    {
      title: '6 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host6'],
      equals: [0, 1, 3, 2]
    },
    {
      title: '10 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9'],
      equals: [0, 1, 3, 2]
    },
    {
      title: '31 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9', 'host10', 'host11', 'host12', 'host13', 'host14', 'host15', 'host16', 'host17', 'host18', 'host19', 'host20', 'host21', 'host22', 'host23', 'host24', 'host25', 'host26', 'host27', 'host28', 'host29', 'host30'],
      equals: [0, 1, 3, 5]
    }
  ];

  schemes.forEach(function(scheme, index) {
    describe('#getHostForComponent() condition: ' + scheme.description, function() {

      delete scheme['description'];

      test_config.forEach(function(test) {
        it(test.title, function () {
          controller.get('hosts').clear();
          test.hosts.forEach(function(_host) {
            controller.get('hosts').pushObject(Em.Object.create({
              host_name: _host,
              cpu: cpu,
              memory: memory
            }));
          });
          expect(controller.getHostForComponent(test.hosts.length, scheme).host_name).to.equal(test.hosts[test.equals[index]]);
        });
      });
    });
  });

  describe('#getZooKeeperServer', function() {
    it('should be array with three host names if hosts number more than three', function() {
      var hosts = [
        {host_name: 'host1'},
        {host_name: 'host2'},
        {host_name: 'host3'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getZooKeeperServer(hosts.length)).to.eql(['host1', 'host2', 'host3']);
    });

    it('should be array with one host names if hosts number less than three', function() {
      var hosts = [
        {host_name: 'host1'},
        {host_name: 'host2'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getZooKeeperServer(hosts.length)).to.eql(['host1']);
    });
  });

  describe('#getGangliaServer', function() {
    it('should be host name if one host ', function() {
      var hosts = [
        {host_name: 'host1'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getGangliaServer(hosts.length)).to.eql('host1');
    });

    it('should be host name if hosts number more than one', function() {
      var hosts = [
        {host_name: 'host1'},
        {host_name: 'host2'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getGangliaServer(hosts.length)).to.eql('host1');
    });

    it('should be host name different from localhost if hosts number more than one', function() {
      var hosts = [
        {host_name: ''},
        {host_name: 'host2'}
      ];
      //first host_name is empty string, because of location.hostname = "" in console,
      //to implement current test case

      controller.set('hosts', hosts);
      expect(controller.getGangliaServer(hosts.length)).to.eql('host2');
    });
  });


  controller.set('content', {});

  describe('#isReassignWizard', function() {
    it('true if content.controllerName is reassignMasterController', function() {
      controller.set('content.controllerName', 'reassignMasterController');
      expect(controller.get('isReassignWizard')).to.equal(true);
    });
    it('false if content.controllerName is not reassignMasterController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isReassignWizard')).to.equal(false);
    });
  });

});
