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

require('controllers/main/alerts_controller');
require('models/alert');

describe('MainAlertsController', function () {

  var controller = App.MainAlertsController.create();

  describe('#loadAlerts()', function () {

    before(function () {
      sinon.stub(controller, 'getFromServer', Em.K);
    });
    after(function () {
      controller.getFromServer.restore();
    });

    it('getFromServer should be called', function () {
      controller.set('resourceName', null);
      controller.set('isLoaded', true);
      controller.set('resourceType', null);
      controller.loadAlerts('name1', 'type1');
      expect(controller.get("isLoaded")).to.be.false;
      expect(controller.get("resourceName")).to.equal('name1');
      expect(controller.get("resourceType")).to.equal('type1');
      expect(controller.getFromServer.calledOnce).to.be.true;
    });
  });

  describe('#update()', function () {

    var clock;

    beforeEach(function () {
      clock = sinon.useFakeTimers();
      sinon.stub(controller, 'getFromServer', Em.K);
      sinon.spy(controller, 'update');
    });
    afterEach(function () {
      clock.restore();
      controller.getFromServer.restore();
      controller.update.restore();
    });

    it('isUpdating = true', function () {
      controller.set('isUpdating', true);
      expect(controller.get("updateTimer")).not.to.be.null;
      clock.tick(App.componentsUpdateInterval);
      expect(controller.getFromServer.calledOnce).to.be.true;
      expect(controller.update.calledTwice).to.be.true;
    });
    it('isUpdating = false', function () {
      controller.set('isUpdating', false);
      expect(controller.update.calledOnce).to.be.true;
    });
  });

  describe('#getFromServer()', function () {
    var obj = Em.Object.create({isNagiosInstalled: false});

    beforeEach(function () {
      sinon.stub(controller, 'getAlertsByService', Em.K);
      sinon.stub(controller, 'getAlertsByHost', Em.K);
      sinon.stub(App.router, 'get', function() {return obj.get('isNagiosInstalled')});
    });
    afterEach(function () {
      controller.getAlertsByService.restore();
      controller.getAlertsByHost.restore();
      App.router.get.restore();
    });

    it('Nagios is not installed', function () {
      obj.set('isNagiosInstalled', false);
      controller.set('isLoaded', false);
      controller.getFromServer();
      expect(controller.get('isLoaded')).to.be.true;
      controller.set('isLoaded', false);
    });
    it('Nagios installed, SERVICE resource type', function () {
      obj.set('isNagiosInstalled', true);
      controller.set('resourceType', 'SERVICE');
      controller.getFromServer();
      expect(controller.get('isLoaded')).to.be.false;
      expect(controller.getAlertsByService.calledOnce).to.be.true;
    });
    it('Nagios installed, HOST resource type', function () {
      obj.set('isNagiosInstalled', true);
      controller.set('resourceType', 'HOST');
      controller.getFromServer();
      expect(controller.get('isLoaded')).to.be.false;
      expect(controller.getAlertsByHost.calledOnce).to.be.true;
    });
    it('Nagios installed, unknown resource type', function () {
      obj.set('isNagiosInstalled', true);
      controller.set('resourceType', 'RS1');
      controller.getFromServer();
      expect(controller.get('isLoaded')).to.be.false;
      expect(controller.getAlertsByService.called).to.be.false;
      expect(controller.getAlertsByHost.called).to.be.false;
    });
  });

  describe('#getAlertsByHost()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('resourceName is null', function () {
      controller.set('resourceName', null);
      expect(controller.getAlertsByHost()).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it('resourceName is correct', function () {
      controller.set('resourceName', 'host1');
      expect(controller.getAlertsByHost()).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#getAlertsByService()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('resourceName is null', function () {
      controller.set('resourceName', null);
      expect(controller.getAlertsByService()).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it('resourceName is correct', function () {
      controller.set('resourceName', 'service1');
      expect(controller.getAlertsByService()).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#getAlertsSuccessCallback()', function () {

    var testCases = [
      {
        title: 'data is null',
        data: null,
        result: []
      },
      {
        title: 'data.alerts is null',
        data: {alerts: null},
        result: []
      },
      {
        title: 'data.alerts.detail is null',
        data: {alerts: {detail: null}},
        result: []
      },
      {
        title: 'data.alerts.detail is empty',
        data: {alerts: {detail: []}},
        result: []
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('isLoaded', false);
        controller.getAlertsSuccessCallback(test.data);
        expect(controller.get('alerts')).to.eql(test.result);
        expect(controller.get('isLoaded')).to.be.true;
      });
    });

    var data = {alerts: {detail: [
      {
        description: 't1',
        service_name: "s1",
        status_time: 1,
        status: 'OK',
        output: 'o1',
        host_name: 'h1',
        last_status_time: 1
      }
    ]}};
    var testCasesOfStatus = [
      {
        title: 'data.alerts.detail is correct, OK status',
        status: 'OK',
        result: '0'
      },
      {
        title: 'data.alerts.detail is correct, WARNING status',
        status: 'WARNING',
        result: '1'
      },
      {
        title: 'data.alerts.detail is correct, CRITICAL status',
        status: 'CRITICAL',
        result: '2'
      },
      {
        title: 'data.alerts.detail is correct, PASSIVE status',
        status: 'PASSIVE',
        result: '3'
      },
      {
        title: 'data.alerts.detail is correct, unknown status',
        status: '',
        result: '4'
      }
    ];
    testCasesOfStatus.forEach(function (test) {
      it(test.title, function () {
        controller.set('isLoaded', false);
        data.alerts.detail[0].status = test.status;
        controller.getAlertsSuccessCallback(data);
        expect(controller.get('alerts.length')).to.equal(1);
        expect(controller.get('alerts').objectAt(0).get('status')).to.equal(test.result);
        expect(controller.get('isLoaded')).to.be.true;
      });
    });
  });

  describe('#getAlertsErrorCallback()', function () {
    it('isLoaded was false', function () {
      controller.set('isLoaded', false);
      controller.getAlertsErrorCallback();
      expect(controller.get('isLoaded')).to.be.true;
    });
    it('isLoaded was true', function () {
      controller.set('isLoaded', true);
      controller.getAlertsErrorCallback();
      expect(controller.get('isLoaded')).to.be.true;
    });
  });
});