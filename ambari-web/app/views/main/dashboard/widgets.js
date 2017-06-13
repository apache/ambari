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

const WidgetObject = Em.Object.extend({
  id: '',
  threshold: '',
  viewClass: null,
  sourceName: '',
  title: '',
  checked: false,
  isVisible: true
});

App.MainDashboardWidgetsView = Em.View.extend(App.Persist, App.LocalStorage, App.TimeRangeMixin, {
  name: 'mainDashboardWidgetsView',
  templateName: require('templates/main/dashboard/widgets'),

  widgetsDefinition: require('data/dashboard_widgets'),

  widgetsDefinitionMap: function () {
    return this.get('widgetsDefinition').toMapByProperty('id');
  }.property('widgetsDefinition.[]'),

  /**
   * List of services
   * @type {Ember.Enumerable}
   */
  content: [],

  /**
   * Key-name to store data in Local Storage and Persist
   * @type {string}
   */
  persistKey: Em.computed.format('user-pref-{0}-dashboard', 'App.router.loginName'),

  /**
   * @type {boolean}
   */
  isDataLoaded: false,

  /**
   * Define if some widget is currently moving
   * @type {boolean}
   */
  isMoving: false,

  /**
   * @type {WidgetObject[]}
   */
  allWidgets: [],

  /**
   * List of visible widgets
   *
   * @type {WidgetObject[]}
   */
  visibleWidgets: Em.computed.filterBy('allWidgets', 'isVisible', true),

  /**
   * List of hidden widgets
   *
   * @type {WidgetObject[]}
   */
  hiddenWidgets: Em.computed.filterBy('allWidgets', 'isVisible', false),

  timeRangeClassName: 'pull-left',

  /**
   * Example:
   * {
   *   visible: [1, 2, 4],
   *   hidden: [3, 5],
   *   threshold: {
   *     1: [80, 90],
   *     2: [],
   *     3: [1, 2]
   *   }
   * }
   * @type {Object|null}
   */
  userPreferences: null,

  didInsertElement: function () {
    this._super();
    this.loadWidgetsSettings().complete(() => {
      this.checkServicesChange();
      this.renderWidgets();
      this.set('isDataLoaded', true);
      App.loadTimer.finish('Dashboard Metrics Page');
      Em.run.next(this, 'makeSortable');
    });
  },

  /**
   * Set visibility-status for widgets
   */
  loadWidgetsSettings: function () {
    return this.getUserPref(this.get('persistKey'));
  },

  /**
   * make POST call to save settings
   * format settings if they are not provided
   *
   * @param {object} [settings]
   */
  saveWidgetsSettings: function (settings) {
    let userPreferences = this.get('userPreferences');
    let newSettings = {
      visible: [],
      hidden: [],
      threshold: {}
    };
    if (arguments.length === 1) {
      newSettings = settings;
    }
    else {
      newSettings.threshold = userPreferences.threshold;
      this.get('allWidgets').forEach(widget => {
        let key = widget.get('isVisible') ? 'visible' : 'hidden';
        newSettings[key].push(widget.get('id'));
      });
    }
    this.set('userPreferences', newSettings);
    this.setDBProperty(this.get('persistKey'), newSettings);
    this.postUserPref(this.get('persistKey'), newSettings);
  },

  getUserPrefSuccessCallback: function (response) {
    if (response) {
      this.set('userPreferences', response);
    } else {
      this.getUserPrefErrorCallback();
    }
  },

  getUserPrefErrorCallback: function () {
    var userPreferences = this.generateDefaultUserPreferences();
    this.saveWidgetsSettings(userPreferences);
  },

  resolveConfigDependencies: function(widgetsDefinition) {
    var clusterEnv = App.router.get('clusterController.clusterEnv.properties') || {};
    if (clusterEnv.hide_yarn_memory_widget === 'true') {
      widgetsDefinition.findProperty('id', 20).isHiddenByDefault = true;
    }
    return widgetsDefinition;
  },

  generateDefaultUserPreferences: function() {
    var widgetsDefinition = this.get('widgetsDefinition');
    var preferences = {
      visible: [],
      hidden: [],
      threshold: {}
    };

    this.resolveConfigDependencies(widgetsDefinition);
    widgetsDefinition.forEach(function(widget) {
      if (App.Service.find(widget.sourceName).get('isLoaded') || widget.sourceName === 'HOST_METRICS') {
        let state = widget.isHiddenByDefault ? 'hidden' : 'visible';
        preferences[state].push(widget.id);
      }
      preferences.threshold[widget.id] = widget.threshold;
    });

    return preferences;
  },

  /**
   * Don't show widget on the Dashboard
   *
   * @param {number} id
   */
  hideWidget(id) {
    this.get('allWidgets').findProperty('id', id).set('isVisible', false);
    this.saveWidgetsSettings();
  },

  /**
   *
   * @param {number} id
   * @param {boolean} isVisible
   * @returns {WidgetObject}
   * @private
   */
  _createWidgetObj(id, isVisible) {
    var widget = this.get('widgetsDefinitionMap')[id];
    return WidgetObject.create({
      id,
      threshold: this.get('userPreferences.threshold')[id],
      viewClass: App[widget.viewName],
      sourceName: widget.sourceName,
      title: widget.title,
      isVisible
    });
  },

  /**
   * set widgets to view in order to render
   */
  renderWidgets: function () {
    var userPreferences = this.get('userPreferences');
    var newVisibleWidgets = userPreferences.visible.map(id => this._createWidgetObj(id, true));
    var newHiddenWidgets = userPreferences.hidden.map(id => this._createWidgetObj(id, false));
    this.set('allWidgets', newVisibleWidgets.concat(newHiddenWidgets));
  },

  /**
   * check if stack has upgraded from HDP 1.0 to 2.0 OR add/delete services.
   * Update the value on server if true.
   */
  checkServicesChange: function () {
    var userPreferences = this.get('userPreferences');
    var defaultPreferences = this.generateDefaultUserPreferences();
    var newValue = {
      visible: userPreferences.visible.slice(0),
      hidden: userPreferences.hidden.slice(0),
      threshold: userPreferences.threshold
    };
    var isChanged = false;

    ['visible', 'hidden'].forEach(state => {
      defaultPreferences[state].forEach(id => {
        if (!userPreferences.visible.contains(id) && !userPreferences.hidden.contains(id)) {
          isChanged = true;
          newValue[state].push(id);
        }
      });
    });
    if (isChanged) {
      this.saveWidgetsSettings(newValue);
    }
  },

  /**
   * Reset widgets visibility-status
   */
  resetAllWidgets: function () {
    App.showConfirmationPopup(() => {
      this.saveWidgetsSettings(this.generateDefaultUserPreferences());
      this.setProperties({
        currentTimeRangeIndex: 0,
        customStartTime: null,
        customEndTime: null
      });
      this.renderWidgets();
    });
  },

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function () {
    var self = this;
    return $("#sortable").sortable({
      items: "> div",
      cursor: "move",
      tolerance: "pointer",
      scroll: false,
      update: function () {
        var widgetsArray = $('div[viewid]');

        var userPreferences = self.get('userPreferences') || self.getDBProperty(self.get('persistKey'));
        var newValue = {
          visible: [],
          hidden: userPreferences.hidden,
          threshold: userPreferences.threshold
        };
        newValue.visible = userPreferences.visible.map((item, index) => {
          var viewID = widgetsArray.get(index).getAttribute('viewid');
          return Number(viewID.split('-')[1]);
        });
        self.saveWidgetsSettings(newValue);
      },
      activate: function () {
        self.set('isMoving', true);
      },
      deactivate: function () {
        self.set('isMoving', false);
      }
    }).disableSelection();
  },

  /**
   * Submenu view for New Dashboard style
   * @type {Ember.View}
   * @class
   */
  plusButtonFilterView: Ember.View.extend({
    tagName: 'ul',
    classNames: ['dropdown-menu'],
    templateName: require('templates/main/dashboard/plus_button_filter'),
    hiddenWidgetsBinding: 'parentView.hiddenWidgets',
    valueBinding: '',
    widgetCheckbox: App.CheckboxView.extend({
      didInsertElement: function () {
        $('.checkbox').click(function (event) {
          event.stopPropagation();
        });
      }
    }),
    applyFilter: function () {
      var parent = this.get('parentView'),
        hiddenWidgets = this.get('hiddenWidgets');
      hiddenWidgets.filterProperty('checked').setEach('isVisible', true);
      parent.saveWidgetsSettings();
    }
  }),

  showAlertsPopup: Em.K

});
