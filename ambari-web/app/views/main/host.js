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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');
var date = require('utils/date');

App.MainHostView = App.TableView.extend({
  templateName:require('templates/main/host'),
  content:function () {
    return this.get('controller.content');
  }.property('controller.content.length'),

  willInsertElement: function() {
    this._super();
  },

  clearFiltersObs: function() {
    var self = this;
    Em.run.next(function() {
      if (self.get('controller.clearFilters')) {
        self.clearFilters();
        self.clearDisplayLength();
        self.clearStartIndex();
      }
    });
  },

  didInsertElement: function() {
    this.addObserver('controller.clearFilters', this, this.clearFiltersObs);
    this.clearFiltersObs();
  },

  sortView: sort.wrapperView,
  nameSort: sort.fieldView.extend({
    name:'publicHostName',
    displayName: Em.I18n.t('common.name')
  }),
  ipSort: sort.fieldView.extend({
    name:'ip',
    displayName: Em.I18n.t('common.ipAddress'),
    type: 'ip'
  }),
  cpuSort: sort.fieldView.extend({
    name:'cpu',
    displayName: Em.I18n.t('common.cpu'),
    type: 'number'
  }),
  memorySort: sort.fieldView.extend({
    name:'memory',
    displayName: Em.I18n.t('common.ram'),
    type: 'number'
  }),
  diskUsageSort: sort.fieldView.extend({
    name:'diskUsage',
    displayName: Em.I18n.t('common.diskUsage')
  }),
  loadAvgSort: sort.fieldView.extend({
    name:'loadAvg',
    displayName: Em.I18n.t('common.loadAvg'),
    type: 'number'
  }),

  HostView:Em.View.extend({
    content:null,
    tagName: 'tr',
    didInsertElement: function(){
      this.$("[rel='HealthTooltip'], [rel='UsageTooltip']").tooltip();
    },

    toggleComponents: function(event) {
      this.$(event.target).find('.caret').toggleClass('right');
      this.$('.host-components').toggle();
    },

    componentsMessage: function() {
      var count = this.get('content.hostComponents.length');
      if (count == 1) {
        return count + ' ' + Em.I18n.t('common.component');
      }
      else {
        return count + ' ' + Em.I18n.t('common.components');
      }
    }.property('content.hostComponents.@each'),

    labels: function() {
      return this.get('content.hostComponents').getEach('displayName').join("<br />");
    }.property('content.hostComponents.@each'),

    usageStyle:function () {
      return "width:" + this.get('content.diskUsage') + "%";
    }.property('content.diskUsage')

  }),

  /**
   * Category view for all hosts
   */
  categoryObject: Em.Object.extend({

    hostsCount: function () {
      var statusString = this.get('healthStatusValue');
      var alerts = this.get('alerts');
      if(alerts){
        return this.get('view.content').filterProperty('criticalAlertsCount').get('length');
      } else if (statusString == "") {
        return this.get('view.content').get('length');
      } else {
        return this.get('view.content').filterProperty('healthClass', statusString ).get('length');
      }
    }.property('view.content.@each.healthClass', 'view.content.@each.criticalAlertsCount'),

    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount')
  }),

  categories: function () {
    var self = this;
    self.categoryObject.reopen({
      view: self,
      isActive: false,
      itemClass: function() {
        return this.get('isActive') ? 'active' : '';
      }.property('isActive')
    });

    var categories = [
      self.categoryObject.create({value: Em.I18n.t('common.all'), healthStatusValue: '', isActive: true, isVisible: false}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.green'), healthStatusValue: 'health-status-LIVE', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.red'), healthStatusValue: 'health-status-DEAD-RED', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.orange'), healthStatusValue: 'health-status-DEAD-ORANGE', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.yellow'), healthStatusValue: 'health-status-DEAD-YELLOW', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.alerts.label'), healthStatusValue: 'health-status-WITH-ALERTS', last: true, alerts: true, isVisible: true })
    ];

    return categories;
  }.property(),


  statusFilter: Em.View.extend({
    column: 0,
    categories: [],
    value: null,
    /**
     * switch active category label
     */
    onCategoryChange: function(){
      this.get('categories').setEach('isActive', false);
      this.get('categories').findProperty('healthStatusValue', this.get('value')).set('isActive', true);
    }.observes('value'),
    showClearFilter: function(){
      var mockEvent = {
        context: this.get('categories').findProperty('healthStatusValue', this.get('value'))
      };
      this.selectCategory(mockEvent);
    },
    selectCategory: function(event){
      var category = event.context;
      this.set('value', category.get('healthStatusValue'));
      if(category.get('alerts')){
        this.get('parentView').updateFilter(0, '', 'string');
        this.get('parentView').updateFilter(7, '>0', 'number');
      } else {
        this.get('parentView').updateFilter(7, '', 'number');
        this.get('parentView').updateFilter(0, category.get('healthStatusValue'), 'string');
      }
    },
    clearFilter: function() {
      this.get('categories').setEach('isActive', false);
      this.set('value', '');
      this.showClearFilter();
    }
  }),

  /**
   * view of the alert filter implemented as a category of host statuses
   */
  alertFilter: Em.View.extend({
    column: 7,
    value: null,
    classNames: ['noDisplay'],
    showClearFilter: function(){
      var mockEvent = {
        context: this.get('parentView.categories').findProperty('healthStatusValue', 'health-status-WITH-ALERTS')
      };
      if(this.get('value')) {
        this.get('parentView.childViews').findProperty('column', 0).selectCategory(mockEvent);
      }
    }
  }),


  /**
   * Count of the hosts with alerts
   */
  hostsWithAlertsCount: function() {
    return this.get('content.length') - this.get('content').filterProperty('criticalAlertsCount', 0).length;
  }.property('content.@each.criticalAlertsCount'),


  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  nameFilterView: filters.createTextView({
    column: 1,
    fieldType: 'width70',
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * Filter view for ip column
   * Based on <code>filters</code> library
   */
  ipFilterView: filters.createTextView({
    column: 2,
    fieldType: 'width70',
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * Filter view for Cpu column
   * Based on <code>filters</code> library
   */
  cpuFilterView: filters.createTextView({
    fieldType: 'width70',
    fieldId: 'cpu_filter',
    column: 3,
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'number');
    }
  }),

  /**
   * Filter view for LoadAverage column
   * Based on <code>filters</code> library
   */
  loadAvgFilterView: filters.createTextView({
    fieldType: 'width70',
    fieldId: 'load_avg_filter',
    column: 5,
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'number');
    }
  }),

  /**
   * Filter view for Ram column
   * Based on <code>filters</code> library
   */
  ramFilterView: filters.createTextView({
    fieldType: 'width70',
    fieldId: 'ram_filter',
    column: 4,
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
    }
  }),

  /**
   * Filter view for HostComponents column
   * Based on <code>filters</code> library
   */
  componentsFilterView: filters.createComponentView({

    column: 6,

    /**
     * Inner FilterView. Used just to render component. Value bind to <code>mainview.value</code> property
     * Base methods was implemented in <code>filters.componentFieldView</code>
     */
    filterView: filters.componentFieldView.extend({
      templateName: require('templates/main/host/component_filter'),

      /**
       * Next three lines bind data to this view
       */
      masterComponentsBinding: 'controller.masterComponents',
      slaveComponentsBinding: 'controller.slaveComponents',
      clientComponentsBinding: 'controller.clientComponents',

      /**
       * Checkbox for quick selecting/deselecting of master components
       */
      masterComponentsChecked:false,
      toggleMasterComponents:function () {
        this.get('masterComponents').setEach('checkedForHostFilter', this.get('masterComponentsChecked'));
      }.observes('masterComponentsChecked'),

      /**
       * Checkbox for quick selecting/deselecting of slave components
       */
      slaveComponentsChecked:false,
      toggleSlaveComponents:function () {
        this.get('slaveComponents').setEach('checkedForHostFilter', this.get('slaveComponentsChecked'));
      }.observes('slaveComponentsChecked'),

      /**
       * Checkbox for quick selecting/deselecting of client components
       */
      clientComponentsChecked: false,
      toggleClientComponents: function() {
        this.get('clientComponents').setEach('checkedForHostFilter', this.get('clientComponentsChecked'));
      }.observes('clientComponentsChecked'),

      /**
       * Clear filter.
       * Called by parent view, when user clicks on <code>x</code> button(clear button)
       */
      clearFilter:function() {
        this.set('masterComponentsChecked', false);
        this.set('slaveComponentsChecked', false);
        this.set('clientComponentsChecked', false);

        this.get('masterComponents').setEach('checkedForHostFilter', false);
        this.get('slaveComponents').setEach('checkedForHostFilter', false);
        this.get('clientComponents').setEach('checkedForHostFilter', false);

        this._super();
      },

      /**
       * Onclick handler for <code>Apply filter</code> button
       */
      applyFilter:function() {
        this._super();
        var self = this;
        var chosenComponents = [];

        this.get('masterComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
          chosenComponents.push(item.get('id'));
        });
        this.get('slaveComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
          chosenComponents.push(item.get('id'));
        });
        this.get('clientComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
          chosenComponents.push(item.get('id'));
        });
        Em.run.next(function() {
          self.set('value', chosenComponents.toString());
        });
      },

      /**
       * Verify that checked checkboxes are equal to value stored in hidden field (components ids list)
       */
      checkComponents: function() {
        var components = this.get('value').split(',');
        var self = this;
        if (components) {
          components.forEach(function(componentId) {
            if(!self.tryCheckComponent(self, 'masterComponents', componentId)) {
              if(!self.tryCheckComponent(self, 'slaveComponents', componentId)) {
                self.tryCheckComponent(self, 'clientComponents', componentId);
              }
            }
          });
        }
      }.observes('value'),

      tryCheckComponent: function(self, category, componentId) {
        var c = self.get(category).findProperty('id', componentId);
        if (c) {
          if (!c.get('checkedForHostFilter')) {
            c.set('checkedForHostFilter', true);
            return true;
          }
        }
        return false;
      }

    }),
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'multiple');
    }
  }),

  /**
   * associations between host property and column index
   */
  colPropAssoc: function(){
    var associations = [];
    associations[0] = 'healthClass';
    associations[1] = 'publicHostName';
    associations[2] = 'ip';
    associations[3] = 'cpu';
    associations[4] = 'memoryFormatted';
    associations[5] = 'loadAvg';
    associations[6] = 'hostComponents';
    associations[7] = 'criticalAlertsCount';
    return associations;
  }.property()
});
