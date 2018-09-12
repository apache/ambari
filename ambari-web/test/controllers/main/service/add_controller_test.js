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
require('controllers/main/service/add_controller');

describe('App.AddServiceController', function () {

  var addServiceController = App.AddServiceController.create();

  after(function () {
    addServiceController.destroy();
  });

  describe('#init', function () {
    var c;
    beforeEach(function () {
      c = App.AddServiceController.create({});
    });
    it('all steps are disabled by default', function () {
      expect(c.get('isStepDisabled.length')).to.eq(c.get('totalSteps'));
      for (var i = 0, length = c.get('isStepDisabled.length'); i < length; i++) {
        expect(c.get('isStepDisabled').findProperty('step', i).get('value')).to.eq(true);
      }
    });
  });

  describe('#setLowerStepsDisable', function() {

    beforeEach(function () {
      var steps = Em.A([
        Em.Object.create({
          step: 0,
          value: false
        }),
        Em.Object.create({
          step: 1,
          value: false
        }),
        Em.Object.create({
          step: 2,
          value: false
        }),
        Em.Object.create({
          step: 3,
          value: false
        }),
        Em.Object.create({
          step: 4,
          value: false
        })
      ]);
      addServiceController.set('isStepDisabled', steps);
      addServiceController.setLowerStepsDisable(3);
    });

    it('Should disable lower steps', function() {
      var expected = [
        {
          "step": 0,
          "value": true
        },
        {
          "step": 1,
          "value": true
        },
        {
          "step": 2,
          "value": true
        },
        {
          "step": 3,
          "value": false
        },
        {
          "step": 4,
          "value": false
        }
      ];
      var res = JSON.parse(JSON.stringify(addServiceController.get('isStepDisabled')));
      expect(res).to.eql(expected);
    });
  });

  describe('#totalSteps', function() {
    beforeEach(function() {
      addServiceController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);
    });

    it('Should return the number of steps', function() {
      var totalSteps = addServiceController.get('totalSteps');
      expect(totalSteps).to.eq(addServiceController.get("steps").length);
    });
  })

  describe('#setStepsEnable', function() {
    beforeEach(function () {
      var steps = Em.A([
        Em.Object.create({
          step: 0,
          value: true
        }),
        Em.Object.create({
          step: 1,
          value: true
        }),
        Em.Object.create({
          step: 2,
          value: true
        }),
        Em.Object.create({
          step: 3,
          value: true
        }),
        Em.Object.create({
          step: 4,
          value: true
        })
      ]);
      
      addServiceController.set('isStepDisabled', steps);
      
      addServiceController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);
      
      addServiceController.set('currentStep', 2);
    });

    it('Should enable next steps', function() {
      var stepController = Em.Object.create({
        isStepDisabled: function () {
          return false;
        }
      });

      sinon.stub(addServiceController, 'getStepController').returns(stepController);

      var expected = [
        {
          "step": 0,
          "value": false
        },
        {
          "step": 1,
          "value": false
        },
        {
          "step": 2,
          "value": false
        },
        {
          "step": 3,
          "value": false
        },
        {
          "step": 4,
          "value": false
        }
      ];

      addServiceController.setStepsEnable();

      var res = JSON.parse(JSON.stringify(addServiceController.get('isStepDisabled')));
      expect(res).to.eql(expected);

      addServiceController.getStepController.restore();
    });
  });

  describe('#saveMasterComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(addServiceController, 'getDBProperty').returns({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
    afterEach(function () {
      addServiceController.getDBProperty.restore();
    });
    it ('Should return hosts', function() {
      var stepController = Em.Object.create({
        selectedServicesMasters: Em.A([
          Em.Object.create({
            display_name: 'n1',
            component_name: 'c1',
            serviceId: 1,
            selectedHost: 'h1',
            mpackInstance: 'm1'
          })
        ])
      });
      addServiceController.saveMasterComponentHosts(stepController);
      expect(addServiceController.get('content.masterComponentHosts')).to.eql([
        {
          "display_name": "n1",
          "component": "c1",
          "serviceId": 1,
          "isInstalled": false,
          "host_id": 11,
          "serviceGroupName": "m1"
        }
      ]);
    });
  });

  describe('#loadConfirmedHosts', function() {
    beforeEach(function () {
      sinon.stub(addServiceController, 'getDBProperty').returns({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
    afterEach(function () {
      addServiceController.getDBProperty.restore();
    });
    it ('Should load hosts from db', function() {
      addServiceController.loadConfirmedHosts();
      expect(addServiceController.get('content.hosts')).to.eql({
        'h1': {
          id: 11
        },
        'h3': {
          id: 13
        },
        'h2': {
          id: 12
        }
      });
    });
  });

  describe('#loadMasterComponentHosts', function() {
    beforeEach(function () {
      addServiceController.set('content.masterComponentHosts', null);
      sinon.stub(addServiceController, 'getDBProperties', function() {
        return {
          masterComponentHosts: Em.A([
            {
              hostName: '',
              host_id: 11
            }
          ]),
          hosts: {
            'h1': {
              id: 11
            },
            'h3': {
              id: 13
            },
            'h2': {
              id: 12
            }
          }
        }
      });
    });
    afterEach(function () {
      addServiceController.getDBProperties.restore();
    });
    it('Should load hosts', function() {
      addServiceController.loadMasterComponentHosts();
      expect(addServiceController.get('content.masterComponentHosts')).to.eql([
        {
          "hostName": "h1",
          "host_id": 11
        }
      ]);
    });
  });

  describe('#loadSlaveComponentHosts', function() {
    beforeEach(function () {
      sinon.stub(addServiceController, 'getDBProperties', function() {
        return {
          hosts: {
            'h1': {
              id: 11
            },
            'h3': {
              id: 13
            },
            'h2': {
              id: 12
            }
          },
          slaveComponentHosts: Em.A([
            {
              hosts: Em.A([
                {
                  hostName: '',
                  host_id: 11
                }
              ])
            }
          ])
        };
      });
    });
    afterEach(function () {
      addServiceController.getDBProperties.restore();
    });
    it ('Should load slave hosts', function() {
      addServiceController.loadSlaveComponentHosts();
      expect(addServiceController.get('content.slaveComponentHosts')).to.eql([
        {
          "hosts": [
            {
              "hostName": "h1",
              "host_id": 11
            }
          ]
        }
      ]);
    });
  });

  describe('#setStepSaved', function() {
    beforeEach(function() {
      addServiceController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);

      addServiceController.set('content.stepsSavedState', null);
    });

    it('Should save step and unsave all subsequent steps when step is not saved yet', function() {
      var expected = Em.Object.create({
        "1": true,
        "2": false,
        "3": false,
        "4": false
      });

      addServiceController.setStepSaved('step1');
      var actual = addServiceController.get('content.stepsSavedState');
      expect(actual).to.deep.equal(expected);
      expect(addServiceController.getStepSavedState('step1')).to.be.true;
    });

    it('Should do nothing when step is already saved', function() {
      var expected = Em.Object.create({
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      addServiceController.set('content.stepsSavedState', expected);

      addServiceController.setStepSaved('step1');
      var actual = addServiceController.get('content.stepsSavedState');
      expect(actual).to.deep.equal(expected);
      expect(addServiceController.getStepSavedState('step1')).to.be.true;
    });
  });

  describe('#setStepUnsaved', function() {
    beforeEach(function() {
      addServiceController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);

      var initial = Em.Object.create({
        "1": true,
        "2": true,
        "3": true,
        "4": true
      })
      addServiceController.set('content.stepsSavedState', initial);
    });

    it('Should set step to unsaved', function() {
      var expected = Em.Object.create({
        "1": false,
        "2": true,
        "3": true,
        "4": true
      })

      addServiceController.setStepUnsaved('step1');
      var actual = addServiceController.get('content.stepsSavedState');
      expect(actual).to.deep.equal(expected);
      expect(addServiceController.getStepSavedState('step1')).to.be.false;
    });
  });

  describe('#getStepSavedState', function() {
    beforeEach(function() {
      addServiceController.set('steps', [
        "step0",
        "step1",
        "step2",
        "step3",
        "step4"
      ]);

      var initial = Em.Object.create({
        "1": true,
        "2": false
      })
      addServiceController.set('content.stepsSavedState', initial);
    });

    it('Should return false for bad step name', function() {
      expect(addServiceController.getStepSavedState('step5')).to.be.false;
    });

    it('Should return false for step that was never saved', function() {
      expect(addServiceController.getStepSavedState('step0')).to.be.false;
    });
  });

  describe('#hasErrors', function () {
    before(function () {
      addServiceController.addError("There is an error.");
    });

    it('Should return true if there are errors.', function () {
      var hasErrors = addServiceController.get('hasErrors');

      expect(hasErrors).to.be.true;
    });

    it('Should return false if there are no errors.', function () {
      addServiceController.clearErrors();
      var hasErrors = addServiceController.get('hasErrors');

      expect(hasErrors).to.be.false;
    });
  });

  describe('#getStepController', function () {
    var wizardStep0Controller = {};
    var wizardStep2Controller = {};

    before(function () {
      addServiceController.set('steps', [
        "step0",
        "step1",
        "step2"
      ]);

      App.router.set('wizardStep0Controller', wizardStep0Controller);
      App.router.set('wizardStep2Controller', wizardStep2Controller);
    });

    it('Should return controller for the step number provided.', function () {
      var stepController = addServiceController.getStepController(2);
      expect(stepController).to.equal(wizardStep2Controller);
    });

    it('Should return controller for the step name provided.', function () {
      var stepController = addServiceController.getStepController("step0");
      expect(stepController).to.equal(wizardStep0Controller);
    });
  });

  describe('#finish', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'setCurrentStep');
      sinon.stub(addServiceController, 'clearStorageData');
      sinon.stub(addServiceController, 'clearServiceConfigProperties');
      sinon.stub(App.themesMapper, 'resetModels');
      addServiceController.finish();
    });
    afterEach(function() {
      addServiceController.setCurrentStep.restore();
      addServiceController.clearStorageData.restore();
      addServiceController.clearServiceConfigProperties.restore();
      App.themesMapper.resetModels.restore();
    });

    it('setCurrentStep should be called', function() {
      expect(addServiceController.setCurrentStep.calledWith('0')).to.be.true;
    });

    it('clearStorageData should be called', function() {
      expect(addServiceController.clearStorageData.calledOnce).to.be.true;
    });

    it('clearServiceConfigProperties should be called', function() {
      expect(addServiceController.clearServiceConfigProperties.calledOnce).to.be.true;
    });

    it('App.themesMapper.resetModels should be called', function() {
      expect(App.themesMapper.resetModels.calledOnce).to.be.true;
    });
  });

  describe('#loadClients', function () {
    var cases = [
      {
        clients: null,
        contentClients: [],
        saveClientsCallCount: 1,
        title: 'no clients info in local db'
      },
      {
        clients: [{}],
        contentClients: [{}],
        saveClientsCallCount: 0,
        title: 'clients info saved in local db'
      }
    ];

    cases.forEach(function (item) {
      describe(item.title, function () {
        beforeEach(function () {
          sinon.stub(addServiceController, 'getDBProperty').withArgs('clients').returns(item.clients);
          addServiceController.set('content.clients', []);
          addServiceController.loadClients();
        });

        afterEach(function () {
          addServiceController.getDBProperty.restore();
        });

        it('content.clients', function () {
          expect(addServiceController.get('content.clients', [])).to.eql(item.contentClients);
        });
      });
    });
  });
});
