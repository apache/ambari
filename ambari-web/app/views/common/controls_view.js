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

var dbInfo = require('data/db_properties_info') || {};

var delay = (function(){
  var timer = 0;
  return function(callback, ms){
    clearTimeout (timer);
    timer = setTimeout(callback, ms);
  };
})();

/**
 * Abstract view for config fields.
 * Add popover support to control
 */
App.ServiceConfigPopoverSupport = Ember.Mixin.create({

  /**
   * Config object. It will instance of App.ServiceConfigProperty
   */
  serviceConfig: null,
  attributeBindings:['readOnly'],
  isPopoverEnabled: true,
  popoverPlacement: 'right',

  didInsertElement: function () {
    App.tooltip($('body'), {
      selector: '[data-toggle=tooltip]',
      placement: 'top'
    });
    // if description for this serviceConfig not exist, then no need to show popover
    if (this.get('isPopoverEnabled') !== 'false' && this.get('serviceConfig.description')) {
      App.popover(this.$(), {
        title: Em.I18n.t('installer.controls.serviceConfigPopover.title').format(
          this.get('serviceConfig.displayName'),
          (this.get('serviceConfig.displayName') == this.get('serviceConfig.name')) ? '' : this.get('serviceConfig.name')
        ),
        content: this.get('serviceConfig.description'),
        placement: this.get('popoverPlacement'),
        trigger: 'hover'
      });
    }
  },

  willDestroyElement: function() {
    this.$().popover('destroy');
  },

  readOnly: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')
});

App.SupportsDependentConfigs = Ember.Mixin.create({

  /**
   * do not apply recommended value if user change value by himself.
   */
  keyUp: function() {
    this.get('controller').removeCurrentFromDependentList(this.get('serviceConfig') || this.get('config'));
  },

  /**
   * method send request to check if some of dependent configs was changes
   * and in case there was changes shows popup with info about changed configs
   *
   * @param {App.ServiceConfigProperty} config
   * @returns {$.Deferred}
   */
  sendRequestRorDependentConfigs: function(config) {
    if (!config || !config.get('isValid')) return $.Deferred().resolve().promise();
    if (['mainServiceInfoConfigsController','wizardStep7Controller'].contains(this.get('controller.name'))) {
      var name = config.get('name');
      var saveRecommended = (this.get('config.value') === this.get('config.recommendedValue'));
      var controller = this.get('controller');
      var type = App.config.getConfigTagFromFileName(config.get('filename'));
      var p = App.StackConfigProperty.find(App.config.configId(name, type));
       if ((p && p.get('propertyDependedBy.length') > 0 || p.get('displayType') === 'user') && config.get('oldValue') !== config.get('value')) {
         var old = config.get('oldValue');
         config.set('oldValue', config.get('value'));
         return controller.getRecommendationsForDependencies([{
           "type": type,
           "name": name,
           "old_value": Em.isNone(old) ? config.get('initialValue') : old
         }], false, function() {
           controller.removeCurrentFromDependentList(config, saveRecommended);
         });
      } else {
        controller.removeCurrentFromDependentList(config, saveRecommended);
      }
    }

    return $.Deferred().resolve().promise();
  },

  /**
   * Restore values for dependent configs by parent config info.
   * NOTE: If dependent config inherited from multiply configs its
   * value will be restored only when all parent configs are being restored.
   *
   * @param {App.ServiceConfigProperty} parentConfig
   */
  restoreDependentConfigs: function(parentConfig) {
    var controller = this.get('controller');
    var dependentConfigs = controller.get('_dependentConfigValues');
    if (controller.updateDependentConfigs) {
      controller.updateDependentConfigs();
      controller.set('_dependentConfigValues', dependentConfigs.reject(function(item) {
        if (item.parentConfigs.contains(parentConfig.get('name'))) {
          if (item.parentConfigs.length > 1) {
            item.parentConfigs.removeObject(parentConfig.get('name'));
          } else {
            // reset property value
            var property = controller.findConfigProperty(item.propertyName, App.config.getOriginalFileName(item.fileName));
            if (property) {
              property.set('value', property.get('savedValue') || property.get('initialValue'));
            }
            return true;
          }
        }
        return false;
      }));
    }
  }

});

/**
 * mixin set class that serve as unique element identifier,
 * id not used in order to avoid collision with ember ids
 */
App.ServiceConfigCalculateId = Ember.Mixin.create({
  idClass: Ember.computed(function () {
    var config = this.get('config') && this.get('config.widget') ? this.get('config') : this.get('serviceConfig') || {};
    var label = Em.get(config, 'name') ? Em.get(config, 'name').toLowerCase().replace(/\./g, '-') : '',
        fileName = Em.get(config, 'filename') ? Em.get(config, 'filename').toLowerCase().replace(/\./g, '-') : '',
        group = Em.get(config, 'group.name') || 'default',
        isOrigin = Em.getWithDefault(config, 'compareConfigs.length', 0) > 0 ? '-origin' : '';
    return 'service-config-' + label + '-' + fileName + '-' + group + isOrigin;
  }),
  classNameBindings: 'idClass'
});

/**
 * Default input control
 * @type {*}
 */
App.ServiceConfigTextField = Ember.TextField.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {

  valueBinding: 'serviceConfig.value',
  classNameBindings: 'textFieldClassName',
  placeholderBinding: 'serviceConfig.savedValue',

  onValueUpdate: function () {
    var self = this;
    delay(function(){
      self.sendRequestRorDependentConfigs(self.get('serviceConfig'));
    }, 500);
  }.observes('serviceConfig.value'),

  //Set editDone true for last edited config text field parameter
  focusOut: function () {
    this.get('serviceConfig').set("editDone", true);
  },
  //Set editDone false for all current category config text field parameter
  focusIn: function () {
    if (!this.get('serviceConfig.isOverridden') && !this.get('serviceConfig.isComparison')) {
      if (this.get('parentView.categoryConfigsAll')) {
        this.get("parentView.categoryConfigsAll").setEach("editDone", false);
      }
    }
  },

  textFieldClassName: function () {
    if (this.get('serviceConfig.unit')) {
      return ['input-small'];
    } else if (this.get('serviceConfig.displayType') === 'principal') {
      return ['span12'];
    } else {
      return ['span9'];
    }
  }.property('serviceConfig.displayType', 'serviceConfig.unit')

});

/**
 * Customized input control with Units type specified
 * @type {Em.View}
 */
