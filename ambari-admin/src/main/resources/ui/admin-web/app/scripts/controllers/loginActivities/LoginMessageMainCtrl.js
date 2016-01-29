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

angular.module('ambariAdminConsole')
  .controller('LoginMessageMainCtrl',['$scope', 'Alert', '$timeout', '$http', '$translate', function($scope, Alert, $timeout, $http, $translate) {
    var $t = $translate.instant;
    $scope.status = false;
    $scope.motdExists = false;
    $scope.text = "";
    $scope.submitDisabled = true;

    $http.get('/api/v1/admin-settings/motd').then(function (res) {
      var respons = JSON.parse(res.data.AdminSettings.content);
      $scope.text = respons.text ? respons.text : "";
      $scope.status = respons.status && respons.status == "true" ? true : false;
      $scope.motdExists = true;
    });

    $scope.inputChangeEvent = function(){
      $scope.submitDisabled = false;
    };
    $scope.changeStatus = function(){
      $scope.status = !$scope.status;
      $scope.submitDisabled = false;
    };

    $scope.saveLoginMsg = function(form) {
      var method = $scope.motdExists ? 'PUT' : 'POST';
      var data = {
        'AdminSettings' : {
          'content' : '{"text":"' + $scope.text + '", "status":"' + $scope.status + '"}',
          'name' : 'motd',
          'setting_type' : 'ambari-server'
        }
      };
      form.submitted = true;
      if (form.$valid){
        $scope.submitDisabled = true;
        $http({
          method: method,
          url: '/api/v1/admin-settings/' + ($scope.motdExists ? 'motd' : ''),
          data: data
        }).then(function successCallback() {
          $scope.motdExists = true;
        }, function errorCallback(data) {
          $scope.submitDisabled = false;
          Alert.error($t('common.loginActivities.saveError'), data.data.message);
        });
      }
    };
  }]);
