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

require('controllers/global/background_operations_controller');
require('views/common/modal_popup');
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
      displayName: "Start service Hive/HCat",
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
          serviceName: "Start service Hive/HCat"
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
          serviceName: "Start service Hive/HCat"
        }
      ],
      isRunning: false
    }
  ];

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
      r: 'ABORTED',
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

  var statusCases = [
    {
      status: 'FAILED',
      result: false
    },
    {
      status: 'ABORTED',
      result: false
    },
    {
      status: 'TIMEDOUT',
      result: false
    },
    {
      status: 'IN_PROGRESS',
      result: true
    },
    {
      status: 'COMPLETED',
      result: false
    },
    {
      status: 'PENDING',
      result: true
    }
  ];

  describe('#setSelectCount', function () {
    var itemsForStatusTest = [
      {
        title: 'Empty',
        data: [],
        result: [0, 0, 0, 0, 0, 0, 0]
      },
      {
        title: 'All Pending',
        data: [
          {status: 'pending'},
          {status: 'queued'}
        ],
        result: [2, 2, 0, 0, 0, 0, 0]
      },
      {
        title: 'All Completed',
        data: [
          {status: 'success'},
          {status: 'completed'}
        ],
        result: [2, 0, 0, 0, 2, 0, 0]
      },
      {
        title: 'All Failed',
        data: [
          {status: 'failed'},
          {status: 'failed'}
        ],
        result: [2, 0, 0, 2, 0, 0, 0]
      },
      {
        title: 'All InProgress',
        data: [
          {status: 'in_progress'},
          {status: 'in_progress'}
        ],
        result: [2, 0, 2, 0, 0, 0, 0]
      },
      {
        title: 'All Aborted',
        data: [
          {status: 'aborted'},
          {status: 'aborted'}
        ],
        result: [2, 0, 0, 0, 0, 2, 0]
      },
      {
        title: 'All Timedout',
        data: [
          {status: 'timedout'},
          {status: 'timedout'}
        ],
        result: [2, 0, 0, 0, 0, 0, 2]
      },
      {
        title: 'Every Category',
        data: [
          {status: 'pending'},
          {status: 'queued'},
          {status: 'success'},
          {status: 'completed'},
          {status: 'failed'},
          {status: 'in_progress'},
          {status: 'aborted'},
          {status: 'timedout'}
        ],
        result: [8, 2, 1, 1, 2, 1, 1]
      }
    ];
    var categories = [
      Ember.Object.create({value: 'all'}),
      Ember.Object.create({value: 'pending'}),
      Ember.Object.create({value: 'in_progress'}),
      Ember.Object.create({value: 'failed'}),
      Ember.Object.create({value: 'completed'}),
      Ember.Object.create({value: 'aborted'}),
      Ember.Object.create({value: 'timedout'})
    ];
    itemsForStatusTest.forEach(function(statusTest) {
      it(statusTest.title, function() {
        App.HostPopup.setSelectCount(statusTest.data, categories);
        expect(categories.mapProperty('count')).to.deep.equal(statusTest.result);
      });
    });
  });

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

  describe('#isAbortableByStatus', function () {
    statusCases.forEach(function (item) {
      it('should return ' + item.result + ' for ' + item.status, function () {
        expect(App.HostPopup.isAbortableByStatus(item.status)).to.equal(item.result);
      });
    });
  });

  describe('#abortRequest', function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.spy(App, 'showConfirmationPopup');
    });
    afterEach(function () {
      App.ajax.send.restore();
      App.showConfirmationPopup.restore();
    });
    it('should show confirmation popup', function () {
      App.HostPopup.abortRequest(Em.Object.create({
        name: 'name'
      }));
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
    });
  });

  describe('#abortRequestSuccessCallback', function () {
    beforeEach(function () {
      sinon.spy(App.ModalPopup, 'show');
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
    });
    it('should open popup', function () {
      App.HostPopup.abortRequestSuccessCallback(null, null, {
        requestName: 'name',
        serviceInfo: Em.Object.create()
      });
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#abortRequestErrorCallback', function () {
    var popup = App.HostPopup;
    beforeEach(function () {
      sinon.stub(App.ajax, 'get', function(k) {
        if (k === 'modalPopup') return null;
        return Em.get(App, k);
      });
      sinon.spy(App.ModalPopup, 'show');
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
      App.ajax.get.restore();
    });
    it('should open popup', function () {
      popup.abortRequestErrorCallback({
        responseText: {
          message: 'message'
        },
        status: 404
      }, 'status', 'error', {
        url: 'url'
      }, {
        requestId: 0,
        serviceInfo: Em.Object.create()
      });
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
    statusCases.forEach(function (item) {
      it('should set serviceInfo.isAbortable to' + item.result + ' if status is ' + item.status, function () {
        popup.abortRequestErrorCallback({
          responseText: {
            message: 'message'
          },
          status: 404
        }, 'status', 'error', {
          url: 'url'
        }, {
          requestId: 0,
          serviceInfo: Em.Object.create({
            status: item.status
          })
        });
        expect(App.HostPopup.isAbortableByStatus(item.status)).to.equal(item.result);
      });
    });
  });

  describe('#setBackgroundOperationHeader', function(){
    beforeEach(function (){
      sinon.stub(App.HostPopup, "get").returns(true);
      sinon.spy(App.HostPopup, "set");
    });

    afterEach(function (){
      App.HostPopup.get.restore();
      App.HostPopup.set.restore();
      App.router.get.restore();
    });

    it("should display '2 Background Operations Running' when there are 2 background operations running", function(){
      sinon.stub(App.router, "get").returns(2);
      App.HostPopup.setBackgroundOperationHeader(false);

      expect(App.HostPopup.set.calledWith("popupHeaderName", "2 Background Operations Running")).to.be.true;
    });

    it("should display '1 Background Operation Running' when there is 1 background operation running", function(){
      sinon.stub(App.router, "get").returns(1);
      App.HostPopup.setBackgroundOperationHeader(false);

      expect(App.HostPopup.set.calledWith("popupHeaderName", "1 Background Operation Running")).to.be.true;
    });
  });

});
