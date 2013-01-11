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

App.WizardStep8View = Em.View.extend({

  templateName: require('templates/wizard/step8'),

  didInsertElement: function () {
    var controller = this.get('controller');
    controller.loadStep();
  },
  spinner : null,

  printReview: function() {
    var o = $("#step8-info");
    o.jqprint();
  },

  showLoadingIndicator: function(){
    if(this.get('controller.hasErrorOccurred')){
      $('.spinner').hide();
      return;
    }
    if(!this.get('controller.isSubmitDisabled')){
      return;
    }

    var opts = {
      lines: 13, // The number of lines to draw
      length: 7, // The length of each line
      width: 4, // The line thickness
      radius: 10, // The radius of the inner circle
      corners: 1, // Corner roundness (0..1)
      rotate: 0, // The rotation offset
      color: '#000', // #rgb or #rrggbb
      speed: 1, // Rounds per second
      trail: 60, // Afterglow percentage
      shadow: false, // Whether to render a shadow
      hwaccel: false, // Whether to use hardware acceleration
      className: 'spinner', // The CSS class to assign to the spinner
      zIndex: 2e9, // The z-index (defaults to 2000000000)
      top: 'auto', // Top position relative to parent in px
      left: 'auto' // Left position relative to parent in px
    };
    var target = $('#spinner')[0];
    this.set('spinner', new Spinner(opts).spin(target));

    /*var el = $('#spinner').children('b');
    el.css('display', 'inline-block');
    var deg = 0;
    var timeoutId = setInterval(function(){
      if(!$('#spinner').length){
        clearInterval(timeoutId);
      }
      deg += 15;
      deg %= 360;
      el.css('transform', 'rotate(' + deg + 'deg)');
      el.css('-ms-transform', 'rotate(' + deg + 'deg)');
      el.css('-o-transform', 'rotate(' + deg + 'deg)');
      el.css('-moz-transform', 'rotate(' + deg + 'deg)');
      el.css('-webkit-transform', 'rotate(' + deg + 'deg)');
    }, 80);*/
  }.observes('controller.isSubmitDisabled','controller.hasErrorOccurred')
});
