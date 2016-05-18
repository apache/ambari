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

function getView() {
  return App.MainAlertDefinitionDetailsView.create({
    initFilters: Em.K,
    controller: Em.Object.create({
      loadAlertInstances: Em.K,
      clearStep: Em.K,
      content: Em.Object.create()
    })
  });
}

describe('App.MainAlertDefinitionDetailsView', function () {

  beforeEach(function () {
    view = getView();
  });

  describe("#willInsertElement()", function() {

    beforeEach(function() {
      sinon.spy(view.get('controller'), 'clearStep');
      sinon.spy(view.get('controller'), 'loadAlertInstances');
      sinon.stub(view, 'loadDefinitionDetails');
    });

    afterEach(function() {
      view.get('controller').clearStep.restore();
      view.get('controller').loadAlertInstances.restore();
      view.loadDefinitionDetails.restore();
    });

    it("clearStep() should be called", function() {
      view.willInsertElement();
      expect(view.get('controller').clearStep.calledOnce).to.be.true;
    });

    it("content is loaded", function() {
      view.set('controller.content.isLoaded', true);
      view.willInsertElement();
      expect(view.get('isLoaded')).to.be.true;
      expect(view.get('controller').loadAlertInstances.calledOnce).to.be.true;
    });

    it("content is not loaded", function() {
      view.set('controller.content.isLoaded', false);
      view.willInsertElement();
      expect(view.get('isLoaded')).to.be.false;
      expect(view.loadDefinitionDetails.calledOnce).to.be.true;
    });
  });

  describe("#loadDefinitionDetails()", function() {
    var mock = {
      updateAlertGroups: function(callback) { callback(); },
      updateAlertDefinitions: function(callback) { callback(); },
      updateAlertDefinitionSummary: function(callback) { callback(); }
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'updateAlertGroups');
      sinon.spy(mock, 'updateAlertDefinitions');
      sinon.spy(mock, 'updateAlertDefinitionSummary');
      sinon.stub(view.get('controller'), 'loadAlertInstances');
      sinon.stub(App.AlertDefinition, 'find').returns({});

      view.loadDefinitionDetails();
    });

    afterEach(function() {
      App.router.get.restore();
      mock.updateAlertGroups.restore();
      mock.updateAlertDefinitions.restore();
      mock.updateAlertDefinitionSummary.restore();
      view.get('controller').loadAlertInstances.restore();
      App.AlertDefinition.find.restore();
    });

    it("updateAlertGroups() should be called", function() {
      expect(mock.updateAlertGroups.calledOnce).to.be.true;
    });

    it("updateAlertDefinitions() should be called", function() {
      expect(mock.updateAlertDefinitions.calledOnce).to.be.true;
    });

    it("updateAlertDefinitionSummary() should be called", function() {
      expect(mock.updateAlertDefinitionSummary.calledOnce).to.be.true;
    });

    it("loadAlertInstances() should be called", function() {
      expect(view.get('isLoaded')).to.be.true;
      expect(view.get('controller.content')).to.eql({});
      expect(view.get('controller').loadAlertInstances.calledOnce).to.be.true;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(view, 'tooltipsUpdater');
      sinon.stub(view, 'filter');
    });

    afterEach(function() {
      view.tooltipsUpdater.restore();
      view.filter.restore();
    });

    it("filter() should be called", function() {
      view.didInsertElement();
      expect(view.filter.calledOnce).to.be.true;
    });

    it("tooltipsUpdater() should be called", function() {
      view.didInsertElement();
      expect(view.tooltipsUpdater.calledOnce).to.be.true;
    });
  });

  describe("#tooltipsUpdater()", function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(App, 'tooltip');
    });

    afterEach(function () {
      Em.run.next.restore();
      App.tooltip.restore();
    });

    it("Em.run.next should be called", function () {
      view.tooltipsUpdater();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#lastDayCount", function () {
    var lastDayCountView;

    beforeEach(function () {
      lastDayCountView = view.get('lastDayCount').create({
        hostName: 'host1',
        parentView: Em.Object.create({
          controller: Em.Object.create()
        })
      });
    });

    describe("#count()", function () {

      it("lastDayAlertsCount is null", function () {
        lastDayCountView.set('parentView.controller.lastDayAlertsCount', null);
        expect(lastDayCountView.get('count')).to.equal(Em.I18n.t('app.loadingPlaceholder'));
      });

      it("lastDayAlertsCount does not contain host", function () {
        lastDayCountView.set('parentView.controller.lastDayAlertsCount', {});
        expect(lastDayCountView.get('count')).to.equal(0);
      });

      it("lastDayAlertsCount is {host1: 1}", function () {
        lastDayCountView.set('parentView.controller.lastDayAlertsCount', {host1: 1});
        expect(lastDayCountView.get('count')).to.equal(1);
      });

    });
  });

  describe("#instanceTableRow", function() {
    var instanceTableRowView;

    beforeEach(function () {
      instanceTableRowView = view.get('instanceTableRow').create();
    });

    describe("#didInsertElement()", function() {

      beforeEach(function() {
        sinon.stub(App, 'tooltip');
      });
      afterEach(function() {
        App.tooltip.restore();
      });

      it("App.tooltip should be called", function() {
        instanceTableRowView.didInsertElement();
        expect(App.tooltip.calledTwice).to.be.true;
      });
    });

    describe("#goToService()", function() {

      beforeEach(function() {
        sinon.stub(App.router, 'transitionTo');
      });
      afterEach(function() {
        App.router.transitionTo.restore();
      });

      it("event is null", function() {
        instanceTableRowView.goToService(null);
        expect(App.router.transitionTo.called).to.be.false;
      });

      it("context is null", function() {
        instanceTableRowView.goToService({context: null});
        expect(App.router.transitionTo.called).to.be.false;
      });

      it("correct context", function() {
        instanceTableRowView.goToService({context: {}});
        expect(App.router.transitionTo.calledWith('main.services.service.summary', {})).to.be.true;
      });
    });

    describe("#goToHostAlerts()", function() {
      var ctrl = Em.Object.create({
        referer: null
      });

      beforeEach(function() {
        sinon.stub(App.router, 'transitionTo');
        sinon.stub(App.router, 'get').returns(ctrl);
      });
      afterEach(function() {
        App.router.transitionTo.restore();
        App.router.get.restore();
      });

      it("event is null", function() {
        instanceTableRowView.goToHostAlerts(null);
        expect(App.router.transitionTo.called).to.be.false;
        expect(ctrl.get('referer')).to.be.null;
      });

      it("context is null", function() {
        instanceTableRowView.goToHostAlerts({context: null});
        expect(App.router.transitionTo.called).to.be.false;
        expect(ctrl.get('referer')).to.be.null;
      });

      it("correct context", function() {
        instanceTableRowView.goToHostAlerts({context: {}});
        expect(ctrl.get('referer')).to.equal('/login');
        expect(App.router.transitionTo.calledWith('main.hosts.hostDetails.alerts', {})).to.be.true;
      });
    });
    describe("#openFullResponse()", function() {

      beforeEach(function() {
        sinon.stub(App, 'showLogsPopup');
      });

      afterEach(function() {
        App.showLogsPopup.restore();
      });

      it("App.showLogsPopup should be called", function() {
        instanceTableRowView.openFullResponse({context: Em.Object.create({text: 'text1'})});
        expect(App.showLogsPopup.calledWith(Em.I18n.t('alerts.instance.fullLogPopup.header'), 'text1')).to.be.true;
      });
    });
  });



  describe("#paginationLeftClass", function() {

    it("startIndex is 2", function() {
      view.set('startIndex', 2);
      expect(view.get('paginationLeftClass')).to.equal('paginate_previous');
    });

    it("startIndex is 1", function() {
      view.set('startIndex', 1);
      expect(view.get('paginationLeftClass')).to.equal('paginate_disabled_previous');
    });

    it("startIndex is 0", function() {
      view.set('startIndex', 0);
      expect(view.get('paginationLeftClass')).to.equal('paginate_disabled_previous');
    });
  });

  describe("#paginationRightClass", function() {

    it("endIndex more than filteredCount", function() {
      view.reopen({
        endIndex: 4,
        filteredCount: 3
      });
      expect(view.get('paginationRightClass')).to.equal('paginate_disabled_next');
    });

    it("endIndex equal to filteredCount", function() {
      view.reopen({
        endIndex: 4,
        filteredCount: 4
      });
      expect(view.get('paginationRightClass')).to.equal('paginate_disabled_next');
    });

    it("endIndex less than filteredCount", function() {
      view.reopen({
        endIndex: 3,
        filteredCount: 4
      });
      view.propertyDidChange('paginationRightClass');
      expect(view.get('paginationRightClass')).to.equal('paginate_next');
    });
  });

});

function getInstanceView() {
  return App.AlertInstanceServiceHostView.create();
}

describe('App.AlertInstanceServiceHostView', function () {

  var instanceView;

  beforeEach(function() {
    instanceView = getInstanceView();
  });

  App.TestAliases.testAsComputedAnd(getInstanceView(), 'showSeparator', ['instance.serviceDisplayName', 'instance.hostName']);

  describe("#serviceIsLink", function() {

    beforeEach(function() {
      sinon.stub(App, 'get').returns(['S1']);
    });
    afterEach(function() {
      App.get.restore();
    });

    it("service belongs to all", function() {
      instanceView.set('instance', Em.Object.create({
        service: Em.Object.create({
          serviceName: 'S1'
        })
      }));
      expect(instanceView.get('serviceIsLink')).to.be.true;
    });
    it("service does not belong to all", function() {
      instanceView.set('instance', Em.Object.create({
        service: Em.Object.create({
          serviceName: 'S2'
        })
      }));
      expect(instanceView.get('serviceIsLink')).to.be.false;
    });
  });
});
