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
var date = require('utils/date/date');

App.MainHostView = App.TableView.extend(App.TableServerViewMixin, {
  templateName:require('templates/main/host'),

  tableName: 'Hosts',
  updaterBinding: 'App.router.updateController',
  filterConditions: [],

  /**
   * Select/deselect all visible hosts flag
   * @property {bool}
   */
  selectAllHosts: false,

  /**
   * Contains all selected hosts on cluster
   */
  selectedHosts: [],

  /**
   * Request error data
   */
  requestError: null,

  /**
   * List of hosts in cluster
   * @type {Array}
   */
  contentBinding: 'controller.content',

  onRequestErrorHandler: function() {
    this.set('requestError', null);
    this.get('controller').get('dataSource').setEach('isRequested', false);
    this.set('filteringComplete', true);
    this.propertyDidChange('pageContent');
  }.observes('requestError'),

  /**
   * flag to toggle displaying selected hosts counter
   */
  showSelectedFilter: Em.computed.bool('selectedHosts.length'),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: Em.computed.i18nFormat('hosts.filters.filteredHostsInfo', 'filteredCount', 'totalCount'),

  /**
   * request latest data filtered by new parameters
   * called when trigger property(<code>refreshTriggers</code>) is changed
   */
  refresh: function () {
    App.loadTimer.start('Hosts Page');
    this.set('filteringComplete', false);
    var updaterMethodName = this.get('updater.tableUpdaterMap')[this.get('tableName')];
    this.get('updater')[updaterMethodName](this.updaterSuccessCb.bind(this), this.updaterErrorCb.bind(this), true);
    return true;
  },

  /**
   * reset filters value by column to which filter belongs
   * @param columns {Array}
   */
  resetFilterByColumns: function (columns) {
    var filterConditions = this.get('filterConditions');
    columns.forEach(function (iColumn) {
      var filterCondition = filterConditions.findProperty('iColumn', iColumn);

      if (filterCondition) {
        filterCondition.value = '';
        this.saveFilterConditions(filterCondition.iColumn, filterCondition.value, filterCondition.type, filterCondition.skipFilter);
      }
    }, this);
  },

  /**
   * Return pagination information displayed on the page
   * @type {String}
   */
  paginationInfo: Em.computed.i18nFormat('tableView.filters.paginationInfo', 'startIndex', 'endIndex', 'filteredCount'),

  paginationLeftClass: function () {
    if (this.get("startIndex") > 1 && this.get('filteringComplete')) {
      return "paginate_previous";
    }
    return "paginate_disabled_previous";
  }.property("startIndex", 'filteringComplete'),

  previousPage: function () {
    if (this.get('paginationLeftClass') === 'paginate_previous') {
      this._super();
    }
  },

  paginationRightClass: function () {
    if ((this.get("endIndex")) < this.get("filteredCount") && this.get('filteringComplete')) {
      return "paginate_next";
    }
    return "paginate_disabled_next";
  }.property("endIndex", 'filteredCount', 'filteringComplete'),

  nextPage: function () {
    if (this.get('paginationRightClass') === 'paginate_next') {
      this._super();
    }
  },

  /**
   * Select View with list of "rows-per-page" options
   * @type {Ember.View}
   */
  rowsPerPageSelectView: Em.Select.extend({
    content: ['10', '25', '50', '100'],
    attributeBindings: ['disabled'],
    disabled: true,

    disableView: function () {
      Em.run.next(this, function(){
        this.set('disabled', !this.get('parentView.filteringComplete'));
      });
    }.observes('parentView.filteringComplete'),

    change: function () {
      this.get('parentView').saveDisplayLength();
      var self = this;
      if (this.get('parentView.startIndex') !== 1 && this.get('parentView.startIndex') !== 0) {
        Ember.run.next(function () {
          self.set('parentView.startIndex', 1);
        });
      }
    }
  }),

  saveStartIndex: function () {
    this.set('controller.startIndex', this.get('startIndex'));
  }.observes('startIndex'),

  clearFiltersObs: function() {
    var self = this;
    Em.run.next(function() {
      if (self.get('controller.clearFilters')) {
        self.clearFilters();
      }
    });
  },
  /**
   * Restore filter properties in view
   */
  willInsertElement: function () {
    if (!this.get('controller.showFilterConditionsFirstLoad')) {
      var didClearedSomething = this.clearFilterConditionsFromLocalStorage();
      this.set('controller.filterChangeHappened', didClearedSomething);
    }
    this._super();
    this.set('startIndex', this.get('controller.startIndex'));
    this.addObserver('pageContent.@each.selected', this, this.selectedHostsObserver);
  },

  /**
   * stub for filter function in TableView
   */
  filter: function () {
    //Since filtering moved to server side, function is empty
  },

  didInsertElement: function() {
    this.addObserver('controller.clearFilters', this, this.clearFiltersObs);
    this.clearFiltersObs();
    this.addObserver('selectAllHosts', this, this.toggleAllHosts);
    this.addObserver('filteringComplete', this, this.overlayObserver);
    this.addObserver('startIndex', this, 'updatePagination');
    this.addObserver('displayLength', this, 'updatePagination');
    this.addObserver('filteredCount', this, this.updatePaging);
  },

  willDestroyElement: function () {
    $('.tooltip').remove();
  },

  onInitialLoad: function () {
    if (this.get('tableFilteringComplete')) {
      if (this.get('controller.filterChangeHappened')) {
        this.refresh();
      } else {
        // no refresh but still need to enable pagination controls
        this.propertyDidChange('filteringComplete');
      }
    }
    // reset filter change marker
    this.set('controller.filterChangeHappened', false);
  }.observes('tableFilteringComplete'),

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
  },

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
    this.combineSelectedFilter();
    //10 is an index of selected column
    var controllerName = this.get('controller.name');
    App.db.setSelectedHosts(controllerName, this.get('selectedHosts'));

    this.addObserver('selectAllHosts', this, this.toggleAllHosts);
  },
  /**
   * combine selected hosts on page with selected hosts which are filtered out but added to cluster
   */
  combineSelectedFilter: function () {
    var controllerName = this.get('controller.name');
    var previouslySelectedHosts = App.db.getSelectedHosts(controllerName);
    var selectedHosts = [];
    var hostsOnPage = this.get('pageContent').mapProperty('hostName');
    selectedHosts = this.get('pageContent').filterProperty('selected').mapProperty('hostName');

    previouslySelectedHosts.forEach(function (hostName) {
      if (!hostsOnPage.contains(hostName)) {
        selectedHosts.push(hostName);
      }
    }, this);
    this.set('selectedHosts', selectedHosts);
  },

  /**
   * filter selected hosts
   */
  filterSelected: function() {
    //10 is an index of selected column
    this.updateFilter(10, this.get('selectedHosts'), 'multiple');
  },

  /**
   * Show spinner when filter/sorting request is in processing
   * @method overlayObserver
   */
  overlayObserver: function() {
    var $tbody = this.$('tbody'),
      $overlay = this.$('.table-overlay'),
      $spinner = $($overlay).find('.spinner');
    if (!this.get('filteringComplete')) {
      if (!$tbody) return;
      var tbodyPos =  $tbody.position();
      if (!tbodyPos) return;
      $spinner.css('display', 'block');
      $overlay.css({
        top: tbodyPos.top + 1,
        left: tbodyPos.left + 1,
        width: $tbody.width() - 1,
        height: $tbody.height() - 1
      });
    }
  },

  /**
   * Clear selectedFilter
   * Set <code>selected</code> to false for each host
   */
  clearSelection: function() {
    this.get('pageContent').setEach('selected', false);
    this.set('selectAllHosts', false);
    App.db.setSelectedHosts(this.get('controller.name'), []);
    this.get('selectedHosts').clear();
    this.filterSelected();
  },



  sortView: sort.serverWrapperView,
  nameSort: sort.fieldView.extend({
    column: 1,
    name:'hostName',
    displayName: Em.I18n.t('common.name')
  }),
  ipSort: sort.fieldView.extend({
    column: 2,
    name:'ip',
    displayName: Em.I18n.t('common.ipAddress'),
    type: 'ip'
  }),
  rackSort: sort.fieldView.extend({
    column: 12,
    name:'rack',
    displayName: Em.I18n.t('common.rack'),
    type: 'rack'
  }),
  cpuSort: sort.fieldView.extend({
    column: 3,
    name:'cpu',
    displayName: Em.I18n.t('common.cores'),
    type: 'number'
  }),
  memorySort: sort.fieldView.extend({
    column: 4,
    name:'memoryFormatted',
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

    willDestroyElement: function() {
      this.$("[rel='HealthTooltip'], [rel='UsageTooltip'], [rel='ComponentsTooltip']").remove();
    },

    displayComponents: function () {
      if (this.get('hasNoComponents')) {
        return;
      }
      var header = Em.I18n.t('common.components'),
        hostName = this.get('content.hostName'),
        items = this.get('content.hostComponents').getEach('displayName');
      App.showHostsTableListPopup(header, hostName, items);
    },

    displayVersions: function () {
      if (this.get('hasSingleVersion')) {
        return;
      }
      var header = Em.I18n.t('common.versions'),
        hostName = this.get('content.hostName'),
        items = this.get('content.stackVersions').filterProperty('isVisible').map(function (stackVersion) {
          return {
            name: stackVersion.get('displayName'),
            status: App.format.role(stackVersion.get('status'), false)
          };
        });
      App.showHostsTableListPopup(header, hostName, items);
    },

    /**
     * Tooltip message for "Restart Required" icon
     * @returns {String}
     */
    restartRequiredComponentsMessage: function() {
      var restartRequiredComponents = this.get('content.componentsWithStaleConfigs');
      var count = this.get('content.componentsWithStaleConfigsCount');
      if (count <= 5) {
        var word = (count == 1) ? Em.I18n.t('common.component') : Em.I18n.t('common.components');
        return Em.I18n.t('hosts.table.restartComponents.withNames').format(restartRequiredComponents.getEach('displayName').join(', ')) + ' ' + word.toLowerCase();
      }
      return Em.I18n.t('hosts.table.restartComponents.withoutNames').format(count);
    }.property('content.componentsWithStaleConfigs'),

    /**
     * Tooltip message for "Maintenance" icon
     * @returns {String}
     */
    componentsInPassiveStateMessage: function() {
      var componentsInPassiveState = this.get('content.componentsInPassiveState');
      var count = this.get('content.componentsInPassiveStateCount');
      if (count <= 5) {
        return Em.I18n.t('hosts.table.componentsInPassiveState.withNames').format(componentsInPassiveState.getEach('displayName').join(', '));
      }
      return Em.I18n.t('hosts.table.componentsInPassiveState.withoutNames').format(count);
    }.property('content.componentsInPassiveState'),

    /**
     * true if host has only one repoversion
     * in this case expander in version column is hidden
     * @returns {Boolean}
     */
    hasSingleVersion: function() {
      return this.get('content.stackVersions').filterProperty('isVisible', true).length < 2;
    }.property('content.stackVersions.length'),

    /**
     * true if host has no components
     * @returns {Boolean}
     */
    hasNoComponents: Em.computed.empty('content.hostComponents'),

    /**

    /**
     * this version is always shown others hidden unless expander is open
     * host may have no stack versions
     * @returns {String}
     */
    currentVersion: function() {
      var currentRepoVersion = this.get('content.stackVersions').findProperty('isCurrent') || this.get('content.stackVersions').objectAt(0);
      return currentRepoVersion ? currentRepoVersion.get('displayName') : "";
    }.property('content.stackVersions'),

    /**
     * CSS value for disk usage bar
     * @returns {String}
     */
    usageStyle:function () {
      return "width:" + this.get('content.diskUsage') + "%";
    }.property('content.diskUsage')

  }),

  /**
   * Update <code>hostsCount</code> in every category
   */
  updateHostsCount: function() {
    var hostsCountMap = this.get('controller.hostsCountMap');

    this.get('categories').forEach(function(category) {
      var hostsCount = (category.get('healthStatus').trim() === "") ? hostsCountMap['TOTAL'] : hostsCountMap[category.get('healthStatus')];

      if (!Em.isNone(hostsCount)) {
        category.set('hostsCount', hostsCount);
        category.set('hasHosts', (hostsCount > 0));
      }
    }, this);
  }.observes('controller.hostsCountMap'),

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
    itemClass: Em.computed.ifThenElse('isActive', 'active', ''),

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
    var category_mocks = require('data/host/categories');

    return category_mocks.map(function(category_mock) {
      return self.categoryObject.create(category_mock);
    });
  }.property(),

  /**
   * Category for <code>selected</code> property of each App.Host
   */
  selectedCategory: Em.computed.findBy('categories', 'selected', true),

  statusFilter: Em.View.extend({
    column: 0,
    categories: [],
    value: null,
    class: "",
    comboBoxLabel: function(){
      var selected = this.get('categories').findProperty('isActive');
      if (!this.get('value') || !selected) {
        return "%@ (%@)".fmt(Em.I18n.t('common.all'), this.get('parentView.totalCount'));
      } else {
        return "%@ (%@)".fmt(selected.get('value'), selected.get('hostsCount'))
      }
    }.property('value', 'parentView.totalCount'),
    /**
     * switch active category label
     */
    onCategoryChange: function () {
      this.get('categories').setEach('isActive', false);
      var selected = this.get('categories').findProperty('healthStatus', this.get('value'));
      selected.set('isActive', true);
      this.set('class', selected.get('class') + ' ' + selected.get('healthClass'));
    }.observes('value'),

    showClearFilter: function () {
      var mockEvent = {
        context: this.get('categories').findProperty('healthStatus', this.get('value'))
      };
      this.selectCategory(mockEvent);
    },
    /**
     * Trigger on Category click
     * @param {Object} event
     */
    selectCategory: function(event){
      var category = event.context;

      this.set('value', category.get('healthStatus'));
      this.get('parentView').resetFilterByColumns([0, 7, 8, 9]);
      if (category.get('isHealthStatus')) {
        var status = category.get('healthStatus');
        if (!status) {
          // only "All" option has no specific status, just refresh
          this.get('parentView').refresh();
        } else {
          this.get('parentView').updateFilter(0, status, 'string');
        }
      } else {
        this.get('parentView').updateFilter(category.get('column'), category.get('filterValue'), category.get('type'));
      }
    },

    /**
     * set value
     * @param {string} value
     */
    setValue: function (value) {
      this.set('value', value);
    },

    clearFilter: function() {
      this.get('categories').setEach('isActive', false);
      this.set('value', '');
      this.set('class', '');
      this.showClearFilter();
    }
  }),

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: Em.computed.alias('controller.colPropAssoc'),

  /**
   * Run <code>clearFilter</code> in the each child filterView
   */
  clearFilters: function() {
    // clean filters stored in-memory and local storage
    this.set('filterConditions', []);
    this.clearFilterConditionsFromLocalStorage();
    // force refresh
    this.refresh();
  }
});
