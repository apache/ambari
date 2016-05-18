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
require('views/main/service/info/summary');
var batchUtils = require('utils/batch_scheduled_requests');

describe('App.MainServiceInfoSummaryView', function() {

  var view = App.MainServiceInfoSummaryView.create({
    monitorsLiveTextView: Em.View.create(),
    controller: Em.Object.create({
      content: Em.Object.create({
        id: 'HDFS',
        serviceName: 'HDFS',
        hostComponents: []
      }),
      getActiveWidgetLayout: Em.K,
      loadWidgetLayouts: Em.K
    }),
    alertsController: Em.Object.create(),
    service: Em.Object.create()
  });

  App.TestAliases.testAsComputedAlias(view, 'servicesHaveClients', 'App.services.hasClient', 'boolean');

  App.TestAliases.testAsComputedAlias(view, 'serviceName', 'service.serviceName', 'string');

  App.TestAliases.testAsComputedAlias(view, 'alertsCount', 'controller.content.alertsCount', 'number');

  App.TestAliases.testAsComputedAlias(view, 'hasCriticalAlerts', 'controller.content.hasCriticalAlerts', 'boolean');

  describe('#servers', function () {
    it('services shouldn\'t have servers except FLUME and ZOOKEEPER', function () {
      expect(view.get('servers')).to.be.empty;
    });

    describe('if one server exists then first server should have isComma and isAnd property false', function () {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          id: 'ZOOKEEPER',
          serviceName: 'ZOOKEEPER',
          hostComponents: [
            Em.Object.create({
              displayName: '',
              isMaster: true
            })
          ]
        }));
      });

      it('isComma', function () {
        expect(view.get('servers').objectAt(0).isComma).to.equal(false);});

      it('isAnd', function () {
        expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      });
    });

    describe('if more than one servers exist then first server should have isComma - true and isAnd - false', function() {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          id: 'ZOOKEEPER',
          serviceName: 'ZOOKEEPER',
          hostComponents: [
            Em.Object.create({
              displayName: '',
              isMaster: true
            }),
            Em.Object.create({
              displayName: '',
              isMaster: true
            })
          ]
        }));
      });

      it('0 isComma', function () {
        expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      });

      it('0 isAnd', function () {
        expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      });

      it('1 isComma', function () {
        expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      });

      it('1 isAnd', function () {
        expect(view.get('servers').objectAt(1).isAnd).to.equal(false);
      });

    });

    describe('if more than two servers exist then second server should have isComma - false and isAnd - true', function () {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          id: 'ZOOKEEPER',
          serviceName: 'ZOOKEEPER',
          hostComponents: [
            Em.Object.create({
              displayName: '',
              isMaster: true
            }),
            Em.Object.create({
              displayName: '',
              isMaster: true
            }),
            Em.Object.create({
              displayName: '',
              isMaster: true
            })
          ]
        }));
      });

      it('0 isComma', function () {
        expect(view.get('servers').objectAt(0).isComma).to.equal(true);
      });

      it('0 isAnd', function () {
        expect(view.get('servers').objectAt(0).isAnd).to.equal(false);
      });

      it('1 isComma', function () {
        expect(view.get('servers').objectAt(1).isComma).to.equal(false);
      });

      it('1 isAnd', function () {
        expect(view.get('servers').objectAt(1).isAnd).to.equal(true);
      });

      it('2 isComma', function () {
        expect(view.get('servers').objectAt(2).isComma).to.equal(false);
      });

      it('2 isAnd', function () {
        expect(view.get('servers').objectAt(2).isAnd).to.equal(false);
      });

    });

  });

  describe('#hasAlertDefinitions', function () {

    beforeEach(function () {
      sinon.stub(App.AlertDefinition, 'find', function () {
        return [
          {
            serviceName: 'HDFS'
          },
          {
            serviceName: 'YARN'
          }
        ];
      });
    });

    afterEach(function () {
      App.AlertDefinition.find.restore();
    });

    it('should return true if at least one alert definition for this service exists', function () {
      view.set('controller.content', Em.Object.create({
        serviceName: 'HDFS'
      }));
      expect(view.get('hasAlertDefinitions')).to.be.true;
    });

    it('should return false if there is no alert definition for this service', function () {
      view.set('controller.content', Em.Object.create({
        serviceName: 'ZOOKEEPER'
      }));
      expect(view.get('hasAlertDefinitions')).to.be.false;
    });

  });

  describe("#restartAllStaleConfigComponents", function () {

    describe('trigger restartAllServiceHostComponents', function () {

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          serviceName: "HDFS"
        }));
        view.set('service', Em.Object.create({
          displayName: 'HDFS'
        }));
        sinon.stub(batchUtils, "restartAllServiceHostComponents", Em.K);
      });

      afterEach(function () {
        batchUtils.restartAllServiceHostComponents.restore();
      });

      it('batch request is started', function () {
        view.restartAllStaleConfigComponents().onPrimary();
        expect(batchUtils.restartAllServiceHostComponents.calledOnce).to.equal(true);
      });

    });

    describe('trigger check last check point warning before triggering restartAllServiceHostComponents', function () {

      var mainServiceItemController;

      beforeEach(function () {
        view.set('controller.content', Em.Object.create({
          serviceName: "HDFS",
          hostComponents: [{
            componentName: 'NAMENODE',
            workStatus: 'STARTED'
          }],
          restartRequiredHostsAndComponents: {
            "host1": ['NameNode'],
            "host2": ['DataNode', 'ZooKeeper']
          }
        }));
        view.set('service', Em.Object.create({
          displayName: 'HDFS'
        }));
        mainServiceItemController = App.MainServiceItemController.create({});
        sinon.stub(mainServiceItemController, 'checkNnLastCheckpointTime', function() {
          return true;
        });
        sinon.stub(App.router, 'get', function(k) {
          if ('mainServiceItemController' === k) {
            return mainServiceItemController;
          }
          return Em.get(App.router, k);
        });
      });

      afterEach(function () {
        mainServiceItemController.checkNnLastCheckpointTime.restore();
        App.router.get.restore();
      });

      it('NN Last CheckPoint is checked', function () {
        view.restartAllStaleConfigComponents();
        expect(mainServiceItemController.checkNnLastCheckpointTime.calledOnce).to.equal(true);
      });

    });

  });

  describe("#setComponentsContent()", function() {

    beforeEach(function() {
      sinon.stub(Em.run, 'next', Em.clb);
      sinon.stub(view, 'updateComponentList');
      view.set('service', Em.Object.create({
        hostComponents: [],
        slaveComponents: [],
        clientComponents: []
      }));
      view.setProperties({
        mastersLength: 0,
        slavesLength: 0,
        clientsLength: 0,
        mastersObj: ['master'],
        slavesObj: ['slave'],
        clientObj: ['client']
      });
    });
    afterEach(function() {
      Em.run.next.restore();
      view.updateComponentList.restore();
    });

    it("service is null", function() {
      view.set('service', null);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.called).to.be.false
    });

    it("update master length", function() {
      view.set('mastersLength', 1);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.calledWith(['master'], [])).to.be.true;
      expect(view.get('mastersLength')).to.be.equal(0);
    });

    it("update slave length", function() {
      view.set('slavesLength', 1);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.calledWith(['slave'], [])).to.be.true;
      expect(view.get('slavesLength')).to.be.equal(0);
    });

    it("update client length", function() {
      view.set('clientsLength', 1);
      view.setComponentsContent();
      expect(Em.run.next.calledOnce).to.be.true;
      expect(view.updateComponentList.calledWith(['client'], [])).to.be.true;
      expect(view.get('clientsLength')).be.equal(0);
    });
  });

  describe("#clientsHostText", function() {

    it("no installed clients", function() {
      view.set('controller.content.installedClients', []);
      view.propertyDidChange('clientsHostText');
      expect(view.get('clientsHostText')).to.be.empty;
    });

    it("has many clients", function() {
      view.set('controller.content.installedClients', [1]);
      view.reopen({
        hasManyClients: true
      });
      view.propertyDidChange('clientsHostText');
      expect(view.get('clientsHostText')).to.be.equal(Em.I18n.t('services.service.summary.viewHosts'));
    });

    it("otherwise", function() {
      view.set('controller.content.installedClients', [1]);
      view.reopen({
        hasManyClients: false
      });
      view.propertyDidChange('clientsHostText');
      expect(view.get('clientsHostText')).to.be.equal(Em.I18n.t('services.service.summary.viewHost'));
    });
  });

  describe("#historyServerUI", function() {

    it("singleNodeInstall is true", function() {
      App.set('singleNodeInstall', true);
      App.set('singleNodeAlias', 'alias');
      view.propertyDidChange('historyServerUI');
      expect(view.get('historyServerUI')).to.equal("http://alias:19888");
    });

    it("singleNodeInstall is false", function () {
      App.set('singleNodeInstall', false);
      view.set('controller.content', Em.Object.create({
        hostComponents: [
          Em.Object.create({
            isMaster: true,
            host: Em.Object.create({
              publicHostName: 'host1'
            })
          })
        ]
      }));
      view.propertyDidChange('historyServerUI');
      expect(view.get('historyServerUI')).to.equal("http://host1:19888");
    });
  });

  describe("#serversHost", function() {

    it("should return empty object", function() {
      view.set('controller.content', Em.Object.create({
        id: 'S1',
        hostComponents: []
      }));
      view.propertyDidChange('serversHost');
      expect(view.get('serversHost')).to.be.empty;
    });

    it("should return server object", function() {
      view.set('controller.content', Em.Object.create({
        id: 'ZOOKEEPER',
        hostComponents: [
          Em.Object.create({
            isMaster: true
          })
        ]
      }));
      view.propertyDidChange('serversHost');
      expect(view.get('serversHost')).to.eql(Em.Object.create({
        isMaster: true
      }));
    });
  });

  describe("#updateComponentList()", function() {

    it("add components to empty source", function() {
      var source = [],
          data = [{id: 1}];
      view.updateComponentList(source, data);
      expect(source.mapProperty('id')).to.eql([1]);
    });

    it("add components to exist source", function() {
      var source = [{id: 1}],
        data = [{id: 1}, {id: 2}];
      view.updateComponentList(source, data);
      expect(source.mapProperty('id')).to.eql([1, 2]);
    });

    it("remove components from exist source", function() {
      var source = [{id: 1}, {id: 2}],
        data = [{id: 1}];
      view.updateComponentList(source, data);
      expect(source.mapProperty('id')).to.eql([1]);
    });
  });

  describe("#componentNameView", function () {
    var componentNameView;

    beforeEach(function () {
      componentNameView = view.get('componentNameView').create();
    });

    describe("#displayName", function () {

      it("component is MYSQL_SERVER", function () {
        componentNameView.set('comp', Em.Object.create({
          componentName: 'MYSQL_SERVER'
        }));
        componentNameView.propertyDidChange('displayName');
        expect(componentNameView.get('displayName')).to.equal(Em.I18n.t('services.hive.databaseComponent'));
      });

      it("any component", function () {
        componentNameView.set('comp', Em.Object.create({
          componentName: 'C1',
          displayName: 'c1'
        }));
        componentNameView.propertyDidChange('displayName');
        expect(componentNameView.get('displayName')).to.equal('c1');
      });
    });
  });


  describe("#getServiceModel()", function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns({serviceName: 'S1'});
      sinon.stub(App.HDFSService, 'find').returns([{serviceName: 'HDFS'}]);
    });
    afterEach(function() {
      App.Service.find.restore();
      App.HDFSService.find.restore();
    });

    it("HDFS service", function() {
      expect(view.getServiceModel('HDFS')).to.eql({serviceName: 'HDFS'});
    });

    it("Simple model service", function() {
      expect(view.getServiceModel('S1')).to.eql({serviceName: 'S1'});
    });
  });

  describe("#updateComponentInformation()", function () {
    it("should count hosts and components", function () {
      view.set('controller.content.restartRequiredHostsAndComponents', {
        'host1': ['c1', 'c2']
      });
      view.updateComponentInformation();
      expect(view.get('componentsCount')).to.equal(2);
      expect(view.get('hostsCount')).to.equal(1);
    });
  });

  describe("#rollingRestartSlaveComponentName ", function() {

    beforeEach(function() {
      sinon.stub(batchUtils, 'getRollingRestartComponentName').returns('C1');
    });
    afterEach(function() {
      batchUtils.getRollingRestartComponentName.restore();
    });

    it("should returns component name", function() {
      view.set('serviceName', 'S1');
      view.propertyDidChange('rollingRestartSlaveComponentName');
      expect(view.get('rollingRestartSlaveComponentName')).to.equal('C1');
    });
  });

  describe("#rollingRestartActionName ", function() {

    beforeEach(function() {
      sinon.stub(App.format, 'role').returns('C1');
    });
    afterEach(function() {
      App.format.role.restore();
    });

    it("rollingRestartSlaveComponentName is set", function() {
      view.reopen({
        rollingRestartSlaveComponentName: 'C1'
      });
      view.propertyDidChange('rollingRestartActionName');
      expect(view.get('rollingRestartActionName')).to.equal(Em.I18n.t('rollingrestart.dialog.title').format('C1'));
    });

    it("rollingRestartSlaveComponentName is null", function() {
      view.reopen({
        rollingRestartSlaveComponentName: null
      });
      view.propertyDidChange('rollingRestartActionName');
      expect(view.get('rollingRestartActionName')).to.be.null;
    });
  });

  describe("#rollingRestartStaleConfigSlaveComponents() ", function() {

    beforeEach(function() {
      sinon.stub(batchUtils, 'launchHostComponentRollingRestart');
    });
    afterEach(function() {
      batchUtils.launchHostComponentRollingRestart.restore();
    });

    it("launchHostComponentRollingRestart should be called", function() {
      view.get('service').setProperties({
        displayName: 's1',
        passiveState: 'ON'
      });
      view.rollingRestartStaleConfigSlaveComponents({context: 'C1'});
      expect(batchUtils.launchHostComponentRollingRestart.calledWith(
        'C1', 's1', true, true
      )).to.be.true;
    });
  });

  describe("#constructGraphObjects()", function() {
    var mock = Em.Object.create({
      isServiceWithWidgets: false
    });

    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns(mock);
      sinon.stub(view, 'getUserPref').returns({
        complete: function(callback){callback();}
      })
    });
    afterEach(function() {
      App.StackService.find.restore();
      view.getUserPref.restore();
    });

    it("metrics not loaded", function() {
      mock.set('isServiceWithWidgets', false);
      view.constructGraphObjects(null);
      expect(view.get('isServiceMetricLoaded')).to.be.false;
      expect(view.getUserPref.called).to.be.false;
    });

    it("metrics loaded", function() {
      App.ChartServiceMetricsG1 = Em.Object.extend();
      mock.set('isServiceWithWidgets', true);
      view.constructGraphObjects(['G1']);
      expect(view.get('isServiceMetricLoaded')).to.be.true;
      expect(view.getUserPref.calledOnce).to.be.true;
      expect(view.get('serviceMetricGraphs')).to.not.be.empty;
    });
  });

  describe("#getUserPrefSuccessCallback()", function() {

    it("currentTimeRangeIndex should be set", function() {
      view.getUserPrefSuccessCallback(1);
      expect(view.get('currentTimeRangeIndex')).to.equal(1);
    });
  });

  describe("#getUserPrefErrorCallback()", function() {

    beforeEach(function() {
      sinon.stub(view, 'postUserPref');
    });
    afterEach(function() {
      view.postUserPref.restore();
    });

    it("request.status = 404", function() {
      view.getUserPrefErrorCallback({status: 404});
      expect(view.get('currentTimeRangeIndex')).to.equal(0);
      expect(view.postUserPref.calledOnce).to.be.true;
    });

    it("request.status = 403", function() {
      view.getUserPrefErrorCallback({status: 403});
      expect(view.postUserPref.called).to.be.false;
    });
  });

  describe("#widgetActions", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App, 'isAuthorized');
      view.setProperties({
        staticWidgetLayoutActions: [{id: 1}],
        staticAdminPrivelegeWidgetActions: [{id: 2}],
        staticGeneralWidgetActions: [{id: 3}]
      });
    });
    afterEach(function() {
      this.mock.restore();
    });

    it("not authorized", function() {
      this.mock.returns(false);
      view.propertyDidChange('widgetActions');
      expect(view.get('widgetActions').mapProperty('id')).to.eql([3]);
    });

    it("is authorized", function() {
      this.mock.returns(true);
      App.supports.customizedWidgetLayout = true;
      view.propertyDidChange('widgetActions');
      expect(view.get('widgetActions').mapProperty('id')).to.eql([1, 2, 3]);
    });
  });

  describe("#doWidgetAction()", function() {

    beforeEach(function() {
      view.set('controller.action1', Em.K);
      sinon.stub(view.get('controller'), 'action1');
    });
    afterEach(function() {
      view.get('controller').action1.restore();
    });

    it("action exist", function() {
      view.doWidgetAction({context: 'action1'});
      expect(view.get('controller').action1.calledOnce).to.be.true;
    });
  });

  describe("#setTimeRange", function() {

    it("range = 0", function() {
      var widget = Em.Object.create({
        widgetType: 'GRAPH',
        properties: {
          time_range: '0'
        }
      });
      view.set('controller.widgets', [widget]);
      view.setTimeRange({context: {value: '0'}});
      expect(widget.get('properties').time_range).to.be.equal('0')
    });

    it("range = 1", function() {
      var widget = Em.Object.create({
        widgetType: 'GRAPH',
        properties: {
          time_range: 0
        }
      });
      view.set('controller.widgets', [widget]);
      view.setTimeRange({context: {value: '1'}});
      expect(widget.get('properties').time_range).to.be.equal('1')
    });
  });

  describe("#makeSortable()", function() {
    var mock = {
      on: function(arg1, arg2, callback) {
        callback();
      },
      off: Em.K,
      sortable: function() {
        return {
          disableSelection: Em.K
        }
      }
    };

    beforeEach(function() {
      sinon.stub(window, '$').returns(mock);
      sinon.spy(mock, 'on');
      sinon.spy(mock, 'off');
      sinon.spy(mock, 'sortable');
      view.makeSortable();
    });
    afterEach(function() {
      window.$.restore();
      mock.on.restore();
      mock.off.restore();
      mock.sortable.restore();
    });

    it("on() should be called", function() {
      expect(mock.on.calledWith('DOMNodeInserted', '#widget_layout')).to.be.true;
    });

    it("sortable() should be called", function() {
      expect(mock.sortable.calledOnce).to.be.true;
    });

    it("off() should be called", function() {
      expect(mock.off.calledWith('DOMNodeInserted', '#widget_layout')).to.be.true;
    });
  });

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.stub(view, 'constructGraphObjects', Em.K);
      this.mock = sinon.stub(App, 'get');
      sinon.stub(view, 'getServiceModel');
      sinon.stub(view.get('controller'), 'getActiveWidgetLayout');
      sinon.stub(view.get('controller'), 'loadWidgetLayouts');
      sinon.stub(view, 'adjustSummaryHeight');
      sinon.stub(view, 'makeSortable');
      sinon.stub(view, 'addWidgetTooltip');

    });

    afterEach(function () {
      view.constructGraphObjects.restore();
      this.mock.restore();
      view.getServiceModel.restore();
      view.get('controller').getActiveWidgetLayout.restore();
      view.get('controller').loadWidgetLayouts.restore();
      view.adjustSummaryHeight.restore();
      view.makeSortable.restore();
      view.addWidgetTooltip.restore();
    });

    it("getServiceModel should be called", function() {
      view.didInsertElement();
      expect(view.getServiceModel.calledOnce).to.be.true;
    });
    it("adjustSummaryHeight should be called", function() {
      view.didInsertElement();
      expect(view.adjustSummaryHeight.calledOnce).to.be.true;
    });
    it("addWidgetTooltip should be called", function() {
      view.didInsertElement();
      expect(view.addWidgetTooltip.calledOnce).to.be.true;
    });
    it("makeSortable should be called", function() {
      view.didInsertElement();
      expect(view.makeSortable.calledOnce).to.be.true;
    });
    it("getActiveWidgetLayout should be called", function() {
      view.didInsertElement();
      expect(view.get('controller').getActiveWidgetLayout.calledOnce).to.be.true;
    });

    describe("serviceName is null, metrics not supported, widgets not supported", function() {
      beforeEach(function () {
        view.set('controller.content.serviceName', null);
        this.mock.returns(false);
        view.didInsertElement();
      });

      it("loadWidgetLayouts should not be called", function() {
        expect(view.get('controller').loadWidgetLayouts.called).to.be.false;
      });
      it("constructGraphObjects should not be called", function() {
        expect(view.constructGraphObjects.called).to.be.false;
      });
    });

    describe("serviceName is set, metrics is supported, widgets is supported", function() {
      beforeEach(function () {
        view.set('controller.content.serviceName', 'S1');
        this.mock.returns(true);
        view.didInsertElement();
      });

      it("loadWidgetLayouts should be called", function() {
        expect(view.get('controller').loadWidgetLayouts.calledOnce).to.be.true;
      });
      it("constructGraphObjects should be called", function() {
        expect(view.constructGraphObjects.calledOnce).to.be.true;
      });
    });
  });

  describe("#addWidgetTooltip()", function() {
    var mock = {
      hoverIntent: Em.K
    };

    beforeEach(function() {
      sinon.stub(Em.run, 'later', function(arg1, callback) {
        callback();
      });
      sinon.stub(App, 'tooltip');
      sinon.stub(window, '$').returns(mock);
      sinon.spy(mock, 'hoverIntent');
      view.addWidgetTooltip();
    });
    afterEach(function() {
      Em.run.later.restore();
      App.tooltip.restore();
      window.$.restore();
      mock.hoverIntent.restore();
    });

    it("Em.run.later should be called", function() {
      expect(Em.run.later.calledOnce).to.be.true;
    });
    it("App.tooltip should be called", function() {
      expect(App.tooltip.calledOnce).to.be.true;
    });
    it("hoverIntent should be called", function() {
      expect(mock.hoverIntent.calledOnce).to.be.true;
    });
  });

  describe("#adjustSummaryHeight()", function() {
    var jQueryMock = {
      find: Em.K,
      attr: Em.K
    };

    beforeEach(function() {
      sinon.stub(window, '$').returns(jQueryMock);
      this.mockFind = sinon.stub(jQueryMock, 'find');
      sinon.spy(jQueryMock, 'attr');
      this.mockGetElementById = sinon.stub(document, 'getElementById');
    });
    afterEach(function() {
      this.mockGetElementById.restore();
      window.$.restore();
      this.mockFind.restore();
      jQueryMock.attr.restore();
    });

    it("summary-info not in DOM", function() {
      this.mockGetElementById.returns(null);
      view.adjustSummaryHeight();
      expect(jQueryMock.find.called).to.be.false;
    });

    it("summary-info has no rows", function() {
      this.mockGetElementById.returns({});
      this.mockFind.returns(null);
      view.adjustSummaryHeight();
      expect(jQueryMock.find.calledOnce).to.be.true;
      expect(jQueryMock.attr.called).to.be.false;
    });

    it("summary-info has rows", function() {
      this.mockGetElementById.returns({
        clientHeight: 10
      });
      this.mockFind.returns([{}]);
      view.adjustSummaryHeight();
      expect(jQueryMock.attr.calledWith('style', "height:20px;")).to.be.true;
    });
  });
});