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
require('mixins/main/host/details/host_components/install_component');

var installComponent;

describe('App.InstallComponent', function () {

  beforeEach(function () {
    installComponent = Em.Object.create(App.InstallComponent);
  });

  describe("#installHostComponentCall()", function() {
    var component = Em.Object.create({
      componentName: 'C1',
      displayName: 'c1'
    });

    beforeEach(function() {
      sinon.stub(installComponent, 'updateAndCreateServiceComponent').returns({done: Em.clb});
      sinon.stub(App.ajax, 'send');
      installComponent.installHostComponentCall('host1', component);
    });
    afterEach(function() {
      installComponent.updateAndCreateServiceComponent.restore();
      App.ajax.send.restore();
    });

    it("updateAndCreateServiceComponent should be called", function() {
      expect(installComponent.updateAndCreateServiceComponent.calledWith('C1')).to.be.true;
    });

    it("App.ajax.send should be called", function() {
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'host.host_component.add_new_component',
        sender: installComponent,
        data: {
          hostName: 'host1',
          component: component,
          data: JSON.stringify({
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installHostComponent') + " " + 'c1'
            },
            Body: {
              host_components: [
                {
                  HostRoles: {
                    component_name: 'C1'
                  }
                }
              ]
            }
          })
        },
        success: 'addNewComponentSuccessCallback',
        error: 'ajaxErrorCallback'
      });
    });
  });

  describe("#addNewComponentSuccessCallback()", function() {
    var params = {
      hostName: 'host1',
      component: Em.Object.create({
        componentName: 'C1',
        serviceName: 'S1',
        displayName: 'c1'
      })
    };

    beforeEach(function() {
      sinon.stub(App.ajax, 'send');
    });
    afterEach(function() {
      App.ajax.send.restore();
    });


    it("App.ajax.send should be called", function() {
      installComponent.addNewComponentSuccessCallback({}, {}, params);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'common.host.host_component.update',
        sender: App.router.get('mainHostDetailsController'),
        data: {
          hostName: 'host1',
          componentName: 'C1',
          serviceName: 'S1',
          component: params.component,
          "context": Em.I18n.t('requestInfo.installNewHostComponent') + " " + 'c1',
          HostRoles: {
            state: 'INSTALLED'
          },
          urlParams: "HostRoles/state=INIT"
        },
        success: 'installNewComponentSuccessCallback',
        error: 'ajaxErrorCallback'
      });
    });
  });

  describe("#ajaxErrorCallback()", function() {

    beforeEach(function() {
      sinon.stub(App.ajax, 'defaultErrorHandler');
    });
    afterEach(function() {
      App.ajax.defaultErrorHandler.restore();
    });

    it("App.ajax.defaultErrorHandler should be called", function() {
      installComponent.ajaxErrorCallback({}, {}, 'error', {method: 'method1', url: 'url1'}, {});
      expect(App.ajax.defaultErrorHandler.calledWith({}, 'url1', 'method1'));
    });
  });

  describe("#updateAndCreateServiceComponent()", function() {

    var updater = {
      updateComponentsState: Em.clb,
      updateServiceMetric: Em.clb
    };

    beforeEach(function() {
      sinon.spy(updater, 'updateComponentsState');
      sinon.spy(updater, 'updateServiceMetric');
      sinon.stub(App.router, 'get').returns(updater);
      sinon.stub(installComponent, 'createServiceComponent');
      installComponent.updateAndCreateServiceComponent('C1');
    });
    afterEach(function() {
      App.router.get.restore();
      installComponent.createServiceComponent.restore();
      updater.updateComponentsState.restore();
      updater.updateServiceMetric.restore();
    });

    it("updater.updateComponentsState should be called", function() {
      expect(updater.updateComponentsState.calledOnce).to.be.true;
    });

    it("updater.updateServiceMetric should be called", function() {
      expect(updater.updateServiceMetric.calledOnce).to.be.true;
    });

    it("createServiceComponent should be called", function() {
      expect(installComponent.createServiceComponent.calledWith('C1')).to.be.true;
    });
  });

  describe("#createServiceComponent()", function() {
    var dfd = {resolve: Em.K};

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([Em.Object.create({
        componentName: 'C2',
        serviceName: 'S1'
      })]);
      sinon.spy(dfd, 'resolve');
      sinon.stub(App.ajax, 'send').returns({complete: Em.clb});
      this.mock = sinon.stub(App.Service, 'find');
      this.mock.returns([{serviceName: "S1"}]);
      this.mock.withArgs('S1').returns(Em.Object.create({serviceComponents: ['C1']}))

    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
      dfd.resolve.restore();
      this.mock.restore();
      App.ajax.send.restore();
    });

    it("component already created", function() {
      expect(installComponent.createServiceComponent('C1', dfd)).to.be.null;
      expect(dfd.resolve.calledOnce).to.be.true;
    });

    it("component not created", function() {
      installComponent.createServiceComponent('C2', dfd);
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'common.create_component',
        sender: installComponent,
        data: {
          componentName: 'C2',
          serviceName: 'S1'
        }
      });
      expect(dfd.resolve.calledOnce).to.be.true;
    });
  });
});