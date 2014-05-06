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

describe('App.WizardController', function () {

  var wizardController = App.WizardController.create({});

  var totalSteps = 11;
  var ruller = [];
  for(var i = 0; i < totalSteps; i++) {
    ruller.push(i);
  }

  describe('#setLowerStepsDisable', function() {
    for(var i = 1; i < totalSteps; i++) {
      var indx = i;
      var steps = [];
      for(var j = 1; j <= indx; j++) {
        steps.push(Em.Object.create({step:j,value:false}));
      }
      wizardController.set('isStepDisabled', steps);
      for(j = 1; j <= indx; j++) {
        it('Steps: ' + i + ' | Disabled: ' + (j-1), function() {
          wizardController.setLowerStepsDisable(j);
          expect(wizardController.get('isStepDisabled').filterProperty('value', true).length).to.equal(j-1);
        });
      }
    }
  });

  // isStep0 ... isStep10 tests
  App.WizardController1 = App.WizardController.extend({currentStep:''});
  var tests = [];
  for(var i = 0; i < totalSteps; i++) {
    var n = ruller.slice(0);
    n.splice(i,1);
    tests.push({i:i,n:n});
  }
  tests.forEach(function(test) {
    describe('isStep'+test.i, function() {
      var w = App.WizardController1.create();
      w.set('currentStep', test.i);
      it('Current Step is ' + test.i + ', so isStep' + test.i + ' is TRUE', function() {
        expect(w.get('isStep'+ test.i)).to.equal(true);
      });
      test.n.forEach(function(indx) {
        it('Current Step is ' + test.i + ', so isStep' + indx + ' is FALSE', function() {
          expect(w.get('isStep'+ indx)).to.equal(false);
        });
      });
    });
  });
  // isStep0 ... isStep10 tests end

  describe('#gotoStep', function() {
    var w = App.WizardController1.create();
    var steps = [];
    for(var j = 0; j < totalSteps; j++) {
      steps.push(Em.Object.create({step:j,value:false}));
    }
    steps.forEach(function(step, index) {
      step.set('value', true);
      w.set('isStepDisabled', steps);
      it('step ' + index + ' is disabled, so gotoStep('+index+') is not possible', function() {
        expect(w.gotoStep(index)).to.equal(false);
      });
    });
  });

  describe('#launchBootstrapSuccessCallback', function() {
    it('Save bootstrapRequestId', function() {
      var data = {requestId: 123};
      var params = {popup: {finishLoading: function(){}}};
      sinon.spy(params.popup, "finishLoading");
      wizardController.launchBootstrapSuccessCallback(data, {}, params);
      expect(params.popup.finishLoading.calledWith(123)).to.be.true;
      params.popup.finishLoading.restore();
    });
  });

});
