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

function getController() {
  return App.NameNodeFederationWizardStep4Controller.create({});
}

describe('App.NameNodeFederationWizardStep4Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  describe("#initializeTasks()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'removeUnneededTasks');
    });

    afterEach(function() {
      controller.removeUnneededTasks.restore();
    });

    it("should execute removeUnneededTasks function", function() {
      controller.initializeTasks();
      expect(controller.removeUnneededTasks.calledOnce).to.be.true;
    });
  });

  describe("#removeUnneededTasks()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'removeTasks');
      this.mock = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      controller.removeTasks.restore();
      this.mock.restore();
    });

    it("Remove tasks if installed services contains RANGER", function() {
      this.mock.returns([
        {serviceName: 'YARN'},
        {serviceName: 'RANGER'}
      ]);
      controller.removeUnneededTasks();
      expect(controller.removeTasks.calledOnce).to.be.true;
    });

    it("Remove tasks if installed services contains RANGER", function() {
      this.mock.returns([
        {serviceName: 'YARN'},
        {serviceName: 'AMBARI_INFRA_SOLR'}
      ]);
      controller.removeUnneededTasks();
      expect(controller.removeTasks.calledOnce).to.be.true;
    });

    it("should not remove any tasks", function() {
      this.mock.returns([
        {serviceName: 'YARN'},
        {serviceName: 'HIVE'}
      ]);
      controller.removeUnneededTasks();
      expect(controller.removeTasks.calledOnce).to.be.false;
    });
  });

  describe("#newNameNodeHosts()", function () {

    it("should not remove any tasks", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.propertyDidChange('newNameNodeHosts');
      expect(controller.get('newNameNodeHosts')).to.eql(['host1']);
    });
  });

  describe("#stopRequiredServices()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'stopServices');
    });

    afterEach(function() {
      controller.stopServices.restore();
    });

    it("should stopServices function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.stopRequiredServices();
      expect(controller.stopServices.calledOnce).to.be.true;
    });
  });

  describe("#reconfigureServices()", function () {

    beforeEach(function() {
      this.services = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      this.services.restore();
    });

    it("should return ajax send request without RANGER and ACCUMULO services configs", function() {
      this.services.returns([]);
      controller.set('content.serviceConfigProperties',
        {
          items: [
            {
              type: 'hdfs-site',
              properties: {
                'dfs.journalnode.edits.dir': 'dir1'
              }
            }
          ]
        }
      );
      controller.reconfigureServices();
      var args = testHelpers.findAjaxRequest('name', 'common.service.multiConfigurations')
      expect(args[0].data.configs.map(function(config){ return config.Clusters.desired_config[0].type})).to.eql(['hdfs-site']);
    });

    it("should return ajax send request with RANGER and ACCUMULO services configs", function() {
      this.services.returns([
        {serviceName: 'RANGER'},
        {serviceName: 'ACCUMULO'}
      ]);
      controller.set('content.serviceConfigProperties',
        {
          items: [
            {
              type: 'hdfs-site',
              properties: {
                'dfs.journalnode.edits.dir': 'dir'
              }
            },
            {
              type: 'ranger-tagsync-site',
              properties: {
                'dfs.journalnode.edits.dir': 'dir2'
              }
            },
            {
              type: 'accumulo-site',
              properties: {
                'dfs.journalnode.edits.dir': 'dir3'
              }
            }
          ]
        }
      );
      controller.reconfigureServices();
      var args = testHelpers.findAjaxRequest('name', 'common.service.multiConfigurations')
      expect(args[0].data.configs.map(function(config){ return config.Clusters.desired_config[0].type})).to.eql(['hdfs-site', 'ranger-tagsync-site', 'accumulo-site']);
    });
  });

  describe("#installHDFSClients()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        {
          componentName: 'JOURNALNODE',
          hostName: 'host2'
        }
      ]);
      sinon.spy(controller, 'createInstallComponentTask');
    });

    afterEach(function() {
      App.HostComponent.find.restore();
      controller.createInstallComponentTask.restore();
    });

    it("should execute createInstallComponentTask function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.installHDFSClients();
      expect(controller.createInstallComponentTask.calledWith('HDFS_CLIENT', ['host1', 'host2'], 'HDFS')).to.be.true;
    });
  });

  describe("#installNameNode()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'createInstallComponentTask');
    });

    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it("should execute createInstallComponentTask function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.installNameNode();
      expect(controller.createInstallComponentTask.calledWith('NAMENODE', ['host1'], 'HDFS')).to.be.true;
    });
  });

  describe("#installZKFC()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'createInstallComponentTask');
    });

    afterEach(function() {
      controller.createInstallComponentTask.restore();
    });

    it("should execute createInstallComponentTask function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.installZKFC();
      expect(controller.createInstallComponentTask.calledWith('ZKFC', ['host1'], 'HDFS')).to.be.true;
    });
  });

  describe("#startJournalNodes()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        {
          componentName: 'JOURNALNODE',
          hostName: 'host2'
        }
      ]);
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
      App.HostComponent.find.restore();
    });

    it("should execute updateComponent function", function() {
      controller.startJournalNodes();
      expect(controller.updateComponent.calledWith('JOURNALNODE', ['host2'], 'HDFS', 'Start')).to.be.true;
    });
  });

  describe("#startNameNodes()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it("should execute updateComponent function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: true,
          hostName: 'host1'
        }
      ]);
      controller.startNameNodes();
      expect(controller.updateComponent.calledWith('NAMENODE', ['host1'], 'HDFS', 'Start')).to.be.true;
    });
  });

  describe("#startZKFCs()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it("should execute updateComponent function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: true,
          hostName: 'host1'
        }
      ]);
      controller.startZKFCs();
      expect(controller.updateComponent.calledWith('ZKFC', ['host1'], 'HDFS', 'Start')).to.be.true;
    });
  });

  describe("#startInfraSolr()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'startServices')
    });

    afterEach(function() {
      controller.startServices.restore();
    });

    it("should execute startServices function", function() {
      controller.startInfraSolr();
      expect(controller.startServices.calledWith(false, ['AMBARI_INFRA_SOLR'], true)).to.be.true;
    });
  });

  describe("#startRangerAdmin()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        {
          componentName: 'RANGER_ADMIN',
          hostName: 'host3'
        }
      ]);
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
      App.HostComponent.find.restore();
    });

    it("should execute updateComponent function", function() {
      controller.startRangerAdmin();
      expect(controller.updateComponent.calledWith('RANGER_ADMIN', ['host3'], 'RANGER', 'Start')).to.be.true;
    });
  });

  describe("#startRangerUsersync()", function () {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        {
          componentName: 'RANGER_USERSYNC',
          hostName: 'host3'
        }
      ]);
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
      App.HostComponent.find.restore();
    });

    it("should execute updateComponent function", function() {
      controller.startRangerUsersync();
      expect(controller.updateComponent.calledWith('RANGER_USERSYNC', ['host3'], 'RANGER', 'Start')).to.be.true;
    });
  });

  describe("#startZKFC()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it("should execute updateComponent function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.startZKFC();
      expect(controller.updateComponent.calledWith('ZKFC', 'host1', 'HDFS', 'Start')).to.be.true;
    });
  });

  describe("#startZKFC2()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it("should execute updateComponent function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        },
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host2'
        }
      ]);
      controller.startZKFC2();
      expect(controller.updateComponent.calledWith('ZKFC', 'host2', 'HDFS', 'Start')).to.be.true;
    });
  });

  describe("#startNameNode()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it("should execute updateComponent function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.startNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', 'host1', 'HDFS', 'Start')).to.be.true;
    });
  });


  describe("#startNameNode2()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'updateComponent')
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it("should execute updateComponent function", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        },
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host2'
        }
      ]);
      controller.startNameNode2();
      expect(controller.updateComponent.calledWith('NAMENODE', 'host2', 'HDFS', 'Start')).to.be.true;
    });
  });

  describe("#formatNameNode()", function () {

    it("should return ajax send request", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.formatNameNode();
      var args = testHelpers.findAjaxRequest('name', 'nameNode.federation.formatNameNode');
      expect(args[0].data.host).to.equal('host1');
    });
  });

  describe("#formatZKFC()", function () {

    it("should return ajax send request", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        }
      ]);
      controller.formatZKFC();
      var args = testHelpers.findAjaxRequest('name', 'nameNode.federation.formatZKFC');
      expect(args[0].data.host).to.equal('host1');
    });
  });

  describe("#bootstrapNameNode()", function () {

    it("should return ajax send request", function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host1'
        },
        {
          component: 'NAMENODE',
          isInstalled: false,
          hostName: 'host2'
        }
      ]);
      controller.bootstrapNameNode();
      var args = testHelpers.findAjaxRequest('name', 'nameNode.federation.bootstrapNameNode');
      expect(args[0].data.host).to.equal('host2');
    });
  });

  describe("#restartAllServices()", function () {

    it("should return ajax send request", function() {
      App.set('clusterName', 'c1');
      controller.restartAllServices();
      var args = testHelpers.findAjaxRequest('name', 'restart.custom.filter');
      expect(args[0].data).to.eql(
        {
          filter: "HostRoles/component_name!=NAMENODE&HostRoles/component_name!=JOURNALNODE&HostRoles/component_name!=ZKFC&HostRoles/component_name!=RANGER_ADMIN&HostRoles/component_name!=RANGER_USERSYNC&HostRoles/cluster_name=c1",
          context: "Restart Required Services"
        }
      );
    });
  });

});
