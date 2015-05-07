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

require('controllers/main/service/widgets/create/step2_controller');


describe('App.WidgetWizardStep2Controller', function () {
  var controller = App.WidgetWizardStep2Controller.create({
    content: Em.Object.create()
  });

  describe("#isEditWidget", function() {
    it("empty name", function() {
      controller.set('content.controllerName', '');
      controller.propertyDidChange('isEditWidget');
      expect(controller.get('isEditWidget')).to.be.false;
    });
    it("correct name", function() {
      controller.set('content.controllerName', 'widgetEditController');
      controller.propertyDidChange('isEditWidget');
      expect(controller.get('isEditWidget')).to.be.true;
    });
  });

  describe("#filteredMetrics", function() {
    var testCases = [
      {
        metric: {
          point_in_time: false
        },
        type: null,
        result: []
      },
      {
        metric: {
          point_in_time: true
        },
        type: null,
        result: [
          {
            point_in_time: true
          }
        ]
      },
      {
        metric: {
          temporal: false
        },
        type: 'GRAPH',
        result: []
      },
      {
        metric: {
          temporal: true
        },
        type: 'GRAPH',
        result: [
          {
            temporal: true
          }
        ]
      }
    ];

    testCases.forEach(function (test) {
      it("type=" + test.type + "; temporal=" + test.metric.temporal + "; point_in_time=" + test.metric.point_in_time, function () {
        controller.get('content').setProperties({
          widgetType: test.type,
          allMetrics: [test.metric]
        });
        controller.propertyDidChange('filteredMetrics');
        expect(controller.get('filteredMetrics')).to.eql(test.result);
      });
    });
  });

  describe("#isSubmitDisabled", function () {
    beforeEach(function () {
      this.expressionFunc = sinon.stub(controller, 'isExpressionComplete');
      this.graphFunc = sinon.stub(controller, 'isGraphDataComplete');
      this.templateFunc = sinon.stub(controller, 'isTemplateDataComplete');
      controller.set('expressions', ['']);
    });
    afterEach(function () {
      this.expressionFunc.restore();
      this.graphFunc.restore();
      this.templateFunc.restore();
      controller.get('expressions').clear();
    });
    it("invalid property", function () {
      controller.set('widgetPropertiesViews', [Em.Object.create({isValid: false})]);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("valid number widget", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', 'NUMBER');
      this.expressionFunc.returns(true);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
    it("invalid number widget", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', 'NUMBER');
      this.expressionFunc.returns(false);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("valid graph widget", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', 'GRAPH');
      this.graphFunc.returns(true);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
    it("invalid graph widget", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', 'GRAPH');
      this.graphFunc.returns(false);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("valid template widget", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', 'TEMPLATE');
      this.templateFunc.returns(true);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
    it("invalid template widget", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', 'TEMPLATE');
      this.templateFunc.returns(false);
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
    it("unknown widget type", function () {
      controller.set('widgetPropertiesViews', []);
      controller.set('content.widgetType', '');
      controller.propertyDidChange('isSubmitDisabled');
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
  });

  describe("#isExpressionComplete()", function() {
    var testCases = [
      {
        expression: null,
        result: false
      },
      {
        expression: Em.Object.create({isInvalid: true}),
        result: false
      },
      {
        expression: Em.Object.create({isInvalid: false, isEmpty: false}),
        result: true
      },
      {
        expression: Em.Object.create({isInvalid: false, isEmpty: true}),
        result: false
      }
    ];
    testCases.forEach(function (test) {
      it("expression = " + test.expression, function () {
        expect(controller.isExpressionComplete(test.expression)).to.equal(test.result);
      });
    });
  });

  describe("#isGraphDataComplete()", function() {
    beforeEach(function () {
      this.mock = sinon.stub(controller, 'isExpressionComplete');
    });
    afterEach(function () {
      this.mock.restore();
    });
    it("dataSets is empty", function() {
      expect(controller.isGraphDataComplete([])).to.be.false;
    });
    it("label is empty", function() {
      expect(controller.isGraphDataComplete([Em.Object.create({label: ''})])).to.be.false;
    });
    it("expression is not complete", function() {
      this.mock.returns(false);
      expect(controller.isGraphDataComplete([Em.Object.create({label: 'abc'})])).to.be.false;
    });
    it("expression is complete", function() {
      this.mock.returns(true);
      expect(controller.isGraphDataComplete([Em.Object.create({label: 'abc'})])).to.be.true;
    });
  });

  describe("#isTemplateDataComplete()", function() {
    beforeEach(function () {
      this.mock = sinon.stub(controller, 'isExpressionComplete');
    });
    afterEach(function () {
      this.mock.restore();
    });
    it("expressions is empty", function() {
      expect(controller.isTemplateDataComplete([])).to.be.false;
    });
    it("templateValue is empty", function() {
      expect(controller.isTemplateDataComplete([{}], '')).to.be.false;
    });
    it("expression is not complete", function() {
      this.mock.returns(false);
      expect(controller.isTemplateDataComplete([{}], 'abc')).to.be.false;
    });
    it("expression is complete", function() {
      this.mock.returns(true);
      expect(controller.isTemplateDataComplete([{}], 'abc')).to.be.true;
    });
  });

  describe("#addDataSet()", function() {
    it("", function() {
      controller.get('dataSets').clear();
      controller.addDataSet(null, true);
      expect(controller.get('dataSets').objectAt(0).get('id')).to.equal(1);
      expect(controller.get('dataSets').objectAt(0).get('isRemovable')).to.equal(false);
      controller.addDataSet(null);
      expect(controller.get('dataSets').objectAt(1).get('id')).to.equal(2);
      expect(controller.get('dataSets').objectAt(1).get('isRemovable')).to.equal(true);
      controller.get('dataSets').clear();
    });
  });

  describe("#removeDataSet()", function () {
    it("", function () {
      var dataSet = Em.Object.create();
      controller.get('dataSets').pushObject(dataSet);
      controller.removeDataSet({context: dataSet});
      expect(controller.get('dataSets')).to.be.empty;
    });
  });

  describe("#addExpression()", function() {
    it("", function() {
      controller.get('expressions').clear();
      controller.addExpression(null, true);
      expect(controller.get('expressions').objectAt(0).get('id')).to.equal(1);
      expect(controller.get('expressions').objectAt(0).get('isRemovable')).to.equal(false);
      controller.addExpression(null);
      expect(controller.get('expressions').objectAt(1).get('id')).to.equal(2);
      expect(controller.get('expressions').objectAt(1).get('isRemovable')).to.equal(true);
      controller.get('expressions').clear();
    });
  });

  describe("#removeExpression()", function () {
    it("", function () {
      var expression = Em.Object.create();
      controller.get('expressions').pushObject(expression);
      controller.removeExpression({context: expression});
      expect(controller.get('expressions')).to.be.empty;
    });
  });

  describe("#initWidgetData()", function() {
    it("new data", function() {
      controller.set('expressions', []);
      controller.set('dataSets', []);
      controller.get('content').setProperties({
        widgetProperties: {a:1},
        widgetValues: [1],
        widgetMetrics: [2]
      });

      controller.initWidgetData();

      expect(controller.get('widgetProperties')).to.eql({a:1});
      expect(controller.get('widgetValues')).to.eql([]);
      expect(controller.get('widgetMetrics')).to.eql([]);
      expect(controller.get('expressions')).to.not.be.empty;
      expect(controller.get('dataSets')).to.not.be.empty;
    });
    it("previously edited", function() {
      controller.set('expressions', [{}]);
      controller.set('dataSets', [{}]);
      controller.get('content').setProperties({
        widgetProperties: {a:1},
        widgetValues: [1],
        widgetMetrics: [2]
      });

      controller.initWidgetData();

      expect(controller.get('widgetProperties')).to.eql({a:1});
      expect(controller.get('widgetValues')).to.eql([1]);
      expect(controller.get('widgetMetrics')).to.eql([2]);
      expect(controller.get('expressions')).to.not.be.empty;
      expect(controller.get('dataSets')).to.not.be.empty;
    });
  });

});
