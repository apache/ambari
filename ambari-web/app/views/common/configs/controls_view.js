/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.ControlsView = Ember.View.extend({

	classNames: ['display-inline-block'],

	templateName: require('templates/common/configs/controls'),

	serviceConfigProperty: null,

	showActions: function() {
		return App.isAccessible('ADMIN') && this.get('serviceConfigProperty.isEditable') && this.get('serviceConfigProperty.isRequiredByAgent') && !this.get('serviceConfigProperty.isComparison');
	}.property('serviceConfigProperty.isEditable', 'serviceConfigProperty.isRequiredByAgent', 'serviceConfigProperty.isComparison'),

	showSwitchToGroup: function() {
		return !this.get('serviceConfigProperty.isEditable') && this.get('serviceConfigProperty.group');
	}.property('showActions', 'serviceConfigProperty.group'),

	showIsFinal: function() {
		return this.get('serviceConfigProperty.supportsFinal');
	}.property('serviceConfigProperty.supportsFinal'),

	showRemove: function() {
		return this.get('showActions') && this.get('serviceConfigProperty.isRemovable');
	}.property('showActions', 'serviceConfigProperty.isRemovable'),

	showOverride: function() {
		return this.get('showActions') && this.get('serviceConfigProperty.isPropertyOverridable');
	}.property('showActions', 'serviceConfigProperty.isPropertyOverridable'),

	showUndo: function() {
		return this.get('showActions') && !this.get('serviceConfigProperty.cantBeUndone') && this.get('serviceConfigProperty.isNotDefaultValue');
	}.property('showActions', 'serviceConfigProperty.cantBeUndone', 'serviceConfigProperty.isNotDefaultValue'),

	showSetRecommended: function() {
		return this.get('showActions') && this.get('serviceConfigProperty.recommendedValueExists');
	}.property('showActions', 'serviceConfigProperty.recommendedValueExists')

});