App.ServiceConfigTextFieldWithUnit = Ember.View.extend(App.ServiceConfigPopoverSupport, App.SupportsDependentConfigs, {
  valueBinding: 'serviceConfig.value',
  classNames: ['input-append', 'with-unit'],
  placeholderBinding: 'serviceConfig.savedValue',

  onValueUpdate: function () {
    var self = this;
    delay(function(){
      self.sendRequestRorDependentConfigs(self.get('serviceConfig'));
    }, 500);
  }.observes('serviceConfig.value'),

  templateName: require('templates/wizard/controls_service_config_textfield_with_unit')
});

/**
 * Password control
 * @type {*}
 */
App.ServiceConfigPasswordField = Ember.TextField.extend({

  serviceConfig: null,
  type: 'password',
  attributeBindings:['readOnly'],
  valueBinding: 'serviceConfig.value',
  classNames: [ 'span4' ],
  placeholder: Em.I18n.t('form.item.placeholders.typePassword'),

  template: Ember.Handlebars.compile('{{view view.retypePasswordView}}'),

  keyPress: function (event) {
    if (event.keyCode == 13) {
      return false;
    }
  },

  retypePasswordView: Ember.TextField.extend({
    placeholder: Em.I18n.t('form.passwordRetype'),
    attributeBindings:['readOnly'],
    type: 'password',
    classNames: [ 'span4', 'retyped-password' ],
    keyPress: function (event) {
      if (event.keyCode == 13) {
        return false;
      }
    },
    valueBinding: 'parentView.serviceConfig.retypedPassword',
    readOnly: function () {
      return !this.get('parentView.serviceConfig.isEditable');
    }.property('parentView.serviceConfig.isEditable')
  }),

  readOnly: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')

});

/**
 * Textarea control
 * @type {*}
 */
App.ServiceConfigTextArea = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {


  onValueUpdate: function () {
    var self = this;
    delay(function(){
      self.sendRequestRorDependentConfigs(self.get('serviceConfig'));
    }, 500);
  }.observes('serviceConfig.value'),

  valueBinding: 'serviceConfig.value',
  rows: 4,
  classNames: ['directories'],
  classNameBindings: ['widthClass'],
  widthClass: 'span9'
});

/**
 * Textarea control for content type
 * @type {*}
 */
App.ServiceConfigTextAreaContent = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {

  valueBinding: 'serviceConfig.value',
  rows: 20,
  classNames: ['span10']
});

/**
 * Textarea control with bigger height
 * @type {*}
 */
App.ServiceConfigBigTextArea = App.ServiceConfigTextArea.extend(App.ServiceConfigCalculateId, {
  rows: 10
});

/**
 * Checkbox control
 * @type {*}
 */
App.ServiceConfigCheckbox = Ember.Checkbox.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {

  allowedPairs: {
    'trueFalse': ["true", "false"],
    'YesNo': ["Yes", "No"],
    'YESNO': ["YES", "NO"],
    'yesNo': ["yes", "no"]
  },

  trueValue: true,
  falseValue: false,

  checked: false,

  /**
   * set appropriate config values pair
   * to define which value is positive (checked) property
   * and what value is negative (unchecked) proeprty
   */
  didInsertElement: function() {
    this._super();
    this.addObserver('serviceConfig.value', this, 'toggleChecker');
    Object.keys(this.get('allowedPairs')).forEach(function(key) {
      if (this.get('allowedPairs')[key].contains(this.get('serviceConfig.value'))) {
        this.set('trueValue', this.get('allowedPairs')[key][0]);
        this.set('falseValue', this.get('allowedPairs')[key][1]);
      }
    }, this);
    this.set('checked', this.get('serviceConfig.value') === this.get('trueValue'))
  },

  willDestroyElement: function() {
    this.removeObserver('serviceConfig.value', this, 'checkedBinding');
  },

  /***
   * defines if checkbox value appropriate to the config value
   * @returns {boolean}
   */
  isNotAppropriateValue: function() {
    return this.get('serviceConfig.value') !== this.get(this.get('checked') + 'Value');
  },

  /**
   * change service config value if click on checkbox
   */
  toggleValue: function() {
    if (this.isNotAppropriateValue()){
      this.set('serviceConfig.value', this.get(this.get('checked') + 'Value'));
      this.get('serviceConfig').set("editDone", true);
      this.sendRequestRorDependentConfigs(this.get('serviceConfig'));
    }
  }.observes('checked'),

  /**
   * change checkbox value if click on undo
   */
  toggleChecker: function() {
    if (this.isNotAppropriateValue())
      this.set('checked', !this.get('checked'));
  },

  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable'),

  //Set editDone false for all current category config text field parameter
  focusIn: function (event) {
    if (!this.get('serviceConfig.isOverridden') && !this.get('serviceConfig.isComparison')) {
      this.get("parentView.categoryConfigsAll").setEach("editDone", false);
    }
  }
});

/**
 * Checkbox control which can hide or show dependent  properties
 * @type {*|void}
 */
App.ServiceConfigCheckboxWithDependencies = App.ServiceConfigCheckbox.extend({

  toggleDependentConfigs: function() {
    if (this.get('serviceConfig.dependentConfigPattern')) {
      if (this.get('serviceConfig.dependentConfigPattern') === "CATEGORY") {
        this.disableEnableCategoryConfigs();
      } else {
        this.showHideDependentConfigs();
      }
    }
  }.observes('checked'),

  disableEnableCategoryConfigs: function () {
    this.get('categoryConfigsAll').setEach('isEditable', this.get('checked'));
    this.set('serviceConfig.isEditable', true);
  },

  showHideDependentConfigs: function () {
    this.get('categoryConfigsAll').forEach(function (c) {
      if (c.get('name').match(this.get('serviceConfig.dependentConfigPattern')) && c.get('name') != this.get('serviceConfig.name'))
        c.set('isVisible', this.get('checked'))
    }, this);
  }
});

