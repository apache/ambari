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
var validator = require('utils/validator');

/**
 * Slider-view for configs
 * Used to numeric values
 * Config value attributes should contain minimum and maximum limits for value
 * @type {App.ConfigWidgetView}
 */
App.SliderConfigWidgetView = App.ConfigWidgetView.extend({

  templateName: require('templates/common/configs/widgets/slider_config_widget'),

  /**
   * Slider-object created on the <code>initSlider</code>
   * @type {Object}
   */
  slider: null,

  /**
   * Mirror of the config-value shown in the input on the left of the slider
   * @type {number}
   */
  mirrorValue: 0,

  /**
   * Determines if used-input <code>mirrorValue</code> is valid
   * Calculated on the <code>mirrorValueObs</code>
   * @type {boolean}
   */
  isMirrorValueValid: true,

  willInsertElement: function () {
    this._super();
    this.addObserver('mirrorValue', this, this.mirrorValueObs);
  },

  didInsertElement: function () {
    this._super();
    this.set('mirrorValue', this.get('config.value'));
    this.initSlider();
  },

  /**
   * Check if <code>mirrorValue</code> was updated by user
   * Validate it. If value is correct, set it to slider and config.value
   * @method mirrorValueObs
   */
  mirrorValueObs: function () {
    var mirrorValue = this.get('mirrorValue'),
      slider = this.get('slider'),
      min = this.get('config.stackConfigProperty.valueAttributes.minimum'),
      max = this.get('config.stackConfigProperty.valueAttributes.maximum'),
      validationFunction = this.get('config.stackConfigProperty.valueAttributes.type') == 'int' ? validator.isValidInt : validator.isValidFloat,
      parseFunction = this.get('config.stackConfigProperty.valueAttributes.type') == 'int' ? parseInt : parseFloat;
    if (validationFunction(mirrorValue)) {
      var parsed = parseFunction(mirrorValue);
      if (parsed >= min && parsed <=max) {
        this.set('isMirrorValueValid', true);
        this.set('config.value', parsed);
        if (slider) {
          slider.setValue(this.get('config.value'));
        }
      }
      else {
        this.set('isMirrorValueValid', false);
      }
    }
    else {
      this.set('isMirrorValueValid', false);
    }
  },

  /**
   * Draw slider for current config
   * @method initSlider
   */
  initSlider: function () {
    var self = this,
      config = this.get('config'),
      valueAttributes = config.get('stackConfigProperty.valueAttributes'),
      unit = valueAttributes.get('unit'),
      ticks = [valueAttributes.minimum],
      ticksLabels = [];
    for (var i = 1; i <= 3; i++) {
      ticks.push(Math.round((valueAttributes.minimum + valueAttributes.maximum) / 4 * i));
    }
    ticks.push(valueAttributes.maximum);
    ticks.forEach(function (tick, indx) {
      ticksLabels.push(indx  % 2===0 ? tick + ' ' + unit : '');
    });
    var slider = new Slider('#' + this.get('elementId') + ' input.slider-input', {
      value: this.get('config.value'),
      ticks: ticks,
      tooltip: 'hide',
      ticks_labels: ticksLabels,
      ticks_snap_bounds: valueAttributes.get('type') === 'int' ? 1 : 0.1,
      step: valueAttributes.get('type') === 'int' ? 1 : 0.1
    });
    slider.on('slide', function (newValue) {
      self.set('config.value', newValue);
      self.set('mirrorValue', newValue);
    });
    this.set('slider', slider);
    this.$('.slider-tick:first, .slider-tick:last').hide(); // hide some ticks. can't do this via css
  },

  /**
   * Restore <code>defaultValue</code> for config
   * Restore <code>mirrorValue</code> too
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    this.get('slider').setValue(this.get('config.value'));
    this.set('mirrorValue', this.get('config.value'));
  }

});
