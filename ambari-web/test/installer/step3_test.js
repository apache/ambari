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
require('utils/http_client');
require('models/host');
require('controllers/wizard/step3_controller');

describe('App.WizardStep3Controller', function () {

  describe('#getAllRegisteredHostsCallback', function () {
    it('One host is already in the cluster, one host is registered', function() {
      var controller = App.WizardStep3Controller.create({
        hostsInCluster: [{
          hostName: 'wst3_host1'
        }],
        bootHosts: [
          {name:'wst3_host1'},
          {name:'wst3_host2'}
        ]
      });
      var test_data = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host1'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host2'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host3'
            }
          }
        ]
      };
      controller.getAllRegisteredHostsCallback(test_data);
      expect(controller.get('hasMoreRegisteredHosts')).to.equal(true);
      expect(controller.get('registeredHosts').length).to.equal(1);
    });

    it('All hosts are new', function() {
      var controller = App.WizardStep3Controller.create({
        hostsInCluster: [{
          hostName: 'wst3_host1'
        }],
        bootHosts: [
          {name:'wst3_host3'},
          {name:'wst3_host4'}
        ]
      });
      var test_data = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host3'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host4'
            }
          }
        ]
      };
      controller.getAllRegisteredHostsCallback(test_data);
      expect(controller.get('hasMoreRegisteredHosts')).to.equal(false);
      expect(controller.get('registeredHosts')).to.equal('');
    });

    it('No new hosts', function() {
      var controller = App.WizardStep3Controller.create({
        hostsInCluster: [{
          hostName: 'wst3_host1'
        }],
        bootHosts: [
          {name:'wst3_host1'}
        ]
      });
      var test_data = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host1'
            }
          }
        ]
      };
      controller.getAllRegisteredHostsCallback(test_data);
      expect(controller.get('hasMoreRegisteredHosts')).to.equal(false);
      expect(controller.get('registeredHosts')).to.equal('');
    });

  });

  var tests = [
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'REGISTERED'}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERING'})
      ],
      m: 'One registered, one registering',
      allHostsComplete: {
        e: false
      },
      isInstallInProgress: {
        e: true
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'REGISTERED'}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERED'})
      ],
      m: 'Two registered',
      allHostsComplete: {
        e: true
      },
      isInstallInProgress: {
        e: false
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'FAILED'}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERED'})
      ],
      m: 'One registered, one failed',
      allHostsComplete: {
        e: true
      },
      isInstallInProgress: {
        e: false
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'FAILED'}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'FAILED'})
      ],
      m: 'Two failed',
      allHostsComplete: {
        e: true
      },
      isInstallInProgress: {
        e: false
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'REGISTERING'}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERING'})
      ],
      m: 'Two registering',
      allHostsComplete: {
        e: false
      },
      isInstallInProgress: {
        e: true
      }
    }
  ];

  describe('#allHostsComplete', function() {
    tests.forEach(function(test) {
      var controller = App.WizardStep3Controller.create({
        bootHosts: test.bootHosts
      });
      it(test.m, function() {
        expect(controller.get('allHostsComplete')).to.equal(test.allHostsComplete.e);
      });
    });
  });

  describe('#isInstallInProgress', function() {
    tests.forEach(function(test) {
      var controller = App.WizardStep3Controller.create({
        bootHosts: test.bootHosts
      });
      it(test.m, function() {
        expect(controller.get('isInstallInProgress')).to.equal(test.isInstallInProgress.e);
      });
    });
  });
});