App.ServiceConfigRadioButtons = Ember.View.extend(App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {
  templateName: require('templates/wizard/controls_service_config_radio_buttons'),

  didInsertElement: function () {
    // on page render, automatically populate JDBC URLs only for default database settings
    // so as to not lose the user's customizations on these fields
    if (['addServiceController', 'installerController'].contains(this.get('controller.wizardController.name')) && !App.StackService.find(this.get('serviceConfig.serviceName')).get('isInstalled')) {
      if (this.get('isNewDb') || this.get('dontUseHandleDbConnection').contains(this.get('serviceConfig.name'))) {
        this.onOptionsChange();
      } else {
        if ((App.get('isHadoopWindowsStack') && this.get('inMSSQLWithIA')) || this.get('serviceConfig.name') === 'DB_FLAVOR') {
          this.onOptionsChange();
        }
        this.handleDBConnectionProperty();
      }
    }
  },

  /**
   * Radio buttons that are not DB options and should not trigger any observer or change any other property's value
   * Ranger service -> "Authentication method" property is an example for non DB related radio button
   */
  nonDBRadioButtons: function() {
    return this.get('dontUseHandleDbConnection').without('DB_FLAVOR');
  }.property('dontUseHandleDbConnection'),

  /**
   * properties with these names don'use handleDBConnectionProperty method
   */
  dontUseHandleDbConnection: function () {
    var version = App.get('currentStackVersion').match(/(\d+)[\.,]?(\d+)?/),
      majorVersion = version?version[1]: 0,
      minorVersion = version? version[2]: 0;
    // functionality added in HDP 2.3
    // remove DB_FLAVOR so it can handle DB Connection checks
    if (App.get('currentStackName') == 'HDP' && majorVersion >= 2  && minorVersion>= 3) {
      return ['ranger.authentication.method'];
    }
    return ['DB_FLAVOR', 'authentication_method'];
  }.property('App.currentStackName'),

  serviceConfig: null,
  categoryConfigsAll: null,

  /**
   * defines if new db is selected;
   * @type {boolean}
   */
  isNewDb: function() {
    return /New /g.test(this.get('serviceConfig.value'));
  }.property('serviceConfig.serviceName', 'serviceConfig.value'),

  /**
   * defines if 'Existing MSSQL Server database with integrated authentication' is selected
   * in this case some properties can have different behaviour
   * @type {boolean}
   */
  inMSSQLWithIA: function() {
    return this.get('serviceConfig.value') === 'Existing MSSQL Server database with integrated authentication';
  }.property('serviceConfig.value'),

  /**
   * Radio button has very uncomfortable values for managing it's state
   * so it's better to use code values that easier to manipulate. Ex:
   * "Existing MySQL Database" transforms to "MYSQL"
   * @type {string}
   */
  getDbTypeFromRadioValue: function() {
    var currentValue = this.get('serviceConfig.value');
    var databases = /MySQL|Postgres|Oracle|Derby|MSSQL|SQLA/gi;
    if (this.get('inMSSQLWithIA')) {
      return 'MSSQL2';
    } else {
      var matches = currentValue.match(databases);
      if (matches) {
        return currentValue.match(databases)[0].toUpperCase();
      } else {
        return "MYSQL";
      }
    }
  }.property('serviceConfig.serviceName', 'serviceConfig.value'),

  onOptionsChange: function () {
    this.sendRequestRorDependentConfigs(this.get('serviceConfig'));
    if (this.get('hostNameProperty') && !this.get('nonDBRadioButtons').contains(this.get('serviceConfig.name'))) {
      /** if new db is selected host name must be same as master of selected service (and can't be changed)**/
      if (this.get('isNewDb')) {
        var initProperty = this.get('hostNameProperty.recommendedValue') || this.get('hostNameProperty.savedValue');
        this.get('hostNameProperty').set('value', initProperty.toString());
        this.get('hostNameProperty').set('isEditable', false);
      } else {
        this.get('hostNameProperty').set('isEditable', true);
      }
      this.setRequiredProperties(['driver', 'sql_jar_connector', 'db_type']);
      if (this.getPropertyByType('connection_url')) {
        this.setConnectionUrl(this.get('hostNameProperty.value'), this.get('databaseProperty.value'), this.get('userProperty.value'), this.get('passwordProperty.value'));
      }
      this.handleSpecialUserPassProperties();
    }
  }.observes('databaseProperty.value', 'hostNameProperty.value', 'serviceConfig.value', 'userProperty.value', 'passwordProperty.value'),

  nameBinding: 'serviceConfig.radioName',

  /**
   * Just property object for database name
   * @type {App.ServiceConfigProperty}
   */
  databaseProperty: function () {
    return this.getPropertyByType('db_name');
  }.property('serviceConfig.serviceName'),

  /**
   * Just property object for host name
   * @type {App.ServiceConfigProperty}
   */
  hostNameProperty: function () {
    var host = this.getPropertyByType('host_name');
    if (host && !host.get('value')) {
      if (host.get('savedValue')) {
        host.set('value', host.get('savedValue'));
      } else if (host.get('recommendedValue')) {
        host.set('value', host.get('recommendedValue'));
      }
    }
    return host;
  }.property('serviceConfig.serviceName', 'serviceConfig.value'),

  /**
   * Just property object for database name
   * @type {App.ServiceConfigProperty}
   */
  userProperty: function () {
    return this.getPropertyByType('user_name');
  }.property('serviceConfig.serviceName'),

  /**
   * Just property object for database name
   * @type {App.ServiceConfigProperty}
   */
  passwordProperty: function () {
    return this.getPropertyByType('password');
  }.property('serviceConfig.serviceName'),

  /**
   *
   * @param propertyType
   * @returns {*}
   */
  getDefaultPropertyValue: function(propertyType) {
    var dbProperties = dbInfo.dpPropertiesMap[this.get('getDbTypeFromRadioValue')],
      serviceName = this.get('serviceConfig.serviceName');
    return dbProperties[serviceName] && dbProperties[serviceName][propertyType]
      ? dbProperties[serviceName][propertyType] : dbProperties[propertyType];
  },

  /**
   *
   * @param propertyType
   * @returns {*|Object}
   */
  getPropertyByType: function(propertyType) {
    if (dbInfo.dpPropertiesByServiceMap[this.get('serviceConfig.serviceName')]) {
      //@TODO: dbInfo.dpPropertiesByServiceMap has corresponding property name but does not have filenames with it. this can cause issue when there are multiple db properties with same name belonging to different files
      /** check if selected service has db properties**/
      return this.get('parentView.serviceConfigs').findProperty('name', dbInfo.dpPropertiesByServiceMap[this.get('serviceConfig.serviceName')][propertyType]);
    }
    return null;
  },

  /**
   * This method update <code>connection_url<code> property, using template described in <code>dpPropertiesMap<code>
   * and sets hostName as dbName in appropriate position of <code>connection_url<code> string
   * @param {String} hostName
   * @param {String} dbName
   * @param {String} user
   * @param {String} password
   * @method setConnectionUrl
   */
  setConnectionUrl: function(hostName, dbName, user, password) {
    var connectionUrlProperty = this.getPropertyByType('connection_url');
    var connectionUrlTemplate = this.getDefaultPropertyValue('connection_url');
    try {
      var connectionUrlValue = connectionUrlTemplate.format(hostName, dbName, user, password);
      connectionUrlProperty.set('value', connectionUrlValue);
      connectionUrlProperty.set('recommendedValue', connectionUrlValue);
    } catch(e) {
      console.error('connection url property or connection url template is missing');
    }
    return connectionUrlProperty;
  },

  /**
   * This method sets recommended values for properties <code>propertiesToUpdate<code> when radio button is changed
   * @param {String[]} propertiesToUpdate - contains type of properties that should be updated;
   * @method setRequiredProperties
   * @returns App.ServiceConfigProperty[]
   */
  setRequiredProperties: function (propertiesToUpdate) {
    propertiesToUpdate.forEach(function(pType) {
      var property = this.getPropertyByType(pType);
      var value = this.getDefaultPropertyValue(pType);
      if (property && value) {
        property.set('value', value);
        property.set('recommendedValue', value);
      }
    }, this);
  },

  /**
   * This method hides properties <code>user_name<code> and <code>password<code> in case selected db is
   * "Existing MSSQL Server database with integrated authentication" or similar
   * @method handleSpecialUserPassProperties
   */
  handleSpecialUserPassProperties: function() {
    ['user_name', 'password'].forEach(function(pType) {
      var property = this.getPropertyByType(pType);
      if (property) {
        property.setProperties({
          'isVisible': !this.get('inMSSQLWithIA'),
          'isRequired': !this.get('inMSSQLWithIA')
        });
      }
    }, this);
  },

  /**
   * `Observer` that add <code>additionalView</code> to <code>App.ServiceConfigProperty</code>
   * that responsible for (if existing db selected)
   * 1. checking database connection
   * 2. showing jdbc driver setup warning msg.
   *
   * @method handleDBConnectionProperty
   **/
  handleDBConnectionProperty: function() {
    if (this.get('dontUseHandleDbConnection').contains(this.get('serviceConfig.name'))) {
      return;
    }
    var handledProperties = ['oozie_database', 'hive_database', 'DB_FLAVOR'];
    var currentValue = this.get('serviceConfig.value');
    var databases = /MySQL|PostgreSQL|Postgres|Oracle|Derby|MSSQL|SQLA/gi;
    var currentDB = currentValue.match(databases)[0];
    var databasesTypes = /MySQL|Postgres|Oracle|Derby|MSSQL|SQLA/gi;
    var currentDBType = currentValue.match(databasesTypes)[0];
    var checkDatabase = /existing/gi.test(currentValue);
    // db connection check button show up if existed db selected
    var propertyAppendTo1 = this.get('categoryConfigsAll').findProperty('displayName', 'Database URL');
    // warning msg under database type radio buttons, to warn the user to setup jdbc driver if existed db selected
    var propertyHive = this.get('categoryConfigsAll').findProperty('displayName', 'Hive Database');
    var propertyOozie = this.get('categoryConfigsAll').findProperty('displayName', 'Oozie Database');
    var propertyAppendTo2 = propertyHive ? propertyHive : propertyOozie;
    // RANGER specific
    if (this.get('serviceConfig.serviceName') === 'RANGER') {
      propertyAppendTo1 = this.get('categoryConfigsAll').findProperty('name', 'ranger.jpa.jdbc.url');
      propertyAppendTo2 = this.get('categoryConfigsAll').findProperty('name', 'DB_FLAVOR');
      // check for all db types when installing Ranger - not only for existing ones
      checkDatabase = true;
    }
    if (propertyAppendTo1) {
      propertyAppendTo1.set('additionalView', null);
    }
    if (propertyAppendTo2) {
      propertyAppendTo2.set('additionalView', null);
    }
    var shouldAdditionalViewsBeSet = currentDB && checkDatabase && handledProperties.contains(this.get('serviceConfig.name')),
      driver = this.getDefaultPropertyValue('sql_jar_connector') ? this.getDefaultPropertyValue('sql_jar_connector').split("/").pop() : 'driver.jar',
      dbType = this.getDefaultPropertyValue('db_type'),
      additionalView1 = shouldAdditionalViewsBeSet ? App.CheckDBConnectionView.extend({databaseName: dbType}) : null,
      additionalView2 = shouldAdditionalViewsBeSet ? Ember.View.extend({
        template: Ember.Handlebars.compile('<div class="alert">{{{view.message}}}</div>'),
        message: function() {
          return Em.I18n.t('services.service.config.database.msg.jdbcSetup').format(dbType, driver);
        }.property()
      }) : null;
    if (propertyAppendTo1) {
      Em.run.next(function () {
        propertyAppendTo1.set('additionalView', additionalView1);
      });
    }
    if (propertyAppendTo2) {
      Em.run.next(function () {
        propertyAppendTo2.set('additionalView', additionalView2);
      });
    }
  }.observes('serviceConfig.value'),

  optionsBinding: 'serviceConfig.options'
});

