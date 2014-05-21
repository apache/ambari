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


describe('App.MainHostDetailsController', function () {

  var controller = App.MainHostDetailsController.create({
    getSecurityStatus: function () {
      return this.get('mockSecurityStatus');
    },
    mockSecurityStatus: false
  });

  describe('#serviceActiveComponents', function () {

    it('No host-components', function () {
      controller.set('content.hostComponents', []);
      expect(controller.get('serviceActiveComponents')).to.be.empty;
    });

    it('No host-components in active state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        service: {
          isInPassive: true
        }
      })]);
      expect(controller.get('serviceActiveComponents')).to.be.empty;
    });
    it('Host-components in active state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        service: {
          isInPassive: false
        }
      })]);
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
      controller.set('serviceActiveComponents', [Em.Object.create({
        isClient: true
      })]);
      expect(controller.get('serviceNonClientActiveComponents')).to.be.empty;
    });
    it('Active host-component is not client', function () {
      controller.set('serviceActiveComponents', [Em.Object.create({
        isClient: false
      })]);
      expect(controller.get('serviceNonClientActiveComponents')).to.eql( [Em.Object.create({
        isClient: false
      })]);
    });
  });

  describe('#startComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "sendStartComponentCommand", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendStartComponentCommand.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.startComponent({context: Em.Object.create({'displayName': 'Comp1'})});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendStartComponentCommand.calledOnce).to.be.true;
    });
  });

  describe('#sendStartComponentCommand()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      var component = Em.Object.create({
        componentName: 'comp1'
      });
      controller.sendStartComponentCommand(component, '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#getDataToSend()', function () {
    var component = Em.Object.create({
      componentName: 'comp1',
      host: {
        hostName: 'host1'
      },
      service: {
        serviceName: 'serv1'
      }
    });
    it('should return correct query info', function () {
      controller.set("content.hostName", "host1");
      expect(controller.getDataToSend('STATE1', 'context', component)).to.deep.eql({
        RequestInfo: {
          "context": 'context',
          "operation_level": {
            "cluster_name": "tdk",
            "host_name": "host1",
            "hostcomponent_name": "comp1",
            "service_name": "serv1",
            "level": "HOST_COMPONENT"
          }
        },
        Body: {
          HostRoles: {
            state: 'STATE1'
          }
        }
      });
      expect(controller.getDataToSend('STATE1', 'context', [component])).to.deep.eql({
        RequestInfo: {
          "context": 'context',
          "operation_level": {
            "cluster_name": "tdk",
            "host_name": "host1",
            "level": "HOST"
          }
        },
        Body: {
          HostRoles: {
            state: 'STATE1'
          }
        }
      });
    });
  });

  describe('#setComponentToData()', function () {
    var testCases = [
      {
        title: 'dataToSend is null',
        content: {
          data: {},
          dataToSend: null,
          component: {}
        },
        result: {
          output: false,
          data: {}
        }
      },
      {
        title: 'component is null',
        content: {
          data: {},
          dataToSend: {},
          component: null
        },
        result: {
          output: false,
          data: {}
        }
      },
      {
        title: 'data is null',
        content: {
          data: null,
          dataToSend: {},
          component: {}
        },
        result: {
          output: false,
          data: null
        }
      },
      {
        title: 'component is object',
        content: {
          data: {},
          dataToSend: {},
          component: Em.Object.create({componentName: 'comp1'})
        },
        result: {
          output: true,
          data: {
            componentName: 'comp1',
            data: '{}'
          }
        }
      },
      {
        title: 'component is array',
        content: {
          data: {},
          dataToSend: {RequestInfo: {}},
          component: [Em.Object.create({componentName: 'comp1'})]
        },
        result: {
          output: true,
          data: {
            data: '{\"RequestInfo\":{\"query\":\"HostRoles/component_name.in(comp1)\"}}'
          }
        }
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.setComponentToData(test.content.data, test.content.dataToSend, test.content.component)).to.equal(test.result.output);
        expect(test.content.data).to.eql(test.result.data);
      });
    });
  });

  describe('#deleteComponent()', function () {

    beforeEach(function () {
      sinon.spy(App.ModalPopup, "show");
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('confirm popup should be displayed', function () {
      var event = {
        context: Em.Object.create({})
      };
      controller.deleteComponent(event);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe('#_doDeleteHostComponent()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('delete request should be sent, component is null', function () {
      controller.set('_deletedHostComponentResult', null);
      expect(controller._doDeleteHostComponent()).to.equal(null);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
    it('delete request should be sent, component is correct', function () {
      controller.set('_deletedHostComponentResult', {});
      expect(controller._doDeleteHostComponent(Em.Object.create({componentName: 'COMP1'}))).to.eql({});
      expect(App.ajax.send.calledOnce).to.be.true;
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
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      App.ajax.send.restore();
    });

    it('confirm popup should be displayed', function () {
      var popup = controller.upgradeComponent({context: Em.Object.create()});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#stopComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "sendStopComponentCommand", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendStopComponentCommand.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.stopComponent({context: Em.Object.create({'displayName': 'Comp1'})});
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.sendStopComponentCommand.calledOnce).to.be.true;
    });
  });

  describe('#sendStopComponentCommand()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      var component = Em.Object.create({
        componentName: 'comp1'
      });
      controller.sendStopComponentCommand(component, '');
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

  describe('#addComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
      sinon.stub(controller, "addClientComponent", Em.K);
      sinon.stub(controller, "primary", Em.K);
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
        componentName: 'COMP1'
      })};
      controller.set('mockSecurityStatus', true);
      var popup = controller.addComponent(event);
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(controller.primary.calledWith(Em.Object.create({
        componentName: 'COMP1'
      }))).to.be.true;
    });
    it('add slave component, securityEnabled = false', function () {
      var event = {context: Em.Object.create({
        componentName: 'COMP1'
      })};
      controller.set('mockSecurityStatus', false);
      controller.addComponent(event);
      expect(controller.addClientComponent.calledWith(Em.Object.create({
        componentName: 'COMP1'
      }))).to.be.true;
    });
    it('add CLIENTS', function () {
      var event = {context: Em.Object.create({
        componentName: 'CLIENTS'
      })};
      controller.set('mockSecurityStatus', true);
      controller.addComponent(event);
      expect(controller.addClientComponent.calledWith(Em.Object.create({
        componentName: 'CLIENTS'
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

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      var component = Em.Object.create({
        componentName: 'COMP1',
        displayName: 'comp1'
      });
      controller.primary(component);
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#addNewComponentSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      controller.addNewComponentSuccessCallback({}, {}, {component: Em.Object.create()});
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

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

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

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

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
      var data = {Clusters: {desired_configs: {'webhcat-site': {tag: 1}}}};
      expect(controller.constructConfigUrlParams(data)).to.eql(['(type=webhcat-site&tag=1)']);
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
  });

  describe('#loadConfigsSuccessCallback()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
      sinon.stub(controller, "constructConfigUrlParams", function () {
        return this.get('mockUrlParams');
      });
    });
    afterEach(function () {
      App.ajax.send.restore();
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
      sinon.spy(App.ajax, "send");
    });
    afterEach(function () {
      controller.getZkServerHosts.restore();
      controller.concatZkNames.restore();
      controller.setZKConfigs.restore();
      App.ajax.send.restore();
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
    it('storm-site is present', function () {
      var configs = {'storm-site': {}};
      expect(controller.setZKConfigs(configs, '', ["host1", 'host2'])).to.be.true;
      expect(configs).to.eql({"storm-site": {
        "storm.zookeeper.servers": "['host1','host2']"
      }});
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
    controller.set('content', {});

    it('No ZooKeeper hosts, fromDeleteHost = false', function () {
      App.HostComponent.find().clear();
      controller.set('fromDeleteHost', false);
      expect(controller.getZkServerHosts()).to.be.empty;
    });
    it('No ZooKeeper hosts, fromDeleteHost = true', function () {
      controller.set('fromDeleteHost', true);
      expect(controller.getZkServerHosts()).to.be.empty;
      expect(controller.get('fromDeleteHost')).to.be.false;
    });
    it('One ZooKeeper host, fromDeleteHost = false', function () {
      controller.set('fromDeleteHost', false);
      App.store.load(App.HostComponent, {
        id: 'ZOOKEEPER_SERVER_host1',
        component_name: 'ZOOKEEPER_SERVER',
        host_id: 'host1'
      });
      expect(controller.getZkServerHosts()).to.eql(['host1']);
    });
    it('One ZooKeeper host match current host name, fromDeleteHost = true', function () {
      controller.set('fromDeleteHost', true);
      controller.set('content.hostName', 'host1');
      expect(controller.getZkServerHosts()).to.be.empty;
      expect(controller.get('fromDeleteHost')).to.be.false;
    });
    it('One ZooKeeper host does not match current host name, fromDeleteHost = true', function () {
      controller.set('fromDeleteHost', true);
      controller.set('content.hostName', 'host2');
      expect(controller.getZkServerHosts()[0]).to.equal("host1");
      expect(controller.get('fromDeleteHost')).to.be.false;
    });
  });

  describe('#installComponent()', function () {

    beforeEach(function () {
      sinon.spy(App.ModalPopup, "show");
      sinon.stub(App.ajax, "send", Em.K);
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
      App.ajax.send.restore();
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

  describe('#runDecommission()', function () {

    beforeEach(function () {
      sinon.stub(controller, "doDecommission", Em.K);
      sinon.stub(controller, "showBackgroundOperationsPopup", Em.K);
      sinon.stub(controller, "doDecommissionRegionServer", Em.K);
    });

    afterEach(function () {
      controller.doDecommissionRegionServer.restore();
      controller.doDecommission.restore();
      controller.showBackgroundOperationsPopup.restore();
    });

    it('HDFS service', function () {
      controller.runDecommission('host1', 'HDFS');
      expect(controller.doDecommission.calledWith('host1', 'HDFS', "NAMENODE", "DATANODE")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('YARN service', function () {
      controller.runDecommission('host1', 'YARN');
      expect(controller.doDecommission.calledWith('host1', 'YARN', "RESOURCEMANAGER", "NODEMANAGER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('MAPREDUCE service', function () {
      controller.runDecommission('host1', 'MAPREDUCE');
      expect(controller.doDecommission.calledWith('host1', 'MAPREDUCE', "JOBTRACKER", "TASKTRACKER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
    it('HBASE service', function () {
      controller.runDecommission('host1', 'HBASE');
      expect(controller.doDecommissionRegionServer.calledWith('host1', 'HBASE', "HBASE_MASTER", "HBASE_REGIONSERVER")).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
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

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      controller.doDecommission('', '', '', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#doDecommissionRegionServer()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      controller.doDecommissionRegionServer('', '', '', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  /**
   * TODO uncomment test when final rules will be implemented into warnBeforeDecommission function
   */
 /* describe('#warnBeforeDecommission()', function () {

    beforeEach(function () {
      sinon.stub(controller, "doDecommissionRegionServer", Em.K);
      sinon.stub(App.ModalPopup, "show", Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
      controller.doDecommissionRegionServer.restore();
    });

    it('Component in passive state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'HBASE_REGIONSERVER',
        passiveState: 'ON'
      })]);
      controller.warnBeforeDecommission('host1', 'HBASE', 'HBASE_REGIONSERVER', 'SLAVE');
      expect(App.ModalPopup.show.called).to.be.false;
      expect(controller.doDecommissionRegionServer.calledWith('host1', 'HBASE', 'HBASE_REGIONSERVER', 'SLAVE')).to.be.true;
    });
    it('Component is not in passive state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'HBASE_REGIONSERVER',
        passiveState: 'OFF'
      })]);
      controller.warnBeforeDecommission('host1', 'HBASE', 'HBASE_REGIONSERVER', 'SLAVE');
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      expect(controller.doDecommissionRegionServer.called).to.be.false;
    });
  });*/

  describe('#doRecommissionAndStart()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

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
      var data = {resources: [{RequestSchedule: {}}]};
      expect(controller.decommissionSuccessCallback(data)).to.be.true;
      expect(controller.showBackgroundOperationsPopup.calledOnce).to.be.true;
    });
  });

  describe('#doRecommissionAndRestart()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

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
      controller.set('content.isNotHeartBeating', false);
      controller.doAction(option);
      expect(controller.doStartAllComponents.calledOnce).to.be.true;
    });
    it('"startAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "startAllComponents"}};
      controller.set('content.isNotHeartBeating', true);
      controller.doAction(option);
      expect(controller.doStartAllComponents.called).to.be.false;
    });
    it('"stopAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "stopAllComponents"}};
      controller.set('content.isNotHeartBeating', false);
      controller.doAction(option);
      expect(controller.doStopAllComponents.calledOnce).to.be.true;
    });
    it('"stopAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "stopAllComponents"}};
      controller.set('content.isNotHeartBeating', true);
      controller.doAction(option);
      expect(controller.doStopAllComponents.called).to.be.false;
    });
    it('"restartAllComponents" action, isNotHeartBeating = false', function () {
      var option = {context: {action: "restartAllComponents"}};
      controller.set('content.isNotHeartBeating', false);
      controller.doAction(option);
      expect(controller.doRestartAllComponents.calledOnce).to.be.true;
    });
    it('"restartAllComponents" action, isNotHeartBeating = true', function () {
      var option = {context: {action: "restartAllComponents"}};
      controller.set('content.isNotHeartBeating', true);
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

    beforeEach(function () {
      sinon.stub(App.ajax, "send", Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('Query should be sent', function () {
      controller.hostPassiveModeRequest('', '');
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#doStartAllComponents()', function () {

    beforeEach(function () {
      sinon.stub(App, "showConfirmationPopup", Em.K);
      sinon.stub(controller, "sendStartComponentCommand", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendStartComponentCommand.restore();
    });

    it('serviceNonClientActiveComponents is null', function () {
      controller.reopen({
        serviceNonClientActiveComponents: []
      });
      controller.set('serviceNonClientActiveComponents', null);
      controller.doStartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceNonClientActiveComponents is empty', function () {
      controller.set('serviceNonClientActiveComponents', []);
      controller.doStartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceNonClientActiveComponents is correct', function () {
      controller.set('serviceNonClientActiveComponents', [{}]);
      var popup = controller.doStartAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      /*popup.onPrimary();
      expect(controller.sendStartComponentCommand.calledWith([{}])).to.be.true;*/
    });
  });

  describe('#doStopAllComponents()', function () {

    beforeEach(function () {
      sinon.stub(App, "showConfirmationPopup", Em.K);
      sinon.stub(controller, "sendStartComponentCommand", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendStartComponentCommand.restore();
    });

    it('serviceNonClientActiveComponents is null', function () {
      controller.set('serviceNonClientActiveComponents', null);
      controller.doStopAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceNonClientActiveComponents is empty', function () {
      controller.set('serviceNonClientActiveComponents', []);
      controller.doStopAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceNonClientActiveComponents is correct', function () {
      controller.set('serviceNonClientActiveComponents', [{}]);
      var popup = controller.doStopAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      /*popup.onPrimary();
      expect(controller.sendStopComponentCommand.calledWith([{}])).to.be.true;*/
    });
  });

  describe('#doRestartAllComponents()', function () {

    beforeEach(function () {
      sinon.stub(App, "showConfirmationPopup", Em.K);
      sinon.stub(controller, "sendStartComponentCommand", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.sendStartComponentCommand.restore();
    });

    it('serviceActiveComponents is null', function () {
      controller.set('serviceActiveComponents', null);
      controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceActiveComponents is empty', function () {
      controller.set('serviceActiveComponents', []);
      controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.called).to.be.false;
    });
    it('serviceActiveComponents is correct', function () {
      controller.set('serviceActiveComponents', [{}]);
      var popup = controller.doRestartAllComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      /*popup.onPrimary();
       expect(controller.restartHostComponents.calledWith([{}])).to.be.true;*/
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
      controller.set('content.hostComponents', null);
      expect(controller.getHostComponentsInfo()).to.eql(result);
    });
    it('content.hostComponents is empty', function () {
      controller.set('content.hostComponents', []);
      expect(controller.getHostComponentsInfo()).to.eql(result);
    });
    it('content.hostComponents has ZOOKEEPER_SERVER', function () {
      App.HostComponent.find().clear();
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'ZOOKEEPER_SERVER',
        workStatus: 'INIT',
        isDeletable: true
      })]);
      expect(controller.getHostComponentsInfo().zkServerInstalled).to.be.true;
    });
    it('content.hostComponents has last component', function () {
      App.store.load(App.HostComponent, {
        id: 'TASKTRACKER_host1',
        component_name: 'TASKTRACKER'
      });
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'TASKTRACKER',
        displayName: 'TaskTracker',
        workStatus: 'INIT',
        isDeletable: true
      })]);
      expect(controller.getHostComponentsInfo().lastComponents).to.eql(['TaskTracker']);
      App.HostComponent.find().clear();
    });
    it('content.hostComponents has master non-deletable component', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'INIT',
        isDeletable: false,
        isMaster: true,
        displayName: 'ZK1'
      })]);
      expect(controller.getHostComponentsInfo().masterComponents).to.eql(['ZK1']);
      expect(controller.getHostComponentsInfo().nonDeletableComponents).to.eql(['ZK1']);
    });
    it('content.hostComponents has running component', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'STARTED',
        isDeletable: true,
        displayName: 'ZK1'
      })]);
      expect(controller.getHostComponentsInfo().runningComponents).to.eql(['ZK1']);
    });
    it('content.hostComponents has non-deletable component', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'INIT',
        isDeletable: false,
        displayName: 'ZK1'
      })]);
      expect(controller.getHostComponentsInfo().nonDeletableComponents).to.eql(['ZK1']);
    });
    it('content.hostComponents has component with UNKNOWN state', function () {
      controller.set('content.hostComponents', [Em.Object.create({
        componentName: 'TASKTRACKER',
        workStatus: 'UNKNOWN',
        isDeletable: false,
        displayName: 'ZK1'
      })]);
      expect(controller.getHostComponentsInfo().unknownComponents).to.eql(['ZK1']);
    });
  });

  describe('#validateAndDeleteHost()', function () {

    beforeEach(function () {
      sinon.stub(App, "showConfirmationPopup", Em.K);
      sinon.stub(controller, "getHostComponentsInfo", function(){
        return this.get('mockHostComponentsInfo');
      });
      sinon.stub(controller, "raiseDeleteComponentsError", Em.K);
      sinon.stub(controller, "_doDeleteHost", Em.K);
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
      controller.getHostComponentsInfo.restore();
      controller.raiseDeleteComponentsError.restore();
      controller._doDeleteHost.restore();
    });

    it('App.supports.deleteHost = false', function () {
      App.supports.deleteHost = false;
      expect(controller.validateAndDeleteHost()).to.be.false;
      App.supports.deleteHost = true;
    });
    it('masterComponents exist', function () {
      controller.set('mockHostComponentsInfo', {masterComponents: [{}]});
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith([{}], 'masterList')).to.be.true;
    });
    it('nonDeletableComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        masterComponents: [],
        nonDeletableComponents: [{}]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith([{}], 'nonDeletableList')).to.be.true;
    });
    it('runningComponents exist', function () {
      controller.set('mockHostComponentsInfo', {
        masterComponents: [],
        nonDeletableComponents: [],
        runningComponents: [{}]
      });
      controller.validateAndDeleteHost();
      expect(controller.raiseDeleteComponentsError.calledWith([{}], 'runningList')).to.be.true;
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
     /* popup.onPrimary();
      expect(controller._doDeleteHost.calledWith([], [])).to.be.true;*/
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
      expect(controller._doDeleteHost.calledWith([], [])).to.be.true;
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

  describe('#_doDeleteHost()', function () {

    beforeEach(function () {
      sinon.stub(App.ModalPopup, "show", Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('Popup should be displayed', function () {
      controller._doDeleteHost([], []);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
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
      controller.set('content.componentsWithStaleConfigs', [{}]);
      var popup = controller.restartAllStaleConfigComponents();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
      popup.onPrimary();
      expect(batchUtils.restartHostComponents.calledWith([{}])).to.be.true;
    });
  });

  describe('#moveComponent()', function () {

    beforeEach(function () {
      sinon.spy(App, "showConfirmationPopup");
    });
    afterEach(function () {
      App.showConfirmationPopup.restore();
    });

    it('popup should be displayed', function () {
      var popup = controller.moveComponent();
      expect(App.showConfirmationPopup.calledOnce).to.be.true;
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
});