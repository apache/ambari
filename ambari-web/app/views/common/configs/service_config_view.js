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

  /**
   * Bound from parent view in the template
   * @type {string}
   */
  filter: '',

  /**
   * Determines that active tab is set during view initialize.
   * @type {boolean}
   */
  initalActiveTabIsSet: false,

  /**
   * Bound from parent view in the template
   * @type {object[]}
   */
  columns: [],

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
   * Determines if user is on the service configs page
   * @type {boolean}
   */
  isOnTheServicePage: function () {
    return this.get('controller.name') === 'mainServiceInfoConfigsController';
  }.property('controller.name'),

  classNameBindings: ['isOnTheServicePage:serviceConfigs'],

  /**
   * flag defines if any config match filter
   * true if all configs should be hidden
   * @type {boolean}
   */
  isAllConfigsHidden: false,

  /**
   * method that runs <code>updateFilterCounters<code> to
   * update filter counters for advanced tab
   * @method showHideAdvancedByFilter
   */
  showHideAdvancedByFilter: function () {
    Em.run.once(this, 'updateFilterCounters');
  }.observes('controller.selectedService.configs.@each.isHiddenByFilter'),

  /**
   * updates filter counters for advanced tab
   * @method updateFilterCounters
   */
  updateFilterCounters: function() {
    if (this.get('controller.selectedService.configs')) {
      var categories = this.get('controller.selectedService.configCategories').mapProperty('name');
      var configsToShow = this.get('controller.selectedService.configs').filter(function(config) {
        return config.get('isHiddenByFilter') == false && categories.contains(config.get('category')) && config.get('isVisible');
      });
      var isAllConfigsHidden = configsToShow.get('length') == 0;
      var isAdvancedHidden = isAllConfigsHidden || configsToShow.filter(function (config) {
        return Em.isNone(config.get('widget'));
      }).get('length') == 0;
      this.set('isAllConfigsHidden', isAllConfigsHidden);
      var advancedTab = App.Tab.find().filterProperty('serviceName', this.get('controller.selectedService.serviceName')).findProperty('isAdvanced');
      advancedTab && advancedTab.set('isAdvancedHidden', isAdvancedHidden);
    }
  },

  /**
   * Check for layout config supports.
   * @returns {Boolean}
   */
  supportsConfigLayout: function() {
    var supportedControllers = ['wizardStep7Controller', 'mainServiceInfoConfigsController', 'mainHostServiceConfigsController'];
    if (!App.get('isClusterSupportsEnhancedConfigs')) {
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
    App.tooltip($("[rel=tooltip]"));
    this.checkCanEdit();
  },

  willDestroyElement: function() {
    this.get('tabs').setEach('isActive', false);
  },

  /**
   * Check if we should show Custom Property category
   * @method checkCanEdit
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

  }.observes('controller.selectedConfigGroup.name'),

  setActiveTab: function (event) {
    if (event.context.get('isHiddenByFilter')) return false;
    this.set('initalActiveTabIsSet', true);
    this.get('tabs').forEach(function (tab) {
      tab.set('isActive', false);
    });
    var currentTab = event.context;
    currentTab.set('isActive', true);
    currentTab.set('isRendered', true);
  },

  /**
   * Object that used for Twitter Bootstrap tabs markup.
   *
   * @returns {Ember.A}
   */
  tabs: function() {
    if (!App.get('isClusterSupportsEnhancedConfigs')) {
      return Em.A([]);
    }
    var tabs = App.Tab.find().filterProperty('serviceName', this.get('controller.selectedService.serviceName'));
    tabs.setEach('isActive', false);
    var advancedTab = tabs.findProperty('isAdvanced', true);
    if (advancedTab) {
      advancedTab.set('isRendered', false);
    }
    this.processTabs(tabs);
    return tabs;
  }.property('controller.selectedServiceNameTrigger'),

  /**
   * Set active tab when view attached and configs are linked to tabs.
   */
  initialActiveTabObserver: function() {
    var tabs = this.get('tabs').filterProperty('isAdvanced', false);
    if (tabs.everyProperty('isConfigsPrepared', true) && !this.get('initalActiveTabIsSet')) {
      this.get('tabs').setEach('isActive', false);
      this.pickActiveTab(this.get('tabs'));
      this.set('initalActiveTabIsSet', true);
    }
  }.observes('tabs.@each.isConfigsPrepared'),

  /**
   * Pick the first non hidden tab and make it active when there is no active tab
   * @method pickActiveTab
   */
  pickActiveTab: function (tabs) {
    if (!tabs) return;
    var activeTab = tabs.findProperty('isActive', true);
    if (activeTab) {
      if (activeTab.get('isHiddenByFilter')) {
        activeTab.set('isActive', false);
        this.pickActiveTab(tabs);
      }
    }
    else {
      var firstHotHiddenTab = tabs.filterProperty('isHiddenByFilter', false).get('firstObject');
      if(firstHotHiddenTab) {
        firstHotHiddenTab.set('isActive', true);
        if (firstHotHiddenTab.get('isAdvanced') && !firstHotHiddenTab.get('isRendered')) {
          firstHotHiddenTab.set('isRendered', true);
        }
      }
    }
  },

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
          // leave a title gap if one of the subsection on the same row within the same section has title
          if (subsection.get('displayName')) {subsectionRow.hasTitleGap = true;}
        }
        section.set('subsectionRows', subsectionRows);
      }
      tab.set('sectionRows', sectionRows);
    }
  },

  /**
   * Mark isHiddenByFilter flag for configs, sub-sections, and tab
   * @method filterEnhancedConfigs
   */
  filterEnhancedConfigs: function () {
    if (!this.get('controller.selectedService')) return true;
    var self = this;

    var serviceConfigs = this.get('controller.selectedService.configs').filterProperty('isVisible', true);
    var filter = (this.get('filter')|| '').toLowerCase();
    var selectedFilters = (this.get('columns') || []).filterProperty('selected');

    if (selectedFilters.length > 0 || filter.length > 0) {
      serviceConfigs.forEach(function (config) {
        var passesFilters = true;

        selectedFilters.forEach(function (filter) {
          if (config.get(filter.attributeName) !== filter.attributeValue) {
            passesFilters = false;
          }
        });

        if (!passesFilters) {
          config.set('isHiddenByFilter', true);
          return false;
        }

        var searchString = config.get('savedValue') + config.get('description') +
          config.get('displayName') + config.get('name') + config.get('value') + config.getWithDefault('stackConfigProperty.displayName', '');

        if (config.get('overrides')) {
          config.get('overrides').forEach(function (overriddenConf) {
            searchString += overriddenConf.get('value') + overriddenConf.get('group.name');
          });
        }

        if (filter != null && typeof searchString === "string") {
          config.set('isHiddenByFilter', !(searchString.toLowerCase().indexOf(filter) > -1));
        } else {
          config.set('isHiddenByFilter', false);
        }
      });
    }
    else {
      serviceConfigs.setEach('isHiddenByFilter', false);
    }

    Em.run.next(function () {
      self.pickActiveTab(self.get('tabs'));
    });
  }.observes('filter', 'columns.@each.selected', 'tabs.@each.isHiddenByFilter')
});