App.ServiceConfigRadioButton = Ember.Checkbox.extend({
  tagName: 'input',
  attributeBindings: ['type', 'name', 'value', 'checked', 'disabled'],
  checked: false,
  type: 'radio',
  name: null,
  value: null,

  didInsertElement: function () {
    console.debug('App.ServiceConfigRadioButton.didInsertElement');
    if (this.get('parentView.serviceConfig.value') === this.get('value')) {
      console.debug(this.get('name') + ":" + this.get('value') + ' is checked');
      this.set('checked', true);
    }
  },

  click: function () {
    this.set('checked', true);
    console.debug('App.ServiceConfigRadioButton.click');
    this.onChecked();
  },

  onChecked: function () {
    // Wrapping the call with Ember.run.next prevents a problem where setting isVisible on component
    // causes JS error due to re-rendering.  For example, this occurs when switching the Config Group
    // in Service Config page
    Em.run.next(this, function() {
      console.debug('App.ServiceConfigRadioButton.onChecked');
      this.set('parentView.serviceConfig.value', this.get('value'));
      var components = this.get('parentView.serviceConfig.options');
      if (components && components.someProperty('foreignKeys')) {
        this.get('controller.stepConfigs').findProperty('serviceName', this.get('parentView.serviceConfig.serviceName')).propertyDidChange('errorCount');
      }
    });
  }.observes('checked'),

  disabled: function () {
    return !this.get('parentView.serviceConfig.isEditable') ||
      !['addServiceController', 'installerController'].contains(this.get('controller.wizardController.name')) && /^New\s\w+\sDatabase$/.test(this.get('value'));
  }.property('parentView.serviceConfig.isEditable')
});

