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
    var supportedControllers = ['wizardStep7Controller', 'mainServiceInfoConfigsController', 'mainHostServiceConfigsController'];
    if (!App.get('supports.enhancedConfigs')) {
      return false;
    }
    if (App.Tab.find().someProperty('serviceName', this.get('controller.selectedService.serviceName')) && supportedControllers.contains(this.get('controller.name'))) {
      return !Em.isEmpty(App.Tab.find().filterProperty('serviceName', this.get('controller.selectedService.serviceName')).filterProperty('isAdvanced', false));
    } else {
      return false;
    }
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

  /**
   * Object that used for Twitter Bootstrap tabs markup.
   *
   * @returns {Ember.A}
   */
  tabs: function() {
    if (!App.get('supports.enhancedConfigs')) {
      return Em.A([]);
    }
    var tabs = App.Tab.find().filterProperty('serviceName', this.get('controller.selectedService.serviceName'));
    // make first tab active
    this.processTabs(tabs);
    tabs.get('firstObject').set('isActive', true);
    return tabs;
  }.property('controller.selectedService.serviceName'),

  /**
   * Data reordering before rendering.
   * Reorder all sections/subsections into rows based on their rowIndex
   * @param tabs
   */
  processTabs: function (tabs) {
    for (var i = 0; i < tabs.length; i++) {
      var tab = tabs[i];

      // process sections
      var sectionRows = [];
      var sections = tab.get('sections');
      for (var j = 0; j < sections.get('length'); j++) {
        var section = sections.objectAt(j);
        var sectionRow = sectionRows[section.get('rowIndex')];
        if (!sectionRow) { sectionRow = sectionRows[section.get('rowIndex')] = []; }
        sectionRow.push(section);

        //process subsections
        var subsections = section.get('subSections');
        var subsectionRows = [];
        for (var k = 0; k < subsections.get('length'); k++) {
          var subsection = subsections.objectAt(k);
          var subsectionRow = subsectionRows[subsection.get('rowIndex')];
          if (!subsectionRow) { subsectionRow = subsectionRows[subsection.get('rowIndex')] = []; }
          subsectionRow.push(subsection);
        }
        section.set('subsectionRows', subsectionRows);
      }
      tab.set('sectionRows', sectionRows);
    }
  }

});
