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

var viewInt, viewFloat;

describe('App.SliderConfigWidgetView', function () {

  beforeEach(function () {
    viewInt = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
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
          })
        })
      })
    });
    viewInt.willInsertElement();
    viewInt.didInsertElement();
    viewFloat = App.SliderConfigWidgetView.create({
      initSlider: Em.K,
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
            maximum: '100',
            unit: '%'
          })
        })
      })
    });
    viewFloat.willInsertElement();
    viewFloat.didInsertElement();
  });

  describe('#mirrorValue', function () {
    it('should be equal to config.value after init', function () {
      expect(viewInt.get('mirrorValue')).to.equal('' + viewInt.get('config.value'));
      expect(viewFloat.get('mirrorValue')).to.equal('' + viewFloat.get('config.value'));
    });
  });

  describe('#mirrorValueObs', function () {

    it('check int', function () {
      viewInt.set('mirrorValue', 1000);
      expect(viewInt.get('isMirrorValueValid')).to.be.true;
      expect(viewInt.get('config.value')).to.equal('1000');

      viewInt.set('mirrorValue', 100500);
      expect(viewInt.get('isMirrorValueValid')).to.be.false;
      expect(viewInt.get('config.value')).to.equal('1000');
    });

    it('check float', function () {
      viewFloat.set('mirrorValue', 55.5);
      expect(viewFloat.get('isMirrorValueValid')).to.be.true;
      expect(viewFloat.get('config.value')).to.equal('55.5');

      viewFloat.set('mirrorValue', 100500.5);
      expect(viewFloat.get('isMirrorValueValid')).to.be.false;
      expect(viewFloat.get('config.value')).to.equal('55.5');
    });

  });

  describe('#prepareValueAttributes', function () {

    it('should parse string to int', function () {

      var max = viewInt.get('config.stackConfigProperty.valueAttributes.maximum'),
        min = viewInt.get('config.stackConfigProperty.valueAttributes.minimum');
      viewInt.set('config.stackConfigProperty.valueAttributes.maximum', '' + max);
      viewInt.set('config.stackConfigProperty.valueAttributes.minimum', '' + min);
      viewInt.prepareValueAttributes();
      expect(viewInt.get('config.stackConfigProperty.valueAttributes.maximum')).to.equal(max);
      expect(viewInt.get('config.stackConfigProperty.valueAttributes.minimum')).to.equal(min);

    });

    it('should parse string to float', function () {

      var max = viewFloat.get('config.stackConfigProperty.valueAttributes.maximum'),
        min = viewFloat.get('config.stackConfigProperty.valueAttributes.minimum');
      viewFloat.set('config.stackConfigProperty.valueAttributes.maximum', '' + max);
      viewFloat.set('config.stackConfigProperty.valueAttributes.minimum', '' + min);
      viewFloat.prepareValueAttributes();
      expect(viewFloat.get('config.stackConfigProperty.valueAttributes.maximum')).to.equal(max);
      expect(viewFloat.get('config.stackConfigProperty.valueAttributes.minimum')).to.equal(min);

    });

  });

});