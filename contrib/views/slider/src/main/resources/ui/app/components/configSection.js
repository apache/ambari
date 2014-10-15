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

App.ConfigSectionComponent = Em.Component.extend({

  layoutName: 'components/configSection',

  config: null,

  section: '',

  /**
   * label for current section
   * @return {String}
   */
  sectionLabel: function () {
    return this.get('section').classify().replace(/([A-Z])/g, ' $1');
  }.property(),

  /**
   * Return True is section name equals 'general'
   * @type {Boolean}
   */
  isGeneral: Ember.computed.equal('section', 'general'),

  /**
   * Return True is section name equals 'custom'
   * @type {Boolean}
   */
  isCustom: Ember.computed.equal('section', 'custom'),

  /**
   * Filtered configs for current section
   * @type {Array}
   */
  sectionConfigs: Ember.computed.filter('config', function (item) {
    if (item.isSet) {
      return item.section === this.get('section');
    }
    if (this.get('isGeneral')) {
      return !item.name.match('^site.') && this.get('predefinedConfigNames').contains(item.name);
    }
    else {
      if (this.get('isCustom')) {
        return !this.get('predefinedConfigNames').contains(item.name);
      }
      else {
        return !!item.name.match('^site.' + this.get('section'));
      }
    }
  }),

  /**
   * Is button "Add Property" visible
   * True - yes, false - no (and "App Property"-form is visible)
   * @type {bool}
   */
  buttonVisible: true,

  /**
   * Template for new config
   * @type {Ember.Object}
   */
  newConfig: Em.Object.create({
    name: '',
    value: '',
    nameError: '',
    hasError: false
  }),

  /**
   * Clear <code>newConfig</code>
   * @method cleanNewConfig
   */
  cleanNewConfig: function() {
    this.get('newConfig').setProperties({
      name: '',
      value: '',
      messsage: '',
      hasError: false
    });
  },

  addPropertyModalButtons: [
    Ember.Object.create({title: Em.I18n.t('common.cancel'), clicked:"discard", dismiss: 'modal'}),
    Ember.Object.create({title: Em.I18n.t('common.add'), clicked:"submit", type:'success'})
  ],

  addPropertyModalTitle: Em.I18n.t('configs.add_property'),

  tooltipRemove:  Em.I18n.t('common.remove'),

  actions: {

    /**
     * Click on "App Property"-button
     * @method addProperty
     */
    addProperty: function() {
      return Bootstrap.ModalManager.show('addPropertyModal');
    },

    /**
     * Delete custom config added by user
     * @param {{name: string, label: string, value: *}} config
     * @method deleteConfig
     */
    deleteConfig: function(config) {
      this.get('config').removeObject(config);
    },

    /**
     * Validate and save custom config added by user
     * @method submit
     */
    submit: function() {
      var name = this.get('newConfig.name'),
        value = this.get('newConfig.value');
      if (this.get('config').mapBy('name').contains(name)) {
        this.get('newConfig').setProperties({
          hasError: true,
          messsage: Em.I18n.t('configs.add_property.name_exists')
        });
        return;
      }
      if (!/^[A-Za-z][A-Za-z0-9_\-\.]*$/.test(name)) {
        this.get('newConfig').setProperties({
          hasError: true,
          messsage: Em.I18n.t('configs.add_property.invalid_name')
        });
        return;
      }
      this.get('config').pushObject(App.ConfigProperty.create({name: name, value: value, label: name}));
      this.cleanNewConfig();
      this.toggleProperty('buttonVisible');
      Bootstrap.ModalManager.hide('addPropertyModal');
    },

    /**
     * Hide "Add Property"-form
     * @method discard
     */
    discard: function() {
      this.cleanNewConfig();
      this.toggleProperty('buttonVisible');
    }
  }

});
