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

  clearFiltersObs: function() {
    var self = this;
    Em.run.next(function() {
      if (self.get('controller.clearFilters')) {
        self.clearFilters();
        self.clearStartIndex();
      }
    });
  },

  didInsertElement: function() {
    this.addObserver('controller.clearFilters', this, this.clearFiltersObs);
    this.clearFiltersObs();
    this.addObserver('selectAllHosts', this, this.toggleAllHosts);
  },

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('hosts.filters.filteredHostsInfo').format(this.get('filteredContent.length'), this.get('content').get('length'));
  }.property('content.length', 'filteredContent.length'),

  /**
   * Select/deselect all visible hosts flag
   * @property {bool}
   */
  selectAllHosts: false,

  /**
   * Set <code>selected</code> property for each App.Host
   */
  toggleAllHosts: function() {
    this.get('pageContent').setEach('selected', this.get('selectAllHosts'));
  },

  /**
   * Trigger updating <code>selectedHostsCount</code> only 1 time
   */
  selectedHostsObserver: function() {
    Ember.run.once(this, 'updateCheckedFlags');
  }.observes('pageContent.@each.selected'),

  /**
   * Update <code>selectAllHosts</code> value
   */
  updateCheckedFlags: function() {
    this.removeObserver('selectAllHosts', this, this.toggleAllHosts);
    if (this.get('pageContent').length) {
      this.set('selectAllHosts', this.get('pageContent').everyProperty('selected', true));
    }
    else {
      this.set('selectAllHosts', false);
    }
    this.addObserver('selectAllHosts', this, this.toggleAllHosts);
  },

  /**
   * Clear selectedFilter
   * Set <code>selected</code> to false for each host
   */
  clearSelection: function() {
    this.get('pageContent').setEach('selected', false);
    this.set('selectAllHosts', false);
    this.clearFilters();
  },

  sortView: sort.wrapperView,
  nameSort: sort.fieldView.extend({
    column: 1,
    name:'publicHostName',
    displayName: Em.I18n.t('common.name')
  }),
  ipSort: sort.fieldView.extend({
    column: 2,
    name:'ip',
    displayName: Em.I18n.t('common.ipAddress'),
    type: 'ip'
  }),
  cpuSort: sort.fieldView.extend({
    column: 3,
    name:'cpu',
    displayName: Em.I18n.t('common.cores'),
    type: 'number'
  }),
  memorySort: sort.fieldView.extend({
    column: 4,
    name:'memory',
    displayName: Em.I18n.t('common.ram'),
    type: 'number'
  }),
  diskUsageSort: sort.fieldView.extend({
    name:'diskUsage',
    displayName: Em.I18n.t('common.diskUsage')
  }),
  loadAvgSort: sort.fieldView.extend({
    column: 5,
    name:'loadAvg',
    displayName: Em.I18n.t('common.loadAvg'),
    type: 'number'
  }),

  HostView:Em.View.extend({
    content:null,
    tagName: 'tr',
    didInsertElement: function(){
      App.tooltip(this.$("[rel='HealthTooltip'], [rel='UsageTooltip'], [rel='ComponentsTooltip']"));
    },

    toggleComponents: function(event) {
      this.$(event.target).find('.caret').toggleClass('right');
      this.$('.host-components').toggle();
    },

    /**
     * Tooltip message for "Restart Required" icon
     * @returns {String}
     */
    restartRequiredComponentsMessage: function() {
      var restartRequiredComponents = this.get('content.hostComponents').filterProperty('staleConfigs', true);
      var count = restartRequiredComponents.length;
      if (count <= 5) {
        var word = (count == 1) ? Em.I18n.t('common.component') : Em.I18n.t('common.components');
        return Em.I18n.t('hosts.table.restartComponents.withNames').format(restartRequiredComponents.getEach('displayName').join(', ')) + ' ' + word.toLowerCase();
      }
      return Em.I18n.t('hosts.table.restartComponents.withoutNames').format(count);
    }.property('content.hostComponents.@each.staleConfigs'),

    /**
     * Tooltip message for "Maintenance" icon
     * @returns {String}
     */
    componentsInMaintenanceMessage: function() {
      var componentsInMaintenance = this.get('content.hostComponents').filterProperty('workStatus', App.HostComponentStatus.maintenance);
      var count = componentsInMaintenance.length;
      if (count <= 5) {
        return Em.I18n.t('hosts.table.componentsInMaintenance.withNames').format(componentsInMaintenance.getEach('displayName').join(', '));
      }
      return Em.I18n.t('hosts.table.componentsInMaintenance.withoutNames').format(count);
    }.property('content.hostComponents.@each.workStatus'),

    /**
     * String with list of host components <code>displayName</code>
     * @returns {String}
     */
    labels: function() {
      return this.get('content.hostComponents').getEach('displayName').join("<br />");
    }.property('content.hostComponents.length'),

    /**
     * CSS value for disk usage bar
     * @returns {String}
     */
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
      var restart = this.get('restart');
      var maintenance = this.get('maintenance');
      var selected = this.get('selected');
      if(alerts) {
        return this.get('view.content').filterProperty('criticalAlertsCount').get('length');
      }
      else {
        if (restart) {
          return this.get('view.content').filterProperty('componentsWithStaleConfigsCount').get('length');
        }
        else {
          if (maintenance) {
            return this.get('view.content').filterProperty('componentsInMaintenanceCount').get('length');
          }
          else {
            if (selected) {
              return this.get('view.content').filterProperty('selected').get('length');
            }
            else {
              if (statusString == "") {
                return this.get('view.content').get('length');
              }
              else {
                return this.get('view.content').filterProperty('healthClass', statusString ).get('length');
              }
            }
          }
        }
      }
    }.property('view.content.@each.selected', 'view.content.@each.healthClass', 'view.content.@each.criticalAlertsCount', 'view.content.@each.componentsInMaintenanceCount', 'view.content.@each.componentsWithStaleConfigsCount'),

    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('hostsCount')
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

    return [
      self.categoryObject.create({value: Em.I18n.t('common.all'), healthStatusValue: '', isActive: true, isVisible: false}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.green'), healthStatusValue: 'health-status-LIVE', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.red'), healthStatusValue: 'health-status-DEAD-RED', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.orange'), healthStatusValue: 'health-status-DEAD-ORANGE', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.yellow'), healthStatusValue: 'health-status-DEAD-YELLOW', isVisible: true}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.alerts.label'), healthStatusValue: 'health-status-WITH-ALERTS', alerts: true, isVisible: true }),
      self.categoryObject.create({value: Em.I18n.t('common.restart'), healthStatusValue: 'health-status-RESTART', restart: true, isVisible: true }),
      self.categoryObject.create({value: Em.I18n.t('common.selected'), healthStatusValue: 'health-status-SELECTED', selected: true, isVisible: false }),
      self.categoryObject.create({value: Em.I18n.t('common.maintenance'), healthStatusValue: 'health-status-MAINTENANCE', maintenance: true, last: true, isVisible: true })
    ];
  }.property(),

  /**
   * Category for <code>selected</code> property of each App.Host
   */
  selectedCategory: function() {
    return this.get('categories').findProperty('selected', true);
  }.property('categories.@each.selected'),

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
      if(category.get('alerts')) {
        this.get('parentView').updateFilter(0, '', 'string');
        this.get('parentView').updateFilter(7, '>0', 'number');
        this.get('parentView').updateFilter(8, '', 'number');
        this.get('parentView').updateFilter(9, '', 'number');
        this.get('parentView').updateFilter(10, '', 'boolean');
      }
      else {
        if(category.get('restart')) {
          this.get('parentView').updateFilter(0, '', 'string');
          this.get('parentView').updateFilter(7, '', 'number');
          this.get('parentView').updateFilter(8, '>0', 'number');
          this.get('parentView').updateFilter(9, '', 'number');
          this.get('parentView').updateFilter(10, '', 'boolean');
        }
        else {
          if(category.get('maintenance')) {
            this.get('parentView').updateFilter(0, '', 'string');
            this.get('parentView').updateFilter(7, '', 'number');
            this.get('parentView').updateFilter(8, '', 'number');
            this.get('parentView').updateFilter(9, '>0', 'number');
            this.get('parentView').updateFilter(10, '', 'boolean');
          }
          else {
            if(category.get('selected')) {
              this.get('parentView').updateFilter(0, '', 'string');
              this.get('parentView').updateFilter(7, '', 'number');
              this.get('parentView').updateFilter(8, '', 'number');
              this.get('parentView').updateFilter(9, '', 'number');
              this.get('parentView').updateFilter(10, true, 'boolean');
            }
            else {
              this.get('parentView').updateFilter(0, category.get('healthStatusValue'), 'string');
              this.get('parentView').updateFilter(7, '', 'number');
              this.get('parentView').updateFilter(8, '', 'number');
              this.get('parentView').updateFilter(9, '', 'number');
              this.get('parentView').updateFilter(10, '', 'boolean');
            }
          }
        }
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
   * view of the staleConfigs filter implemented as a category of host statuses
   */
  restartFilter: Em.View.extend({
    column: 8,
    value: null,
    classNames: ['noDisplay'],
    showClearFilter: function(){
      var mockEvent = {
        context: this.get('parentView.categories').findProperty('healthStatusValue', 'health-status-RESTART')
      };
      if(this.get('value')) {
        this.get('parentView.childViews').findProperty('column', 0).selectCategory(mockEvent);
      }
    }
  }),

  /**
   * view of the maintenance filter implemented as a category of host statuses
   */
  maintenanceFilter: Em.View.extend({
    column: 9,
    value: null,
    classNames: ['noDisplay'],
    showClearFilter: function(){
      var mockEvent = {
        context: this.get('parentView.categories').findProperty('healthStatusValue', 'health-status-MAINTENANCE')
      };
      if(this.get('value')) {
        this.get('parentView.childViews').findProperty('column', 0).selectCategory(mockEvent);
      }
    }
  }),

  selectedFilter: Em.View.extend({
    column: 10,
    value: null,
    class: ['noDisplay'],
    showClearFilter: function(){
      var mockEvent = {
        context: this.get('parentView.categories').findProperty('healthStatusValue', 'health-status-SELECTED')
      };
      if(this.get('value')) {
        this.get('parentView.childViews').findProperty('column', 0).selectCategory(mockEvent);
      }
    }
  }),


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
    associations[8] = 'componentsWithStaleConfigsCount';
    associations[9] = 'componentsInMaintenanceCount';
    associations[10] = 'selected';
    return associations;
  }.property()
});