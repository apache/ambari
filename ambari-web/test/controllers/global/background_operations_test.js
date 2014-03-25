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


var App = require('app');

require('config');
require('utils/updater');
require('utils/ajax');

require('models/host_component');

require('controllers/global/background_operations_controller');
require('views/common/modal_popup');
require('utils/host_progress_popup');

var controller;

describe('App.BackgroundOperationsController', function () {

  /**
   * Predefined data
   *
   */
  App.set('clusterName', 'testName');
  App.bgOperationsUpdateInterval = 100;

  var tests = Em.A([
    {
      levelInfo: Em.Object.create({
        name: 'REQUESTS_LIST',
        requestId: null,
        taskId: null,
        sync: false
      }),
      e: {
        name: 'background_operations.get_most_recent',
        successCallback: 'callBackForMostRecent',
        data: {}
      },
      response: {items:[]},
      m: '"Get Most Recent"'
    },
    {
      levelInfo: Em.Object.create({
        name: 'TASK_DETAILS',
        requestId: 1,
        taskId: 1,
        sync: false
      }),
      e: {
        name: 'background_operations.get_by_task',
        successCallback: 'callBackFilteredByTask',
        data: {
          taskId: 1,
          requestId: 1,
          sync: false
        }
      },
      response: {items:{Tasks:{request_id:0}}},
      m: '"Filtered By task"'
    },
    {
      levelInfo: Em.Object.create({
        name: 'TASKS_LIST',
        requestId: 1,
        taskId: 1,
        sync: false
      }),
      e: {
        name: 'background_operations.get_by_request',
        successCallback: 'callBackFilteredByRequest',
        data: {
          requestId: 1,
          sync: false
        }
      },
      response: {items:{Requests:{id:0}}},
      m: '"Filtered By Request (TASKS_LIST)"'
    },
    {
      levelInfo: Em.Object.create({
        name: 'HOSTS_LIST',
        requestId: 1,
        taskId: 1,
        sync: false
      }),
      e: {
        name: 'background_operations.get_by_request',
        successCallback: 'callBackFilteredByRequest',
        data: {
          requestId: 1,
          sync: false
        }
      },
      response: {items:{Requests:{id:0}}},
      m: '"Filtered By Request (HOSTS_LIST)"'
    }
  ]);

  describe('#getQueryParams', function() {
    beforeEach(function() {
      controller = App.BackgroundOperationsController.create();
      App.testMode = false;
    });
    afterEach(function() {
      App.testMode = true;
    });

    tests.forEach(function(test) {
      it(test.m, function() {
        controller.set('levelInfo', test.levelInfo);
        var r = controller.getQueryParams();
        expect(r.name).to.equal(test.e.name);
        expect(r.successCallback).to.equal(test.e.successCallback);
        expect(r.data).to.eql(test.e.data);
      });
    });
  });

});
