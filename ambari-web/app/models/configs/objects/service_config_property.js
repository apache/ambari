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
 * @class ServiceConfigProperty
 */
App.ServiceConfigProperty = Em.Object.extend({

  name: '',
  displayName: '',

  /**
   * value that is shown on IU
   * and is changing by user
   * @type {String|null}
   */
  value: '',

  /**
   * value that is saved on cluster configs
   * and stored in /api/v1/clusters/{name}/configurations
   * @type {String|null}
   */
  savedValue: null,

  /**
   * value that is returned from server as recommended
   * or stored on stack
   * @type {String|null}
   */
  recommendedValue: null,

  /**
   * initial value of config. if value is saved it will be initial
   * otherwise first recommendedValue will be initial
   * @type {String|null}
   */
  initialValue: null,

  /**
   * value that is shown on IU
   * and is changing by user
   * @type {boolean}
   */
  isFinal: false,

  /**
   * value that is saved on cluster configs api
   * @type {boolean}
   */
  savedIsFinal: null,

  /**
   * value that is returned from server as recommended
   * or stored on stack
   * @type {boolean}
   */
  recommendedIsFinal: null,

  /**
   * @type {boolean}
   */
  supportsFinal: false,

  /**
   * Hint message to display in tooltip. Tooltip will be wrapped on question mark icon.
   * If value is <code>false</code> no tooltip and question mark icon.
   *
   * @type {boolean|string}
   */
  hintMessage: false,

  /**
   * Display label on the right side from input. In general used for checkbox only.
   *
   * @type {boolean}
   */
  rightSideLabel: false,

  /**
   * Text to be shown as placeholder
   * By default savedValue is shown as placeholder
   * @type {String}
   */
  placeholderText: '',

  /**
   * type of widget View
   * @type {string}
   * @default null
   */
  widgetType: null,

  /**
   * Placeholder used for configs with input type text
   */
  placeholder: function () {
    return this.get('placeholderText') || this.get('savedValue');
  }.property('savedValue', 'placeholderText'),

  retypedPassword: '',
  description: '',
  displayType: 'string', // string, digits, number, directories, custom
  unit: '',
  category: 'General',
  isRequired: true, // by default a config property is required
  isReconfigurable: true, // by default a config property is reconfigurable
  isEditable: true, // by default a config property is editable
  isNotEditable: Ember.computed.not('isEditable'),
  hideFinalIcon: function () {
    return (!this.get('isFinal')) && this.get('isNotEditable');
  }.property('isFinal', 'isNotEditable'),
  isVisible: true,
  isMock: false, // mock config created created only to displaying
  isRequiredByAgent: true, // Setting it to true implies property will be stored in configuration
  isSecureConfig: false,
  errorMessage: '',
  warnMessage: '',
  serviceConfig: null, // points to the parent App.ServiceConfig object
  filename: '',
  isOriginalSCP : true, // if true, then this is original SCP instance and its value is not overridden value.
  parentSCP: null, // This is the main SCP which is overridden by this. Set only when isOriginalSCP is false.
  overrides : null,
  overrideValues: [],
  group: null, // Contain group related to this property. Set only when isOriginalSCP is false.
  isUserProperty: null, // This property was added by user. Hence they get removal actions etc.
  isOverridable: true,
  compareConfigs: [],
  isComparison: false,
  hasCompareDiffs: false,
  showLabel: true,
  error: false,
  warn: false,
  previousValue: null, // cached value before changing config <code>value</code>

  /**
   * List of <code>isFinal</code>-values for overrides
   * Set in the controller
   * Should be empty array by default!
   * @type {boolean[]}
   */
  overrideIsFinalValues: [],

  /**
   * true if property has warning or error
   * @type {boolean}
   */
  hasIssues: function () {
    var originalSCPIssued = (this.get('errorMessage') + this.get('warnMessage')) !== "";
    var overridesIssue = false;
    (this.get('overrides') || []).forEach(function(override) {
      if (override.get('errorMessage') + override.get('warnMessage') !== "") {
        overridesIssue = true;
        return;
      }
    });
    return originalSCPIssued || overridesIssue;
  }.property('errorMessage', 'warnMessage', 'overrideErrorTrigger'),

  overrideErrorTrigger: 0, //Trigger for overridable property error
  isRestartRequired: false,
  restartRequiredMessage: 'Restart required',
  index: null, //sequence number in category
  editDone: false, //Text field: on focusOut: true, on focusIn: false
  isNotSaved: false, // user property was added but not saved
  hasInitialValue: false, //if true then property value is defined and saved to server
  isHiddenByFilter: false, //if true then hide this property (filtered out)
  rowStyleClass: null, // CSS-Class to be applied on the row showing this config
  showAsTextBox: false,

  /**
   * config is invisible since wrapper section is hidden
   * @type {boolean}
   */
  hiddenBySection: false,

  /**
   * @type {boolean}
   */
  recommendedValueExists: function () {
    return !Em.isNone(this.get('recommendedValue')) && (this.get('recommendedValue') != "")
      && this.get('isRequiredByAgent') && !this.get('cantBeUndone');
  }.property('recommendedValue'),

  /**
   * Usage example see on <code>App.ServiceConfigRadioButtons.handleDBConnectionProperty()</code>
   *
   * @property {Ember.View} additionalView - custom view related to property
   **/
  additionalView: null,

  /**
   * On Overridable property error message, change overrideErrorTrigger value to recount number of errors service have
   */
  observeErrors: function () {
    this.set("overrideErrorTrigger", this.get("overrideErrorTrigger") + 1);
  }.observes("overrides.@each.errorMessage"),
  /**
   * No override capabilities for fields which are not edtiable
   * and fields which represent master hosts.
   */
  isPropertyOverridable: function () {
    var overrideable = this.get('isOverridable');
    var editable = this.get('isEditable');
    var overrides = this.get('overrides');
    var dt = this.get('displayType');
    return overrideable && (editable || !overrides || !overrides.length) && ("componentHost" != dt);
  }.property('isEditable', 'displayType', 'isOverridable', 'overrides.length'),

  isOverridden: function() {
    var overrides = this.get('overrides');
    return (overrides != null && overrides.get('length')>0) || !this.get('isOriginalSCP');
  }.property('overrides', 'overrides.length', 'isOriginalSCP'),

  isOverrideChanged: function () {
    if (Em.isNone(this.get('overrides')) && this.get('overrideValues.length') === 0) return false;
    return JSON.stringify(this.get('overrides').mapProperty('isFinal')) !== JSON.stringify(this.get('overrideIsFinalValues'))
      || JSON.stringify(this.get('overrides').mapProperty('value')) !== JSON.stringify(this.get('overrideValues'));
  }.property('isOverridden', 'overrides.@each.isNotDefaultValue', 'overrideValues.length'),

  isRemovable: function() {
    var isOriginalSCP = this.get('isOriginalSCP');
    var isUserProperty = this.get('isUserProperty');
    var isRequiredByAgent = this.get('isRequiredByAgent');
    var isEditable = this.get('isEditable');
    var hasOverrides = this.get('overrides.length') > 0;
    // Removable when this is a user property, or it is not an original property and it is editable
    return isEditable && !hasOverrides && isRequiredByAgent && (isUserProperty || !isOriginalSCP);
  }.property('isUserProperty', 'isOriginalSCP', 'overrides.length'),

  init: function () {
    if (Em.isNone(this.get('value'))) {
      if (!Em.isNone(this.get('savedValue'))) {
        this.set('value', this.get('savedValue'));
      } else if (!Em.isNone(this.get('recommendedValue'))) {
        this.set('value', this.get('recommendedValue'));
      }
    }
    if(this.get("displayType") === "password"){
      this.set('retypedPassword', this.get('value'));
      this.set('recommendedValue', '');
    }
    this.set('initialValue', this.get('value'));
    this.updateDescription();
  },

  /**
   * Indicates when value is not the default value.
   * Returns false when there is no default value.
   */
  isNotDefaultValue: function () {
    var value = this.get('value');
    var savedValue = this.get('savedValue');
    var supportsFinal = this.get('supportsFinal');
    var isFinal = this.get('isFinal');
    var savedIsFinal = this.get('savedIsFinal');
    // ignore precision difference for configs with type of `float` which value may ends with 0
    // e.g. between 0.4 and 0.40
    if (this.get('stackConfigProperty') && this.get('stackConfigProperty.valueAttributes.type') == 'float') {
      savedValue = !Em.isNone(savedValue) ? '' + parseFloat(savedValue) : null;
      value = '' + parseFloat(value);
    }
    return (savedValue != null && value !== savedValue) || (supportsFinal && !Em.isNone(savedIsFinal) && isFinal !== savedIsFinal);
  }.property('value', 'savedValue', 'isEditable', 'isFinal', 'savedIsFinal'),

  /**
   * Don't show "Undo" for hosts on Installer Step7
   */
  cantBeUndone: function() {
    return ["componentHost", "componentHosts", "radio button"].contains(this.get('displayType'));
  }.property('displayType'),

  /**
   * Used in <code>templates/common/configs/service_config_category.hbs</code>
   * @type {boolean}
   */
  undoAvailable: function () {
    return !this.get('cantBeUndone') && this.get('isNotDefaultValue');
  }.property('cantBeUndone', 'isNotDefaultValue'),

  /**
   * Used in <code>templates/common/configs/service_config_category.hbs</code>
   * @type {boolean}
   */
  removeAvailable: function () {
    return this.get('isRemovable') && !this.get('isComparison');
  }.property('isComparison', 'isRemovable'),

  /**
   * Used in <code>templates/common/configs/service_config_category.hbs</code>
   * @type {boolean}
   */
  switchGroupAvailable: function () {
    return !this.get('isEditable') && this.get('group');
  }.property('isEditable', 'group'),

  /**
   * Used in <code>templates/common/configs/service_config_category.hbs</code>
   * @type {boolean}
   */
  setRecommendedAvailable: function () {
    return this.get('isEditable') && this.get('recommendedValueExists');
  }.property('isEditable', 'recommendedValueExists'),

  /**
   * Used in <code>templates/common/configs/service_config_category.hbs</code>
   * @type {boolean}
   */
  overrideAvailable: function () {
    return !this.get('isComparison') && this.get('isPropertyOverridable') && (this.get('displayType') !== 'password');
  }.property('isPropertyOverridable', 'isComparison'),

  isValid: function () {
    return this.get('errorMessage') === '';
  }.property('errorMessage'),

  viewClass: function () {
    switch (this.get('displayType')) {
      case 'checkbox':
      case 'boolean':
        if (this.get('dependentConfigPattern')) {
          return App.ServiceConfigCheckboxWithDependencies;
        } else {
          return App.ServiceConfigCheckbox;
        }
      case 'password':
        return App.ServiceConfigPasswordField;
      case 'combobox':
        return App.ServiceConfigComboBox;
      case 'radio button':
        return App.ServiceConfigRadioButtons;
        break;
      case 'directories':
        return App.ServiceConfigTextArea;
        break;
      case 'directory':
        return App.ServiceConfigTextField;
        break;
      case 'content':
        return App.ServiceConfigTextAreaContent;
        break;
      case 'multiLine':
        return App.ServiceConfigTextArea;
        break;
      case 'custom':
        return App.ServiceConfigBigTextArea;
      case 'componentHost':
        return App.ServiceConfigMasterHostView;
      case 'label':
        return App.ServiceConfigLabelView;
      case 'componentHosts':
        return App.ServiceConfigComponentHostsView;
      case 'supportTextConnection':
        return App.checkConnectionView;
      case 'capacityScheduler':
        return App.CapacitySceduler;
      default:
        if (this.get('unit')) {
          return App.ServiceConfigTextFieldWithUnit;
        } else {
          return App.ServiceConfigTextField;
        }
    }
  }.property('displayType'),

  validate: function () {
    var value = this.get('value');
    var supportsFinal = this.get('supportsFinal');
    var isFinal = this.get('isFinal');
    var valueRange = this.get('valueRange');

    var isError = false;
    var isWarn = false;

    if (typeof value === 'string' && value.length === 0) {
      if (this.get('isRequired')) {
        this.set('errorMessage', 'This is required');
        isError = true;
      } else {
        return;
      }
    }

    if (!isError) {
      switch (this.get('displayType')) {
        case 'int':
          if (('' + value).trim().length === 0) {
            this.set('errorMessage', '');
            isError = false;
            return;
          }
          if (validator.isConfigValueLink(value)) {
            isError = false;
          } else if (!validator.isValidInt(value)) {
            this.set('errorMessage', 'Must contain digits only');
            isError = true;
          } else {
            if(valueRange){
              if(value < valueRange[0] || value > valueRange[1]){
                this.set('errorMessage', 'Must match the range');
                isError = true;
              }
            }
          }
          break;
        case 'float':
          if (validator.isConfigValueLink(value)) {
            isError = false;
          } else if (!validator.isValidFloat(value)) {
            this.set('errorMessage', 'Must be a valid number');
            isError = true;
          }
          break;
        case 'checkbox':
          break;
        case 'directories':
        case 'directory':
          if (this.get('configSupportHeterogeneous')) {
            if (!validator.isValidDataNodeDir(value)) {
              this.set('errorMessage', 'dir format is wrong, can be "[{storage type}]/{dir name}"');
              isError = true;
            }
          } else {
            if (!validator.isValidDir(value)) {
              this.set('errorMessage', 'Must be a slash or drive at the start');
              isError = true;
            }
          }
          if (!isError) {
            if (!validator.isAllowedDir(value)) {
              this.set('errorMessage', 'Can\'t start with "home(s)"');
              isError = true;
            } else {
              // Invalidate values which end with spaces.
              if (value !== ' ' && validator.isNotTrimmedRight(value)) {
                this.set('errorMessage', Em.I18n.t('form.validator.error.trailingSpaces'));
                isError = true;
              }
            }
          }
          break;
        case 'custom':
          break;
        case 'email':
          if (!validator.isValidEmail(value)) {
            this.set('errorMessage', 'Must be a valid email address');
            isError = true;
          }
          break;
        case 'supportTextConnection':
        case 'host':
          var connectionProperties = ['kdc_host'];
          var hiveOozieHostNames = ['hive_hostname','oozie_hostname'];
          if(hiveOozieHostNames.contains(this.get('name'))) {
            if (validator.hasSpaces(value)) {
              this.set('errorMessage', Em.I18n.t('host.spacesValidation'));
              isError = true;
            }
          } else {
            if ((validator.isNotTrimmed(value) && connectionProperties.contains(this.get('name')) || validator.isNotTrimmed(value))) {
              this.set('errorMessage', Em.I18n.t('host.trimspacesValidation'));
              isError = true;
            }
          }
          break;
        case 'password':
          // retypedPassword is set by the retypePasswordView child view of App.ServiceConfigPasswordField
          if (value !== this.get('retypedPassword')) {
            this.set('errorMessage', 'Passwords do not match');
            isError = true;
          }
          break;
        case 'user':
        case 'database':
        case 'db_user':
          if (!validator.isValidDbName(value)){
            this.set('errorMessage', 'Value is not valid');
            isError = true;
          }
          break;
        case 'multiLine':
        case 'content':
        default:
          if(this.get('name')=='javax.jdo.option.ConnectionURL' || this.get('name')=='oozie.service.JPAService.jdbc.url') {
            if (validator.isConfigValueLink(value)) {
              isError = false;
            } else if (validator.isNotTrimmed(value)) {
              this.set('errorMessage', Em.I18n.t('host.trimspacesValidation'));
              isError = true;
            }
          } else {
            // Avoid single space values which is work around for validate empty properties.
            // Invalidate values which end with spaces.
            if (value !== ' ' && validator.isNotTrimmedRight(value)) {
              this.set('errorMessage', Em.I18n.t('form.validator.error.trailingSpaces'));
              isError = true;
            }
          }
          break;
      }
    }

    if (!isWarn || isError) { // Errors get priority
      this.set('warnMessage', '');
      this.set('warn', false);
    } else {
      this.set('warn', true);
    }

    if (!isError) {
      this.set('errorMessage', '');
      this.set('error', false);
    } else {
      this.set('error', true);
    }
  }.observes('value', 'isFinal', 'retypedPassword'),

  /**
   * defines specific directory properties that
   * allows setting drive type before dir name
   * ex: [SSD]/usr/local/my_dir
   * @param config
   * @returns {*|Boolean|boolean}
   */
  configSupportHeterogeneous: function() {
    if (App.get('isHadoop22Stack')) {
      return ['directories', 'directory'].contains(this.get('displayType')) && ['dfs.datanode.data.dir'].contains(this.get('name'));
    } else {
      return false;
    }
  }.property('displayType', 'name', 'App.isHadoop22Stack'),

  /**
   * Update description for `password`-config
   * Add extra-message about their comparison
   *
   * @method updateDescription
   */
  updateDescription: function () {
    var description = this.get('description');
    var displayType = this.get('displayType');
    var additionalDescription = Em.I18n.t('services.service.config.password.additionalDescription');
    if ('password' === displayType) {
      if (description) {
        if (!description.contains(additionalDescription)) {
          description += '<br />' + additionalDescription;
        }
      } else {
        description = additionalDescription;
      }
    }
    this.set('description', description);
  }

});
