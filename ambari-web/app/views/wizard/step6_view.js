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

App.WizardStep6View = App.TableView.extend({

  templateName: require('templates/wizard/step6'),

  /**
   * Number of visible rows
   * @type {string}
   */
  displayLength: "25",

  /**
   * List of hosts
   * @type {object[]}
   */
  content: function () {
    return this.get('controller.hosts');
  }.property('controller.hosts'),

  /**
   * Synonym to <code>content</code> in this <code>App.TableView</code>
   * @type {object[]}
   */
  filteredContent: Em.computed.alias('content'),

  /**
   * Set <code>label</code> and do <code>loadStep</code>
   * @method didInsertElement
   */
  didInsertElement: function () {
    this.setLabel();
    this.get('controller').loadStep();
  },

  /**
   * Set <code>label</code> value
   * @method setLabel
   */
  setLabel: function () {
    var clients = this.get('controller.content.clients');
    var label = !!clients.length ? Em.I18n.t('installer.step6.body') +  Em.I18n.t('installer.step6.body.clientText') : Em.I18n.t('installer.step6.body');

    clients.forEach(function (_client) {
      if (clients.length === 1) {
        label = label + ' ' + _client.display_name;
      } else {
        if (_client !== clients[clients.length - 1]) {           // [clients.length - 1]
          label = label + ' ' + _client.display_name;
          if (_client !== clients[clients.length - 2]) {
            label = label + ',';
          }
        }
        else {
          label = label + ' ' + Em.I18n.t('and') + ' ' + _client.display_name + '.';
        }
      }
    }, this);
    this.set('label', label);
  },

  checkboxClick: function(e) {
    var checkbox = e.context;
    Em.set(checkbox, 'checked', !checkbox.checked);
    this.get('controller').checkCallback(checkbox.component);
    this.get('controller').callValidation();
  },

  columnCount: function() {
    var hosts = this.get('controller.hosts');
    return hosts && hosts.length > 0 ? hosts[0].get('checkboxes').length + 1 : 1;
  }.property('controller.hosts.@each.checkboxes')
});

App.WizardStep6HostView = Em.View.extend({

  /**
   * Bound <code>host</code> object
   * @type {object}
   */
  host: null,

  'data-qa': 'hostname-block',

  tagName: 'td',

  /**
   * Create hover-labels for hostName with list of installed master-components
   * @method didInsertElement
   */
  didInsertElement: function () {
    const componentNames = this.get('controller')
      .getMasterComponentsForHost(this.get('host.hostName'))
      .map(_component => App.format.role(_component, false))
      .join('<br />');
    App.popover(this.$(), {
      title: Em.I18n.t('installer.step6.wizardStep6Host.title').format(this.get('host.hostName')),
      content: `<div data-qa="master-component-popover">${componentNames}</div>`,
      placement: 'right',
      trigger: 'hover'
    });
  },

  willDestroyElement: function() {
    this.$().popover('destroy');
  }

});
