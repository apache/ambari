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
var numberUtils = require('utils/number_utils');

require('views/main/dashboard/widgets/hdfs_capacity');

describe('App.NameNodeCapacityPieChartView', function () {

  var view;

  beforeEach(function () {
    view = App.NameNodeCapacityPieChartView.create({
      model: Em.Object.create()
    });
  });

  describe('#didInsertElement()', function () {

    beforeEach(function () {
      sinon.stub(view, 'calc', Em.K);
    });

    it('should execute calc function', function () {
      view.didInsertElement();
      expect(view.calc.calledOnce).to.be.true;
    });
  });

  describe('#ccalcHiddenInfo()', function () {

    it('should return calculated data with no info', function () {
      expect(view.calcHiddenInfo()).to.eql([
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.DFSused'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(null, 1, 'parseFloat'), 0),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.nonDFSused'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(null, 1, 'parseFloat'), 0),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.remaining'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(null, 1, 'parseFloat'), 0)
      ]);
    });

    it('should return calculated data with not available percents', function () {
      view.set('model.capacityTotal', 100);
      view.set('model.capacityRemaining', -1);
      view.set('model.capacityUsed', -1);
      view.set('model.capacityNonDfsUsed', -1);
      expect(view.calcHiddenInfo()).to.eql([
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.DFSused'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(-1, 1, 'parseFloat'), Em.I18n.t('services.service.summary.notAvailable') + " "),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.nonDFSused'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(-1, 1, 'parseFloat'), Em.I18n.t('services.service.summary.notAvailable') + " "),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.remaining'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(-1, 1, 'parseFloat'), Em.I18n.t('services.service.summary.notAvailable') + " ")
      ]);
    });

    it('should return calculated data', function () {
      view.set('model.capacityTotal', 100);
      view.set('model.capacityRemaining', 50);
      view.set('model.capacityUsed', 100);
      view.set('model.capacityNonDfsUsed', 50);
      expect(view.calcHiddenInfo()).to.eql([
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.DFSused'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(100, 1, 'parseFloat'), '100.00'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.nonDFSused'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(50, 1, 'parseFloat'), '50.00'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.remaining'),
        Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info')
          .format(numberUtils.bytesToSize(50, 1, 'parseFloat'), '50.00')
      ]);
    });
  });

  describe('#calcDataForPieChart()', function () {

    it('should return calculated data [0,0]', function () {
      expect(view.calcDataForPieChart()).to.eql([0, 0]);
    });

    it('should return calculated data', function () {
      view.set('model.capacityTotal', 100);
      view.set('model.capacityRemaining', 50);
      expect(view.calcDataForPieChart()).to.eql(['50', '50.0']);
    });
  });

});
