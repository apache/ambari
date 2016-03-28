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

require('controllers/main/service/reassign/step6_controller');

describe('App.ReassignMasterWizardStep6Controller', function () {

  var controller = App.ReassignMasterWizardStep6Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create(),
      reassignHosts: Em.Object.create()
    })
  });

  beforeEach(function () {
    sinon.stub(App.ajax, 'send', Em.K);
  });
  afterEach(function () {
    App.ajax.send.restore();
  });


  describe('#initializeTasks()', function () {
    it('No commands', function () {
      controller.set('commands', []);
      controller.set('hostComponents', []);
      controller.initializeTasks();

      expect(controller.get('tasks')).to.be.empty;
    });
    it('One command', function () {
      controller.set('commands', ['COMMAND1']);
      controller.initializeTasks();

      expect(controller.get('tasks')[0].get('id')).to.equal(0);
      expect(controller.get('tasks')[0].get('command')).to.equal('COMMAND1');
    });
  });

  describe('#hideRollbackButton()', function () {

    it('No showRollback command', function () {
      controller.set('tasks', [Em.Object.create({
        showRollback: false
      })]);
      controller.hideRollbackButton();
      expect(controller.get('tasks')[0].get('showRollback')).to.be.false;
    });
    it('showRollback command is present', function () {
      controller.set('tasks', [Em.Object.create({
        showRollback: true
      })]);
      controller.hideRollbackButton();
      expect(controller.get('tasks')[0].get('showRollback')).to.be.false;
    });
  });

  describe('#onComponentsTasksSuccess()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted', Em.K);
    });
    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('One host-component', function () {
      controller.set('multiTaskCounter', 1);
      controller.set('hostComponents', [
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(0);
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
    it('two host-components', function () {
      controller.set('multiTaskCounter', 2);
      controller.set('hostComponents', [
        {},
        {}
      ]);
      controller.onComponentsTasksSuccess();
      expect(controller.get('multiTaskCounter')).to.equal(1);
      expect(controller.onTaskCompleted.called).to.be.false;
    });
  });


  describe('#loadStep()', function () {
    var isHaEnabled = true;

    beforeEach(function () {
      controller.set('content.reassign.service_id', 'service1');
      sinon.stub(controller, 'onTaskStatusChange', Em.K);
      sinon.stub(controller, 'initializeTasks', Em.K);
      sinon.stub(App, 'get', function () {
        return isHaEnabled;
      });
    });
    afterEach(function () {
      controller.onTaskStatusChange.restore();
      controller.initializeTasks.restore();
      App.get.restore();
    });

    it('reassign component is NameNode and HA enabled', function () {
      isHaEnabled = true;
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE', 'ZKFC']);
    });
    it('reassign component is NameNode and HA disabled', function () {
      isHaEnabled = false;
      controller.set('content.reassign.component_name', 'NAMENODE');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['NAMENODE']);
    });
    it('reassign component is RESOURCEMANAGER', function () {
      controller.set('content.reassign.component_name', 'RESOURCEMANAGER');

      controller.loadStep();
      expect(controller.get('hostComponents')).to.eql(['RESOURCEMANAGER']);
    });
  });

  describe('#startServices()', function () {
    before(function () {
      sinon.stub(App.router, 'get').returns({"skip.service.checks": "false"});
    });
    after(function () {
      App.router.get.restore();
    });
    it('', function () {
      controller.startServices();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#deleteHostComponents()', function () {

    it('No host components', function () {
      controller.set('hostComponents', []);
      controller.set('content.reassignHosts.source', 'host1');
      controller.deleteHostComponents();
      expect(App.ajax.send.called).to.be.false;
    });
    it('delete two components', function () {
      controller.set('hostComponents', [1, 2]);
      controller.set('content.reassignHosts.source', 'host1');
      controller.deleteHostComponents();
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({
        "hostName": "host1",
        "componentName": 1
      });
      expect(App.ajax.send.getCall(1).args[0].data).to.eql({
        "hostName": "host1",
        "componentName": 2
      });
    });
  });

  describe('#onDeleteHostComponentsError()', function () {
    beforeEach(function () {
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      sinon.stub(controller, 'onTaskError', Em.K);
    });
    afterEach(function () {
      controller.onComponentsTasksSuccess.restore();
      controller.onTaskError.restore();
    });

    it('task success', function () {
      var error = {
        responseText: 'org.apache.ambari.server.controller.spi.NoSuchResourceException'
      }
      controller.onDeleteHostComponentsError(error);
      expect(controller.onComponentsTasksSuccess.calledOnce).to.be.true;
    });
    it('unknown error', function () {
      var error = {
        responseText: ''
      }
      controller.onDeleteHostComponentsError(error);
      expect(controller.onTaskError.calledOnce).to.be.true;
    });
  });

  describe('#stopMysqlService()', function () {
    it('stopMysqlService', function () {
      controller.stopMysqlService();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#putHostComponentsInMaintenanceMode()', function () {
    beforeEach(function(){
      sinon.stub(controller, 'onComponentsTasksSuccess', Em.K);
      controller.set('content.reassignHosts.source', 'source');
    });
    afterEach(function(){
      controller.onComponentsTasksSuccess.restore();
    });
    it('No host-components', function () {
      controller.set('hostComponents', []);
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.called).to.be.false;
      expect(controller.get('multiTaskCounter')).to.equal(0);
    });
    it('One host-components', function () {
      controller.set('hostComponents', [{}]);
      controller.putHostComponentsInMaintenanceMode();
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(controller.get('multiTaskCounter')).to.equal(1);
    });
  });
});