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
require('controllers/main/host/details');
require('models/service');
require('models/host_component');
var batchUtils = require('utils/batch_scheduled_requests');
var componentsUtils = require('utils/components');
var controller;

describe('App.MainHostDetailsController', function () {


  beforeEach(function () {
    sinon.stub(App.ajax, 'send').returns({
      then: Em.K
    });
    controller = App.MainHostDetailsController.create({
      content: Em.Object.create()
    });
  });
  afterEach(function () {
    App.ajax.send.restore();
  });

  describe('#routeHome()', function () {
    it('transiotion to dashboard', function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
      controller.routeHome();
      expect(App.router.transitionTo.calledWith('main.dashboard.index')).to.be.true;
      App.router.transitionTo.restore();
    });
  });

  describe('#routeToService()', function () {
    it('transiotion to dashboard', function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
      controller.routeToService({context: {'service': 'service'}});
      expect(App.router.transitionTo.calledWith('main.services.service.summary', {'service': 'service'})).to.be.true;
      App.router.transitionTo.restore();
    });
  });

  describe('#startComponent()', function () {
    it('call sendComponentCommand', function () {
      var event = {
        context: Em.Object.create({
          displayName: 'comp'
        })
      };
      sinon.stub(App, 'showConfirmationPopup', function (callback) {
        callback();
      });
      sinon.stub(controller, 'sendComponentCommand');
      controller.startComponent(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.sendComponentCommand.calledWith(Em.Object.create({
        displayName: 'comp'
      })), Em.I18n.t('requestInfo.startHostComponent') + " comp", App.HostComponentStatus.started).to.be.true;
      App.showConfirmationPopup.restore();
      controller.sendComponentCommand.restore();
    });
  });

  describe('#stopComponent()', function () {
    it('call sendComponentCommand', function () {
      var event = {
        context: Em.Object.create({
          displayName: 'comp'
        })
      };
      sinon.stub(App, 'showConfirmationPopup', function (callback) {
        callback();
      });
      sinon.stub(controller, 'sendComponentCommand');
      controller.stopComponent(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      expect(controller.sendComponentCommand.calledWith(Em.Object.create({
        displayName: 'comp'
      })), Em.I18n.t('requestInfo.stopHostComponent') + " comp", App.HostComponentStatus.started).to.be.true;
      App.showConfirmationPopup.restore();
      controller.sendComponentCommand.restore();
    });
  });

  describe('#sendComponentCommand()', function () {
    it('single component', function () {
      controller.set('content.hostName', 'host1');
      var component = Em.Object.create({
        service: {serviceName: 'S1'},
        componentName: 'COMP1'
      });

      controller.sendComponentCommand(component, {}, 'state');
      expect(App.ajax.send.getCall(0).args[0].name).to.be.equal('common.host.host_component.update');
      expect(App.ajax.send.getCall(0).args[0].data).to.be.eql({
        "hostName": "host1",
        "context": {},
        "component": Em.Object.create({
          service: {serviceName: 'S1'},
          componentName: 'COMP1'
        }),
        "HostRoles": {
          "state": "state"
        },
        "componentName": "COMP1",
        "serviceName": "S1"
      });
    });
    it('multiple component', function () {
      controller.set('content.hostName', 'host1');
      var component = [
        Em.Object.create({
          service: {serviceName: 'S1'},
          componentName: 'COMP1'
        }),
        Em.Object.create({
          service: {serviceName: 'S1'},
          componentName: 'COMP2'
        })
      ];

      controller.sendComponentCommand(component, {}, 'state');
      expect(App.ajax.send.getCall(0).args[0].name).to.be.equal('common.host.host_components.update');
      expect(App.ajax.send.getCall(0).args[0].data).to.be.eql({
        "hostName": "host1",
        "context": {},
        "component": [
          Em.Object.create({
            service: {serviceName: 'S1'},
            componentName: 'COMP1'
          }),
          Em.Object.create({
            service: {serviceName: 'S1'},
            componentName: 'COMP2'
          })
        ],
        "HostRoles": {
          "state": "state"
        },
        "query": "HostRoles/component_name.in(COMP1,COMP2)"
      });
    });
  });

  describe('#sendComponentCommandSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'mimicWorkStatusChange', Em.K);
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
    });
    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
      controller.mimicWorkStatusChange.restore();
    });
    it('testMode, starting component', function () {
      var params = {
        component: Em.Object.create({}),
        HostRoles: {
          state: App.HostComponentStatus.started
        }
      };

      App.set('testMode', true);
      controller.sendComponentCommandSuccessCallback({}, {}, params);
      expect(controller.mimicWorkStatusChange.calledWith(Em.Object.create({
        workStatus: App.HostComponentStatus.starting
      }), App.HostComponentStatus.starting, App.HostComponentStatus.started)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('testMode, stopping component', function () {
      var params = {
        component: Em.Object.create({}),
        HostRoles: {
          state: App.HostComponentStatus.stopped
        }
      };

      App.set('testMode', true);
      controller.sendComponentCommandSuccessCallback({}, {}, params);
      expect(controller.mimicWorkStatusChange.calledWith(Em.Object.create({
        workStatus: App.HostComponentStatus.stopping
      }), App.HostComponentStatus.stopping, App.HostComponentStatus.stopped)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('testMode, stopping component', function () {
      var params = {
        component: Em.Object.create({}),
        HostRoles: {
          state: App.HostComponentStatus.stopped
        }
      };

      App.set('testMode', false);
      controller.sendComponentCommandSuccessCallback({}, {}, params);
      expect(controller.mimicWorkStatusChange.called).to.be.false;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#ajaxErrorCallback()', function () {
    it('call componentsUtils.ajaxErrorCallback', function () {
      sinon.stub(componentsUtils, 'ajaxErrorCallback', Em.K);
      controller.ajaxErrorCallback('request', 'ajaxOptions', 'error', 'opt', 'params');
      expect(componentsUtils.ajaxErrorCallback.calledWith('request', 'ajaxOptions', 'error', 'opt', 'params')).to.be.true;
      componentsUtils.ajaxErrorCallback.restore();
    });
  });

  describe('#showBackgroundOperationsPopup()', function () {
    var mock = {
      done: function (callback) {
        callback(this.initValue);
      }
    };
    var bgController = {
      showPopup: Em.K
    };
    beforeEach(function () {
      var stub = sinon.stub(App.router, 'get');
      stub.withArgs('applicationController').returns({
        dataLoading: function () {
          return mock;
        }
      });
      stub.withArgs('backgroundOperationsController').returns(bgController);
      sinon.spy(bgController, 'showPopup');
      sinon.spy(mock, 'done');
    });
    afterEach(function () {
      bgController.showPopup.restore();
      mock.done.restore();
      App.router.get.restore();
    });
    it('initValue is true, callback is undefined', function () {
      mock.initValue = true;
      controller.showBackgroundOperationsPopup();
      expect(mock.done.calledOnce).to.be.true;
      expect(bgController.showPopup.calledOnce).to.be.true;
    });
    it('initValue is false, callback is defined', function () {
      mock.initValue = false;
      var callback = sinon.stub();
      controller.showBackgroundOperationsPopup(callback);
      expect(mock.done.calledOnce).to.be.true;
      expect(bgController.showPopup.calledOnce).to.be.false;
      expect(callback.calledOnce).to.be.true;
    });
  });


  describe('#serviceActiveComponents', function () {

    it('No host-components', function () {
      controller.set('content', {hostComponents: []});
      expect(controller.get('serviceActiveComponents')).to.be.empty;
    });

    it('No host-components in active state', function () {
      controller.set('content', {hostComponents: [Em.Object.create({
        service: {
          isInPassive: true
        }
      })]});
      expect(controller.get('serviceActiveComponents')).to.be.empty;
    });
    it('Host-components in active state', function () {
      controller.set('content', {hostComponents: [Em.Object.create({
        service: {
          isInPassive: false
        }
      })]});
      expect(controller.get('serviceActiveComponents')).to.eql([Em.Object.create({
        service: {
          isInPassive: false
        }
      })]);
    });
  });

  describe('#serviceNonClientActiveComponents', function () {

    it('No active host-components', function () {
      controller.reopen({
        serviceActiveComponents: []
      });
      controller.set('serviceActiveComponents', []);
      expect(controller.get('serviceNonClientActiveComponents')).to.be.empty;
    });

    it('Active host-component is client', function () {
      controller.reopen({serviceActiveComponents: [Em.Object.create({
        isClient: true
      })]});
      expect(controller.get('serviceNonClientActiveComponents')).to.be.empty;
    });
    it('Active host-component is not client', function () {
      controller.reopen({serviceActiveComponents: [Em.Object.create({
        isClient: false
      })]});
      expect(controller.get('serviceNonClientActiveComponents')).to.eql([Em.Object.create({
        isClient: false
      })]);
    });
  });

  describe('#deleteComponent()', function () {
    it('confirm popup should be displayed', function () {
      var event = {
        context: Em.Object.create({})
      };
      sinon.spy(App.ModalPopup, "show");
      sinon.stub(controller, '_doDeleteHostComponent', Em.K);

      var popup = controller.deleteComponent(event);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller._doDeleteHostComponent.calledWith(Em.Object.create({}))).to.be.true;
      App.ModalPopup.show.restore();
      controller._doDeleteHostComponent.restore();
    });
  });

  describe('#mimicWorkStatusChange()', function () {

    var clock;
    beforeEach(function () {
      clock = sinon.useFakeTimers();
    });
    afterEach(function () {
      clock.restore()
    });

    it('change status of object', function () {
      var entity = Em.Object.create({
        workStatus: ''
      });
      controller.mimicWorkStatusChange(entity, 'STATE1', 'STATE2');
      expect(entity.get('workStatus')).to.equal('STATE1');
      clock.tick(App.testModeDelayForActions);
      expect(entity.get('workStatus')).to.equal('STATE2');
    });
    it('change status of objects in array', function () {
      var entity = [Em.Object.create({
        workStatus: ''
      })];
      controller.mimicWorkStatusChange(entity, 'STATE1', 'STATE2');
      expect(entity[0].get('workStatus')).to.equal('STATE1');
      clock.tick(App.testModeDelayForActions);
      expect(entity[0].get('workStatus')).to.equal('STATE2');
    });
  });

  describe('#upgradeComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('confirm popup should be displayed', function () {
      var popup = controller.upgradeComponent({context: Em.Object.create()});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#restartComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, "restartHostComponents", Em.K);
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.restartComponent({context: Em.Object.create({'displayName': 'Comp1'})});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledOnce).to.be.true;
    });
  });

  describe('#securityEnabled', function () {
    it('', function () {
      sinon.stub(App.router, 'get').withArgs('mainAdminSecurityController.securityEnabled').returns(true);

      controller.propertyDidChange('securityEnabled');
      expect(controller.get('securityEnabled')).to.be.true;
      App.router.get.restore();
    });
  });


  describe('#addComponent()', function () {
    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "addClientComponent", Em.K);
      sinon.stub(controller, "primary", Em.K);
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: "HDFS_CLIENT"
      })]});
      controller.reopen({
        securityEnabled: false
      });
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.addClientComponent.restore();
      controller.primary.restore();
    });

    it('add ZOOKEEPER_SERVER', function () {
      var event = {context: Em.Object.create({
        componentName: 'ZOOKEEPER_SERVER'
      })};
      var popup = controller.addComponent(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.primary.calledWith(Em.Object.create({
        componentName: 'ZOOKEEPER_SERVER'
      }))).to.be.true;
    });
    it('add slave component, securityEnabled = true', function () {
      var event = {context: Em.Object.create({
        componentName: 'HIVE_CLIENT'
      })};
      controller.set('securityEnabled', true);
      var popup = controller.addComponent(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.primary.calledWith(Em.Object.create({
        componentName: 'HIVE_CLIENT'
      }))).to.be.true;
    });
    it('add slave component, securityEnabled = false', function () {
      var event = {context: Em.Object.create({
        componentName: 'HIVE_CLIENT'
      })};
      controller.set('securityEnabled', false);
      controller.addComponent(event);
      expect(controller.addClientComponent.calledWith(Em.Object.create({
        componentName: 'HIVE_CLIENT'
      }))).to.be.true;
    });
  });

  describe('#formatClientsMessage()', function () {
    var testCases = [
      {
        title: 'subComponentNames is null',
        client: Em.Object.create({
          subComponentNames: null,
          displayName: 'CLIENTS'
        }),
        result: 'CLIENTS'
      },
      {
        title: 'subComponentNames is empty',
        client: Em.Object.create({
          subComponentNames: [],
          displayName: 'CLIENTS'
        }),
        result: 'CLIENTS'
      },
      {
        title: 'displayName is null',
        client: Em.Object.create({
          subComponentNames: ['DATANODE'],
          displayName: null
        }),
        result: ' (DataNode)'
      },
      {
        title: 'displayName is CLIENTS',
        client: Em.Object.create({
          subComponentNames: ['DATANODE'],
          displayName: 'CLIENTS'
        }),
        result: 'CLIENTS (DataNode)'
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.formatClientsMessage(test.client)).to.equal(test.result);
      });
    });
  });

  describe('#addClientComponent()', function () {

    beforeEach(function () {
      sinon.spy(App.ModalPopup, "show");
      sinon.stub(controller, "primary", Em.K);
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
      controller.primary.restore();
    });

    it('not CLIENT component', function () {
      var component = Em.Object.create({'componentName': 'Comp1'});
      var popup = controller.addClientComponent(component);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.primary.calledWith(Em.Object.create({'componentName': 'Comp1'}))).to.be.true;
    });
    it('CLIENT components, with empty subComponentNames', function () {
      var component = Em.Object.create({
        componentName: 'CLIENTS',
        subComponentNames: []
      });
      var popup = controller.addClientComponent(component);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.primary.calledOnce).to.be.false;
    });
    it('CLIENT components, with two sub-component', function () {
      var component = Em.Object.create({
        componentName: 'CLIENTS',
        subComponentNames: ['DATANODE', 'TASKTRACKER']
      });
      var popup = controller.addClientComponent(component);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.primary.calledTwice).to.be.true;
    });
  });

  describe('#primary()', function () {
    it('Query should be sent', function () {
      var component = Em.Object.create({
        componentName: 'COMP1',
        displayName: 'comp1'
      });
      controller.primary(component);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#installNewComponentSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });
    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
    });

    it('data.Requests is null', function () {
      var data = {Requests: null};
      expect(controller.installNewComponentSuccessCallback(data, {}, {})).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data.Requests.id is null', function () {
      var data = {Requests: {id: null}};
      expect(controller.installNewComponentSuccessCallback(data, {}, {})).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data.Requests.id is correct', function () {
      var data = {Requests: {id: 1}};
      expect(controller.installNewComponentSuccessCallback(data, {}, {component: []})).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#refreshComponentConfigs()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "sendRefreshComponentConfigsCommand", Em.K);
    });

    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendRefreshComponentConfigsCommand.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.refreshComponentConfigs({context: Em.Object.create({'displayName': 'Comp1'})});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendRefreshComponentConfigsCommand.calledOnce).to.be.true;
    });
  });

  describe('#sendRefreshComponentConfigsCommand()', function () {
    it('Query should be sent', function () {
      var component = Em.Object.create({
        service: {},
        componentName: 'COMP1',
        host: {}
      });
      controller.sendRefreshComponentConfigsCommand(component, {});
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#loadConfigs()', function () {
    it('Query should be sent', function () {
      controller.loadConfigs();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#constructConfigUrlParams()', function () {

    it('URL params should be empty', function () {
      var data = {};
      App.Service.find().clear();
      expect(controller.constructConfigUrlParams(data)).to.eql([]);
    });
    it('isHaEnabled = true', function () {
      App.store.load(App.Service, {
        id: 'HDFS',
        service_name: 'HDFS'
      });
      var data = {Clusters: {desired_configs: {'core-site': {tag: 1}}}};
      App.HostComponent.find().clear();
      App.set('currentStackVersion', 'HDP-2.0.2');
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=core-site&tag=1)']);
      App.store.load(App.HostComponent, {
        id: 'SECONDARY_NAMENODE_host1',
        component_name: 'SECONDARY_NAMENODE'
      });
      App.set('currentStackVersion', 'HDP-2.0.1');
    });
    it('HBASE is installed', function () {
      App.store.load(App.Service, {
        id: 'HBASE',
        service_name: 'HBASE'
      });
      var data = {Clusters: {desired_configs: {'hbase-site': {tag: 1}}}};
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=hbase-site&tag=1)']);
      App.Service.find().clear();
    });
    it('HIVE is installed', function () {
      App.store.load(App.Service, {
        id: 'HIVE',
        service_name: 'HIVE'
      });
      var data = {Clusters: {desired_configs: {'webhcat-site': {tag: 1}, 'hive-site': {tag: 1}}}};
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=webhcat-site&tag=1)', '(type=hive-site&tag=1)']);
      App.Service.find().clear();
    });
    it('STORM is installed', function () {
      App.store.load(App.Service, {
        id: 'STORM',
        service_name: 'STORM'
      });
      var data = {Clusters: {desired_configs: {'storm-site': {tag: 1}}}};
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=storm-site&tag=1)']);
      App.Service.find().clear();
    });
    it('YARN for 2.2 stack is installed', function () {
      App.set('currentStackVersion', 'HDP-2.2.0');
      App.store.load(App.Service, {
        id: 'YARN',
        service_name: 'YARN'
      });
      var data = {Clusters: {desired_configs: {'yarn-site': {tag: 1}}}};
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=yarn-site&tag=1)']);
      App.set('currentStackVersion', 'HDP-2.0.1');
      App.Service.find().clear();
    });
    it('isRMHaEnabled true', function () {
      sinon.stub(App, 'get').withArgs('isRMHaEnabled').returns(true);
      var data = {Clusters: {desired_configs: {'yarn-site': {tag: 1}}}};
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=yarn-site&tag=1)']);
      App.get.restore();
    });
  });

  describe('#loadConfigsSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "constructConfigUrlParams", function () {
        return this.get('mockUrlParams');
      });
    });
    afterEach(function () {
      controller.constructConfigUrlParams.restore();
    });

    it('url params is empty', function () {
      controller.set('mockUrlParams', []);
      expect(controller.loadConfigsSuccessCallback()).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it('url params are correct', function () {
      controller.set('mockUrlParams', ['param1']);
      expect(controller.loadConfigsSuccessCallback()).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#saveZkConfigs()', function () {

    beforeEach(function () {
      sinon.stub(controller, "getZkServerHosts", Em.K);
      sinon.stub(controller, "concatZkNames", Em.K);
      sinon.stub(controller, "setZKConfigs", Em.K);
    });
    afterEach(function () {
      controller.getZkServerHosts.restore();
      controller.concatZkNames.restore();
      controller.setZKConfigs.restore();
    });

    it('data.items is empty', function () {
      var data = {items: []};
      controller.saveZkConfigs(data);
      expect(App.ajax.send.called).to.be.false;
    });
    it('data.items has one item', function () {
      var data = {items: [
        {
          type: 'type1',
          properties: {}
        }
      ]};
      controller.saveZkConfigs(data);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
    it('data.items has two items', function () {
      var data = {items: [
        {
          type: 'type1',
          properties: {}
        },
        {
          type: 'type2',
          properties: {}
        }
      ]};
      controller.saveZkConfigs(data);
      expect(App.ajax.send.calledTwice).to.be.true;
    });
  });

  describe('#setZKConfigs()', function () {
    it('configs is null', function () {
      expect(controller.setZKConfigs(null)).to.be.false;
    });
    it('zks is null', function () {
      expect(controller.setZKConfigs({}, '', null)).to.be.false;
    });
    it('isHaEnabled = true', function () {
      var configs = {'core-site': {}};
      App.HostComponent.find().clear();
      App.store.load(App.Service, {
        id: 'HDFS',
        service_name: 'HDFS'
      });
      App.set('currentStackVersion', 'HDP-2.0.2');
      expect(controller.setZKConfigs(configs, 'host1:2181', [])).to.be.true;
      expect(configs).to.eql({"core-site": {
        "ha.zookeeper.quorum": "host1:2181"
      }});
      App.store.load(App.HostComponent, {
        id: 'SECONDARY_NAMENODE_host1',
        component_name: 'SECONDARY_NAMENODE'
      });
      App.set('currentStackVersion', 'HDP-2.0.1');
    });
    it('hbase-site is present', function () {
      var configs = {'hbase-site': {}};
      expect(controller.setZKConfigs(configs, '', ['host1', 'host2'])).to.be.true;
      expect(configs).to.eql({"hbase-site": {
        "hbase.zookeeper.quorum": "host1,host2"
      }});
    });
    it('webhcat-site is present', function () {
      var configs = {'webhcat-site': {}};
      expect(controller.setZKConfigs(configs, 'host1:2181', [])).to.be.true;
      expect(configs).to.eql({"webhcat-site": {
        "templeton.zookeeper.hosts": "host1:2181"
      }});
    });
    it('hive-site is present and stack < 2.2', function () {
      var version = App.get('currentStackVersion');
      var configs = {'hive-site': {}};
      App.set('currentStackVersion', 'HDP-2.1.0');
      expect(controller.setZKConfigs(configs, 'host1:2181', [])).to.be.true;
      expect(configs).to.eql({"hive-site": {
        'hive.cluster.delegation.token.store.zookeeper.connectString': "host1:2181"
      }});
      App.set('currentStackVersion', version);
    });
    it('hive-site is present and stack > 2.2', function () {
      var version = App.get('currentStackVersion');
      var configs = {'hive-site': {}};
      App.set('currentStackVersion', 'HDP-2.2.0');
      expect(controller.setZKConfigs(configs, 'host1:2181', [])).to.be.true;
      expect(configs).to.eql({"hive-site": {
        'hive.cluster.delegation.token.store.zookeeper.connectString': "host1:2181",
        'hive.zookeeper.quorum': "host1:2181"
      }});
      App.set('currentStackVersion', version);
    });
    it('yarn-site is present and stack > 2.2', function () {
      var version = App.get('currentStackVersion');
      var configs = {'yarn-site': {}};
      App.set('currentStackVersion', 'HDP-2.2.0');
      expect(controller.setZKConfigs(configs, 'host1:2181', [])).to.be.true;
      expect(configs).to.eql({"yarn-site": {
        'hadoop.registry.zk.quorum': "host1:2181"
      }});
      App.set('currentStackVersion', version);
    });
    it('storm-site is present', function () {
      var configs = {'storm-site': {}};
      expect(controller.setZKConfigs(configs, '', ["host1", 'host2'])).to.be.true;
      expect(configs).to.eql({"storm-site": {
        "storm.zookeeper.servers": "['host1','host2']"
      }});
    });
    it('isRMHaEnabled true', function () {
      var configs = {'yarn-site': {}};
      sinon.stub(App, 'get').withArgs('isRMHaEnabled').returns(true);
      expect(controller.setZKConfigs(configs, 'host1:2181', ['host1', 'host2'])).to.be.true;
      expect(configs).to.eql({"yarn-site": {
        "yarn.resourcemanager.zk-address": "host1,host2"
      }});
      App.get.restore();
    });
  });

  describe('#concatZkNames()', function () {
    it('No ZooKeeper hosts', function () {
      expect(controller.concatZkNames([])).to.equal('');
    });
    it('One ZooKeeper host', function () {
      expect(controller.concatZkNames(['host1'])).to.equal('host1:2181');
    });
    it('Two ZooKeeper hosts', function () {
      expect(controller.concatZkNames(['host1', 'host2'])).to.equal('host1:2181,host2:2181');
    });
  });

  describe('#getZkServerHosts()', function () {

    beforeEach(function () {
      controller.set('content', {});
    });

    afterEach(function () {
      App.HostComponent.find.restore();
    });

    it('No ZooKeeper hosts, fromDeleteHost = false', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return []
      });
      controller.set('fromDeleteHost', false);
      expect(controller.getZkServerHosts()).to.be.empty;
    });

    it('No ZooKeeper hosts, fromDeleteHost = true', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return []
      });
      controller.set('fromDeleteHost', true);
      expect(controller.getZkServerHosts()).to.be.empty;
      expect(controller.get('fromDeleteHost')).to.be.false;
    });

    it('One ZooKeeper host, fromDeleteHost = false', function () {
      controller.set('fromDeleteHost', false);
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {id: 'ZOOKEEPER_SERVER_host1',
            componentName: 'ZOOKEEPER_SERVER',
            hostName: 'host1'
          }
        ]
      });
      expect(controller.getZkServerHosts()).to.eql(['host1']);
    });

    it('One ZooKeeper host match current host name, fromDeleteHost = true', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {id: 'ZOOKEEPER_SERVER_host1',
            componentName: 'ZOOKEEPER_SERVER',
            hostName: 'host1'
          }
        ]
      });
      controller.set('fromDeleteHost', true);
      controller.set('content.hostName', 'host1');
      expect(controller.getZkServerHosts()).to.be.empty;
      expect(controller.get('fromDeleteHost')).to.be.false;
    });

    it('One ZooKeeper host does not match current host name, fromDeleteHost = true', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {id: 'ZOOKEEPER_SERVER_host1',
            componentName: 'ZOOKEEPER_SERVER',
            hostName: 'host1'
          }
        ]
      });
      controller.set('fromDeleteHost', true);
      controller.set('content.hostName', 'host2');
      expect(controller.getZkServerHosts()[0]).to.equal("host1");
      expect(controller.get('fromDeleteHost')).to.be.false;
    });
  });

  describe('#installComponent()', function () {

    beforeEach(function () {
      sinon.spy(App.ModalPopup, "show");
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('popup should be displayed', function () {
      var event = {context: Em.Object.create()};
      var popup = controller.installComponent(event);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#decommission()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "runDecommission", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.runDecommission.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.decommission(Em.Object.create({service: {}}));
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.runDecommission.calledOnce).to.be.true;
    });
  });

  describe('#recommission()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "runRecommission", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.runRecommission.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.recommission(Em.Object.create({service: {}}));
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.runRecommission.calledOnce).to.be.true;
    });
  });

  describe('#runDecommission()', function () {

    beforeEach(function () {
      sinon.stub(controller, "doDecommission", Em.K);
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });

    afterEach(function () {
      controller.doDecommission.restore();
      controller.showBackgroundOperationsPopup.restore();
    });

    it('HDFS service', function () {
      controller.runDecommission('host1', 'HDFS');
      expect(controller.doDecommission.calledWith('host1', 'HDFS', "NAMENODE", "DATANODE")).to.be.true;
    });
    it('YARN service', function () {
      controller.runDecommission('host1', 'YARN');
      expect(controller.doDecommission.calledWith('host1', 'YARN', "RESOURCEMANAGER", "NODEMANAGER")).to.be.true;
    });
    it('MAPREDUCE service', function () {
      controller.runDecommission('host1', 'MAPREDUCE');
      expect(controller.doDecommission.calledWith('host1', 'MAPREDUCE', "JOBTRACKER", "TASKTRACKER")).to.be.true;
    });
    it('HBASE service', function () {
      sinon.stub(controller, 'warnBeforeDecommission', Em.K);
      controller.runDecommission('host1', 'HBASE');
      expect(controller.warnBeforeDecommission.calledWith('host1')).to.be.true;
      controller.warnBeforeDecommission.restore();
    });
  });

  describe('#runRecommission()', function () {

    beforeEach(function () {
      sinon.stub(controller, "doRecommissionAndStart", Em.K);
      sinon.stub(controller, "doRecommissionAndRestart", Em.K);
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });

    afterEach(function () {
      controller.doRecommissionAndStart.restore();
      controller.doRecommissionAndRestart.restore();
      controller.showBackgroundOperationsPopup.restore();
    });

    it('HDFS service', function () {
      controller.runRecommission('host1', 'HDFS');
      expect(controller.doRecommissionAndStart.calledWith('host1', 'HDFS', "NAMENODE", "DATANODE")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('YARN service', function () {
      controller.runRecommission('host1', 'YARN');
      expect(controller.doRecommissionAndStart.calledWith('host1', 'YARN', "RESOURCEMANAGER", "NODEMANAGER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('MAPREDUCE service', function () {
      controller.runRecommission('host1', 'MAPREDUCE');
      expect(controller.doRecommissionAndRestart.calledWith('host1', 'MAPREDUCE', "JOBTRACKER", "TASKTRACKER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('HBASE service', function () {
      controller.runRecommission('host1', 'HBASE');
      expect(controller.doRecommissionAndStart.calledWith('host1', 'HBASE', "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#doDecommission()', function () {
    it('Query should be sent', function () {
      controller.doDecommission('', '', '', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#doDecommissionRegionServer()', function () {
    it('Query should be sent', function () {
      controller.doDecommissionRegionServer('', '', '', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#warnBeforeDecommission()', function () {
    beforeEach(function () {
      sinon.stub(controller, "showHbaseActiveWarning", Em.K);
      sinon.stub(controller, "checkRegionServerState", Em.K);
    });
    afterEach(function () {
      controller.checkRegionServerState.restore();
      controller.showHbaseActiveWarning.restore();
    });

    it('Component in passive state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'HBASE_REGIONSERVER',
        passiveState: 'ON'
      })]);
      controller.warnBeforeDecommission('host1');
      expect(controller.checkRegionServerState.calledOnce).to.be.true;
    });
    it('Component is not in passive state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'HBASE_REGIONSERVER',
        passiveState: 'OFF'
      })]);
      controller.warnBeforeDecommission('host1');
      expect(controller.showHbaseActiveWarning.calledOnce).to.be.true;
    });
  });

  describe('#checkRegionServerState()', function () {
    it('', function () {
      expect(controller.checkRegionServerState('host1')).to.be.an('object');
      expect(App.ajax.send.getCall(0).args[0].data.hostNames).to.equal('host1');
    });
  });

  describe('#checkRegionServerStateSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, "doDecommissionRegionServer", Em.K);
      sinon.stub(controller, "showRegionServerWarning", Em.K);
    });
    afterEach(function () {
      controller.doDecommissionRegionServer.restore();
      controller.showRegionServerWarning.restore();
    });

    it('Decommission all regionservers', function () {
      var data = {
        items: [
          {
            HostRoles: {
              host_name: 'host1'
            }
          },
          {
            HostRoles: {
              host_name: 'host2'
            }
          }
        ]
      };
      controller.checkRegionServerStateSuccessCallback(data, {}, {hostNames: 'host1,host2'});
      expect(controller.showRegionServerWarning.calledOnce).to.be.true;
    });
    it('Decommission one of two regionservers', function () {
      var data = {
        items: [
          {
            HostRoles: {
              host_name: 'host1'
            }
          },
          {
            HostRoles: {
              host_name: 'host2'
            }
          }
        ]
      };
      controller.checkRegionServerStateSuccessCallback(data, {}, {hostNames: 'host1'});
      expect(controller.doDecommissionRegionServer.calledWith('host1', "HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
    });
    it('Decommission one of three regionservers', function () {
      var data = {
        items: [
          {
            HostRoles: {
              host_name: 'host1'
            }
          },
          {
            HostRoles: {
              host_name: 'host2'
            }
          },
          {
            HostRoles: {
              host_name: 'host3'
            }
          }
        ]
      };
      controller.checkRegionServerStateSuccessCallback(data, {}, {hostNames: 'host1'});
      expect(controller.doDecommissionRegionServer.calledWith('host1', "HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
    });
  });

  describe('#showRegionServerWarning()', function () {
    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
    });
    it('', function () {
      controller.showRegionServerWarning();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#doRecommissionAndStart()', function () {
    it('Query should be sent', function () {
      controller.doRecommissionAndStart('', '', '', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#decommissionSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
    });
    afterEach(function () {
      controller.showBackgroundOperationsPopup.restore();
    });

    it('data is null', function () {
      expect(controller.decommissionSuccessCallback(null)).to.be.false;
      expect(controller.showBackgroundOperationsPopup.called).to.be.false;
    });
    it('data has Requests', function () {
      var data = {Requests: []};
      expect(controller.decommissionSuccessCallback(data)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('data has resources', function () {
      var data = {resources: [
        {RequestSchedule: {}}
      ]};
      expect(controller.decommissionSuccessCallback(data)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#doRecommissionAndRestart()', function () {
    it('Query should be sent', function () {
      controller.doRecommissionAndRestart('', '', '', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#doAction()', function () {

    beforeEach(function () {
      sinon.stub(controller, "validateAndDeleteHost", Em.K);
      sinon.stub(controller, "doStartAllComponents", Em.K);
      sinon.stub(controller, "doStopAllComponents", Em.K);
      sinon.stub(controller, "doRestartAllComponents", Em.K);
      sinon.stub(controller, "onOffPassiveModeForHost", Em.K);
    });

    afterEach(function () {
      controller.validateAndDeleteHost.restore();
      controller.doStartAllComponents.restore();
      controller.doStopAllComponents.restore();
      controller.doRestartAllComponents.restore();
      controller.onOffPassiveModeForHost.restore();
    });

    it('"deleteHost" action', function () {
      var option = {context: {action: "deleteHost"}};
      controller.doAction(option);
      expect(controller.validateAndDeleteHost.calledOnce).to.be.true;
    });

    it('"startAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "startAllComponents"}};
      controller.set('content', {isNotHeartBeating: false});
      controller.doAction(option);
      expect(controller.doStartAllComponents.calledOnce).to.be.true;
    });

    it('"startAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "startAllComponents"}};
      controller.set('content', {isNotHeartBeating: true});
      controller.doAction(option);
      expect(controller.doStartAllComponents.called).to.be.false;
    });

    it('"stopAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "stopAllComponents"}};
      controller.set('content', {isNotHeartBeating: false});
      controller.doAction(option);
      expect(controller.doStopAllComponents.calledOnce).to.be.true;
    });

    it('"stopAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "stopAllComponents"}};
      controller.set('content', {isNotHeartBeating: true});
      controller.doAction(option);
      expect(controller.doStopAllComponents.called).to.be.false;
    });

    it('"restartAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "restartAllComponents"}};
      controller.set('content', {isNotHeartBeating: false});
      controller.doAction(option);
      expect(controller.doRestartAllComponents.calledOnce).to.be.true;
    });

    it('"restartAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "restartAllComponents"}};
      controller.set('content', {isNotHeartBeating: true});
      controller.doAction(option);
      expect(controller.doRestartAllComponents.called).to.be.false;
    });

    it('"onOffPassiveModeForHost" action', function () {
      var option = {context: {action: "onOffPassiveModeForHost"}};
      controller.doAction(option);
      expect(controller.onOffPassiveModeForHost.calledWith({action: "onOffPassiveModeForHost"})).to.be.true;
    });
  });

  describe('#onOffPassiveModeForHost()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "hostPassiveModeRequest", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.hostPassiveModeRequest.restore();
    });

    it('popup should be displayed, active = true', function () {
      var popup = controller.onOffPassiveModeForHost({active: true});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.hostPassiveModeRequest.calledWith('ON')).to.be.true;
    });
    it('popup should be displayed, active = false', function () {
      var popup = controller.onOffPassiveModeForHost({active: false});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.hostPassiveModeRequest.calledWith('OFF')).to.be.true;
    });
  });

  describe('#hostPassiveModeRequest()', function () {
    it('Query should be sent', function () {
      controller.hostPassiveModeRequest('', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#doStartAllComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      controller.reopen({serviceActiveComponents: []});
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('serviceNonClientActiveComponents is empty', function () {
      controller.reopen({
        serviceNonClientActiveComponents: []
      });
      controller.doStartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceNonClientActiveComponents is correct', function () {
      controller.reopen({
        serviceNonClientActiveComponents: [
          {}
        ]
      });
      sinon.stub(controller, 'sendComponentCommand', Em.K);
      var popup = controller.doStartAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendComponentCommand.calledWith(
        [
          {}
        ],
        Em.I18n.t('hosts.host.maintainance.startAllComponents.context'),
        App.HostComponentStatus.started)
      ).to.be.true;
      controller.sendComponentCommand.restore();
    });
  });

  describe('#doStopAllComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      controller.reopen({serviceActiveComponents: []});
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('serviceNonClientActiveComponents is empty', function () {
      controller.reopen({
        serviceNonClientActiveComponents: []
      });
      controller.doStopAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });

    it('serviceNonClientActiveComponents is correct', function () {
      controller.reopen({
        serviceNonClientActiveComponents: [
          {}
        ]
      });
      sinon.stub(controller, 'sendComponentCommand', Em.K);
      var popup = controller.doStopAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendComponentCommand.calledWith(
        [
          {}
        ],
        Em.I18n.t('hosts.host.maintainance.stopAllComponents.context'),
        App.HostComponentStatus.stopped)
      ).to.be.true;
      controller.sendComponentCommand.restore();
    });
  });

  describe('#doRestartAllComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('serviceActiveComponents is empty', function () {
      controller.reopen({
        serviceActiveComponents: []
      });
      controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });

    it('serviceActiveComponents is correct', function () {
      controller.reopen({
        serviceActiveComponents: [
          {}
        ]
      });
      sinon.stub(batchUtils, 'restartHostComponents', Em.K);

      var popup = controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith(
        [
          {}
        ])
      ).to.be.true;
      batchUtils.restartHostComponents.restore();

    });
  });

  describe('#getHostComponentsInfo()', function () {

    var result = {
      zkServerInstalled: false,
      lastComponents: [],
      masterComponents: [],
      runningComponents: [],
      nonDeletableComponents: [],
      unknownComponents: []
    };

    it('content.hostComponents is null', function () {
      controller.set('content', {hostComponents: null});
      expect(controller.getHostComponentsInfo()).to.eql(result);
    });
    it('content.hostComponents is empty', function () {
      controller.set('content', {hostComponents: []});
      expect(controller.getHostComponentsInfo()).to.eql(result);
    });
    it('content.hostComponents has ZOOKEEPER_SERVER', function () {
      App.HostComponent.find().clear();
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: 'ZOOKEEPER_SERVER',
        workStatus: 'INIT',
        isDeletable: true
      })]});
      expect(controller.getHostComponentsInfo().zkServerInstalled).to.be.true;
    });
    it('content.hostComponents has last component', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {
            id: 'TASKTRACKER_host1',
            componentName: 'TASKTRACKER'
          }
        ];
      });
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: 'TASKTRACKER',
        displayName: 'TaskTracker',
        workStatus: 'INIT',
        isDeletable: true
      })]});
      expect(controller.getHostComponentsInfo().lastComponents).to.eql(['TaskTracker']);
      App.HostComponent.find.restore();
    });
    it('content.hostComponents has master non-deletable component', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {
            id: 'TASKTRACKER_host1',
            componentName: 'TASKTRACKER'
          }
        ];
      });
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'INIT',
        isDeletable: false,
        isMaster: true,
        displayName: 'ZK1'
      })]});
      expect(controller.getHostComponentsInfo().masterComponents).to.eql(['ZK1']);
      expect(controller.getHostComponentsInfo().nonDeletableComponents).to.eql(['ZK1']);
      App.HostComponent.find.restore();
    });
    it('content.hostComponents has running component', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {
            id: 'TASKTRACKER_host1',
            componentName: 'TASKTRACKER'
          }
        ];
      });
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'STARTED',
        isDeletable: true,
        displayName: 'ZK1'
      })]});
      expect(controller.getHostComponentsInfo().runningComponents).to.eql(['ZK1']);
      App.HostComponent.find.restore();
    });
    it('content.hostComponents has non-deletable component', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {
            id: 'TASKTRACKER_host1',
            componentName: 'TASKTRACKER'
          }
        ];
      });
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'INIT',
        isDeletable: false,
        displayName: 'ZK1'
      })]});
      expect(controller.getHostComponentsInfo().nonDeletableComponents).to.eql(['ZK1']);
      App.HostComponent.find.restore();
    });
    it('content.hostComponents has component with UNKNOWN state', function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          {
            id: 'TASKTRACKER_host1',
            componentName: 'TASKTRACKER'
          }
        ];
      });
      controller.set('content', {hostComponents: [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'UNKNOWN',
        isDeletable: false,
        displayName: 'ZK1'
      })]});
      expect(controller.getHostComponentsInfo().unknownComponents).to.eql(['ZK1']);
      App.HostComponent.find.restore();
    });
  });

  describe('#validateAndDeleteHost()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "getHostComponentsInfo", function () {
        return this.get('mockHostComponentsInfo');
      });
      sinon.stub(controller, "raiseDeleteComponentsError", Em.K);
      sinon.stub(controller, "confirmDeleteHost", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.getHostComponentsInfo.restore();
      controller.raiseDeleteComponentsError.restore();
      controller.confirmDeleteHost.restore();
    });

    it('App.supports.deleteHost = false', function () {
      App.supports.deleteHost = false;
      expect(controller.validateAndDeleteHost()).to.be.false;
      App.supports.deleteHost = true;
    });
    it('masterComponents exist', function () {
      controller.set('mockHostComponentsInfo', {masterComponents: [
        {}
      ]});
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith([
        {}
      ], 'masterList')).to.be.true;
    });
    it('nonDeletableComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        masterComponents: [],
        nonDeletableComponents: [
          {}
        ]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith([
        {}
      ], 'nonDeletableList')).to.be.true;
    });
    it('runningComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        masterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [
          {}
        ]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith([
        {}
      ], 'runningList')).to.be.true;
    });
    it('zkServerInstalled = true', function () {
      controller.set('mockHostComponentsInfo', {
        masterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        unknownComponents: [],
        lastComponents: [],
        zkServerInstalled: true
      });
      var popup = controller.validateAndDeleteHost();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.confirmDeleteHost.calledWith([], [])).to.be.true;
    });
    it('zkServerInstalled = false', function () {
      controller.set('mockHostComponentsInfo', {
        masterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [],
        unknownComponents: [],
        lastComponents: [],
        zkServerInstalled: false
      });
      controller.validateAndDeleteHost();
      expect(controller.confirmDeleteHost.calledWith([], [])).to.be.true;
    });
  });

  describe('#raiseDeleteComponentsError()', function () {

    beforeEach(function () {
      sinon.stub(App.ModalPopup, "show", Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('Popup should be displayed', function () {
      controller.raiseDeleteComponentsError([], '');
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#confirmDeleteHost()', function () {
    it('Popup should be displayed', function () {
      sinon.spy(App.ModalPopup, "show");
      sinon.stub(controller, 'doDeleteHost');

      var popup = controller.confirmDeleteHost([], []);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.doDeleteHost.calledOnce).to.be.true;
      App.ModalPopup.show.restore();
      controller.doDeleteHost.restore();
    });
  });

  describe('#restartAllStaleConfigComponents()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, "restartHostComponents", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
    });

    it('popup should be displayed', function () {
      controller.set('content', {componentsWithStaleConfigs: [
        {}
      ]});
      var popup = controller.restartAllStaleConfigComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith([
        {}
      ])).to.be.true;
    });
  });

  describe('#moveComponent()', function () {
    it('popup should be displayed', function () {
      var mock = {
        saveComponentToReassign: Em.K,
        getSecurityStatus: Em.K,
        setCurrentStep: Em.K
      };
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(App.router, 'get').withArgs('reassignMasterController').returns(mock);
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.spy(mock, "saveComponentToReassign");
      sinon.spy(mock, "getSecurityStatus");
      sinon.spy(mock, "setCurrentStep");

      var popup = controller.moveComponent({context: {}});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(App.router.get.calledWith('reassignMasterController')).to.be.true;
      expect(mock.saveComponentToReassign.calledWith({})).to.be.true;
      expect(mock.getSecurityStatus.calledOnce).to.be.true;
      expect(mock.setCurrentStep.calledWith('1')).to.be.true;
      expect(App.router.transitionTo.calledWith('reassign')).to.be.true;

      App.showConfirmationPopup.restore();
      App.router.get.restore();
      App.router.transitionTo.restore();
      mock.saveComponentToReassign.restore();
      mock.getSecurityStatus.restore();
      mock.setCurrentStep.restore();

    });
  });

  describe('#refreshConfigs()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(batchUtils, "restartHostComponents", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      batchUtils.restartHostComponents.restore();
    });

    it('No components', function () {
      var event = {context: []};
      controller.refreshConfigs(event);
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('No components with stale configs', function () {
      var event = {context: [Em.Object.create({
        staleConfigs: false
      })]};
      controller.refreshConfigs(event);
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('Components with stale configs', function () {
      var event = {context: [Em.Object.create({
        staleConfigs: true
      })]};
      var popup = controller.refreshConfigs(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith([Em.Object.create({
        staleConfigs: true
      })])).to.be.true;
    });
  });

  describe('#getTotalComponent()', function () {

    beforeEach(function () {
      sinon.stub(App.SlaveComponent, 'find', function () {
        return Em.Object.create({
          componentName: "SLAVE",
          totalCount: 1
        });
      });
      sinon.stub(App.ClientComponent, 'find', function () {
        return Em.Object.create({
          componentName: "CLIENT",
          totalCount: 1
        });
      });
      sinon.stub(App.HostComponent, 'find', function () {
        return [Em.Object.create({
          componentName: "MASTER",
          totalCount: 1
        })]
      });
    });
    afterEach(function () {
      App.SlaveComponent.find.restore();
      App.ClientComponent.find.restore();
      App.HostComponent.find.restore();
    });

    it('component is slave', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "SLAVE",
        isSlave: true
      }))).to.equal(1);
    });
    it('component is client', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "CLIENT",
        isClient: true
      }))).to.equal(1);
    });
    it('component is master', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "MASTER"
      }))).to.equal(1);
    });
    it('unknown component', function () {
      expect(controller.getTotalComponent(Em.Object.create({
        componentName: "UNKNOWN"
      }))).to.equal(0);
    });
  });
  describe('#downloadClientConfigs()', function () {

    beforeEach(function () {
      sinon.stub($, 'fileDownload', function() {
        return {
          fail: function() { return false; }
        }
      });
    });
    afterEach(function () {
      $.fileDownload.restore();
    });

    it('should launch $.fileDownload method', function () {
      controller.downloadClientConfigs({
        context: Em.Object.create({
          componentName: 'name'
        })
      });
      expect($.fileDownload.calledOnce).to.be.true;
    });
  });

  describe('#executeCustomCommands', function () {
    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('confirm popup should be displayed', function () {
      var popup = controller.executeCustomCommand({context: Em.Object.create()});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#_doDeleteHostComponent()', function () {
    it('single component', function () {
      controller.set('content.hostName', 'host1');
      var component = Em.Object.create({componentName: 'COMP'});
      controller._doDeleteHostComponent(component);
      expect(App.ajax.send.getCall(0).args[0].name).to.be.equal('common.delete.host_component');
      expect(App.ajax.send.getCall(0).args[0].data).to.be.eql({
        componentName: 'COMP',
        hostName: 'host1'
      });
    });
    it('all components', function () {
      controller.set('content.hostName', 'host1');
      controller._doDeleteHostComponent(null);
      expect(App.ajax.send.getCall(0).args[0].name).to.be.equal('common.delete.host');
      expect(App.ajax.send.getCall(0).args[0].data).to.be.eql({
        componentName: '',
        hostName: 'host1'
      });
    });
  });

  describe('#_doDeleteHostComponentSuccessCallback()', function () {
    it('ZOOKEEPER_SERVER component', function () {
      var data = {
        componentName: 'ZOOKEEPER_SERVER'
      }
      sinon.stub(controller, 'loadConfigs', Em.K);
      controller._doDeleteHostComponentSuccessCallback({}, {}, data);
      expect(controller.get('_deletedHostComponentResult')).to.be.null;
      expect(controller.get('fromDeleteZkServer')).to.be.true;
      expect(controller.loadConfigs.calledOnce).to.be.true;
      controller.loadConfigs.restore();
    });
    it('Not ZOOKEEPER_SERVER component', function () {
      var data = {
        componentName: 'COMP'
      }
      controller.set('fromDeleteZkServer', false);
      controller._doDeleteHostComponentSuccessCallback({}, {}, data);
      expect(controller.get('_deletedHostComponentResult')).to.be.null;
      expect(controller.get('fromDeleteZkServer')).to.be.false;
    });
  });

  describe('#upgradeComponentSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
      sinon.stub(controller, 'mimicWorkStatusChange', Em.K);
    });
    afterEach(function () {
      controller.mimicWorkStatusChange.restore();
      controller.showBackgroundOperationsPopup.restore();
    });
    it('testMode is true', function () {
      App.set('testMode', true);

      controller.upgradeComponentSuccessCallback({}, {}, {component: "COMP"});
      expect(controller.mimicWorkStatusChange.calledWith("COMP", App.HostComponentStatus.starting, App.HostComponentStatus.started)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('testMode is false', function () {
      App.set('testMode', false);

      controller.upgradeComponentSuccessCallback({}, {}, {component: "COMP"});
      expect(controller.mimicWorkStatusChange.called).to.be.false;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#refreshComponentConfigsSuccessCallback()', function () {
    it('call showBackgroundOperationsPopup', function () {
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
      controller.refreshComponentConfigsSuccessCallback();
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
      controller.showBackgroundOperationsPopup.restore();
    });
  });

  describe('#checkZkConfigs()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'removeObserver');
      sinon.stub(controller, 'loadConfigs');
    });
    afterEach(function () {
      controller.loadConfigs.restore();
      controller.removeObserver.restore();
      App.router.get.restore();
    });
    it('No operations of ZOOKEEPER_SERVER', function () {
      sinon.stub(App.router, 'get').withArgs('backgroundOperationsController.services').returns([]);
      controller.checkZkConfigs();
      expect(controller.removeObserver.called).to.be.false;
      expect(controller.loadConfigs.called).to.be.false;
    });
    it('Operation of ZOOKEEPER_SERVER running', function () {
      sinon.stub(App.router, 'get').withArgs('backgroundOperationsController.services').returns([Em.Object.create({
        id: 1,
        isRunning: true
      })]);
      controller.set('zkRequestId', 1);
      controller.checkZkConfigs();
      expect(controller.removeObserver.called).to.be.false;
      expect(controller.loadConfigs.called).to.be.false;
    });
    it('Operation of ZOOKEEPER_SERVER finished', function () {
      sinon.stub(App.router, 'get').withArgs('backgroundOperationsController.services').returns([Em.Object.create({
        id: 1
      })]);
      var clock = sinon.useFakeTimers();
      controller.set('zkRequestId', 1);
      controller.checkZkConfigs();
      expect(controller.removeObserver.calledWith('App.router.backgroundOperationsController.serviceTimestamp', controller, controller.checkZkConfigs)).to.be.true;
      clock.tick(App.get('componentsUpdateInterval'));
      expect(controller.loadConfigs.calledOnce).to.be.true;
      clock.restore();
    });
  });

  describe('#_doDeleteHostComponentErrorCallback()', function () {
    it('call showBackgroundOperationsPopup', function () {
      controller._doDeleteHostComponentErrorCallback({}, 'textStatus', {}, {url: 'url'});
      expect(controller.get('_deletedHostComponentResult')).to.be.eql({xhr: {}, url: 'url', method: 'DELETE'});
    });
  });

  describe('#installComponentSuccessCallback()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'showBackgroundOperationsPopup', Em.K);
      sinon.stub(controller, 'mimicWorkStatusChange', Em.K);
    });
    afterEach(function () {
      controller.mimicWorkStatusChange.restore();
      controller.showBackgroundOperationsPopup.restore();
    });
    it('testMode is true', function () {
      App.set('testMode', true);

      controller.installComponentSuccessCallback({}, {}, {component: "COMP"});
      expect(controller.mimicWorkStatusChange.calledWith("COMP", App.HostComponentStatus.installing, App.HostComponentStatus.stopped)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('testMode is false', function () {
      App.set('testMode', false);

      controller.installComponentSuccessCallback({}, {}, {component: "COMP"});
      expect(controller.mimicWorkStatusChange.called).to.be.false;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#showHbaseActiveWarning()', function () {
    it('popup should be displayed', function () {
      sinon.spy(App.ModalPopup, "show");
      var popup = controller.showHbaseActiveWarning(Em.Object.create({service: {}}));
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      App.ModalPopup.show.restore();
    });
  });

  describe('#updateHost()', function () {
    it('popup should be displayed', function () {
      sinon.stub(batchUtils, "infoPassiveState", Em.K);
      controller.updateHost({}, {}, {passive_state: 'state'});
      expect(controller.get('content.passiveState')).to.equal('state');
      expect(batchUtils.infoPassiveState.calledWith('state')).to.be.true;
      batchUtils.infoPassiveState.restore();
    });
  });

  describe('#updateComponentPassiveState()', function () {
    it('popup should be displayed', function () {
      controller.set('content.hostName', 'host1');
      var component = Em.Object.create({
        componentName: 'COMP1'
      });
      controller.updateComponentPassiveState(component, 'state', 'message');
      expect(App.ajax.send.getCall(0).args[0].data).to.be.eql({
        "hostName": "host1",
        "componentName": "COMP1",
        "component": component,
        "passive_state": "state",
        "context": "message"
      });
    });
  });

  describe('#updateHostComponent()', function () {
    it('popup should be displayed', function () {
      sinon.stub(batchUtils, "infoPassiveState", Em.K);
      var params = {
        component: Em.Object.create(),
        passive_state: 'state'
      }
      controller.updateHostComponent({}, {}, params);
      expect(params.component.get('passiveState')).to.equal('state');
      expect(batchUtils.infoPassiveState.calledWith('state')).to.be.true;
      batchUtils.infoPassiveState.restore();
    });
  });

  describe('#toggleMaintenanceMode()', function () {
    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, 'updateComponentPassiveState');
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.updateComponentPassiveState.restore();
    });
    it('passive state is ON', function () {
      var event = {context: Em.Object.create({
        passiveState: 'ON'
      })};
      var popup = controller.toggleMaintenanceMode(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.updateComponentPassiveState.calledWith(Em.Object.create({
        passiveState: 'ON'
      }), 'OFF')).to.be.true;
    });
    it('passive state is OFF', function () {
      var event = {context: Em.Object.create({
        passiveState: 'OFF'
      })};
      var popup = controller.toggleMaintenanceMode(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.updateComponentPassiveState.calledWith(Em.Object.create({
        passiveState: 'OFF'
      }), 'ON')).to.be.true;
    });
  });

  describe('#reinstallClients()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'sendComponentCommand');
    });
    afterEach(function () {
      controller.sendComponentCommand.restore();
    });
    it('No clients to install', function () {
      var event = {context: [
        Em.Object.create({
          workStatus: 'INSTALLED'
        })
      ]};
      controller.reinstallClients(event);
      expect(controller.sendComponentCommand.called).to.be.false;
    });
    it('No clients to install', function () {
      var event = {context: [
        Em.Object.create({
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          workStatus: 'INIT'
        }),
        Em.Object.create({
          workStatus: 'INSTALL_FAILED'
        })
      ]};
      controller.reinstallClients(event);
      expect(controller.sendComponentCommand.calledWith([
        Em.Object.create({
          workStatus: 'INIT'
        }),
        Em.Object.create({
          workStatus: 'INSTALL_FAILED'
        })], Em.I18n.t('host.host.details.installClients'), 'INSTALLED')).to.be.true;
    });
  });

  describe("#executeCustomCommandSuccessCallback()", function () {
    it("BO popup should be shown", function () {
      var mock = {
        showPopup: Em.K
      };
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'showPopup');
      var data = {
        Requests: {
          id: 1
        }
      };
      controller.executeCustomCommandSuccessCallback(data, {}, {});

      expect(App.router.get.calledWith('backgroundOperationsController')).to.be.true;
      expect(mock.showPopup.calledOnce).to.be.true;
      App.router.get.restore();
      mock.showPopup.restore();
    });
  });

  describe("#executeCustomCommandErrorCallback()", function () {
    beforeEach(function () {
      sinon.stub($, 'parseJSON');
      sinon.spy(App, 'showAlertPopup');
    });
    afterEach(function () {
      App.showAlertPopup.restore();
      $.parseJSON.restore();
    });
    it("data empty", function () {
      controller.executeCustomCommandErrorCallback(null);

      expect(App.showAlertPopup.calledWith(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), Em.I18n.t('services.service.actions.run.executeCustomCommand.error'))).to.be.true;
      expect($.parseJSON.called).to.be.false;
    });
    it("responseText empty", function () {
      var data = {
        responseText: null
      };
      controller.executeCustomCommandErrorCallback(data);

      expect(App.showAlertPopup.calledWith(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), Em.I18n.t('services.service.actions.run.executeCustomCommand.error'))).to.be.true;
      expect($.parseJSON.called).to.be.false;
    });
    it("data empty", function () {
      var data = {
        responseText: "test"
      };
      controller.executeCustomCommandErrorCallback(data);
      expect(App.showAlertPopup.calledWith(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), Em.I18n.t('services.service.actions.run.executeCustomCommand.error'))).to.be.true;
      expect($.parseJSON.calledWith('test')).to.be.true;
    });
  });

  describe("#doDeleteHost()", function() {
    beforeEach(function(){
      controller.set('fromDeleteHost', false);
      controller.set('content.hostName', 'host1');
      sinon.stub(controller, '_doDeleteHostComponent', function (comp, callback) {
        callback();
      });
    });
    afterEach(function(){
      controller._doDeleteHostComponent.restore();
    });
    it("Host has no components", function() {
      controller.set('content.hostComponents', Em.A([]));
      controller.doDeleteHost(Em.K);
      expect(controller.get('fromDeleteHost')).to.be.true;
      expect(App.ajax.send.getCall(0).args[0].data.hostName).to.be.equal('host1');
      expect(App.ajax.send.getCall(0).args[0].name).to.be.equal('common.delete.host');
    });
    it("Host has components", function() {
      controller.set('content.hostComponents', Em.A([Em.Object.create({
        componentName: 'COMP1'
      })]));
      controller.doDeleteHost(Em.K);
      expect(controller._doDeleteHostComponent.calledWith(Em.Object.create({
        componentName: 'COMP1'
      }))).to.be.true;
      expect(controller.get('fromDeleteHost')).to.be.true;
      expect(App.ajax.send.getCall(0).args[0].data.hostName).to.be.equal('host1');
      expect(App.ajax.send.getCall(0).args[0].name).to.be.equal('common.delete.host');
    });
  });

  describe("#deleteHostSuccessCallback", function() {
    it("call updateHost", function() {
      var mock = {
        updateHost: function(callback){
          callback();
        },
        getAllHostNames: Em.K
      };
      sinon.stub(App.router, 'get').withArgs('updateController').returns(mock).withArgs('clusterController').returns(mock);
      sinon.spy(mock, 'updateHost');
      sinon.spy(mock, 'getAllHostNames');
      sinon.stub(controller, 'loadConfigs', Em.K);
      sinon.stub(App.router, 'transitionTo', Em.K);

      controller.deleteHostSuccessCallback();
      expect(App.router.get.calledWith('updateController')).to.be.true;
      expect(mock.updateHost.calledOnce).to.be.true;
      expect(controller.loadConfigs.calledOnce).to.be.true;
      expect(App.router.transitionTo.calledWith('hosts.index')).to.be.true;
      expect(App.router.get.calledWith('clusterController')).to.be.true;
      expect(mock.getAllHostNames.calledOnce).to.be.true;

      App.router.get.restore();
      mock.updateHost.restore();
      mock.getAllHostNames.restore();
      controller.loadConfigs.restore();
      App.router.transitionTo.restore();
    });
  });

  describe("#deleteHostErrorCallback", function() {
    it("call defaultErrorHandler", function() {
      sinon.stub(controller, 'loadConfigs', Em.K);
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
      controller.deleteHostErrorCallback({status: 'status', statusText: "statusText"}, 'textStatus', 'errorThrown', {url: 'url'});
      expect(controller.loadConfigs.calledOnce).to.be.true;
      expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
      App.ajax.defaultErrorHandler.restore();
      controller.loadConfigs.restore();
    });
  });
});
