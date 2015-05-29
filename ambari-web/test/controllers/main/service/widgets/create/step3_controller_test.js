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

require('controllers/main/service/widgets/create/step3_controller');


describe('App.WidgetWizardStep3Controller', function () {
  var controller = App.WidgetWizardStep3Controller.create({
    content: Em.Object.create()
  });

  describe("#isEditController", function () {
    it("empty name", function () {
      controller.set('content.controllerName', '');
      controller.propertyDidChange('isEditController');
      expect(controller.get('isEditController')).to.be.false;
    });
    it("widgetEditController name", function () {
      controller.set('content.controllerName', 'widgetEditController');
      controller.propertyDidChange('isEditController');
      expect(controller.get('isEditController')).to.be.true;
    });
  });

  describe("#widgetScope", function () {
    it("isSharedChecked - false", function () {
      controller.set('isSharedChecked', false);
      controller.propertyDidChange('widgetScope');
      expect(controller.get('widgetScope')).to.equal('User');
    });
    it("isSharedChecked - true", function () {
      controller.set('isSharedChecked', true);
      controller.propertyDidChange('widgetScope');
      expect(controller.get('widgetScope')).to.equal('Cluster');
    });
  });

  describe("#isSubmitDisabled", function () {
    it("widgetName - null", function () {
      controller.set('widgetName', null);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("widgetName empty ", function () {
      controller.set('widgetName', '');
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("widgetName contains only whitespace", function () {
      controller.set('widgetName', ' ');
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("widgetName correct", function () {
      controller.set('widgetName', 'w1');
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
  });

  describe("#initPreviewData()", function () {
    beforeEach(function () {
      sinon.stub(controller, 'addObserver');
    });
    afterEach(function () {
      controller.addObserver.restore();
    });
    it("", function () {
      controller.set('content', Em.Object.create({
        widgetProperties: 'widgetProperties',
        widgetValues: 'widgetValues',
        widgetMetrics: 'widgetMetrics',
        widgetAuthor: 'widgetAuthor',
        widgetName: 'widgetName',
        widgetDescription: 'widgetDescription',
        widgetScope: 'CLUSTER',
        controllerName: 'widgetEditController'
      }));
      controller.initPreviewData();
      controller.get('isSharedCheckboxDisabled') ? expect(controller.addObserver.calledWith('isSharedChecked')).to.be.false:
        expect(controller.addObserver.calledWith('isSharedChecked')).to.be.true;
      expect(controller.get('widgetProperties')).to.equal('widgetProperties');
      expect(controller.get('widgetValues')).to.equal('widgetValues');
      expect(controller.get('widgetMetrics')).to.equal('widgetMetrics');
      expect(controller.get('widgetAuthor')).to.equal('widgetAuthor');
      expect(controller.get('widgetName')).to.equal('widgetName');
      expect(controller.get('widgetDescription')).to.equal('widgetDescription');
      expect(controller.get('isSharedChecked')).to.be.true;
      expect(controller.get('isSharedCheckboxDisabled')).to.be.true;
    });
  });

  describe("#showConfirmationOnSharing()", function () {
    beforeEach(function () {
      sinon.spy(App, 'showConfirmationFeedBackPopup');
    });
    afterEach(function () {
      App.showConfirmationFeedBackPopup.restore();
    });
    it("isSharedChecked - false", function () {
      controller.set('isSharedChecked', false);
      controller.showConfirmationOnSharing();
      expect(App.showConfirmationFeedBackPopup.called).to.be.false;
    });
    it("isSharedChecked - true", function () {
      controller.set('isSharedChecked', true);
      var popup = controller.showConfirmationOnSharing();
      expect(App.showConfirmationFeedBackPopup.calledOnce).to.be.true;
      popup.onSecondary();
      expect(controller.get('isSharedChecked')).to.be.false;
      popup.onPrimary();
      expect(controller.get('isSharedChecked')).to.be.true;
    });
  });

  describe("#collectWidgetData()", function () {
    it("", function () {
      controller.setProperties({
        widgetName: 'widgetName',
        content: Em.Object.create({widgetType: 'T1'}),
        widgetDescription: 'widgetDescription',
        widgetScope: 'Cluster',
        widgetAuthor: 'widgetAuthor',
        widgetMetrics: [{data: 'data', name: 'm1'}],
        widgetValues: [{computedValue: 'cv', value: 'v'}],
        widgetProperties: 'widgetProperties'
      });
      expect(controller.collectWidgetData()).to.eql({
        "WidgetInfo": {
          "widget_name": "widgetName",
          "widget_type": "T1",
          "description": "widgetDescription",
          "scope": "CLUSTER",
          "author": "widgetAuthor",
          "metrics": [
            {
              "name": "m1"
            }
          ],
          "values": [
            {
              "value": "v"
            }
          ],
          "properties": "widgetProperties"
        }
      });
    });
  });

  describe("#cancel()", function () {
    var mock = {
      cancel: Em.K
    };
    beforeEach(function () {
      sinon.spy(mock, 'cancel');
      sinon.stub(App.router, 'get').returns(mock);
    });
    afterEach(function () {
      App.router.get.restore();
      mock.cancel.restore();
    });
    it("", function () {
      controller.cancel();
      expect(mock.cancel.calledOnce).to.be.true;
    });
  });

  describe("#complete()", function () {
    var mock = {
      finishWizard: Em.K
    };
    beforeEach(function () {
      sinon.spy(mock, 'finishWizard');
      sinon.stub(controller, 'collectWidgetData');
      sinon.stub(App.router, 'get').returns(mock);
      sinon.stub(App.router, 'send');
    });
    afterEach(function () {
      App.router.get.restore();
      App.router.send.restore();
      controller.collectWidgetData.restore();
      mock.finishWizard.restore();
    });
    it("", function () {
      controller.complete();
      expect(controller.collectWidgetData.calledOnce).to.be.true;
      expect(App.router.send.calledWith('complete')).to.be.true;
      expect(mock.finishWizard.calledOnce).to.be.true;
    });
  });
});
