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
require('views/main/dashboard/widgets/pxf_live');

var view;

function testCounterOrNa(propertyName, dependentKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        model: Em.Object.create()
      });
      view.get('model').set(dependentKey, []);
    });

    it('n/a (1)', function () {
      view.get('model').set(dependentKey, null);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('n/a (2)', function () {
      view.get('model').set(dependentKey, undefined);
      expect(view.get(propertyName)).to.be.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('value exist', function () {
      view.get('model').set(dependentKey, 123);
      expect(view.get(propertyName)).to.be.equal(123);
    });

  });
}

describe('App.PxfUpView', function() {

  beforeEach(function () {
    view = App.PxfUpView.create({
      model: Em.Object.create()
    });
  });

  testCounterOrNa('pxfsStarted', 'pxfsStarted');
  testCounterOrNa('pxfsInstalled', 'pxfsInstalled');
  testCounterOrNa('pxfsTotal', 'pxfsTotal');

  describe('#hiddenInfo()', function () {

    it('should return not available statuses', function () {
      expect(view.get('hiddenInfo')).to.eql(
        [
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.components.started'),
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.components.stopped'),
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.components.total')
        ]
      );
    });

    it('should return components statuses', function () {
      view.set('model.pxfsStarted', 3);
      view.set('model.pxfsInstalled', 1);
      view.set('model.pxfsTotal', 4);
      expect(view.get('hiddenInfo')).to.eql(
        [
          3 + ' ' + Em.I18n.t('dashboard.services.components.started'),
          1 + ' ' + Em.I18n.t('dashboard.services.components.stopped'),
          4 + ' ' + Em.I18n.t('dashboard.services.components.total')
        ]
      );
    });
  });

  describe('#data()', function () {

    it('should return null', function () {
      view.set('model.pxfsStarted', 3);
      expect(view.get('data')).to.equal(null);
    });

    it('should return string data', function () {
      view.set('model.pxfsStarted', 3);
      view.set('model.pxfsTotal', 4);
      expect(view.get('data')).to.equal(1);
    });
  });

  describe('#content()', function () {

    it('should return n/a', function () {
      view.set('model.pxfsStarted', 3);
      expect(view.get('content')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return string content', function () {
      view.set('model.pxfsStarted', 3);
      view.set('model.pxfsTotal', 4);
      expect(view.get('content')).to.equal('3/4');
    });
  });

  describe('#someMetricsNA()', function () {

    it('should return true', function () {
      view.set('model.pxfsStarted', 3);
      expect(view.get('someMetricsNA')).to.be.true;
    });

    it('should return false', function () {
      view.set('model.pxfsStarted', 3);
      view.set('model.pxfsTotal', 4);
      expect(view.get('someMetricsNA')).to.be.false;
    });
  });

  describe('#hintInfo()', function () {

    it('should return formatted value', function () {
      view.set('model.pxfsTotal', 150);
      expect(view.get('hintInfo')).to.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint4').format(Em.I18n.t('dashboard.widgets.PXFAgents'),'150'));
    });
  });

});
