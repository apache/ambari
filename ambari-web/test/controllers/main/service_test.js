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
require('controllers/main/service');

var mainServiceController;

describe('App.MainServiceController', function () {

  var tests = Em.A([
    {
      isStartStopAllClicked: true,
      content: Em.A([
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'HIVE',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'HDFS',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'TEZ',
          isClientsOnly: true
        })
      ]),
      eStart: true,
      eStop: true,
      mStart: 'mainServiceController StartAll is Disabled 2',
      mStop: 'mainServiceController StopAll is Disabled 2'
    },
    {
      isStartStopAllClicked: false,
      content: Em.A([
        Em.Object.create({
          healthStatus: 'green',
          serviceName: 'HIVE',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'HDFS',
          isClientsOnly: false
        }),
        Em.Object.create({
          healthStatus: 'red',
          serviceName: 'TEZ',
          isClientsOnly: true
        })
      ]),
      eStart: false,
      eStop: false,
      mStart: 'mainServiceController StartAll is Enabled 3',
      mStop: 'mainServiceController StopAll is Enabled 3'
    }

  ]);

  beforeEach(function() {
    mainServiceController = App.MainServiceController.create();
  });

  describe('#isStartAllDisabled', function () {
    tests.forEach(function (test) {
      it(test.mStart, function () {
        mainServiceController = App.MainServiceController.create({
          content: test.content,
          isStartStopAllClicked: test.isStartStopAllClicked
        });
        expect(mainServiceController.get('isStartAllDisabled')).to.equals(test.eStart);
      });
    });
  });

  describe('#isStopAllDisabled', function () {
    tests.forEach(function (test) {
      it(test.mStop, function () {
        mainServiceController = App.MainServiceController.create({
          content: test.content,
          isStartStopAllClicked: test.isStartStopAllClicked
        });
        expect(mainServiceController.get('isStopAllDisabled')).to.equals(test.eStop);
      });
    });
  });

  describe('#isStartStopAllClicked', function () {

    beforeEach(function () {
      sinon.stub(App.router, 'get', function () {
        return Em.Object.create({
          allOperationsCount: 1
        });
      });
    });

    afterEach(function () {
      App.router.get.restore();
    });

    it('should be based on BG ops count', function () {
      expect(mainServiceController.get('isStartStopAllClicked')).to.be.true;
    });

  });

  describe('#cluster', function() {

    var tests = Em.A([
      {
        isLoaded: true,
        cluster: [],
        m: 'cluster is loaded',
        e: {name: 'c1'}
      },
      {
        isLoaded: false,
        cluster: [],
        m: 'cluster is not loaded',
        e: null
      }
    ]).forEach(function(test) {
        it(test.m, function() {
          sinon.stub(App.router, 'get', function(k) {
            if ('clusterController.isClusterDataLoaded' === k) return test.isLoaded;
            return Em.get(App.router, k);
          });
          sinon.stub(App.Cluster, 'find', function() {
            return [test.e];
          });
          var c = mainServiceController.get('cluster');
          App.router.get.restore();
          App.Cluster.find.restore();
          expect(c).to.eql(test.e);
        });
      });

  });

  describe('#startAllService', function() {

    beforeEach(function() {
      sinon.stub(mainServiceController, 'allServicesCall', Em.K);
    });

    afterEach(function() {
      mainServiceController.allServicesCall.restore();
    });

    it('target is disabled', function() {
      var event = {target: {className: 'disabled', nodeType: 1}};
      var r = mainServiceController.startAllService(event);
      expect(r).to.be.null;
    });

    it('parent is disabled', function() {
      var event = {target: {parentElement: {className: 'disabled', nodeType: 1}}};
      var r = mainServiceController.startAllService(event);
      expect(r).to.be.null;
    });

    it('nothing disabled', function() {
      var event = {target: {}}, query = 'query';
      mainServiceController.startAllService(event).onPrimary(query);
      expect(mainServiceController.allServicesCall.calledWith('STARTED', query));
    });

  });

  describe('#stopAllService', function() {

    beforeEach(function() {
      sinon.stub(mainServiceController, 'allServicesCall', Em.K);
    });

    afterEach(function() {
      mainServiceController.allServicesCall.restore();
    });

    it('target is disabled', function() {
      var event = {target: {className: 'disabled', nodeType: 1}};
      var r = mainServiceController.stopAllService(event);
      expect(r).to.be.null;
    });

    it('parent is disabled', function() {
      var event = {target: {parentElement: {className: 'disabled', nodeType: 1}}};
      var r = mainServiceController.stopAllService(event);
      expect(r).to.be.null;
    });

    it('nothing disabled', function() {
      var event = {target: {}}, query = 'query';
      mainServiceController.stopAllService(event).onPrimary(query);
      expect(mainServiceController.allServicesCall.calledWith('STARTED', query));
    });

  });

  describe('#startStopAllService', function() {
    var event = { target: document.createElement("BUTTON") };

    beforeEach(function() {
      sinon.stub(mainServiceController, 'allServicesCall', Em.K);
      sinon.spy(Em.I18n, "t");
    });

    afterEach(function() {
      mainServiceController.allServicesCall.restore();
      Em.I18n.t.restore();
    });

    it ("should confirm stop if state is INSTALLED", function() {
      mainServiceController.startStopAllService(event, "INSTALLED");
      expect(Em.I18n.t.calledWith('services.service.stopAll.confirmMsg')).to.be.ok;
      expect(Em.I18n.t.calledWith('services.service.stop.confirmButton')).to.be.ok;
    });

    it ("should check last checkpoint for NN before confirming stop", function() {
      var mainServiceItemController = App.MainServiceItemController.create({});
      sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
        return true;
      });
      sinon.stub(App.router, 'get', function(k) {
        if ('mainServiceItemController' === k) {
          return mainServiceItemController;
        }
        return Em.get(App.router, k);
      });
      sinon.stub(App.Service, 'find', function() {
        return [{
          serviceName: "HDFS",
          workStatus: "STARTED"
        }];
      });
      mainServiceController.startStopAllService(event, "INSTALLED");
      expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      mainServiceItemController.checkNnLastCheckpointTime.restore();
      App.router.get.restore();
      App.Service.find.restore();
    });

    it ("should confirm start if state is not INSTALLED", function() {
      mainServiceController.startStopAllService(event, "STARTED");
      expect(Em.I18n.t.calledWith('services.service.startAll.confirmMsg')).to.be.ok;
      expect(Em.I18n.t.calledWith('services.service.start.confirmButton')).to.be.ok;
    });
  });

  describe('#allServicesCall', function() {

    beforeEach(function() {
      sinon.stub($, 'ajax', Em.K);
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return false;
        if ('clusterName' === k) return 'tdk';
        return Em.get(App, k);
      });
    });

    afterEach(function() {
      $.ajax.restore();
      App.get.restore();
    });

    it('should do ajax-request', function() {
      var state = 'STARTED',
        query = 'some query';
      mainServiceController.allServicesCall(state, query);
      var params = $.ajax.args[0][0];
      expect(params.type).to.equal('PUT');
      expect(params.url.contains('/clusters/tdk/services?')).to.be.true;
      var data = JSON.parse(params.data);
      expect(data.Body.ServiceInfo.state).to.equal(state);
      expect(data.RequestInfo.context).to.equal(App.BackgroundOperationsController.CommandContexts.START_ALL_SERVICES);
    });

  });

  describe('#allServicesCallErrorCallback', function() {

    it('should set status to FAIL', function() {
      var params = {query: Em.Object.create({status: ''})};
      mainServiceController.allServicesCallErrorCallback({}, {}, '', {}, params);
      expect(params.query.get('status')).to.equal('FAIL');
    });

  });

  describe('#gotoAddService', function() {

    beforeEach(function() {
      sinon.stub(App.router, 'transitionTo', Em.K);
    });

    afterEach(function() {
      App.router.transitionTo.restore();
    });

    it('should not go to wizard', function() {
      mainServiceController.reopen({isAllServicesInstalled: true});
      mainServiceController.gotoAddService();
      expect(App.router.transitionTo.called).to.be.false;
    });

    it('should go to wizard', function() {
      mainServiceController.reopen({isAllServicesInstalled: false});
      mainServiceController.gotoAddService();
      expect(App.router.transitionTo.calledWith('main.serviceAdd')).to.be.true;
    });

  });

});
