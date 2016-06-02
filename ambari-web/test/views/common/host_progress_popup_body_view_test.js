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
var view;

describe('App.HostProgressPopupBodyView', function () {

  beforeEach(function () {
    view = App.HostProgressPopupBodyView.create({
      controller: Em.Object.create({
        dataSourceController: Em.Object.create({}),
        setBackgroundOperationHeader: Em.K,
        hosts: []
      }),
      parentView: Em.Object.create()
    });
  });

  describe('#switchLevel', function () {

    var map = App.HostProgressPopupBodyView.create().get('customControllersSwitchLevelMap');

    Object.keys(map).forEach(function (controllerName) {
      var methodName = map [controllerName];
      var levelName = 'REQUESTS_LIST';

      beforeEach(function () {
        sinon.stub(view, methodName, Em.K);
      });

      afterEach(function () {
        view[methodName].restore();
      });

      it('should call ' + methodName, function () {
        view.set('controller.dataSourceController.name', controllerName);
        view.switchLevel(levelName);
        expect(view[methodName].args[0]).to.eql([levelName]);
      });

    });

  });

  describe('_determineRoleRelation', function() {
    var cases;

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([{componentName: 'DATANODE'}]);
      sinon.stub(App.StackService, 'find').returns([{serviceName: 'HDFS'}])
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.StackService.find.restore();
    });

    cases = [
      {
        task: { role: 'HDFS_SERVICE_CHECK'},
        m: 'Role is HDFS_SERVICE_CHECK',
        e: {
          type: 'service',
          value: 'HDFS'
        }
      },
      {
        task: { role: 'DATANODE'},
        m: 'Role is DATANODE',
        e: {
          type: 'component',
          value: 'DATANODE'
        }
      },
      {
        task: { role: 'UNDEFINED'},
        m: 'Role is UNDEFINED',
        e: false
      }
    ];

    cases.forEach(function(test) {
      it(test.m, function() {
        view.reopen({
          currentHost: Em.Object.create({
            logTasks: [
              { Tasks: { id: 1, role: test.task.role }}
            ]
          })
        });

        var ret = view._determineRoleRelation(Em.Object.create({ id: 1 }));
        expect(ret).to.be.eql(test.e);
      });
    });
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(view, 'updateHostInfo', Em.K);
      view.didInsertElement();
    });

    afterEach(function () {
      view.updateHostInfo.restore();
    });

    it('should display relevant info', function () {
      expect(view.updateHostInfo.calledOnce).to.be.true;
    });

  });

  describe('#preloadHostModel', function() {
    describe('When Log Search installed', function() {

      beforeEach(function() {
        this.HostModelStub = sinon.stub(App.Host, 'find');
        this.isLogSearchInstalled = sinon.stub(view, 'get').withArgs('isLogSearchInstalled');
        this.logSearchSupported = sinon.stub(App, 'get').withArgs('supports.logSearch');
        this.updateCtrlStub = sinon.stub(App.router.get('updateController'), 'updateLogging');
      });

      afterEach(function () {
        App.Host.find.restore();
        view.get.restore();
        App.get.restore();
        App.router.get('updateController').updateLogging.restore();
      });

      [
        {
          hostName: 'host1',
          logSearchSupported: true,
          isLogSearchInstalled: true,
          requestFailed: false,
          hosts: [
            {
              hostName: 'host2'
            }
          ],
          e: {
            updateLoggingCalled: true
          },
          m: 'Host absent, log search installed and supported'
        },
        {
          hostName: 'host1',
          logSearchSupported: true,
          isLogSearchInstalled: true,
          requestFailed: false,
          hosts: [
            {
              hostName: 'host1'
            }
          ],
          e: {
            updateLoggingCalled: false
          },
          m: 'Host present, log search installed and supported'
        },
        {
          hostName: 'host1',
          logSearchSupported: false,
          isLogSearchInstalled: true,
          requestFailed: false,
          hosts: [
            {
              hostName: 'host1'
            }
          ],
          e: {
            updateLoggingCalled: false
          },
          m: 'Host present, log search installed and support is off'
        },
        {
          hostName: 'host1',
          logSearchSupported: true,
          isLogSearchInstalled: true,
          requestFailed: true,
          hosts: [
            {
              hostName: 'host2'
            }
          ],
          e: {
            updateLoggingCalled: true
          },
          m: 'Host is absent, log search installed and supported, update request was failed'
        },
        {
          hostName: 'host1',
          logSearchSupported: true,
          isLogSearchInstalled: false,
          requestFailed: true,
          hosts: [
            {
              hostName: 'host2'
            }
          ],
          e: {
            updateLoggingCalled: false
          },
          m: 'Host is absent, log search not installed and supported'
        }
      ].forEach(function(test) {

        it('hostInfoLoaded should be true on init', function() {
          expect(Em.get(view, 'hostInfoLoaded')).to.be.true;
        });

        describe(test.m, function () {

          beforeEach(function () {
            this.HostModelStub.returns(test.hosts);
            this.isLogSearchInstalled.returns(test.isLogSearchInstalled);
            this.logSearchSupported.returns(test.logSearchSupported);
            if (test.requestFailed) {
              this.updateCtrlStub.returns($.Deferred().reject().promise());
            } else {
              this.updateCtrlStub.returns($.Deferred().resolve().promise());
            }
            Em.set(view, 'hostInfoLoaded', false);
            view.preloadHostModel(test.hostName);
          });

          it('updateLogging call validation', function() {
            expect(App.router.get('updateController').updateLogging.called).to.be.equal(test.e.updateLoggingCalled);
          });

          it('in result hostInfoLoaded should be always true', function() {
            expect(Em.get(view, 'hostInfoLoaded')).to.be.true;
          });

        });
      });
    });
  });

  describe("#resetState()", function () {

    beforeEach(function() {
      sinon.stub(view.get('controller'), 'setBackgroundOperationHeader');
      sinon.stub(view, 'setOnStart');
      sinon.stub(view, 'rerender');
      sinon.stub(view, 'updateSelectView');
    });

    afterEach(function() {
      view.get('controller').setBackgroundOperationHeader.restore();
      view.setOnStart.restore();
      view.rerender.restore();
      view.updateSelectView.restore();
    });

    it("should set properties of parentView", function() {
      view.set('parentView.isOpen', true);
      expect(JSON.stringify(view.get('parentView'))).to.be.equal(JSON.stringify({
        "isOpen": true,
        "isLogWrapHidden": true,
        "isTaskListHidden": true,
        "isHostListHidden": true,
        "isServiceListHidden": false
      }));
    });

    it("setBackgroundOperationHeader should be called", function() {
      view.set('parentView.isOpen', true);
      expect(view.get('controller').setBackgroundOperationHeader.calledWith(false)).to.be.true;
    });

    it("controller.hosts should be empty", function() {
      view.set('controller.hosts', [{}]);
      view.set('parentView.isOpen', true);
      expect(view.get('controller.hosts')).to.be.empty;
    });

    it("setOnStart should be called", function() {
      view.set('parentView.isOpen', true);
      expect(view.setOnStart.calledOnce).to.be.true;
    });

    it("rerender should be called", function() {
      view.set('parentView.isOpen', true);
      expect(view.rerender.calledOnce).to.be.true;
    });
  });
});
