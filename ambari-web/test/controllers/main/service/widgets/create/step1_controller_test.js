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

App = require('app');

require('controllers/main/service/widgets/create/step1_controller');


describe('App.WidgetWizardStep1Controller', function () {
  var controller = App.WidgetWizardStep1Controller.create();

  describe("#isSubmitDisabled", function() {
    it("disabled", function() {
      controller.set('widgetType', '');
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("enabled", function() {
      controller.set('widgetType', 'w1');
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
  });

  describe("#chooseOption()", function () {
    before(function () {
      sinon.stub(controller, 'next');
    });
    after(function () {
      controller.next.restore();
    });
    it("", function () {
      controller.chooseOption({context: 'type1'});
      expect(controller.get('widgetType')).to.equal('type1');
      expect(controller.next.calledOnce).to.be.true;
    });
  });

  describe("#loadStep()", function () {
    before(function () {
      sinon.stub(controller, 'clearStep');
    });
    after(function () {
      controller.clearStep.restore();
    });
    it("", function () {
      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
    });
  });

  describe("#clearStep()", function () {
    it("", function () {
      controller.clearStep();
      expect(controller.get('widgetType')).to.be.empty;
    });
  });

  describe("#next()", function () {
    before(function () {
      sinon.stub(App.router, 'send');
    });
    after(function () {
      App.router.send.restore();
    });
    it("", function () {
      controller.next();
      expect(App.router.send.calledWith('next')).to.be.true;
    });
  });



});
