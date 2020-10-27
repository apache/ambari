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
require('models/cluster');
require('controllers/wizard');
require('controllers/main/service/reassign_controller');

describe('App.ReassignMasterController', function () {

  var reassignMasterController;

  beforeEach(function () {
    reassignMasterController = App.ReassignMasterController.create({});
  });

  describe('#totalSteps', function () {

    var cases = [
      {
        componentName: 'ZOOKEEPER_SERVER',
        result: 4
      },
      {
        componentName: 'RESOURCE_MANAGER',
        result: 4
      },
      {
        componentName: 'OOZIE_SERVER',
        result: 6
      },
      {
        componentName: 'APP_TIMELINE_SERVER',
        result: 6
      },
      {
        componentName: 'NAMENODE',
        result: 6
      }
    ];

    cases.forEach(function (c) {
      it('check ' + c.componentName, function () {
        reassignMasterController.set('content.reassign', {'component_name': c.componentName});
        expect(reassignMasterController.get('totalSteps')).to.equal(c.result);
        reassignMasterController.set('content.reassign', {service_id:null});
      });
    });
  });

  describe('#saveMasterComponentHosts', function () {

    var stepController = Em.Object.create({
        selectedServicesMasters: [
          Em.Object.create({
            display_name: 'd0',
            component_name: 'c0',
            selectedHost: 'h0',
            serviceId: 's0'
          }),
          Em.Object.create({
            display_name: 'd1',
            component_name: 'c1',
            selectedHost: 'h1',
            serviceId: 's1'
          })
        ]
      }),
      masterComponentHosts = [
        {
          display_name: 'd0',
          component: 'c0',
          hostName: 'h0',
          serviceId: 's0',
          isInstalled: true
        },
        {
          display_name: 'd1',
          component: 'c1',
          hostName: 'h1',
          serviceId: 's1',
          isInstalled: true
        }
      ];

    beforeEach(function () {
      sinon.stub(App.db, 'setMasterComponentHosts', Em.K);
      sinon.stub(reassignMasterController, 'setDBProperty', Em.K);
      reassignMasterController.saveMasterComponentHosts(stepController);
    });

    afterEach(function () {
      App.db.setMasterComponentHosts.restore();
      reassignMasterController.setDBProperty.restore();
    });

    it('setMasterComponentHosts is called once', function () {
      expect(App.db.setMasterComponentHosts.calledOnce).to.be.true;
    });

    it('setDBProperty is called once', function () {
      expect(reassignMasterController.setDBProperty.calledOnce).to.be.true;
    });

    it('setMasterComponentHosts is called with valid arguments', function () {
      expect(App.db.setMasterComponentHosts.calledWith(masterComponentHosts)).to.be.true;
    });

    it('setDBProperty is called with valid arguments', function () {
      expect(reassignMasterController.setDBProperty.calledWith('masterComponentHosts', masterComponentHosts)).to.be.true;
    });

    it('masterComponentHosts are equal to ' + JSON.stringify(masterComponentHosts), function () {
      expect(reassignMasterController.get('content.masterComponentHosts')).to.eql(masterComponentHosts);
    });

  });

  describe('#loadMap', function() {

    describe('should load service', function() {
      var loadComponentToReassign = false;
      var loadDatabaseType = false;
      var loadServiceProperties = false;
      var load = false;

      var checker = {
        loadComponentToReassign: function() {
          loadComponentToReassign = true;
        },
        loadDatabaseType: function() {
          loadDatabaseType = true;
        },
        loadServiceProperties: function() {
          loadServiceProperties = true;
        },
        load: function() {
          load = true;
        }
      };

      beforeEach(function () {
        reassignMasterController.loadMap['1'][0].callback.call(checker);
      });

      it('component to reassign is loaded', function () {
        expect(loadComponentToReassign).to.be.true;
      });

      it('database type is loaded', function () {
        expect(loadDatabaseType).to.be.true;
      });

      it('service properties are loaded', function () {
        expect(loadServiceProperties).to.be.true;
      });

      it('cluster is loaded', function () {
        expect(load).to.be.true;
      });
    });

    describe('should load hosts', function() {
      var loadServicesFromServer = false;
      var loadMasterComponentHosts = false;
      var loadConfirmedHosts = false;

      var checker = {
        loadServicesFromServer: function () {
          loadServicesFromServer = true;
        },
        loadMasterComponentHosts: function () {
          loadMasterComponentHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadConfirmedHosts: function () {
          loadConfirmedHosts = true;
        }
      };

      beforeEach(function () {
        reassignMasterController.loadMap['2'][0].callback.call(checker);
      });

      it('services are loaded', function () {
        expect(loadServicesFromServer).to.be.true;
      });

      it('master component hosts are loaded', function () {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('confirmed hosts are loaded', function () {
        expect(loadConfirmedHosts).to.be.true;
      });
    });

    describe('should load reassign hosts', function() {
      var loadReassignHosts = false;

      var checker = {
        loadReassignHosts: function () {
          loadReassignHosts = true;
        }        
      };

      beforeEach(function () {
        reassignMasterController.loadMap['3'][0].callback.call(checker);
      });

      it('hosts are loaded', function () {
        expect(loadReassignHosts).to.be.true;
      });
    });

    describe('should loadTasksStatuses configs', function() {
      var loadTasksStatuses = false;
      var loadTasksRequestIds = false;
      var loadRequestIds = false;
      var loadReassignComponentsInMM = false;
      var loadConfigs = false;
      var loadSecureConfigs = false;

      var checker = {
        loadTasksStatuses: function () {
          loadTasksStatuses = true;
        },
        loadTasksRequestIds: function () {
          loadTasksRequestIds = true;
        },
        loadRequestIds: function () {
          loadRequestIds = true;
        },
        loadReassignComponentsInMM: function () {
          loadReassignComponentsInMM = true;
        },
        loadConfigs: function () {
          loadConfigs = true;
        },
        loadSecureConfigs: function () {
          loadSecureConfigs = true;
        }
      };

      beforeEach(function () {
        reassignMasterController.loadMap['4'][0].callback.call(checker);
      });

      it('task statuses loaded', function () {
        expect(loadTasksStatuses).to.be.true;
      });

      it('tasks request ids are loaded', function () {
        expect(loadTasksRequestIds).to.be.true;
      });

      it('request ids are loaded', function () {
        expect(loadRequestIds).to.be.true;
      });

      it('reassign component is loaded', function () {
        expect(loadReassignComponentsInMM).to.be.true;
      });

      it('configs are loaded', function () {
        expect(loadConfigs).to.be.true;
      });

      it('secure configs are loaded', function () {
        expect(loadSecureConfigs).to.be.true;
      });
    });

    describe('should load component dir', function() {
      var loadComponentDir = false;

      var checker = {
        loadComponentDir: function () {
          loadComponentDir = true;
        }
      };

      beforeEach(function () {
        reassignMasterController.loadMap['5'][0].callback.call(checker);
      });

      it('component dir is loaded', function () {
        expect(loadComponentDir).to.be.true;
      });
    });
  });

  describe('#isComponentWithReconfiguration()', function() {

    it('should return true', function () {
      reassignMasterController.set('serviceToConfigSiteMap', Em.Object.create({ comp1: {} }));
      reassignMasterController.set('content.reassign.component_name', 'comp1');
      reassignMasterController.propertyDidChange('isComponentWithReconfiguration');
      expect(reassignMasterController.get('isComponentWithReconfiguration')).to.be.true;
    });
  });

  describe('#loadTasksStatuses', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'getReassignTasksStatuses').returns('st');
    });
    afterEach(function () {
      App.db.getReassignTasksStatuses.restore();
    });

    it('should load task statuses', function () {
      reassignMasterController.loadTasksStatuses();
      expect(reassignMasterController.get('content.tasksStatuses')).to.equal('st');
    });
  });

  describe('#saveClusterStatus', function() {

    beforeEach(function() {
      sinon.stub(reassignMasterController, 'save');
    });
    afterEach(function() {
      reassignMasterController.save.restore();
    });

    it('cluster status should be saved', function() {
      reassignMasterController.set('content.cluster', {});
      reassignMasterController.saveClusterStatus({requestId: [1], oldRequestsId: []});
      expect(reassignMasterController.get('content.cluster')).to.be.eql({
        requestId: [1],
        oldRequestsId: [1]
      });
      expect(reassignMasterController.save.calledWith('cluster')).to.be.true;
    });
  });

  describe('#loadComponentToReassign', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'getMasterToReassign').returns('comp1');
    });
    afterEach(function () {
      App.db.getMasterToReassign.restore();
    });

    it('should load component to reassign', function () {
      reassignMasterController.loadComponentToReassign();
      expect(reassignMasterController.get('content.reassign')).to.equal('comp1');
    });
  });

  describe('#saveComponentToReassign', function () {
    var masterComponent = Em.Object.create({
      componentName: 'comp1',
      displayName: 'comp1',
      serviceName: 's1',
      hostName: 'host1'
    });

    beforeEach(function () {
      sinon.stub(App.db, 'setMasterToReassign');
    });
    afterEach(function () {
      App.db.setMasterToReassign.restore();
    });

    it('should save component to reassign', function () {
      reassignMasterController.saveComponentToReassign(masterComponent);
      expect(App.db.setMasterToReassign.calledWith(
        {
          component_name: masterComponent.get('componentName'),
          display_name: masterComponent.get('displayName'),
          service_id: masterComponent.get('service.serviceName'),
          host_id: masterComponent.get('hostName')
        }
      )).to.be.true;
    });
  });

  describe('#saveTasksStatuses', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'setReassignTasksStatuses');
    });
    afterEach(function () {
      App.db.setReassignTasksStatuses.restore();
    });

    it('should save task statuses', function () {
      reassignMasterController.saveTasksStatuses('st');
      expect(reassignMasterController.get('content.tasksStatuses')).to.equal('st');
    });
  });

  describe('#loadTasksRequestIds', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'getReassignTasksRequestIds').returns(['id1', 'id2']);
    });
    afterEach(function () {
      App.db.getReassignTasksRequestIds.restore();
    });

    it('should load task request ids', function () {
      reassignMasterController.loadTasksRequestIds();
      expect(reassignMasterController.get('content.tasksRequestIds')).to.eql(['id1', 'id2']);
    });
  });

  describe('#saveTasksRequestIds', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'setReassignTasksRequestIds');
    });
    afterEach(function () {
      App.db.setReassignTasksRequestIds.restore();
    });

    it('should save task request ids', function () {
      reassignMasterController.saveTasksRequestIds(['id1', 'id2']);
      expect(reassignMasterController.get('content.tasksRequestIds')).to.eql(['id1', 'id2']);
    });
  });

  describe('#loadRequestIds', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'getReassignMasterWizardRequestIds').returns(['id1', 'id2']);
    });
    afterEach(function () {
      App.db.getReassignMasterWizardRequestIds.restore();
    });

    it('should load request ids', function () {
      reassignMasterController.loadRequestIds();
      expect(reassignMasterController.get('content.requestIds')).to.eql(['id1', 'id2']);
    });
  });

  describe('#saveRequestIds', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'setReassignMasterWizardRequestIds');
    });
    afterEach(function () {
      App.db.setReassignMasterWizardRequestIds.restore();
    });

    it('should save request ids', function () {
      reassignMasterController.saveRequestIds(['id1', 'id2']);
      expect(reassignMasterController.get('content.requestIds')).to.eql(['id1', 'id2']);
    });
  });

  describe('#loadComponentDir', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'getReassignMasterWizardComponentDir').returns('dir');
    });
    afterEach(function () {
      App.db.getReassignMasterWizardComponentDir.restore();
    });

    it('should load component dir', function () {
      reassignMasterController.loadComponentDir();
      expect(reassignMasterController.get('content.componentDir')).to.equal('dir');
    });
  });

  describe('#saveComponentDir', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'setReassignMasterWizardComponentDir');
    });
    afterEach(function () {
      App.db.setReassignMasterWizardComponentDir.restore();
    });

    it('should save component dir', function () {
      reassignMasterController.saveComponentDir('dir');
      expect(reassignMasterController.get('content.componentDir')).to.equal('dir');
    });
  });

  describe('#loadReassignHosts', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'getReassignMasterWizardReassignHosts').returns(['host1', 'host2']);
    });
    afterEach(function () {
      App.db.getReassignMasterWizardReassignHosts.restore();
    });

    it('should load reassign hosts', function () {
      reassignMasterController.loadReassignHosts();
      expect(reassignMasterController.get('content.reassignHosts')).to.eql(['host1', 'host2']);
    });
  });

  describe('#saveReassignHosts', function () {

    beforeEach(function () {
      sinon.stub(App.db, 'setReassignMasterWizardReassignHosts');
    });
    afterEach(function () {
      App.db.setReassignMasterWizardReassignHosts.restore();
    });

    it('should save reassign hosts', function () {
      reassignMasterController.saveReassignHosts(['host1', 'host2']);
      expect(reassignMasterController.get('content.reassignHosts')).to.eql(['host1', 'host2']);
    });
  });

  describe('#loadSecureConfigs', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'getDBProperty').returns('conf');
    });
    afterEach(function () {
      reassignMasterController.getDBProperty.restore();
    });

    it('should load secure configs', function () {
      reassignMasterController.loadSecureConfigs();
      expect(reassignMasterController.get('content.secureConfigs')).to.equal('conf');
    });
  });

  describe('#saveSecureConfigs', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'setDBProperty');
    });
    afterEach(function () {
      reassignMasterController.setDBProperty.restore();
    });

    it('should save secure configs', function () {
      reassignMasterController.saveSecureConfigs('conf');
      expect(reassignMasterController.get('content.secureConfigs')).to.equal('conf');
    });
  });

  describe('#loadServiceProperties', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'getDBProperty').returns('props');
    });
    afterEach(function () {
      reassignMasterController.getDBProperty.restore();
    });

    it('should load service properties', function () {
      reassignMasterController.loadServiceProperties();
      expect(reassignMasterController.get('content.serviceProperties')).to.equal('props');
    });
  });

  describe('#saveServiceProperties', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'setDBProperty');
    });
    afterEach(function () {
      reassignMasterController.setDBProperty.restore();
    });

    it('should save service properties', function () {
      reassignMasterController.saveServiceProperties('props');
      expect(reassignMasterController.get('content.serviceProperties')).to.equal('props');
    });
  });

  describe('#loadConfigs', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'getDBProperties').returns(
        {
          configs: 'conf',
          configsAttributes: 'attr'
        }
      );
      reassignMasterController.loadConfigs();
    });
    afterEach(function () {
      reassignMasterController.getDBProperties.restore();
    });

    it('should load configs', function () {
      reassignMasterController.loadConfigs();
      expect(reassignMasterController.get('content.configs')).to.equal('conf');
    });

    it('should load configs attributes', function () {
      reassignMasterController.loadConfigs();
      expect(reassignMasterController.get('content.configsAttributes')).to.equal('attr');
    });
  });

  describe('#saveConfigs', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'setDBProperties');
      reassignMasterController.saveConfigs('conf', 'attr');
    });
    afterEach(function () {
      reassignMasterController.setDBProperties.restore();
    });

    it('should save configs', function () {
      expect(reassignMasterController.get('content.configs')).to.equal('conf');
    });

    it('should save configs attributes', function () {
      expect(reassignMasterController.get('content.configsAttributes')).to.equal('attr');
    });
  });

  describe('#loadDatabaseType', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'getDBProperty').returns('type');
      sinon.stub(App.router.reassignMasterController, 'get').returns({
        without: function() { return 'manual' }
      });
    });
    afterEach(function () {
      reassignMasterController.getDBProperty.restore();
      App.router.reassignMasterController.get.restore();
    });

    it('should load database type', function () {
      reassignMasterController.loadDatabaseType();
      expect(reassignMasterController.get('content.databaseType')).to.equal('type');
    });

    it('should load database type for OOZIE_SERVER component', function () {
      reassignMasterController.reopen(Em.Object.create({
        content: {
          reassign: {
            component_name: 'OOZIE_SERVER'
          },
          hasCheckDBStep: true
        }
      }));
      reassignMasterController.set('content.hasCheckDBStep', true);
      reassignMasterController.loadDatabaseType();
      expect(reassignMasterController.get('totalSteps')).to.equal(4);
    });
  });

  describe('#saveDatabaseType', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'setDBProperty');
    });
    afterEach(function () {
      reassignMasterController.setDBProperty.restore();
    });

    it('should save database type', function () {
      reassignMasterController.saveDatabaseType('type');
      expect(reassignMasterController.get('content.databaseType')).to.equal('type');
    });
  });

  describe('#loadReassignComponentsInMM', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'getDBProperty').returns('rcomp');
    });
    afterEach(function () {
      reassignMasterController.getDBProperty.restore();
    });

    it('should load reassign component', function () {
      reassignMasterController.loadReassignComponentsInMM();
      expect(reassignMasterController.get('content.reassignComponentsInMM')).to.equal('rcomp');
    });
  });

  describe('#saveReassignComponentsInMM', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'setDBProperty');
    });
    afterEach(function () {
      reassignMasterController.setDBProperty.restore();
    });

    it('should save reassign component', function () {
      reassignMasterController.saveReassignComponentsInMM('rcomp');
      expect(reassignMasterController.get('content.reassignComponentsInMM')).to.equal('rcomp');
    });
  });

  describe('#clearAllSteps', function () {

    beforeEach(function () {
      sinon.stub(reassignMasterController, 'setDBProperty');
      sinon.stub(reassignMasterController, 'clearInstallOptions');
      sinon.stub(reassignMasterController, 'getCluster').returns('c1');
      reassignMasterController.clearAllSteps();
    });
    afterEach(function () {
      reassignMasterController.setDBProperty.restore();
      reassignMasterController.clearInstallOptions.restore();
      reassignMasterController.getCluster.restore();
    });

    it('should clear install options', function () {
      expect(reassignMasterController.clearInstallOptions.calledOnce).to.be.true;
    });

    it('should set cluster', function () {
      expect(reassignMasterController.get('content.cluster')).to.equal('c1');
    });
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("should set current step", function() {
      reassignMasterController.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#finish()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({updateAll: Em.K});
      sinon.stub(reassignMasterController, 'clearStorageData');
      sinon.stub(reassignMasterController, 'clearAllSteps');
      sinon.stub(reassignMasterController, 'resetDbNamespace');
    });

    afterEach(function() {
      App.router.get.restore();
      reassignMasterController.clearStorageData.restore();
      reassignMasterController.clearAllSteps.restore();
      reassignMasterController.resetDbNamespace.restore();
    });

    it("App.router.send should be called", function() {
      reassignMasterController.finish();
      expect(reassignMasterController.clearAllSteps.calledOnce).to.be.true;
      expect(reassignMasterController.clearStorageData.calledOnce).to.be.true;
      expect(reassignMasterController.resetDbNamespace.calledOnce).to.be.true;
      expect(App.router.get.calledOnce).to.be.true;
    });
  });

  describe("#getReassignComponentsInMM()", function () {

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'NAMENODE',
          hostName: 'host1',
          workStatus: 'STARTED',
          isActive: false
        }),
        Em.Object.create({
          componentName: 'comp2',
          hostName: 'host2',
          workStatus: 'STOPPED',
          isActive: true
        })
      ]);
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function() {
      App.HostComponent.find.restore();
      App.get.restore();
    });

    it("should get reassign component", function() {
      reassignMasterController.reopen(Em.Object.create({
        content: {
          reassign: {
            component_name: 'NAMENODE'
          },
          reassignHosts: {
            source: 'host1'
          }
        }
      }));
      expect(reassignMasterController.getReassignComponentsInMM()).to.eql(['NAMENODE']);
    });
  });
});
