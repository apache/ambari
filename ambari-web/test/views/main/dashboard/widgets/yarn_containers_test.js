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

require('views/main/dashboard/widgets/yarn_containers');

function getView() {
  return App.YarnContainersView.create({
    model: Em.Object.create()
  });
}

describe('App.YarnContainersView', function() {

  var view;

  beforeEach(function () {
    view = getView();
  });

  describe('#hiddenInfo()', function () {

    it('should return not available statuses', function () {
      view.set('model.containersAllocated', [1,2,3]);
      view.set('model.containersPending', [1,2]);
      view.set('model.containersReserved', [1]);
      view.set('model.metricsNotAvailable', true);
      expect(view.get('hiddenInfo')).to.eql(
        [
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.yarn.containers.allocated'),
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.yarn.containers.pending'),
          Em.I18n.t('services.service.summary.notAvailable') + ' ' + Em.I18n.t('dashboard.services.yarn.containers.reserved')
        ]
      );
    });

    it('should return container statuses', function () {
      view.set('model.containersAllocated', [1,2,3]);
      view.set('model.containersPending', [1,2]);
      view.set('model.containersReserved', [1]);
      expect(view.get('hiddenInfo')).to.eql(
        [
          3 + ' ' + Em.I18n.t('dashboard.services.yarn.containers.allocated'),
          2 + ' ' + Em.I18n.t('dashboard.services.yarn.containers.pending'),
          1 + ' ' + Em.I18n.t('dashboard.services.yarn.containers.reserved')
        ]
      );
    });
  });

  describe('#content()', function () {

    it('should return string content', function () {
      view.set('model.containersAllocated', [1,2,3]);
      view.set('model.containersPending', [1,2]);
      view.set('model.containersReserved', [1]);
      expect(view.get('content')).to.equal('3/2/1');
    });
  });

  describe('#someMetricsNA()', function () {

    it('should return true', function () {
      view.set('model.containersAllocated', [1,2,3]);
      view.set('model.containersPending', [1, 2]);
      expect(view.get('someMetricsNA')).to.be.false;
    });

    it('should return false', function () {
      view.set('model.containersAllocated', [1,2,3]);
      view.set('model.containersPending', [1, 2]);
      view.set('model.containersReserved', [1]);
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
