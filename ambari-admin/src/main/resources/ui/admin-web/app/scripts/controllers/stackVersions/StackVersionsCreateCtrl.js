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
.controller('StackVersionsCreateCtrl', ['$scope', 'StackVersions', '$routeParams', function($scope, StackVersions, $routeParams) {
  $scope.clusterName = $routeParams.clusterName;
  $scope.upgradePack = {
    value: null,
    options: [
      'Pack 1',
      'Pack 2',
      'Pack 3',
      'Pack 4'
    ]
  };
  $scope.upgradePack.value = $scope.upgradePack.options[0];

  $scope.upgradeStack = {
    value: null,
    options: [
      'HDP-2.2',
      'HDP-2.3',
      'HDP-2.4'
    ]
  };
  $scope.upgradeStack.value = $scope.upgradeStack.options[0];

  $scope.repositories = [
    {
      os: 'redhat5',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null},
      ],
      selected: false
    },
    {
      os: 'redhat6',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null},
      ],
      selected: false
    },
    {
      os: 'sles11',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null},
      ],
      selected: false
    },
    {
      os: 'ubuntu12',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null},
      ],
      selected: false
    }
  ];
  
}]);
