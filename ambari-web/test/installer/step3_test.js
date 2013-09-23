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
      allHostsComplete: {
        e: false
      },
      isInstallInProgress: {
        e: true
      },
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
      allHostsComplete: {
        e: true
      },
      isInstallInProgress: {
        e: false
      },
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
      allHostsComplete: {
        e: true
      },
      isInstallInProgress: {
        e: false
      },
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
      allHostsComplete: {
        e: true
      },
      isInstallInProgress: {
        e: false
      },
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
      allHostsComplete: {
        e: false
      },
      isInstallInProgress: {
        e: true
      },
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

  describe('#visibleHosts', function() {
    var c = ['RUNNING', 'REGISTERING', 'REGISTERED', 'FAILED'];
    tests.forEach(function(test) {
      describe(test.m, function() {
        c.forEach(function(_c) {
          var controller = App.WizardStep3Controller.create({
            hosts: test.bootHosts
          });
          controller.set('category', {hostsBootStatus: _c});
          it(_c, function() {
            expect(controller.get('visibleHosts').length).to.equal(test.visibleHosts[_c].e);
          });
        });
        var controller = App.WizardStep3Controller.create({
          hosts: test.bootHosts
        });
        controller.set('category', false);
        it('ALL', function() {
          expect(controller.get('visibleHosts').length).to.equal(test.bootHosts.length);
        });
      });
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

  describe('#onAllChecked', function() {
    tests.forEach(function(test) {
      var controller = App.WizardStep3Controller.create({
        hosts: test.bootHosts
      });
      controller.set('allChecked', true);
      it(test.m, function() {
        expect(controller.get('visibleHosts').getEach('isChecked')).to.eql(test.onAllChecked.e);
      });
    });
  });

  describe('#noHostsSelected', function() {
    tests.forEach(function(test) {
      it(test.m + ' - nothing checked', function() {
        var controller = App.WizardStep3Controller.create({
          hosts: test.bootHosts
        });
        controller.get('hosts').setEach('isChecked', false);
        console.log(controller.hosts);
        expect(controller.get('noHostsSelected')).to.equal(true);
      });
      it(test.m + ' - one checked', function() {
        var controller = App.WizardStep3Controller.create({
          hosts: test.bootHosts
        });
        controller.get('hosts').setEach('isChecked', true);
        expect(controller.get('noHostsSelected')).to.equal(false);
      });
    });
  });



});