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

var viewInt, viewFloat, viewPercent;

describe('App.SliderConfigWidgetView', function () {

  beforeEach(function () {
    viewInt = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
      initPopover: Em.K,
      slider: {
        enable: Em.K,
        disable: Em.K,
        setValue: Em.K
      },
      config: Em.Object.create({
        name: 'a.b.c',
        description: 'A B C',
        value: '486',
        defaultValue: '486',
        stackConfigProperty: Em.Object.create({
          valueAttributes: Em.Object.create({
            type: 'int',
            minimum: '0',
            maximum: '2096',
            unit: 'MB'
          }),
          widget: Em.Object.create({
            type: 'slider',
            units: [{ 'unit-name': 'MB'}]
          })
        })
      })
    });
    viewInt.willInsertElement();
    viewInt.didInsertElement();

    viewFloat = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
      initPopover: Em.K,
      slider: {
        enable: Em.K,
        disable: Em.K,
        setValue: Em.K
      },
      config: Em.Object.create({
        name: 'a.b.c2',
        description: 'A B C 2',
        value: '72.2',
        defaultValue: '72.2',
        stackConfigProperty: Em.Object.create({
          valueAttributes: Em.Object.create({
            type: 'float',
            minimum: '0',
            maximum: '100'
          }),
          widget: Em.Object.create({
            type: 'slider',
            units: [{ 'unit-name': 'float'}]
          })
        })
      })
    });
    viewFloat.willInsertElement();
    viewFloat.didInsertElement();

    viewPercent = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
      initPopover: Em.K,
      slider: {
        enable: Em.K,
        disable: Em.K,
        setValue: Em.K
      },
      config: Em.Object.create({
        name: 'a.b.c3',
        description: 'A B C 3',
        value: '0.22',
        defaultValue: '0.22',
        stackConfigProperty: Em.Object.create({
          valueAttributes: Em.Object.create({
            type: 'float',
            minimum: '0',
            maximum: '0.8'
          }),
          widget: Em.Object.create({
            type: 'slider',
            units: [{ 'unit-name': 'percent'}]
          })
        })
      })
    });
    viewPercent.willInsertElement();
    viewPercent.didInsertElement();

    sinon.stub(viewInt, 'changeBoundaries', Em.K);
    sinon.stub(viewFloat, 'changeBoundaries', Em.K);
    sinon.stub(viewPercent, 'changeBoundaries', Em.K);
  });

  afterEach(function() {
    viewInt.changeBoundaries.restore();
    viewFloat.changeBoundaries.restore();
    viewPercent.changeBoundaries.restore();
  });

  describe('#mirrorValue', function () {
    it('should be equal to config.value after init', function () {
      expect('' + viewInt.get('mirrorValue')).to.equal(viewInt.get('config.value'));
      expect('' + viewFloat.get('mirrorValue')).to.equal(viewFloat.get('config.value'));
    });

    it('should be converted according to widget format', function() {
      expect(viewPercent.get('mirrorValue')).to.equal(22);
    });
  });

  describe('#mirrorValueObs', function () {

    it('check int', function () {
      viewInt.set('mirrorValue', 1000);
      expect(viewInt.get('isMirrorValueValid')).to.be.true;
      expect(viewInt.get('config.value')).to.equal('1000');
      expect(viewInt.get('config.errorMessage')).to.equal('');
      expect(viewInt.get('config.warnMessage')).to.equal('');

      viewInt.set('mirrorValue', 100500);
      expect(viewInt.get('isMirrorValueValid')).to.be.false;
      expect(viewInt.get('config.value')).to.equal('1000');
      expect(viewInt.get('config.errorMessage')).to.equal('');
      expect(viewInt.get('config.warnMessage')).to.have.property('length').that.is.least(1);
    });

    it('check float', function () {
      viewFloat.set('mirrorValue', 55.5);
      expect(viewFloat.get('isMirrorValueValid')).to.be.true;
      expect(viewFloat.get('config.value')).to.equal('55.5');
      expect(viewFloat.get('config.errorMessage')).to.equal('');
      expect(viewFloat.get('config.warnMessage')).to.equal('');

      viewFloat.set('mirrorValue', 100500.5);
      expect(viewFloat.get('isMirrorValueValid')).to.be.false;
      expect(viewFloat.get('config.value')).to.equal('55.5');
      expect(viewFloat.get('config.errorMessage')).to.equal('');
      expect(viewFloat.get('config.warnMessage')).to.have.property('length').that.is.least(1);
    });

    it('check percent', function () {
      viewPercent.set('mirrorValue', 32);
      expect(viewPercent.get('isMirrorValueValid')).to.be.true;
      expect(viewPercent.get('config.value')).to.equal('0.32');
      expect(viewPercent.get('config.errorMessage')).to.equal('');
      expect(viewPercent.get('config.warnMessage')).to.equal('');

      viewPercent.set('mirrorValue', 100500.5);
      expect(viewPercent.get('isMirrorValueValid')).to.be.false;
      expect(viewPercent.get('config.value')).to.equal('0.32');
      expect(viewPercent.get('config.errorMessage')).to.equal('');
      expect(viewPercent.get('config.warnMessage')).to.have.property('length').that.is.least(1);
    });
  });

  describe('#initSlider', function() {
    beforeEach(function() {
      this.view = App.SliderConfigWidgetView;
    });

    afterEach(function() {
      this.view.destroy();
      this.view = null;
    });

    var tests = [
      {
        viewSetup: {
          minMirrorValue: 20,
          maxMirrorValue: 100,
          widgetRecommendedValue: 30,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'float' },
              widget: { units: [ { 'unit-name': "percent"}]}
            })
          })
        },
        e: {
          ticks: [20,30,40,60,80,90,100],
          ticksLabels: ['20 %', '', '', '60 %', '', '', '100 %']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 5,
          maxMirrorValue: 50,
          widgetRecommendedValue: 35,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int' },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [5, 16, 22, 28, 35, 39, 50],
          ticksLabels: ['5 ','', '', '28 ', '', '', '50 ']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 2,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [1,2],
          ticksLabels: ['1 ', '2 ']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 3,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [1,2,3],
          ticksLabels: ['1 ', '2 ', '3 ']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0,
          maxMirrorValue: 3,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [0,1,2,3],
          ticksLabels: ['0 ', '1 ', '2 ', '3 ']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 1,
          maxMirrorValue: 5,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [1,2,3,4,5],
          ticksLabels: ['1 ', '', '3 ', '', '5 ']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0,
          maxMirrorValue: 5,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [0,2,3,5],
          ticksLabels: ['0 ', '2 ', '3 ', '5 ']
        }
      },
      {
        viewSetup: {
          minMirrorValue: 0,
          maxMirrorValue: 23,
          widgetRecommendedValue: 2,
          config: Em.Object.create({
            stackConfigProperty: Em.Object.create({
              valueAttributes: { type: 'int', increment_step: 1 },
              widget: { units: [ { 'unit-name': "int"}]}
            })
          })
        },
        e: {
          ticks: [0,2,6,12,17,20,23],
          ticksLabels: ['0 ', '', '', '12 ', '', '', '23 ']
        }
      }
    ];

    tests.forEach(function(test) {
      it('should generate ticks: {0} - tick labels: {1}'.format(test.e.ticks, test.e.ticksLabels), function() {
        var ticks, ticksLabels;
        this.view = this.view.create(test.viewSetup);
        var sliderCopy= window.Slider.prototype;
        window.Slider = function(a, b) {
          ticks = b.ticks;
          ticksLabels = b.ticks_labels;
          return {
            on: function() {
              return this;
            }
          };
        };
        sinon.stub(this.view, '$')
          .withArgs('input.slider-input').returns([])
          .withArgs('.ui-slider-wrapper:eq(0) .slider-tick').returns({
            eq: function() {return this;},
            addClass: function() {return this;},
            on: function() {return this;},
            append: function() {return this;},
            find: function() {return this;},
            css: function() {return this;},
            width: function() {},
            last: function() { return this;},
            hide: function() { return this;}
          });
        this.view.willInsertElement();
        this.view.initSlider();
        window.Slider.prototype = sliderCopy;
        this.view.$.restore();
        expect(ticks.toArray()).to.be.eql(test.e.ticks);
        expect(ticksLabels.toArray()).to.be.eql(test.e.ticksLabels);
      });
    });
  });

});
