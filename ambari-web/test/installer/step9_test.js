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
require('models/stack_service_component');
require('models/hosts');
require('controllers/wizard/step9_controller');
require('utils/helper');
var modelSetup = require('test/init_model_test');
var c, obj;
describe('App.InstallerStep9Controller', function () {
  beforeEach(function () {
    modelSetup.setupStackServiceComponent();
    c = App.WizardStep9Controller.create();
    obj = App.InstallerController.create();
  });
  afterEach(function () {
    modelSetup.cleanStackServiceComponent();
  });

  describe('#isSubmitDisabled', function () {
    var tests = Em.A([
      {controllerName: 'addHostController', state: 'STARTED', e: false},
      {controllerName: 'addHostController', state: 'START FAILED', e: false},
      {controllerName: 'addHostController', state: 'INSTALL FAILED', e: false},
      {controllerName: 'addHostController', state: 'PENDING', e: true},
      {controllerName: 'addHostController', state: 'INSTALLED', e: true},
      {controllerName: 'addServiceController', state: 'STARTED', e: false},
      {controllerName: 'addServiceController', state: 'START FAILED', e: false},
      {controllerName: 'addServiceController', state: 'INSTALL FAILED', e: false},
      {controllerName: 'addServiceController', state: 'PENDING', e: true},
      {controllerName: 'addServiceController', state: 'INSTALLED', e: true},
      {controllerName: 'installerController', state: 'STARTED', e: false},
      {controllerName: 'installerController', state: 'START FAILED', e: false},
      {controllerName: 'installerController', state: 'INSTALL FAILED', e: true},
      {controllerName: 'installerController', state: 'INSTALLED', e: true},
      {controllerName: 'installerController', state: 'PENDING', e: true}
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({
        content: {
          controllerName: test.controllerName,
          cluster: {
            status: test.state
          }
        }
      });
      it('controllerName is ' + test.controllerName + '; cluster status is ' + test.state + '; isSubmitDisabled should be ' + test.e, function () {
        expect(controller.get('isSubmitDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#status', function () {
    var tests = Em.A([
      {
        hosts: [
          {status: 'failed'},
          {status: 'success'}
        ],
        isStepFailed: false,
        progress: '100',
        m: 'One host is failed',
        e: 'failed'
      },
      {
        hosts: [
          {status: 'warning'},
          {status: 'success'}
        ],
        m: 'One host is failed and step is not failed',
        isStepFailed: false,
        progress: '100',
        e: 'warning'
      },
      {
        hosts: [
          {status: 'warning'},
          {status: 'success'}
        ],
        m: 'One host is failed and step is failed',
        isStepFailed: true,
        progress: '100',
        e: 'failed'
      },
      {
        hosts: [
          {status: 'success'},
          {status: 'success'}
        ],
        m: 'All hosts are success and progress is 100',
        isStepFailed: false,
        progress: '100',
        e: 'success'
      },
      {
        hosts: [
          {status: 'success'},
          {status: 'success'}
        ],
        m: 'All hosts are success and progress is 50',
        isStepFailed: false,
        progress: '50',
        e: 'info'
      }
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({hosts: test.hosts, isStepFailed: function () {
        return test.isStepFailed
      }, progress: test.progress});
      controller.updateStatus();
      it(test.m, function () {
        expect(controller.get('status')).to.equal(test.e);
      });
    });
  });

  describe('#showRetry', function () {
    it('cluster status is not INSTALL FAILED', function () {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: 'INSTALLED'}}});
      expect(controller.get('showRetry')).to.equal(false);
    });
    it('cluster status is INSTALL FAILED', function () {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: 'INSTALL FAILED'}}});
      expect(controller.get('showRetry')).to.equal(true);
    });
  });

  describe('#resetHostsForRetry', function () {
    var hosts = {'host1': Em.Object.create({status: 'failed', message: 'Failed'}), 'host2': Em.Object.create({status: 'success', message: 'Success'})};
    var controller = App.WizardStep9Controller.create({content: {hosts: hosts}});
    it('All should have status "pending" and message "Waiting"', function () {
      controller.resetHostsForRetry();
      for (var name in hosts) {
        if (hosts.hasOwnProperty(name)) {
          expect(controller.get('content.hosts')[name].get('status', 'pending')).to.equal('pending');
          expect(controller.get('content.hosts')[name].get('message', 'Waiting')).to.equal('Waiting');
        }
      }
    });
  });

  var hosts_for_load_and_render = {
    'host1': {
      message: 'message1',
      status: 'unknown',
      progress: '1',
      logTasks: [
        {},
        {}
      ],
      bootStatus: 'REGISTERED'
    },
    'host2': {
      message: '',
      status: 'failed',
      progress: '1',
      logTasks: [
        {},
        {}
      ],
      bootStatus: ''
    },
    'host3': {
      message: '',
      status: 'waiting',
      progress: null,
      logTasks: [
        {},
        {}
      ],
      bootStatus: ''
    },
    'host4': {
      message: 'message4',
      status: null,
      progress: '10',
      logTasks: [
        {}
      ],
      bootStatus: 'REGISTERED'
    }
  };

  describe('#loadHosts', function () {
    var controller = App.WizardStep9Controller.create({content: {hosts: hosts_for_load_and_render}});
    controller.loadHosts();
    var loaded_hosts = controller.get('hosts');
    it('Only REGISTERED hosts', function () {
      expect(loaded_hosts.length).to.equal(2);
    });
    it('All hosts have progress 0', function () {
      expect(loaded_hosts.everyProperty('progress', 0)).to.equal(true);
    });
    it('All hosts have progress 0', function () {
      expect(loaded_hosts.everyProperty('progress', 0)).to.equal(true);
    });
    it('All host don\'t have logTasks', function () {
      expect(loaded_hosts.everyProperty('logTasks.length', 0)).to.equal(true);
    });
  });

  describe('#hostHasClientsOnly', function () {
    var tests = Em.A([
      {
        hosts: [
          Em.Object.create({
            hostName: 'host1',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}},
              {Tasks: {role: 'DATANODE'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'old_status', progress: '10'}
          }),
          Em.Object.create({
            hostName: 'host2',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'success', progress: '100'}
          })
        ],
        jsonError: false
      },
      {
        hosts: [
          Em.Object.create({
            hostName: 'host1',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}},
              {Tasks: {role: 'DATANODE'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'success', progress: '100'}
          }),
          Em.Object.create({
            hostName: 'host2',
            logTasks: [
              {Tasks: {role: 'HDFS_CLIENT'}}
            ],
            status: 'old_status',
            progress: '10',
            e: {status: 'success', progress: '100'}
          })
        ],
        jsonError: true
      }
    ]);
    tests.forEach(function (test) {
      it('', function () {
        var controller = App.WizardStep9Controller.create({hosts: test.hosts});
        controller.hostHasClientsOnly(test.jsonError);
        test.hosts.forEach(function (host) {
          expect(controller.get('hosts').findProperty('hostName', host.hostName).get('status')).to.equal(host.e.status);
          expect(controller.get('hosts').findProperty('hostName', host.hostName).get('progress')).to.equal(host.e.progress);
        });
      });
    });
  });

  describe('#onSuccessPerHost', function () {
    var tests = Em.A([
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'pending'}),
        actions: [],
        e: {status: 'success'},
        m: 'No tasks for host'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'success'},
        m: 'All Tasks COMPLETED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        m: 'All Tasks COMPLETED and cluster status FAILED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        m: 'Not all Tasks COMPLETED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        m: 'Not all Tasks COMPLETED and cluster status FAILED'
      }
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: test.cluster.status}}});
      controller.onSuccessPerHost(test.actions, test.host);
      it(test.m, function () {
        expect(test.host.status).to.equal(test.e.status);
      });
    });
  });

  describe('#onErrorPerHost', function () {
    var tests = Em.A([
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'pending'}),
        actions: [],
        e: {status: 'pending'},
        isMasterFailed: false,
        m: 'No tasks for host'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task FAILED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'ABORTED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task ABORTED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'TIMEDOUT'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task TIMEDOUT and cluster status INSTALLED'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'FAILED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'failed'},
        isMasterFailed: true,
        m: 'One Task FAILED and cluster status PENDING isMasterFailed true'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({status: 'info'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {status: 'info'},
        isMasterFailed: false,
        m: 'One Task FAILED and cluster status PENDING isMasterFailed false'
      }
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: test.cluster.status}}, isMasterFailed: function () {
        return test.isMasterFailed;
      }});
      controller.onErrorPerHost(test.actions, test.host);
      it(test.m, function () {
        expect(test.host.status).to.equal(test.e.status);
      });
    });
  });

  describe('#isMasterFailed', function () {
    var tests = Em.A([
      {
        actions: [
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'DATANODE'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'GANGLIA_MONITOR'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'SUPERVISOR'}}
        ],
        e: false,
        m: 'No one Master is failed'
      },
      {
        actions: [
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'NAMENODE'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'GANGLIA_MONITOR'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'SUPERVISOR'}}
        ],
        e: true,
        m: 'One Master is failed'
      },
      {
        actions: [
          {Tasks: {command: 'PENDING', status: 'FAILED', role: 'NAMENODE'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'GANGLIA_MONITOR'}},
          {Tasks: {command: 'INSTALL', status: 'FAILED', role: 'SUPERVISOR'}}
        ],
        e: false,
        m: 'one Master is failed but command is not install'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var controller = App.WizardStep9Controller.create();
        expect(controller.isMasterFailed(test.actions)).to.equal(test.e);
      });
    });
  });

  describe('#onInProgressPerHost', function () {
    var tests = Em.A([
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: true},
        m: 'All Tasks COMPLETED'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'IN_PROGRESS'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: false},
        m: 'One Task IN_PROGRESS'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'QUEUED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: false},
        m: 'One Task QUEUED'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [
          {Tasks: {status: 'PENDING'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: {message: 'default_message', b: false},
        m: 'One Task PENDING'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var controller = App.WizardStep9Controller.create();
        controller.onInProgressPerHost(test.actions, test.host);
        expect(test.host.message == test.e.message).to.equal(test.e.b);
      });
    });
  });

  describe('#progressPerHost', function () {
    var tests = Em.A([
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({progress: 0}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'QUEUED'}},
          {Tasks: {status: 'QUEUED'}},
          {Tasks: {status: 'IN_PROGRESS'}}
        ],
        e: {ret: 17, host: '17'},
        m: 'All types of status available. cluster status PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({progress: 0}),
        actions: [],
        e: {ret: 33, host: '33'},
        m: 'No tasks available. cluster status PENDING'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: [],
        e: {ret: 100, host: '100'},
        m: 'No tasks available. cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'QUEUED'}},
          {Tasks: {status: 'QUEUED'}},
          {Tasks: {status: 'IN_PROGRESS'}}
        ],
        e: {ret: 68, host: '68'},
        m: 'All types of status available. cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({progress: 0}),
        actions: [],
        e: {ret: 100, host: '100'},
        m: 'Cluster status is not PENDING or INSTALLED'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var controller = App.WizardStep9Controller.create({content: {cluster: {status: test.cluster.status}}});
        var progress = controller.progressPerHost(test.actions, test.host);
        expect(progress).to.equal(test.e.ret);
        expect(test.host.progress).to.equal(test.e.host);
      });
    });
  });

  describe('#clearStep', function () {
    var controller = App.WizardStep9Controller.create({hosts: [
      {},
      {},
      {}
    ]});
    it('All to default values', function () {
      controller.clearStep();
      expect(controller.get('hosts.length')).to.equal(0);
      expect(controller.get('status')).to.equal('info');
      expect(controller.get('progress')).to.equal('0');
      expect(controller.get('numPolls')).to.equal(1);
    });
  });

  describe('#replacePolledData', function () {
    var controller = App.WizardStep9Controller.create({polledData: [
      {},
      {},
      {}
    ]});
    var newPolledData = [
      {}
    ];
    controller.replacePolledData(newPolledData);
    it('replacing polled data', function () {
      expect(controller.get('polledData.length')).to.equal(newPolledData.length);
    });
  });

  describe('#isSuccess', function () {
    var tests = Em.A([
      {
        polledData: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'COMPLETED'}}
        ],
        e: true,
        m: 'All tasks are COMPLETED'
      },
      {
        polledData: [
          {Tasks: {status: 'COMPLETED'}},
          {Tasks: {status: 'FAILED'}}
        ],
        e: false,
        m: 'Not all tasks are COMPLETED'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var controller = App.WizardStep9Controller.create();
        expect(controller.isSuccess(test.polledData)).to.equal(test.e);
      });
    });
  });

  describe('#isStepFailed', function () {
    var tests = Em.A([
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'PENDING'}}
        ],
        e: true,
        m: 'GANGLIA_MONITOR 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'GANGLIA_MONITOR', status: 'PENDING'}}
        ],
        e: false,
        m: 'GANGLIA_MONITOR 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'PENDING'}}
        ],
        e: true,
        m: 'HBASE_REGIONSERVER 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'HBASE_REGIONSERVER', status: 'PENDING'}}
        ],
        e: false,
        m: 'HBASE_REGIONSERVER 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'PENDING'}}
        ],
        e: true,
        m: 'TASKTRACKER 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'TASKTRACKER', status: 'PENDING'}}
        ],
        e: false,
        m: 'TASKTRACKER 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'FAILED'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: true,
        m: 'DATANODE 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: false,
        m: 'DATANODE 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'NAMENODE', status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: true,
        m: 'NAMENODE failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL', role: 'NAMENODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}},
          {Tasks: {command: 'INSTALL', role: 'DATANODE', status: 'PENDING'}}
        ],
        e: false,
        m: 'Nothing failed failed'
      }
    ]);
    tests.forEach(function (test) {
      var controller = App.WizardStep9Controller.create({polledData: test.polledData});
      it(test.m, function () {
        expect(controller.isStepFailed()).to.equal(test.e);
      });
    });
  });

  describe('#finishState', function () {
    var statuses = Em.A(['INSTALL FAILED', 'START FAILED', 'STARTED']);
    it('Installer is finished', function () {
      statuses.forEach(function (status) {
        var controller = App.WizardStep9Controller.create({content: {cluster: {status: status}}});
        var result = controller.finishState();
        expect(result).to.equal(true);
      });
    });
    it('Unknown cluster status ', function () {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: 'FAKE_STATUS'}}});
      var result = controller.finishState();
      expect(result).to.equal(false);
    });
    it('for INSTALLED status should call isServicesStarted', function () {
      c.set('content', {cluster: {status: 'INSTALLED'}});
      var polledData = {'{}': {}};
      sinon.stub(c, 'isServicesStarted', Em.K);
      c.finishState(polledData);
      expect(c.isServicesStarted.calledWith(polledData)).to.equal(true);
      c.isServicesStarted.restore();
    });
    it('for PENDING status should call isServicesInstalled', function () {
      c.set('content', {cluster: {status: 'PENDING'}});
      var polledData = {'{}': {}};
      sinon.stub(c, 'isServicesInstalled', Em.K);
      c.finishState(polledData);
      expect(c.isServicesInstalled.calledWith(polledData)).to.equal(true);
      c.isServicesInstalled.restore();
    });
  });

  describe('#setLogTasksStatePerHost', function () {
    var tests = Em.A([
      {
        tasksPerHost: [
          {Tasks: {id: 1, status: 'COMPLETED'}},
          {Tasks: {id: 2, status: 'COMPLETED'}}
        ],
        tasks: [],
        e: {m: 'COMPLETED', l: 2},
        m: 'host didn\'t have tasks and got 2 new'
      },
      {
        tasksPerHost: [
          {Tasks: {id: 1, status: 'COMPLETED'}},
          {Tasks: {id: 2, status: 'COMPLETED'}}
        ],
        tasks: [
          {Tasks: {id: 1, status: 'IN_PROGRESS'}},
          {Tasks: {id: 2, status: 'IN_PROGRESS'}}
        ],
        e: {m: 'COMPLETED', l: 2},
        m: 'host had 2 tasks and got both updated'
      },
      {
        tasksPerHost: [],
        tasks: [
          {Tasks: {id: 1, status: 'IN_PROGRESS'}},
          {Tasks: {id: 2, status: 'IN_PROGRESS'}}
        ],
        e: {m: 'IN_PROGRESS', l: 2},
        m: 'host had 2 tasks and didn\'t get updates'
      },
      {
        tasksPerHost: [
          {Tasks: {id: 1, status: 'COMPLETED'}},
          {Tasks: {id: 2, status: 'COMPLETED'}},
          {Tasks: {id: 3, status: 'COMPLETED'}}
        ],
        tasks: [
          {Tasks: {id: 1, status: 'IN_PROGRESS'}},
          {Tasks: {id: 2, status: 'IN_PROGRESS'}}
        ],
        e: {m: 'COMPLETED', l: 3},
        m: 'host had 2 tasks and got both updated and 1 new'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var controller = App.WizardStep9Controller.create({hosts: [Em.Object.create({logTasks: test.tasks})]});
        controller.setLogTasksStatePerHost(test.tasksPerHost, controller.get('hosts')[0]);
        expect(controller.get('hosts')[0].get('logTasks').everyProperty('Tasks.status', test.e.m)).to.equal(true);
        expect(controller.get('hosts')[0].get('logTasks.length')).to.equal(test.e.l);
      });
    });
  });

  describe('#parseHostInfo', function () {

    var tests = Em.A([
      {
        cluster: {status: 'PENDING'},
        hosts: Em.A([
          Em.Object.create({name: 'host1', status: '', message: '', progress: '', logTasks: []}),
          Em.Object.create({name: 'host2', status: '', message: '', progress: '', logTasks: []})
        ]),
        polledData: {
          tasks: [
            {Tasks: {host_name: 'host2', status: 'COMPLETED'}},
            {Tasks: {host_name: 'host2', status: 'COMPLETED'}}
          ]
        },
        e: {
          hosts: {
            host1: {progress: '33'},
            host2: {progress: '33'}
          },
          progress: '33'
        },
        m: 'Two hosts. One host without tasks. Second host has all tasks COMPLETED. Cluster status is PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        hosts: Em.A([
          Em.Object.create({name: 'host1', status: '', message: '', progress: '', logTasks: []}),
          Em.Object.create({name: 'host2', status: '', message: '', progress: '', logTasks: []})
        ]),
        polledData: {
          tasks: [
            {Tasks: {host_name: 'host1', status: 'IN_PROGRESS'}},
            {Tasks: {host_name: 'host2', status: 'IN_PROGRESS'}}
          ]
        },
        e: {hosts: {host1: {progress: '12'}, host2: {progress: '12'}}, progress: '12'},
        m: 'Two hosts. Each host has one task IN_PROGRESS. Cluster status is PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        hosts: Em.A([
          Em.Object.create({name: 'host1', status: '', message: '', progress: '', logTasks: []}),
          Em.Object.create({name: 'host2', status: '', message: '', progress: '', logTasks: []})
        ]),
        polledData: {
          tasks: [
            {Tasks: {host_name: 'host1', status: 'QUEUED'}},
            {Tasks: {host_name: 'host2', status: 'QUEUED'}}
          ]
        },
        e: {
          hosts: {
            host1: {progress: '3'},
            host2: {progress: '3'}
          },
          progress: '3'
        },
        m: 'Two hosts. Each host has one task QUEUED. Cluster status is PENDING'
      },
      {
        cluster: {status: 'INSTALLED'},
        hosts: Em.A([
          Em.Object.create({name: 'host1', status: '', message: '', progress: '', logTasks: []}),
          Em.Object.create({name: 'host2', status: '', message: '', progress: '', logTasks: []})
        ]),
        polledData: {
          tasks: [
            {Tasks: {host_name: 'host2', status: 'COMPLETED'}},
            {Tasks: {host_name: 'host2', status: 'COMPLETED'}}
          ]
        },
        e: {
          hosts: {
            host1: {progress: '100'},
            host2: {progress: '100'}
          },
          progress: '100'
        },
        m: 'Two hosts. One host without tasks. Second host has all tasks COMPLETED. Cluster status is INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        hosts: Em.A([
          Em.Object.create({name: 'host1', status: '', message: '', progress: '', logTasks: []}),
          Em.Object.create({name: 'host2', status: '', message: '', progress: '', logTasks: []})
        ]),
        polledData: {
          tasks: [
            {Tasks: {host_name: 'host1', status: 'IN_PROGRESS'}},
            {Tasks: {host_name: 'host2', status: 'IN_PROGRESS'}}
          ]
        },
        e: {
          hosts: {
            host1: {progress: '58'},
            host2: {progress: '58'}
          },
          progress: '58'
        },
        m: 'Two hosts. Each host has one task IN_PROGRESS. Cluster status is INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        hosts: Em.A([
          Em.Object.create({name: 'host1', status: '', message: '', progress: '', logTasks: []}),
          Em.Object.create({name: 'host2', status: '', message: '', progress: '', logTasks: []})
        ]),
        polledData: {
          tasks: [
            {Tasks: {host_name: 'host1', status: 'QUEUED'}},
            {Tasks: {host_name: 'host2', status: 'QUEUED'}}
          ]
        },
        e: {
          hosts: {
            host1: {progress: '40'},
            host2: {progress: '40'}
          },
          progress: '40'
        },
        m: 'Two hosts. Each host has one task QUEUED. Cluster status is INSTALLED'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var controller = App.WizardStep9Controller.create({hosts: test.hosts, content: {cluster: {status: test.cluster.status}}, finishState: function () {
          return false;
        }});
        var logTasksChangesCounter = controller.get('logTasksChangesCounter');
        controller.parseHostInfo(test.polledData);
        expect(controller.get('logTasksChangesCounter')).to.equal(logTasksChangesCounter + 1);
        for (var name in test.e.hosts) {
          if (test.e.hosts.hasOwnProperty(name)) {
            expect(controller.get('hosts').findProperty('name', name).get('progress')).to.equal(test.e.hosts[name].progress);
          }
        }
        expect(controller.get('progress')).to.equal(test.e.progress);
      });
    });
    it('shouldn\'t do nothing if polledData.Requests.id != requestId', function () {
      c.set('content', {cluster: {requestId: 1}});
      var polledData = {Requests: {id: 2}, tasks: []};
      sinon.spy(c, 'finishState');
      expect(c.parseHostInfo(polledData)).to.equal(false);
      expect(c.finishState.called).to.equal(false);
      c.finishState.restore();
    });
  });

  describe('#isAllComponentsInstalledSuccessCallback', function () {

    describe('', function () {
      var hosts = Em.A([
        Em.Object.create({name: 'host1', status: 'failed', expectedStatus: 'heartbeat_lost'}),
        Em.Object.create({name: 'host2', status: 'info', expectedStatus: 'heartbeat_lost'}),
        Em.Object.create({name: 'host3', status: 'warning', expectedStatus: 'warning'}),
        Em.Object.create({name: 'host4', status: 'info', expectedStatus: 'info'})
      ]);
      var heartbeatLostData = {
        "items": [
          {
            "Hosts": {
              "cluster_name": "c1",
              "host_name": "host1",
              "host_state": "HEARTBEAT_LOST"
            },
            "host_components": [
              {
                "HostRoles": {
                  "cluster_name": "c1",
                  "component_name": "NAMENODE",
                  "host_name": "host1",
                  "state": "INSTALL_FAILED"
                }
              }
            ]
          },
          {
            "Hosts": {
              "cluster_name": "c1",
              "host_name": "host2",
              "host_state": "HEARTBEAT_LOST"
            },
            "host_components": [
              {
                "HostRoles": {
                  "cluster_name": "c1",
                  "component_name": "ZOOKEEPER_SERVER",
                  "host_name": "host2",
                  "state": "UNKNOWN"
                }
              }
            ]
          },
          {
            "Hosts": {
              "cluster_name": "c1",
              "host_name": "host3",
              "host_state": "HEALTHY"
            },
            "host_components": [
              {
                "HostRoles": {
                  "cluster_name": "c1",
                  "component_name": "DATANODE",
                  "host_name": "host3",
                  "state": "INSTALL_FAILED"
                }
              }
            ]
          },
          {
            "Hosts": {
              "cluster_name": "c1",
              "host_name": "host4",
              "host_state": "HEALTHY"
            },
            "host_components": [
              {
                "HostRoles": {
                  "cluster_name": "c1",
                  "component_name": "PIG",
                  "host_name": "host4",
                  "state": "INSTALLED"
                }
              },
              {
                "HostRoles": {
                  "cluster_name": "c1",
                  "component_name": "DATANODE",
                  "host_name": "host3",
                  "state": "INSTALLED"
                }
              }
            ]
          }
        ]
      };

      var controller = App.WizardStep9Controller.create({hosts: hosts, content: {controllerName: 'installerController'}});

      App.testMode = true;
      // Action
      controller.isAllComponentsInstalledSuccessCallback(heartbeatLostData);


      // Validation  for the status of all hosts.
      controller.get('hosts').forEach(function (test) {
        var status = heartbeatLostData.items.findProperty('Hosts.host_name', test.get('name')).Hosts.host_state;
        it('Host "' + test.get('name') + '"' + ' with status "' + status + '" ', function () {
          expect(test.get('status')).to.equal(test.get('expectedStatus'));
        });
      });

    });

    describe('', function () {
      var noHeartbeatLostData = {
        "items": [
          {
            "Hosts": {
              "cluster_name": "c1",
              "host_name": "host1",
              "host_state": "HEALTHY"
            },
            "host_components": [
              {
                "HostRoles": {
                  "cluster_name": "c1",
                  "component_name": "NAMENODE",
                  "host_name": "host1",
                  "state": "INSTALL_FAILED"
                }
              }
            ]
          }
        ]
      };

      var hosts = Em.A([Em.Object.create({name: 'host1', status: 'failed'})]);
      // When there is no heartbeat lost for any host and cluster failed install task, Refreshing the page should not launch start all services request.
      // Below transitions are possibilities in this function
      // PENDING -> INSTALL or PENDING. This transition happens when install all services request is completed successfully.
      // INSTALL FAILED -> INSTALL FAILED. No transition should happen when install all services request fails and then user hits refresh
      // Cluster is not expected to enter this function in other states: INSTALLED, START FAILED, STARTED

      var statuses = Em.A(['INSTALL FAILED', 'INSTALLED', 'START FAILED', 'STARTED']);  // Cluster in any of this states should have no effect on the state from this function
      statuses.forEach(function (priorStatus) {
        var controller = App.WizardStep9Controller.create({hosts: hosts, content: {controllerName: 'installerController', cluster: {status: priorStatus}}, togglePreviousSteps: function () {
        }});
        // Action
        controller.isAllComponentsInstalledSuccessCallback(noHeartbeatLostData);
        // Validation for the cluster state.
        var actualStatus = controller.get('content.cluster.status');
        it('Cluster state before entering the function "' + priorStatus + '"', function () {
          expect(actualStatus).to.equal(priorStatus);
        });
      });
    });
  });

  // isServicesInstalled is called after every poll for "Install All Services" request.
  // This function should result into a call to "Start All Services" request only if install request completed successfully.
  describe('#isServicesInstalled', function () {

    var hostStateJsonData = {
      "items": [
        {
          "Hosts": {
            "cluster_name": "c1",
            "host_name": "ambari-1.c.apache.internal",
            "host_state": "HEALTHY"
          },
          "host_components": [
            {
              "HostRoles": {
                "cluster_name": "c1",
                "component_name": "GANGLIA_MONITOR",
                "host_name": "ambari-1.c.apache.internal",
                "state": "STARTED"
              }
            }
          ]
        }
      ]
    };
    var hosts = Em.A([Em.Object.create({name: 'host1', progress: '33', status: 'info'}),
      Em.Object.create({name: 'host2', progress: '33', status: 'info'})]);
    // polledData has all hosts with status completed to trigger transition from install->start request.
    var polledData = Em.A([Em.Object.create({Tasks: {name: 'host1', status: 'COMPLETED'}}),
      Em.Object.create({Tasks: {name: 'host2', status: 'COMPLETED'}})]);
    var controller = App.WizardStep9Controller.create({hosts: hosts, content: {controllerName: 'installerController',
      cluster: {status: 'PENDING', name: 'c1'}}, launchStartServices: function () {
      return true;
    }});
    var tests = Em.A([
      // controller has "status" value as "info" initially. If no errors are encountered then wizard stages
      // transition info->success, on error info->error, on warning info->warning
      {status: 'info', e: {startServicesCalled: true}, m: 'If no failed tasks then start services request should be called'},
      {status: 'failed', e: {startServicesCalled: false}, m: 'If install request has failed tasks then start services call should not be called'}
    ]);

    beforeEach(function () {
      App.testMode = true;
      sinon.spy(controller, 'launchStartServices');
      sinon.stub($, 'ajax').yieldsTo('success', hostStateJsonData);
    });

    afterEach(function () {
      App.testMode = false;
      controller.launchStartServices.restore();
      $.ajax.restore();
    });

    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('status', test.status);
        //Action
        controller.isServicesInstalled(polledData);
        //Validation
        expect(controller.launchStartServices.called).to.equal(test.e.startServicesCalled);
      });
    });
  });

  // On completion of Start all services error callback function,
  // Cluster Status should be INSTALL FAILED
  // All progress bar on the screen should be finished (100%) with blue color.
  // Retry button should be enabled, next button should be disabled

  describe('#launchStartServicesErrorCallback', function () {
    App.testMode = true;
    // override the actual function
    App.popup = {
      setErrorPopup: function () {
        return true;
      }
    };
    var hosts = Em.A([Em.Object.create({name: 'host1', progress: '33', status: 'info'}), Em.Object.create({name: 'host2', progress: '33', status: 'info'})]);
    var controller = App.WizardStep9Controller.create({hosts: hosts, content: {controllerName: 'installerController', cluster: {status: 'PENDING', name: 'c1'}}, togglePreviousSteps: function () {
    }});

    //Action
    controller.launchStartServicesErrorCallback({status: 500, statusTesxt: 'Server Error'}, {}, '', {});
    it('Cluster Status should be INSTALL FAILED', function () {
      expect(controller.get('content.cluster.status')).to.equal('INSTALL FAILED');
    });

    it('Main progress bar on the screen should be finished (100%) with red color', function () {
      expect(controller.get('progress')).to.equal('100');
      expect(controller.get('status')).to.equal('failed');
    });

    it('All Host progress bars on the screen should be finished (100%) with blue color', function () {
      controller.get('hosts').forEach(function (host) {
        expect(host.get('progress')).to.equal('100');
        expect(host.get('status')).to.equal('info');
      });
    });

    it('Next button should be disabled', function () {
      expect(controller.get('isSubmitDisabled')).to.equal(true);
    });

    it('Retry button should be visible', function () {
      expect(controller.get('showRetry')).to.equal(true);
    })

  });

  describe('#submit', function () {
    it('should call App.router.send', function () {
      sinon.stub(App.router, 'send', Em.K);
      c.submit();
      expect(App.router.send.calledWith('next')).to.equal(true);
      App.router.send.restore();
    });
  });

  describe('#back', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      App.router.send.restore();
    });
    it('should call App.router.send', function () {
      c.reopen({isSubmitDisabled: false});
      c.back();
      expect(App.router.send.calledWith('back')).to.equal(true);
    });
    it('shouldn\'t call App.router.send', function () {
      c.reopen({isSubmitDisabled: true});
      c.back();
      expect(App.router.send.called).to.equal(false);
    });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      sinon.stub(c, 'clearStep', Em.K);
      sinon.stub(c, 'loadHosts', Em.K);
    });
    afterEach(function () {
      c.clearStep.restore();
      c.loadHosts.restore();
    });
    it('should call clearStep', function () {
      c.loadStep();
      expect(c.clearStep.calledOnce).to.equal(true);
    });
    it('should call loadHosts', function () {
      c.loadStep();
      expect(c.loadHosts.calledOnce).to.equal(true);
    });
  });

  describe('#startPolling', function () {
    beforeEach(function () {
      sinon.stub(c, 'getLogsByRequestErrorCallback', Em.K);
    });
    afterEach(function () {
      c.getLogsByRequestErrorCallback.restore();
    });
    it('should set isSubmitDisabled to true', function () {
      c.set('isSubmitDisabled', false);
      c.startPolling();
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });
    it('should call doPolling', function () {
      sinon.stub(c, 'doPolling', Em.K);
      c.startPolling();
      expect(c.doPolling.calledOnce).to.equal(true);
      c.doPolling.restore();
    });
  });

  describe('#loadLogData', function () {
    beforeEach(function () {
      sinon.stub(c, 'getLogsByRequest', Em.K);
      c.set('wizardController', Em.Object.create({
        cluster: {oldRequestsId: []},
        getDBProperty: function (name) {
          return this.get(name);
        }
      }));
    });
    afterEach(function () {
      c.getLogsByRequest.restore();
    });
    it('shouldn\'t call getLogsByRequest if no requestIds', function () {
      c.set('wizardController.cluster.oldRequestsId', []);
      c.loadLogData();
      expect(c.getLogsByRequest.called).to.equal(false);
    });
    it('should call getLogsByRequest 3 times', function () {
      c.set('wizardController.cluster.oldRequestsId', [1, 2, 3]);
      c.loadLogData();
      expect(c.getLogsByRequest.calledThrice).to.equal(true);
    });
    it('should set POLL_INTERVAL to 1 if testMode enabled', function () {
      App.set('testMode', true);
      c.set('wizardController.cluster.oldRequestsId', [1, 2, 3]);
      c.loadLogData();
      expect(c.get('POLL_INTERVAL')).to.equal(1);
      App.set('testMode', false);
    });
  });

  describe('#loadCurrentTaskLog', function () {
    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it('shouldn\'t call App.ajax.send if no currentOpenTaskId', function () {
      c.set('currentOpenTaskId', null);
      c.loadCurrentTaskLog();
      expect(App.ajax.send.called).to.equal(false);
    });
    it('should call App.ajax.send with provided data', function () {
      sinon.stub(c, 'togglePreviousSteps', Em.K);
      c.set('currentOpenTaskId', 1);
      c.set('currentOpenTaskRequestId', 2);
      c.set('content', {cluster: {name: 3}});
      c.loadCurrentTaskLog();
      expect(App.ajax.send.args[0][0].data).to.eql({taskId: 1, requestId: 2, clusterName: 3, sync: true});
      c.togglePreviousSteps.restore();
    });
  });

  describe('#loadCurrentTaskLogSuccessCallback', function () {
    it('should increment logTasksChangesCounter', function () {
      c.set('logTasksChangesCounter', 0);
      c.loadCurrentTaskLogSuccessCallback();
      expect(c.get('logTasksChangesCounter')).to.equal(1);
    });
    it('should update stdout, stderr', function () {
      c.set('currentOpenTaskId', 1);
      c.reopen({
        hosts: [
          Em.Object.create({
            name: 'h1',
            logTasks: [
              {Tasks: {id: 1, stdout: '', stderr: ''}}
            ]
          })
        ]
      });
      var data = {Tasks: {host_name: 'h1', id: 1, stderr: 'stderr', stdout: 'stdout'}};
      c.loadCurrentTaskLogSuccessCallback(data);
      var t = c.get('hosts')[0].logTasks[0].Tasks;
      expect(t.stdout).to.equal('stdout');
      expect(t.stderr).to.equal('stderr');
    });
    it('shouldn\'t update stdout, stderr', function () {
      c.set('currentOpenTaskId', 1);
      c.reopen({
        hosts: [
          Em.Object.create({
            name: 'h1',
            logTasks: [
              {Tasks: {id: 2, stdout: '', stderr: ''}}
            ]
          })
        ]
      });
      var data = {Tasks: {host_name: 'h1', id: 1, stderr: 'stderr', stdout: 'stdout'}};
      c.loadCurrentTaskLogSuccessCallback(data);
      var t = c.get('hosts')[0].logTasks[0].Tasks;
      expect(t.stdout).to.equal('');
      expect(t.stderr).to.equal('');
    });
    it('shouldn\'t update stdout, stderr (2)', function () {
      c.set('currentOpenTaskId', 1);
      c.reopen({
        hosts: [
          Em.Object.create({
            name: 'h2',
            logTasks: [
              {Tasks: {id: 1, stdout: '', stderr: ''}}
            ]
          })
        ]
      });
      var data = {Tasks: {host_name: 'h1', id: 1, stderr: 'stderr', stdout: 'stdout'}};
      c.loadCurrentTaskLogSuccessCallback(data);
      var t = c.get('hosts')[0].logTasks[0].Tasks;
      expect(t.stdout).to.equal('');
      expect(t.stderr).to.equal('');
    });
  });

  describe('#loadCurrentTaskLogErrorCallback', function () {
    it('should set currentOpenTaskId to 0', function () {
      c.set('currentOpenTaskId', 123);
      c.loadCurrentTaskLogErrorCallback();
      expect(c.get('currentOpenTaskId')).to.equal(0);
    });
  });

  describe('#getLogsByRequest', function () {
    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
      c.togglePreviousSteps.restore();
    });
    it('should call App.ajax.send with provided data', function () {
      var polling = 1;
      var requestId = 2;
      c.set('content', {cluster: {name: 3}});
      c.set('numPolls', 4);
      c.getLogsByRequest(polling, requestId);
      expect(App.ajax.send.args[0][0].data).to.eql({polling: polling, requestId: requestId, cluster: 3, numPolls: 4});
    });
  });

  describe('#doPolling', function () {
    beforeEach(function () {
      sinon.stub(c, 'getLogsByRequest', Em.K);
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      c.getLogsByRequest.restore();
      c.togglePreviousSteps.restore();
    });
    it('should increment numPolls if testMode', function () {
      App.set('testMode', true);
      c.set('numPolls', 0);
      c.doPolling();
      expect(c.get('numPolls')).to.equal(1);
      App.set('testMode', false);
    });
    it('should call getLogsByRequest', function () {
      c.set('content', {cluster: {requestId: 1}});
      c.doPolling();
      expect(c.getLogsByRequest.calledWith(true, 1)).to.equal(true);
    });
  });

  describe('#isAllComponentsInstalled', function () {
    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
      sinon.stub(c, 'togglePreviousSteps', Em.K);
      sinon.stub(c, 'saveClusterStatus', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
      c.togglePreviousSteps.restore();
      c.saveClusterStatus.restore();
    });
    it('shouldn\'t call App.ajax.send', function () {
      c.set('content', {controllerName: 'addServiceController'});
      c.isAllComponentsInstalled();
      expect(App.ajax.send.called).to.equal(false);
    });
    it('shouldn\'t call App.ajax.send (2)', function () {
      c.set('content', {controllerName: 'addHostController'});
      c.isAllComponentsInstalled();
      expect(App.ajax.send.called).to.equal(false);
    });
    it('should call App.ajax.send', function () {
      c.set('content', {cluster: {name: 'n'}, controllerName: 'installerController'});
      c.isAllComponentsInstalled();
      expect(App.ajax.send.args[0][0].data).to.eql({cluster: 'n'});
    });
  });

  describe('#isAllComponentsInstalledErrorCallback', function () {
    beforeEach(function () {
      sinon.stub(c, 'saveClusterStatus', Em.K);
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      c.saveClusterStatus.restore();
      c.togglePreviousSteps.restore();
    });
    it('should call saveClusterStatus', function () {
      c.isAllComponentsInstalledErrorCallback({});
      expect(c.saveClusterStatus.calledOnce).to.equal(true);
    });
  });

  describe('#saveClusterStatus', function () {
    beforeEach(function () {
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      c.togglePreviousSteps.restore();
    });
    it('in testMode should set content.cluster', function () {
      var d = {n: 'n'};
      c.set('content', {cluster: ''});
      App.set('testMode', true);
      c.saveClusterStatus(d);
      expect(c.get('content.cluster')).to.eql(d);
      App.set('testMode', false);
    });
    it('if testMode is false should use content.controller', function () {
      var d = {n: 'n'},
        obj = Em.Object.create({
          saveClusterStatus: Em.K
        });
      sinon.stub(App.router, 'get', function () {
        return obj;
      });
      sinon.spy(obj, 'saveClusterStatus');
      c.set('content', {cluster: ''});
      App.set('testMode', false);
      c.saveClusterStatus(d);
      expect(obj.saveClusterStatus.calledWith(d)).to.eql(true);
      App.set('testMode', true);
      obj.saveClusterStatus.restore();
      App.router.get.restore();
    });
  });

  describe('#saveInstalledHosts', function () {
    beforeEach(function () {
      sinon.stub(c, 'togglePreviousSteps', Em.K);
    });
    afterEach(function () {
      c.togglePreviousSteps.restore();
    });
    it('if testMode is false should use content.controller', function () {
      var d = {n: 'n'},
        obj = Em.Object.create({
          saveInstalledHosts: Em.K
        });
      sinon.stub(App.router, 'get', function () {
        return obj;
      });
      sinon.spy(obj, 'saveInstalledHosts');
      c.set('content', {cluster: ''});
      App.set('testMode', false);
      c.saveInstalledHosts(d);
      expect(obj.saveInstalledHosts.calledWith(d)).to.eql(true);
      App.set('testMode', true);
      obj.saveInstalledHosts.restore();
      App.router.get.restore();
    });
  });

  describe('#getComponentMessage', function () {
    var tests = Em.A([
      {
        clients: ['c1'],
        m: 'One client',
        e: 'c1'
      },
      {
        clients: ['c1', 'c2'],
        m: 'Two clients',
        e: 'c1 and c2'
      },
      {
        clients: ['c1', 'c2', 'c3'],
        m: 'Three clients',
        e: 'c1, c2 and c3'
      },
      {
        clients: ['c1', 'c2', 'c3', 'c4'],
        m: 'Four clients',
        e: 'c1, c2, c3 and c4'
      },
      {
        clients: ['c1', 'c2', 'c3', 'c4', 'c5'],
        m: 'Five clients',
        e: 'c1, c2, c3, c4 and c5'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        var label = c.getComponentMessage(test.clients);
        expect(label).to.equal(test.e);
      });
    });
  });

  describe('#togglePreviousSteps', function () {
    beforeEach(function () {
      sinon.stub(obj, 'setStepsEnable', Em.K);
      sinon.stub(obj, 'setLowerStepsDisable', Em.K);
      sinon.stub(App.router, 'get', function () {
        return obj;
      });
    });
    afterEach(function () {
      App.router.get.restore();
      obj.setStepsEnable.restore();
      obj.setLowerStepsDisable.restore();
    });
    it('shouldn\'t do nothing on testMode', function () {
      App.set('testMode', true);
      c.togglePreviousSteps();
      expect(App.router.get.called).to.equal(false);
      App.set('testMode', false);
    });
    Em.A([
        {
          status: 'INSTALL FAILED',
          controllerName: 'installerController',
          e: {
            setStepsEnable: true,
            setLowerStepsDisable: false
          }
        },
        {
          status: 'STARTED',
          controllerName: 'installerController',
          e: {
            setStepsEnable: false,
            setLowerStepsDisable: true
          }
        },
        {
          status: 'INSTALL FAILED',
          controllerName: 'addServiceController',
          e: {
            setStepsEnable: false,
            setLowerStepsDisable: true
          }
        },
        {
          status: 'STARTED',
          controllerName: 'addServiceController',
          e: {
            setStepsEnable: false,
            setLowerStepsDisable: true
          }
        }
      ]).forEach(function (test) {
        it(test.status + ' ' + test.controllerName, function () {
          App.set('testMode', false);
          c.reopen({content: {cluster: {status: test.status}, controllerName: test.controllerName}});
          c.togglePreviousSteps();
          expect(App.router.get.calledWith('installerController')).to.equal(true);
          if (test.e.setStepsEnable) {
            expect(obj.setStepsEnable.calledOnce).to.equal(true);
          }
          else {
            expect(obj.setStepsEnable.called).to.equal(false);
          }
          if (test.e.setLowerStepsDisable) {
            expect(obj.setLowerStepsDisable.calledWith(9)).to.equal(true);
          }
          else {
            expect(obj.setLowerStepsDisable.called).to.equal(false);
          }
        });
      });
  });

  describe('#navigateStep', function () {
    beforeEach(function () {
      sinon.stub(c, 'togglePreviousSteps', Em.K);
      sinon.stub(c, 'loadStep', Em.K);
      sinon.stub(c, 'loadLogData', Em.K);
      sinon.stub(c, 'startPolling', Em.K);
    });
    afterEach(function () {
      c.togglePreviousSteps.restore();
      c.loadStep.restore();
      c.loadLogData.restore();
      c.startPolling.restore();
    });
    it('should set custom data in testMode', function () {
      App.set('testMode', true);
      c.reopen({content: {cluster: {status: 'st', isCompleted: true, requestId: 0}}});
      c.navigateStep();
      expect(c.get('content.cluster.status')).to.equal('PENDING');
      expect(c.get('content.cluster.isCompleted')).to.equal(false);
      expect(c.get('content.cluster.requestId')).to.equal(1);
      App.set('testMode', false);
    });
    it('isCompleted = true, requestId = 1', function () {
      App.set('testMode', false);
      c.reopen({content: {cluster: {isCompleted: true, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(1)).to.equal(true);
      expect(c.get('progress')).to.equal('100');
    });
    it('isCompleted = false, requestId = 1, status = INSTALL FAILED', function () {
      App.set('testMode', false);
      c.reopen({content: {cluster: {status: 'INSTALL FAILED', isCompleted: false, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(1)).to.equal(true);
    });
    it('isCompleted = false, requestId = 1, status = START FAILED', function () {
      App.set('testMode', false);
      c.reopen({content: {cluster: {status: 'START FAILED', isCompleted: false, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(1)).to.equal(true);
    });
    it('isCompleted = false, requestId = 1, status = OTHER', function () {
      App.set('testMode', false);
      c.reopen({content: {cluster: {status: 'STARTED', isCompleted: false, requestId: 1}}});
      c.navigateStep();
      expect(c.loadStep.calledOnce).to.equal(true);
      expect(c.loadLogData.calledWith(1)).to.equal(true);
      expect(c.startPolling.calledOnce).to.equal(true);
    });
  });

  describe('#launchStartServices', function () {
    beforeEach(function () {
      sinon.spy(App.ajax, 'send');
      sinon.stub(c, 'togglePreviousSteps', Em.K);
      sinon.stub(c, 'saveClusterStatus', Em.K);
      c.reopen({content: {}});
    });
    afterEach(function () {
      App.ajax.send.restore();
      c.togglePreviousSteps.restore();
      c.saveClusterStatus.restore();
    });
    it('should set numPolls to 6 in testMode', function () {
      App.set('testMode', true);
      c.set('numPolls', 0);
      c.launchStartServices();
      expect(c.get('numPolls')).to.equal(6);
      App.set('testMode', false);
    });
    Em.A([
        {
          controllerName: 'installerController',
          clusterName: 'c1',
          e: {
            name: 'wizard.step9.installer.launch_start_services'
          }
        },
        {
          controllerName: 'addHostController',
          clusterName: 'c1',
          wizardController: Em.Object.create({
            getDBProperty: function () {
              return {h1: '', h2: ''};
            }
          }),
          e: {
            name: 'wizard.step9.add_host.launch_start_services',
            data: 'host_name.in(h1,h2)'
          }
        }
      ]).forEach(function (test) {
        it(test.controllerName, function () {
          c.reopen({content: {controllerName: test.controllerName, cluster: {name: test.clusterName}}});
          if (test.wizardController) {
            c.reopen({wizardController: test.wizardController});
          }
          c.launchStartServices();
          var r = App.ajax.send.args[0][0];
          expect(r.data.cluster).to.equal(test.clusterName);
          expect(r.name).to.equal(test.e.name);
          if (test.e.data) {
            expect(r.data.data.contains(test.e.data)).to.equal(true);
          }
        });
      });
  });

  describe('#isServicesStarted', function () {
    beforeEach(function () {
      sinon.stub(c, 'saveClusterStatus', Em.K);
      sinon.stub(c, 'saveInstalledHosts', Em.K);
    });
    afterEach(function () {
      c.saveClusterStatus.restore();
      c.saveInstalledHosts.restore();
    });
    Em.A([
        {
          polledData: [
            {Tasks: {status: 'PENDING'}}
          ],
          m: 'PENDING',
          e: false
        },
        {
          polledData: [
            {Tasks: {status: 'QUEUED'}}
          ],
          m: 'QUEUED',
          e: false
        },
        {
          polledData: [
            {Tasks: {status: 'IN_PROGRESS'}}
          ],
          m: 'IN_PROGRESS',
          e: false
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          var r = c.isServicesStarted(test.polledData);
          expect(r).to.equal(test.e);
        });
      });
    Em.A([
        {
          polledData: [
            {Tasks: {status: 'SUCCESS'}}
          ],
          m: 'tasks ok, isSuccess true',
          isSuccess: true,
          e: {
            status: 'STARTED',
            hasInstallTime: true
          }
        },
        {
          polledData: [
            {Tasks: {status: 'SUCCESS'}}
          ],
          m: 'tasks ok, isSuccess false',
          isSuccess: false,
          e: {
            status: 'START FAILED',
            hasInstallTime: false
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          sinon.stub(c, 'isSuccess', function () {
            return test.isSuccess;
          });
          c.reopen({content: {cluster: {requestId: 2}}});
          var r = c.isServicesStarted(test.polledData);
          var args = c.saveClusterStatus.args[0][0];
          expect(r).to.equal(true);
          expect(c.get('progress')).to.equal('100');
          expect(args.status).to.equal(test.e.status);
          expect(args.requestId).to.equal(2);
          expect(args.hasOwnProperty('installTime')).to.equal(test.e.hasInstallTime);
          expect(args.isCompleted).to.equal(true);
          expect(c.saveInstalledHosts.calledOnce).to.equal(true);
          c.isSuccess.restore();
        });
      });
  });

  describe('#launchStartServicesSuccessCallback', function () {
    beforeEach(function () {
      sinon.stub(c, 'saveClusterStatus', Em.K);
      sinon.stub(c, 'doPolling', Em.K);
      sinon.stub(c, 'hostHasClientsOnly', Em.K);
    });
    afterEach(function () {
      c.saveClusterStatus.restore();
      c.doPolling.restore();
      c.hostHasClientsOnly.restore();
    });
    it('should call doPolling if some data were received', function () {
      c.launchStartServicesSuccessCallback({Requests: {id: 2}});
      expect(c.doPolling.calledOnce).to.equal(true);
    });
    Em.A([
        {
          jsonData: {Requests: {id: 2}},
          e: {
            hostHasClientsOnly: false,
            clusterStatus: {
              status: 'INSTALLED',
              requestId: 2,
              isStartError: false,
              isCompleted: false
            }
          }
        },
        {
          jsonData: null,
          e: {
            hostHasClientsOnly: true,
            clusterStatus: {
              status: 'STARTED',
              isStartError: false,
              isCompleted: true
            },
            status: 'success',
            progress: '100'
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          c.launchStartServicesSuccessCallback(test.jsonData);
          expect(c.hostHasClientsOnly.calledWith(test.e.hostHasClientsOnly)).to.equal(true);
          expect(c.saveClusterStatus.calledWith(test.e.clusterStatus)).to.equal(true);
          if (test.e.status) {
            expect(c.get('status')).to.equal(test.e.status);
          }
          if (test.e.progress) {
            expect(c.get('progress')).to.equal(test.e.progress);
          }
        });
      });
  });

  describe('#getLogsByRequestSuccessCallback', function () {
    beforeEach(function () {
      sinon.stub(c, 'isAllComponentsInstalled', Em.K);
      sinon.stub(window, 'setTimeout', Em.K);
    });
    afterEach(function () {
      c.isAllComponentsInstalled.restore();
      window.setTimeout.restore();
    });
    Em.A([
        {
          polling: false,
          status: 'INSTALL FAILED',
          m: 'should call isAllComponentsInstalled',
          e: true
        },
        {
          polling: false,
          status: 'INSTALLED',
          m: 'shouldn\'t call isAllComponentsInstalled',
          e: false
        },
        {
          polling: true,
          status: 'INSTALL FAILED',
          m: 'shouldn\'t call isAllComponentsInstalled (2)',
          e: false
        },
        {
          polling: true,
          status: 'INSTALLED',
          m: 'shouldn\'t call isAllComponentsInstalled (3)',
          e: false
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          sinon.stub(c, 'parseHostInfo', Em.K);
          c.reopen({content: {cluster: {status: test.status}}});
          c.getLogsByRequestSuccessCallback({}, {}, {polling: test.polling});
          if (test.e) {
            expect(c.isAllComponentsInstalled.calledOnce).to.equal(true);
          }
          else {
            expect(c.isAllComponentsInstalled.called).to.equal(false);
          }
          c.parseHostInfo.restore();
        });
      });
  });

});