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

require('views/common/modal_popup');
require('controllers/global/background_operations_controller');
require('utils/helper');
require('utils/host_progress_popup');

describe('App.HostPopup', function () {

  var services = [
    {
      displayName: "Start service WebHCat",
      hosts: [
        {
          logTasks: [
            {
              Tasks: {
                command: "START",
                host_name: "ip-10-12-123-90.ec2.internal",
                role: "WEBHCAT_SERVER",
                status: "QUEUED"
              },
              href: "http://ec2-54-224-233-43.compute-1.amazonaws.com:8080/api/v1/clusters/mycluster/requests/23/tasks/94"
            }
          ],
          name: "ip-10-12-123-90.ec2.internal",
          publicName: "ip-10-12-123-90.ec2.internal",
          serviceName: "Start service WebHCat"
        }
      ],
      isRunning: false
    },
    {
      displayName: "Start service Hive",
      hosts: [
        {
          logTasks: [
            {
              Tasks: {
                command: "INSTALL",
                host_name: "ip-10-12-123-90.ec2.internal",
                status: "COMPLETED"
              },
              href: "http://ec2-54-224-233-43.compute-1.amazonaws.com:8080/api/v1/clusters/mycluster/requests/15/tasks/76"
            }
          ],
          name: "ip-10-12-123-90.ec2.internal",
          publicName: "ip-10-12-123-90.ec2.internal",
          serviceName: "Start service Hive"
        },
        {
          logTasks: [
            {
              Tasks: {
                command: "START",
                host_name: "ip-10-33-7-23.ec2.internal",
                status: "COMPLETED"
              },
              href: "http://ec2-54-224-233-43.compute-1.amazonaws.com:8080/api/v1/clusters/mycluster/requests/15/tasks/78"
            },
            {
              Tasks: {
                command: "START",
                host_name: "ip-10-33-7-23.ec2.internal",
                status: "COMPLETED"
              },
              href: "http://ec2-54-224-233-43.compute-1.amazonaws.com:8080/api/v1/clusters/mycluster/requests/15/tasks/79"
            }
          ],
          name: "ip-10-33-7-23.ec2.internal",
          publicName: "ip-10-33-7-23.ec2.internal",
          serviceName: "Start service Hive"
        }
      ],
      isRunning: false
    }
  ];

  var bgController = App.BackgroundOperationsController.create();
  bgController.set('services', services);

  describe('#initPopup', function() {
    App.HostPopup.initPopup("", bgController, true);
    it('services loaded', function() {
      expect(App.HostPopup.get('inputData').length).to.equal(services.length);
    });
  });

  var test_tasks = [
    {
      t: [
        {
          Tasks: {
            status: 'COMPLETED',
            id: 2
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 3
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 1
          }
        }
      ],
      m: 'All COMPLETED',
      r: 'SUCCESS',
      p: 100,
      ids: [1,2,3]
    },
    {
      t: [
        {
          Tasks: {
            status: 'FAILED',
            id: 2
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 1
          }
        }
        ,
        {
          Tasks: {
            status: 'COMPLETED',
            id: 3
          }
        }
      ],
      m: 'One FAILED',
      r: 'FAILED',
      p: 100,
      ids: [1,2,3]
    },
    {
      t: [
        {
          Tasks: {
            status: 'ABORTED',
            id: 1
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 2
          }
        }
      ],
      m: 'One ABORTED',
      r: 'CANCELLED',
      p: 100,
      ids: [1,2]
    },
    {
      t: [
        {
          Tasks: {
            status: 'TIMEDOUT',
            id: 3
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 1
          }
        }
      ],
      m: 'One TIMEDOUT',
      r: 'TIMEDOUT',
      p: 100,
      ids: [1,3]
    },
    {
      t: [
        {
          Tasks: {
            status: 'IN_PROGRESS',
            id: 1
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 2
          }
        }
      ],
      m: 'One IN_PROGRESS',
      r: 'IN_PROGRESS',
      p: 68,
      ids: [1,2]
    },
    {
      t: [
        {
          Tasks: {
            status: 'QUEUED',
            id: 2
          }
        },
        {
          Tasks: {
            status: 'COMPLETED',
            id: 3
          }
        }
      ],
      m: 'Something else',
      r: 'PENDING',
      p: 55,
      ids: [2,3]
    }
  ];

  describe('#getStatus', function() {
    test_tasks.forEach(function(test_task) {
      it(test_task.m, function() {
        expect(App.HostPopup.getStatus(test_task.t)[0]).to.equal(test_task.r);
      });
    });
  });

  describe('#getProgress', function() {
    test_tasks.forEach(function(test_task) {
      it(test_task.m, function() {
        expect(App.HostPopup.getProgress(test_task.t)).to.equal(test_task.p);
      });
    });
  });

  describe('#sortTasksById', function() {
    test_tasks.forEach(function(test_task) {
      it(test_task.m, function() {
        expect(App.HostPopup.sortTasksById(test_task.t).mapProperty('Tasks.id')).to.eql(test_task.ids);
      });
    });
  });

});