App.ServiceConfigComboBox = Ember.Select.extend(App.ServiceConfigPopoverSupport, App.ServiceConfigCalculateId, App.SupportsDependentConfigs, {
  contentBinding: 'serviceConfig.options',
  selectionBinding: 'serviceConfig.value',
  placeholderBinding: 'serviceConfig.savedValue',
  classNames: [ 'span3' ]
});


/**
 * Base component for host config with popover support
 */
App.ServiceConfigHostPopoverSupport = Ember.Mixin.create({

  /**
   * Config object. It will instance of App.ServiceConfigProperty
   */
  serviceConfig: null,

  didInsertElement: function () {
    App.popover(this.$(), {
      title: this.get('serviceConfig.displayName'),
      content: this.get('serviceConfig.description'),
      placement: 'right',
      trigger: 'hover'
    });
  }
});

/**
 * Master host component.
 * Show hostname without ability to edit it
 * @type {*}
 */
App.ServiceConfigMasterHostView = Ember.View.extend(App.ServiceConfigHostPopoverSupport, App.ServiceConfigCalculateId, {

  classNames: ['master-host', 'span6'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('{{value}}')

});

/**
 * text field property view that enables possibility
 * for check connection
 * @type {*}
 */
App.checkConnectionView = App.ServiceConfigTextField.extend({
  didInsertElement: function() {
    this._super();
    var kdc = this.get('categoryConfigsAll').findProperty('name', 'kdc_type');
    var propertyAppendTo = this.get('categoryConfigsAll').findProperty('name', 'domains');
    if (propertyAppendTo) {
      try {
        propertyAppendTo.set('additionalView', App.CheckDBConnectionView.extend({databaseName: kdc && kdc.get('value')}));
      } catch (e) {
        console.error('error while adding "Test connection button"');
      }
    }
  }
});

/**
 * Show value as plain label in italics
 * @type {*}
 */
App.ServiceConfigLabelView = Ember.View.extend(App.ServiceConfigHostPopoverSupport, App.ServiceConfigCalculateId, {

  classNames: ['master-host', 'span6'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('<i>{{view.value}}</i>')
});

/**
 * Base component to display Multiple hosts
 * @type {*}
 */
App.ServiceConfigMultipleHostsDisplay = Ember.Mixin.create(App.ServiceConfigHostPopoverSupport, App.ServiceConfigCalculateId, {

  hasNoHosts: function () {
    console.log('view', this.get('viewName')); //to know which View cause errors
    console.log('controller', this.get('controller').name); //should be slaveComponentGroupsController
    if (!this.get('value')) {
      return true;
    }
    return this.get('value').length === 0;
  }.property('value'),

  hasOneHost: function () {
    return this.get('value').length === 1;
  }.property('value'),

  hasMultipleHosts: function () {
    return this.get('value').length > 1;
  }.property('value'),

  otherLength: function () {
    var len = this.get('value').length;
    if (len > 2) {
      return Em.I18n.t('installer.controls.serviceConfigMultipleHosts.others').format(len - 1);
    } else {
      return Em.I18n.t('installer.controls.serviceConfigMultipleHosts.other');
    }
  }.property('value')

});


/**
 * Multiple master host component.
 * Show hostnames without ability to edit it
 * @type {*}
 */
App.ServiceConfigMasterHostsView = Ember.View.extend(App.ServiceConfigMultipleHostsDisplay, App.ServiceConfigCalculateId, {

  viewName: "serviceConfigMasterHostsView",
  valueBinding: 'serviceConfig.value',

  classNames: ['master-hosts', 'span6'],
  templateName: require('templates/wizard/master_hosts'),

  /**
   * Onclick handler for link
   */
  showHosts: function () {
    var serviceConfig = this.get('serviceConfig');
    App.ModalPopup.show({
      header: Em.I18n.t('installer.controls.serviceConfigMasterHosts.header').format(serviceConfig.category),
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/wizard/master_hosts_popup')
      }),
      secondary: null
    });
  }

});

/**
 * Show tabs list for slave hosts
 * @type {*}
 */
App.SlaveComponentGroupsMenu = Em.CollectionView.extend(App.ServiceConfigCalculateId, {

  content: function () {
    return this.get('controller.componentGroups');
  }.property('controller.componentGroups'),

  tagName: 'ul',
  classNames: ["nav", "nav-tabs"],

  itemViewClass: Em.View.extend({
    classNameBindings: ["active"],

    active: function () {
      return this.get('content.active');
    }.property('content.active'),

    errorCount: function () {
      return this.get('content.properties').filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
    }.property('content.properties.@each.isValid', 'content.properties.@each.isVisible'),

    templateName: require('templates/wizard/controls_slave_component_groups_menu')
  })

});

/**
 * <code>Add group</code> button
 * @type {*}
 */
App.AddSlaveComponentGroupButton = Ember.View.extend(App.ServiceConfigCalculateId, {

  tagName: 'span',
  slaveComponentName: null,

  didInsertElement: function () {
    App.popover(this.$(), {
      title: Em.I18n.t('installer.controls.addSlaveComponentGroupButton.title').format(this.get('slaveComponentName')),
      content: Em.I18n.t('installer.controls.addSlaveComponentGroupButton.content').format(this.get('slaveComponentName'), this.get('slaveComponentName'), this.get('slaveComponentName')),
      placement: 'right',
      trigger: 'hover'
    });
  }

});

/**
 * Multiple Slave Hosts component
 * @type {*}
 */
App.ServiceConfigSlaveHostsView = Ember.View.extend(App.ServiceConfigMultipleHostsDisplay, App.ServiceConfigCalculateId, {

  viewName: 'serviceConfigSlaveHostsView',

  classNames: ['slave-hosts', 'span6'],

  valueBinding: 'serviceConfig.value',

  templateName: require('templates/wizard/slave_hosts'),

  /**
   * Onclick handler for link
   */
  showHosts: function () {
    var serviceConfig = this.get('serviceConfig');
    App.ModalPopup.show({
      header: Em.I18n.t('installer.controls.serviceConfigMasterHosts.header').format(serviceConfig.category),
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/wizard/master_hosts_popup')
      }),
      secondary: null
    });
  }

});

/**
 * properties for present active slave group
 * @type {*}
 */
App.SlaveGroupPropertiesView = Ember.View.extend(App.ServiceConfigCalculateId, {

  viewName: 'serviceConfigSlaveHostsView',

  group: function () {
    return this.get('controller.activeGroup');
  }.property('controller.activeGroup'),

  groupConfigs: function () {
    console.log("************************************************************************");
    console.log("The value of group is: " + this.get('group'));
    console.log("************************************************************************");
    return this.get('group.properties');
  }.property('group.properties.@each').cacheable(),

  errorCount: function () {
    return this.get('group.properties').filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
  }.property('configs.@each.isValid', 'configs.@each.isVisible')
});

