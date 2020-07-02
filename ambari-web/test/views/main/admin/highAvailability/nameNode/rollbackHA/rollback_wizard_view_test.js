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
require('views/main/admin/highAvailability/nameNode/rollbackHA/rollback_wizard_view');

describe('App.RollbackHighAvailabilityWizardView', function () {
  var view = App.RollbackHighAvailabilityWizardView.create({
    controller: Em.Object.create({
      content: {},
      setLowerStepsDisable: Em.K
    })
  });

  describe("#didInsertElement()", function() {

    beforeEach(function () {
      sinon.spy(view.get('controller'), 'setLowerStepsDisable');
    });
    afterEach(function () {
      view.get('controller').setLowerStepsDisable.restore();
    });

    it("setLowerStepsDisable should not be called", function () {
      view.set('controller.currentStep', 3);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.called).to.be.false;
    });

    it("call setLowerStepsDisable", function () {
      view.set('controller.currentStep', 5);
      view.didInsertElement();
      expect(view.get('controller').setLowerStepsDisable.calledOnce).to.be.true;
    });

  });
});
