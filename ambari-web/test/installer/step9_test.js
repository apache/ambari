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

describe('App.InstallerStep9Controller', function () {

  describe('#isSubmitDisabled', function () {
    var tests = [
      {controllerName: 'addHostController',state: 'STARTED',e: false},
      {controllerName: 'addHostController',state: 'START FAILED',e: false},
      {controllerName: 'addHostController',state: 'INSTALL FAILED',e: false},
      {controllerName: 'addHostController',state: 'PENDING',e: true},
      {controllerName: 'addHostController',state: 'INSTALLED',e: true},
      {controllerName: 'addServiceController',state: 'STARTED',e: false},
      {controllerName: 'addServiceController',state: 'START FAILED',e: false},
      {controllerName: 'addServiceController',state: 'INSTALL FAILED',e: false},
      {controllerName: 'addServiceController',state: 'PENDING',e: true},
      {controllerName: 'addServiceController',state: 'INSTALLED',e: true},
      {controllerName: 'installerController',state: 'STARTED',e: false},
      {controllerName: 'installerController',state: 'START FAILED',e: false},
      {controllerName: 'installerController',state: 'INSTALL FAILED',e: true},
      {controllerName: 'installerController',state: 'INSTALLED',e: true},
      {controllerName: 'installerController',state: 'PENDING',e: true}
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep9Controller.create({
        content: {
          controllerName: test.controllerName,
            cluster: {
            status: test.state
          }
        }
      });
      it('controllerName is ' + test.controllerName + '; cluster status is ' + test.state + '; isSubmitDisabled should be ' + test.e, function() {
        expect(controller.get('isSubmitDisabled')).to.equal(test.e);
      });
    });

  });

  describe('#status', function() {
    var tests = [
      {
        hosts: [{status: 'failed'},{status: 'success'}],
        isStepFailed: false,
        progress: '100',
        m:'One host is failed',
        e:'failed'
      },
      {
        hosts: [{status: 'warning'},{status: 'success'}],
        m:'One host is failed and step is not failed',
        isStepFailed: false,
        progress: '100',
        e:'warning'
      },
      {
        hosts: [{status: 'warning'},{status: 'success'}],
        m:'One host is failed and step is failed',
        isStepFailed: true,
        progress: '100',
        e:'failed'
      },
      {
        hosts: [{status: 'success'},{status: 'success'}],
        m:'All hosts are success and progress is 100',
        isStepFailed: false,
        progress: '100',
        e:'success'
      },
      {
        hosts: [{status: 'success'},{status: 'success'}],
        m:'All hosts are success and progress is 50',
        isStepFailed: false,
        progress: '50',
        e:'info'
      }
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep9Controller.create({hosts: test.hosts, isStepFailed: function(){return test.isStepFailed}, progress: test.progress});
      it(test.m, function() {
        expect(controller.get('status')).to.equal(test.e);
      });
    });
  });

  describe('#visibleHosts', function() {
    var hosts = [
      Em.Object.create({status: 'failed'}),
      Em.Object.create({status: 'success'}),
      Em.Object.create({status: 'success'}),
      Em.Object.create({status: 'warning'}),
      Em.Object.create({status: 'info'}),
      Em.Object.create({status: 'info'})
    ];
    var tests = [
      {category: {hostStatus: 'all'},e: hosts.length},
      {category:{hostStatus: 'inProgress'},e: 2},
      {category: {hostStatus: 'warning'},e: 1},
      {category: {hostStatus: 'failed'},e: 1},
      {category: {hostStatus: 'success'},e: 2}
    ];
    var controller = App.WizardStep9Controller.create({
      hosts: hosts
    });
    tests.forEach(function(test) {
      it('selected category with hostStatus "' + test.category.hostStatus + '"', function() {
        controller.selectCategory({context: test.category});
        expect(controller.get('visibleHosts.length')).to.equal(test.e);
      });
    });
  });

  describe('#showRetry', function() {
    it('cluster status is not INSTALL FAILED', function() {
      var controller = App.WizardStep9Controller.create({content: {cluster:{status:'INSTALLED'}}});
      expect(controller.get('showRetry')).to.equal(false);
    });
    it('cluster status is INSTALL FAILED', function() {
      var controller = App.WizardStep9Controller.create({content: {cluster:{status:'INSTALL FAILED'}}});
      expect(controller.get('showRetry')).to.equal(true);
    });
  });

  describe('#resetHostsForRetry', function() {
    var hosts = {'host1':Em.Object.create({status:'failed', message:'Failed'}), 'host2':Em.Object.create({status:'success', message:'Success'})};
    var controller = App.WizardStep9Controller.create({content:{hosts: hosts}});
    it('All should have status "pending" and message "Waiting"', function() {
      controller.resetHostsForRetry();
      for (var name in hosts) {
        expect(controller.get('content.hosts')[name].get('status','pending')).to.equal('pending');
        expect(controller.get('content.hosts')[name].get('message','Waiting')).to.equal('Waiting');
      }
    });
  });

  var hosts_for_load_and_render = {
    'host1': {
      message: 'message1',
      status: 'unknown',
      progress: '1',
      tasks: [{},{}],
      logTasks: [{},{}],
      bootStatus: 'REGISTERED'
    },
    'host2': {
      message: '',
      status: 'failed',
      progress: '1',
      tasks: [{},{}],
      logTasks: [{},{}],
      bootStatus: ''
    },
    'host3': {
      message: '',
      status: 'waiting',
      progress: null,
      tasks: [{},{}],
      logTasks: [{},{}],
      bootStatus: ''
    },
    'host4': {
      message: 'message4',
      status: null,
      progress: '10',
      tasks: [],
      logTasks: [{}],
      bootStatus: 'REGISTERED'
    }
  };
  
  describe('#loadHosts', function() {
    var controller = App.WizardStep9Controller.create({content: {hosts: hosts_for_load_and_render}});
    var loaded_hosts = controller.loadHosts();
    it('Only REGISTERED hosts', function() {
      expect(loaded_hosts.length).to.equal(2);
    });
    it('All hosts have progress 0', function() {
      expect(loaded_hosts.everyProperty('progress', 0)).to.equal(true);
    });
    it('All hosts have progress 0', function() {
      expect(loaded_hosts.everyProperty('progress', 0)).to.equal(true);
    });
    it('All host don\'t have tasks and logTasks', function() {
      expect(loaded_hosts.everyProperty('tasks.length', 0)).to.equal(true);
      expect(loaded_hosts.everyProperty('logTasks.length', 0)).to.equal(true);
    });
  });

  describe('#renderHosts', function() {
    var controller = App.WizardStep9Controller.create({content: {hosts: hosts_for_load_and_render}});
    var loaded_hosts = controller.loadHosts();
    controller.renderHosts(loaded_hosts);
    it('All host should be rendered', function() {
      expect(controller.get('hosts.length')).to.equal(loaded_hosts.length);
    });
  });

  describe('#hostHasClientsOnly', function() {
    var tests = [
      {
        hosts: [
          Em.Object.create({
            hostName: 'host1',
            logTasks: [{Tasks: {role: 'HDFS_CLIENT'}},{Tasks: {role: 'DATANODE'}}],
            status: 'old_status',
            progress: '10',
            e: {status: 'old_status',progress: '10'}
          }),
          Em.Object.create({
            hostName: 'host2',
            logTasks: [{Tasks: {role: 'HDFS_CLIENT'}}],
            status: 'old_status',
            progress: '10',
            e: {status: 'success',progress: '100'}
          })
        ],
        jsonError: false
      },
      {
        hosts: [
          Em.Object.create({
            hostName: 'host1',
            logTasks: [{Tasks: {role: 'HDFS_CLIENT'}},{Tasks: {role: 'DATANODE'}}],
            status: 'old_status',
            progress: '10',
            e: {status: 'success',progress: '100'}
          }),
          Em.Object.create({
            hostName: 'host2',
            logTasks: [{Tasks: {role: 'HDFS_CLIENT'}}],
            status: 'old_status',
            progress: '10',
            e: {status: 'success',progress: '100'}
          })
        ],
        jsonError: true
      }
    ];
    tests.forEach(function(test) {
      it('', function() {
        var controller = App.WizardStep9Controller.create({hosts: test.hosts});
        controller.hostHasClientsOnly(test.jsonError);
        test.hosts.forEach(function(host) {
          expect(controller.get('hosts').findProperty('hostName', host.hostName).get('status')).to.equal(host.e.status);
          expect(controller.get('hosts').findProperty('hostName', host.hostName).get('progress')).to.equal(host.e.progress);
        });
      });
    });
  });

  describe('#onSuccessPerHost', function() {
    var tests = [
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
        actions: [{Tasks: {status: 'COMPLETED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'success'},
        m: 'All Tasks COMPLETED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'COMPLETED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'info'},
        m: 'All Tasks COMPLETED and cluster status FAILED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'FAILED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'info'},
        m: 'Not all Tasks COMPLETED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'FAILED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'info'},
        m: 'Not all Tasks COMPLETED and cluster status FAILED'
      }
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: test.cluster.status}}});
      controller.onSuccessPerHost(test.actions, test.host);
      it(test.m, function() {
        expect(test.host.status).to.equal(test.e.status);
      });
    });
  });

  describe('#onErrorPerHost', function() {
    var tests = [
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
        actions: [{Tasks: {status: 'FAILED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task FAILED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'ABORTED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task ABORTED and cluster status INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'TIMEDOUT'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'warning'},
        isMasterFailed: false,
        m: 'One Task TIMEDOUT and cluster status INSTALLED'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'FAILED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'failed'},
        isMasterFailed: true,
        m: 'One Task FAILED and cluster status PENDING isMasterFailed true'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({status: 'info'}),
        actions: [{Tasks: {status: 'COMPLETED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {status: 'info'},
        isMasterFailed: false,
        m: 'One Task FAILED and cluster status PENDING isMasterFailed false'
      }
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep9Controller.create({content: {cluster: {status: test.cluster.status}}, isMasterFailed: function(){return test.isMasterFailed;}});
      controller.onErrorPerHost(test.actions, test.host);
      it(test.m, function() {
        expect(test.host.status).to.equal(test.e.status);
      });
    });
  });

  describe('#isMasterFailed', function() {
    var tests = [
      {
        actions: [
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'DATANODE'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'GANGLIA_MONITOR'}}
        ],
        e: false,
        m: 'No one Master is failed'
      },
      {
        actions: [
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'NAMENODE'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'GANGLIA_MONITOR'}}
        ],
        e: true,
        m: 'One Master is failed'
      },
      {
        actions: [
          {Tasks: {command: 'PENDING',status: 'FAILED',role: 'NAMENODE'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'TASKTRACKER'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'HBASE_REGIONSERVER'}},
          {Tasks: {command: 'INSTALL',status: 'FAILED',role: 'GANGLIA_MONITOR'}}
        ],
        e: false,
        m: 'one Master is failed but command is not install'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create();
        expect(controller.isMasterFailed(test.actions)).to.equal(test.e);
      });
    });
  });

  describe('#onInProgressPerHost', function() {
    var tests = [
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [{Tasks: {status: 'COMPLETED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {message: 'default_message',b: true},
        m: 'All Tasks COMPLETED'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [{Tasks: {status: 'IN_PROGRESS'}},{Tasks: {status: 'COMPLETED'}}],
        e: {message: 'default_message',b: false},
        m: 'One Task IN_PROGRESS'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [{Tasks: {status: 'QUEUED'}},{Tasks: {status: 'COMPLETED'}}],
        e: {message: 'default_message',b: false},
        m: 'One Task QUEUED'
      },
      {
        host: Em.Object.create({message: 'default_message'}),
        actions: [{Tasks: {status: 'PENDING'}},{Tasks: {status: 'COMPLETED'}}],
        e: {message: 'default_message',b: false},
        m: 'One Task PENDING'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create();
        controller.onInProgressPerHost(test.actions, test.host);
        expect(test.host.message == test.e.message).to.equal(test.e.b);
      });
    });
  });

  describe('#progressPerHost', function() {
    var tests = [
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
        e: {ret: 17,host: '17'},
        m: 'All types of status available. cluster status PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        host: Em.Object.create({progress: 0}),
        actions: [],
        e: {ret: 33,host: '33'},
        m: 'No tasks available. cluster status PENDING'
      },
      {
        cluster: {status: 'INSTALLED'},
        host: Em.Object.create({progress: 0}),
        actions: [],
        e: {ret: 100,host: '100'},
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
        e: {ret: 68,host: '68'},
        m: 'All types of status available. cluster status INSTALLED'
      },
      {
        cluster: {status: 'FAILED'},
        host: Em.Object.create({progress: 0}),
        actions: [],
        e: {ret: 100,host: '100'},
        m: 'Cluster status is not PENDING or INSTALLED'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create({content: {cluster: {status: test.cluster.status}}});
        var progress = controller.progressPerHost(test.actions, test.host);
        expect(progress).to.equal(test.e.ret);
        expect(test.host.progress).to.equal(test.e.host);
      });
    });
  });

  describe('#clearStep', function() {
    var controller = App.WizardStep9Controller.create({hosts: [{},{},{}]});
    it('All to default values', function() {
      controller.clearStep();
      expect(controller.get('hosts.length')).to.equal(0);
      expect(controller.get('status')).to.equal('info');
      expect(controller.get('progress')).to.equal('0');
      expect(controller.get('isStepCompleted')).to.equal(false);
      expect(controller.get('numPolls')).to.equal(1);
    });
  });

  describe('#replacePolledData', function() {
    var controller = App.WizardStep9Controller.create({polledData: [{},{},{}]});
    var newPolledData = [{}];
    controller.replacePolledData(newPolledData);
    it('replacing polled data', function() {
      expect(controller.get('polledData.length')).to.equal(newPolledData.length);
    });
  });

  describe('#isSuccess', function() {
    var tests = [
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
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create();
        expect(controller.isSuccess(test.polledData)).to.equal(test.e);
      });
    });
  });

  describe('#isStepFailed', function() {
    var tests = [
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'GANGLIA_MONITOR',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'GANGLIA_MONITOR',status: 'FAILED'}},
          {Tasks: {command: 'INSTALL',role: 'GANGLIA_MONITOR',status: 'PENDING'}}
        ],
        e: true,
        m: 'GANGLIA_MONITOR 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'GANGLIA_MONITOR',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'GANGLIA_MONITOR',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'GANGLIA_MONITOR',status: 'PENDING'}}
        ],
        e: false,
        m: 'GANGLIA_MONITOR 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'HBASE_REGIONSERVER',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'HBASE_REGIONSERVER',status: 'FAILED'}},
          {Tasks: {command: 'INSTALL',role: 'HBASE_REGIONSERVER',status: 'PENDING'}}
        ],
        e: true,
        m: 'HBASE_REGIONSERVER 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'HBASE_REGIONSERVER',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'HBASE_REGIONSERVER',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'HBASE_REGIONSERVER',status: 'PENDING'}}
        ],
        e: false,
        m: 'HBASE_REGIONSERVER 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'TASKTRACKER',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'TASKTRACKER',status: 'FAILED'}},
          {Tasks: {command: 'INSTALL',role: 'TASKTRACKER',status: 'PENDING'}}
        ],
        e: true,
        m: 'TASKTRACKER 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'TASKTRACKER',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'TASKTRACKER',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'TASKTRACKER',status: 'PENDING'}}
        ],
        e: false,
        m: 'TASKTRACKER 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'FAILED'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}}
        ],
        e: true,
        m: 'DATANODE 2/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}}
        ],
        e: false,
        m: 'DATANODE 1/3 failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'NAMENODE',status: 'TIMEDOUT'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}}
        ],
        e: true,
        m: 'NAMENODE failed'
      },
      {
        polledData: [
          {Tasks: {command: 'INSTALL',role: 'NAMENODE',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}},
          {Tasks: {command: 'INSTALL',role: 'DATANODE',status: 'PENDING'}}
        ],
        e: false,
        m: 'Nothing failed failed'
      }
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep9Controller.create({polledData: test.polledData});
      it(test.m, function() {
        expect(controller.isStepFailed()).to.equal(test.e);
      });
    });
  });

  describe('#getUrl', function() {
    var clusterName = 'tdk';
    var cluster = App.WizardStep9Controller.create({content:{cluster:{name: clusterName, requestId: null}}});
    it('check requestId priority', function() {
      cluster.set('content.cluster.requestId', 123);
      var url = cluster.getUrl(321);
      expect(url).to.equal(App.apiPrefix + '/clusters/' + clusterName + '/requests/' + '321' + '?fields=tasks/*');
      url = cluster.getUrl();
      expect(url).to.equal(App.apiPrefix + '/clusters/' + clusterName + '/requests/' + '123' + '?fields=tasks/*');
    });
  });

  describe('#finishState', function() {
    var statuses = ['INSTALL FAILED', 'START FAILED', 'STARTED'];
    it('Installer is finished', function() {
      statuses.forEach(function(status) {
        var controller = App.WizardStep9Controller.create({content:{cluster:{status:status}}});
        var result = controller.finishState();
        expect(result).to.equal(true);
      });
    });
    it('Unknown cluster status ', function() {
      var controller = App.WizardStep9Controller.create({content:{cluster:{status:'FAKE_STATUS'}}});
      var result = controller.finishState();
      expect(result).to.equal(false);
    });
  });

  describe('#setTasksPerHost', function() {
    var tests = [
      {
        hosts: [
          Em.Object.create({
            name: 'host1',
            tasks: [],
            bootStatus: 'REGISTERED'
          }),
          Em.Object.create({
            name: 'host2',
            tasks: [],
            bootStatus: 'REGISTERED'
          }),
          Em.Object.create({
            name: 'host3',
            tasks: [],
            bootStatus: 'REGISTERED'
          })
        ],
        polledData: [
          {Tasks: {host_name: 'host1'}},
          {Tasks: {host_name: 'host1'}},
          {Tasks: {host_name: 'host1'}},
          {Tasks: {host_name: 'host2'}},
          {Tasks: {host_name: 'host2'}},
          {Tasks: {host_name: 'host3'}}
        ],
        e: {
          host1: {count: 3},
          host2: {count: 2},
          host3: {count: 1}
        },
        m: 'Several tasks for each host'
      },
      {
        hosts: [
          Em.Object.create({
            name: 'host1',
            tasks: [],
            bootStatus: 'REGISTERED'
          }),
          Em.Object.create({
            name: 'host2',
            tasks: [],
            bootStatus: 'REGISTERED'
          }),
          Em.Object.create({
            name: 'host3',
            tasks: [],
            bootStatus: 'REGISTERED'
          })
        ],
        polledData: [
          {Tasks: {host_name: 'host1'}},
          {Tasks: {host_name: 'host2'}}
        ],
        e: {
          host1: {count: 1},
          host2: {count: 1},
          host3: {count: 0}
        },
        m: 'Some hosts without tasks'
      },
      {
        hosts: [
          Em.Object.create({
            name: 'host1',
            tasks: [],
            bootStatus: 'REGISTERED'
          }),
          Em.Object.create({
            name: 'host2',
            tasks: [],
            bootStatus: 'REGISTERED'
          }),
          Em.Object.create({
            name: 'host3',
            tasks: [],
            bootStatus: 'REGISTERED'
          })
        ],
        polledData: [],
        e: {
          host1: {count: 0},
          host2: {count: 0},
          host3: {count: 0}
        },
        m: 'No tasks'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create({polledData: test.polledData, hosts: test.hosts});
        controller.setTasksPerHost();
        for(var name in test.e.hosts) {
          expect(controller.get('hosts').findProperty('name', name).get('tasks.length')).to.equal(test.e[name].count);
        }
      });
    });
  });

  describe('#setLogTasksStatePerHost', function() {
    var tests = [
      {
        tasksPerHost: [{Tasks: {id: 1,message: '2'}},{Tasks: {id: 2,message: '2'}}],
        tasks: [],
        e: {m: '2',l: 2},
        m: 'host didn\'t have tasks and got 2 new'
      },
      {
        tasksPerHost: [{Tasks: {id: 1,message: '2'}},{Tasks: {id: 2,message: '2'}}],
        tasks: [{Tasks: {id: 1,message: '1'}},{Tasks: {id: 2,message: '1'}}],
        e: {m: '2',l: 2},
        m: 'host had 2 tasks and got both updated'
      },
      {
        tasksPerHost: [],
        tasks: [{Tasks: {id: 1,message: '1'}},{Tasks: {id: 2,message: '1'}}],
        e: {m: '1',l: 2},
        m: 'host had 2 tasks and didn\'t get updates'
      },
      {
        tasksPerHost: [{Tasks: {id: 1,message: '2'}},{Tasks: {id: 2,message: '2'}},{Tasks: {id: 3,message: '2'}}],
        tasks: [{Tasks: {id: 1,message: '1'}},{Tasks: {id: 2,message: '1'}}],
        e: {m: '2',l: 3},
        m: 'host had 2 tasks and got both updated and 1 new'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create({hosts: [Em.Object.create({logTasks: test.tasks})]});
        var logTasksChangesCounter = controller.get('logTasksChangesCounter');
        controller.setLogTasksStatePerHost(test.tasksPerHost, controller.get('hosts')[0]);
        expect(controller.get('hosts')[0].get('logTasks').everyProperty('Tasks.message', test.e.m)).to.equal(true);
        expect(controller.get('hosts')[0].get('logTasks.length')).to.equal(test.e.l);
        expect(controller.get('logTasksChangesCounter')).to.equal(logTasksChangesCounter + 1);
      });
    });
  });

  describe('#parseHostInfo', function() {
    var requestId = 1;
    var polledData = {Requests:{id:2}};
    it('Invalid requestId. Should return false', function() {
      var controller = App.WizardStep9Controller.create({content: {cluster:{requestId: requestId}}});
      var res = controller.parseHostInfo(polledData);
      expect(res).to.equal(false);
    });

    var tests = [
      {
        cluster: {status: 'PENDING'},
        hosts: [
          Em.Object.create({name: 'host1',status: '',message: '',progress: '',logTasks: []}),
          Em.Object.create({name: 'host2',status: '',message: '',progress: '',logTasks: []})
        ],
        polledData: {
          tasks:[
            {Tasks: {host_name: 'host2',status: 'COMPLETED'}},
            {Tasks: {host_name: 'host2',status: 'COMPLETED'}}
          ]
        },
        e: {
          hosts:{
            host1: {progress: '33'},
            host2: {progress: '33'}
          },
          progress: '33'
        },
        m: 'Two hosts. One host without tasks. Second host has all tasks COMPLETED. Cluster status is PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        hosts: [
          Em.Object.create({name: 'host1',status: '',message: '',progress: '',logTasks: []}),
          Em.Object.create({name: 'host2',status: '',message: '',progress: '',logTasks: []})
        ],
        polledData: {
          tasks:[
            {Tasks: {host_name: 'host1',status: 'IN_PROGRESS'}},
            {Tasks: {host_name: 'host2',status: 'IN_PROGRESS'}}
          ]
        },
        e: {hosts:{host1: {progress: '12'},host2: {progress: '12'}},progress: '12'},
        m: 'Two hosts. Each host has one task IN_PROGRESS. Cluster status is PENDING'
      },
      {
        cluster: {status: 'PENDING'},
        hosts: [
          Em.Object.create({name: 'host1',status: '',message: '',progress: '',logTasks: []}),
          Em.Object.create({name: 'host2',status: '',message: '',progress: '',logTasks: []})
        ],
        polledData: {
          tasks:[
            {Tasks: {host_name: 'host1',status: 'QUEUED'}},
            {Tasks: {host_name: 'host2',status: 'QUEUED'}}
          ]
        },
        e: {
          hosts:{
            host1: {progress: '3'},
            host2: {progress: '3'}
          },
          progress: '3'
        },
        m: 'Two hosts. Each host has one task QUEUED. Cluster status is PENDING'
      },
      {
        cluster: {status: 'INSTALLED'},
        hosts: [
          Em.Object.create({name: 'host1',status: '',message: '',progress: '',logTasks: []}),
          Em.Object.create({name: 'host2',status: '',message: '',progress: '',logTasks: []})
        ],
        polledData: {
          tasks:[
            {Tasks: {host_name: 'host2',status: 'COMPLETED'}},
            {Tasks: {host_name: 'host2',status: 'COMPLETED'}}
          ]
        },
        e: {
          hosts:{
            host1: {progress: '100'},
            host2: {progress: '100'}
          },
          progress: '100'
        },
        m: 'Two hosts. One host without tasks. Second host has all tasks COMPLETED. Cluster status is INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        hosts: [
          Em.Object.create({name: 'host1',status: '',message: '',progress: '',logTasks: []}),
          Em.Object.create({name: 'host2',status: '',message: '',progress: '',logTasks: []})
        ],
        polledData: {
          tasks:[
            {Tasks: {host_name: 'host1',status: 'IN_PROGRESS'}},
            {Tasks: {host_name: 'host2',status: 'IN_PROGRESS'}}
          ]
        },
        e: {
          hosts:{
            host1: {progress: '58'},
            host2: {progress: '58'}
          },
          progress: '58'
        },
        m: 'Two hosts. Each host has one task IN_PROGRESS. Cluster status is INSTALLED'
      },
      {
        cluster: {status: 'INSTALLED'},
        hosts: [
          Em.Object.create({name: 'host1',status: '',message: '',progress: '',logTasks: []}),
          Em.Object.create({name: 'host2',status: '',message: '',progress: '',logTasks: []})
        ],
        polledData: {
          tasks:[
            {Tasks: {host_name: 'host1',status: 'QUEUED'}},
            {Tasks: {host_name: 'host2',status: 'QUEUED'}}
          ]
        },
        e: {
          hosts:{
            host1: {progress: '40'},
            host2: {progress: '40'}
          },
          progress: '40'
        },
        m: 'Two hosts. Each host has one task QUEUED. Cluster status is INSTALLED'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.WizardStep9Controller.create({hosts: test.hosts, content: {cluster:{status: test.cluster.status}}, finishState: function(){return false;}});
        controller.parseHostInfo(test.polledData);
        for (var name in test.e.hosts) {
          expect(controller.get('hosts').findProperty('name', name).get('progress')).to.equal(test.e.hosts[name].progress);
        }
        expect(controller.get('progress')).to.equal(test.e.progress);
      });
    });
  });

});