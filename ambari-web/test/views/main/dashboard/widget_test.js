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

  var dashboardWidgetView = App.DashboardWidgetView.create({
    parentView: Em.Object.create({
      widgetsMapper: Em.K,
      getUserPref: function () {return {complete: Em.K}},
      postUserPref: Em.K,
      translateToReal: Em.K,
      visibleWidgets: [],
      hiddenWidgets: []
    })
  });

  describe('#viewID', function () {
    it('viewID is computed with id', function () {
      dashboardWidgetView.set('id', 5);
      expect(dashboardWidgetView.get('viewID')).to.equal('widget-5');
    });
  });

  describe('#model', function () {
    it('model_type is null', function () {
      dashboardWidgetView.set('model_type', null);
      dashboardWidgetView.propertyDidChange('model');
      expect(dashboardWidgetView.get('model')).to.eql({});
    });
    it('model_type is valid', function () {
      dashboardWidgetView.set('model_type', 's');
      dashboardWidgetView.propertyDidChange('model');
      dashboardWidgetView.set('parentView.s_model', {'s': {}});
      expect(dashboardWidgetView.get('model')).to.eql({'s': {}});
    });
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.stub(App, 'tooltip', Em.K);
    });
    after(function () {
      App.tooltip.restore();
    });
    it("call App.tooltip", function () {
      dashboardWidgetView.didInsertElement();
      expect(App.tooltip.calledOnce).to.be.true;
    });
  });

  describe("#deleteWidget()", function () {
    beforeEach(function () {
      sinon.stub(dashboardWidgetView.get('parentView'), 'widgetsMapper').returns({});
      sinon.stub(dashboardWidgetView.get('parentView'), 'getUserPref').returns({
        complete: Em.K
      });
    });

    afterEach(function () {
      dashboardWidgetView.get('parentView').widgetsMapper.restore();
      dashboardWidgetView.get('parentView').getUserPref.restore();
    });

    it("testMode is off", function () {
      dashboardWidgetView.set('parentView.persistKey', 'key');
      dashboardWidgetView.deleteWidget();
      expect(dashboardWidgetView.get('parentView').getUserPref.calledWith('key')).to.be.true;
    });
  });

  describe("#deleteWidgetComplete()", function () {
    beforeEach(function () {
      sinon.spy(dashboardWidgetView.get('parentView'), 'postUserPref');
      sinon.spy(dashboardWidgetView.get('parentView'), 'translateToReal');
      dashboardWidgetView.set('parentView.currentPrefObject', {
        dashboardVersion: 'new',
        visible: ['1', '2'],
        hidden: [],
        threshold: 'threshold'
      });
      dashboardWidgetView.set('parentView.persistKey', 'key');
      dashboardWidgetView.deleteWidgetComplete();
    });
    afterEach(function () {
      dashboardWidgetView.get('parentView').postUserPref.restore();
      dashboardWidgetView.get('parentView').translateToReal.restore();
    });
    it("postUserPref is called with correct data", function () {
      var arg = JSON.parse(JSON.stringify(dashboardWidgetView.get('parentView').postUserPref.args[0][1]));
      expect(arg).to.be.eql({
        dashboardVersion: 'new',
        visible: ['1', '2'],
        hidden: [[5, null]],
        threshold: 'threshold'
      });
    });
    it("translateToReal is called with valid data", function () {
      var arg = JSON.parse(JSON.stringify(dashboardWidgetView.get('parentView').translateToReal.args[0][0]));
      expect(arg).to.be.eql({
        dashboardVersion: 'new',
        visible: ['1', '2'],
        hidden: [[5, null]],
        threshold: 'threshold'
      });
    });
  });

  describe("#editWidget()", function () {
    before(function () {
      sinon.stub(dashboardWidgetView, 'showEditDialog', Em.K);
    });
    after(function () {
      dashboardWidgetView.showEditDialog.restore();
    });
    it("call showEditDialog", function () {
      dashboardWidgetView.editWidget();
      expect(dashboardWidgetView.showEditDialog.calledOnce).to.be.true;
    });
  });

  describe("#showEditDialog()", function () {
    var obj = Em.Object.create({
      observeThresh1Value: Em.K,
      observeThresh2Value: Em.K,
      thresh1: '1',
      thresh2: '2'
    });
    beforeEach(function () {
      sinon.spy(obj, 'observeThresh1Value');
      sinon.spy(obj, 'observeThresh2Value');
      sinon.stub(dashboardWidgetView.get('parentView'), 'getUserPref').returns({
        complete: Em.K
      });
      var popup = dashboardWidgetView.showEditDialog(obj);
      popup.onPrimary();
    });
    afterEach(function () {
      obj.observeThresh1Value.restore();
      obj.observeThresh2Value.restore();
      dashboardWidgetView.get('parentView').getUserPref.restore();
    });

    it("observeThresh1Value is called once", function () {
      expect(obj.observeThresh1Value.calledOnce).to.be.true;
    });

    it("observeThresh2Value is called once", function () {
      expect(obj.observeThresh2Value.calledOnce).to.be.true;
    });

    it("thresh1 = 1", function () {
      expect(dashboardWidgetView.get('thresh1')).to.equal(1);
    });

    it("thresh2 = 2", function () {
      expect(dashboardWidgetView.get('thresh2')).to.equal(2);
    });

    it("getUserPref is called once", function () {
      expect(dashboardWidgetView.get('parentView').getUserPref.calledOnce).to.be.true;
    });
  });

  describe('#model', function () {
    it('model_type is null', function () {
      dashboardWidgetView.set('model_type', null);
      dashboardWidgetView.propertyDidChange('model');
      expect(dashboardWidgetView.get('model')).to.eql({});
    });
    it('model_type is valid', function () {
      dashboardWidgetView.set('model_type', 's');
      dashboardWidgetView.propertyDidChange('model');
      dashboardWidgetView.set('parentView.s_model', {'s': {}});
      expect(dashboardWidgetView.get('model')).to.eql({'s': {}});
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
            thresh1: '',
            maxValue: 0
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('admin.users.editError.requiredField')
          }
        },
        {
          data: {
            thresh1: 'NaN',
            maxValue: 0
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresh1: '-1',
            maxValue: 0
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresh1: '2',
            maxValue: 1
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.invalid').format(1)
          }
        },
        {
          data: {
            thresh1: '1',
            thresh2: '1',
            maxValue: 2
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.smaller')
          }
        },
        {
          data: {
            thresh1: '1',
            thresh2: '0',
            maxValue: 2
          },
          result: {
            isThresh1Error: true,
            errorMessage1: Em.I18n.t('dashboard.widgets.error.smaller')
          }
        },
        {
          data: {
            thresh1: '1',
            thresh2: '2',
            maxValue: 2
          },
          result: {
            isThresh1Error: false,
            errorMessage1: ''
          }
        }
      ];
      testCases.forEach(function (test) {
        describe("thresh1 - " + test.data.thresh1 + ', maxValue - ' + test.data.maxValue, function () {

          beforeEach(function () {
            widget.set('isThresh2Error', false);
            widget.set('thresh2', test.data.thresh2 || "");
            widget.set('thresh1', test.data.thresh1);
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
            thresh2: '',
            maxValue: 0
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('admin.users.editError.requiredField')
          }
        },
        {
          data: {
            thresh2: 'NaN',
            maxValue: 0
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresh2: '-1',
            maxValue: 0
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('dashboard.widgets.error.invalid').format(0)
          }
        },
        {
          data: {
            thresh2: '2',
            maxValue: 1
          },
          result: {
            isThresh2Error: true,
            errorMessage2: Em.I18n.t('dashboard.widgets.error.invalid').format(1)
          }
        },
        {
          data: {
            thresh2: '2',
            maxValue: 2
          },
          result: {
            isThresh2Error: false,
            errorMessage2: ''
          }
        }
      ];
      testCases.forEach(function (test) {
        describe("thresh2 - " + test.data.thresh2 + ', maxValue - ' + test.data.maxValue, function () {

          beforeEach(function () {
            widget.set('thresh2', test.data.thresh2 || "");
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
