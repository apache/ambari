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
require('controllers/main/service/widgets/edit_controller');
var testHelpers = require('test/helpers');

describe('App.WidgetEditController', function () {
  var controller;

  beforeEach(function () {
    controller = App.WidgetEditController.create({});
  });

  describe("#installServicesRequest()", function () {

    it("should call send ajax request", function() {
      var args = testHelpers.findAjaxRequest('name', 'widgets.wizard.edit');
      controller.set('content.widgetId', 'id');
      controller.putWidgetDefinition({});
      expect(args).to.exists;
    });
  });

  describe("#finish()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setCurrentStep');
      sinon.stub(controller, 'save');
      sinon.stub(controller, 'resetDbNamespace');
    });

    afterEach(function() {
      controller.setCurrentStep.restore();
      controller.save.restore();
      controller.resetDbNamespace.restore();
    });

    it("should set current step", function() {
      controller.finish();
      expect(controller.setCurrentStep.calledOnce).to.be.true;
      expect(controller.save.callCount).to.equal(13);
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });
  });

  describe('#loadMap', function() {

    describe('should load widget data', function () {
      var loadCount = 0;

      var checker = {
        load: function () {
          loadCount++;
        }
      };

      beforeEach(function () {
        controller.loadMap['1'][0].callback.call(checker);
      });

      it('widget data is loaded', function () {
        expect(loadCount).to.equal(5);
      });
    });

    describe('should load all metrics', function () {
      var loadAllMetrics = false;

      var checker = {
        loadAllMetrics: function () {
          loadAllMetrics = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['1'][1].callback.call(checker);
      });

      it('all metrics are loaded', function () {
        expect(loadAllMetrics).to.be.true;
      });
    });

    describe('should load widget info', function () {
      var loadCount = 0;

      var checker = {
        load: function () {
          loadCount++;
        }
      };

      beforeEach(function () {
        controller.loadMap['2'][0].callback.call(checker);
      });

      it('widget info is loaded', function () {
        expect(loadCount).to.equal(3);
      });
    });
  });
});
