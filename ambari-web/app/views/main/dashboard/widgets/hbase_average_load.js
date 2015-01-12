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

App.HBaseAverageLoadView = App.TextDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.HBaseAverageLoad'),
  id: '14',

  model_type: 'hbase',
  hiddenInfo: function () {
    var avgLoad = this.get('model.averageLoad');
    if (isNaN(avgLoad)) {
      avgLoad = Em.I18n.t('services.service.summary.notAvailable');
    }
    var result = [];
    result.pushObject(Em.I18n.t('dashboard.services.hbase.averageLoadPerServer').format(avgLoad));
    return result;
  }.property("model.averageLoad"),

  isGreen: function () {
    return this.get('data') <= this.get('thresh1');
  }.property('data','thresh1','thresh2'),
  isRed: function () {
    return this.get('data') > this.get('thresh2');
  }.property('data','thresh1','thresh2'),
  isNA: function (){
    return this.get('data') === null || isNaN(this.get('data'));
  }.property('data'),

  thresh1: 0.5,
  thresh2: 2,
  maxValue: 'infinity',

  data: function () {
    return this.get('model.averageLoad');
  }.property("model.averageLoad"),

  content: function (){
    if(this.get('data') || this.get('data') == 0){
      return this.get('data') + "";
    }else{
      return Em.I18n.t('services.service.summary.notAvailable');
    }
  }.property('model.averageLoad'),

  editWidget: function (event) {
    var parent = this;
    var configObj = Ember.Object.create({
      thresh1: parent.get('thresh1') + '',
      thresh2: parent.get('thresh2') + '',
      hintInfo: Em.I18n.t('dashboard.widgets.hintInfo.hint2'),
      isThresh1Error: false,
      isThresh2Error: false,
      errorMessage1: "",
      errorMessage2: "",
      maxValue: 'infinity',
      observeNewThresholdValue: function () {
        var thresh1 = this.get('thresh1');
        var thresh2 = this.get('thresh2');
        if (thresh1.trim() != "") {
          if (isNaN(thresh1) || thresh1 < 0) {
            this.set('isThresh1Error', true);
            this.set('errorMessage1', 'Invalid! Enter a number larger than 0');
          } else if ( this.get('isThresh2Error') === false && parseFloat(thresh2)<= parseFloat(thresh1)){
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

        if (thresh2.trim() != "") {
          if (isNaN(thresh2) || thresh2 < 0) {
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

      }.observes('thresh1', 'thresh2')

    });

    var browserVerion = this.getInternetExplorerVersion();
    App.ModalPopup.show( {
      header: Em.I18n.t('dashboard.widgets.popupHeader'),
      classNames: [ 'sixty-percent-width-modal-edit-widget'],
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/edit_widget_popup'),
        configPropertyObj: configObj
      }),
      primary: Em.I18n.t('common.apply'),
      onPrimary: function () {
        configObj.observeNewThresholdValue();
        if (!configObj.isThresh1Error && !configObj.isThresh2Error) {
          parent.set('thresh1', parseFloat(configObj.get('thresh1')) );
          parent.set('thresh2', parseFloat(configObj.get('thresh2')) );
          if (!App.get('testMode')) {
            //save to persist
            var big_parent = parent.get('parentView');
            big_parent.getUserPref(big_parent.get('persistKey'));
            var oldValue = big_parent.get('currentPrefObject');
            oldValue.threshold[parseInt(parent.id)] = [configObj.get('thresh1'), configObj.get('thresh2')];
            big_parent.postUserPref(big_parent.get('persistKey'),oldValue);
          }

          this.hide();
        }
      },

      didInsertElement: function () {
        var colors = [App.healthStatusGreen, App.healthStatusOrange, App.healthStatusRed]; //color green, orange ,red
        var handlers = [33, 66]; //fixed value

        if (browserVerion == -1 || browserVerion > 9) {
          configObj.set('isIE9', false);
          configObj.set('isGreenOrangeRed', true);
          $("#slider-range").slider({
            range:true,
            disabled:true, //handlers cannot move
            min: 0,
            max: 100,
            values: handlers,
            create: function (event, ui) {
              parent.updateColors(handlers, colors);
            }
          });
        } else {
          configObj.set('isIE9', true);
          configObj.set('isGreenOrangeRed', true);
        }
      }
    });
  }

});
