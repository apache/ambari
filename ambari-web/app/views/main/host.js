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
  /**
   * List of hosts in cluster
   * @type {Array}
   */
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

  willDestroyElement: function() {
    this.get('categories').forEach(function(c) {
      if (c.get('observes')) {
        c.removeObserver(c.get('observes'), c, c.updateHostsCount);
      }
    });
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

  /**
   * Confirmation Popup for bulk Operations
   */
  bulkOperationConfirm: function(operationData, selection) {
    var hosts = [];
    var self = this;
    switch(selection) {
      case 's':
        hosts = this.get('content').filterProperty('selected');
        break;
      case 'f':
        hosts = this.get('filteredContent');
        break;
      case 'a':
        hosts = this.get('content').toArray();
        break;
    }
    // no hosts - no actions
    if (!hosts.length) {
      console.log('No bulk operation if no hosts selected.');
      return;
    }
    var hostNames = hosts.mapProperty('hostName');
    var hostsToSkip = [];
    if (operationData.action == "DECOMMISSION") {
      hostsToSkip = hosts.filter(function(host) {
        var invalidStateComponents = host.get('hostComponents').filter(function(component) {
          return component.get('componentName') == operationData.realComponentName && component.get('workStatus') == 'INSTALLED';
        });
        return invalidStateComponents.length > 0;
      });
    }
    var hostNamesSkipped = hostsToSkip.mapProperty('hostName');
    var message;
    if (operationData.componentNameFormatted) {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hostComponents').format(operationData.message, operationData.componentNameFormatted, hostNames.length);
    }
    else {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hosts').format(operationData.message, hostNames.length);
    }
    App.ModalPopup.show({
      header: Em.I18n.t('hosts.bulkOperation.confirmation.header'),
      hostNames: hostNames.join("\n"),
      hostNamesSkipped: function() {
        if (hostNamesSkipped.length) {
          return hostNamesSkipped.join("<br/>");
        }
        return false;
      }.property(),
      onPrimary: function() {
        self.get('controller').bulkOperation(operationData, hosts);
        this._super();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_operation_confirm_popup'),
        message: message,
        warningInfo: Em.I18n.t('hosts.bulkOperation.warningInfo.body'),
        textareaVisible: false,
        textTrigger: function() {
          this.set('textareaVisible', !this.get('textareaVisible'));
        },
        putHostNamesToTextarea: function() {
          var hostNames = this.get('parentView.hostNames');
          if (this.get('textareaVisible')) {
            var wrapper = $(".task-detail-log-maintext");
            $('.task-detail-log-clipboard').html(hostNames).width(wrapper.width()).height(250);
            Em.run.next(function() {
              $('.task-detail-log-clipboard').select();
            });
          }
        }.observes('textareaVisible')
      })
    });
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
      this.set('isComponentsCollapsed', true);
    },

    toggleComponents: function(event) {
      this.set('isComponentsCollapsed', !this.get('isComponentsCollapsed'));
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
    componentsInPassiveStateMessage: function() {
      var componentsInPassiveState = this.get('content.hostComponents').filter(function(component) {
        return component.get('passiveState') !== 'OFF';
      });
      var count = componentsInPassiveState.length;
      if (count <= 5) {
        return Em.I18n.t('hosts.table.componentsInPassiveState.withNames').format(componentsInPassiveState.getEach('displayName').join(', '));
      }
      return Em.I18n.t('hosts.table.componentsInPassiveState.withoutNames').format(count);
    }.property('content.hostComponents.@each.passiveState'),

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
   * @type {Object}
   */
  //@TODO maybe should be separated to two types (basing on <code>isHealthStatus</code>)
  categoryObject: Em.Object.extend({

    /**
     * Text used with <code>hostsCount</code> in category label
     * @type {String}
     */
    value: null,
    /**
     * Is category based on host health status
     * @type {bool}
     */
    isHealthStatus: true,
    /**
     * host health status (used if <code>isHealthStatus</code> is true)
     * @type {String}
     */
    healthStatusValue: '',
    /**
     * Should category be displayed on the top of the hosts table
     * @type {bool}
     */
    isVisible: true,
    /**
     * Is category selected now
     * @type {bool}
     */
    isActive: false,
    /**
     * String with path that category should observe
     * @type {String}
     */
    observes: null,
    /**
     * CSS-class for span in the category-link (used if <code>isHealthStatus</code> is false)
     * @type {String}
     */
    class: null,
    /**
     * Associated column number
     * @type {Number}
     */
    column: null,
    /**
     * Type of filter value (string, number, boolean)
     * @type {String}
     */
    type: null,
    /**
     * @type {String|Number|bool}
     */
    filterValue: null,
    /**
     * <code>App.Host</code> property that should be used to calculate <code>hostsCount</code> (used if <code>isHealthStatus</code> is false)
     * @type {String}
     */
    hostProperty: null,

    /**
     * Number of host in current category
     * @type {Number}
     */
    hostsCount: 0,

    /**
     * Determine if category has hosts
     * @type {bool}
     */
    hasHosts: false,

    /**
     * Add "active" class for category span-wrapper if current category is selected
     * @type {String}
     */
    itemClass: function() {
      return this.get('isActive') ? 'active' : '';
    }.property('isActive'),

    /**
     * Trigger updating <code>hostsCount</code> only 1 time
     */
    updateHostsCount: function() {
      Em.run.once(this, 'updateOnce');
    },
    /**
     * Update <code>hostsCount</code> in current category
     */
    updateOnce: function() {
      //skip update when view is destroyed
      if(!this.get('view.content')) return;
      var statusString = this.get('healthStatusValue');
      if (this.get('isHealthStatus')) {
        if (statusString == "") {
          this.set('hostsCount', this.get('view.content').get('length'));
        }
        else {
          this.set('hostsCount', this.get('view.content').filterProperty('healthClass', statusString).get('length'));
        }
      }
      else {
        this.set('hostsCount', this.get('view.content').filterProperty(this.get('hostProperty')).get('length'));
      }
      this.set('hasHosts', !!this.get('hostsCount'));
    },

    /**
     * Text shown on the right of category icon
     * @type {String}
     */
    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('hostsCount')
  }),

  /**
   * List of categories used to filter hosts
   * @type {Array}
   */
  categories: function () {
    var self = this;
    self.categoryObject.reopen({
      view: self
    });

    var category_mocks = require('data/host/categories');

    return category_mocks.map(function(category_mock) {
      var c = self.categoryObject.create(category_mock);
      if (c.get('observes')) {
        c.addObserver(c.get('observes'), c, c.updateHostsCount);
        c.updateHostsCount();
      }
      return c;
    });
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
    class: "",
    comboBoxLabel: function(){
      var selected = this.get('categories').findProperty('isActive',true);
      if (!this.get('value') || !selected) {
        return "%@ (%@)".fmt(Em.I18n.t('common.all'), this.get('parentView.content.length'));
      } else {
        return "%@ (%@)".fmt(selected.get('value'), selected.get('hostsCount'))
      }
    }.property('value'),
    /**
     * switch active category label
     */
    onCategoryChange: function() {
      this.get('categories').setEach('isActive', false);
      var selected = this.get('categories').findProperty('healthStatusValue', this.get('value'));
      selected.set('isActive', true);
      this.set('class', selected.get('class') || this.get('value'));
    }.observes('value'),

    showClearFilter: function(){
      var mockEvent = {
        context: this.get('categories').findProperty('healthStatusValue', this.get('value'))
      };
      this.selectCategory(mockEvent);
    },
    /**
     * Trigger on Category click
     * @param {Object} event
     */
    selectCategory: function(event){
      var category = event.context;
      var self = this;
      this.set('value', category.get('healthStatusValue'));
      if (category.get('isHealthStatus')) {
        this.get('parentView').updateFilter(0, category.get('healthStatusValue'), 'string');
        this.get('categories').filterProperty('isHealthStatus', false).forEach(function(c) {
          self.get('parentView').updateFilter(c.get('column'), '', c.get('type'));
        });
      }
      else {
        this.get('parentView').updateFilter(0, '', 'string');
        this.get('categories').filterProperty('isHealthStatus', false).forEach(function(c) {
          if (c.get('column') === category.get('column')) {
            self.get('parentView').updateFilter(category.get('column'), category.get('filterValue'), category.get('type'));
          }
          else {
            self.get('parentView').updateFilter(c.get('column'), '', c.get('type'));
          }
        });
      }
    },
    clearFilter: function() {
      this.get('categories').setEach('isActive', false);
      this.set('value', '');
      this.set('class', '');
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
  passiveStateFilter: Em.View.extend({
    column: 9,
    value: null,
    classNames: ['noDisplay'],
    showClearFilter: function(){
      var mockEvent = {
        context: this.get('parentView.categories').findProperty('healthStatusValue', 'health-status-PASSIVE_STATE')
      };
      if(this.get('value')) {
        this.get('parentView.childViews').findProperty('column', 0).selectCategory(mockEvent);
      }
    }
  }),

  /**
   * view of the "selected" filter implemented as a category of host statuses
   */
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
    fieldType: 'filter-input-width',
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
    fieldType: 'filter-input-width',
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * Filter view for Cpu column
   * Based on <code>filters</code> library
   */
  cpuFilterView: filters.createTextView({
    fieldType: 'filter-input-width',
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
    fieldType: 'filter-input-width',
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
    fieldType: 'filter-input-width',
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
   * @type {Array}
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
    associations[9] = 'componentsInPassiveStateCount';
    associations[10] = 'selected';
    return associations;
  }.property()
});
