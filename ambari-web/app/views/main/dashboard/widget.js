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

  title: null,
  templateName: null, // each has specific template

  /**
   * Setup model for widget by `model_type`. Usually `model_type` is a lowercase service name,
   * for example `hdfs`, `yarn`, etc. You need to set `model_type` in extended object View, for example
   * look App.DataNodeUpView.
   * @return {Object} - model that set up in App.MainDashboardView.setWidgetsDataModel()
   */
  model : function () {
    if (!this.get('model_type')) return {};
    return this.get('parentView').get(this.get('model_type') + '_model');
  }.property(), //data bind from parent view

  id: null, // id 1-10 used to identify
  viewID: function(){ // used by re-sort
    return 'widget-' + this.get('id');
  }.property('id'),  //html id bind to view-class: widget-(1)
  attributeBindings: ['viewID'],

  isPieChart: false,
  isText: false,
  isProgressBar: false,
  isLinks: false,
  content: null, // widget content pieChart/ text/ progress bar/links/ metrics. etc
  hiddenInfo: null, // more info details
  hiddenInfoClass: "hidden-info-two-line",

  thresh1: null, //@type {Number}
  thresh2: null, //@type {Number}

  didInsertElement: function () {
    App.tooltip(this.$("[rel='ZoomInTooltip']"), {placement : 'left'});
  },

  deleteWidget: function (event) {
    var parent = this.get('parentView');
    var self = this;
    if (App.get('testMode')) {
      //update view on dashboard
      var objClass = parent.widgetsMapper(this.id);
      parent.get('visibleWidgets').removeObject(objClass);
      parent.get('hiddenWidgets').pushObject(Em.Object.create({displayName: this.get('title'), id: this.get('id'), checked: false}));
    } else {
      //reconstruct new persist value then post in persist
      parent.getUserPref(parent.get('persistKey')).complete(function(){
        var oldValue = parent.get('currentPrefObject');
        var deletedId = self.get('id');
        var newValue = Em.Object.create({
          dashboardVersion: oldValue.dashboardVersion,
          visible: [],
          hidden: oldValue.hidden,
          threshold: oldValue.threshold
        });
        for (var i = 0; i <= oldValue.visible.length - 1; i++) {
          if (oldValue.visible[i] != deletedId) {
            newValue.visible.push(oldValue.visible[i]);
          }
        }
        newValue.hidden.push([deletedId, self.get('title')]);
        parent.postUserPref(parent.get('persistKey'), newValue);
        parent.translateToReal(newValue);
      });
    }
  },

  editWidget: function (event) {
    var self = this;
    var max_tmp =  parseFloat(self.get('maxValue'));
    var configObj = Ember.Object.create({
      thresh1: self.get('thresh1') + '',
      thresh2: self.get('thresh2') + '',
      hintInfo: Em.I18n.t('dashboard.widgets.hintInfo.common').format(max_tmp),
      isThresh1Error: false,
      isThresh2Error: false,
      errorMessage1: "",
      errorMessage2: "",
      maxValue: max_tmp,
      observeNewThresholdValue: function () {
        var thresh1 = this.get('thresh1');
        var thresh2 = this.get('thresh2');
        if (thresh1.trim() != "") {
          if (isNaN(thresh1) || thresh1 > max_tmp || thresh1 < 0) {
            this.set('isThresh1Error', true);
            this.set('errorMessage1', 'Invalid! Enter a number between 0 - ' + max_tmp);
          } else if (this.get('isThresh2Error') === false && parseFloat(thresh2)<= parseFloat(thresh1)) {
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
          if (isNaN(thresh2) || thresh2 > max_tmp || thresh2 < 0) {
            this.set('isThresh2Error', true);
            this.set('errorMessage2', 'Invalid! Enter a number between 0 - ' + max_tmp);
          } else {
            this.set('isThresh2Error', false);
            this.set('errorMessage2', '');
          }
        } else {
          this.set('isThresh2Error', true);
          this.set('errorMessage2', 'This is required');
        }

        // update the slider handles and color
        if (this.get('isThresh1Error') === false && this.get('isThresh2Error') === false) {
          $("#slider-range").slider('values', 0 , parseFloat(thresh1));
          $("#slider-range").slider('values', 1 , parseFloat(thresh2));
        }
      }.observes('thresh1', 'thresh2')

    });

    var browserVersion = this.getInternetExplorerVersion();
    App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.popupHeader'),
      classNames: [ 'sixty-percent-width-modal-edit-widget' ],
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/edit_widget_popup'),
        configPropertyObj: configObj
      }),
      primary: Em.I18n.t('common.apply'),
      onPrimary: function() {
        configObj.observeNewThresholdValue();
        if (!configObj.isThresh1Error && !configObj.isThresh2Error) {
          self.set('thresh1', parseFloat(configObj.get('thresh1')) );
          self.set('thresh2', parseFloat(configObj.get('thresh2')) );

          if (!App.get('testMode')) {
            // save to persist
            var parent = self.get('parentView');
            parent.getUserPref(parent.get('persistKey')).complete(function () {
              var oldValue = parent.get('currentPrefObject');
              oldValue.threshold[parseInt(self.get('id'))] = [configObj.get('thresh1'), configObj.get('thresh2')];
              parent.postUserPref(parent.get('persistKey'), oldValue);
            });
          }

          this.hide();
        }
      },

      didInsertElement: function () {
        var handlers = [configObj.get('thresh1'), configObj.get('thresh2')];
        var colors = ['#95A800', '#FF8E00', '#B80000']; //color green, orange ,red

        if (browserVersion == -1 || browserVersion > 9) {
          configObj.set('isIE9', false);
          configObj.set('isGreenOrangeRed', true);
          $("#slider-range").slider({
            range: true,
            min: 0,
            max: max_tmp,
            values: handlers,
            create: function (event, ui) {
              updateColors(handlers);
            },
            slide: function (event, ui) {
              updateColors(ui.values);
              configObj.set('thresh1', ui.values[0] + '');
              configObj.set('thresh2', ui.values[1] + '');
            },
            change: function (event, ui) {
              updateColors(ui.values);
            }
          });

          function updateColors(handlers) {
            var colorstops = colors[0] + ", "; // start with the first color
            for (var i = 0; i < handlers.length; i++) {
              colorstops += colors[i] + " " + handlers[i]*100/max_tmp + "%,";
              colorstops += colors[i+1] + " " + handlers[i]*100/max_tmp + "%,";
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

  getInternetExplorerVersion: function (){
    var rv = -1; //return -1 for other browsers
    if (navigator.appName == 'Microsoft Internet Explorer') {
      var ua = navigator.userAgent;
      var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null)
        rv = parseFloat( RegExp.$1 ); // IE version 1-10
    }
    var isFirefox = typeof InstallTrigger !== 'undefined';   // Firefox 1.0+
    if (isFirefox) {
      return -2;
    }else{
      return rv;
    }
  },

  /**
   * for widgets has hidden info(hover info),
   * calculate the hover content top number
   * based on how long the hiddenInfo is
   */
  hoverContentTopClass: function () {
    var lineNum = this.get('hiddenInfo.length');
    if (lineNum == 2) {
      return "content-hidden-two-line";
    } else if (lineNum == 3) {
      return "content-hidden-three-line";
    } else if (lineNum == 4) {
      return "content-hidden-four-line";
    } else if (lineNum == 5) {
      return "content-hidden-five-line";
    } else if (lineNum == 6) {
      return "content-hidden-six-line";
    }
    return '';
  }.property('hiddenInfo.length')

});


App.DashboardWidgetView.reopenClass({
  class: 'span2p4'
});
