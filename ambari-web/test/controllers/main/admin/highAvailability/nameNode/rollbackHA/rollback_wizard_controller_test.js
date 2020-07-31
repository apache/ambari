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

describe('App.RollbackHighAvailabilityWizardController', function() {
  var controller;

  beforeEach(function() {
    controller = App.RollbackHighAvailabilityWizardController.create();
  });

  describe('#setCurrentStep', function() {
    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });
    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it('App.clusterStatus.setClusterStatus should be called', function() {
      controller.setCurrentStep();
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

  describe('#saveClusterStatus', function() {
    beforeEach(function() {
      sinon.stub(controller, 'save');
    });
    afterEach(function() {
      controller.save.restore();
    });

    it('cluster status should be saved', function() {
      controller.set('content.cluster', {});
      controller.saveClusterStatus({requestId: [1], oldRequestsId: []});
      expect(controller.get('content.cluster')).to.be.eql({
        requestId: [1],
        oldRequestsId: [1]
      });
      expect(controller.save.calledWith('cluster')).to.be.true;
    });
  });

  describe('#saveTasksStatuses', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setRollbackHighAvailabilityWizardTasksStatuses');
    });
    afterEach(function() {
      App.db.setRollbackHighAvailabilityWizardTasksStatuses.restore();
    });

    it('setRollbackHighAvailabilityWizardTasksStatuses should be called', function() {
      controller.saveTasksStatuses([{}]);
      expect(App.db.setRollbackHighAvailabilityWizardTasksStatuses.calledWith([{}])).to.be.true;
      expect(controller.get('content.tasksStatuses')).to.be.eql([{}]);
    });
  });

  describe('#saveRequestIds', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setRollbackHighAvailabilityWizardRequestIds');
    });
    afterEach(function() {
      App.db.setRollbackHighAvailabilityWizardRequestIds.restore();
    });

    it('setRollbackHighAvailabilityWizardRequestIds should be called', function() {
      controller.saveRequestIds([1]);
      expect(controller.get('content.requestIds')).to.be.eql([1]);
      expect(App.db.setRollbackHighAvailabilityWizardRequestIds.calledWith([1])).to.be.true;
    });
  });

  describe('#saveSelectedSNN', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setRollBackHighAvailabilityWizardSelectedSNN');
    });
    afterEach(function() {
      App.db.setRollBackHighAvailabilityWizardSelectedSNN.restore();
    });

    it('setRollBackHighAvailabilityWizardSelectedSNN should be called', function() {
      controller.saveSelectedSNN('addNN');
      expect(controller.get('content.selectedAddNN')).to.be.eql('addNN');
      expect(App.db.setRollBackHighAvailabilityWizardSelectedSNN.calledWith('addNN')).to.be.true;
    });
  });

  describe('#saveSelectedAddNN', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'setRollBackHighAvailabilityWizardSelectedAddNN');
    });
    afterEach(function() {
      App.db.setRollBackHighAvailabilityWizardSelectedAddNN.restore();
    });

    it('setRollBackHighAvailabilityWizardSelectedAddNN should be called', function() {
      controller.saveSelectedAddNN('sNN');
      expect(controller.get('content.selectedSNN')).to.be.eql('sNN');
      expect(App.db.setRollBackHighAvailabilityWizardSelectedAddNN.calledWith('sNN')).to.be.true;
    });
  });

  describe('#loadAddNNHost', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getRollBackHighAvailabilityWizardAddNNHost').returns('host1');
    });
    afterEach(function() {
      App.db.getRollBackHighAvailabilityWizardAddNNHost.restore();
    });

    it('addNNHost value should be set', function() {
      controller.loadAddNNHost();
      expect(controller.get('content.addNNHost')).to.be.equal('host1');
    });
  });

  describe('#loadSNNHost', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getRollBackHighAvailabilityWizardSNNHost').returns('host1');
    });
    afterEach(function() {
      App.db.getRollBackHighAvailabilityWizardSNNHost.restore();
    });

    it('sNNHost value should be set', function() {
      controller.loadSNNHost();
      expect(controller.get('content.sNNHost')).to.be.equal('host1');
    });
  });

  describe('#loadTasksStatuses', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getRollbackHighAvailabilityWizardTasksStatuses').returns('st');
    });
    afterEach(function() {
      App.db.getRollbackHighAvailabilityWizardTasksStatuses.restore();
    });

    it('tasksStatuses should be set', function() {
      controller.loadTasksStatuses();
      expect(controller.get('content.tasksStatuses')).to.be.eql('st');
    });
  });

  describe('#loadRequestIds', function() {
    beforeEach(function() {
      sinon.stub(App.db, 'getRollbackHighAvailabilityWizardRequestIds').returns([1]);
    });
    afterEach(function() {
      App.db.getRollbackHighAvailabilityWizardRequestIds.restore();
    });

    it('requestIds should be set', function() {
      controller.loadRequestIds();
      expect(controller.get('content.requestIds')).to.be.eql([1]);
    });
  });

  describe('#loadAllPriorSteps', function() {
    beforeEach(function() {
      sinon.stub(controller, 'loadSNNHost');
      sinon.stub(controller, 'loadAddNNHost');
      sinon.stub(controller, 'load');
    });
    afterEach(function() {
      controller.loadSNNHost.restore();
      controller.loadAddNNHost.restore();
      controller.load.restore();
    });

    ['1', '2', '3'].forEach(function(step) {
      it('should load data for step '+step, function() {
      controller.set('currentStep', step);
      controller.loadAllPriorSteps();
      expect(controller.loadSNNHost.calledOnce).to.be.true;
      expect(controller.loadAddNNHost.calledOnce).to.be.true;
      expect(controller.load.calledOnce).to.be.true;
    })});
  });

  describe('#clearAllSteps', function() {
    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions');
      sinon.stub(controller, 'getCluster').returns({name: 'c1'});
    });
    afterEach(function() {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });

    it('clearInstallOptions should be called', function() {
      controller.clearAllSteps();
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
      expect(controller.get('content.cluster')).to.be.eql({name: 'c1'});
    });
  });

  describe('#clearTasksData', function() {
    beforeEach(function() {
      sinon.stub(controller, 'saveRequestIds');
      sinon.stub(controller, 'saveTasksStatuses');
      controller.clearTasksData();
    });
    afterEach(function() {
      controller.saveRequestIds.restore();
      controller.saveTasksStatuses.restore();
    });

    it('saveRequestIds should be called', function() {
      expect(controller.saveRequestIds.calledWith(undefined)).to.be.true;
    });

    it('saveTasksStatuses should be called', function() {
      expect(controller.saveTasksStatuses.calledWith(undefined)).to.be.true;
    });
  });

  describe('#finish', function() {
    var mock = {
      updateAll: Em.K
    };
    beforeEach(function() {
      sinon.spy(mock, 'updateAll');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(controller, 'setCurrentStep');
      sinon.stub(controller, 'clearAllSteps');
      controller.finish();
    });
    afterEach(function() {
      controller.setCurrentStep.restore();
      controller.clearAllSteps.restore();
      mock.updateAll.restore();
      App.router.get.restore();
    });

    it('setCurrentStep should be called', function() {
      expect(controller.setCurrentStep.calledWith('1')).to.be.true;
    });

    it('clearAllSteps should be called', function() {
      expect(controller.clearAllSteps.calledOnce).to.be.true;
    });

    it('updateAll should be called', function() {
      expect(mock.updateAll.called).to.be.true;
    });
  });
});
