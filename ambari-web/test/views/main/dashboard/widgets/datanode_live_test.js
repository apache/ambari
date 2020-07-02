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
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widgets/datanode_live');

var view;

function testCounterOrNa(propertyName, dependentKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        model: Em.Object.create({
          metricsNotAvailable: true
        })
      });
      view.get('model').set(dependentKey, []);
    });

    it('n/a (1)', function () {
      view.get('model').set(dependentKey, []);
      view.get('model').set('metricsNotAvailable', true);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('n/a (2)', function () {
      view.get('model').set(dependentKey, null);
      view.get('model').set('metricsNotAvailable', true);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('n/a (3)', function () {
      view.get('model').set(dependentKey, null);
      view.get('model').set('metricsNotAvailable', false);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('value exist', function () {
      view.get('model').set(dependentKey, [{}]);
      view.get('model').set('metricsNotAvailable', false);
      expect(view.get(propertyName)).to.be.equal(1);
    });

  });
}

describe('App.DataNodeUpView', function() {

  beforeEach(function () {
    view = App.DataNodeUpView.create({
      model: Em.Object.create()
    });
  });

  testCounterOrNa('dataNodesLive', 'liveDataNodes');

  testCounterOrNa('dataNodesDead', 'deadDataNodes');

  testCounterOrNa('dataNodesDecom', 'decommissionDataNodes');

  describe('#hiddenInfo()', function () {

    it('should return not available statuses', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.deadDataNodes', [1,2]);
      view.set('model.decommissionDataNodes', [1]);
      view.set('model.metricsNotAvailable', true);
      expect(view.get('hiddenInfo')).to.eql(
        [
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead'),
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.decom')
        ]
      );
    });

    it('should return nodes statuses', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.deadDataNodes', [1,2]);
      view.set('model.decommissionDataNodes', [1]);
      view.set('model.metricsNotAvailable', false);
      expect(view.get('hiddenInfo')).to.eql(
        [
          3 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
          2 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead'),
          1 + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.decom')
        ]
      );
    });
  });

  describe('#data()', function () {

    it('should return null', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.metricsNotAvailable', true);
      expect(view.get('data')).to.equal(null);
    });

    it('should return string data', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.dataNodesTotal', 5);
      view.set('model.metricsNotAvailable', false);
      expect(view.get('data')).to.equal(60);
    });
  });

  describe('#content()', function () {

    it('should return n/a', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.metricsNotAvailable', true);
      expect(view.get('content')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return string content', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.dataNodesTotal', 5);
      view.set('model.metricsNotAvailable', false);
      expect(view.get('content')).to.equal('3/5');
    });
  });

  describe('#someMetricsNA()', function () {

    it('should return true', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.metricsNotAvailable', true);
      expect(view.get('someMetricsNA')).to.be.true;
    });

    it('should return false', function () {
      view.set('model.liveDataNodes', [1,2,3]);
      view.set('model.dataNodesTotal', 5);
      view.set('model.metricsNotAvailable', false);
      expect(view.get('someMetricsNA')).to.be.false;
    });
  });

  describe('#hintInfo()', function () {

    it('should return formatted value', function () {
      view.set('maxValue', 150);
      expect(view.get('hintInfo')).to.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint1').format('150'));
    });
  });

});
