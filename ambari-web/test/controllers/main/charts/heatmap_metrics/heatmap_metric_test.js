/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('controllers/main/charts/heatmap_metrics/heatmap_metric');
var date = require('utils/date/date');

describe('MainChartHeatmapMetric', function () {
  var mainChartHeatmapMetric = App.MainChartHeatmapMetric.create({});

  beforeEach(function () {
    mainChartHeatmapMetric = App.MainChartHeatmapMetric.create({});
  });

  describe('#formatLegendNumber', function () {
    var tests = [
      {m:'undefined to undefined',i:undefined,e:undefined},
      {m:'0 to 0',i:0,e:0},
      {m:'1 to 1',i:1,e:1},
      {m:'1.23 to 1.2',i:1.23,e:1.2}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(mainChartHeatmapMetric.formatLegendNumber(test.i)).to.equal(test.e);
      });
    });
    it('NaN to NaN' + ' ', function () {
      expect(isNaN(mainChartHeatmapMetric.formatLegendNumber(NaN))).to.equal(true);
    });
  });


  describe('#slotDefinitions', function () {
    beforeEach(function () {
      sinon.stub(mainChartHeatmapMetric, 'generateSlot', Em.K);
    });
    afterEach(function () {
      mainChartHeatmapMetric.generateSlot.restore();
    });
    it('one slot', function () {
      mainChartHeatmapMetric.set('numberOfSlots', 1);
      mainChartHeatmapMetric.set('maximumValue', 100);
      mainChartHeatmapMetric.set('minimumValue', 0);

      mainChartHeatmapMetric.propertyDidChange('slotDefinitions');

      expect(mainChartHeatmapMetric.get('slotDefinitions').length).to.equal(3);
      expect(mainChartHeatmapMetric.generateSlot.getCall(0).args).to.eql([0, 100, '', {r: 0, g: 204, b: 0}]);
      expect(mainChartHeatmapMetric.generateSlot.callCount).to.be.equal(1);
    });
    it('two slots', function () {
      mainChartHeatmapMetric.set('numberOfSlots', 2);
      mainChartHeatmapMetric.set('maximumValue', 100);
      mainChartHeatmapMetric.set('minimumValue', 0);

      mainChartHeatmapMetric.propertyDidChange('slotDefinitions');

      expect(mainChartHeatmapMetric.get('slotDefinitions').length).to.equal(4);
      expect(mainChartHeatmapMetric.generateSlot.getCall(0).args).to.eql([0, 50, '', {r: 0, g: 204, b: 0}]);
      expect(mainChartHeatmapMetric.generateSlot.getCall(1).args).to.eql([50, 100, '', {r: 159, g: 238, b: 0}]);
      expect(mainChartHeatmapMetric.generateSlot.callCount).to.be.equal(2);
    });
  });

  describe('#generateSlot()', function () {
    beforeEach(function () {
      sinon.stub(mainChartHeatmapMetric, 'formatLegendNumber').returns('val');
      sinon.stub(date, 'timingFormat').returns('time');
    });
    afterEach(function () {
      mainChartHeatmapMetric.formatLegendNumber.restore();
      date.timingFormat.restore();
    });
    it('label suffix is empty', function () {
      expect(mainChartHeatmapMetric.generateSlot(0, 1, '', {r: 0, g: 0, b: 0})).to.eql(Em.Object.create({
        "from": "val",
        "to": "val",
        "label": "val - val",
        "cssStyle": "background-color:rgb(0,0,0)"
      }));

      expect(mainChartHeatmapMetric.formatLegendNumber.getCall(0).args).to.eql([0]);
      expect(mainChartHeatmapMetric.formatLegendNumber.getCall(1).args).to.eql([1]);
    });
    it('label suffix is "ms"', function () {
      expect(mainChartHeatmapMetric.generateSlot(0, 1, 'ms', {r: 0, g: 0, b: 0})).to.eql(Em.Object.create({
        "from": "val",
        "to": "val",
        "label": "time - time",
        "cssStyle": "background-color:rgb(0,0,0)"
      }));

      expect(mainChartHeatmapMetric.formatLegendNumber.getCall(0).args).to.eql([0]);
      expect(mainChartHeatmapMetric.formatLegendNumber.getCall(1).args).to.eql([1]);
      expect(date.timingFormat.getCall(0).args).to.eql(['val', 'zeroValid']);
      expect(date.timingFormat.getCall(1).args).to.eql(['val', 'zeroValid']);
    });
  });

  describe('#getHatchStyle()', function () {
    var testCases = [
      {
        title: 'unknown browser',
        data: {},
        result: 'background-color:rgb(135, 206, 250)'
      },
      {
        title: 'webkit browser',
        data: {
          webkit: true
        },
        result: 'background-image:-webkit-repeating-linear-gradient(-45deg, #FF1E10, #FF1E10 3px, #ff6c00 3px, #ff6c00 6px)'
      },
      {
        title: 'mozilla browser',
        data: {
          mozilla: true
        },
        result: 'background-image:repeating-linear-gradient(-45deg, #FF1E10, #FF1E10 3px, #ff6c00 3px, #ff6c00 6px)'
      },
      {
        title: 'IE version 9',
        data: {
          msie: true,
          version: '9.0'
        },
        result: 'background-color:rgb(135, 206, 250)'
      },
      {
        title: 'IE version 10',
        data: {
          msie: true,
          version: '10.0'
        },
        result: 'background-image:repeating-linear-gradient(-45deg, #FF1E10, #FF1E10 3px, #ff6c00 3px, #ff6c00 6px)'
      }
    ]

    testCases.forEach(function(test){
      it(test.title, function () {
        jQuery.browser = test.data;
        expect(mainChartHeatmapMetric.getHatchStyle()).to.equal(test.result);
      });
    });
  });

  describe('#hostToSlotMap', function () {
    it('hostToValueMap is null', function () {
      mainChartHeatmapMetric.set('hostToValueMap', null);
      mainChartHeatmapMetric.set('hostNames', []);
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.be.empty;
    });
    it('hostNames is null', function () {
      mainChartHeatmapMetric.set('hostToValueMap', {});
      mainChartHeatmapMetric.set('hostNames', null);
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.be.empty;
    });
    it('slot greater than -1', function () {
      mainChartHeatmapMetric.set('hostToValueMap', {});
      mainChartHeatmapMetric.set('hostNames', ['host1']);
      sinon.stub(mainChartHeatmapMetric, 'calculateSlot').returns(0);
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.eql({'host1': 0});
      expect(mainChartHeatmapMetric.calculateSlot.calledWith({}, 'host1')).to.be.true;
      mainChartHeatmapMetric.calculateSlot.restore();
    });
    it('slot equal to -1', function () {
      mainChartHeatmapMetric.set('hostToValueMap', {});
      mainChartHeatmapMetric.set('hostNames', ['host1']);
      sinon.stub(mainChartHeatmapMetric, 'calculateSlot').returns('-1');
      mainChartHeatmapMetric.propertyDidChange('hostToSlotMap');
      expect(mainChartHeatmapMetric.get('hostToSlotMap')).to.be.empty;
      expect(mainChartHeatmapMetric.calculateSlot.calledWith({}, 'host1')).to.be.true;
      mainChartHeatmapMetric.calculateSlot.restore();
    });
  });

  describe('#calculateSlot()', function () {
    var testCases = [
      {
        title: 'hostToValueMap is empty',
        data: {
          hostToValueMap: {},
          hostName: 'host1',
          slotDefinitions: []
        },
        result: -1
      },
      {
        title: 'host value is NaN',
        data: {
          hostToValueMap: {'host1': NaN},
          hostName: 'host1',
          slotDefinitions: []
        },
        result: -2
      },
      {
        title: 'host value correct but slotDefinitions does not contain host value',
        data: {
          hostToValueMap: {'host1': 1},
          hostName: 'host1',
          slotDefinitions: [{}, {}]
        },
        result: -1
      },
      {
        title: 'host value -1',
        data: {
          hostToValueMap: {'host1': -1},
          hostName: 'host1',
          slotDefinitions: [
            {
              from: 0,
              to: 10
            },
            {},
            {}
          ]
        },
        result: 0
      },
      {
        title: 'host value 11',
        data: {
          hostToValueMap: {'host1': 11},
          hostName: 'host1',
          slotDefinitions: [
            {
              from: 0,
              to: 10
            },
            {},
            {}
          ]
        },
        result: 0
      },
      {
        title: 'host value 5',
        data: {
          hostToValueMap: {'host1': 5},
          hostName: 'host1',
          slotDefinitions: [
            {},
            {
              from: 0,
              to: 10
            },
            {},
            {}
          ]
        },
        result: 1
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        sinon.stub(mainChartHeatmapMetric, 'get').withArgs('slotDefinitions').returns(test.data.slotDefinitions);
        expect(mainChartHeatmapMetric.calculateSlot(test.data.hostToValueMap, test.data.hostName)).to.equal(test.result);
        mainChartHeatmapMetric.get.restore();
      });
    });
  });

});
