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
require('models/rack');
require('controllers/main/charts/heatmap');

describe('MainChartsHeatmapController', function () {

  describe('#validation()', function () {
    var controller = App.MainChartsHeatmapController.create({
      allMetrics: [],
      selectedMetric: Ember.Object.create({maximumValue: 100})
    });
    it('should set maximumValue if inputMaximum consists only of digits', function () {
      controller.set("inputMaximum", 5);
      expect(controller.get('selectedMetric.maximumValue')).to.equal(5);
    });
    it('should not set maximumValue if inputMaximum consists not only of digits', function () {
      controller.set("inputMaximum", 'qwerty');
      expect(controller.get('selectedMetric.maximumValue')).to.equal(5);
    });
    it('should not set maximumValue if inputMaximum consists not only of digits', function () {
      controller.set("inputMaximum", '100%');
      expect(controller.get('selectedMetric.maximumValue')).to.equal(5);
    });
    it('should set maximumValue if inputMaximum consists only of digits', function () {
      controller.set("inputMaximum", 1000);
      expect(controller.get('selectedMetric.maximumValue')).to.equal(1000);
    })
  });

  describe('#showHeatMapMetric()', function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    var controller = App.MainChartsHeatmapController.create({
      activeWidgetLayout: Em.Object.create({
        displayName: 'widget',
        id: '1',
        scope: 'CLUSTER',
        layoutName: 'defualt_layout',
        sectionName: 'default_section'
      })
    });

    it('should call App.ajax', function () {
      controller.showHeatMapMetric({context:{id: 2}});
      expect(App.ajax.send.called).to.be.true;
    });
  });

  describe('#rackClass', function () {
    var controller = App.MainChartsHeatmapController.create({
      allMetrics: [],
      racks: [1]
    });
    it('should return "span12" for 1 cluster rack', function () {
      expect(controller.get('rackClass')).to.equal('span12');
    });
    it('should return "span6" for 2 cluster racks', function () {
      controller.set('racks', [1, 2]);
      expect(controller.get('rackClass')).to.equal('span6');
    });
    it('should return "span4" for 3 cluster racks', function () {
      controller.set('racks', [1, 2, 3]);
      expect(controller.get('rackClass')).to.equal('span4');
    });
  });
});

