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
App.EditableWithLimitWidgetMixin = Em.Mixin.create({

  hintInfo: '',

  editWidget: function () {
    var parent = this;
    var maxTmp = parseFloat(parent.get('maxValue'));
    var configObj = Ember.Object.create({
      thresholdMin: parent.get('thresholdMin') + '',
      thresholdMax: parent.get('thresholdMax') + '',
      hintInfo: parent.get('hintInfo'),
      thresholdMinError: false,
      thresholdMaxError: false,
      thresholdMinErrorMessage: '',
      thresholdMaxErrorMessage: '',
      maxValue: maxTmp,
      observeNewThresholdValue: function () {
        var thresholdMin = this.get('thresholdMin');
        var thresholdMax = this.get('thresholdMax');
        if (thresholdMin.trim() !== '') {
          if (isNaN(thresholdMin) || thresholdMin > maxTmp || thresholdMin < 0){
            this.set('thresholdMinError', true);
            this.set('thresholdMinErrorMessage', 'Invalid! Enter a number between 0 - ' + maxTmp);
          } else if ( this.get('thresholdMaxError') === false && parseFloat(thresholdMax)<= parseFloat(thresholdMin)) {
            this.set('thresholdMinError', true);
            this.set('thresholdMinErrorMessage', 'Threshold 1 should be smaller than threshold 2 !');
          } else {
            this.set('thresholdMinError', false);
            this.set('thresholdMinErrorMessage', '');
          }
        } else {
          this.set('thresholdMinError', true);
          this.set('thresholdMinErrorMessage', 'This is required');
        }

        if (thresholdMax.trim() !== '') {
          if (isNaN(thresholdMax) || thresholdMax > maxTmp || thresholdMax < 0) {
            this.set('thresholdMaxError', true);
            this.set('thresholdMaxErrorMessage', 'Invalid! Enter a number between 0 - ' + maxTmp);
          } else {
            this.set('thresholdMaxError', false);
            this.set('thresholdMaxErrorMessage', '');
          }
        } else {
          this.set('thresholdMaxError', true);
          this.set('thresholdMaxErrorMessage', 'This is required');
        }

        // update the slider handles and color
        if (!this.get('thresholdMinError') && !this.get('thresholdMaxError')) {
          $("#slider-range").slider('values', 0 , parseFloat(thresholdMin));
          $("#slider-range").slider('values', 1 , parseFloat(thresholdMax));
        }
      }.observes('thresholdMin', 'thresholdMax')

    });

    App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.popupHeader'),
      classNames: ['modal-edit-widget'],
      modalDialogClasses: ['modal-lg'],
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/edit_widget_popup'),
        configPropertyObj: configObj
      }),
      primary: Em.I18n.t('common.apply'),
      onPrimary: function () {
        configObj.observeNewThresholdValue();
        if (!configObj.thresholdMinError && !configObj.thresholdMaxError) {
          parent.set('thresholdMin', parseFloat(configObj.get('thresholdMin')) );
          parent.set('thresholdMax', parseFloat(configObj.get('thresholdMax')) );
          if (!App.get('testMode')) {
            var bigParent = parent.get('parentView');
            bigParent.getUserPref(bigParent.get('persistKey'));
            var oldValue = bigParent.get('currentPrefObject');
            oldValue.threshold[parseInt(parent.id, 10)] = [configObj.get('thresholdMin'), configObj.get('thresholdMax')];
            bigParent.postUserPref(bigParent.get('persistKey'),oldValue);
          }
          this.hide();
        }
      },

      didInsertElement: function () {
        this._super();
        var handlers = [configObj.get('thresholdMin'), configObj.get('thresholdMax')];
        var colors = [App.healthStatusRed, App.healthStatusOrange, App.healthStatusGreen]; //color red, orange, green

        $("#slider-range").slider({
          range: true,
          min: 0,
          max: maxTmp,
          values: handlers,
          create: function () {
            parent.updateColors(handlers, colors);
          },
          slide: function (event, ui) {
            parent.updateColors(ui.values, colors);
            configObj.set('thresholdMin', ui.values[0] + '');
            configObj.set('thresholdMax', ui.values[1] + '');
          },
          change: function (event, ui) {
            parent.updateColors(ui.values, colors);
          }
        });

      }
    });
  }

});
