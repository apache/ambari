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

require('mixins/main/service/configs/component_actions_by_configs');
var testHelpers = require('test/helpers');
var stringUtils = require('utils/string_utils');


var mixin;

describe('App.ComponentActionsByConfigs', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.ComponentActionsByConfigs, {
      stepConfigs: [],
      content: Em.Object.create()
    });
  });

  describe("#doConfigActions()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'doComponentDeleteActions');
      sinon.stub(mixin, 'doComponentAddActions');
      mixin.set('stepConfigs', [Em.Object.create({
        serviceName: 'S1',
        configs: [{
          configActionComponent: {}
        }]
      })]);
      mixin.set('content.serviceName', 'S1');
      mixin.doConfigActions();
    });

    afterEach(function() {
      mixin.doComponentDeleteActions.restore();
      mixin.doComponentAddActions.restore();
    });

    it("doComponentDeleteActions should be called", function() {
      expect(mixin.doComponentDeleteActions.calledWith([{
        configActionComponent: {}
      }])).to.be.true;
    });

    it("doComponentAddActions should be called", function() {
      expect(mixin.doComponentAddActions.calledWith([{
        configActionComponent: {}
      }])).to.be.true;
    });
  });

  describe("#isComponentActionsPresent()", function () {

    beforeEach(function() {
      this.mockDelete = sinon.stub(mixin, 'getComponentsToDelete').returns(1);
      this.mockAdd = sinon.stub(mixin, 'getComponentsToAdd').returns(1);
      mixin.set('stepConfigs', [Em.Object.create({
        serviceName: 'S1',
        configs: [{
          configActionComponent: {}
        }]
      })]);
      mixin.set('content.serviceName', 'S1');
    });

    afterEach(function() {
      this.mockAdd.restore();
      this.mockDelete.restore();
    });

    it("no delete or add components", function() {
      this.mockAdd.returns([]);
      this.mockDelete.returns([]);
      expect(mixin.isComponentActionsPresent()).to.be.false;
    });

    it("has delete and no add components", function() {
      this.mockAdd.returns([]);
      this.mockDelete.returns([{}]);
      expect(mixin.isComponentActionsPresent()).to.be.true;
    });

    it("no delete and has add components", function() {
      this.mockAdd.returns([{}]);
      this.mockDelete.returns([]);
      expect(mixin.isComponentActionsPresent()).to.be.true;
    });
  });

  describe("#getComponentsToDelete()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          hostName: 'host1'
        })
      ]);
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it("should return array of components to delete", function() {
      var configActionComponents = [
        {
          configActionComponent: {
            action: 'delete',
            componentName: 'C1',
            hostName: 'host1'
          }
        }
      ];
      expect(mixin.getComponentsToDelete(configActionComponents)).to.be.eql([{
        action: 'delete',
        componentName: 'C1',
        hostName: 'host1'
      }]);
    });
  });

  describe("#getComponentsToAdd()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          serviceName: 'S1'
        })
      ]);
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'S1',
          hostComponents: [
            Em.Object.create({
              componentName: 'C1',
              hostName: 'host2'
            })
          ]
        })
      ]);
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.Service.find.restore();
    });

    it("should return array of components to add", function() {
      var configActionComponents = [
        {
          configActionComponent: {
            action: 'add',
            componentName: 'C1',
            hostName: 'host1'
          }
        }
      ];
      expect(mixin.getComponentsToAdd(configActionComponents)).to.be.eql([{
        action: 'add',
        componentName: 'C1',
        hostName: 'host1'
      }]);
    });
  });

  describe("#doComponentDeleteActions()", function () {

    beforeEach(function() {
      this.mockDelete = sinon.stub(mixin, 'getComponentsToDelete');
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          displayName: 'c1'
        })
      ]);
      sinon.stub(mixin, 'setRefreshYarnQueueRequest');
      sinon.stub(mixin, 'getInstallHostComponentsRequest').returns({});
      sinon.stub(mixin, 'getDeleteHostComponentRequest').returns({});
      sinon.stub(mixin, 'setOrderIdForBatches');
    });

    afterEach(function() {
      this.mockDelete.restore();
      App.StackServiceComponent.find.restore();
      mixin.setRefreshYarnQueueRequest.restore();
      mixin.getDeleteHostComponentRequest.restore();
      mixin.setOrderIdForBatches.restore();
      mixin.getInstallHostComponentsRequest.restore();
    });

    it("App.ajax.send should not be called", function() {
      this.mockDelete.returns([]);
      mixin.doComponentDeleteActions();
      expect(testHelpers.findAjaxRequest('name', 'common.batch.request_schedules')).to.not.exists;
    });

    it("App.ajax.send should be called", function() {
      this.mockDelete.returns([{
        componentName: 'C1',
        hostName: 'host1'
      }]);
      mixin.doComponentDeleteActions();
      var args = testHelpers.findAjaxRequest('name', 'common.batch.request_schedules');
      expect(args[0]).to.be.eql({
        name: 'common.batch.request_schedules',
        sender: mixin,
        data: {
          intervalTimeSeconds: 1,
          tolerateSize: 0,
          batches: [{}, {}]
        }
      });
    });
  });

  describe("#doComponentAddActions()", function () {

    beforeEach(function() {
      this.mockAdd = sinon.stub(mixin, 'getComponentsToAdd');
      sinon.stub(mixin, 'getDependentComponents').returns([]);
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1',
          displayName: 'c1'
        })
      ]);
      sinon.stub(mixin, 'setCreateComponentRequest');
      sinon.stub(mixin, 'getCreateHostComponentsRequest').returns({});
      sinon.stub(mixin, 'getInstallHostComponentsRequest').returns({});
      sinon.stub(mixin, 'setRefreshYarnQueueRequest');
      sinon.stub(mixin, 'getStartHostComponentsRequest').returns({});
      sinon.stub(mixin, 'setOrderIdForBatches');
      sinon.stub(stringUtils, 'getFormattedStringFromArray').returns('c1');
    });

    afterEach(function() {
      this.mockAdd.restore();
      mixin.getDependentComponents.restore();
      App.StackServiceComponent.find.restore();
      mixin.setCreateComponentRequest.restore();
      mixin.getCreateHostComponentsRequest.restore();
      mixin.getInstallHostComponentsRequest.restore();
      mixin.setRefreshYarnQueueRequest.restore();
      mixin.getStartHostComponentsRequest.restore();
      mixin.setOrderIdForBatches.restore();
      stringUtils.getFormattedStringFromArray.restore();
    });

    it("App.ajax.send should not be called", function() {
      this.mockAdd.returns([]);
      mixin.doComponentAddActions();
      expect(testHelpers.findAjaxRequest('name', 'common.batch.request_schedules')).to.not.exists;
    });

    it("App.ajax.send should be called", function() {
      this.mockAdd.returns([{
        componentName: 'C1',
        hostName: 'host1',
        isClient: false
      }]);
      mixin.doComponentAddActions();
      var args = testHelpers.findAjaxRequest('name', 'common.batch.request_schedules');
      expect(args[0]).to.be.eql({
        name: 'common.batch.request_schedules',
        sender: mixin,
        data: {
          intervalTimeSeconds: 1,
          tolerateSize: 0,
          batches: [{}, {}, {}]
        }
      });
    });
  });

  describe("#getDependentComponents()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        dependencies: [{
          scope: 'host',
          componentName: 'C2'
        }],
        isClient: false
      }));
      sinon.stub(App.HostComponent, 'find').returns([]);
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.HostComponent.find.restore();
    });

    it("should return dependent components", function() {
      var componentsToAdd = [{
        componentName: 'C1',
        hostName: 'host1'
      }];
      expect(mixin.getDependentComponents(componentsToAdd)).to.be.eql([
        {
          componentName: 'C2',
          hostName: 'host1',
          isClient: false
        }
      ]);
    });
  });

  describe("#setOrderIdForBatches()", function () {

    it("should set order_id", function() {
      var batches = [{}, {}, {}];
      mixin.setOrderIdForBatches(batches);
      expect(batches).to.be.eql([
        {order_id: 1},
        {order_id: 2},
        {order_id: 3}
      ]);
    });
  });

  describe("#getCreateHostComponentsRequest()", function () {

    it("App.ajax.send should be called", function() {
      expect(mixin.getCreateHostComponentsRequest('host1', ['C1'])).to.be.eql({
        "type": 'POST',
        "uri": "/api/v1/clusters/mycluster/hosts",
        "RequestBodyInfo": {
          "RequestInfo": {
            "query": "Hosts/host_name.in(host1)"
          },
          "Body": {
            "host_components": [{
              "HostRoles": {
                "component_name": 'C1'
              }
            }]
          }
        }
      });
    });
  });

  describe("#getInstallHostComponentsRequest()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getUpdateHostComponentsRequest').returns({});
    });

    afterEach(function() {
      mixin.getUpdateHostComponentsRequest.restore();
    });

    it("should return request object", function() {
      expect(mixin.getInstallHostComponentsRequest('host1', [{componentName: 'C1'}])).to.be.eql({});
      expect(mixin.getUpdateHostComponentsRequest.calledWith(
        'host1',
        [{componentName: 'C1'}],
        App.HostComponentStatus.stopped,
        Em.I18n.t('requestInfo.installComponents'))).to.be.true;
    });
  });

  describe("#getStartHostComponentsRequest()", function () {

    beforeEach(function() {
      sinon.stub(mixin, 'getUpdateHostComponentsRequest').returns({});
    });

    afterEach(function() {
      mixin.getUpdateHostComponentsRequest.restore();
    });

    it("should return request object", function() {
      expect(mixin.getStartHostComponentsRequest('host1', [{componentName: 'C1'}])).to.be.eql({});
      expect(mixin.getUpdateHostComponentsRequest.calledWith(
        'host1',
        [{componentName: 'C1'}],
        App.HostComponentStatus.started,
        Em.I18n.t('requestInfo.startHostComponents'))).to.be.true;
    });
  });

  describe("#getUpdateHostComponentsRequest()", function () {

    it("should return request object", function() {
      expect(mixin.getUpdateHostComponentsRequest('host1', ['C1', 'C2'], 'INSTALLED', 'context')).to.be.eql({
        "type": 'PUT',
        "uri": "/api/v1/clusters/mycluster/hosts/host1/host_components",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": 'context',
            "operation_level": {
              "level": "HOST",
              "cluster_name": 'mycluster',
              "host_names": 'host1'
            },
            "query": "HostRoles/component_name.in(C1,C2)"
          },
          "Body": {
            "HostRoles": {
              "state": 'INSTALLED'
            }
          }
        }
      });
    });
  });

  describe("#getDeleteHostComponentRequest()", function () {

    it("should return request object", function() {
      expect(mixin.getDeleteHostComponentRequest('host1', 'C1')).to.be.eql({
        "type": 'DELETE',
        "uri": "/api/v1/clusters/mycluster/hosts/host1/host_components/C1"
      });
    });
  });

  describe("#setCreateComponentRequest()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([Em.Object.create({
        componentName: 'C1',
        serviceName: 'S1'
      })]);
      sinon.stub(App.Service, 'find').returns([Em.Object.create({
        serviceName: 'S1',
        serviceComponents: []
      })]);
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.Service.find.restore();
    });

    it("should add batch", function() {
      var batches = [];
      mixin.setCreateComponentRequest(batches, ["C1"]);
      expect(batches).to.be.eql([
        {
          "type": 'POST',
          "uri": "/api/v1/clusters/mycluster/services/S1/components/C1"
        }
      ]);

    });
  });

  describe("#setRefreshYarnQueueRequest()", function () {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns(Em.Object.create({
        hostComponents: [Em.Object.create({
          componentName: 'RESOURCEMANAGER',
          hostName: 'host1'
        })]
      }));
    });

    afterEach(function() {
      App.Service.find.restore();
    });

    it("should not add a batch", function() {
      var batches = [];
      mixin.set('allConfigs', []);
      mixin.setRefreshYarnQueueRequest(batches);
      expect(batches).to.be.empty;
    });

    it("should add a batch", function() {
      var batches = [];
      mixin.set('allConfigs', [Em.Object.create({
        filename: 'capacity-scheduler.xml',
        value: 'val1',
        initialValue: 'val2'
      })]);
      mixin.setRefreshYarnQueueRequest(batches);
      expect(batches).to.be.eql([{
        "type": 'POST',
        "uri": "/api/v1/clusters/mycluster/requests",
        "RequestBodyInfo": {
          "RequestInfo": {
            "context": Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
            "command": "REFRESHQUEUES",
            "parameters/forceRefreshConfigTags": "capacity-scheduler"
          },
          "Requests/resource_filters": [
            {
              service_name: "YARN",
              component_name: "RESOURCEMANAGER",
              hosts: "host1"
            }
          ]
        }
      }]);
    });
  });

});