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
require('controllers/main/admin/highAvailability/hawq/removeStandby/step2_controller');

function getController() {
  return App.RemoveHawqStandbyWizardStep2Controller.create({});
}

describe('App.RemoveHawqStandbyWizardStep2Controller', function () {
  var controller;

  beforeEach(function () {
    controller = getController();
  });

  describe('#isSubmitDisabled', function () {

    var cases = [
      {
        isLoaded: false,
        isSubmitDisabled: true,
        title: 'wizard step content not loaded'
      },
      {
        isLoaded: true,
        isSubmitDisabled: false,
        title: 'wizard step content loaded'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.reopen({
          content: Em.Object.create({})
        });
        controller.set('isLoaded', item.isLoaded);
        expect(controller.get('isSubmitDisabled')).to.equal(item.isSubmitDisabled);
      });
    });

  });

  describe("#submit()", function () {

    var mock = {
      getKDCSessionState: Em.clb
    };

    beforeEach(function() {
      sinon.spy(mock, 'getKDCSessionState');
      sinon.stub(App, 'get').returns(mock);
      sinon.stub(App.router, 'send');
    });
    afterEach(function() {
      App.get.restore();
      App.router.send.restore();
      mock.getKDCSessionState.restore();
    });

    it('App.router.send should be called', function() {
      controller.set('isLoaded', true);
      controller.submit();
      expect(mock.getKDCSessionState.calledOnce).to.be.true;
      expect(App.router.send.calledOnce).to.be.true;
    });

    it('App.router.send should not be called', function() {
      controller.set('isLoaded', false);
      controller.submit();
      expect(mock.getKDCSessionState.calledOnce).to.be.false;
      expect(App.router.send.calledOnce).to.be.false;
    });
  });

});
