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
require('controllers/main/alerts/add_alert_definition/add_alert_definition_controller');

var controller;

describe('App.AddAlertDefinitionController', function () {

  beforeEach(function () {
    controller = App.AddAlertDefinitionController.create();
  });

  describe("#createNewAlertDefinition()", function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it("", function () {
      controller.createNewAlertDefinition('data');
      expect(App.ajax.send.getCall(0).args[0]).to.eql({
        name: 'alerts.create_alert_definition',
        sender: controller,
        data: {
          data: 'data'
        }
      });
    });
  });

  describe("#finish()", function () {
    beforeEach(function () {
      sinon.stub(controller, 'clear', Em.K);
    });
    afterEach(function () {
      controller.clear.restore();
    });

    it("", function () {
      controller.finish();
      expect(controller.clear.calledOnce).to.be.true;
    });
  });
});