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
require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widgets/node_managers_live');

function getView() {
  return App.NodeManagersLiveView.create({
    parentView: Em.Object.create()
  });
}

describe('App.NodeManagersLiveView', function() {

  App.TestAliases.testAsComputedAnd(getView(), 'isDataAvailable', ['!model.metricsNotAvailable', 'App.router.clusterController.isComponentsStateLoaded']);

  App.TestAliases.testAsComputedAlias(getView(), 'nodeManagersLive', 'model.nodeManagersCountActive');

  var view;

  beforeEach(function () {
    view = getView();
  });

  describe('#hiddenInfo()', function () {

    it('should return not available statuses', function () {
      expect(view.get('hiddenInfo')).to.eql(
        [
          Em.I18n.t('services.service.summary.notAvailable') + " active",
          Em.I18n.t('services.service.summary.notAvailable') + " lost",
          Em.I18n.t('services.service.summary.notAvailable') + " unhealthy",
          Em.I18n.t('services.service.summary.notAvailable') + " rebooted",
          Em.I18n.t('services.service.summary.notAvailable') + " decommissioned"
        ]
      );
    });

    it('should return statuses', function () {
      view.set('model.nodeManagersCountActive', 6);
      view.set('model.nodeManagersCountLost', 3);
      view.set('model.nodeManagersCountUnhealthy', 3);
      view.set('model.nodeManagersCountRebooted', 1);
      view.set('model.nodeManagersCountDecommissioned', 1);
      expect(view.get('hiddenInfo')).to.eql(
        [
          6 + " active",
          3 + " lost",
          3 + " unhealthy",
          1 + " rebooted",
          1 + " decommissioned"
        ]
      );
    });
  });

  describe('#data()', function () {

    it('should return null', function () {
      expect(view.get('data')).to.equal(null);
    });

    it('should return string with data', function () {
      view.set('model.nodeManagersCountActive', 3);
      view.set('model.nodeManagersTotal', 6);
      expect(view.get('data')).to.equal(50);
    });
  });

  describe('#content()', function () {

    it('should return not available', function () {
      expect(view.get('content')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return string content', function () {
      view.set('model.nodeManagersCountActive', 3);
      view.set('model.nodeManagersTotal', 6);
      expect(view.get('content')).to.equal('3/6');
    });
  });

  describe('#hintInfo()', function () {

    it('should return formatted value', function () {
      view.set('maxValue', 150);
      expect(view.get('hintInfo')).to.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint1').format('150'));
    });
  });

});
