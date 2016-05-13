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

/**
 * @type {Em.Mixin}
 */
App.SingleNumericThresholdMixin = Em.Mixin.create({

  /**
   * @type {Em.Object}
   * @class
   */
  widgetConfig: Ember.Object.extend({
    thresh1: '',
    hintInfo: '',
    isThresh1Error: false,
    errorMessage1: "",

    maxValue: 0,
    observeThresh1Value: function () {
      var thresh1 = this.get('thresh1');
      var maxValue = this.get('maxValue');

      if (thresh1.trim() !== "") {
        if (isNaN(thresh1) || thresh1 > maxValue || thresh1 < 0) {
          this.set('isThresh1Error', true);
          this.set('errorMessage1', Em.I18n.t('dashboard.widgets.error.invalid').format(maxValue));
        } else {
          this.set('isThresh1Error', false);
          this.set('errorMessage1', '');
        }
      } else {
        this.set('isThresh1Error', true);
        this.set('errorMessage1', Em.I18n.t('admin.users.editError.requiredField'));
      }
        this.updateSlider();
    }.observes('thresh1', 'maxValue'),

    updateSlider: function () {
      var thresh1 = this.get('thresh1');
      // update the slider handles and color
      if (this.get('isThresh1Error') === false) {
        $("#slider-range")
          .slider('values', 0, parseFloat(thresh1))
      }
    }
  }),

  /**
   * edit widget
   * @param {object} event
   */
  editWidget: function () {
    var parent = this;
    var maxTmp = parseFloat(this.get('maxValue'));
    var configObj = this.get('widgetConfig').create({
      thresh1: this.get('thresh1') + '',
      hintInfo: this.get('hintInfo') + '',
      maxValue: parseFloat(this.get('maxValue'))
    });

    var browserVersion = this.getInternetExplorerVersion();
    App.ModalPopup.show({
        header: Em.I18n.t('dashboard.widgets.popupHeader'),
        classNames: ['sixty-percent-width-modal-edit-widget'],
        bodyClass: Ember.View.extend({
          templateName: require('templates/main/dashboard/edit_widget_popup_single_threshold'),
          configPropertyObj: configObj
        }),
        primary: Em.I18n.t('common.apply'),
        onPrimary: function () {
          configObj.observeThresh1Value();
          if (!configObj.isThresh1Error) {
            parent.set('thresh1', parseFloat(configObj.get('thresh1')));
            if (!App.get('testMode')) {
              // save to persist
              var bigParent = parent.get('parentView');
              bigParent.getUserPref(bigParent.get('persistKey')).complete(function () {
                var oldValue = bigParent.get('currentPrefObject');
                oldValue.threshold[parseInt(parent.id, 10)] = [configObj.get('thresh1')];
                bigParent.postUserPref(parent.get('persistKey'), oldValue);
              });
            }
            this.hide();
          }
        },
        didInsertElement: function () {
          this._super();
          var handlers = [configObj.get('thresh1')];
          var colors = [App.healthStatusGreen, App.healthStatusRed]; //color green,red

          if (browserVersion === -1 || browserVersion > 9) {
            configObj.set('isIE9', false);
            configObj.set('isGreenRed', true);
            $("#slider-range").slider({
              range: false,
              min: 0,
              max: maxTmp,
              values: handlers,
              create: function () {
              updateColors(handlers);
              },
              slide: function (event, ui) {
              updateColors(ui.values);
                configObj.set('thresh1', ui.values[0] + '');
              },
              change: function (event, ui) {
              updateColors(ui.values);
              }
            });

            function updateColors(handlers) {
              var colorstops = colors[0] + ", "; // start with the first color
              for (var i = 0; i < handlers.length; i++) {
                colorstops += colors[i] + " " + handlers[i] * 100 / maxTmp + "%,";
                colorstops += colors[i + 1] + " " + handlers[i] * 100 / maxTmp + "%,";
              }
              colorstops += colors[colors.length - 1];
              var sliderElement = $('#slider-range');
              var css1 = '-webkit-linear-gradient(left,' + colorstops + ')'; // chrome & safari
              sliderElement.css('background-image', css1);
              var css2 = '-ms-linear-gradient(left,' + colorstops + ')'; // IE 10+
              sliderElement.css('background-image', css2);
              var css3 = '-moz-linear-gradient(left,' + colorstops + ')'; // Firefox
              sliderElement.css('background-image', css3);

              sliderElement.find('.ui-widget-header').css({'background-color': '#FF8E00', 'background-image': 'none'}); // change the  original ranger color
            }
          } else {
            configObj.set('isIE9', true);
            configObj.set('isGreenRed', true);
          }
        }

      });

  }

});
