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
require('controllers/main/service/reassign/step3_controller');
var controller;

describe('App.ReassignMasterWizardStep3Controller', function () {

  beforeEach(function(){
    controller = App.ReassignMasterWizardStep3Controller.create();
  });

  describe("#submit()", function() {
    var mock = {
      getKDCSessionState: function (callback) {
        callback();
      }
    };
    beforeEach(function () {
      sinon.stub(App, 'get').returns(mock);
      sinon.spy(mock, 'getKDCSessionState');
      sinon.stub(App.router, 'send', Em.K);
      controller.submit();
    });
    afterEach(function () {
      App.get.restore();
      mock.getKDCSessionState.restore();
      App.router.send.restore();
    });
    it('getKDCSessionState is called once', function () {
      expect(mock.getKDCSessionState.calledOnce).to.be.true;
    });
    it('User is moved to the next step', function () {
      expect(App.router.send.calledWith("next")).to.be.true;
    });
  });
});
