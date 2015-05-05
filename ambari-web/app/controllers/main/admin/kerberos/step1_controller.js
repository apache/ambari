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

App.KerberosWizardStep1Controller = Em.Controller.extend({
  name: "kerberosWizardStep1Controller",

  selectedItem: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),

  isSubmitDisabled: function() {
    return this.get('selectedOption.preConditions').someProperty('checked',false);
  }.property('selectedOption', 'selectedOption.preConditions.@each.checked'),

  options: [
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
      preConditions: [
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.1'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.2'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc.condition.3'),
          checked: false
        })
      ]
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
      preConditions: [
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.1'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.2'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.3'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.4'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.ad.condition.5'),
          checked: false
        })
      ]
    }),
    Em.Object.create({
      displayName: Em.I18n.t('admin.kerberos.wizard.step1.option.manual'),
      value: Em.I18n.t('admin.kerberos.wizard.step1.option.manual'),
      preConditions: [
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.1'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.2'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.3'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.4'),
          checked: false
        }),
        Em.Object.create({
          displayText: Em.I18n.t('admin.kerberos.wizard.step1.option.manual.condition.5'),
          checked: false
        })
      ]
    })
  ],

  /**
   * precondition for the selected KDC option
   */
  selectedOption: function () {
    var options = this.get('options');
    var selectedItem = this.get('selectedItem');
    return options.findProperty('value', selectedItem);
  }.property('selectedItem'),



  loadStep: function () {
    this.set('selectedItem', Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
  },

  next: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
});
