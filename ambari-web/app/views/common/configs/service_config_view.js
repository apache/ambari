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

App.ServiceConfigView = Em.View.extend({
  templateName: require('templates/common/configs/service_config'),
  isRestartMessageCollapsed: false,
  filter: '', //from template
  columns: [], //from template
  propertyFilterPopover: [Em.I18n.t('services.service.config.propertyFilterPopover.title'), Em.I18n.t('services.service.config.propertyFilterPopover.content')],
  canEdit: true, // View is editable or read-only?
  supportsHostOverrides: function () {
    switch (this.get('controller.name')) {
      case 'wizardStep7Controller':
        return this.get('controller.selectedService.serviceName') !== 'MISC';
      case 'mainServiceInfoConfigsController':
      case 'mainHostServiceConfigsController':
        return true;
      default:
        return false;
    }
  }.property('controller.name', 'controller.selectedService'),

  /**
   * Check for layout config supports.
   * @returns {Boolean}
   */
  supportsConfigLayout: function() {
    var supportedControllers = ['wizardStep7Controller', 'mainServiceInfoConfigsController'];
    var unSupportedServices = ['MISC'];

    return supportedControllers.contains(this.get('controllerName')) || !unSupportedServices.contains(this.get('controller.selectedService.serviceName'));
  }.property('controller.name', 'controller.selectedService'),

  showConfigHistoryFeature: false,
  toggleRestartMessageView: function () {
    this.$('.service-body').toggle('blind', 200);
    this.set('isRestartMessageCollapsed', !this.get('isRestartMessageCollapsed'));
  },
  didInsertElement: function () {
    if (this.get('isNotEditable') === true) {
      this.set('canEdit', false);
    }
    if (this.$('.service-body')) {
      this.$('.service-body').hide();
    }
    App.tooltip($(".restart-required-property"), {html: true});
    App.tooltip($(".icon-lock"), {placement: 'right'});
    this.checkCanEdit();
  },

  /**
   * Check if we should show Custom Property category
   */
  checkCanEdit: function () {
    var controller = this.get('controller');
    if (!controller.get('selectedService.configCategories')) {
      return;
    }

    if (controller.get('selectedConfigGroup')) {
      controller.get('selectedService.configCategories').filterProperty('siteFileName').forEach(function (config) {
        config.set('customCanAddProperty', config.get('canAddProperty'));
      });
    }

  }.observes(
      'App.router.mainServiceInfoConfigsController.selectedConfigGroup.name',
      'App.router.wizardStep7Controller.selectedConfigGroup.name'
  ),

  // TODO remove workaround for `Advanced` tab after Tab model implementation
  /**
   * Create `Advanced` tab which is not related to fancy tab section.
   * This tab contains properties without widgets.
   *
   * @returns {Ember.Object}
   */
  getAdvancedTab: function() {
    var tabId = 'advanced-' + this.get('controller.selectedService.serviceName').toLowerCase() + '-tab';
    return this.createTabObject(Em.Object.create({
      id: tabId,
      name: Em.I18n.t('common.advanced')
    }));
  },

  /**
   * @param {Ember.Object} model
   * @returns {Ember.Object}
   */
  createTabObject: function(model) {
    return Em.Object.create({
      id: model.get('id'),
      displayName: model.get('name'),
      tabClassName: '.' + model.get('id'),
      isAdvanced: model.get('name') == Em.I18n.t('common.advanced')
    });
  },

  /**
   * Object that used for Twitter Bootstrap tabs markup.
   *
   * @returns {Ember.A}
   */
  tabs: function() {
    var tabs = Em.A([]);
    // filter Tab models by service name and create object here
    tabs.pushObject(this.getAdvancedTab());
    // make first tab active
    tabs.get('firstObject').set('isActive', true);
    return tabs;
  }.property('controller.selectedService.serviceName')
});
