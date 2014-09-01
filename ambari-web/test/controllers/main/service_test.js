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

  describe('#isAllServicesInstalled', function() {

    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function() {
        return [
          {serviceName: 's1'},
          {serviceName: 's2'},
          {serviceName: 'HUE'}
        ];
      });
      mainServiceController.set('content', {});
    });

    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should be false if content is not loaded', function() {
      expect(mainServiceController.get('isAllServicesInstalled')).to.be.false;
    });

    var tests = Em.A([
      {
        hue: false,
        content: ['', ''],
        m: 'no hue',
        e: true
      },
      {
        hue: false,
        content: [''],
        m: 'no hue (2)',
        e: false
      },
      {
        hue: true,
        content: ['', '', ''],
        m: 'hue',
        e: true
      },
      {
        hue: false,
        content: ['', ''],
        m: 'hue (2)',
        e: true
      }
    ]).forEach(function(test) {
        it(test.m, function() {
          mainServiceController.reopen({content: {content: test.content}});
          sinon.stub(App, 'get', function(k) {
            if ('supports.hue' == k) return test.hue;
            return Em.get(App, k);
          });
          var r = mainServiceController.get('isAllServicesInstalled');
          App.get.restore();
          expect(r).to.equal(test.e);
        });
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
            if ('clusterController.isLoaded' === k) return test.isLoaded;
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

  describe('#allServicesCallSuccessCallback', function() {

    it('should set status to FAIL', function() {
      var params = {query: Em.Object.create({status: ''})};
      mainServiceController.allServicesCallSuccessCallback({Requests: {id: 1}}, {}, params);
      expect(params.query.get('status')).to.equal('SUCCESS');
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
