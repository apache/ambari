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

App = require('app');

require('controllers/main/service/reassign/step7_controller');
var controller;

describe('App.ReassignMasterWizardStep7Controller', function () {

  beforeEach(function () {
    sinon.stub(App.ajax, 'send', Em.K);
    controller = App.ReassignMasterWizardStep7Controller.create({
      content: Em.Object.create({
        reassign: Em.Object.create(),
        reassignHosts: Em.Object.create()
      })
    });
  });
  afterEach(function () {
    App.ajax.send.restore();
  });

  describe('#initializeTasks()', function () {
    it('should set isLoaded to true', function () {
      controller.set('isLoaded', false);

      controller.initializeTasks();
      expect(controller.get('isLoaded')).to.be.true;
    });
  });

  describe("#putHostComponentsInMaintenanceMode()", function() {
    it("no host-components", function() {
      controller.set('hostComponents', []);
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.called).to.be.false;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("one host-component", function() {
      controller.set('hostComponents', ['C1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.calledWith({
        name: 'common.host.host_component.passive',
        sender: controller,
        data: {
          hostName: 'host1',
          passive_state: "ON",
          componentName: 'C1'
        },
        success: 'onComponentsTasksSuccess',
        error: 'onTaskError'
      })).to.be.true;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("two host-components", function() {
      controller.set('hostComponents', ['C1', 'C2']);
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.calledTwice).to.be.true;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
  });

  describe("#deleteHostComponents()", function() {
    it("no host-components", function() {
      controller.set('hostComponents', []);
      controller.deleteHostComponents();
      expect(App.ajax.send.called).to.be.false;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("one host-component", function() {
      controller.set('hostComponents', ['C1']);
      controller.set('content.reassignHosts.target', 'host1');
      controller.deleteHostComponents();
      expect(App.ajax.send.calledWith({
        name: 'common.delete.host_component',
        sender: controller,
        data: {
          hostName: 'host1',
          componentName: 'C1'
        },
        success: 'onComponentsTasksSuccess',
        error: 'onDeleteHostComponentsError'
      })).to.be.true;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it("two host-components", function() {
      controller.set('hostComponents', ['C1', 'C2']);
      controller.deleteHostComponents();
      expect(App.ajax.send.calledTwice).to.be.true;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
  });
});
