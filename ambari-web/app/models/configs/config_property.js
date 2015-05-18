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

App.ConfigProperty = DS.Model.extend({

  /**
   * id is consist of property <code>name<code>+<code>fileName<code>+<code>configVersion.version<code>
   */
  id: DS.attr('string'),

  /**
   * config property name
   * @property {string}
   */
  name: DS.attr('string'),

  /**
   * config property name
   * @property {string}
   */
  fileName: DS.attr('string'),

  /**
   * value of property
   * by default is same as <code>savedValue<code>
   * @property {string}
   */
  value: DS.attr('string'),

  /**
   * saved value of property
   * @property {string}
   */
  savedValue: DS.attr('string'),

  /**
   * recommended value of property
   * that is returned from server
   * @property {string}
   */
  recommendedValue: DS.attr('string'),

  /**
   * defines if property is final
   * @property {boolean}
   */
  isFinal: DS.attr('boolean', {defaultValue: false}),


  /**
   * value saved on cluster
   */
  savedIsFinal: DS.attr('boolean', {defaultValue: false}),

  /**
   * value recommendedFrom Server
   * @property {boolean}
   */
  recommendedIsFinal: DS.attr('boolean', {defaultValue: false}),

  /**
   * link to config version
   * @property {App.ConfigVersion}
   */
  configVersion: DS.belongsTo('App.ConfigVersion'),

  /**
   * link to config version
   * from this model we can get all static info about property
   * @property {App.ConfigVersion}
   */
  stackConfigProperty: DS.belongsTo('App.StackConfigProperty'),

  /**
   * defines if property should be visible for user
   * all properties that has <code>isVisible<code> false will be present in model
   * and saved but will be hidden from user
   * @property {boolean}
   */
  isVisible: DS.attr('boolean', {defaultValue: true}),

  /**
   * defines if property value is required
   * in case user enter empty value error will be shown
   * @property {boolean}
   */
  isRequired: DS.attr('boolean', {defaultValue: true}),

  /**
   * defines if property can be edited by user
   * @property {boolean}
   */
  isEditable: DS.attr('boolean', {defaultValue: true}),

  /**
   * opposite to <code>isEditable<code> property
   * @property {boolean}
   */
  isNotEditable: Ember.computed.not('isEditable'),

  /**
   * defines if property can contain overriden values
   * @property {boolean}
   */
  isOverridable: DS.attr('boolean', {defaultValue: true}),

  /**
   * defines if property is used for security
   * @property {boolean}
   */
  isSecureConfig: DS.attr('boolean', {defaultValue: false}),

  /**
   * if false - don't save property
   * @property {boolean}
   */
  isRequiredByAgent: DS.attr('boolean', {defaultValue: true}),

  /**
   * if true - property is not saved
   * used for properties added by user
   * @property {boolean}
   */
  isNotSaved: DS.attr('boolean', {defaultValue: false}),

  /**
   * if true - don't show property
   * @property {boolean}
   */
  isHiddenByFilter: DS.attr('boolean', {defaultValue: false}),

  /**
   * properties with this flag set to false will not be saved
   * @property {boolean}
   */
  saveRecommended: DS.attr('boolean', {defaultValue: true}),

  /**
   * Don't show "Undo" for hosts on Installer Step7
   * if value is true
   * @property {boolean}
   */
  cantBeUndone: DS.attr('boolean', {defaultValue: false}),

  /**
   * error message; by default is empty
   * if value is not correct or missing for required property
   * this will contain error message
   * @property {string}
   */
  errorMessage: DS.attr('string', {defaultValue: ''}),

  /**
   * warning message; by default is empty
   * if value is out of recommended range
   * this will contain warning message
   * @property {string}
   */
  warnMessage: DS.attr('string', {defaultValue: ''}),

  /**
   * defines if property has errors
   * @type {boolean}
   */
  hasErrors: function() {
    return this.get('errorMessage') !== '';
  }.property('errorMessage'),

  /**
   * defines if property has warnings
   * @type {boolean}
   */
  hasWarnings: function() {
    return this.get('warnMessage') !== '';
  }.property('warnMessage'),

  /**
   * defines if property belongs to default config group
   * if true it's config group is default
   * @type {boolean}
   */
  isOriginalSCP: function() {
    return this.get('configVersion.isDefault');
  }.property('configVersion.isDefault'),

  /**
   * defines if property is added by user
   * @property {boolean}
   */
  isUserProperty: function() {
    return Em.isNone(this.get('stackConfigProperty'));
  }.property('stackConfigProperty'),

  /**
   * defines if this property is belongs to version
   * with which we make comparison
   * @property {boolean}
   */
  isForCompare: function() {
    return this.get('configVersion.isForCompare');
  }.property('configVersion.isForCompare'),

  /**
   * if this property can be final
   * @property {boolean}
   */
  supportsFinal: function () {
    return this.get('stackConfigProperty.supportsFinal') || this.get('isUserProperty');
  }.property('stackConfigProperty.supportsFinal', 'isUserProperty'),
  /**
   * Indicates when value is not the default value.
   * Returns false when there is no default value.
   * @type {boolean}
   */
  isNotDefaultValue: function () {
    return this.get('isEditable')
      && (!Em.isNone(this.get('savedValue') && this.get('value') !== this.get('savedValue'))
      || (this.get('supportsFinal') && !Em.isNone(this.get('savedIsFinal')) && this.get('isFinal') !== this.get('savedIsFinal')));
  }.property('value', 'savedValue', 'isEditable', 'isFinal', 'savedIsFinal'),

  /**
   * opposite to <code>hasErrors<code>
   */
  isValid: Ember.computed.not('hasErrors')
});


App.ConfigProperty.FIXTURES = [];

