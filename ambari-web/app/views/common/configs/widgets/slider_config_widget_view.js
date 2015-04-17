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

  /**
   * Unit label to display.
   * @type {String}
   */
  unitLabel: '',

  undoAllowed: false,

  /**
   * max allowed value transformed form config unit to widget unit
   * @type {Number}
   */
  maxMirrorValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var max = this.widgetValueByConfigAttributes(this.get('config.stackConfigProperty.valueAttributes.maximum'));
    return parseFunction(max);
  }.property('config.stackConfigProperty.valueAttributes.maximum'),

  /**
   * min allowed value transformed form config unit to widget unit
   * @type {Number}
   */
  minMirrorValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var min = this.widgetValueByConfigAttributes(this.get('config.stackConfigProperty.valueAttributes.minimum'));
    return parseFunction(min);
  }.property('config.stackConfigProperty.valueAttributes.minimum'),

  /**
   * step transformed form config units to widget units
   * @type {Number}
   */
  mirrorStep: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var step = this.widgetValueByConfigAttributes(this.get('config.stackConfigProperty.valueAttributes.increment_step'));
    return step ? parseFunction(step) : this.get('unitType') === 'int' ? 1 : 0.1;
  }.property('config.stackConfigProperty.valueAttributes.increment_step'),


  /**
   * Default value of config property transformed according widget format
   * @returns {Number}
   */
  widgetDefaultValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    return parseFunction(this.widgetValueByConfigAttributes(this.get('config.defaultValue')));
  }.property('config.defaultValue'),

  /**
   * unit type of widget
   * @type {String}
   */
  unitType: function() {
    return this.get('config.stackConfigProperty.widget.units.length') && this.get('config.stackConfigProperty.widget.units')[0]['unit-name'];
  }.property('config.stackConfigProperty.widget.units.@each.unit-name'),

  /**
   * Function used to parse widget mirror value
   * For integer - parseInt, for float - parseFloat
   * @type {Function}
   */
  mirrorValueParseFunction: function () {
    return this.get('unitType') === 'int' ? parseInt : parseFloat;
  }.property('unitType'),

  /**
   * Function used to validate widget mirror value
   * For integer - validator.isValidInt, for float - validator.isValidFloat
   * @type {Function}
   */
  mirrorValueValidateFunction: function () {
    return this.get('unitType') === 'int' ? validator.isValidInt : validator.isValidFloat;
  }.property('unitType'),

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
    this._super();
  }.observes('config.isEditable'),

  willInsertElement: function () {
    this._super();
    this.prepareValueConverter();
    this.addObserver('mirrorValue', this, this.mirrorValueObs);
  },

  didInsertElement: function () {
    this._super();
    this.setValue();
    this.initSlider();
    this.toggleWidgetState();
    this.initPopover();
    this.addObserver('maxMirrorValue', this, this.changeBoundaries);
    this.addObserver('minMirrorValue', this, this.changeBoundaries);
    this.addObserver('mirrorStep', this, this.changeBoundaries);
  },

  willDestroyElement: function() {
    this.removeObserver('maxMirrorValue', this, this.changeBoundaries);
    this.removeObserver('minMirrorValue', this, this.changeBoundaries);
    this.removeObserver('mirrorStep', this, this.changeBoundaries);
  },

  /**
   * Check if <code>mirrorValue</code> was updated by user
   * Validate it. If value is correct, set it to slider and config.value
   * @method mirrorValueObs
   */
  mirrorValueObs: function () {
    var mirrorValue = this.get('mirrorValue'),
      slider = this.get('slider'),
      min = this.get('minMirrorValue'),
      max = this.get('maxMirrorValue'),
      validationFunction = this.get('mirrorValueValidateFunction'),
      parseFunction = this.get('mirrorValueParseFunction');
    if (validationFunction(mirrorValue)) {
      var parsed = parseFunction(mirrorValue);
      if (parsed >= min && parsed <= max) {
        this.set('isMirrorValueValid', true);
        this.set('config.errorMessage', '');
        this.set('config.value', '' + this.configValueByWidget(parsed));
        if (slider) {
          slider.setValue(parsed);
        }
      } else {
        this.set('isMirrorValueValid', false);
        this.set('config.errorMessage', 'Invalid value');
      }
    } else {
      this.set('isMirrorValueValid', false);
      this.set('config.errorMessage', 'Invalid value');
    }
  },

  /**
   * @override
   * @method setValue
   * set widget value same as config value
   */
  setValue: function(value) {
    var parseFunction = this.get('parseFunction');
    value = value || parseFunction(this.get('config.value'));
    this.set('mirrorValue', this.widgetValueByConfigAttributes(value));
  },

  /**
   * Setup convert table according to widget unit-name and property type.
   * Set label for unit to display.
   * @method prepareValueConverter
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
      ticks = [this.get('minMirrorValue')],
      ticksLabels = [],
      defaultValue = this.widgetValueByConfigAttributes(this.valueForTick(+config.get('defaultValue'))),
      defaultValueMirroredId,
      defaultValueId;

    // ticks and labels
    for (var i = 1; i <= 3; i++) {
      var val = (this.get('maxMirrorValue') + this.get('minMirrorValue')) / 4 * i;
      // if value's type is float, ticks may be float too
      ticks.push(this.valueForTick(val));
    }
    ticks.push(this.get('maxMirrorValue'));
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
      value: this.get('mirrorValue'),
      ticks: ticks,
      tooltip: 'always',
      ticks_labels: ticksLabels,
      step: this.get('mirrorStep'),
      formatter: function(val) {
        return val + self.get('unitLabel');
      }
    });

    slider.on('change', function (obj) {
      var val = self.get('mirrorValueParseFunction')(obj.newValue);
      self.set('config.value', '' + self.configValueByWidget(val));
      self.set('mirrorValue', val);
    }).on('slideStop', function() {
      /**
       * action to run sendRequestRorDependentConfigs when
       * we have changed config value within slider
       */
      self.get('controller').removeCurrentFromDependentList(self.get('config'));
      self.sendRequestRorDependentConfigs(self.get('config'));
    });
    this.set('slider', slider);
    var sliderTicks = this.$('.ui-slider-wrapper:eq(0) .slider-tick');
    var defaultSliderTick = sliderTicks.eq(defaultValueId);

    sliderTicks.eq(defaultValueId).addClass('slider-tick-default').on('mousedown', function(e) {
      if (self.get('disabled')) return false;
      self.restoreValue();
      e.stopPropagation();
      return false;
    });
    // create label for default value and align it
    defaultSliderTick.append('<span>{0}</span>'.format(defaultValue + this.get('unitLabel')));
    defaultSliderTick.find('span').css('marginLeft', -defaultSliderTick.find('span').width()/2 + 'px');
    // if mirrored value was added need to hide the tick for it
    if (defaultValueMirroredId) {
      sliderTicks.eq(defaultValueMirroredId).hide();
    }
    // mark last tick to fix it style
    sliderTicks.last().addClass('last');
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
    return this.get('unitType') === 'int' ? Math.round(val) : parseFloat(val.toFixed(3));
  },

  /**
   * Restore <code>defaultValue</code> for config
   * Restore <code>mirrorValue</code> too
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    this.get('slider').setValue(this.get('widgetDefaultValue'));
  },

  /**
   * Run changeBoundariesOnce only once
   * @method changeBoundaries
   */
  changeBoundaries: function() {
    if (this.get('config.stackConfigProperty.widget')) {
      Em.run.once(this, 'changeBoundariesOnce');
    }
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
    if (this.get('slider')) {
      this.get('slider').destroy();
      if (this.get('mirrorValue') > this.get('maxMirrorValue')) {
        this.setValue(this.get('maxMirrorValue'));
      }
      if (this.get('mirrorValue') < this.get('minMirrorValue')) {
        this.setValue(this.get('minMirrorValue'));
      }
      this.initSlider();
      this.toggleWidgetState();
    }
  }

});
