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
  placeholderBinding: 'serviceConfig.defaultValue',
  isPopoverEnabled: true,

  didInsertElement: function () {
    if (this.get('isPopoverEnabled') !== 'false') {
      this.$().popover({
        title: this.get('serviceConfig.displayName') + '<br><small>' + this.get('serviceConfig.name') + '</small>',
        content: this.get('serviceConfig.description'),
        placement: 'right',
        trigger: 'hover'
      });
    }
  }
});

/**
 * Default input control
 * @type {*}
 */
App.ServiceConfigTextField = Ember.TextField.extend(App.ServiceConfigPopoverSupport, {

  valueBinding: 'serviceConfig.value',
  classNameBindings: 'textFieldClassName',
  placeholderBinding: 'serviceConfig.defaultValue',

  textFieldClassName: function () {
    // sets the width of the field depending on display type
    if (['directory', 'url', 'email', 'user', 'host'].contains(this.get('serviceConfig.displayType'))) {
      return ['span6'];
    } else if (this.get('serviceConfig.displayType') === 'advanced') {
      return ['span6'];
    } else {
      return ['input-small'];
    }
  }.property('serviceConfig.displayType'),

  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')

});

/**
 * Customized input control with Utits type specified
 * @type {*}
 */
App.ServiceConfigTextFieldWithUnit = Ember.View.extend(App.ServiceConfigPopoverSupport, {
  valueBinding: 'serviceConfig.value',
  classNames: [ 'input-append' ],
  placeholderBinding: 'serviceConfig.defaultValue',

  template: Ember.Handlebars.compile('{{view App.ServiceConfigTextField serviceConfigBinding="view.serviceConfig" isPopoverEnabled="false"}}<span class="add-on">{{view.serviceConfig.unit}}</span>'),

  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')

});

/**
 * Password control
 * @type {*}
 */
App.ServiceConfigPasswordField = Ember.TextField.extend({

  serviceConfig: null,
  type: 'password',
  valueBinding: 'serviceConfig.value',
  classNames: [ 'span3' ],
  placeholder: 'Type password',

  template: Ember.Handlebars.compile('{{view view.retypePasswordView placeholder="Retype password"}}'),

  retypePasswordView: Ember.TextField.extend({
    type: 'password',
    classNames: [ 'span3', 'retyped-password' ],
    valueBinding: 'parentView.serviceConfig.retypedPassword'
  })

});

/**
 * Textarea control
 * @type {*}
 */
App.ServiceConfigTextArea = Ember.TextArea.extend(App.ServiceConfigPopoverSupport, {

  valueBinding: 'serviceConfig.value',
  rows: 4,
  classNames: ['span6'],
  placeholderBinding: 'serviceConfig.defaultValue',

  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')

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
  template: Ember.Handlebars.compile([
    '{{#each option in view.options}}',
    '<label class="radio">',
    '{{#view App.ServiceConfigRadioButton nameBinding = "view.name" valueBinding = "option.displayName"}}',
    '{{/view}}',
    '{{option.displayName}} &nbsp;',
    '</label>',
    '{{/each}}'
  ].join('\n')),
  serviceConfig: null,
  categoryConfigs: null,
  nameBinding: 'serviceConfig.radioName',
  optionsBinding: 'serviceConfig.options',
  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')
});

App.ServiceConfigRadioButton = Ember.Checkbox.extend({
  tagName: 'input',
  attributeBindings: ['type', 'name', 'value', 'checked'],
  checked: false,
  type: 'radio',
  name: null,
  value: null,

  didInsertElement: function () {
    if (this.get('parentView.serviceConfig.value') === this.get('value')) {
      this.set('checked', true);
    }
  },

  click: function () {
    this.set('checked', true);
    this.onChecked();
  },

  onChecked: function () {
    this.set('parentView.serviceConfig.value', this.get('value'));
    var components = this.get('parentView.serviceConfig.options');
    components.forEach(function (_component) {
      _component.foreignKeys.forEach(function (_componentName) {
        if (this.get('parentView.categoryConfigs').someProperty('name', _componentName)) {
          var component = this.get('parentView.categoryConfigs').findProperty('name', _componentName);
          if (_component.displayName === this.get('value')) {
            component.set('isVisible', true);
          } else {
            component.set('isVisible', false);
          }
        }
      }, this);
    }, this);
  }.observes('checked') ,

  disabled: function () {
    return !this.get('parentView.serviceConfig.isEditable');
  }.property('parentView.serviceConfig.isEditable')
});

App.ServiceConfigComboBox = Ember.Select.extend(App.ServiceConfigPopoverSupport, {
  contentBinding: 'serviceConfig.options',
  selectionBinding: 'serviceConfig.value',
  classNames: [ 'span3' ],
  disabled: function () {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable')
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
    this.$().popover({
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
 * Base component to display Multiple hosts
 * @type {*}
 */
App.ServiceConfigMultipleHostsDisplay = Ember.Mixin.create(App.ServiceConfigHostPopoverSupport, {

  hasNoHosts: function () {
    console.log('view', this.get('viewName')); //to know which View cause errors
    console.log('controller', this.get('controller').name); //should be slaveComponentGroupsController
    if (!this.get('value')) {
      // debugger;
      return true;
    }
    return this.get('value').length === 0;
  }.property('value'),

  hasOneHost: function () {
    return this.get('value').length > 0;
  }.property('value'),

  hasMultipleHosts: function () {
    return (this.get('value').length > 1 && typeof(this.get('value')) == 'object');
  }.property('value'),

  otherLength: function () {
    var len = this.get('value').length;
    if (len > 2) {
      return (len - 1) + ' others';
    } else {
      return '1 other';
    }
  }.property('value')

})


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
      header: serviceConfig.category + ' Hosts',
      bodyClass: Ember.View.extend({
        serviceConfig: serviceConfig,
        templateName: require('templates/wizard/master_hosts_popup')
      }),
      onPrimary: function () {
        this.hide();
      },
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

    template: Ember.Handlebars.compile('<a {{action showSlaveComponentGroup view.content target="controller"}} href="#"> {{view.content.name}}{{#if view.errorCount}}<span class="badge badge-important">{{view.errorCount}}</span>{{/if}}</a><i {{action removeSlaveComponentGroup view.content target="controller"}} class="icon-remove"></i>')
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
    this.$().popover({
      title: 'Add a ' + this.get('slaveComponentName') + ' Group',
      content: 'If you need different settings on certain ' + this.get('slaveComponentName') + 's, you can add a ' + this.get('slaveComponentName') + ' group.<br>' +
        'All ' + this.get('slaveComponentName') + 's within the same group will have the same set of settings.  You can create multiple groups.',
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

  templateName: require('templates/wizard/slave_hosts')

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
    return this.get('error') ? 'group with this name already exist' : '';
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

