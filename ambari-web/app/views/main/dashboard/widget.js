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
  model : function () {
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
    isThresh1Error: false,
    isThresh2Error: false,
    errorMessage1: "",
    errorMessage2: "",
    maxValue: 0,
    observeThresh1Value: function () {
      var thresholdMin = this.get('thresholdMin');
      var thresholdMax = this.get('thresholdMax');
      var maxValue = this.get('maxValue');

      if (thresholdMin.trim() !== "") {
        if (isNaN(thresholdMin) || thresholdMin > maxValue || thresholdMin < 0) {
          this.set('isThresh1Error', true);
          this.set('errorMessage1', Em.I18n.t('dashboard.widgets.error.invalid').format(maxValue));
        } else if (this.get('isThresh2Error') === false && parseFloat(thresholdMax) <= parseFloat(thresholdMin)) {
          this.set('isThresh1Error', true);
          this.set('errorMessage1', Em.I18n.t('dashboard.widgets.error.smaller'));
        } else {
          this.set('isThresh1Error', false);
          this.set('errorMessage1', '');
        }
      } else {
        this.set('isThresh1Error', true);
        this.set('errorMessage1', Em.I18n.t('admin.users.editError.requiredField'));
      }
      this.updateSlider();
    }.observes('thresholdMin', 'maxValue'),
    observeThresh2Value: function () {
      var thresholdMax = this.get('thresholdMax');
      var maxValue = this.get('maxValue');

      if (thresholdMax.trim() !== "") {
        if (isNaN(thresholdMax) || thresholdMax > maxValue || thresholdMax < 0) {
          this.set('isThresh2Error', true);
          this.set('errorMessage2', Em.I18n.t('dashboard.widgets.error.invalid').format(maxValue));
        } else {
          this.set('isThresh2Error', false);
          this.set('errorMessage2', '');
        }
      } else {
        this.set('isThresh2Error', true);
        this.set('errorMessage2', Em.I18n.t('admin.users.editError.requiredField'));
      }
      this.updateSlider();
    }.observes('thresholdMax', 'maxValue'),
    updateSlider: function () {
      var thresholdMin = this.get('thresholdMin');
      var thresholdMax = this.get('thresholdMax');
      // update the slider handles and color
      if (this.get('isThresh1Error') === false && this.get('isThresh2Error') === false) {
        $("#slider-range")
          .slider('values', 0, parseFloat(thresholdMin))
          .slider('values', 1, parseFloat(thresholdMax));
      }
    }
  }),

  didInsertElement: function () {
    App.tooltip(this.$("[rel='ZoomInTooltip']"), {
      placement: 'left',
      template: '<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner graph-tooltip"></div></div>'
    });
  },

  findModelBySource: function (source) {
    if (source === 'HOST_METRICS' && App.get('services.hostMetrics').length > 0) {
      return App.get('services.hostMetrics');
    }
    var extendedModel = App.Service.extendedModel[source];
    if (extendedModel) {
      return App[extendedModel].find(source);
    } else {
      return App.Service.find(source);
    }
  },

  willDestroyElement : function() {
    $("[rel='ZoomInTooltip']").tooltip('destroy');
  },

  /**
   * delete widget
   */
  deleteWidget: function () {
    var parent = this.get('parentView');
    var userPreferences = parent.get('userPreferences');
    var deletedId = this.get('id');
    var newValue = {
      visible: userPreferences.visible.slice(0).without(deletedId),
      hidden: userPreferences.hidden.concat([deletedId]),
      threshold: userPreferences.threshold
    };
    parent.saveWidgetsSettings(newValue);
    parent.renderWidgets();
  },

  /**
   * edit widget
   * @param {object} event
   */
  editWidget: function (event) {
    var configObj = this.get('widgetConfig').create({
      thresholdMin: this.get('thresholdMin') + '',
      thresholdMax: this.get('thresholdMax') + '',
      maxValue: parseFloat(this.get('maxValue'))
    });
    this.showEditDialog(configObj)
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
      primary: Em.I18n.t('common.apply'),
      onPrimary: function () {
        configObj.observeThresh1Value();
        configObj.observeThresh2Value();
        if (!configObj.isThresh1Error && !configObj.isThresh2Error) {
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
        var browserVersion = self.getInternetExplorerVersion();
        var handlers = [configObj.get('thresholdMin'), configObj.get('thresholdMax')];
        var colors = [App.healthStatusGreen, App.healthStatusOrange, App.healthStatusRed]; //color green, orange ,red

        if (browserVersion === -1 || browserVersion > 9) {
          configObj.set('isIE9', false);
          configObj.set('isGreenOrangeRed', true);
          $("#slider-range").slider({
            range: true,
            min: 0,
            max: maxValue,
            values: handlers,
            create: function () {
              updateColors(handlers);
            },
            slide: function (event, ui) {
              updateColors(ui.values);
              configObj.set('thresholdMin', ui.values[0] + '');
              configObj.set('thresholdMax', ui.values[1] + '');
            },
            change: function (event, ui) {
              updateColors(ui.values);
            }
          });

          function updateColors(handlers) {
            var colorstops = colors[0] + ", "; // start with the first color
            for (var i = 0; i < handlers.length; i++) {
              colorstops += colors[i] + " " + handlers[i] * 100 / maxValue + "%,";
              colorstops += colors[i + 1] + " " + handlers[i] * 100 / maxValue + "%,";
            }
            colorstops += colors[colors.length - 1];
            var sliderElement = $('#slider-range');
            var css1 = '-webkit-linear-gradient(left,' + colorstops + ')'; // chrome & safari
            sliderElement.css('background-image', css1);
            var css2 = '-ms-linear-gradient(left,' + colorstops + ')'; // IE 10+
            sliderElement.css('background-image', css2);
            //$('#slider-range').css('filter', 'progid:DXImageTransform.Microsoft.gradient( startColorStr= ' + colors[0] + ', endColorStr= ' + colors[2] +',  GradientType=1 )' ); // IE 10-
            var css3 = '-moz-linear-gradient(left,' + colorstops + ')'; // Firefox
            sliderElement.css('background-image', css3);

            sliderElement.find('.ui-widget-header').css({'background-color': '#FF8E00', 'background-image': 'none'}); // change the  original ranger color
          }
        } else {
          configObj.set('isIE9', true);
          configObj.set('isGreenOrangeRed', true);
        }
      }
    });
  },

  /**
   * @returns {number}
   */
  getInternetExplorerVersion: function () {
    var rv = -1; //return -1 for other browsers
    if (navigator.appName === 'Microsoft Internet Explorer') {
      var ua = navigator.userAgent;
      var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null) {
        rv = parseFloat(RegExp.$1); // IE version 1-10
      }
    }
    return rv;
  },

  /**
   * for widgets has hidden info(hover info),
   * calculate the hover content top number
   * based on how long the hiddenInfo is
   * @returns {string}
   */
  hoverContentTopClass: function () {
    var lineNum = this.get('hiddenInfo.length');
    if (lineNum === 2) {
      return "content-hidden-two-line";
    }
    if (lineNum === 3) {
      return "content-hidden-three-line";
    }
    if (lineNum === 4) {
      return "content-hidden-four-line";
    }
    if (lineNum === 5) {
      return "content-hidden-five-line";
    }
    if (lineNum === 6) {
      return "content-hidden-six-line";
    }
    return '';
  }.property('hiddenInfo.length')

});