/**
 * DropDown component for <code>select hosts for groups</code> popup
 * @type {*}
 */
App.SlaveComponentDropDownGroupView = Ember.View.extend(App.ServiceConfigCalculateId, {

  viewName: "slaveComponentDropDownGroupView",

  /**
   * On change handler for <code>select hosts for groups</code> popup
   * @param event
   */
  changeGroup: function (event) {
    var host = this.get('content');
    var groupName = $('#' + this.get('elementId') + ' select').val();
    this.get('controller').changeHostGroup(host, groupName);
  },

  optionTag: Ember.View.extend({

    /**
     * Whether current value(OptionTag value) equals to host value(assigned to SlaveComponentDropDownGroupView.content)
     */
    selected: function () {
      return this.get('parentView.content.group') === this.get('content');
    }.property('content')
  })
});

/**
 * Show info about current group
 * @type {*}
 */
App.SlaveComponentChangeGroupNameView = Ember.View.extend(App.ServiceConfigCalculateId, {

  contentBinding: 'controller.activeGroup',
  classNames: ['control-group'],
  classNameBindings: 'error',
  error: false,
  setError: function () {
    this.set('error', false);
  }.observes('controller.activeGroup'),
  errorMessage: function () {
    return this.get('error') ? Em.I18n.t('installer.controls.slaveComponentChangeGroupName.error') : '';
  }.property('error'),

  /**
   * Onclick handler for saving updated group name
   * @param event
   */
  changeGroupName: function (event) {
    var inputVal = $('#' + this.get('elementId') + ' input[type="text"]').val();
    if (inputVal !== this.get('content.name')) {
      var result = this.get('controller').changeSlaveGroupName(this.get('content'), inputVal);
      this.set('error', result);
    }
  }
});
/**
 * View for testing connection to database.
 **/
