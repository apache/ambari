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
require('views/common/controls_view');
/**
 * Common view for config widgets
 * @type {Em.View}
 */
App.ConfigWidgetView = Em.View.extend(App.SupportsDependentConfigs, App.WidgetPopoverSupport, App.ConvertUnitWidgetViewMixin, App.ServiceConfigCalculateId, {

  /**
   * @type {App.ConfigProperty}
   */
  config: null,

  /**
   * Determines if user hover on widget-view
   * @type {boolean}
   */
  isHover: false,

  /**
   * Determines if widget controls should be disabled
   * @type {boolean}
   */
  disabled: false,

  /**
   * Determines if widget is editable
   * It true - show all control-elements (undo, override, finalize etc) for widget
   * If false - no widget control-elements will be shown
   * Bound from template
   * @type {boolean}
   */
  canEdit: true,

  canNotEdit: Em.computed.not('canEdit'),

  /**
   * Config label class attribute. Displays validation status of config.
   * @type {string}
   */
  configLabelClass: '',

  /**
   * defines if widget should be shown
   * if not, text-field with config value or label "Undefined" should be shown
   * @type {boolean}
   */
  doNotShowWidget: function() {
    return this.get('isPropertyUndefined') || this.get('config.showAsTextBox');
  }.property('isPropertyUndefined', 'config.showAsTextBox'),

  /**
   * defines if property in not defined in selected version
   * in this case "Undefined" should be shown instead of widget
   * @type {boolean}
   */
  isPropertyUndefined: function() {
    return this.get('config.value') === "Undefined";
  }.property('config.value'),

  /**
   * Tab where current widget placed
   * Bound in the template
   * @type {App.Tab}
   */
  tab: null,

  /**
   * Section where current widget placed
   * Bound in the template
   * @type {App.Section}
   */
  section: null,

  /**
   * Subsection where current widget placed
   * Bound in the template
   * @type {App.SubSection}
   */
  subSection: null,

  /**
   * Determines if user can switch custom widget-view to the input-field
   * @type {boolean}
   */
  supportSwitchToTextBox: false,

  /**
   * @type {boolean}
   */
  showPencil: function () {
    return this.get('supportSwitchToTextBox') && !this.get('disabled');
  }.property('supportSwitchToTextBox', 'disabled'),

  /**
   * Alias to <code>config.isOriginalSCP</code>
   * Should be used in the templates
   * Don't use original <code>config.isOriginalSCP</code> in the widget-templates!!!
   * @type {boolean}
   */
  isOriginalSCPBinding: 'config.isOriginalSCP',

  /**
   * Check if property validation failed for overridden property in case when its value is equal to parent
   * config property.
   * @type {boolean}
   */
  isOverrideEqualityError: function() {
    return this.get('config.parentSCP') && this.get('config.parentSCP.value') == this.get('config.value');
  }.property('config.isValid'),

  /**
   * Alias to <code>config.isComparison</code>
   * Should be used in the templates
   * Don't use original <code>config.isComparison</code> in the widget-templates!!!
   * @type {boolean}
   */
  isComparisonBinding: 'config.isComparison',

  classNameBindings:['isComparison:compare-mode', 'config.overrides.length:overridden-property'],

  issueMessage: '',

  issueView: Em.View.extend({

    tagName: 'i',

    classNames: ['icon-warning-sign'],

    classNameBindings: ['issueIconClass'],

    attributeBindings:['issueMessage:data-original-title'],

    /**
     * @type {App.ServiceConfigProperty}
     */
    config: null,

    /**
     * @type {string}
     */
    issueIconClass: '',

    /**
     * @type {string}
     */
    issueMessage: '',

    didInsertElement: function() {
      App.tooltip($(this.get('element')));
      this.errorLevelObserver();
      this.addObserver('issuedConfig.warnMessage', this, this.errorLevelObserver);
      this.addObserver('issuedConfig.errorMessage', this, this.errorLevelObserver);
      this.addObserver('parentView.isPropertyUndefined', this, this.errorLevelObserver);
    },

    willDestroyElement: function() {
      this.removeObserver('issuedConfig.warnMessage', this, this.errorLevelObserver);
      this.removeObserver('issuedConfig.errorMessage', this, this.errorLevelObserver);
      this.removeObserver('parentView.isPropertyUndefined', this, this.errorLevelObserver);
    },

    /**
     *
     * @method errorLevelObserver
     */
    errorLevelObserver: function() {
      var messageLevel = this.get('issuedConfig.errorMessage') ? 'ERROR': this.get('issuedConfig.warnMessage') ? 'WARN' : 'NONE';
      if (this.get('parentView.isPropertyUndefined')) {
        messageLevel = 'NONE';
      }
      var issue = {
        ERROR: {
          iconClass: '',
          message: this.get('issuedConfig.errorMessage'),
          configLabelClass: 'text-error'
        },
        WARN: {
          iconClass: 'warning',
          message: this.get('issuedConfig.warnMessage'),
          configLabelClass: 'text-warning'
        },
        NONE: {
          iconClass: 'hide',
          message: false,
          configLabelClass: ''
        }
      }[messageLevel];
      this.set('parentView.configLabelClass', issue.configLabelClass);
      this.set('issueIconClass', issue.iconClass);
      this.set('issueMessage', issue.message);
      this.set('parentView.issueMessage', issue.message);
    },

    /**
     * @type {App.ServiceConfigProperty}
     */
    issuedConfig: function() {
      var config = this.get('config');
      // check editable override
      if (!config.get('isEditable') && config.get('isOriginalSCP') && config.get('overrides.length') && config.get('overrides').someProperty('isEditable', true)) {
        config = config.get('overrides').findProperty('isEditable', true);
      } else if (config.get('isOriginalSCP') && config.get('isEditable')) {
        // use original config if it is not valid
        if (!config.get('isValid')) {
          return config;
        // scan overrides for non valid values and use it
        } else if (config.get('overrides.length') && config.get('overrides').someProperty('isValid', false)) {
          return config.get('overrides').findProperty('isValid', false);
        }
      }
      return config;
    }.property('config.isEditable', 'config.overrides.length')

  }),

  /**
   * Config name to display.
   * @type {String}
   */
  configLabel: function() {
    return this.get('config.stackConfigProperty.displayName') || this.get('config.displayName') || this.get('config.name');
  }.property('config.name', 'config.displayName'),


  /**
   * Error message computed in config property model
   * @type {String}
   */
  configErrorMessageBinding: 'config.errorMessage',

  /**
   * Determines if config-value was changed
   * @type {boolean}
   */
  valueIsChanged: function () {
    return !Em.isNone(this.get('config.savedValue')) && this.get('config.value') != this.get('config.savedValue');
  }.property('config.value', 'config.savedValue'),

  /**
   * Enable/disable widget state
   * @method toggleWidgetState
   */
  toggleWidgetState: function () {
    this.set('disabled', !this.get('config.isEditable'));
  }.observes('config.isEditable'),

  /**
   * Reset config-value to its default
   * @method restoreValue
   */
  restoreValue: function () {
    var self = this;
    this.set('config.value', this.get('config.savedValue'));
    this.sendRequestRorDependentConfigs(this.get('config')).done(function() {
      self.restoreDependentConfigs(self.get('config'));
    });

    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.savedIsFinal'));
    }
    Em.$('body > .tooltip').remove();
  },

  /**
   * set <code>recommendedValue<code> to config
   * and send request to change dependent configs
   * @method setRecommendedValue
   */
  setRecommendedValue: function() {
    var self = this;
    this.set('config.value', this.get('config.recommendedValue'));
    this.sendRequestRorDependentConfigs(this.get('config')).done(function() {
      if (self.get('config.value') === self.get('config.savedValue')) {
        self.restoreDependentConfigs(self.get('config'));
      }
    });

    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.recommendedIsFinal'));
    }
    Em.$('body > .tooltip').remove();
  },

  /**
   * Determines if override is allowed for <code>config</code>
   * @type {boolean}
   */
  overrideAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    return config.get('isOriginalSCP') && config.get('isPropertyOverridable') && !this.get('config.isComparison');
  }.property('config.isOriginalSCP', 'config.isPropertyOverridable', 'config.isComparison'),

  /**
   * Determines if undo is allowed for <code>config</code>
   * @type {boolean}
   */
  undoAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    if (!this.get('isOriginalSCP') || this.get('disabled')) return false;
    return !config.get('cantBeUndone') && config.get('isNotDefaultValue');
  }.property('config.cantBeUndone', 'config.isNotDefaultValue', 'isOriginalSCP', 'disabled'),

  /**
   * Determines if "final"-button should be shown
   * @type {boolean}
   */
  showFinalConfig: function () {
    var config = this.get('config');
    return config.get('isFinal') || (!config.get('isNotEditable') && this.get('isHover'));
  }.property('config.isFinal', 'config.isNotEditable', 'isHover'),

  /**
   *
   * @param {{context: App.ServiceConfigProperty}} event
   * @method toggleFinalFlag
   */
  toggleFinalFlag: function (event) {
    var configProperty = event.context;
    if (configProperty.get('isNotEditable')) {
      return;
    }
    configProperty.toggleProperty('isFinal');
  },

  /**
   * sync widget value with config value when dependent properties
   * have been loaded or changed
   * @method syncValueWithConfig
   */
  syncValueWithConfig: function() {
    this.setValue(this.get('config.value'));
  }.observes('controller.recommendationTimeStamp'),

  /**
   * defines if config has same config group as selected
   * @type {boolean}
   */
  referToSelectedGroup: function() {
    return this.get('controller.selectedConfigGroup.isDefault') && this.get('config.group') === null
    || this.get('controller.selectedConfigGroup.name') === this.get('config.group.name');
  }.property('controller.selectedConfigGroup.name', 'controller.selectedConfigGroup.isDefault'),

  didInsertElement: function () {
    App.tooltip($(this.get('element')).find('span'));
    var self = this;
    var element = this.$();
    if (element) {
      element.hover(function() {
        self.set('isHover', true);
      }, function() {
        self.set('isHover', false);
      });
    }
    this.initIncompatibleWidgetAsTextBox();
  },

  willInsertElement: function() {
    var configConditions = this.get('config.configConditions');
    if (configConditions && configConditions.length) {
      this.configValueObserver();

      //Add Observer to configCondition that depends on another config value
      var isConditionConfigDependent =  configConditions.filterProperty('resource', 'config').length;
      if (isConditionConfigDependent) {
        this.addObserver('config.value', this, this.configValueObserver);
      }
    }
  },

  willDestroyElement: function() {
    if (this.get('config.configConditions')) {
      this.removeObserver('config.value', this, this.configValueObserver);
    }
  },

  configValueObserver: function() {
    var configConditions = this.get('config.configConditions');
    var serviceName = this.get('config.serviceName');
    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName',serviceName).get('configs');
    var isConditionTrue;
    configConditions.forEach(function(configCondition){
      var ifStatement =  configCondition.get("if");
      if (configCondition.get("resource") === 'config') {
        isConditionTrue = App.config.calculateConfigCondition(ifStatement, serviceConfigs);
        if (configCondition.get("type") === 'subsection' || configCondition.get("type") === 'subsectionTab') {
          this.changeSubsectionAttribute(configCondition, isConditionTrue);
        } else {
          this.changeConfigAttribute(configCondition, isConditionTrue);
        }
      } else if (configCondition.get("resource") === 'service') {
        var service = App.Service.find().findProperty('serviceName', ifStatement);
        var serviceName;
        if (service) {
          isConditionTrue = true;
        } else if (!service && this.get('controller.allSelectedServiceNames') && this.get('controller.allSelectedServiceNames').length) {
          isConditionTrue = this.get('controller.allSelectedServiceNames').contains(ifStatement);
        } else {
          isConditionTrue = false;
        }
        this.changeConfigAttribute(configCondition, isConditionTrue);
      }
    }, this);
  },


  /**
   *
   * @param configCondition {App.ThemeCondition}
   * @param isConditionTrue {boolean}
   */
  changeConfigAttribute: function(configCondition, isConditionTrue) {
    var conditionalConfigName = configCondition.get("configName");
    var conditionalConfigFileName = configCondition.get("fileName");
    var serviceName = this.get('config.serviceName');
    var serviceConfigs = this.get('controller.stepConfigs').findProperty('serviceName',serviceName).get('configs');
    var action = isConditionTrue ? configCondition.get("then") : configCondition.get("else");
    var valueAttributes = action.property_value_attributes;
    for (var key in valueAttributes) {
      if (valueAttributes.hasOwnProperty(key)) {
        var valueAttribute = App.StackConfigValAttributesMap[key] || key;
        var conditionalConfig = serviceConfigs.filterProperty('filename',conditionalConfigFileName).findProperty('name', conditionalConfigName);
        if (conditionalConfig) {
          conditionalConfig.set(valueAttribute, valueAttributes[key]);
        }
      }
    }
  },

  /**
   *
   * @param subsectionCondition {App.ThemeCondition}
   * @param isConditionTrue {boolean}
   */
  changeSubsectionAttribute: function(subsectionCondition, isConditionTrue) {
    var subsectionConditionName = subsectionCondition.get('name');
    var action = isConditionTrue ? subsectionCondition.get("then") : subsectionCondition.get("else");
    if (subsectionCondition.get('id')) {
      var valueAttributes = action.property_value_attributes;
      if (valueAttributes && !Em.none(valueAttributes['visible'])) {
        var themeResource;
        if (subsectionCondition.get('type') === 'subsection') {
          themeResource = App.SubSection.find().findProperty('name', subsectionConditionName);
        } else if (subsectionCondition.get('type') === 'subsectionTab') {
          themeResource = App.SubSectionTab.find().findProperty('name', subsectionConditionName);
        }
        themeResource.set('isHiddenByConfig', !valueAttributes['visible']);
        themeResource.get('configs').setEach('hiddenBySection', !valueAttributes['visible']);
      }
    }
  },



  /**
   * set widget value same as config value
   * useful for widgets that work with intermediate config value, not original
   * for now used in slider widget
   * @abstract
   */
  setValue: Em.K,

  /**
   * Config group bound property. Needed for correct render process in template.
   *
   * @returns {App.ConfigGroup|Boolean}
   */
  configGroup: function() {
    return !this.get('config.group') || this.get('config.group.isDefault') ? false : this.get('config.group');
  }.property('config.group.name'),

  /**
   * switcher to display config as widget or text field
   * @method toggleWidgetView
   */
  toggleWidgetView: function() {
    if (!this.get('isWidgetViewAllowed')) {
      return false;
    }
    if (this.get('config.showAsTextBox')) {
      this.textBoxToWidget();
    } else {
      this.widgetToTextBox();
    }
  },

  /**
   * switch display of config to text field
   * @method widgetToTextBox
   */
  widgetToTextBox: function() {
    this.set("config.showAsTextBox", true);
  },

  /**
   * switch display of config to widget
   * @method textBoxToWidget
   */
  textBoxToWidget: function() {
    if (this.isValueCompatibleWithWidget()) {
      this.setValue(this.get('config.value'));
      this.set("config.showAsTextBox", false);
    }
  },

  /**
   * check if config value can be converted to config widget value
   * IMPORTANT! Each config-widget that override this method should use <code>updateWarningsForCompatibilityWithWidget</code>
   * @returns {boolean}
   */
  isValueCompatibleWithWidget: function() {
    return (this.get('isOverrideEqualityError') && !this.get('config.isValid')) || this.get('config.isValid') || !this.get('supportSwitchToTextBox');
  },

  /**
   * Initialize widget with incompatible value as textbox
   */
  initIncompatibleWidgetAsTextBox : function() {
    this.get('config').set('showAsTextBox', !this.isValueCompatibleWithWidget());
  },

  /**
   * Returns <code>true</code> if raw value can be used by widget or widget view is activated.
   * @returns {Boolean}
   */
  isWidgetViewAllowed: function() {
    if (!this.get('config.showAsTextBox')) {
      return true;
    }
    return this.isValueCompatibleWithWidget();
  }.property('config.value', 'config.isFinal', 'config.showAsTextBox'),

  /**
   * Used in <code>isValueCompatibleWithWidget</code>
   * Updates issue-parameters if config is in the raw-mode
   * @param {string} message empty string if value compatible with widget, error-message if value isn't compatible with widget
   * @method updateWarningsForCompatibilityWithWidget
   */
  updateWarningsForCompatibilityWithWidget: function (message) {
    this.setProperties({
      warnMessage: message,
      'config.warnMessage': message,
      issueMessage: message,
      configLabelClass: message ? 'text-warning' : ''
    });
  }

});
