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
  var cpu = 2, memory = 4;

  var methods = ['getKerberosServer', 'getNameNode', 'getSNameNode', 'getJobTracker', 'getResourceManager', 'getHistoryServer', 'getHBaseMaster', 'getOozieServer', 'getHiveServer', 'getHiveMetastore', 'getWebHCatServer'];

  var test_config = [
    {
      title: '1 host',
      hosts: ['host0'],
      equals: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    },
    {
      title: '2 hosts',
      hosts: ['host0', 'host1'],
      equals: [1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1]
    },
    {
      title: '3 hosts',
      hosts: ['host0', 'host1', 'host2'],
      equals: [1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1]
    },
    {
      title: '5 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4'],
      equals: [1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1]
    },
    {
      title: '6 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host6'],
      equals: [3, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2]
    },
    {
      title: '10 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9'],
      equals: [3, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2]
    },
    {
      title: '31 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9', 'host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9', 'host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9', 'host0'],
      equals: [5, 0, 1, 2, 2, 2, 3, 3, 4, 4, 4]
    }
  ];

  test_config.forEach(function(test) {
    describe(test.title, function() {
      var controller = App.WizardStep5Controller.create();
      controller.clearStep();

      test.hosts.forEach(function(_host) {
        controller.get('hosts').pushObject(Em.Object.create({
          host_name: _host,
          cpu: cpu,
          memory: memory
        }));
      });

      methods.forEach(function(method, index) {
        it('#' + method + '()', function() {
          expect(controller[method](test.hosts.length).host_name).to.equal(test.hosts[test.equals[index]]);
        });
      });

    });

  });


  var controller = App.WizardStep5Controller.create();
  controller.set('content', {});

  describe('#isReassignWizard', function() {
    it('true if content.controllerName is reassignMasterController', function() {
      controller.set('content.controllerName', 'reassignMasterController');
      expect(controller.get('isReassignWizard')).to.equal(true);
    })
    it('false if content.controllerName is not reassignMasterController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isReassignWizard')).to.equal(false);
    })
  });

});