App.CheckDBConnectionView = Ember.View.extend({
  templateName: require('templates/common/form/check_db_connection'),
  /** @property {string} btnCaption - text for button **/
  btnCaption: function() {
    return this.get('parentView.service.serviceName') === 'KERBEROS'
      ? Em.I18n.t('services.service.config.kdc.btn.idle')
      : Em.I18n.t('services.service.config.database.btn.idle')
  }.property('parentView.service.serviceName'),
  /** @property {string} responseCaption - text for status link **/
  responseCaption: null,
  /** @property {boolean} isConnecting - is request to server activated **/
  isConnecting: false,
  /** @property {boolean} isValidationPassed - check validation for required fields **/
  isValidationPassed: null,
  /** @property {string} databaseName- name of current database **/
  databaseName: null,
  /** @property {boolean} isRequestResolved - check for finished request to server **/
  isRequestResolved: false,
  /** @property {boolean} isConnectionSuccess - check for successful connection to database **/
  isConnectionSuccess: null,
  /** @property {string} responseFromServer - message from server response **/
  responseFromServer: null,
  /** @property {Object} ambariRequiredProperties - properties that need for custom action request **/
  ambariRequiredProperties: null,
  /** @property {Number} currentRequestId - current custom action request id **/
  currentRequestId: null,
  /** @property {Number} currentTaskId - current custom action task id **/
  currentTaskId: null,
  /** @property {jQuery.Deferred} request - current $.ajax request **/
  request: null,
  /** @property {Number} pollInterval - timeout interval for ajax polling **/
  pollInterval: 3000,
  /** @property {Object} logsPopup - popup with DB connection check info **/
  logsPopup: null,
  /** @property {string} hostNameProperty - host name property based on service and database names **/
  hostNameProperty: function() {
    if (!/wizard/i.test(this.get('controller.name')) && this.get('parentView.service.serviceName') === 'HIVE') {
      return this.get('parentView.service.serviceName').toLowerCase() + '_hostname';
    } else if (this.get('parentView.service.serviceName') === 'KERBEROS') {
      return 'kdc_host';
    } else if (this.get('parentView.service.serviceName') === 'RANGER') {
      return '{0}_{1}_host'.format(this.get('parentView.service.serviceName').toLowerCase(), this.get('databaseName').toLowerCase());
    }
    return '{0}_existing_{1}_host'.format(this.get('parentView.service.serviceName').toLowerCase(), this.get('databaseName').toLowerCase());
  }.property('databaseName'),
  /** @property {boolean} isBtnDisabled - disable button on failed validation or active request **/
  isBtnDisabled: function() {
    return !this.get('isValidationPassed') || this.get('isConnecting');
  }.property('isValidationPassed', 'isConnecting'),
  /** @property {object} requiredProperties - properties that necessary for database connection **/
  requiredProperties: function() {
    var propertiesMap = {
      OOZIE: ['oozie.db.schema.name', 'oozie.service.JPAService.jdbc.username', 'oozie.service.JPAService.jdbc.password', 'oozie.service.JPAService.jdbc.driver', 'oozie.service.JPAService.jdbc.url'],
      HIVE: ['ambari.hive.db.schema.name', 'javax.jdo.option.ConnectionUserName', 'javax.jdo.option.ConnectionPassword', 'javax.jdo.option.ConnectionDriverName', 'javax.jdo.option.ConnectionURL'],
      KERBEROS: ['kdc_host'],
      RANGER: App.get('isHadoop23Stack') ? ['db_user', 'db_password', 'db_name', 'ranger.jpa.jdbc.url', 'ranger.jpa.jdbc.driver'] :
          ['db_user', 'db_password', 'db_name', 'ranger_jdbc_connection_url', 'ranger_jdbc_driver']
    };
    return propertiesMap[this.get('parentView.service.serviceName')];
  }.property('App.isHadoop23Stack'),
  /** @property {Object} propertiesPattern - check pattern according to type of connection properties **/
  propertiesPattern: function() {
    var patterns = {
      db_connection_url: /jdbc\.url|connection_url|connectionurl|kdc_host/ig
    };
    if (this.get('parentView.service.serviceName') != "KERBEROS") {
      patterns.user_name = /(username|dblogin|db_user)$/ig;
      patterns.user_passwd = /(dbpassword|password|db_password)$/ig;
    }
    return patterns;
  }.property('parentView.service.serviceName'),
  /** @property {String} masterHostName - host name location of Master Component related to Service **/
  masterHostName: function() {
    var serviceMasterMap = {
      'OOZIE': 'oozieserver_host',
      'HDFS': 'hadoop_host',
      'HIVE': 'hivemetastore_host',
      'KERBEROS': 'kdc_host',
      'RANGER': 'rangerserver_host'
    };
    return this.get('parentView.categoryConfigsAll').findProperty('name', serviceMasterMap[this.get('parentView.service.serviceName')]).get('value');
  }.property('parentView.service.serviceName', 'parentView.categoryConfigsAll.@each.value'),
  /** @property {Object} connectionProperties - service specific config values mapped for custom action request **/
  connectionProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      propObj[key] = this.getConnectionProperty(this.get('propertiesPattern')[key]);
    }

    if (this.get('parentView.service.serviceName') === 'RANGER') {
      var dbFlavor = this.get('parentView.categoryConfigsAll').findProperty('name','DB_FLAVOR').get('value'),
        databasesTypes = /MYSQL|POSTGRES|ORACLE|MSSQL|SQLA/gi,
        dbType = dbFlavor.match(databasesTypes)?dbFlavor.match(databasesTypes)[0].toLowerCase():'';

      if (dbType==='oracle') {
        // fixes oracle SYSDBA issue
        propObj['user_name'] = "\'%@ as sysdba\'".fmt(propObj['user_name']);
      }
    }
    return propObj;
  }.property('parentView.categoryConfigsAll.@each.value'),
  /**
   * Properties that stores in local storage used for handling
   * last success connection.
   *
   * @property {Object} preparedDBProperties
   **/
  preparedDBProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      var propName = this.getConnectionProperty(this.get('propertiesPattern')[key], true);
      propObj[propName] = this.get('parentView.categoryConfigsAll').findProperty('name', propName).get('value');
    }
    return propObj;
  }.property(),
  /** Check validation and load ambari properties **/
  didInsertElement: function() {
    var kdc = this.get('parentView.categoryConfigsAll').findProperty('name', 'kdc_type');
    if (kdc) {
      var name = kdc.get('value') == 'Existing MIT KDC' ? 'KDC' : 'AD';
      App.popover(this.$(), {
        title: Em.I18n.t('services.service.config.database.btn.idle'),
        content: Em.I18n.t('installer.controls.checkConnection.popover').format(name),
        placement: 'right',
        trigger: 'hover'
      });
    }
    this.handlePropertiesValidation();
    this.getAmbariProperties();
  },
  /** On view destroy **/
  willDestroyElement: function() {
    this.set('isConnecting', false);
    this._super();
  },
  /**
   * Observer that take care about enabling/disabling button based on required properties validation.
   *
   * @method handlePropertiesValidation
   **/
  handlePropertiesValidation: function() {
    this.restore();
    var isValid = true;
    var properties = [].concat(this.get('requiredProperties'));
    properties.push(this.get('hostNameProperty'));
    properties.forEach(function(propertyName) {
      var property = this.get('parentView.categoryConfigsAll').findProperty('name', propertyName);
      if(property && !property.get('isValid')) isValid = false;
    }, this);
    this.set('isValidationPassed', isValid);
  }.observes('parentView.categoryConfigsAll.@each.isValid', 'parentView.categoryConfigsAll.@each.value', 'databaseName'),

   getConnectionProperty: function(regexp, isGetName) {
   var _this = this;
      var propertyName = _this.get('requiredProperties').filter(function(item) {
    return regexp.test(item);
    })[0];
    return (isGetName) ? propertyName : _this.get('parentView.categoryConfigsAll').findProperty('name', propertyName).get('value');
  },
  /**
   * Set up ambari properties required for custom action request
   *
   * @method getAmbariProperties
   **/
  getAmbariProperties: function() {
    var clusterController = App.router.get('clusterController');
    var _this = this;
    if (!App.isEmptyObject(App.db.get('tmp', 'ambariProperties')) && !this.get('ambariProperties')) {
      this.set('ambariProperties', App.db.get('tmp', 'ambariProperties'));
      return;
    }
    if (App.isEmptyObject(clusterController.get('ambariProperties'))) {
      clusterController.loadAmbariProperties().done(function(data) {
        _this.formatAmbariProperties(data.RootServiceComponents.properties);
      });
    } else {
      this.formatAmbariProperties(clusterController.get('ambariProperties'));
    }
  },

  formatAmbariProperties: function(properties) {
    var defaults = {
      threshold: "60",
      ambari_server_host: location.hostname,
      check_execute_list : "db_connection_check"
    };
    var properties = App.permit(properties, ['jdk.name','jdk_location','java.home']);
    var renameKey = function(oldKey, newKey) {
      if (properties[oldKey]) {
        defaults[newKey] = properties[oldKey];
        delete properties[oldKey];
      }
    };
    renameKey('java.home', 'java_home');
    renameKey('jdk.name', 'jdk_name');
    $.extend(properties, defaults);
    App.db.set('tmp', 'ambariProperties', properties);
    this.set('ambariProperties', properties);
  },
  /**
   * `Action` method for starting connect to current database.
   *
   * @method connectToDatabase
   **/
  connectToDatabase: function() {
    if (this.get('isBtnDisabled')) return;
    this.set('isRequestResolved', false);
    App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', {});
    this.setConnectingStatus(true);
    if (App.get('testMode')) {
      this.startPolling();
    } else {
      this.runCheckConnection();
    }
  },

  /**
   * runs check connections methods depending on service
   * @return {void}
   * @method runCheckConnection
   */
  runCheckConnection: function() {
    if (this.get('parentView.service.serviceName') === 'KERBEROS') {
      this.runKDCCheck();
    } else {
      this.createCustomAction();
    }
  },

  /**
   * send ajax request to perforn kdc host check
   * @return {App.ajax}
   * @method runKDCCheck
   */
  runKDCCheck: function() {
    return App.ajax.send({
      name: 'admin.kerberos_security.test_connection',
      sender: this,
      data: {
        kdcHostname: this.get('masterHostName')
      },
      success: 'onRunKDCCheckSuccess',
      error: 'onCreateActionError'
    });
  },

  /**
   *
   * @param data
   */
  onRunKDCCheckSuccess: function(data) {
    var statusCode = {
      success: 'REACHABLE',
      failed: 'UNREACHABLE'
    };
    if (data == statusCode['success']) {
      this.setResponseStatus('success');
    } else {
      this.setResponseStatus('failed');
    }
    this.set('responseFromServer', data);
  },

  /**
   * Run custom action for database connection.
   *
   * @method createCustomAction
   **/
  createCustomAction: function() {
    var params = $.extend(true, {}, { db_name: this.get('databaseName').toLowerCase() }, this.get('connectionProperties'), this.get('ambariProperties'));
    App.ajax.send({
      name: 'custom_action.create',
      sender: this,
      data: {
        requestInfo: {
          parameters: params
        },
        filteredHosts: [this.get('masterHostName')]
      },
      success: 'onCreateActionSuccess',
      error: 'onCreateActionError'
    });
  },
  /**
   * Run updater if task is created successfully.
   *
   * @method onConnectActionS
   **/
  onCreateActionSuccess: function(data) {
    this.set('currentRequestId', data.Requests.id);
    App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('currentRequestId')
      },
      success: 'setCurrentTaskId'
    });
  },

  setCurrentTaskId: function(data) {
    this.set('currentTaskId', data.items[0].Tasks.id);
    this.startPolling();
  },

  startPolling: function() {
    if (this.get('isConnecting'))
      this.getTaskInfo();
  },

  getTaskInfo: function() {
    var request = App.ajax.send({
      name: 'custom_action.request',
      sender: this,
      data: {
        requestId: this.get('currentRequestId'),
        taskId: this.get('currentTaskId')
      },
      success: 'getTaskInfoSuccess'
    });
    this.set('request', request);
  },

  getTaskInfoSuccess: function(data) {
    var task = data.Tasks;
    this.set('responseFromServer', {
      stderr: task.stderr,
      stdout: task.stdout
    });
    if (task.status === 'COMPLETED') {
      var structuredOut = task.structured_out.db_connection_check;
      if (structuredOut.exit_code != 0) {
        this.set('responseFromServer', {
          stderr: task.stderr,
          stdout: task.stdout,
          structuredOut: structuredOut.message
        });
        this.setResponseStatus('failed');
      } else {
        App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', this.get('preparedDBProperties'));
        this.setResponseStatus('success');
      }
    }
    if (task.status === 'FAILED') {
      this.setResponseStatus('failed');
    }
    if (/PENDING|QUEUED|IN_PROGRESS/.test(task.status)) {
      Em.run.later(this, function() {
        this.startPolling();
      }, this.get('pollInterval'));
    }
  },

  onCreateActionError: function(jqXhr, status, errorMessage) {
    this.setResponseStatus('failed');
    this.set('responseFromServer', errorMessage);
  },

  setResponseStatus: function(isSuccess) {
    var isSuccess = isSuccess == 'success';
    this.setConnectingStatus(false);
    this.set('responseCaption', isSuccess ? Em.I18n.t('services.service.config.database.connection.success') : Em.I18n.t('services.service.config.database.connection.failed'));
    this.set('isConnectionSuccess', isSuccess);
    this.set('isRequestResolved', true);
    if (this.get('logsPopup')) {
      var statusString = isSuccess ? 'common.success' : 'common.error';
      this.set('logsPopup.header', Em.I18n.t('services.service.config.connection.logsPopup.header').format(this.get('databaseName'), Em.I18n.t(statusString)));
    }
  },
  /**
   * Switch captions and statuses for active/non-active request.
   *
   * @method setConnectionStatus
   * @param {Boolean} [active]
   */
  setConnectingStatus: function(active) {
    if (active) {
      this.set('responseCaption', Em.I18n.t('services.service.config.database.connection.inProgress'));
    }
    this.set('controller.testConnectionInProgress', !!active);
    this.set('btnCaption', !!active ? Em.I18n.t('services.service.config.database.btn.connecting') : Em.I18n.t('services.service.config.database.btn.idle'));
    this.set('isConnecting', !!active);
  },
  /**
   * Set view to init status.
   *
   * @method restore
   **/
  restore: function() {
    if (this.get('request')) {
      this.get('request').abort();
      this.set('request', null);
    }
    this.set('responseCaption', null);
    this.set('responseFromServer', null);
    this.setConnectingStatus(false);
    this.set('isRequestResolved', false);
  },
  /**
   * `Action` method for showing response from server in popup.
   *
   * @method showLogsPopup
   **/
  showLogsPopup: function() {
    if (this.get('isConnectionSuccess')) return;
    var _this = this;
    var statusString = this.get('isRequestResolved') ? 'common.error' : 'common.testing';
    var popup = App.showAlertPopup(Em.I18n.t('services.service.config.connection.logsPopup.header').format(this.get('databaseName'), Em.I18n.t(statusString)), null, function () {
      _this.set('logsPopup', null);
    });
    popup.reopen({
      onClose: function () {
        this._super();
        _this.set('logsPopup', null);
      }
    });
    if (typeof this.get('responseFromServer') == 'object') {
      popup.set('bodyClass', Em.View.extend({
        checkDBConnectionView: _this,
        templateName: require('templates/common/error_log_body'),
        openedTask: function () {
          return this.get('checkDBConnectionView.responseFromServer');
        }.property('checkDBConnectionView.responseFromServer.stderr', 'checkDBConnectionView.responseFromServer.stdout', 'checkDBConnectionView.responseFromServer.structuredOut')
      }));
    } else {
      popup.set('body', this.get('responseFromServer'));
    }
    this.set('logsPopup', popup);
    return popup;
  }
});

