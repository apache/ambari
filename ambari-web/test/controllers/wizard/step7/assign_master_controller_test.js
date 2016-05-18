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
var stringUtils = require('utils/string_utils');
var numberUtils = require('utils/number_utils');
require('models/stack_service_component');

describe('App.AssignMasterOnStep7Controller', function () {
  var view;

  beforeEach(function () {
    view = App.AssignMasterOnStep7Controller.create();
  });

  describe("#content", function () {

    it("content is correct", function () {
      view.set('configWidgetContext.controller', Em.Object.create({
        content: {'name': 'name'}
      }));
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.eql({'name': 'name'});
    });

    it("content is null", function () {
      view.set('configWidgetContext.controller', Em.Object.create({
        content: null
      }));
      view.propertyDidChange('content');
      expect(view.get('content')).to.be.empty;
    });
  });

  describe("#execute()", function () {
    var context = Em.Object.create({
      controller: {
        content: Em.Object.create({
          controllerName: ""
        })
      }
    });

    beforeEach(function() {
      this.mock = sinon.stub(view, 'getAllMissingDependentServices');
      sinon.stub(view, 'showInstallServicesPopup');
      sinon.stub(view, 'showAssignComponentPopup');
      sinon.stub(view, 'removeMasterComponent');
      view.reopen({
        content: Em.Object.create()
      });
    });

    afterEach(function() {
      this.mock.restore();
      view.showInstallServicesPopup.restore();
      view.showAssignComponentPopup.restore();
      view.removeMasterComponent.restore();
    });

    it("ADD action, controllerName is empty", function() {
      this.mock.returns([{}]);
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.showInstallServicesPopup.calledOnce).to.be.true;
    });

    it("ADD action, controllerName is set", function() {
      context = Em.Object.create({
        controller: {
          content: Em.Object.create({
            controllerName: "ctrl1"
          })
        }
      });
      this.mock.returns([{}]);
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.showAssignComponentPopup.calledOnce).to.be.true;
    });

    it("ADD action, no dependent services", function() {
      this.mock.returns([]);
      view.execute(context, 'ADD', {componentName: 'C1'});
      expect(view.showAssignComponentPopup.calledOnce).to.be.true;
    });

    it("DELETE action", function() {
      this.mock.returns([{}]);
      view.execute(context, 'DELETE', {componentName: 'C1'});
      expect(view.removeMasterComponent.calledOnce).to.be.true;
    });
  });

  describe("#showAssignComponentPopup()", function () {

    beforeEach(function() {
      sinon.stub(view, 'loadMasterComponentHosts');
      sinon.stub(App.ModalPopup, 'show');
    });

    afterEach(function() {
      view.loadMasterComponentHosts.restore();
      App.ModalPopup.show.restore();
    });

    it("loadMasterComponentHosts should be called", function() {
      view.reopen({
        content: {
          controllerName: null
        }
      });
      view.showAssignComponentPopup();
      expect(view.loadMasterComponentHosts.calledOnce).to.be.true;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it("loadMasterComponentHosts should not be called", function() {
      view.reopen({
        content: {
          controllerName: 'ctrl1'
        }
      });
      view.showAssignComponentPopup();
      expect(view.loadMasterComponentHosts.called).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });
  });

  describe("#showInstallServicesPopup()", function () {
    var mock = Em.Object.create({
      config: Em.Object.create({
        initialValue: 'init',
        value: '',
        displayName: 'c1'
      }),
      setValue: Em.K,
      toggleProperty: Em.K
    });

    beforeEach(function() {
      sinon.stub(stringUtils, 'getFormattedStringFromArray');
      sinon.stub(mock, 'setValue');
      sinon.stub(mock, 'toggleProperty');
      sinon.spy(App.ModalPopup, 'show');
    });

    afterEach(function() {
      stringUtils.getFormattedStringFromArray.restore();
      mock.setValue.restore();
      mock.toggleProperty.restore();
      App.ModalPopup.show.restore();
    });

    it("test", function() {
      view.set('configWidgetContext', mock);
      var popup = view.showInstallServicesPopup();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(mock.get('config.value')).to.be.equal('init');
      expect(mock.setValue.calledWith('init')).to.be.true;
    });
  });

  describe("#removeMasterComponent()", function () {
    var mock = {
      setDBProperty: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(mock, 'setDBProperty');
    });

    afterEach(function() {
      App.router.get.restore();
      mock.setDBProperty.restore();
    });

    it("should set masterComponentHosts", function() {
      view.reopen({
        content: Em.Object.create({
          controllerName: 'ctrl1',
          masterComponentHosts: [
            {component: 'C1'},
            {component: 'C2'}
          ],
          recommendationsHostGroups: {
            blueprint: {host_groups: [{name: 'host-group-1', components: [{name: 'C1'}, {name: 'C2'}]}]},
            blueprint_cluster_binding: {host_groups: [{name: 'host-group-1', hosts: [{fqdn: 'localhost'}]}]}
          }
        }),
        configWidgetContext: {
          config: Em.Object.create()
        }
      });
      view.set('mastersToCreate', ['C2']);
      view.removeMasterComponent();
      expect(view.get('content.masterComponentHosts')).to.be.eql([{component: 'C1'}]);
      expect(view.get('content.recommendationsHostGroups').blueprint).to.be.eql({host_groups: [{name: 'host-group-1', components: [{name: 'C1'}]}]});
      expect(mock.setDBProperty.calledWith('masterComponentHosts', [{component: 'C1'}])).to.be.true;
    });
  });

  describe("#renderHostInfo()", function () {

    beforeEach(function() {
      sinon.stub(App.Host, 'find').returns([
        Em.Object.create({
          hostName: 'host1',
          cpu: 1,
          memory: 1,
          diskInfo: {}
        })
      ]);
      sinon.stub(view, 'sortHosts');
      sinon.stub(numberUtils, 'bytesToSize').returns(1);
    });

    afterEach(function() {
      App.Host.find.restore();
      view.sortHosts.restore();
      numberUtils.bytesToSize.restore();
    });

    it("should set hosts", function() {
      view.reopen({
        content: Em.Object.create({
          controllerName: null
        })
      });
      view.renderHostInfo();
      expect(view.get('hosts')).to.be.eql([Em.Object.create({
        host_name: 'host1',
        cpu: 1,
        memory: 1,
        disk_info: {},
        host_info: Em.I18n.t('installer.step5.hostInfo').fmt('host1', 1, 1)
      })]);
      expect(view.sortHosts.calledWith([Em.Object.create({
        host_name: 'host1',
        cpu: 1,
        memory: 1,
        disk_info: {},
        host_info: Em.I18n.t('installer.step5.hostInfo').fmt('host1', 1, 1)
      })])).to.be.true;
    });
  });

  describe("#loadMasterComponentHosts()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'C1'
        }),
        Em.Object.create({
          componentName: 'C2'
        })
      ]);
      sinon.stub(App, 'get').returns(['C2']);
    });

    afterEach(function() {
      App.get.restore();
      App.HostComponent.find.restore();
    });

    it("should set master components", function() {
      view.loadMasterComponentHosts();
      expect(view.get('masterComponentHosts').mapProperty('component')).to.be.eql(['C2']);
    });
  });

  describe("#getAllMissingDependentServices()", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        stackService: Em.Object.create({
          requiredServices: ['S1', 'S2']
        })
      }));
      sinon.stub(App.Service, 'find').returns([
        {serviceName: 'S1'}
      ]);
      sinon.stub(App.StackService, 'find', function(input) {
        return Em.Object.create({displayName: input});
      });
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    it("test", function() {
      view.set('configActionComponent', Em.Object.create({
        componentName: 'C1'
      }));
      expect(view.getAllMissingDependentServices()).to.be.eql(['S2']);
    });
  });

  describe("#submit()", function () {
    var popup = {
      hide: Em.K
      },
      mock = {
        saveMasterComponentHosts: Em.K,
        loadMasterComponentHosts: Em.K,
        setDBProperty: Em.K
      },
      config = Em.Object.create({
        filename: 'file1',
        name: 'conf1'
      });

    beforeEach(function() {
      sinon.stub(popup, 'hide');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(mock, 'saveMasterComponentHosts');
      sinon.stub(mock, 'loadMasterComponentHosts');
      sinon.stub(mock, 'setDBProperty');
      view.reopen({
        content: Em.Object.create({
          controllerName: 'ctrl1'
        }),
        selectedServicesMasters: [
          {
            component_name: 'C1',
            selectedHost: 'host1'
          }
        ],
        popup: popup,
        configActionComponent: {
          componentName: 'C1'
        },
        configWidgetContext: Em.Object.create({
          config: Em.Object.create({
            configAction: {
              hostComponentConfig: {
                fileName: 'file1',
                configName: 'conf1'
              }
            },
            serviceName: 'S1',
            toggleProperty: Em.K
          }),
          controller: Em.Object.create({
            stepConfigs: [
              Em.Object.create({
                serviceName: 'S1',
                configs: [
                  config
                ]
              })
            ]
          })
        })
      });
      view.submit();
    });

    afterEach(function() {
      App.router.get.restore();
      popup.hide.restore();
      mock.saveMasterComponentHosts.restore();
      mock.loadMasterComponentHosts.restore();
      mock.setDBProperty.restore();
    });

    it("saveMasterComponentHosts should be called", function() {
      expect(mock.saveMasterComponentHosts.calledOnce).to.be.true;
    });

    it("loadMasterComponentHosts                           should be called", function() {
      expect(mock.loadMasterComponentHosts.calledOnce).to.be.true;
    });

    it("configActionComponent should be set", function() {
      expect(view.get('configWidgetContext.config.configActionComponent')).to.be.eql({
        componentName: 'C1',
        hostName: 'host1'
      });
    });

    it("config should be set", function() {
      expect(config.get('value')).to.be.equal('host1');
      expect(config.get('recommendedValue')).to.be.equal('host1');
    });
  });
});