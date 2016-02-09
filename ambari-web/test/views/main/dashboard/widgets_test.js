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
require('messages');
var filters = require('views/common/filter_view');
require('mixins/common/userPref');
require('mixins/common/localStorage');
require('views/main/dashboard/widgets');

describe('App.MainDashboardWidgetsView', function () {

  var view = App.MainDashboardWidgetsView.create();

  describe('#setInitPrefObject', function () {
    var host_metrics_widgets_count = 4;
    var hdfs_widgets_count = 7;
    var hbase_widgets_count = 4;
    var yarn_widgets_count = 4;
    var total_widgets_count = 20;
    var tests = Em.A([
      {
        models: {
          host_metrics_model: null,
          hdfs_model: null,
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - host_metrics_widgets_count - hdfs_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'All models are null'
      },
      {
        models: {
          host_metrics_model: {},
          hdfs_model: null,
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - hdfs_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'hdfs_model, hbase_model, yarn_model are null'
      },
      {
        models: {
          host_metrics_model: {},
          hdfs_model: {},
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'hbase_model, yarn_model are null'
      },
      {
        models: {
          host_metrics_model: {},
          hdfs_model: {},
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'hbase_model and yarn_model are null'
      },
      {
        models: {
          host_metrics_model: {},
          hdfs_model: {},
          hbase_model: {},
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - yarn_widgets_count - 1,
          hiddenL: 1
        },
        m: 'yarn_model is null'
      },
      {
        models: {
          host_metrics_model: {},
          hdfs_model: {},
          hbase_model: {},
          yarn_model: {}
        },
        e: {
          visibleL: total_widgets_count,
          hiddenL: 1
        },
        m: 'All models are not null'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        view.set('host_metrics_model', test.models.host_metrics_model);
        view.set('hdfs_model', test.models.hdfs_model);
        view.set('hbase_model', test.models.hbase_model);
        view.set('yarn_model', test.models.yarn_model);
        view.setInitPrefObject();
        expect(view.get('initPrefObject.visible.length')).to.equal(test.e.visibleL);
        expect(view.get('initPrefObject.hidden.length')).to.equal(test.e.hiddenL);
      });
    });
  });

  describe('#persistKey', function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get', function (k) {
        if ('loginName' === k) return 'tdk';
        return Em.get(App.router, k);
      });
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it('Check it', function () {
      expect(view.get('persistKey')).to.equal('user-pref-tdk-dashboard');
    });
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.stub(view, 'setWidgetsDataModel', Em.K);
      sinon.stub(view, 'setInitPrefObject', Em.K);
      sinon.stub(view, 'setOnLoadVisibleWidgets', Em.K);
      sinon.stub(Em.run, 'next', Em.K);
    });
    after(function () {
      view.setWidgetsDataModel.restore();
      view.setInitPrefObject.restore();
      view.setOnLoadVisibleWidgets.restore();
      Em.run.next.restore();
    });
    it("", function () {
      view.didInsertElement();
      expect(view.setWidgetsDataModel.calledOnce).to.be.true;
      expect(view.setInitPrefObject.calledOnce).to.be.true;
      expect(view.setOnLoadVisibleWidgets.calledOnce).to.be.true;
      expect(Em.run.next.calledWith(view, 'makeSortable')).to.be.true;
      expect(view.get('isDataLoaded')).to.be.true
    });
  });

  describe("#setWidgetsDataModel()", function () {
    beforeEach(function () {
      this.model = sinon.stub(App.Service, 'find');
      this.get = sinon.stub(App, 'get');
    });
    afterEach(function () {
      this.model.restore();
      this.get.restore();
    });
    it("No host_metrics_model", function () {
      this.get.returns([]);
      this.model.returns([Em.Object.create({
        serviceName: 'S1',
        id: 'S1'
      })]);
      view.set('host_metrics_model', null);
      view.setWidgetsDataModel();
      expect(view.get('host_metrics_model')).to.be.null;
      expect(view.get('s1_model')).to.eql(Em.Object.create({
        serviceName: 'S1',
        id: 'S1'
      }));
    });
    it("host_metrics_model is present", function () {
      this.get.returns([1]);
      this.model.returns([Em.Object.create({
        serviceName: 'HDFS',
        id: 'HDFS'
      })]);
      view.set('host_metrics_model', null);
      view.setWidgetsDataModel();
      expect(view.get('host_metrics_model')).to.eql([1]);
      expect(view.get('hdfs_model.id')).to.equal('HDFS');
    });
  });

  describe("#plusButtonFilterView", function () {
    var plusButtonFilterView = view.get('plusButtonFilterView').create({
      parentView: view
    });
    plusButtonFilterView.reopen({
      visibleWidgets: [],
      hiddenWidgets: []
    });

    describe("#applyFilter()", function () {
      var widget = {checked: true};
      beforeEach(function () {
        sinon.stub(view, 'getUserPref').returns({
          complete: Em.K
        });
        sinon.stub(view, 'widgetsMapper').returns(widget);
      });
      afterEach(function () {
        view.getUserPref.restore();
        view.widgetsMapper.restore();
      });
      it("testMode is on", function () {
        App.set('testMode', true);
        plusButtonFilterView.set('hiddenWidgets', [widget]);
        plusButtonFilterView.applyFilter();
        expect(view.getUserPref.called).to.be.false;
        expect(plusButtonFilterView.get('visibleWidgets')).not.to.be.empty;
        expect(plusButtonFilterView.get('hiddenWidgets')).to.be.empty;
      });
      it("testMode is off", function () {
        App.set('testMode', false);
        plusButtonFilterView.applyFilter();
        expect(view.getUserPref.calledOnce).to.be.true;
      });
    });

    describe("#applyFilterComplete()", function () {
      var widget = {checked: true};
      beforeEach(function () {
        sinon.stub(view, 'postUserPref');
        sinon.stub(view, 'translateToReal');
      });
      afterEach(function () {
        view.postUserPref.restore();
        view.translateToReal.restore();
      });
      beforeEach(function () {
        sinon.stub(App.router, 'get', function (k) {
          if ('loginName' === k) return 'tdk';
          return Em.get(App.router, k);
        });
      });
      afterEach(function () {
        App.router.get.restore();
      });
      it("", function () {
        plusButtonFilterView.set('hiddenWidgets', [
          Em.Object.create({
            checked: true,
            id: 1,
            displayName: 'i1'
          }),
          Em.Object.create({
            checked: false,
            id: 2,
            displayName: 'i2'
          })
        ]);
        view.set('currentPrefObject', Em.Object.create({
          dashboardVersion: 'new',
          visible: [],
          hidden: [],
          threshold: 'threshold'
        }));
        view.set('persistKey', 'key');
        plusButtonFilterView.applyFilterComplete();
        expect(view.postUserPref.calledOnce).to.be.true;
        expect(view.translateToReal.getCall(0).args[0]).to.eql(Em.Object.create({
          dashboardVersion: 'new',
          visible: [1],
          hidden: [
            [2, 'i2']
          ],
          threshold: 'threshold'
        }));
        expect(plusButtonFilterView.get('hiddenWidgets.length')).to.equal(1);
      });
    });
  });

  describe("#translateToReal()", function () {
    beforeEach(function () {
      sinon.stub(view, 'widgetsMapper').returns(Em.Object.create());
      view.set('visibleWidgets', []);
      view.set('hiddenWidgets', []);
    });
    afterEach(function () {
      view.widgetsMapper.restore();
    });
    it("version is not new", function () {
      var data = {
        dashboardVersion: null,
        visible: [],
        hidden: [],
        threshold: []
      };
      view.translateToReal(data);
      expect(view.get('visibleWidgets')).to.be.empty;
      expect(view.get('hiddenWidgets')).to.be.empty;
    });
    it("version is new", function () {
      var data = {
        dashboardVersion: 'new',
        visible: [1],
        hidden: [
          ['id', 'title']
        ],
        threshold: [
          [],
          [
            ['tresh1'],
            ['tresh2']
          ]
        ]
      };
      view.translateToReal(data);
      expect(view.get('visibleWidgets')).to.not.be.empty;
      expect(view.get('hiddenWidgets')).to.not.be.empty;
    });
  });

  describe("#setOnLoadVisibleWidgets()", function () {
    beforeEach(function () {
      sinon.stub(view, 'translateToReal', Em.K);
      sinon.stub(view, 'getUserPref').returns({complete: Em.K});
    });
    afterEach(function () {
      view.translateToReal.restore();
      view.getUserPref.restore();
    });

    it("testMode is true", function () {
      App.set('testMode', true);
      view.setOnLoadVisibleWidgets();
      expect(view.translateToReal.calledOnce).to.be.true;
    });
    it("testMode is false", function () {
      App.set('testMode', false);
      view.setOnLoadVisibleWidgets();
      expect(view.getUserPref.calledOnce).to.be.true;
    });
  });

  describe("#removeWidget()", function () {
    it("", function () {
      var widget = {};
      var value = {
        visible: [widget],
        hidden: [
          [widget]
        ]
      };
      value = view.removeWidget(value, widget);
      expect(value.visible).to.be.empty;
      expect(value.hidden).to.be.empty;
    });
  });

  describe("#containsWidget()", function () {
    it("widget visible", function () {
      var widget = {};
      var value = {
        visible: [widget],
        hidden: [
          [widget]
        ]
      };
      expect(view.containsWidget(value, widget)).to.be.true;
    });
    it("widget absent", function () {
      var widget = {};
      var value = {
        visible: [],
        hidden: []
      };
      expect(view.containsWidget(value, widget)).to.be.false;
    });
    it("widget hidden", function () {
      var widget = {};
      var value = {
        visible: [],
        hidden: [
          [widget]
        ]
      };
      expect(view.containsWidget(value, widget)).to.be.true;
    });
  });

  describe("#persistKey", function () {
    before(function () {
      sinon.stub(App.router, 'get').withArgs('loginName').returns('user');
    });
    after(function () {
      App.router.get.restore();
    });
    it("", function () {
      view.propertyDidChange('persistKey');
      expect(view.get('persistKey')).to.equal('user-pref-user-dashboard');
    });
  });

  describe("#getUserPrefSuccessCallback()", function () {

    it("response is null", function () {
      view.set('currentPrefObject', null);
      view.getUserPrefSuccessCallback(null, {}, {});
      expect(view.get('currentPrefObject')).to.be.null;
    });

    it("response is correct", function () {
      view.set('currentPrefObject', null);
      view.getUserPrefSuccessCallback({}, {}, {});
      expect(view.get('currentPrefObject')).to.eql({});
    });

    it('should update missing thresholds', function () {

      view.set('currentPrefObject', null);
      view.getUserPrefSuccessCallback({
        threshold: {
          17: []
        }
      }, {}, {});
      expect(view.get('currentPrefObject.threshold')['17']).to.eql([70, 90]);

    });

  });

  describe("#resetAllWidgets()", function () {

    var cases = [
      {
        testMode: true,
        postUserPrefCallCount: 0,
        setDBPropertyCallCount: 0,
        title: 'test mode'
      },
      {
        testMode: false,
        postUserPrefCallCount: 1,
        setDBPropertyCallCount: 1,
        title: 'real cluster'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(App, 'showConfirmationPopup', function (callback) {
            callback();
          });
          sinon.stub(App, 'get').withArgs('testMode').returns(item.testMode);
          sinon.stub(view, 'postUserPref', Em.K);
          sinon.stub(view, 'setDBProperty', Em.K);
          sinon.stub(view, 'translateToReal', Em.K);
          view.setProperties({
            currentTimeRangeIndex: 1,
            customStartTime: 1000,
            customEndTime: 2000
          });
          view.resetAllWidgets();
        });

        afterEach(function () {
          App.get.restore();
          App.showConfirmationPopup.restore();
          view.postUserPref.restore();
          view.setDBProperty.restore();
          view.translateToReal.restore();
        });

        it('persist reset', function () {
          expect(view.postUserPref.callCount).to.equal(item.postUserPrefCallCount);
        });
        it('local storage reset', function () {
          expect(view.setDBProperty.callCount).to.equal(item.setDBPropertyCallCount);
        });
        it('time range reset', function () {
          expect(view.get('currentTimeRangeIndex')).to.equal(0);
        });
        it('custom start time reset', function () {
          expect(view.get('customStartTime')).to.be.null;
        });
        it('custom end time reset', function () {
          expect(view.get('customEndTime')).to.be.null;
        });
        it('default settings application', function () {
          expect(view.translateToReal.callCount).to.equal(1);
        });

      });

    });
  });

  describe('#checkServicesChange', function () {

    var emptyCurrentPref = {
        visible: [],
        hidden: [],
        threshold: {}
      },
      widgetsMap = {
        hdfs_model: ['1', '2', '3', '4', '5', '10', '11'],
        host_metrics_model: ['6', '7', '8', '9'],
        hbase_model: ['12', '13', '14', '15', '16'],
        yarn_model: ['17', '18', '19', '20', '23'],
        storm_model: ['21'],
        flume_model: ['22']
      },
      emptyModelTitle = '{0} absent',
      notEmptyModelTitle = '{0} present';

    Em.keys(widgetsMap).forEach(function (item, index, array) {
      it(notEmptyModelTitle.format(item), function () {
        array.forEach(function (modelName) {
          view.set(modelName, modelName == item ? {} : null);
        });
        expect(view.checkServicesChange(emptyCurrentPref).visible).to.eql(widgetsMap[item]);
      });
    });

    Em.keys(widgetsMap).forEach(function (item, index, array) {
      it(emptyModelTitle.format(item), function () {
        var expected = [];
        array.forEach(function (modelName) {
          if (modelName == item) {
            view.set(modelName, null);
          } else {
            view.set(modelName, {});
            expected = expected.concat(widgetsMap[modelName]);
          }
        });
        expect(view.checkServicesChange({
          visible: widgetsMap[item],
          hidden: [],
          threshold: {}
        }).visible).to.eql(expected);
      });
    });

  });
});
