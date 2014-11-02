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

  didInsertElement: function () {
    $('body').tooltip({
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
        placement: 'right',
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

/**
 * if config value contains &amp;|&lt;|&gt;|&quot;|&apos;
 * input field converts it to &|<|>|"|'
 * this mixin helps to aviod such convertation and show values as is.
 */
App.SkipXmlEscapingSupport = Ember.Mixin.create({
  didInsertElement: function() {
    this._super();
    if (this.get('serviceConfig.value').match(/(&amp;|&lt;|&gt;|&quot;|&apos;)/g)) {
      this.set('value', this.get('serviceConfig.value').replace('&','&amp;'));
      this.set('value', this.get('value').replace('&amp;','&'));
    }
  }
});

/**
 * Default input control
 * @type {*}
 */
App.ServiceConfigTextField = Ember.TextField.extend(App.ServiceConfigPopoverSupport, App.SkipXmlEscapingSupport, {

  valueBinding: 'serviceConfig.value',
  classNameBindings: 'textFieldClassName',
  placeholderBinding: 'serviceConfig.defaultValue',

  keyPress: function (event) {
    if (event.keyCode == 13) {
      return false;
    }
  },
  //Set editDone true for last edited config text field parameter
  focusOut: function (event) {
    this.get('serviceConfig').set("editDone", true);
  },
  //Set editDone false for all current category config text field parameter
  focusIn: function (event) {
    if (!this.get('serviceConfig.isOverridden') && !this.get('serviceConfig.isComparison')) {
      this.get("parentView.categoryConfigsAll").setEach("editDone", false);
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
 * Customized input control with Utits type specified
 * @type {*}
 */
App.ServiceConfigTextFieldWithUnit = Ember.View.extend(App.ServiceConfigPopoverSupport, {
  valueBinding: 'serviceConfig.value',
  classNames: ['input-append', 'with-unit'],
  placeholderBinding: 'serviceConfig.defaultValue',

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
App.ServiceConfigTextArea = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, App.SkipXmlEscapingSupport, {

  valueBinding: 'serviceConfig.value',
  rows: 4,
  classNames: ['span9', 'directories']
});

/**
 * Textarea control for content type
 * @type {*}
 */
App.ServiceConfigTextAreaContent = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, App.SkipXmlEscapingSupport, {

  valueBinding: 'serviceConfig.value',
  rows: 20,
  classNames: ['span10']
});

/**
 * Textarea control with bigger height
 * @type {*}
 */
App.ServiceConfigBigTextArea = App.ServiceConfigTextArea.extend({
  rows: 10
});

/**
 * Checkbox control
 * @type {*}
 */
App.ServiceConfigCheckbox = Ember.Checkbox.extend(App.ServiceConfigPopoverSupport, {

  checkedBinding: 'serviceConfig.value',

  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')
});

App.ServiceConfigRadioButtons = Ember.View.extend({
  templateName: require('templates/wizard/controls_service_config_radio_buttons'),

  didInsertElement: function () {
    // on page render, automatically populate JDBC URLs only for default database settings
    // so as to not lose the user's customizations on these fields
    if (['addServiceController', 'installerController'].contains(App.clusterStatus.wizardControllerName)) {
      if (['New PostgreSQL Database', 'New MySQL Database', 'New Derby Database'].contains(this.get('serviceConfig.value'))) {
        this.onOptionsChange();
      } else {
        this.handleDBConnectionProperty();
      }
    }
  },

  configs: function () {
    if (this.get('controller.name') == 'mainServiceInfoConfigsController') return this.get('categoryConfigsAll');
    return this.get('categoryConfigsAll').filterProperty('isObserved', true);
  }.property('categoryConfigsAll'),

  serviceConfig: null,
  categoryConfigsAll: null,

  onOptionsChange: function () {
    // The following if condition will be satisfied only for installer wizard flow
    if (this.get('configs').length) {
      var connectionUrl = this.get('connectionUrl');
      var dbClass = this.get('dbClass');
      if (connectionUrl) {
        if (this.get('serviceConfig.serviceName') === 'HIVE') {
          switch (this.get('serviceConfig.value')) {
            case 'New MySQL Database':
            case 'Existing MySQL Database':
              connectionUrl.set('value', "jdbc:mysql://" + this.get('hostName') + "/" + this.get('databaseName') + "?createDatabaseIfNotExist=true");
              dbClass.set('value', "com.mysql.jdbc.Driver");
              break;
            case 'New PostgreSQL Database':
            case Em.I18n.t('services.service.config.hive.oozie.postgresql'):
              connectionUrl.set('value', "jdbc:postgresql://" + this.get('hostName') + ":5432/" + this.get('databaseName'));
              dbClass.set('value', "org.postgresql.Driver");
              break;
            case 'Existing Oracle Database':
              connectionUrl.set('value', "jdbc:oracle:thin:@//" + this.get('hostName') + ":1521/" + this.get('databaseName'));
              dbClass.set('value', "oracle.jdbc.driver.OracleDriver");
              break;
          }
        } else if (this.get('serviceConfig.serviceName') === 'OOZIE') {
          switch (this.get('serviceConfig.value')) {
            case 'New Derby Database':
              connectionUrl.set('value', "jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true");
              dbClass.set('value', "org.apache.derby.jdbc.EmbeddedDriver");
              break;
            case 'Existing MySQL Database':
              connectionUrl.set('value', "jdbc:mysql://" + this.get('hostName') + "/" + this.get('databaseName'));
              dbClass.set('value', "com.mysql.jdbc.Driver");
              break;
            case Em.I18n.t('services.service.config.hive.oozie.postgresql'):
              connectionUrl.set('value', "jdbc:postgresql://" + this.get('hostName') + ":5432/" + this.get('databaseName'));
              dbClass.set('value', "org.postgresql.Driver");
              break;
            case 'Existing Oracle Database':
              connectionUrl.set('value', "jdbc:oracle:thin:@//" + this.get('hostName') + ":1521/" + this.get('databaseName'));
              dbClass.set('value', "oracle.jdbc.driver.OracleDriver");
              break;
          }
        }
        connectionUrl.set('defaultValue', connectionUrl.get('value'));
      }
    }
  }.observes('databaseName', 'hostName', 'connectionUrl','dbClass'),

  nameBinding: 'serviceConfig.radioName',

  databaseName: function () {
    switch (this.get('serviceConfig.serviceName')) {
      case 'HIVE':
        return this.get('categoryConfigsAll').findProperty('name', 'ambari.hive.db.schema.name').get('value');
      case 'OOZIE':
        return this.get('categoryConfigsAll').findProperty('name', 'oozie.db.schema.name').get('value');
      default:
        return null;
    }
  }.property('configs.@each.value', 'serviceConfig.serviceName'),


  hostName: function () {
    var value = this.get('serviceConfig.value');
    var returnValue;
    var hostname;
    if (this.get('serviceConfig.serviceName') === 'HIVE') {
      switch (value) {
        case 'New MySQL Database':
        case 'New PostgreSQL Database':
          hostname = this.get('categoryConfigsAll').findProperty('name', 'hive_ambari_host');
          break;
        case 'Existing MySQL Database':
          hostname = this.get('categoryConfigsAll').findProperty('name', 'hive_existing_mysql_host');
          break;
        case Em.I18n.t('services.service.config.hive.oozie.postgresql'):
          hostname = this.get('categoryConfigsAll').findProperty('name', 'hive_existing_postgresql_host');
          break;
        case 'Existing Oracle Database':
          hostname = this.get('categoryConfigsAll').findProperty('name', 'hive_existing_oracle_host');
          break;
      }
      if (hostname) {
        returnValue = hostname.get('value');
      } else {
        returnValue = this.get('categoryConfigsAll').findProperty('name', 'hive_hostname').get('value');
      }

    } else if (this.get('serviceConfig.serviceName') === 'OOZIE') {
      switch (value) {
        case 'New Derby Database':
          hostname = this.get('categoryConfigsAll').findProperty('name', 'oozie_ambari_host');
          break;
        case 'Existing MySQL Database':
          hostname = this.get('categoryConfigsAll').findProperty('name', 'oozie_existing_mysql_host');
          break;
        case Em.I18n.t('services.service.config.hive.oozie.postgresql'):
          hostname = this.get('categoryConfigsAll').findProperty('name', 'oozie_existing_postgresql_host');
          break;
        case 'Existing Oracle Database':
          hostname = this.get('categoryConfigsAll').findProperty('name', 'oozie_existing_oracle_host');
          break;
      }
      if (hostname) {
        returnValue = hostname.get('value');
      } else {
        returnValue = this.get('categoryConfigsAll').findProperty('name', 'oozie_hostname').get('value');
      }
    }
    return returnValue;
  }.property('serviceConfig.serviceName', 'serviceConfig.value', 'configs.@each.value'),

  connectionUrl: function () {
    if (this.get('serviceConfig.serviceName') === 'HIVE') {
      return this.get('categoryConfigsAll').findProperty('name', 'javax.jdo.option.ConnectionURL');
    } else {
      return this.get('categoryConfigsAll').findProperty('name', 'oozie.service.JPAService.jdbc.url');
    }
  }.property('serviceConfig.serviceName'),

  dbClass: function () {
    if (this.get('serviceConfig.serviceName') === 'HIVE') {
      return this.get('categoryConfigsAll').findProperty('name', 'javax.jdo.option.ConnectionDriverName');
    } else {
      return this.get('categoryConfigsAll').findProperty('name', 'oozie.service.JPAService.jdbc.driver');
    }
  }.property('serviceConfig.serviceName'),

  /**
   * `Observer` that add <code>additionalView</code> to <code>App.ServiceConfigProperty</code>
   * that responsible for (if existing db selected)
   * 1. checking database connection
   * 2. showing jdbc driver setup warning msg.
   *
   * @method handleDBConnectionProperty
   **/
  handleDBConnectionProperty: function() {
    if (!['addServiceController', 'installerController'].contains(App.clusterStatus.wizardControllerName)) return;
    var handledProperties = ['oozie_database', 'hive_database'];
    var currentValue = this.get('serviceConfig.value');
    var databases = /MySQL|PostgreSQL|Oracle|Derby/gi;
    var currentDB = currentValue.match(databases)[0];
    var databasesTypes = /MySQL|PostgreS|Oracle|Derby/gi;
    var currentDBType = currentValue.match(databasesTypes)[0];
    var existingDatabase = /existing/gi.test(currentValue);
    // db connection check button show up if existed db selected
    if (App.supports.databaseConnection) {
      var propertyAppendTo1 = this.get('categoryConfigsAll').findProperty('displayName', 'Database URL');
      if (currentDB && existingDatabase) {
        if (handledProperties.contains(this.get('serviceConfig.name'))) {
          if (propertyAppendTo1) propertyAppendTo1.set('additionalView', App.CheckDBConnectionView.extend({databaseName: currentDB}));
        }
      } else {
        propertyAppendTo1.set('additionalView', null);
      }
    }
    // warning msg under database type radio buttons, to warn the user to setup jdbc driver if existed db selected
    var propertyHive = this.get('categoryConfigsAll').findProperty('displayName', 'Hive Database');
    var propertyOozie = this.get('categoryConfigsAll').findProperty('displayName', 'Oozie Database');
    var propertyAppendTo2 = propertyHive ? propertyHive : propertyOozie;
    if (currentDB && existingDatabase) {
      if (handledProperties.contains(this.get('serviceConfig.name'))) {
        if (propertyAppendTo2) {
          propertyAppendTo2.set('additionalView', Ember.View.extend({
            template: Ember.Handlebars.compile('<div class="alert">{{{view.message}}}</div>'),
            message: Em.I18n.t('services.service.config.database.msg.jdbcSetup').format(currentDBType.toLowerCase(), currentDBType.toLowerCase())
          }));
        }
      }
    } else {
      propertyAppendTo2.set('additionalView', null);
    }
  }.observes('serviceConfig.value', 'configs.@each.value'),

  optionsBinding: 'serviceConfig.options'
});

App.ServiceConfigRadioButton = Ember.Checkbox.extend({
  tagName: 'input',
  attributeBindings: ['type', 'name', 'value', 'checked'],
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
      if (components) {
        components.forEach(function (_component) {
          if (_component.foreignKeys) {
            _component.foreignKeys.forEach(function (_componentName) {
              if (this.get('parentView.categoryConfigsAll').someProperty('name', _componentName)) {
                var component = this.get('parentView.categoryConfigsAll').findProperty('name', _componentName);
                component.set('isVisible', _component.displayName === this.get('value'));
              }
            }, this);
          }
        }, this);
      }
    });
  }.observes('checked'),

  disabled: function () {
    return !this.get('parentView.serviceConfig.isEditable');
  }.property('parentView.serviceConfig.isEditable')
});

App.ServiceConfigComboBox = Ember.Select.extend(App.ServiceConfigPopoverSupport, {
  contentBinding: 'serviceConfig.options',
  selectionBinding: 'serviceConfig.value',
  placeholderBinding: 'serviceConfig.defaultValue',
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
App.ServiceConfigMasterHostView = Ember.View.extend(App.ServiceConfigHostPopoverSupport, {

  classNames: ['master-host', 'span6'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('{{value}}')

});

/**
 * Show value as plain label in italics
 * @type {*}
 */
App.ServiceConfigLabelView = Ember.View.extend(App.ServiceConfigHostPopoverSupport, {

  classNames: ['master-host', 'span6'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('<i>{{view.value}}</i>')
});

/**
 * Base component to display Multiple hosts
 * @type {*}
 */
App.ServiceConfigMultipleHostsDisplay = Ember.Mixin.create(App.ServiceConfigHostPopoverSupport, {

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
App.ServiceConfigMasterHostsView = Ember.View.extend(App.ServiceConfigMultipleHostsDisplay, {

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
App.SlaveComponentGroupsMenu = Em.CollectionView.extend({

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
App.AddSlaveComponentGroupButton = Ember.View.extend({

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
App.ServiceConfigSlaveHostsView = Ember.View.extend(App.ServiceConfigMultipleHostsDisplay, {

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
App.SlaveGroupPropertiesView = Ember.View.extend({

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
App.SlaveComponentDropDownGroupView = Ember.View.extend({

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
App.SlaveComponentChangeGroupNameView = Ember.View.extend({

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
  btnCaption: Em.I18n.t('services.service.config.database.btn.idle'),
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
  /** @property {string} hostNameProperty - host name property based on service and database names **/
  hostNameProperty: function() {
    if (!/wizard/i.test(this.get('controller.name')) && this.get('parentView.service.serviceName') === 'HIVE') {
      return this.get('parentView.service.serviceName').toLowerCase() + '_hostname';
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
      OOZIE: ['oozie.db.schema.name','oozie.service.JPAService.jdbc.username','oozie.service.JPAService.jdbc.password','oozie.service.JPAService.jdbc.driver','oozie.service.JPAService.jdbc.url'],
      HIVE: ['ambari.hive.db.schema.name','javax.jdo.option.ConnectionUserName','javax.jdo.option.ConnectionPassword','javax.jdo.option.ConnectionDriverName','javax.jdo.option.ConnectionURL']
    };
    return propertiesMap[this.get('parentView.service.serviceName')];
  }.property(),
  /** @property {Object} propertiesPattern - check pattern according to type of connection properties **/
  propertiesPattern: function() {
    return {
      user_name: /username$/ig,
      user_passwd: /password$/ig,
      db_connection_url: /jdbc\.url|connectionurl/ig
    }
  }.property(),
  /** @property {String} masterHostName - host name location of Master Component related to Service **/
  masterHostName: function() {
    var serviceMasterMap = {
      'OOZIE': 'oozieserver_host',
      'HIVE': 'hivemetastore_host'
    };
    return this.get('parentView.categoryConfigsAll').findProperty('name', serviceMasterMap[this.get('parentView.service.serviceName')]).get('value');
  }.property(),
  /** @property {Object} connectionProperties - service specific config values mapped for custom action request **/
  connectionProperties: function() {
    var propObj = {};
    for (var key in this.get('propertiesPattern')) {
      propObj[key] = this.getConnectionProperty(this.get('propertiesPattern')[key]);
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
    if (this.get('isBtnDisabled')) return false;
    var self = this;
    self.set('isRequestResolved', false);
    App.db.set('tmp', this.get('parentView.service.serviceName') + '_connection', {});
    this.setConnectingStatus(true);
    this.createCustomAction();
  },
  /**
   * Run custom action for database connection.
   *
   * @method createCustomAction
   **/
  createCustomAction: function() {
    var dbName = this.get('databaseName').toLowerCase() === 'postgresql' ? 'postgres' : this.get('databaseName').toLowerCase();
    var params = $.extend(true, {}, { db_name: dbName }, this.get('connectionProperties'), this.get('ambariProperties'));
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
      this.set('responseFromServer', {
        stderr: task.stderr,
        stdout: task.stdout
      });
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
  },
  /**
   * Switch captions and statuses for active/non-active request.
   *
   * @method setConnectionStatus
   * @param {Boolean} [active]
   */
  setConnectingStatus: function(active) {
    if (active) {
      this.set('responseCaption', null);
      this.set('responseFromServer', null);
    }
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
    var popup = App.showAlertPopup('Error: {0} connection'.format(this.get('databaseName')));
    if (typeof this.get('responseFromServer') == 'object') {
      popup.set('bodyClass', Em.View.extend({
        templateName: require('templates/common/error_log_body'),
        openedTask: _this.get('responseFromServer')
      }));
    } else {
      popup.set('body', this.get('responseFromServer'));
    }
    return popup;
  }
});

