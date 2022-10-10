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
require('controllers/main/admin/highAvailability/journalNode/wizard_controller');


describe('App.ManageJournalNodeWizardController', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardController.create({
      content: Em.Object.create(),
      currentStep: 0
    });
  });

  describe('#setCurrentStep', function() {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it('setCurrentStep should be called', function() {
      controller.setCurrentStep(1, true);
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe('#getCluster', function() {

    beforeEach(function() {
      sinon.stub(App.router, 'getClusterName').returns('c1');
    });
    afterEach(function() {
      App.router.getClusterName.restore();
    });

    it('should return cluster object', function() {
      controller.set('clusterStatusTemplate', {});
      expect(controller.getCluster()).to.be.eql({
        name: 'c1'
      });
    });
  });

  describe('#getJournalNodesToAdd', function() {

    it('should return journalnodes hosts array', function() {
      controller.set('content.masterComponentHosts', [
        Em.Object.create({
          component: 'JOURNALNODE',
          isInstalled: false,
          hostName: 'host1'
        })
      ]);
      expect(controller.getJournalNodesToAdd()).to.be.eql(['host1']);
    });

    it('should return empty array', function() {
      controller.set('content.masterComponentHosts', null);
      expect(controller.getJournalNodesToAdd()).to.be.empty;
    });
  });

  describe('#getJournalNodesToDelete', function() {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'JOURNALNODE',
          hostName: 'host2'
        }),
        Em.Object.create({
          componentName: 'JOURNALNODE',
          hostName: 'host1'
        })
      ]);
    });

    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('should return empty array', function() {
      controller.set('content.masterComponentHosts', null);
      expect(controller.getJournalNodesToDelete()).to.be.empty;
    });

    it('should return journalnodes hosts array', function() {
      controller.set('content.masterComponentHosts', [
        Em.Object.create({
          component: 'JOURNALNODE',
          isInstalled: false,
          hostName: 'host1'
        })
      ]);
      expect(controller.getJournalNodesToDelete()).to.be.eql([
        'host2'
      ]);
    });
  });

  describe('#isDeleteOnly', function() {

    beforeEach(function() {
      this.mockAdd = sinon.stub(controller, 'getJournalNodesToAdd').returns([]);
      this.mockDelete = sinon.stub(controller, 'getJournalNodesToDelete').returns([]);
    });
    afterEach(function() {
      this.mockDelete.restore();
      this.mockAdd.restore();
    });

    it('should return false when currentStep less than 2nd', function() {
      controller.set('currentStep', 1);
      expect(controller.get('isDeleteOnly')).to.be.false;
    });

    it('should return false when there are nodes to add', function() {
      this.mockAdd.returns(['host1']);
      controller.set('currentStep', 2);
      controller.propertyDidChange('isDeleteOnly');
      expect(controller.get('isDeleteOnly')).to.be.false;
    });

    it('should return false when there are no nodes to delete', function() {
      this.mockAdd.returns([]);
      this.mockDelete.returns([]);
      controller.set('currentStep', 2);
      controller.propertyDidChange('isDeleteOnly');
      expect(controller.get('isDeleteOnly')).to.be.false;
    });

    it('should return true when there are nodes to delete', function() {
      this.mockAdd.returns([]);
      this.mockDelete.returns(['host1']);
      controller.set('currentStep', 2);
      controller.propertyDidChange('isDeleteOnly');
      expect(controller.get('isDeleteOnly')).to.be.true;
    });
  });

  describe('#saveServiceConfigProperties', function() {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it('serviceConfigProperties should be set', function() {
      var stepCtrl = Em.Object.create({
        serverConfigData: {
          items: [{
            type: 'file1',
            properties: {
              c1: 'val1'
            }
          }]
        },
        stepConfigs: [
          Em.Object.create({
            configs: [
              Em.Object.create({
                name: 'c1',
                value: 'val1',
                filename: 'file1'
              })
            ]
          })
        ]
      });
      controller.saveServiceConfigProperties(stepCtrl);
      expect(JSON.stringify(controller.get('content.serviceConfigProperties'))).to.be.eql(JSON.stringify({
        "items": [
          {
            "type": "file1",
            "properties": {
              "c1": "val1"
            }
          }
        ]
      }));
      expect(controller.setDBProperty.calledWith('serviceConfigProperties', {
        "items": [
          {
            "type": "file1",
            "properties": {
              "c1": "val1"
            }
          }
        ]
      })).to.be.true;
    });
  });

  describe('#loadServiceConfigProperties', function() {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns({});
    });

    it('serviceConfigProperties should be set', function() {
      controller.loadServiceConfigProperties();
      expect(controller.get('content.serviceConfigProperties')).to.be.eql({});
    });
  });

  describe('#saveNNs', function() {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        {
          displayNameAdvanced: 'Active NameNode'
        },
        {
          displayNameAdvanced: 'Standby NameNode'
        }
      ]);
      sinon.stub(controller, 'setDBProperty');
    });

    afterEach(function() {
      controller.setDBProperty.restore();
      App.HostComponent.find.restore();
    });

    it('NameNodes should be set', function() {
      controller.saveNNs();
      expect(controller.get('content.activeNN')).to.be.eql({
        displayNameAdvanced: 'Active NameNode'
      });
      expect(controller.get('content.standByNN')).to.be.eql({
        displayNameAdvanced: 'Standby NameNode'
      });
    });

    it('setDBProperty should be called', function() {
      controller.saveNNs();
      expect(controller.setDBProperty.calledWith('activeNN', {
        displayNameAdvanced: 'Active NameNode'
      })).to.be.true;
      expect(controller.setDBProperty.calledWith('standByNN', {
        displayNameAdvanced: 'Standby NameNode'
      })).to.be.true;
    });
  });

  describe('#loadNNs', function() {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns({});
    });

    it('NameNodes should be set', function() {
      controller.loadNNs();
      expect(controller.get('content.activeNN')).to.be.eql({});
      expect(controller.get('content.standByNN')).to.be.eql({});
    });
  });

  describe('#loadConfigTag', function() {

    beforeEach(function() {
      sinon.stub(App.db, 'getManageJournalNodeWizardConfigTag').returns('val1');
    });

    it('should load config tag', function() {
      controller.loadConfigTag('tag1');
      expect(App.db.getManageJournalNodeWizardConfigTag.calledWith('tag1')).to.be.true;
      expect(controller.get('content.tag1')).to.be.equal('val1');
    });
  });

  describe('#saveConfigTag', function() {

    beforeEach(function() {
      sinon.stub(App.db, 'setManageJournalNodeWizardConfigTag');
    });

    it('should save config tag', function() {
      controller.saveConfigTag({name: 'tag1', value: 'val1'});
      expect(App.db.setManageJournalNodeWizardConfigTag.calledWith({name: 'tag1', value: 'val1'})).to.be.true;
      expect(controller.get('content.tag1')).to.be.equal('val1');
    });
  });

  describe('#saveNameServiceIds', function() {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });

    it('nameServiceId should be set', function() {
      controller.saveNameServiceIds(['id0', 'id1']);
      expect(controller.setDBProperty.calledWith('nameServiceIds', ['id0', 'id1'])).to.be.true;
      expect(controller.get('content.nameServiceIds')).to.eql(['id0', 'id1']);
    });
  });

  describe('#loadNameServiceIds', function() {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns(['id0', 'id1']);
    });

    it('nameServiceId should be set', function() {
      controller.loadNameServiceIds();
      expect(controller.get('content.nameServiceIds')).to.eql(['id0', 'id1']);
    });
  });

  describe('#clearAllSteps', function() {

    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({name: 'c1'});
    });

    it('cluster should be set and clearInstallOptions should be called', function() {
      controller.clearAllSteps();
      expect(controller.get('content.cluster')).to.be.eql({name: 'c1'});
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
    });
  });

  describe('#clearTasksData', function() {

    beforeEach(function() {
      sinon.stub(controller, 'saveTasksStatuses');
      sinon.stub(controller, 'saveRequestIds');
      sinon.stub(controller, 'saveTasksRequestIds');
      controller.clearTasksData();
    });

    afterEach(function() {
      controller.saveTasksRequestIds.restore();
      controller.saveRequestIds.restore();
      controller.saveTasksStatuses.restore();
    });

    it('saveTasksStatuses should be called', function() {
      expect(controller.saveTasksStatuses.calledWith(undefined)).to.be.true;
    });

    it('saveRequestIds should be called', function() {
      expect(controller.saveRequestIds.calledWith(undefined)).to.be.true;
    });

    it('saveTasksRequestIds should be called', function() {
      expect(controller.saveTasksRequestIds.calledWith(undefined)).to.be.true;
    });
  });

  describe('#finish', function() {
    var mock = {
      updateAll: Em.K
    };

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'updateAll');
      sinon.stub(controller, 'resetDbNamespace');
      controller.finish();
    });

    afterEach(function() {
      controller.resetDbNamespace.restore();
      mock.updateAll.restore();
      App.router.get.restore();
    });

    it('resetDbNamespace should be called', function() {
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });

    it('updateAll should be called', function() {
      expect(mock.updateAll.calledOnce).to.be.true;
    });
  });

  describe('#loadMap', function() {

    describe('should load hdfs user', function() {
      var loadServicesFromServer = false;
      var loadMasterComponentHosts = false;
      var loadHosts = false;
      var loadHdfsUserFromServer = false;
      var loadCluster = false;
      var loadHdfsUser = false;
      var saveHdfsUser = false;
      var saveNNs = false;
      var loadNNs = false;

      var checker = {
        loadServicesFromServer: function () {
          loadServicesFromServer = true;
        },
        loadMasterComponentHosts: function () {
          loadMasterComponentHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadHosts: function () {
          loadHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadHdfsUserFromServer: function () {
          loadHdfsUserFromServer = true;
          return $.Deferred().resolve().promise();
        },
        load: function () {
          loadCluster = true;
          loadHdfsUser = true;
        },
        save: function () {
          saveHdfsUser = true;
        },
        saveNNs: function () {
          saveNNs = true;
        },
        loadNNs: function () {
          loadNNs = true;
        },
        getDBProperty: function () {
          return true;
        }
      };

      beforeEach(function () {
        controller.loadMap['1'][0].callback.call(checker);
      })

      it('services from server are loaded', function () {
        expect(loadServicesFromServer).to.be.true;
      });

      it('master component hosts are loaded', function () {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('hosts are loaded', function () {
        expect(loadHosts).to.be.true;
      });

      it('hdfs users are loaded', function () {
        expect(loadHdfsUserFromServer).to.be.false;
      });

      it('cluster is loaded', function () {
        expect(loadCluster).to.be.true;
      });

      it('hdfs user is loaded', function () {
        expect(loadHdfsUser).to.be.true;
      });

      it('hdfs user is saved', function () {
        expect(saveHdfsUser).to.be.true;
      });

      it('NNs are saved', function () {
        expect(saveNNs).to.be.false;
      });

      it('NNs are loaded', function () {
        expect(loadNNs).to.be.true;
      });
    });

    describe('should service config properties', function() {
      var loadNameServiceIds = false;
      var loadServiceConfigProperties = false;

      var checker = {
        loadServiceConfigProperties: function () {
          loadServiceConfigProperties = true;
        },
        loadNameServiceIds: function () {
          loadNameServiceIds = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['2'][0].callback.call(checker);
      });

      it('name service ids are loaded', function () {
        expect(loadNameServiceIds).to.be.true;
      });

      it('service config properties are loaded', function () {
        expect(loadServiceConfigProperties).to.be.true;
      });
    });

    describe('should load tasks', function() {
      var loadTasksStatuses = false;
      var loadTasksRequestIds = false;
      var loadRequestIds = false;

      var checker = {
        loadTasksStatuses: function () {
          loadTasksStatuses = true;
        },
        loadTasksRequestIds: function () {
          loadTasksRequestIds = true;
        },
        loadRequestIds: function () {
          loadRequestIds = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['4'][0].callback.call(checker);
      });

      it('task statuses are loaded', function () {
        expect(loadTasksStatuses).to.be.true;
      });

      it('task request ids are loaded', function () {
        expect(loadTasksRequestIds).to.be.true;
      });

      it('request ids are loaded', function () {
        expect(loadRequestIds).to.be.true;
      });
    });
  });
});
