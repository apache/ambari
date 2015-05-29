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

  classNames: ['widget-config'],

  templateName: require('templates/common/configs/widgets/slider_config_widget'),

  supportSwitchToCheckBox: true,

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

  /**
   * List of widget's properties which <code>changeBoundaries</code>-method should observe
   * @type {string[]}
   */
  changeBoundariesProperties: ['maxMirrorValue', 'widgetRecommendedValue','minMirrorValue', 'mirrorStep'],

  /**
   * max allowed value transformed form config unit to widget unit
   * @type {Number}
   */
  maxMirrorValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var defaultGroupAttr = this.get('config.stackConfigProperty.valueAttributes');
    var groupAttr = this.get('configGroup') && this.get('config.stackConfigProperty.valueAttributes')[this.get('configGroup.name')];
    var maximum = (groupAttr && !Em.isNone(groupAttr['maximum'])) ? groupAttr['maximum'] : defaultGroupAttr['maximum'];
    var max = this.widgetValueByConfigAttributes(maximum);
    return parseFunction(max);
  }.property('config.stackConfigProperty.valueAttributes.maximum', 'controller.forceUpdateBoundaries'),

  /**
   * min allowed value transformed form config unit to widget unit
   * @type {Number}
   */
  minMirrorValue: function() {
    var parseFunction = this.get('mirrorValueParseFunction');
    var defaultGroupAttr = this.get('config.stackConfigProperty.valueAttributes');
    var groupAttr = this.get('configGroup') && this.get('config.stackConfigProperty.valueAttributes')[this.get('configGroup.name')];
    var minimum = (groupAttr && !Em.isNone(groupAttr['minimum'])) ? groupAttr['minimum'] : defaultGroupAttr['minimum'];
    var min = this.widgetValueByConfigAttributes(minimum);
    return parseFunction(min);
  }.property('config.stackConfigProperty.valueAttributes.minimum', 'controller.forceUpdateBoundaries'),

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
  widgetDefaultValue: function () {
    var parseFunction = this.get('mirrorValueParseFunction');
    return parseFunction(this.widgetValueByConfigAttributes(this.get('config.savedValue')));
  }.property('config.savedValue'),

  /**
   * Default value of config property transformed according widget format
   * @returns {Number}
   */
  widgetRecommendedValue: function () {
    var parseFunction = this.get('mirrorValueParseFunction');
    return parseFunction(this.widgetValueByConfigAttributes(this.get('config.recommendedValue')));
  }.property('config.recommendedValue'),

  /**
   * unit type of widget
   * @type {String}
   */
  unitType: function () {
    var widgetUnit = this.get('config.stackConfigProperty.widget.units.length') && this.get('config.stackConfigProperty.widget.units')[0]['unit-name'].toLowerCase();
    var configUnit = this.get('config.stackConfigProperty.valueAttributes.type').toLowerCase();
    if (widgetUnit) {
      return this.get('units').indexOf(widgetUnit) > this.get('units').indexOf(configUnit) ? 'float' : this.get('config.stackConfigProperty.valueAttributes.type')
    } else {
      return 'float';
    }
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
    var self = this;
    this.get('changeBoundariesProperties').forEach(function(property) {
      self.addObserver(property, self, self.changeBoundaries);
    });
  },

  willDestroyElement: function() {
    var self = this;
    this.get('changeBoundariesProperties').forEach(function(property) {
      self.removeObserver(property, self, self.changeBoundaries);
    });
    this.removeObserver('mirrorValue', this, this.mirrorValueObs);
    if (this.get('slider')) {
      this.get('slider').destroy();
    }
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
      if (parsed > max) {
        this.set('isMirrorValueValid', false);
        this.get('config').setProperties({
          warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format(max + this.get('unitLabel')),
          warn: true
        });
      } else if (parsed < min) {
        this.set('isMirrorValueValid', false);
        this.get('config').setProperties({
          warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.less').format(min + this.get('unitLabel')),
          warn: true
        });
      } else {
        this.set('isMirrorValueValid', !this.get('config.error'));
        this.set('config.value', '' + this.configValueByWidget(parsed));
        if (slider) {
          slider.setValue(parsed);
        }
      }
      // avoid precision during restore value
      if (!Em.isNone(this.get('config.savedValue')) && parsed == parseFunction(this.widgetValueByConfigAttributes(this.get('config.savedValue')))) {
        this.set('config.value', this.get('config.savedValue'));
      }
    } else {
      this.set('isMirrorValueValid', false);
      this.set('config.errorMessage', 'Invalid value');
    }
  },

  /**
   * set widget value same as config value
   * @override
   * @method setValue
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
      recommendedValue = this.valueForTick(+this.get('widgetRecommendedValue')),
      range = this.get('maxMirrorValue') - this.get('minMirrorValue'),
      // for little odd numbers in range 4..23 and widget type 'int' use always 4 ticks
      isSmallInt = this.get('unitType') == 'int' && range > 4 && range < 23 && range % 2 == 1,
      recommendedValueMirroredId,
      recommendedValueId;

    // ticks and labels
    for (var i = 1; i <= 3; i++) {
      var val = this.get('minMirrorValue') + range * (i / (isSmallInt ? 3 : 4));
      // if value's type is float, ticks may be float too
      ticks.push(this.valueForTick(val));
    }

    ticks.push(this.get('maxMirrorValue'));
    ticks = ticks.uniq();
    ticks.forEach(function (tick, index, items) {
      ticksLabels.push((items.length < 5 || index % 2 === 0 || items.length - 1 == index) ? tick + ' ' + self.get('unitLabel') : '');
    });

    // default marker should be added only if recommendedValue is in range [min, max]
    if (recommendedValue <= this.get('maxMirrorValue') && recommendedValue >= this.get('minMirrorValue') && recommendedValue != '') {
      // process additional tick for default value if it not defined in previous computation
      if (!ticks.contains(recommendedValue)) {
        // push default value
        ticks.push(recommendedValue);
        // and resort array
        ticks = ticks.sort(function (a, b) {
          return a - b;
        });
        recommendedValueId = ticks.indexOf(recommendedValue);
        // to save nice tick labels layout we should add new tick value which is mirrored by index to default value
        recommendedValueMirroredId = ticks.length - recommendedValueId;
        // push mirrored default value behind default
        if (recommendedValueId == recommendedValueMirroredId) {
          recommendedValueMirroredId--;
        }
        // push empty label for default value tick
        ticksLabels.insertAt(recommendedValueId, '');
        // push empty to mirrored position
        ticksLabels.insertAt(recommendedValueMirroredId, '');
        // for saving correct sliding need to add value to mirrored position which is average between previous
        // and next value
        ticks.insertAt(recommendedValueMirroredId, this.valueForTick((ticks[recommendedValueMirroredId] + ticks[recommendedValueMirroredId - 1]) / 2));
        // get new index for default value
        recommendedValueId = ticks.indexOf(recommendedValue);
      } else {
        recommendedValueId = ticks.indexOf(recommendedValue);
      }
    }

    /**
     * Slider some times change config value while being created,
     * this may happens when slider recreating couple times during small period.
     * To cover this situation need to reset config value after slider initializing
     * @type {Sting}
     */
    var correctConfigValue = this.get('config.value');

    var slider = new Slider(this.$('input.slider-input')[0], {
      value: this.get('mirrorValue'),
      ticks: ticks,
      tooltip: 'always',
      ticks_labels: ticksLabels,
      step: this.get('mirrorStep'),
      formatter: function(val) {
        if (Em.isArray(val)) {
          return val[0] + self.get('unitLabel');
        } else {
          return val + self.get('unitLabel');
        }
      }
    });

    /**
     * Resetting config value, look for <code>correctConfigValue<code>
     * for more info
     */
    this.set('config.value', correctConfigValue);

    slider.on('change', function (obj) {
      var val = self.get('mirrorValueParseFunction')(obj.newValue);
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

    if (recommendedValueId) {
      sliderTicks.eq(recommendedValueId).addClass('slider-tick-default').on('mousedown', function(e) {
        if (self.get('disabled')) return false;
        self.setValue(self.get('config.recommendedValue'));
        e.stopPropagation();
        return false;
      });
      // create label for default value and align it
      // defaultSliderTick.append('<span>{0}</span>'.format(recommendedValue + this.get('unitLabel')));
      // defaultSliderTick.find('span').css('marginLeft', -defaultSliderTick.find('span').width()/2 + 'px');
      // if mirrored value was added need to hide the tick for it
      if (recommendedValueMirroredId) {
        sliderTicks.eq(recommendedValueMirroredId).hide();
      }
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
   * Restore <code>savedValue</code> for config
   * Restore <code>mirrorValue</code> too
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    this.get('slider').setValue(this.get('widgetDefaultValue'));
  },

  /**
   * @method setRecommendedValue
   */
  setRecommendedValue: function () {
    this._super();
    this.get('slider').setValue(this.get('widgetRecommendedValue'));
  },

  /**
   * Determines if config-value was changed
   * @type {boolean}
   */
  valueIsChanged: function () {
    return !Em.isNone(this.get('config.savedValue')) && this.get('parseFunction')(this.get('config.value')) != this.get('parseFunction')(this.get('config.savedValue'));
  }.property('config.value', 'config.savedValue'),

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
  },

  /**
   * Workaround for bootstrap-slider widget that was initiated inside hidden container.
   * @method refreshSliderObserver
   */
  refreshSliderObserver: function() {
    var sliderTickLabel = this.$('.ui-slider-wrapper:eq(0) .slider-tick-label:first');
    var self = this;
    if (sliderTickLabel.width() == 0) {
      Em.run.later('sync', function() {
        self.changeBoundariesOnce();
      }, 10);
    }
  }.observes('parentView.content.isActive', 'parentView.parentView.tab.isActive'),

  /**
   * Check if value provided by user in the textbox may be used in the slider
   * @returns {boolean}
   * @method isValueCompatibleWithWidget
   */
  isValueCompatibleWithWidget: function() {
    if (this._super()) {
      if (!this.get('validateFunction')(this.get('config.value'))) {
        return false;
      }
      var configValue = this.get('parseFunction')(this.get('config.value'));
      if (this.get('config.stackConfigProperty.valueAttributes.minimum')) {
        var min = this.get('parseFunction')(this.get('config.stackConfigProperty.valueAttributes.minimum'));
        if (configValue < min) return false;
      }
      if (this.get('config.stackConfigProperty.valueAttributes.maximum')) {
        var max = this.get('parseFunction')(this.get('config.stackConfigProperty.valueAttributes.maximum'));
        if (configValue > max) return false;
      }
      return true;
    }
    return false;
  }

});