/**
 * View with input field used to repo-version URLs
 * @type {*}
 */
App.BaseUrlTextField = Ember.TextField.extend({

  layout: Ember.Handlebars.compile('<div class="pull-left">{{yield}}</div> {{#if view.valueWasChanged}}<div class="pull-right"><a class="btn-small" {{action "restoreValue" target="view"}}><i class="icon-undo"></i></a></div>{{/if}}'),

  /**
   * Binding in the template
   * @type {App.RepositoryVersion}
   */
  repository: null,

  /**
   * @type {string}
   */
  valueBinding: 'repository.baseUrl',

  /**
   * @type {string}
   */
  defaultValue: '',

  /**
   *  validate base URL
   */
  validate: function () {
    if (this.get('repository.skipValidation')) {
      this.set('repository.hasError', false);
    } else {
      this.set('repository.hasError', !(validator.isValidBaseUrl(this.get('value'))));
    }
    this.get('parentView').uiValidation();
  }.observes('value', 'repository.skipValidation'),

  /**
   * Determines if user have put some new value
   * @type {boolean}
   */
  valueWasChanged: function () {
    return this.get('value') !== this.get('defaultValue');
  }.property('value', 'defaultValue'),

  didInsertElement: function () {
    this.set('defaultValue', this.get('value'));
  },

  /**
   * Restore value and unset error-flag
   * @method restoreValue
   */
  restoreValue: function () {
    this.set('value', this.get('defaultValue'));
  }
});
