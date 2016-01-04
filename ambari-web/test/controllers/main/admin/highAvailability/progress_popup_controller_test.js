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

require('controllers/main/admin/highAvailability/progress_popup_controller');

describe('App.HighAvailabilityProgressPopupController', function () {

  var controller;

  beforeEach(function () {
    controller = App.HighAvailabilityProgressPopupController.create();
  });

  after(function () {
    controller.destroy();
  });

  describe('#startTaskPolling', function () {

    beforeEach(function () {
      sinon.stub(App.updater, 'run', Em.K);
      sinon.stub(App.updater, 'immediateRun', Em.K);
    });

    afterEach(function () {
      App.updater.run.restore();
      App.updater.immediateRun.restore();
    });

    describe('should start task polling', function () {

      beforeEach(function () {
        controller.startTaskPolling(1, 2);
      });

      it('isTaskPolling = true', function () {
        expect(controller.get('isTaskPolling')).to.be.true;
      });

      it('taskInfo.id = 2', function () {
        expect(controller.get('taskInfo.id'), 2);
      });

      it('taskInfo.requestId = 1', function () {
        expect(controller.get('taskInfo.requestId'), 1);
      });

      it('App.updater.run is called once', function () {
        expect(App.updater.run.calledOnce).to.be.true;
      });

      it('App.updater.immediateRun is called once', function () {
        expect(App.updater.immediateRun.calledOnce).to.be.true;
      });

    });

  });

  describe('#stopTaskPolling', function () {

    it('should stop task polling', function () {
      controller.stopTaskPolling();
      expect(controller.get('isTaskPolling')).to.be.false;
    });

  });

  describe('#updateTask', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it('should send polling request', function () {
      controller.updateTask();
      expect(App.ajax.send.calledOnce).to.be.true;
    });

  });

  describe('#updateTaskSuccessCallback', function () {

    beforeEach(function () {
      controller.reopen({
        taskInfo: {}
      });
    });

    var cases = [
        {
          status: 'FAILED',
          isTaskPolling: false
        },
        {
          status: 'COMPLETED',
          isTaskPolling: false
        },
        {
          status: 'TIMEDOUT',
          isTaskPolling: false
        },
        {
          status: 'ABORTED',
          isTaskPolling: false
        },
        {
          status: 'QUEUED',
          isTaskPolling: true
        },
        {
          status: 'IN_PROGRESS',
          isTaskPolling: true
        }
      ],
      tasks = {
        stderr: 'error',
        stdout: 'output',
        output_log: 'output-log.txt',
        error_log: 'error-log.txt'
      },
      title = '{0}polling task if it\'s status is {1}';

    cases.forEach(function (item) {
      var message = title.format(item.isTaskPolling ? '' : 'not ', item.status);
      describe(message, function () {

        beforeEach(function () {
          controller.updateTaskSuccessCallback({
            Tasks: $.extend(tasks, {
              status: item.status
            })
          });
        });

        it('stderr is valid', function () {
          expect(controller.get('taskInfo.stderr')).to.equal('error');
        });

        it('stdout is valid', function () {
          expect(controller.get('taskInfo.stdout')).to.equal('output');
        });

        it('outputLog is valid', function () {
          expect(controller.get('taskInfo.outputLog')).to.equal('output-log.txt');
        });

        it('errorLog is valid', function () {
          expect(controller.get('taskInfo.errorLog')).to.equal('error-log.txt');
        });

        it('isTaskPolling is valid', function () {
          expect(controller.get('isTaskPolling')).to.equal(item.isTaskPolling);
        });

      });
    });

  });

  describe('#getHosts', function () {

    var cases = [
      {
        name: 'background_operations.get_by_request',
        title: 'default background operation polling'
      },
      {
        stageId: 0,
        name: 'common.request.polling',
        stageIdPassed: '0',
        title: 'polling by stage, stageId = 0'
      },
      {
        stageId: 1,
        name: 'common.request.polling',
        stageIdPassed: 1,
        title: 'polling by stage'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          controller.setProperties({
            requestIds: [1, 2],
            stageId: item.stageId
          });
          controller.getHosts();
        });

        it('two requests are sent', function () {
          expect(App.ajax.send.calledTwice).to.be.true;
        });

        it('1st call name is valid', function () {
          expect(App.ajax.send.firstCall.args[0].name).to.equal(item.name);
        });

        it('2nd call name is valid', function () {
          expect(App.ajax.send.secondCall.args[0].name).to.equal(item.name);
        });

        it('1st stageId is valid', function () {
          expect(App.ajax.send.firstCall.args[0].data.stageId).to.eql(item.stageIdPassed);
        });

        it('2nd stageId is valid', function () {
          expect(App.ajax.send.secondCall.args[0].data.stageId).to.eql(item.stageIdPassed);
        });

      });
    });

  });

});
