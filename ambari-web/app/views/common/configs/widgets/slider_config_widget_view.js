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
   * Unit label to display.
   * @type {String}
   */
  unitLabel: '',

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
    this.prepareValueConverter();
    this.addObserver('mirrorValue', this, this.mirrorValueObs);
  },

  didInsertElement: function () {
    this._super();
    this.set('mirrorValue', this.widgetValueByConfigAttributes(this.get('config.value')));
    this.prepareValueAttributes();
    this.initSlider();
    this.toggleWidgetState();
    this.initPopover();
    this.addObserver('config.stackConfigProperty.valueAttributes.minimum', this, this.changeBoundaries);
    this.addObserver('config.stackConfigProperty.valueAttributes.maximum', this, this.changeBoundaries);
    this.addObserver('config.stackConfigProperty.valueAttributes.step', this, this.changeBoundaries);
  },

  willDestroyElement: function() {
    this.removeObserver('config.stackConfigProperty.valueAttributes.step', this, this.changeBoundaries);
    this.removeObserver('config.stackConfigProperty.valueAttributes.maximum', this, this.changeBoundaries);
    this.removeObserver('config.stackConfigProperty.valueAttributes.minimum', this, this.changeBoundaries);
  },

  /**
   * view class for text box that is used with slider widget
   * @type {Em.TextField}
   */
  MirrorValueView: Em.TextField.extend({
    focusOut: function() {
      this.get('parentView').sendRequestRorDependentConfigs(this.get('parentView.config'));
    }
  }),

  /**
   * Check if <code>mirrorValue</code> was updated by user
   * Validate it. If value is correct, set it to slider and config.value
   * @method mirrorValueObs
   */
  mirrorValueObs: function () {
    var mirrorValue = this.get('mirrorValue'),
      slider = this.get('slider'),
      min = this.widgetValueByConfigAttributes(this.get('config.stackConfigProperty.valueAttributes.minimum')),
      max = this.widgetValueByConfigAttributes(this.get('config.stackConfigProperty.valueAttributes.maximum')),
      validationFunction = this.get('validateFunction'),
      parseFunction = this.get('parseFunction');
    if (validationFunction(mirrorValue)) {
      var parsed = parseFunction(mirrorValue);
      if (parsed >= min && parsed <= max) {
        this.set('isMirrorValueValid', true);
        this.set('config.errorMessage', '');
        this.set('config.value', '' + this.configValueByWidget(parsed));
        if (slider) {
          slider.setValue(parsed);
        }
      }
      else {
        this.set('isMirrorValueValid', false);
        this.set('config.errorMessage', 'Invalid value');
      }
    }
    else {
      this.set('isMirrorValueValid', false);
      this.set('config.errorMessage', 'Invalid value');
    }
  },

  /**
   * @override
   * @method setValue
   * set widget value same as config value
   */
  setValue: function() {
    var parseFunction = this.get('parseFunction');
    this.set('mirrorValue', this.widgetValueByConfigAttributes(parseFunction(this.get('config.value'))));
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
   * Setup convert table according to widget unit-name and property type.
   * Set label for unit to display.
   */
  prepareValueConverter: function() {
    var widgetUnit = this._converterGetWidgetUnits();
    if (['int', 'float'].contains(this._converterGetPropertyAttributes()) && widgetUnit == 'percent') {
      this.set('currentDimensionType', 'percent.percent_' + this._converterGetPropertyAttributes());
    }
    this.set('unitLabel', Em.getWithDefault(this.get('unitLabelMap'), widgetUnit, widgetUnit));
  },

  /**
   * Draw slider for current config
   * @method initSlider
   */
  initSlider: function () {
    var self = this,
      config = this.get('config'),
      valueAttributes = config.get('stackConfigProperty.valueAttributes'),
      parseFunction = this.get('parseFunction'),
      ticks = [this.widgetValueByConfigAttributes(valueAttributes.minimum)],
      ticksLabels = [],
      defaultValue = this.widgetValueByConfigAttributes(this.valueForTick(+config.get('defaultValue'))),
      defaultValueMirroredId,
      defaultValueId;

    // ticks and labels
    for (var i = 1; i <= 3; i++) {
      var val = this.widgetValueByConfigAttributes((valueAttributes.minimum + valueAttributes.maximum)) / 4 * i;
      // if value's type is float, ticks may be float too
      ticks.push(this.valueForTick(val));
    }
    ticks.push(this.widgetValueByConfigAttributes(valueAttributes.maximum));
    ticks.forEach(function (tick, index) {
      ticksLabels.push(index % 2 === 0 ? tick + ' ' + self.get('unitLabel') : '');
    });
    // process additional tick for default value if it not defined in previous computation
    if (!ticks.contains(defaultValue)) {
      // push default value
      ticks.push(defaultValue);
      // and resort array
      ticks = ticks.sort(function(a,b) { return a-b; });
      defaultValueId = ticks.indexOf(defaultValue);
      // to save nice tick labels layout we should add new tick value which is mirrored by index to default value
      defaultValueMirroredId = ticks.length - defaultValueId;
      // push empty label for default value tick
      ticksLabels.insertAt(defaultValueId, '');
      // push empty to mirrored position
      ticksLabels.insertAt(defaultValueMirroredId, '');
      // for saving correct sliding need to add value to mirrored position which is average between previous
      // and next value
      ticks.insertAt(defaultValueMirroredId, (ticks[defaultValueMirroredId] + ticks[defaultValueMirroredId - 1])/2);
      // get new index for default value
      defaultValueId = ticks.indexOf(defaultValue);
    } else {
      defaultValueId = ticks.indexOf(defaultValue);
    }

    var slider = new Slider(this.$('input.slider-input')[0], {
      value: this.widgetValueByConfigAttributes(parseFunction(this.get('config.value'))),
      ticks: ticks,
      tooltip: 'hide',
      ticks_labels: ticksLabels,
      ticks_snap_bounds: Em.get(valueAttributes, 'type') === 'int' ? 1 : 0.1,
      step: Em.get(valueAttributes, 'type') === 'int' ? 1 : 0.1
    });

    slider.on('change', function (obj) {
      var val = parseFunction(obj.newValue);
      self.set('config.value', '' + self.configValueByWidget(val));
      self.set('mirrorValue', val);
    }).on('slideStop', function() {
        /**
         * action to run sendRequestRorDependentConfigs when
         * we have changed config value within slider
         */
        self.sendRequestRorDependentConfigs(self.get('config'));
      });
    this.set('slider', slider);
    var sliderTicks = this.$('.ui-slider-wrapper:eq(0) .slider-tick');

    sliderTicks.eq(defaultValueId).addClass('slider-tick-default').on('click', function() {
      self.restoreValue();
    });
    // if mirrored value was added need to hide the tick for it
    if (defaultValueMirroredId) {
      sliderTicks.eq(defaultValueMirroredId).hide();
    }
    // hide some ticks. can't do this via css
    if (defaultValueId == 0) {
      sliderTicks.last().hide();
    } else
      if (defaultValueId == ticks.length - 1) {
        sliderTicks.first().hide();
      }
      else {
        sliderTicks.first().hide();
        sliderTicks.last().hide();
      }
  },

  /**
   * Convert value according to property attribute unit.
   *
   * @method valueForTick
   * @param {Number} val
   * @private
   * @returns {Number}
   */
  valueForTick: function(val) {
    return this.get('config.stackConfigProperty.valueAttributes').type === 'int' ? Math.round(val) : parseFloat(val.toFixed(2));
  },

  /**
   * Restore <code>defaultValue</code> for config
   * Restore <code>mirrorValue</code> too
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    var parseFunction = this.get('parseFunction'),
      val = this.widgetValueByConfigAttributes(parseFunction(this.get('config.value')));
    this.get('slider').setValue(val);
    this.set('mirrorValue', val);
  },

  /**
   * Run changeBoundariesOnce only once
   * @method changeBoundaries
   */
  changeBoundaries: function() {
    Em.run.once(this, 'changeBoundariesOnce');
  },

  /**
   * recreate widget in case max or min values were changed
   * @method changeBoundariesOnce
   */
  changeBoundariesOnce: function () {
    if ($.mocho) {
      //temp fix as it can broke test that doesn't have any connection with this method
      return;
    }
    var self = this;
    var valueAttributes = this.get('config.stackConfigProperty.valueAttributes');

    self.prepareValueAttributes();
    if (self.get('slider')) {
      self.get('slider').destroy();
      self.initSlider();
      if (self.get('config.value') > Em.get(valueAttributes, 'maximum')) {
        self.set('mirrorValue', this.widgetValueByConfigAttributes(Em.get(valueAttributes, 'maximum')));
      }
      if (self.get('config.value') < Em.get(valueAttributes, 'minimum')) {
        self.set('mirrorValue', this.widgetValueByConfigAttributes(Em.get(valueAttributes, 'minimum')));
      }
      self.toggleWidgetState();
    }
  }

});
