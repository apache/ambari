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
   * Determines if widget controls should be disabled
   * @type {boolean}
   */
  disabled: false,

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

  /**
   * Function used to parse config value (based on <code>config.stackConfigProperty.valueAttributes.type</code>)
   * For integer - parseInt, for float - parseFloat
   * @type {Function}
   */
  parseFunction: function () {
    return this.get('config.stackConfigProperty.valueAttributes.type') === 'int' ? parseInt : parseFloat;
  }.property('config.stackConfigProperty.valueAttributes.type'),

  /**
   * Function used to validate config value (based on <code>config.stackConfigProperty.valueAttributes.type</code>)
   * For integer - validator.isValidInt, for float - validator.isValidFloat
   * @type {Function}
   */
  validateFunction: function () {
    return this.get('config.stackConfigProperty.valueAttributes.type') === 'int' ? validator.isValidInt : validator.isValidFloat;
  }.property('config.stackConfigProperty.valueAttributes.type'),

  /**
   * Enable/disable slider state
   * @method toggleWidgetState
   */
  toggleWidgetState: function () {
    var slider = this.get('slider');
    this.get('config.isEditable') ? slider.enable() : slider.disable();
    this.set('disabled', !this.get('config.isEditable'));
  }.observes('config.isEditable'),

  willInsertElement: function () {
    this._super();
    this.addObserver('mirrorValue', this, this.mirrorValueObs);
  },

  didInsertElement: function () {
    this._super();
    this.set('mirrorValue', this.get('config.value'));
    this.prepareValueAttributes();
    this.initSlider();
    this.toggleWidgetState();
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
      validationFunction = this.get('validateFunction'),
      parseFunction = this.get('parseFunction');
    if (validationFunction(mirrorValue)) {
      var parsed = parseFunction(mirrorValue);
      if (parsed >= min && parsed <= max) {
        this.set('isMirrorValueValid', true);
        this.set('config.value', '' + parsed);
        if (slider) {
          slider.setValue(parsed);
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
   * valueAttributes are strings, but should be numbers
   * parse them using <code>parseFunction</code>
   * @method prepareValueAttributes
   */
  prepareValueAttributes: function () {
    var valueAttributes = this.get('config.stackConfigProperty.valueAttributes'),
      parseFunction = this.get('parseFunction');
    if (!valueAttributes) return;
    Em.set(valueAttributes, 'maximum', parseFunction(valueAttributes.maximum));
    Em.set(valueAttributes, 'minimum', parseFunction(valueAttributes.minimum));
  },

  /**
   * Draw slider for current config
   * @method initSlider
   */
  initSlider: function () {
    var self = this,
      config = this.get('config'),
      valueAttributes = config.get('stackConfigProperty.valueAttributes'),
      unit = Em.getWithDefault(valueAttributes, 'unit', ''),
      parseFunction = this.get('parseFunction'),
      ticks = [valueAttributes.minimum],
      ticksLabels = [];

    // ticks and labels
    for (var i = 1; i <= 3; i++) {
      var val = (valueAttributes.minimum + valueAttributes.maximum) / 4 * i;
      // if value's type is float, ticks may be float too
      ticks.push(valueAttributes.type === 'int' ? Math.round(val) : parseFloat(val.toFixed(1)));
    }
    ticks.push(valueAttributes.maximum);
    ticks.forEach(function (tick, index) {
      ticksLabels.push(index % 2 === 0 ? tick + ' ' + unit : '');
    });

    var slider = new Slider('#' + this.get('elementId') + ' input.slider-input', {
      value: parseFunction(this.get('config.value')),
      ticks: ticks,
      tooltip: 'hide',
      ticks_labels: ticksLabels,
      ticks_snap_bounds: Em.get(valueAttributes, 'type') === 'int' ? 1 : 0.1,
      step: Em.get(valueAttributes, 'type') === 'int' ? 1 : 0.1
    });

    slider.on('change', function (obj) {
      var val = parseFunction(obj.newValue);
      self.set('config.value', '' + val);
      self.set('mirrorValue', val);
    });

    this.set('slider', slider);
    // hide some ticks. can't do this via css
    this.$('.slider-tick:first, .slider-tick:last').hide();
  },

  /**
   * Restore <code>defaultValue</code> for config
   * Restore <code>mirrorValue</code> too
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    var parseFunction = this.get('parseFunction'),
      val = parseFunction(this.get('config.value'));
    this.get('slider').setValue(val);
    this.set('mirrorValue', val);
  }

});
