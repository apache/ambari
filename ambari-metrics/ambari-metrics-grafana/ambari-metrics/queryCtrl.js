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
define([
      'angular',
      'lodash'
    ],
    function (angular, _) {
      'use strict';

      var module = angular.module('grafana.controllers');

      module.controller('AmbariMetricsQueryCtrl', function($scope) {

        $scope.init = function() {
          $scope.target.errors = validateTarget($scope.target);
          $scope.aggregators = ['none','avg', 'sum', 'min', 'max'];
          $scope.precisions = ['default','seconds', 'minutes', 'hours', 'days'];
          $scope.transforms = ['none','diff','rate'];
          $scope.seriesAggregators = ['none', 'avg', 'sum', 'min', 'max'];

          if (!$scope.target.aggregator) {
            $scope.target.aggregator = 'avg';
          }
          $scope.precisionInit = function () {
           if (typeof $scope.target.precision == 'undefined') {
                $scope.target.precision = "default";
           }
          };
          $scope.transform = function () {
           if (typeof $scope.target.transform == 'undefined') {
                $scope.target.transform = "none";
           }
          };
          $scope.seriesAggregator = function () {
           if (typeof $scope.target.seriesAggregator == 'undefined') {
                $scope.target.seriesAggregator = "none";
           }
          };
          $scope.$watch('target.app', function (newValue) {
            if (newValue === '') {
              $scope.target.metric = '';
              $scope.target.hosts = '';
              $scope.target.cluster = '';
            }
          });
          if (!$scope.target.downsampleAggregator) {
            $scope.target.downsampleAggregator = 'avg';
          }

          $scope.datasource.getAggregators().then(function(aggs) {
            $scope.aggregators = aggs;
          });
        };

        $scope.targetBlur = function() {
          $scope.target.errors = validateTarget($scope.target);

          // this does not work so good
          if (!_.isEqual($scope.oldTarget, $scope.target) && _.isEmpty($scope.target.errors)) {
            $scope.oldTarget = angular.copy($scope.target);
            $scope.get_data();
          }
        };

        $scope.getTextValues = function(metricFindResult) {
          return _.map(metricFindResult, function(value) { return value.text; });
        };

        $scope.suggestApps = function(query, callback) {
          $scope.datasource.suggestApps(query)
            .then($scope.getTextValues)
            .then(callback);
        };

        $scope.suggestClusters = function(query, callback) {
          $scope.datasource.suggestClusters($scope.target.app)
            .then($scope.getTextValues)
            .then(callback);
        };

        $scope.suggestHosts = function(query, callback) {
          $scope.datasource.suggestHosts($scope.target.app, $scope.target.cluster)
            .then($scope.getTextValues)
            .then(callback);
        };

        $scope.suggestMetrics = function(query, callback) {
          $scope.datasource.suggestMetrics(query, $scope.target.app)
            .then($scope.getTextValues)
            .then(callback);
        };

        $scope.suggestTagKeys = function(query, callback) {
          $scope.datasource.metricFindQuery('tag_names(' + $scope.target.metric + ')')
              .then($scope.getTextValues)
              .then(callback);
        };

        $scope.suggestTagValues = function(query, callback) {
          $scope.datasource.metricFindQuery('tag_values(' + $scope.target.metric + ',' + $scope.target.currentTagKey + ')')
              .then($scope.getTextValues)
              .then(callback);
        };

        $scope.addTag = function() {
          if (!$scope.addTagMode) {
            $scope.addTagMode = true;
            return;
          }

          if (!$scope.target.tags) {
            $scope.target.tags = {};
          }

          $scope.target.errors = validateTarget($scope.target);

          if (!$scope.target.errors.tags) {
            $scope.target.tags[$scope.target.currentTagKey] = $scope.target.currentTagValue;
            $scope.target.currentTagKey = '';
            $scope.target.currentTagValue = '';
            $scope.targetBlur();
          }

          $scope.addTagMode = false;
        };

        $scope.removeTag = function(key) {
          delete $scope.target.tags[key];
          $scope.targetBlur();
        };

        function validateTarget(target) {
          var errs = {};

          if (target.tags && _.has(target.tags, target.currentTagKey)) {
            errs.tags = "Duplicate tag key '" + target.currentTagKey + "'.";
          }

          return errs;
        }

        $scope.init();
      });

    });
