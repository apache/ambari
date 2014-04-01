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

App.WizardStep3View = App.TableView.extend({

  templateName: require('templates/wizard/step3'),

  content:function () {
    return this.get('controller.hosts');
  }.property('controller.hosts.length'),

  message:'',
  linkText: '',
  status: '',

  selectedCategory: function() {
    return this.get('categories').findProperty('isActive');
  }.property('categories.@each.isActive'),

  registeredHostsMessage: '',

  displayLength: "25",

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  pageChecked: false,

  /**
   * select checkboxes of hosts on page
   */
  onPageChecked: function () {
    if (this.get('selectionInProgress')) return;
    this.get('pageContent').setEach('isChecked', this.get('pageChecked'));
  }.observes('pageChecked'),

  /**
   * select checkboxes of all hosts
   */
  selectAll: function () {
    this.get('content').setEach('isChecked', true);
  },

  /**
   * reset checkbox of all hosts
   */
  unSelectAll: function() {
    this.get('content').setEach('isChecked', false);
  },

  watchSelectionOnce: function () {
    Em.run.once(this, 'watchSelection');
  }.observes('content.@each.isChecked', 'pageContent'),

  /**
   * watch selection and calculate such flags as:
   * - noHostsSelected
   * - selectedHostsCount
   * - pageChecked
   */
  watchSelection: function() {
    this.set('selectionInProgress', true);
    this.set('pageChecked', !!this.get('pageContent.length') && this.get('pageContent').everyProperty('isChecked', true));
    this.set('selectionInProgress', false);
    var noHostsSelected = true;
    var selectedHostsCount = 0;
    this.get('content').forEach(function(host){
      selectedHostsCount += ~~host.get('isChecked');
      noHostsSelected = (noHostsSelected) ? !host.get('isChecked') : noHostsSelected;
    });
    this.set('noHostsSelected', noHostsSelected);
    this.set('selectedHostsCount', selectedHostsCount);
  },

  setRegisteredHosts: function(){
    this.set('registeredHostsMessage',Em.I18n.t('installer.step3.warning.registeredHosts').format(this.get('controller.registeredHosts').length));
  }.observes('controller.registeredHosts'),

  categoryObject: Em.Object.extend({
    hostsCount: 0,
    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount'),
    isActive: false,
    itemClass: function () {
      return this.get('isActive') ? 'active' : '';
    }.property('isActive')
  }),

  categories: function () {
    return [
      this.categoryObject.create({value: Em.I18n.t('common.all'), hostsBootStatus: 'ALL', isActive: true}),
      this.categoryObject.create({value: Em.I18n.t('installer.step3.hosts.status.installing'), hostsBootStatus: 'RUNNING'}),
      this.categoryObject.create({value: Em.I18n.t('installer.step3.hosts.status.registering'), hostsBootStatus: 'REGISTERING'}),
      this.categoryObject.create({value: Em.I18n.t('common.success'), hostsBootStatus: 'REGISTERED' }),
      this.categoryObject.create({value: Em.I18n.t('common.fail'), hostsBootStatus: 'FAILED', last: true })
    ];
  }.property(),

  hostBootStatusObserver: function(){
    Ember.run.once(this, 'countCategoryHosts');
    Ember.run.once(this, 'filter');
    Ember.run.once(this, 'monitorStatuses');
  }.observes('content.@each.bootStatus'),

  countCategoryHosts: function () {
    var counters = {
      "RUNNING": 0,
      "REGISTERING": 0,
      "REGISTERED": 0,
      "FAILED": 0
    };
    this.get('content').forEach(function (host) {
      if (counters[host.get('bootStatus')] !== undefined) {
        counters[host.get('bootStatus')]++;
      }
    }, this);
    counters["ALL"] = this.get('content.length');
    this.get('categories').forEach(function(category) {
      category.set('hostsCount', counters[category.get('hostsBootStatus')]);
    }, this);
  },


  /**
   * filter hosts by category
   */
  filter: function () {
    var self = this;
    Em.run.next(function () {
      var result = [];
      var selectedCategory = self.get('selectedCategory');
      if (!selectedCategory || selectedCategory.get('hostsBootStatus') === 'ALL') {
        result = self.get('content');
      } else {
        result = self.get('content').filterProperty('bootStatus', self.get('selectedCategory.hostsBootStatus'));
      }
      self.set('filteredContent', result);
    });
  }.observes('selectedCategory'),
  /**
   * Trigger on Category click
   * @param {Object} event
   */
  selectCategory: function (event) {
    var categoryStatus = event.context.get('hostsBootStatus');
    var self = this;
    this.get('categories').forEach(function (category) {
      category.set('isActive', (category.get('hostsBootStatus') === categoryStatus));
    });
    this.watchSelection();
  },

  /**
   * Select "All" hosts category
   * run registration of failed hosts again
   */
  retrySelectedHosts: function () {
    var eventObject = {context: Em.Object.create({hostsBootStatus: 'ALL'})};
    this.selectCategory(eventObject);
    this.get('controller').retrySelectedHosts();
  },

  monitorStatuses: function() {
    var hosts = this.get('controller.bootHosts');
    var failedHosts = hosts.filterProperty('bootStatus', 'FAILED').length;

    if (hosts.length === 0) {
      this.set('status', 'alert-warn');
      this.set('linkText', '');
      this.set('message', Em.I18n.t('installer.step3.warnings.missingHosts'));
    } else if (!this.get('controller.isWarningsLoaded')) {
      this.set('status', 'alert-info');
      this.set('linkText', '');
      this.set('message', Em.I18n.t('installer.step3.warning.loading'));
    } else if (this.get('controller.isHostHaveWarnings') || this.get('controller.repoCategoryWarnings.length') || this.get('controller.diskCategoryWarnings.length')) {
      this.set('status', 'alert-warn');
      this.set('linkText', Em.I18n.t('installer.step3.warnings.linkText'));
      this.set('message', Em.I18n.t('installer.step3.warnings.fails').format(hosts.length - failedHosts));
    } else {
      this.set('status', 'alert-success');
      this.set('linkText', Em.I18n.t('installer.step3.noWarnings.linkText'));
      if (failedHosts == 0) {
        // all are ok
        this.set('message', Em.I18n.t('installer.step3.warnings.noWarnings').format(hosts.length));
      } else if (failedHosts == hosts.length) {
        // all failed
        this.set('status', 'alert-warn');
        this.set('linkText', '');
        this.set('message', Em.I18n.t('installer.step3.warnings.allFailed').format(failedHosts));
      } else {
        // some failed
        this.set('message', Em.I18n.t('installer.step3.warnings.someWarnings').format((hosts.length - failedHosts), failedHosts));
      }
    }
  }.observes('controller.isWarningsLoaded', 'controller.isHostHaveWarnings', 'controller.repoCategoryWarnings', 'controller.diskCategoryWarnings')
});

//todo: move it inside WizardStep3View
App.WizardHostView = Em.View.extend({

  tagName: 'tr',
  classNameBindings: ['hostInfo.bootStatus'],
  hostInfo: null,

  remove: function () {
    this.get('controller').removeHost(this.get('hostInfo'));
  },

  retry: function() {
    this.get('controller').retryHost(this.get('hostInfo'));
  },

  isRemovable: function () {
    return true;
  }.property(),

  isRetryable: function() {
    // return ['FAILED'].contains(this.get('hostInfo.bootStatus'));
    return false;
  }.property('hostInfo.bootStatus')

});


