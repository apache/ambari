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

  colspan: function () {
    return 11 + +App.get('supports.stackUpgrade');
  }.property("App.supports.stackUpgrade"),

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
  showSelectedFilter: function () {
    return this.get('selectedHosts.length') > 0;
  }.property('selectedHosts'),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('hosts.filters.filteredHostsInfo').format(this.get('filteredCount'), this.get('totalCount'));
  }.property('filteredCount', 'totalCount'),

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
  paginationInfo: function () {
    return this.t('tableView.filters.paginationInfo').format(this.get('startIndex'), this.get('endIndex'), this.get('filteredCount'));
  }.property('startIndex', 'endIndex', 'filteredCount'),

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

  /**
   * Returs all hostNames if amount is less than {minShown} or
   * first elements of array (number of elements - {minShown}) converted to string
   * @param {Array} hostNames - array of all listed hostNames
   * @param {String} divider - string to separate hostNames
   * @param {Number} minShown - min amount of hostName to be shown
   * @returns {String} hostNames
   * @method showHostNames
   */
  showHostNames: function(hostNames, divider, minShown) {
    if (hostNames.length > minShown) {
      return hostNames.slice(0, minShown).join(divider) + divider + Em.I18n.t("installer.step8.other").format(hostNames.length - minShown);
    } else {
      return hostNames.join(divider);
    }
  },

  /**
   * Confirmation Popup for bulk Operations
   */
  bulkOperationConfirm: function(operationData, selection) {
    var hostsNames = [],
      queryParams = [];
    switch(selection) {
      case 's':
        hostsNames = this.get('selectedHosts');
        if(hostsNames.length > 0){
          queryParams.push({
            key: 'Hosts/host_name',
            value: hostsNames,
            type: 'MULTIPLE'
          });
        }
        break;
      case 'f':
        queryParams = this.get('controller').getQueryParameters(true).filter(function (obj) {
          return !(obj.key == 'page_size' || obj.key == 'from');
        });
        break;
    }

    if (operationData.action === 'SET_RACK_INFO') {
      this.getHostsForBulkOperations(queryParams, operationData, null);
      return;
    }

    var loadingPopup = App.ModalPopup.show({
      header: Em.I18n.t('jobs.loadingTasks'),
      primary: false,
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<div class="spinner"></div>')
      })
    });

    this.getHostsForBulkOperations(queryParams, operationData, loadingPopup);
  },

  getHostsForBulkOperations: function (queryParams, operationData, loadingPopup) {
    var params = App.router.get('updateController').computeParameters(queryParams);

    App.ajax.send({
      name: 'hosts.bulk.operations',
      sender: this,
      data: {
        parameters: params,
        operationData: operationData,
        loadingPopup: loadingPopup
      },
      success: 'getHostsForBulkOperationSuccessCallback'
    });
  },

  convertHostsObjects: function(hosts) {
    var newHostArr = [];
    hosts.forEach(function (host) {
      newHostArr.push({
        index:host.index,
        id:host.id,
        clusterId: host.cluster_id,
        passiveState: host.passive_state,
        hostName: host.host_name,
        hostComponents: host.host_components
      })
    });
    return newHostArr;
  },

  getHostsForBulkOperationSuccessCallback: function(json, opt, param) {
    var self = this,
    operationData = param.operationData,
    hosts = this.convertHostsObjects(App.hostsMapper.map(json, true));
    // no hosts - no actions
    if (!hosts.length) {
      console.log('No bulk operation if no hosts selected.');
      return;
    }
    var hostNames = hosts.mapProperty('hostName');
    var hostsToSkip = [];
    if (operationData.action == "DECOMMISSION") {
      var hostComponentStatusMap = {}; // "DATANODE_c6401.ambari.apache.org" => "STARTED"
      var hostComponentIdMap = {}; // "DATANODE_c6401.ambari.apache.org" => "DATANODE"
      if (json.items) {
        json.items.forEach(function(host) {
          if (host.host_components) {
            host.host_components.forEach(function(component) {
              hostComponentStatusMap[component.id] = component.HostRoles.state;
              hostComponentIdMap[component.id] = component.HostRoles.component_name;
            });
          }
        });
      }
      hostsToSkip = hosts.filter(function(host) {
        var invalidStateComponents = host.hostComponents.filter(function(component) {
          return hostComponentIdMap[component] == operationData.realComponentName && hostComponentStatusMap[component] == 'INSTALLED';
        });
        return invalidStateComponents.length > 0;
      });
    }
    var hostNamesSkipped = hostsToSkip.mapProperty('hostName');
    if (operationData.action === 'PASSIVE_STATE') {
      hostNamesSkipped = [];
      var outOfSyncHosts = App.StackVersion.find().findProperty('isCurrent').get('outOfSyncHosts');
      for (var i = 0; i < outOfSyncHosts.length; i++) {
        if (hostNames.contains(outOfSyncHosts[i])) {
          hostNamesSkipped.push(outOfSyncHosts[i]);
        }
      }
    }
    var message;
    if (operationData.componentNameFormatted) {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hostComponents').format(operationData.message, operationData.componentNameFormatted, hostNames.length);
    }
    else {
      message = Em.I18n.t('hosts.bulkOperation.confirmation.hosts').format(operationData.message, hostNames.length);
    }

    if (param.loadingPopup) {
      param.loadingPopup.hide();
    }

    if (operationData.action === 'SET_RACK_INFO') {
      self.get('controller').bulkOperation(operationData, hosts);
      return;
    }

    App.ModalPopup.show({
      header: Em.I18n.t('hosts.bulkOperation.confirmation.header'),
      hostNames: hostNames.join("\n"),
      visibleHosts: self.showHostNames(hostNames, "\n", 3),
      hostNamesSkippedVisible: self.showHostNames(hostNamesSkipped, "\n", 3),
      hostNamesSkipped: function() {
        if (hostNamesSkipped.length) {
          return hostNamesSkipped.join("\n");
        }
        return false;
      }.property(),
      expanded: false,
      didInsertElement: function() {
        this.set('expanded', hostNames.length <= 3);
      },
      onPrimary: function() {
        self.get('controller').bulkOperation(operationData, hosts);
        this._super();
      },
      bodyClass: Em.View.extend({
        templateName: require('templates/main/host/bulk_operation_confirm_popup'),
        message: message,
        warningInfo: function() {
          switch (operationData.action) {
            case "DECOMMISSION":
              return Em.I18n.t('hosts.bulkOperation.warningInfo.body');
            case "PASSIVE_STATE":
              return operationData.state === 'OFF' ? Em.I18n.t('hosts.passiveMode.popup.version.mismatch.multiple')
              .format(App.StackVersion.find().findProperty('isCurrent').get('repositoryVersion.repositoryVersion')) : "";
            default:
              return ""
          }
        }.property(),
        textareaVisible: false,
        textTrigger: function() {
          this.set('textareaVisible', !this.get('textareaVisible'));
        },
        showAll: function() {
          this.set('parentView.visibleHosts', this.get('parentView.hostNames'));
          this.set('parentView.hostNamesSkippedVisible', this.get('parentView.hostNamesSkipped'));
          this.set('parentView.expanded', true);
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
    hasNoComponents: function() {
      return !this.get('content.hostComponents.length');
    }.property('content.hostComponents.length'),

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
    itemClass: function() {
      return this.get('isActive') ? 'active' : '';
    }.property('isActive'),

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
  selectedCategory: function() {
    return this.get('categories').findProperty('selected', true);
  }.property('categories.@each.selected'),

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
   * Filter view for rack column
   * Based on <code>filters</code> library
   */
  rackFilterView: filters.createTextView({
    column: 12,
    fieldType: 'filter-input-width rack-input',
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
   * Filter view for Ram column
   * Based on <code>filters</code> library
   */
  ramFilterView: filters.createTextView({
    fieldType: 'filter-input-width',
    fieldId: 'ram_filter',
    column: 4,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
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
       * Master components
       * @returns {Array}
       */
      masterComponents: function () {
        var components = App.MasterComponent.find().rejectProperty('totalCount', 0);
        components.setEach('checkedForHostFilter', false);
        return components;
      }.property('App.router.clusterController.isComponentsStateLoaded'),

      /**
       * Slave components
       * @returns {Array}
       */
      slaveComponents: function () {
        var components = App.SlaveComponent.find().rejectProperty('totalCount', 0);
        components.setEach('checkedForHostFilter', false);
        return components;
      }.property('App.router.clusterController.isComponentsStateLoaded'),

      /**
       * Client components
       * @returns {Array}
       */
      clientComponents: function () {
        var components = App.ClientComponent.find().rejectProperty('totalCount', 0);
        components.setEach('checkedForHostFilter', false);
        return components;
      }.property('App.router.clusterController.isComponentsStateLoaded'),

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
          self.set('value', chosenComponents);
        });
      },

      /**
       * Verify that checked checkboxes are equal to value stored in hidden field (components ids list)
       */
      checkComponents: function() {
        var components = this.get('value');
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

  versionsFilterView: filters.wrapperView.extend({
    column: 11,
    filterView: filters.componentFieldView.extend({
      templateName: require('templates/main/host/version_filter'),
      selectedVersion: null,
      selectedStatus: null,
      value: [],

      versionSelectView: filters.createSelectView({
        classNames: ['notActive'],
        fieldType: 'filter-input-width',
        filterPropertyName: 'repository_versions/RepositoryVersions/display_name',
        content: function () {
          return  [
            {
              value: '',
              label: Em.I18n.t('common.all')
            }
          ].concat(this.get('controller.allHostStackVersions').filterProperty('isVisible', true).mapProperty('displayName').uniq().map(function (version) {
            return {
              value: version,
              label: version
            }
          }));
        }.property('App.router.clusterController.isLoaded', 'controller.allHostStackVersions.length'),
        onChangeValue: function () {
          this.set('parentView.selectedVersion', this.get('value'));
        }
      }),
      statusSelectView: filters.createSelectView({
        classNames: ['notActive'],
        fieldType: 'filter-input-width',
        filterPropertyName: 'HostStackVersions/state',
        content: function () {
          return [
            {
              value: '',
              label: Em.I18n.t('common.all')
            }
          ].concat(App.HostStackVersion.statusDefinition.map(function (status) {
            return {
              value: status,
              label: App.HostStackVersion.formatStatus(status)
            }
          }));
        }.property('App.router.clusterController.isLoaded'),
        onChangeValue: function () {
          this.set('parentView.selectedStatus', this.get('value'));
        }
      }),
      /**
       * Onclick handler for <code>Apply filter</code> button
       */
      applyFilter: function () {
        this._super();
        var self = this;
        var filterProperties = [];
        if (this.get('selectedVersion')) {
          filterProperties.push({
            property: 'repository_versions/RepositoryVersions/display_name',
            value: this.get('selectedVersion')
          });
        }
        if (this.get('selectedStatus')) {
          filterProperties.push({
            property: 'HostStackVersions/state',
            value: this.get('selectedStatus')
          });
        }
        self.set('value', filterProperties);
      },
      /**
       * Clear filter to initial state
       */
      clearFilter: function () {
        this.set('value', []);
        this.get('childViews').forEach(function (view) {
          if (typeof view.clearFilter === "function") view.clearFilter();
        });
      }
    }),
    setValue: function (value) {
      var versionSelectView = this.get('childViews')[0];

      //restore selected options in Select views
      versionSelectView.get('childViews').forEach(function (view) {
        var filter = value.findProperty('property', view.get('filterPropertyName'));
        if (filter && view.get('content').findProperty('value', filter.value)) {
          view.set('selected', view.get('content').findProperty('value', filter.value));
        }
      }, this);
      this._super(value);
    },
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'sub-resource');
    },
    clearFilter: function () {
      this._super();
      this.get('childViews').forEach(function (view) {
        if (typeof view.clearFilter === "function") view.clearFilter();
      });
    }
  }),

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    return this.get('controller.colPropAssoc');
  }.property('controller.colPropAssoc'),

  /**
   * Run <code>clearFilter</code> in the each child filterView
   */
  clearFilters: function() {
    // clean filters stored in-memory and local storage
    this.set('filterConditions', []);
    this.clearFilterConditionsFromLocalStorage();
    // clean UI
    this.get('_childViews').forEach(function(childView) {
      if (childView['clearFilter']) {
        childView.clearFilter();
      }
    });
    // force refresh
    this.refresh();
  }
});
