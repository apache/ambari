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
require('controllers/main/admin/highAvailability/hawq/removeStandby/wizard_controller');

function getController() {
  return App.RemoveHawqStandbyWizardController.create({});
}

describe('App.RemoveHawqStandbyWizardController', function () {
  var controller;

  beforeEach(function () {
    controller = getController();
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("should set current step", function() {
      controller.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe('#loadMap', function() {

    describe('should load cluster', function() {
      var loadCluster = false;
      var checker = {
        load: function() {
          loadCluster = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['1'][0].callback.call(checker);
      });

      it('cluster info is loaded', function () {
        expect(loadCluster).to.be.true;
      });
    });

    describe('should load service hosts', function() {
      var loadServicesFromServer = false;
      var loadMasterComponentHosts = false;
      var loadHawqHosts = false;

      var checker = {
        loadServicesFromServer: function () {
          loadServicesFromServer = true;
        },
        loadMasterComponentHosts: function () {
          loadMasterComponentHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadHawqHosts: function () {
          loadHawqHosts = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['2'][0].callback.call(checker);
      });

      it('services from server are loaded', function () {
        expect(loadServicesFromServer).to.be.true;
      });

      it('master component hosts are loaded', function () {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('Hawq hosts are loaded', function () {
        expect(loadHawqHosts).to.be.true;
      });
    });

    describe('should load tasks', function() {
      var loadConfigs = false;
      var loadTasksStatuses = false;
      var loadTasksRequestIds = false;
      var loadRequestIds = false;

      var checker = {
        loadConfigs: function () {
          loadConfigs = true;
        },
        loadTasksStatuses: function () {
          loadTasksStatuses = true;
          return $.Deferred().resolve().promise();
        },
        loadTasksRequestIds: function () {
          loadTasksRequestIds = true;
        },
        loadRequestIds: function () {
          loadRequestIds = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['3'][0].callback.call(checker);
      });

      it('service config properties are loaded', function () {
        expect(loadConfigs).to.be.true;
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

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("App.clusterStatus.setClusterStatus should be called", function() {
      controller.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#saveHawqHosts()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it("hawqHosts should be set", function() {
      controller.saveHawqHosts(['host1']);
      expect(controller.get('content.hawqHosts')).to.be.eql(['host1']);
    });

    it("setDBProperty should be called", function() {
      controller.saveHawqHosts(['host1']);
      expect(controller.setDBProperty.calledWith('hawqHosts', ['host1'])).to.be.true;
    });
  });

  describe("#loadHawqHosts()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns(['host1']);
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("hawqHosts should be set", function() {
      controller.loadHawqHosts();
      expect(controller.get('content.hawqHosts')).to.be.eql(['host1']);
    });
  });

  describe("#saveConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty');
    });

    afterEach(function() {
      controller.setDBProperty.restore();
    });

    it("configs should be set", function() {
      controller.saveConfigs([{}]);
      expect(controller.get('content.configs')).to.be.eql([{}]);
    });

    it("setDBProperty should be called", function() {
      controller.saveConfigs([{}]);
      expect(controller.setDBProperty.calledWith('configs', [{}])).to.be.true;
    });
  });

  describe("#loadConfigs()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns([{}]);
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("configs should be set", function() {
      controller.loadConfigs();
      expect(controller.get('content.configs')).to.be.eql([{}]);
    });
  });

  describe("#clearAllSteps()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({});
      controller.clearAllSteps();
    });

    afterEach(function() {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });

    it("clearInstallOptions should be called", function() {
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
    });

    it("cluster should be set", function() {
      expect(controller.get('content.cluster')).to.be.eql({});
    });
  });

  describe("#finish()", function () {
    var container = {
      updateAll: Em.K
    };

    beforeEach(function() {
      sinon.stub(controller, 'resetDbNamespace');
      sinon.stub(App.router, 'get').returns(container);
      sinon.stub(container, 'updateAll');
      controller.finish();
    });

    afterEach(function() {
      controller.resetDbNamespace.restore();
      App.router.get.restore();
      container.updateAll.restore();
    });

    it("resetDbNamespace should be called", function() {
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });

    it("updateAll should be called", function() {
      expect(container.updateAll.calledOnce).to.be.true;
    });

    it("isFinished should be true", function() {
      expect(controller.get('isFinished')).to.be.true;
    });
  });

});
