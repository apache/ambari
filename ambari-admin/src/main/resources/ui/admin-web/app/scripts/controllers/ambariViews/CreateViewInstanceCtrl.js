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
.controller('CreateViewInstanceCtrl',['$scope', 'View', '$modalInstance', 'viewVersion', function($scope, View, $modalInstance, viewVersion) {
	$scope.form = {};

	$scope.view = viewVersion;
	$scope.isAdvancedClosed = true;
	$scope.instanceExists = false;


	$scope.instance = {
		view_name: viewVersion.ViewVersionInfo.view_name,
		version: viewVersion.ViewVersionInfo.version,
		instance_name: '',
		label: '',
		visible: true,
		icon_path: '',
		icon64_path: '',
		properties: viewVersion.ViewVersionInfo.parameters
	};

	$scope.nameValidationPattern = /^\s*\w*\s*$/;

	$scope.save = function() {
		window.f = $scope.form.isntanceCreateForm;
		$scope.form.isntanceCreateForm.submitted = true;
		if($scope.form.isntanceCreateForm.$valid){
			View.getInstance($scope.instance.view_name, $scope.instance.version, $scope.instance.instance_name)
			.then(function(data) {
				if (data.ViewInstanceInfo) {
					$scope.instanceExists = true;
				} else {
					View.createInstance($scope.instance)
					.then(function(data) {
						$modalInstance.close();
					})
					.catch(function(data) {
						console.log(data);
					});
				}
			})
			.catch(function() {
				console.log('Error');
			});
		}
	};

	$scope.cancel = function() {
		$modalInstance.dismiss();
	};

}]);