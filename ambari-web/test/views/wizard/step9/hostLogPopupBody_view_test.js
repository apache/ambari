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
require('views/wizard/step9/hostLogPopupBody_view');
var view;

describe('App.WizardStep9HostLogPopupBodyView', function() {

  beforeEach(function() {
    view = App.WizardStep9HostLogPopupBodyView.create({
      parentView: Em.Object.create({
        host: Em.Object.create()
      })
    });
  });

  describe('#isHeartbeatLost', function() {
    it('should depends on parentView.host.status', function() {
      view.set('parentView.host.status', 'success');
      expect(view.get('isHeartbeatLost')).to.equal(false);
      view.set('parentView.host.status', 'heartbeat_lost');
      expect(view.get('isHeartbeatLost')).to.equal(true);
    });
  });

  describe('#isNoTasksScheduled', function() {
    it('should be same to parentView.host.isNoTasksForInstall', function() {
      view.set('parentView.host.isNoTasksForInstall', true);
      expect(view.get('isNoTasksScheduled')).to.equal(true);
      view.set('parentView.host.isNoTasksForInstall', false);
      expect(view.get('isNoTasksScheduled')).to.equal(false);
    });
  });

  describe('#visibleTasks', function() {
    Em.A([
        {
          value: 'pending',
          f: ['pending', 'queued']
        },
        {
          value: 'in_progress',
          f: ['in_progress']
        },
        {
          value: 'failed',
          f: ['failed']
        },
        {
          value: 'completed',
          f: ['completed']
        },
        {
          value: 'aborted',
          f: ['aborted']
        },
        {
          value: 'timedout',
          f: ['timedout']
        },
        {
          value: 'all'
        }
      ]).forEach(function(test) {
        it(test.value, function() {
          view.reopen({
            category: Em.Object.create({value: test.value}),
            tasks: Em.A([
              {status: 'pending', isVisible: false},
              {status: 'queued', isVisible: false},
              {status: 'in_progress', isVisible: false},
              {status: 'failed', isVisible: false},
              {status: 'completed', isVisible: false},
              {status: 'aborted', isVisible: false},
              {status: 'timedout', isVisible: false}
            ])
          });
          view.visibleTasks();
          var visibleTasks = view.get('tasks').filter(function(task) {
            if (test.f) {
              return test.f.contains(task.status);
            }
            return true;
          });
          expect(visibleTasks.everyProperty('isVisible', true)).to.equal(true);
        });
    });
  });

  describe('#backToTaskList', function() {
    it('should call destroyClipBoard', function() {
      sinon.stub(view, 'destroyClipBoard', Em.K);
      view.backToTaskList();
      expect(view.destroyClipBoard.calledOnce).to.equal(true);
      view.destroyClipBoard.restore();
    });
    it('should set isLogWrapHidden to true', function() {
      view.set('isLogWrapHidden', false);
      view.backToTaskList();
      expect(view.get('isLogWrapHidden')).to.equal(true);
    });
  });

  describe('#getStartedTasks', function() {
    it('should return tasks with some status', function() {
      var logTasks = Em.A([
        {Tasks: {}}, {Tasks: {status: 's'}}, {Tasks: {status: null}}, {Tasks: {status: 'v'}}
      ]);
      expect(view.getStartedTasks({logTasks: logTasks}).length).to.equal(2);
    });
  });

  describe('#openedTask', function() {
    it('should return currently open task', function() {
      var task = Em.Object.create({id: 2});
      view.reopen({
        tasks: Em.A([
          Em.Object.create({id: 1}),
          Em.Object.create({id: 3}),
          task,
          Em.Object.create({id: 4})
        ])
      });
      view.set('parentView.c', {currentOpenTaskId: 2});
      expect(view.get('openedTask.id')).to.equal(2);
    });
  });

  describe('#tasks', function() {
    var testTask = {
      Tasks: {
        status: 'init',
        id: 1,
        request_id: 2,
        role: 'PIG',
        stderr: 'stderr',
        stdout: 'stdout',
        host_name: 'host1',
        command: 'Cmd',
        command_detail: 'TEST SERVICE/COMPONENT_DESCRIPTION'
      }
    };

    it('should map tasks', function() {
      view.set('parentView.host.logTasks', [testTask]);
      var t = view.get('tasks');
      expect(t.length).to.equal(1);
      var first = t[0];
      expect(first.get('id')).to.equal(1);
      expect(first.get('requestId')).to.equal(2);
      expect(first.get('command')).to.equal('cmd');
      expect(first.get('commandDetail')).to.equal(' Test Component Description');
      expect(first.get('role')).to.equal('Pig');
      expect(first.get('stderr')).to.equal('stderr');
      expect(first.get('stdout')).to.equal('stdout');
      expect(first.get('isVisible')).to.equal(true);
      expect(first.get('hostName')).to.equal('host1');
    });

    it('should set cog icon', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'pending';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-cog');
    });

    it('should set cog icon (2)', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'queued';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-cog');
    });

    it('should set cogs icon', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'in_progress';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-cogs');
    });

    it('should set ok icon', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'completed';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-ok');
    });

    it('should set icon-exclamation-sign icon', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'failed';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-exclamation-sign');
    });

    it('should set minus icon', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'aborted';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-minus');
    });

    it('should set time icon', function() {
      var t = Em.copy(testTask);
      t.Tasks.status = 'timedout';
      view.set('parentView.host.logTasks', [t]);
      var first = view.get('tasks')[0];
      expect(first.get('icon')).to.equal('icon-time');
    });

  });

  describe('#toggleTaskLog', function() {
    it('isLogWrapHidden is true', function() {
      var taskInfo = {
        id: 1,
        requestId: 2
      };
      view.set('isLogWrapHidden', true);
      view.set('parentView.c', Em.Object.create({loadCurrentTaskLog: Em.K}));
      sinon.spy(view.get('parentView.c'), 'loadCurrentTaskLog');
      view.toggleTaskLog({context: taskInfo});
      expect(view.get('isLogWrapHidden')).to.equal(false);
      expect(view.get('parentView.c.currentOpenTaskId')).to.equal(taskInfo.id);
      expect(view.get('parentView.c.currentOpenTaskRequestId')).to.equal(taskInfo.requestId);
      expect(view.get('parentView.c').loadCurrentTaskLog.calledOnce).to.equal(true);
      view.get('parentView.c').loadCurrentTaskLog.restore();
    });
    it('isLogWrapHidden is false', function() {
      var taskInfo = {};
      view.set('isLogWrapHidden', false);
      view.set('parentView.c', Em.Object.create({loadCurrentTaskLog: Em.K}));
      view.toggleTaskLog({context: taskInfo});
      expect(view.get('isLogWrapHidden')).to.equal(true);
      expect(view.get('parentView.c.currentOpenTaskId')).to.equal(0);
      expect(view.get('parentView.c.currentOpenTaskRequestId')).to.equal(0);
    });
  });

});