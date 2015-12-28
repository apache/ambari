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
var validator = require('utils/validator');

App.ConfigRecommendationParser = Em.Mixin.create(App.ConfigRecommendations, {

	stepConfigs: [],

	/**
	 * Method that goes through all configs
	 * and apply recommendations using callbacks
	 *
	 * @param recommendationObject
	 * @param configs
	 * @param parentProperties
	 * @param configGroup
	 * @param updateCallback
	 * @param removeCallback
	 * @param updateBoundariesCallback
	 */
	parseRecommendations: function(recommendationObject, configs, parentProperties, configGroup,
	                               updateCallback, removeCallback, updateBoundariesCallback) {
		var propertiesToDelete = [];
		configs.forEach(function (config) {
			var name = Em.get(config, 'name'), fileName = Em.get(config, 'filename'),
				site = App.config.getConfigTagFromFileName(fileName);
			if (recommendationObject[site]) {
				var properties = recommendationObject[site].properties,
					property_attributes = recommendationObject[site].property_attributes;
				if (properties) {
					var recommendedValue = App.config.formatValue(properties[name]);
					if (!Em.isNone(recommendedValue)) {
						/** update config **/
						updateCallback(config, recommendedValue, parentProperties, configGroup);

						delete recommendationObject[site].properties[name];
					}
				}
				if (property_attributes) {
					var propertyAttributes = property_attributes[name];
					var stackProperty = App.configsCollection.getConfigByName(name, fileName);
					for (var attr in propertyAttributes) {
						if (attr == 'delete' && this.allowUpdateProperty(parentProperties, name, fileName)) {
							propertiesToDelete.push(config);
						} else if (stackProperty) {
							/** update config boundaries **/
							updateBoundariesCallback(stackProperty, attr, propertyAttributes[attr], configGroup);
						}
					}
				}
			}
		}, this);

		if (propertiesToDelete.length) {
			propertiesToDelete.forEach(function (property) {
				/** remove config **/
				removeCallback(property, configs, parentProperties, configGroup);

			}, this);
		}
	},

	/**
	 * Method that goes through all configs
	 * and apply recommendations to configs when it's needed
	 *
	 * @param {Object} recommendationObject
	 * @param {Object[]} configs
	 * @param {Object[]} parentProperties
	 */
	updateConfigsByRecommendations: function (recommendationObject, configs, parentProperties) {
		this.parseRecommendations(recommendationObject, configs, parentProperties, null,
			this._updateConfigByRecommendation.bind(this), this._removeConfigByRecommendation.bind(this), this._updateBoundaries.bind(this));
	},

	/**
	 * This method goes through all config recommendations
	 * and trying to add new properties
	 *
	 * @param {Object} recommendationObject
	 * @param {Object[]} parentProperties
	 */
	addByRecommendations: function (recommendationObject, parentProperties) {
		for (var site in recommendationObject) {
			if (Object.keys(recommendationObject[site].properties).length) {
				var stepConfig = App.config.getStepConfigForProperty(this.get('stepConfigs'), site), configs = [];
				if (stepConfig) {
					for (var propertyName in recommendationObject[site].properties) {
						if (this.allowUpdateProperty(parentProperties, propertyName, site)) {
							this._addConfigByRecommendation(configs, propertyName, site, recommendationObject[site].properties[propertyName], parentProperties);
						}
					}
					var mergedConfigs = configs.concat(stepConfig.get('configs'));
					stepConfig.set('configs', mergedConfigs);
				}
			}
		}
	},

	/**
	 * Update config based on recommendations
	 *
	 * @param config
	 * @param recommendedValue
	 * @param parentProperties
	 * @protected
	 */
	_updateConfigByRecommendation: function (config, recommendedValue, parentProperties) {
		Em.assert('config should be defined', config);
		Em.set(config, 'recommendedValue', recommendedValue);
		if (this.allowUpdateProperty(parentProperties, Em.get(config, 'name'), Em.get(config, 'filename'))) {
			Em.set(config, 'value', recommendedValue);
			this.applyRecommendation(Em.get(config, 'name'), Em.get(config, 'filename'), Em.get(config, 'group.name'), recommendedValue, this._getInitialValue(config), parentProperties);
		}
		if (this.updateInitialOnRecommendations(Em.get(config, 'serviceName'))) {
			Em.set(config, 'initialValue', recommendedValue);
		}
	},

	/**
	 * Add config based on recommendations
	 *
	 * @param configs
	 * @param name
	 * @param fileName
	 * @param recommendedValue
	 * @param parentProperties
	 * @protected
	 */
	_addConfigByRecommendation: function (configs, name, fileName, recommendedValue, parentProperties) {
		fileName = App.config.getOriginalFileName(fileName);
		var stackConfig = App.configsCollection.getConfigByName(name, fileName),
			service = App.config.get('serviceByConfigTypeMap')[App.config.getConfigTagFromFileName(fileName)];
		if (service) {
			var serviceName = stackConfig ? stackConfig.serviceName : service && service.get('serviceName'),
				popupProperty = this.getRecommendation(name, fileName),
				initialValue = popupProperty ? popupProperty.value : null;

			var coreObject = {
				"value": recommendedValue,
				"recommendedValue": recommendedValue,
				"initialValue": this.updateInitialOnRecommendations(serviceName) ? recommendedValue : initialValue,
				"savedValue": !this.useInitialValue(serviceName) && !Em.isNone(initialValue) ? initialValue : null
			};
			var addedProperty = stackConfig || App.config.createDefaultConfig(name, serviceName, fileName, false);
			Em.setProperties(addedProperty, coreObject);
			var addedPropertyObject = App.ServiceConfigProperty.create(addedProperty);
			configs.pushObject(addedPropertyObject);
			addedPropertyObject.validate();

			this.applyRecommendation(name, fileName, "Default",
				recommendedValue, null, parentProperties);
		}
	},

	/**
	 * Remove config based on recommendations
	 *
	 * @param config
	 * @param configsCollection
	 * @param parentProperties
	 * @protected
	 */
	_removeConfigByRecommendation: function (config, configsCollection, parentProperties) {
		Em.assert('config and configsCollection should be defined', config && configsCollection);
		configsCollection.removeObject(config);

		this.applyRecommendation(Em.get(config, 'name'), Em.get(config, 'filename'), Em.get(config, 'group.name'),
			null, this._getInitialValue(config), parentProperties);
	},

	/**
	 * Update config valueAttributes by recommendations
	 *
	 * @param {Object} stackProperty
	 * @param {string} attr
	 * @param {Number|String|Boolean} value
	 * @protected
	 */
	_updateBoundaries: function(stackProperty, attr, value) {
		Em.set(stackProperty.valueAttributes, attr, value);
	},

	/**
	 * Get default config value
	 * <code>savedValue<code> for installed services
	 * <code>initialValue<code> for new services
	 *
	 * @param configProperty
	 * @returns {*}
	 * @protected
	 */
	_getInitialValue: function (configProperty) {
		if (!configProperty) return null;
		return this.useInitialValue(Em.get(configProperty, 'serviceName')) ?
			Em.get(configProperty, 'initialValue') : Em.get(configProperty, 'savedValue');
	},

	/**
	 * Update initial only when <code>initialValue<code> is used
	 *
	 * @param {string} serviceName
	 * @returns {boolean}
	 */
	updateInitialOnRecommendations: function(serviceName) {
		return this.useInitialValue(serviceName);
	},

	/**
	 * Defines if initialValue of config can be used on current controller
	 * if not savedValue is used instead
	 *
	 * @param {String} serviceName
	 * @return {boolean}
	 */
	useInitialValue: function (serviceName) {
		return false;
	},

	/**
	 * Defines if recommendation allowed to be applied
	 *
	 * @param parentProperties
	 * @param name
	 * @param fileName
	 * @returns {boolean}
	 */
	allowUpdateProperty: function (parentProperties, name, fileName) {
		return true;
	}
});