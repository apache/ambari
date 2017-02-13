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

App.DashboardWidgetView = Em.View.extend({

  /**
   * @type {string}
   * @default null
   */
  title: Em.computed.alias('widget.title'),

  templateName: null,

  sourceName: Em.computed.alias('widget.sourceName'),

  widget: null,

  /**
   * @type {object} - record from model that serve as data source
   */
  model: function () {
    var model = Em.Object.create();
    if (Em.isNone(this.get('sourceName'))) {
      return model;
    }
    return this.findModelBySource(this.get('sourceName'));
  }.property('sourceName'),

  /**
   * @type {number}
   * @default null
   */
  id: Em.computed.alias('widget.id'),

  /**
   * html id bind to view-class: widget-(1)
   * used by re-sort
   * @type {string}
   */
  viewID: Em.computed.format('widget-{0}', 'id'),

  classNames: ['span2p4'],

  attributeBindings: ['viewID'],

  /**
   * widget content pieChart/ text/ progress bar/links/ metrics. etc
   * @type {Array}
   * @default null
   */
  content: null,

  /**
   * more info details
   * @type {Array}
   */
  hiddenInfo: [],

  /**
   * @type {string}
   */
  hiddenInfoClass: "hidden-info-two-line",

  /**
   * @type {number}
   * @default 0
   */
  thresholdMin: function() {
    return Em.isNone(this.get('widget.threshold')) ? 0 : this.get('widget.threshold')[0];
  }.property('widget.threshold'),

  /**
   * @type {number}
   * @default 0
   */
  thresholdMax: function() {
    return Em.isNone(this.get('widget.threshold')) ? 0 : this.get('widget.threshold')[1];
  }.property('widget.threshold'),

  /**
   * @type {Boolean}
   * @default false
   */
  isDataLoadedBinding: 'App.router.clusterController.isServiceContentFullyLoaded',

  /**
   * @type {Em.Object}
   * @class
   */
  widgetConfig: Ember.Object.extend({
    thresholdMin: '',
    thresholdMax: '',
    hintInfo: Em.computed.i18nFormat('dashboard.widgets.hintInfo.common', 'maxValue'),
    thresholdMinError: false,
    thresholdMaxError: false,
    thresholdMinErrorMessage: "",
    thresholdMaxErrorMessage: "",
    maxValue: 0,
    validateThreshold: function(thresholdName) {
      var thresholdMin = this.get('thresholdMin'),
       thresholdMax = this.get('thresholdMax'),
       maxValue = this.get('maxValue'),
       currentThreshold = this.get(thresholdName),
       isError = false,
       errorMessage = '';

      if (currentThreshold.trim() !== "") {
        if (isNaN(currentThreshold) || currentThreshold > maxValue || currentThreshold < 0) {
          isError = true;
          errorMessage = Em.I18n.t('dashboard.widgets.error.invalid').format(maxValue);
        } else if (parseFloat(thresholdMax) <= parseFloat(thresholdMin)) {
          isError = true;
          errorMessage = Em.I18n.t('dashboard.widgets.error.smaller');
        } else {
          isError = false;
          errorMessage = '';
        }
      } else {
        isError = true;
        errorMessage = Em.I18n.t('admin.users.editError.requiredField');
      }
      this.set(thresholdName + 'ErrorMessage', errorMessage);
      this.set(thresholdName + 'Error', isError);
      this.updateSlider();
    },
    observeThreshMinValue: function () {
      this.validateThreshold('thresholdMin');
    }.observes('thresholdMin', 'maxValue'),
    observeThreshMaxValue: function () {
      this.validateThreshold('thresholdMax');
    }.observes('thresholdMax', 'maxValue'),
    updateSlider: function () {
      if (this.get('thresholdMinError') === false && this.get('thresholdMaxError') === false) {
        $("#slider-range")
          .slider('values', 0, parseFloat(this.get('thresholdMin')))
          .slider('values', 1, parseFloat(this.get('thresholdMax')));
      }
    }
  }),

  didInsertElement: function () {
    App.tooltip(this.$("[rel='ZoomInTooltip']"), {
      placement: 'left',
      template: '<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner graph-tooltip"></div></div>'
    });
  },

  /**
   *
   * @param {string} source
   * @returns {App.Service}
   */
  findModelBySource: function (source) {
    if (source === 'HOST_METRICS' && App.get('services.hostMetrics').length > 0) {
      return App.get('services.hostMetrics');
    }
    var extendedModel = App.Service.extendedModel[source];
    if (extendedModel) {
      return App[extendedModel].find(source);
    }
    return App.Service.find(source);
  },

  willDestroyElement : function() {
    $("[rel='ZoomInTooltip']").tooltip('destroy');
  },

  /**
   * delete widget
   */
  deleteWidget: function () {
    this.get('parentView').hideWidget(this.get('id'));
  },

  /**
   * edit widget
   */
  editWidget: function () {
    var configObj = this.get('widgetConfig').create({
      thresholdMin: this.get('thresholdMin') + '',
      thresholdMax: this.get('thresholdMax') + '',
      maxValue: parseFloat(this.get('maxValue'))
    });
    this.showEditDialog(configObj);
  },

  /**
   *  show edit dialog
   * @param {Em.Object} configObj
   * @returns {App.ModalPopup}
   */
  showEditDialog: function (configObj) {
    var self = this;
    var maxValue = this.get('maxValue');

    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.popupHeader'),
      classNames: ['modal-edit-widget'],
      modalDialogClasses: ['modal-lg'],
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/edit_widget_popup'),
        configPropertyObj: configObj
      }),
      configObj: configObj,
      disablePrimary: Em.computed.or('configObj.thresholdMinError', 'configObj.thresholdMaxError'),
      primary: Em.I18n.t('common.apply'),
      onPrimary: function () {
        configObj.observeThreshMinValue();
        configObj.observeThreshMaxValue();
        if (!configObj.thresholdMinError && !configObj.thresholdMaxError) {
          self.set('thresholdMin', parseFloat(configObj.get('thresholdMin')));
          self.set('thresholdMax', parseFloat(configObj.get('thresholdMax')));

          var parent = self.get('parentView');
          var userPreferences = parent.get('userPreferences');
          userPreferences.threshold[Number(self.get('id'))] = [configObj.get('thresholdMin'), configObj.get('thresholdMax')];
          parent.saveWidgetsSettings(userPreferences);
          parent.renderWidgets();

          this.hide();
        }
      },

      didInsertElement: function () {
        this._super();
        var _this = this;
        var handlers = [configObj.get('thresholdMin'), configObj.get('thresholdMax')];

        $("#slider-range").slider({
          range: true,
          min: 0,
          max: maxValue,
          values: handlers,
          create: function () {
            _this.updateColors(handlers);
          },
          slide: function (event, ui) {
            _this.updateColors(ui.values);
            configObj.set('thresholdMin', ui.values[0] + '');
            configObj.set('thresholdMax', ui.values[1] + '');
          },
          change: function (event, ui) {
            _this.updateColors(ui.values);
          }
        });
      },
      updateColors: function (handlers) {
        var colors = [App.healthStatusGreen, App.healthStatusOrange, App.healthStatusRed];
        var colorStops = colors[0] + ", ";

        for (var i = 0; i < handlers.length; i++) {
          colorStops += colors[i] + " " + handlers[i] * 100 / maxValue + "%,";
          colorStops += colors[i + 1] + " " + handlers[i] * 100 / maxValue + "%,";
        }
        colorStops += colors[colors.length - 1];
        var sliderElement = $('#slider-range');
        var gradient = 'linear-gradient(left,' + colorStops + ')';

        sliderElement.css('background-image', '-webkit-' + gradient);
        sliderElement.css('background-image', '-ms-' + gradient);
        sliderElement.css('background-image', '-moz-' + gradient);
        sliderElement.find('.ui-widget-header').css({
          'background-color': '#FF8E00',
          'background-image': 'none'
        });
      }
    });
  }
});
