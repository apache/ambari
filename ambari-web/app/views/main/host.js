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

  didInsertElement:function () {
    this.filter();
    if (this.get('controller.comeWithAlertsFilter')) {
      this.set('controller.comeWithAlertsFilter', false);
      this.set('controller.filteredByAlerts', true);
    } else {
      this.set('controller.filteredByAlerts', false);
    }
    this.tooltipsUpdate();
  },

  tooltipsUpdate: function () {
    Ember.run.next(function(){
      $("[rel='HealthTooltip']").tooltip();
      $("[rel='UsageTooltip']").tooltip();
    });
  }.observes('controller.content.length'),

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
    shortLabels: function() {
      var labels = this.get('content.hostComponents').getEach('displayName');
      var shortLabels = '';
      var c = 0;
      labels.forEach(function(label) {
        if (label) {
          if (c < 2) {
            shortLabels += label.replace(/[^A-Z]/g, '') + ', ';
            c++;
          }
        }
      });
      shortLabels = shortLabels.substr(0, shortLabels.length - 2);
      if (labels.length > 2) {
        shortLabels += ' ' + Em.I18n.t('and') + ' ' + (labels.length - 2) + ' ' + Em.I18n.t('more');
      }
      return shortLabels;
    }.property('labels'),

    labels: function(){
      return this.get('content.hostComponents').getEach('displayName').join('\n');
    }.property('content.hostComponents.@each'),

    usageStyle:function () {
      return "width:" + this.get('content.diskUsage') + "%";
      //return "width:" + (25+Math.random()*50) + "%"; // Just for tests purposes
    }.property('content.diskUsage')

  }),

  /**
   * Category view for all hosts
   */
  categoryObject: Em.Object.extend({

    hostsCount: function () {
      var statusString = this.get('healthStatusValue');
      if (statusString == "") {
        return this.get('view.content').get('length');
      } else {
        return this.get('view.content').filterProperty('healthClass', statusString ).get('length');
      }

    }.property('view.content.@each.healthClass'),

    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount')
  }),

  getCategory: function(field, value){
    return this.get('categories').find(function(item) {
      return item.get(field) == value;
    });
  },

  categories: function () {
    var self = this;
    self.categoryObject.reopen({
      view: self,
      isActive: function() {
        return this.get('view.category') == this;
      }.property('view.category'),
      itemClass: function() {
        return this.get('isActive') ? 'active' : '';
      }.property('isActive')
    });

    var categories = [
      self.categoryObject.create({value: Em.I18n.t('common.all'), healthStatusValue: ''}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.green'), healthStatusValue: 'health-status-LIVE'}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.red'), healthStatusValue: 'health-status-DEAD-RED'}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.orange'), healthStatusValue: 'health-status-DEAD-ORANGE'}),
      self.categoryObject.create({value: Em.I18n.t('hosts.host.healthStatusCategory.yellow'), healthStatusValue: 'health-status-DEAD-YELLOW', last: true })
    ];

    this.set('category', categories.get('firstObject'));

    return categories;
  }.property(),

  category: false,

  selectCategory: function(event, context){
    this.set('category', event.context);
    this.updateFilter(0, event.context.get('healthStatusValue'), 'string');
  },

  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  nameFilterView: filters.createTextView({
    onChangeValue: function(){
      this.get('parentView').updateFilter(1, this.get('value'), 'string');
    }
  }),

  /**
   * Filter view for ip column
   * Based on <code>filters</code> library
   */
  ipFilterView: filters.createTextView({
    onChangeValue: function(){
      this.get('parentView').updateFilter(2, this.get('value'), 'string');
    }
  }),

  /**
   * Filter view for Cpu column
   * Based on <code>filters</code> library
   */
  cpuFilterView: filters.createTextView({
    fieldType: 'input-mini',
    fieldId: 'cpu_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(3, this.get('value'), 'number');
    }
  }),

  /**
   * Filter view for LoadAverage column
   * Based on <code>filters</code> library
   */
  loadAvgFilterView: filters.createTextView({
    fieldType: 'input-mini',
    fieldId: 'load_avg_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(5, this.get('value'), 'number');
    }
  }),

  /**
   * Filter view for Ram column
   * Based on <code>filters</code> library
   */
  ramFilterView: filters.createTextView({
    fieldType: 'input-mini',
    fieldId: 'ram_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(4, this.get('value'), 'ambari-bandwidth');
    }
  }),

  /**
   * Filter view for HostComponents column
   * Based on <code>filters</code> library
   */
  componentsFilterView: filters.createComponentView({
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
        this.set('value', chosenComponents.toString());
      },

      didInsertElement:function () {
        if (this.get('controller.comeWithFilter')) {
          this.applyFilter();
          this.set('controller.comeWithFilter', false);
        } else {
          this.clearFilter();
        }
      }

    }),
    onChangeValue: function(){
      this.get('parentView').updateFilter(6, this.get('value'), 'multiple');
    }
  }),

  /**
   * Filter hosts by hosts with at least one alert
   */
  filterByAlerts:function() {
    if (this.get('controller.filteredByAlerts')) {
      this.updateFilter(7, '>0', 'number')
    } else {
      this.updateFilter(7, '', 'number')
    }
  }.observes('controller.filteredByAlerts'),

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
