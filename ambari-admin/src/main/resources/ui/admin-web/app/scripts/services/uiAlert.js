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
'use strict';

angular.module('angularAlert',[])
.factory('uiAlert', [function() {
  var alerts = [];
  var isRunning = false;

  var alertBoxGenerator = function(title, message, type) {
    var elem = angular.element('<div/>').addClass('alert');
    elem.css({
      'position': 'fixed',
      'left': '50%',
      'z-index': '10000',
      'opacity': '1',
      WebkitTransition : 'all .5s ease-in-out',
      MozTransition    : 'all .5s ease-in-out',
      MsTransition     : 'all .5s ease-in-out',
      OTransition      : 'all .5s ease-in-out',
      transition       : 'all .5s ease-in-out',
      '-webkit-transform': 'translateX(-50%)',
      '-ms-transform': 'translateX(-50%)',
      '-o-transform': 'translateX(-50%)',
      'transform': 'translateX(-50%)'
    });
    if(!message){
      elem.html(title);
    } else {
      elem.html('<strong>' + title + '</strong> ' + message);
    }

    elem.addClass('alert-' + (type ? type : 'info') );
    elem.appendTo('body');

    // return elem;
    alerts.push(elem);
    resetAlertsPositions();
  };

  var resetAlertsPositions = function() {
    var top = 10, height=0;
    for(var i = 0 ; i < alerts.length; i++){
      alerts[i].css('top', top);
      height = alerts[i].css('height').replace('px', '') * 1;
      top += height + 10;
    }

    if(!isRunning && alerts.length){
      isRunning = true;
      setTimeout(function() {
        alerts.shift().css('opacity', '0').one('transitionend webkitTransitionEnd oTransitionEnd otransitionend MSTransitionEnd', function() {
          isRunning = false;
          this.remove();
          resetAlertsPositions();
        });
      }, 5000);
    }
  };

  var Alert = function(title, message, type) {
    alertBoxGenerator(title, message, type);
  };

  Alert.success = function(title, message) {
    alertBoxGenerator(title, message, 'success');
  };

  Alert.info = function(title, message) {
    alertBoxGenerator(title, message, 'info');
  };

  Alert.warning = function(title, message) {
    alertBoxGenerator(title, message, 'warning');
  };


  Alert.danger = function(title, message) {
    alertBoxGenerator(title, message, 'danger');
  };

  return Alert;
}]);
