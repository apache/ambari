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
require('views/main/dashboard/widget');

describe('App.DashboardWidgetView', function () {

  var dashboardWidgetView;

  dashboardWidgetView = App.DashboardWidgetView.create({
    parentView: Em.Object.create({
      widgetsMapper: Em.K,
      getUserPref: function () {
        return {complete: Em.K}
      },
      postUserPref: Em.K,
      translateToReal: Em.K,
      visibleWidgets: [],
      hiddenWidgets: []
    }),
    widget: Em.Object.create({
      id: 5,
      sourceName: 'HDFS',
      title: 'Widget'
    })
  });

  describe('#viewID', function () {
    it('viewID is computed with id', function () {
      expect(dashboardWidgetView.get('viewID')).to.equal('widget-5');
    });
  });

  describe('#model', function () {

    beforeEach(function() {
      sinon.stub(dashboardWidgetView, 'findModelBySource').returns(Em.Object.create({serviceName: 'HDFS'}));
    });

    afterEach(function() {
      dashboardWidgetView.findModelBySource.restore();
    });

    it('sourceName is null', function () {
      dashboardWidgetView.set('widget.sourceName', null);
      dashboardWidgetView.propertyDidChange('model');
      expect(dashboardWidgetView.get('model')).to.be.an.object;
    });
    it('sourceName is valid', function () {
      dashboardWidgetView.set('widget.sourceName', 'HDFS');
      dashboardWidgetView.propertyDidChange('model');
      expect(dashboardWidgetView.get('model')).to.eql(Em.Object.create({serviceName: 'HDFS'}));
    });
  });

  describe("#didInsertElement()", function () {

    beforeEach(function () {
      sinon.stub(App, 'tooltip', Em.K);
    });
    afterEach(function () {
      App.tooltip.restore();
    });

    it("call App.tooltip", function () {
      dashboardWidgetView.didInsertElement();
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#editWidget()", function () {

    beforeEach(function () {
      sinon.stub(dashboardWidgetView, 'showEditDialog', Em.K);
    });
    afterEach(function () {
      dashboardWidgetView.showEditDialog.restore();
    });

    it("call showEditDialog", function () {
      dashboardWidgetView.editWidget();
      expect(dashboardWidgetView.showEditDialog.calledOnce).to.be.true;
    });
  });

  describe('#hoverContentTopClass', function () {
    var tests = [
      {
        h: ['', ''],
        e: 'content-hidden-two-line',
        m: '2 lines'
      },
      {
        h: ['', '', ''],
        e: 'content-hidden-three-line',
        m: '3 lines'
      },
      {
        h: [''],
        e: '',
        m: '1 line'
      },
      {
        h: [],
        e: '',
        m: '0 lines'
      },
      {
        h: ['', '', '', '', ''],
        e: 'content-hidden-five-line',
        m: '5 lines'
      },
      {
        h: ['', '', '', ''],
        e: 'content-hidden-four-line',
        m: '4 lines'
      },
      {
        h: ['', '', '', '', '', ''],
        e: 'content-hidden-six-line',
        m: '6 lines'
      }
    ];
    tests.forEach(function (test) {
      it(test.m, function () {
        dashboardWidgetView.set('hiddenInfo', test.h);
        expect(dashboardWidgetView.get('hoverContentTopClass')).to.equal(test.e);
      });
    });
  });

  describe("#widgetConfig", function() {
    var widget = dashboardWidgetView.get('widgetConfig').create();
    describe("#hintInfo", function() {
      it("is formatted with maxValue", function() {
        widget.set('maxValue', 1);
        widget.propertyDidChange('hintInfo');
        expect(widget.get('hintInfo')).to.equal(Em.I18n.t('dashboard.widgets.hintInfo.common').format(1));
      });
    });
    describe("#observeThresh1Value", function() {
      beforeEach(function () {
        sinon.stub(widget, 'updateSlider', Em.K);
      });
      afterEach(function () {
        widget.updateSlider.restore();
      });
      var testCases = [
        {
          data: {
            thresholdMin: '',
            maxValue: 0
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('admin.users.editError.requiredField')
          }
        },
        {
          data: {
            thresholdMin: 'NaN',
            maxValue: 0
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresholdMin: '-1',
            maxValue: 0
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresholdMin: '2',
            maxValue: 1
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.invalid').format(1)
          }
        },
        {
          data: {
            thresholdMin: '1',
            thresholdMax: '1',
            maxValue: 2
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.smaller')
          }
        },
        {
          data: {
            thresholdMin: '1',
            thresholdMax: '0',
            maxValue: 2
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.smaller')
          }
        },
        {
          data: {
            thresholdMin: '1',
            thresholdMax: '2',
            maxValue: 2
          },
          result: {
            isThresh1Error: false,
            errorMessage1: ''
          }
        }
      ];
      testCases.forEach(function (test) {
        describe("thresholdMin - " + test.data.thresholdMin + ', maxValue - ' + test.data.maxValue, function () {

          beforeEach(function () {
            widget.set('isThresh2Error', false);
            widget.set('thresholdMax', test.data.thresholdMax || "");
            widget.set('thresholdMin', test.data.thresholdMin);
            widget.set('maxValue', test.data.maxValue);
            widget.observeThresh1Value();
          });

          it('isThresh1Error is ' + test.result.isThresh1Error, function () {
            expect(widget.get('isThresh1Error')).to.equal(test.result.isThresh1Error);
          });

          it('errorMessage1 is ' + test.result.errorMessage1, function () {
            expect(widget.get('errorMessage1')).to.equal(test.result.errorMessage1);
          });

          it('updateSlider is called', function () {
            expect(widget.updateSlider.called).to.be.true;
          });

        });
      });
    });

    describe("#observeThresh2Value", function() {
      beforeEach(function () {
        sinon.stub(widget, 'updateSlider', Em.K);
      });
      afterEach(function () {
        widget.updateSlider.restore();
      });
      var testCases = [
        {
          data: {
            thresholdMax: '',
            maxValue: 0
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('admin.users.editError.requiredField')
          }
        },
        {
          data: {
            thresholdMax: 'NaN',
            maxValue: 0
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresholdMax: '-1',
            maxValue: 0
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresholdMax: '2',
            maxValue: 1
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('dashboard.widgets.error.invalid').format(1)
          }
        },
        {
          data: {
            thresholdMax: '2',
            maxValue: 2
          },
          result: {
            isThresh2Error: false,
            errorMessage2: ''
          }
        }
      ];
      testCases.forEach(function (test) {
        describe("thresholdMax - " + test.data.thresholdMax + ', maxValue - ' + test.data.maxValue, function () {

          beforeEach(function () {
            widget.set('thresholdMax', test.data.thresholdMax || "");
            widget.set('maxValue', test.data.maxValue);
            widget.observeThresh2Value();
          });

          it('isThresh2Error is ' + test.result.isThresh2Error, function () {
            expect(widget.get('isThresh2Error')).to.equal(test.result.isThresh2Error);
          });

          it('errorMessage2 is ' + JSON.stringify(test.result.errorMessage2), function () {
            expect(widget.get('errorMessage2')).to.equal(test.result.errorMessage2);
          });

          it('updateSlider is called', function () {
            expect(widget.updateSlider.called).to.be.true;
          });
        });
      });
    });
  });

});
