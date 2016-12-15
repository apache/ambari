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

App.EditableWidgetMixin = Em.Mixin.create({

  hintInfo: '',

  editWidget: function () {
    var self = this;
    var configObj = Ember.Object.create({
      thresholdMin: self.get('thresholdMin') + '',
      thresholdMax: self.get('thresholdMax') + '',
      hintInfo: self.get('hintInfo'),
      isThresh1Error: false,
      isThresh2Error: false,
      errorMessage1: "",
      errorMessage2: "",
      maxValue: 'infinity',
      observeNewThresholdValue: function () {
        var thresholdMin = this.get('thresholdMin');
        var thresholdMax = this.get('thresholdMax');
        if (thresholdMin.trim() !== "") {
          if (isNaN(thresholdMin) || thresholdMin < 0) {
            this.set('isThresh1Error', true);
            this.set('errorMessage1', 'Invalid! Enter a number larger than 0');
          } else if ( this.get('isThresh2Error') === false && parseFloat(thresholdMax)<= parseFloat(thresholdMin)){
            this.set('isThresh1Error', true);
            this.set('errorMessage1', 'Threshold 1 should be smaller than threshold 2 !');
          } else {
            this.set('isThresh1Error', false);
            this.set('errorMessage1', '');
          }
        } else {
          this.set('isThresh1Error', true);
          this.set('errorMessage1', 'This is required');
        }

        if (thresholdMax.trim() !== "") {
          if (isNaN(thresholdMax) || thresholdMax < 0) {
            this.set('isThresh2Error', true);
            this.set('errorMessage2', 'Invalid! Enter a number larger than 0');
          } else {
            this.set('isThresh2Error', false);
            this.set('errorMessage2', '');
          }
        } else {
          this.set('isThresh2Error', true);
          this.set('errorMessage2', 'This is required');
        }

      }.observes('thresholdMin', 'thresholdMax')

    });

    App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.popupHeader'),
      classNames: [ 'modal-edit-widget'],
      modalDialogClasses: ['modal-lg'],
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/edit_widget_popup'),
        configPropertyObj: configObj
      }),
      primary: Em.I18n.t('common.apply'),
      onPrimary: function () {
        configObj.observeNewThresholdValue();
        if (!configObj.isThresh1Error && !configObj.isThresh2Error) {

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
        var colors = [App.healthStatusGreen, App.healthStatusOrange, App.healthStatusRed]; //color green, orange ,red
        var handlers = [33, 66]; //fixed value

        $("#slider-range").slider({
          range: true,
          disabled: true, //handlers cannot move
          min: 0,
          max: 100,
          values: handlers,
          create: function (event, ui) {
            self.updateColors(handlers, colors);
          }
        });
      }
    });
  }

});