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
var testHelpers = require('test/helpers');

describe('App.HighAvailabilityRollbackController', function () {
  var controller;
  var masterComponentHosts = [
    {
      component: 'NAMENODE',
      isInstalled: true,
      hostName: 'host1'
    },
    {
      component: 'NAMENODE',
      isInstalled: false,
      hostName: 'host2'
    },
    {
      component: 'SECONDARY_NAMENODE',
      isInstalled: false,
      hostName: 'host2'
    },
    {
      component: 'JORNALNODE',
      isInstalled: false,
      hostName: 'host2'
    }
  ];

  beforeEach(function() {
    controller = App.HighAvailabilityRollbackController.create({
      popup: {
        proceedOnClose: Em.K
      },
      saveLogs: Em.K
    });
  });

  describe('#loadStep()', function() {
    var initData = false;
    var clearStep = false;
    var loadTasks = false;
    var addObserver = false;
    var onTaskStatusChange = false;

    var checker = {
      initData: function () {
        initData = true;
        return $.Deferred().resolve().promise();
      },
      clearStep: function () {
        clearStep = true;
      },
      loadTasks: function () {
        loadTasks = true;
      },
      addObserver: function () {
        addObserver = true;
      },
      onTaskStatusChange: function () {
        onTaskStatusChange = true;
      }
    };

    beforeEach(function () {
      controller.loadStep.call(checker);
    });

    it('data is initialized', function () {
      expect(initData).to.be.true;
    });

    it('step is cleared', function () {
      expect(clearStep).to.be.true;
    });

    it('tasks are loaded', function () {
      expect(loadTasks).to.be.true;
    });

    it('observer is added', function () {
      expect(addObserver).to.be.true;
    });

    it('status is changed', function () {
      expect(onTaskStatusChange).to.be.true;
    });
  });

  describe('#initData()', function() {
    var loadMasterComponentHosts = false;
    var loadFailedTask = false;
    var loadHdfsClientHosts = false;

    var checker = {
      loadMasterComponentHosts: function () {
        loadMasterComponentHosts = true;
        return $.Deferred().resolve().promise();
      },
      loadFailedTask: function () {
        loadFailedTask = true;
      },
      loadHdfsClientHosts: function () {
        loadHdfsClientHosts = true;
      }
    };

    beforeEach(function () {
      controller.initData.call(checker);
    });

    it('master component hosts are loaded', function () {
      expect(loadMasterComponentHosts).to.be.true;
    });

    it('failed tasks are loaded', function () {
      expect(loadFailedTask).to.be.true;
    });

    it('hdfs client hosts are loaded', function () {
      expect(loadHdfsClientHosts).to.be.true;
    });
  });

  describe('#skipTask()', function() {

    it('should skip task', function () {
      controller.set('tasks', [
        Em.Object.create({
          id: 'task1',
          status: 'FAILED',
          showRetry: true,
          showSkip: true
        })
      ]);
      controller.skipTask();
      expect(controller.get('tasks').findProperty('id', 'task1')).to.eql(Em.Object.create({
        id: 'task1',
        status: 'COMPLETED',
        showRetry: false,
        showSkip: false
      }));
    });
  });

  describe('#retryTask()', function() {

    it('should skip task', function () {
      controller.set('tasks', [
        Em.Object.create({
          id: 'task1',
          status: 'FAILED',
          showRetry: true,
          showSkip: true
        })
      ]);
      controller.retryTask();
      expect(controller.get('tasks').findProperty('id', 'task1')).to.eql(Em.Object.create({
        id: 'task1',
        status: 'PENDING',
        showRetry: false,
        showSkip: false
      }));
    });
  });

  describe('#onTaskCompleted()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'getTaskStatus');
      sinon.stub(controller, 'setTaskStatus');
    });

    afterEach(function () {
      controller.getTaskStatus.restore();
      controller.setTaskStatus.restore();
    });

    it('should set task status', function () {
      controller.set('currentTaskId', 'task1');
      controller.onTaskCompleted();
      expect(controller.getTaskStatus.calledWith('task1')).to.be.true;
      expect(controller.setTaskStatus.calledWith('task1', 'COMPLETED')).to.be.true;
    });
  });

  describe('#getTaskStatus()', function() {

    it('should skip task', function () {
      controller.set('tasks', [
        Em.Object.create({
          id: 'task1',
          status: 'FAILED'
        })
      ]);
      controller.set('currentTaskId', 'task1');
      expect(controller.getTaskStatus('task1')).to.equal('FAILED');
    });
  });

  describe('#loadFailedTask()', function() {

    beforeEach(function () {
      sinon.stub(App.db, 'getHighAvailabilityWizardFailedTask').returns({id: 'task1'});
    });

    afterEach(function () {
      App.db.getHighAvailabilityWizardFailedTask.restore();
    });

    it('should load failed task', function () {
      controller.loadFailedTask();
      expect(controller.get('failedTask')).to.eql({id: 'task1'});
    });
  });

  describe('#loadFailedTask()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'removeObserver');
      sinon.stub(controller.popup, 'proceedOnClose');
    });

    afterEach(function () {
      controller.removeObserver.restore();
      controller.popup.proceedOnClose.restore();
    });

    it('should remove observer and proceed', function () {
      controller.set('isSubmitDisabled', false);
      controller.done();
      expect(controller.removeObserver.calledOnce).to.be.true;
      expect(controller.popup.proceedOnClose.calledOnce).to.be.true;
    });
  });

  describe('#stopAllServices()', function() {

    it('should skip task', function () {
      controller.stopAllServices();
      var args = testHelpers.findAjaxRequest('name', 'common.services.update');
      expect(args).to.exists;
    });
  });

  describe('#restoreHBaseConfigs()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'loadConfigTag');
    });

    afterEach(function () {
      controller.loadConfigTag.restore();
    });

    it('should restore hbase configs', function () {
      controller.set('content.hbaseSiteTag', 'v1');
      controller.restoreHBaseConfigs();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.load_hbase_configs');
      expect(args[0].data.hbaseSiteTag).to.equal('v1');
    });
  });

  describe('#restoreAccumuloConfigs()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'loadConfigTag');
    });

    afterEach(function () {
      controller.loadConfigTag.restore();
    });

    it('should restore accumulo configs', function () {
      controller.set('content.accumuloSiteTag', 'v1');
      controller.restoreAccumuloConfigs();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.load_accumulo_configs');
      expect(args[0].data.accumuloSiteTag).to.equal('v1');
    });
  });

  describe('#restoreHawqConfigs()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'loadConfigTag');
    });

    afterEach(function () {
      controller.loadConfigTag.restore();
    });

    it('should restore hawq configs', function () {
      controller.set('content.hawqSiteTag', 'v1');
      controller.set('content.hdfsClientTag', 'v2');
      controller.restoreHawqConfigs();
      var args = testHelpers.filterAjaxRequests('name', 'admin.high_availability.load_hawq_configs');
      expect(args[0][0].data.tagName).to.equal('v1');
      expect(args[1][0].data.tagName).to.equal('v2');
    });
  });

  describe('#stopFailoverControllers()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('should execute stopFailoverControllers function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.stopFailoverControllers();
      expect(controller.updateComponent.calledOnce).to.be.true;
    });
  });

  describe('#deleteFailoverControllers()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'checkBeforeDelete');
    });

    afterEach(function () {
      controller.checkBeforeDelete.restore();
    });

    it('should execute checkBeforeDelete function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.deleteFailoverControllers();
      expect(controller.checkBeforeDelete.calledOnce).to.be.true;
    });
  });

  describe('#stopStandbyNameNode()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('should execute updateComponent function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.stopStandbyNameNode();
      expect(controller.updateComponent.calledOnce).to.be.true;
    });
  });

  describe('#stopNameNode()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('should execute updateComponent function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.stopNameNode();
      expect(controller.updateComponent.calledOnce).to.be.true;
    });
  });

  describe('#restoreHDFSConfigs()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'unInstallHDFSClients');
    });

    afterEach(function () {
      controller.unInstallHDFSClients.restore();
    });

    it('should execute restoreHDFSConfigs function', function () {
      controller.restoreHDFSConfigs();
      expect(controller.unInstallHDFSClients.calledOnce).to.be.true;
    });
  });

  describe('#enableSecondaryNameNode()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('should execute updateComponent function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.enableSecondaryNameNode();
      expect(controller.updateComponent.calledOnce).to.be.true;
    });
  });

  describe('#stopJournalNodes()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function () {
      controller.updateComponent.restore();
    });

    it('should execute updateComponent function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.stopJournalNodes();
      expect(controller.updateComponent.calledOnce).to.be.true;
    });
  });

  describe('#deleteJournalNodes()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'unInstallComponent');
    });

    afterEach(function () {
      controller.unInstallComponent.restore();
    });

    it('should execute unInstallComponent function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.deleteJournalNodes();
      expect(controller.unInstallComponent.calledOnce).to.be.true;
    });
  });

  describe.skip('#deleteAdditionalNameNode()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'unInstallComponent');
    });

    afterEach(function () {
      controller.unInstallComponent.restore();
    });

    it('should execute unInstallComponent function', function () {
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.deleteAdditionalNameNode();
      expect(controller.unInstallComponent.calledOnce).to.be.true;
    });
  });

  describe('#startAllServices()', function() {

    it('should start all services', function () {
      controller.startAllServices();
      var args = testHelpers.findAjaxRequest('name', 'common.services.update');
      expect(args).to.exists;
    });
  });

  describe('#onLoadHbaseConfigs()', function() {

    it('should make ajax send request', function () {
      var data = {
        items: [
          {
            type: 'hbase-site',
            properties: {}
          }
        ]
      };
      controller.onLoadHbaseConfigs(data);
      var args = testHelpers.findAjaxRequest('name', 'admin.save_configs');
      expect(args).to.exists;
    });
  });

  describe('#onLoadAccumuloConfigs()', function() {


    it('should make ajax send request', function () {
      var data = {items: [
          {
            type: 'accumulo-site',
            properties: {}
          }
        ]};
      controller.onLoadAccumuloConfigs(data);
      var args = testHelpers.findAjaxRequest('name', 'admin.save_configs');
      expect(args).to.exists;
    });
  });

  describe('#onLoadHawqConfigs()', function() {

    it('should make ajax send request', function () {
      var data = {items: [
          {
            type: 'hawq-site',
            properties: {}
          }
        ]};
      controller.onLoadHawqConfigs(data);
      var args = testHelpers.findAjaxRequest('name', 'admin.save_configs');
      expect(args).to.exists;
    });
  });

  describe('#onDeletedHDFSClient()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'loadConfigTag');
    });

    afterEach(function () {
      controller.loadConfigTag.restore();
    });

    it('should make ajax send request', function () {
      controller.set('deletedHdfsClients', 1);
      controller.set('content.hdfsClientHostNames', ['host1', 'host2']);
      controller.set('content.hdfsSiteTag', 'v1');
      controller.set('content.coreSiteTag', 'v2');
      controller.onDeletedHDFSClient();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.load_configs');
      expect(args[0].data).to.eql({
        hdfsSiteTag: 'v1',
        coreSiteTag: 'v2'
      });
    });

    it('should set deletedHdfsClients property', function () {
      controller.set('deletedHdfsClients', 0);
      controller.set('content.hdfsClientHostNames', ['host1', 'host2']);
      controller.onDeletedHDFSClient();
      expect(controller.get('deletedHdfsClients')).to.equal(1);
    });
  });

  describe('#onLoadConfigs()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'loadConfigTag');
    });

    afterEach(function () {
      controller.loadConfigTag.restore();
    });

    it('should make 2 ajax send requests', function () {
      var data = {
        items: [
          {
            type: 'hdfs-site',
            properties: {}
          },
          {
            type: 'core-site',
            properties: {}
          }
        ]
      };
      controller.onLoadConfigs(data);
      var args = testHelpers.filterAjaxRequests('name', 'admin.save_configs');
      expect(args[0][0].data.siteName).to.equal('hdfs-site');
      expect(args[1][0].data.siteName).to.equal('core-site');
    });
  });

  describe('#onHdfsConfigsSaved()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('should execute onTaskCompleted function', function () {
      controller.set('configsSaved', true);
      controller.onHdfsConfigsSaved();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });

    it('should set configsSaved property to true', function () {
      controller.set('configsSaved', false);
      controller.onHdfsConfigsSaved();
      expect(controller.get('configsSaved')).to.be.true;
    });
  });

  describe('#unInstallHDFSClients()', function() {

    it('should make ajax send requests', function () {
      controller.set('content.hdfsClientHostNames', ['host1', 'host2']);
      controller.unInstallHDFSClients();
      var args = testHelpers.filterAjaxRequests('name', 'common.delete.host_component');
      expect(args.length).to.equal(2);
    });
  });

  describe('#unInstallComponent()', function() {

    it('should make ajax send request', function () {
      controller.unInstallComponent('comp1', 'host1');
      var args = testHelpers.findAjaxRequest('name', 'common.host.host_component.passive');
      expect(args[0].data).to.eql({
        hostName: 'host1',
        componentName: 'comp1',
        passive_state: "ON",
        taskNum: 1,
        callback: 'checkBeforeDelete'
      });
    });

    it('should make ajax send requests', function () {
      controller.unInstallComponent('comp1', ['host1', 'host2']);
      var args = testHelpers.filterAjaxRequests('name', 'common.host.host_component.passive');
      expect(args.length).to.equal(2);
    });
  });

  describe('#checkBeforeDelete()', function() {

    it('should make ajax send request', function () {
      controller.checkBeforeDelete('comp1', 'host1');
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.getHostComponent');
      expect(controller.get('hostsToPerformDel')).to.eql([]);
      expect(args[0].data).to.eql({
        componentName: 'comp1',
        hostName: 'host1',
        taskNum: 1,
        callback: 'deleteComponent'
      });
    });

    it('should make ajax send requests', function () {
      controller.checkBeforeDelete('comp1', ['host1', 'host2']);
      var args = testHelpers.filterAjaxRequests('name', 'admin.high_availability.getHostComponent');
      expect(controller.get('hostsToPerformDel')).to.eql([]);
      expect(args.length).to.equal(2);
    });
  });

  describe('#checkResult()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'deleteComponent');
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function () {
      controller.deleteComponent.restore();
      controller.onTaskCompleted.restore();
    });

    it('should execute deleteComponent function', function () {
      controller.set('hostsToPerformDel', []);
      controller.checkResult({}, 'success', {
        componentName: 'comp1',
        hostName: 'host1',
        taskNum: 1,
        callback: 'deleteComponent'
      });
      expect(controller.deleteComponent.calledWith('comp1', ['host1'])).to.be.true;
    });

    it('should execute onTaskCompleted function', function () {
      controller.set('hostsToPerformDel', []);
      controller.checkResult({}, 'error', {
        componentName: 'comp1',
        hostName: 'host1',
        taskNum: 1,
        callback: 'deleteComponent'
      });
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });
  });

  describe('#deleteComponent()', function() {

    it('should make ajax send request', function () {
      controller.deleteComponent('comp1', 'host1');
      var args = testHelpers.findAjaxRequest('name', 'common.delete.host_component');
      expect(controller.get('numOfDelOperations')).to.equal(1);
      expect(args[0].data).to.eql({
        componentName: 'comp1',
        hostName: 'host1'
      });
    });

    it('should make ajax send requests', function () {
      controller.deleteComponent('comp1', ['host1', 'host2']);
      var args = testHelpers.filterAjaxRequests('name', 'common.delete.host_component');
      expect(controller.get('numOfDelOperations')).to.equal(2);
      expect(args.length).to.equal(2);
    });
  });

  describe('#onDeleteComplete()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'onTaskCompleted');
    });

    afterEach(function () {
      controller.onTaskCompleted.restore();
    });

    it('should execute onTaskCompleted function', function () {
      controller.set('numOfDelOperations', 1);
      controller.onDeleteComplete();
      expect(controller.onTaskCompleted.calledOnce).to.be.true;
    });

    it('should set numOfDelOperations property', function () {
      controller.set('numOfDelOperations', 2);
      controller.onDeleteComplete();
      expect(controller.get('numOfDelOperations')).to.equal(1);
    });
  });

  describe.skip('#deletePXF()', function() {

    beforeEach(function () {
      this.mock = sinon.stub(controller, 'getSlaveComponentHosts');
      sinon.stub(controller, 'updateComponent');
      sinon.stub(controller, 'checkBeforeDelete');
    });

    afterEach(function () {
      this.mock.restore();
      controller.updateComponent.restore();
      controller.checkBeforeDelete.restore();
    });

    it('should remove PXF', function () {
      this.mock.returns([
        {
          componentName: 'PXF',
          hosts: [{hostName: 'host2'}]
        },
        {
          componentName: 'DATANODE',
          hosts: [{hostName: 'host1'}]
        }
      ]);
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.deletePXF();
      expect(controller.updateComponent.calledOnce).to.be.true;
      expect(controller.checkBeforeDelete.calledOnce).to.be.true;
    });

    it('should not remove PXF', function () {
      this.mock.returns([
        {
          componentName: 'PXF',
          hosts: [{hostName: 'host1'}]
        },
        {
          componentName: 'DATANODE',
          hosts: [{hostName: 'host2'}]
        }
      ]);
      controller.set('content.masterComponentHosts', masterComponentHosts);
      controller.deletePXF();
      expect(controller.updateComponent.calledOnce).to.be.false;
      expect(controller.checkBeforeDelete.calledOnce).to.be.false;
    });
  });

  describe('#setCommandsAndTasks()', function() {

    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns([]);
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    it('should set tasks property', function () {
      var tmpTasks = [
        Em.Object.create({command: 'deleteSNameNode'}),
        Em.Object.create({command: 'startAllServices'}),
        Em.Object.create({command: 'reconfigureHBase'}),
        Em.Object.create({command: 'reconfigureAMS'}),
        Em.Object.create({command: 'reconfigureAccumulo'}),
        Em.Object.create({command: 'reconfigureHawq'}),
        Em.Object.create({command: 'installPXF'}),
        Em.Object.create({command: 'startZKFC'}),
        Em.Object.create({command: 'installZKFC'}),
        Em.Object.create({command: 'startSecondNameNode'}),
        Em.Object.create({command: 'startNameNode'}),
        Em.Object.create({command: 'startZooKeeperServers'}),
        Em.Object.create({command: 'reconfigureHDFS'}),
        Em.Object.create({command: 'disableSNameNode'}),
        Em.Object.create({command: 'startJournalNodes'}),
        Em.Object.create({command: 'installJournalNodes'}),
        Em.Object.create({command: 'installNameNode'}),
        Em.Object.create({command: 'stopAllServices'}),
        Em.Object.create({command: 'restoreHawqConfigs'}),
        Em.Object.create({command: 'restoreAccumuloConfigs'}),
        Em.Object.create({command: 'restoreHBaseConfigs'}),
        Em.Object.create({command: 'deletePXF'})
      ];
      controller.set('failedTask', {command: 'reconfigureHDFS'});
      controller.set('commands', []);
      controller.setCommandsAndTasks(tmpTasks);
      expect(controller.get('tasks').mapProperty('command').join(', ')).to.equal('startZooKeeperServers, reconfigureHDFS, disableSNameNode, startJournalNodes, installJournalNodes, installNameNode, stopAllServices');
    });
  });

  describe('#clearStep()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'setCommandsAndTasks').returns([]);
    });

    afterEach(function () {
      controller.setCommandsAndTasks.restore();
    });

    it('should clear step', function () {
      controller.set('commands', ['deletePXF']);
      controller.clearStep();
      expect(controller.get('isSubmitDisabled')).to.be.true;
      expect(controller.get('tasks')).to.eql([]);
      expect(controller.get('logs')).to.eql([]);
      expect(controller.get('currentRequestIds')).to.eql([]);
      expect(controller.setCommandsAndTasks.calledOnce).to.be.true;
    });
  });

  describe('#onTaskStatusChange()', function() {

    beforeEach(function () {
      sinon.stub(controller, 'setTaskStatus');
      sinon.stub(controller, 'runTask');
      sinon.stub(controller, 'saveTasksStatuses');
      sinon.stub(controller, 'saveRequestIds');
      sinon.stub(controller, 'saveLogs');
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function () {
      controller.setTaskStatus.restore();
      controller.runTask.restore();
      controller.saveTasksStatuses.restore();
      controller.saveRequestIds.restore();
      controller.saveLogs.restore();
      App.clusterStatus.setClusterStatus.restore();
    });

    it('should set cluster status{1}', function () {
      controller.set('tasks', [
        Em.Object.create({status: 'FAILED'})
      ]);
      controller.onTaskStatusChange();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it('should set cluster status{2}', function () {
      controller.onTaskStatusChange();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });

    it('should set cluster status{3}', function () {
      controller.set('tasks', [
        Em.Object.create({status: 'PENDING'})
      ]);
      controller.onTaskStatusChange();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

});
