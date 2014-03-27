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
        Em.Object.create({name:'wst3_host1', bootStatus: 'REGISTERED', isChecked: false}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERING', isChecked: false})
      ],
      m: 'One registered, one registering',
      visibleHosts: {
        RUNNING: {
          e: 0
        },
        REGISTERING: {
          e: 1
        },
        REGISTERED: {
          e: 1
        },
        FAILED: {
          e: 0
        }
      },
      onAllChecked: {
        e: [true, true]
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'REGISTERED', isChecked: false}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERED', isChecked: false})
      ],
      m: 'Two registered',
      visibleHosts: {
        RUNNING: {
          e: 0
        },
        REGISTERING: {
          e: 0
        },
        REGISTERED: {
          e: 2
        },
        FAILED: {
          e: 0
        }
      },
      onAllChecked: {
        e: [true, true]
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'FAILED', isChecked: false}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERED', isChecked: false})
      ],
      m: 'One registered, one failed',
      visibleHosts: {
        RUNNING: {
          e: 0
        },
        REGISTERING: {
          e: 0
        },
        REGISTERED: {
          e: 1
        },
        FAILED: {
          e: 1
        }
      },
      onAllChecked: {
        e: [true, true]
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'FAILED', isChecked: false}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'FAILED', isChecked: false})
      ],
      m: 'Two failed',
      visibleHosts: {
        RUNNING: {
          e: 0
        },
        REGISTERING: {
          e: 0
        },
        REGISTERED: {
          e: 0
        },
        FAILED: {
          e: 2
        }
      },
      onAllChecked: {
        e: [true, true]
      }
    },
    {
      bootHosts: [
        Em.Object.create({name:'wst3_host1', bootStatus: 'REGISTERING', isChecked: false}),
        Em.Object.create({name:'wst3_host2', bootStatus: 'REGISTERING', isChecked: false})
      ],
      m: 'Two registering',
      visibleHosts: {
        RUNNING: {
          e: 0
        },
        REGISTERING: {
          e: 2
        },
        REGISTERED: {
          e: 0
        },
        FAILED: {
          e: 0
        }
      },
      onAllChecked: {
        e: [true, true]
      }
    }
  ];

  describe('#registrationTimeoutSecs', function() {
    it('Manual install', function() {
      var controller = App.WizardStep3Controller.create({
        content: {
          installOptions: {
            manualInstall: true
          }
        }
      });
      expect(controller.get('registrationTimeoutSecs')).to.equal(15);
    });
    it('Not manual install', function() {
      var controller = App.WizardStep3Controller.create({
        content: {
          installOptions: {
            manualInstall: false
          }
        }
      });
      expect(controller.get('registrationTimeoutSecs')).to.equal(120);
    });
  });

  describe('#isHostHaveWarnings', function() {
    var tests = [
      {
        warnings: [{},{}],
        m: 'Warnings exist',
        e: true
      },
      {
        warnings: [],
        m: 'Warnings don\'t exist',
        e: false
      }
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep3Controller.create();
      controller.set('warnings', test.warnings);
      it(test.m, function() {
        expect(controller.get('isHostHaveWarnings')).to.equal(test.e);
      });
    });
  });
});