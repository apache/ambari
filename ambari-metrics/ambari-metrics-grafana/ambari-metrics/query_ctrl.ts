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

///<reference path="../../../headers/common.d.ts" />

import angular from 'angular';
import _ from 'lodash';
import {QueryCtrl} from "app/plugins/sdk";

export class AmbariMetricsQueryCtrl extends QueryCtrl {

    static templateUrl = 'partials/query.editor.html';
    aggregators: any;
    aggregator: any;
    errors: any;
    precisions: any;
    transforms: any;
    transform: any;
    precisionInit: any;
    suggestMetrics: any;
    suggestApps: any;
    suggestHosts: any;
    seriesAggregators: any;

    /** @ngInject **/
    constructor($scope, $injector) {
        super($scope, $injector);
        this.errors = this.validateTarget(this.target);
        this.aggregators = ['none','avg', 'sum', 'min', 'max'];
        this.precisions = ['default','seconds', 'minutes', 'hours', 'days'];
        this.transforms = ['none','diff','rate'];
        this.seriesAggregators = ['none', 'avg', 'sum', 'min', 'max'];

        if (!this.target.aggregator) {
            this.target.aggregator = 'avg';
        }
        this.precisionInit = function () {
            if (typeof this.target.precision == 'undefined') {
                this.target.precision = "default";
            }
        };
        this.transform = function () {
            if (typeof this.target.transform == 'undefined') {
                this.target.transform = "none";
            }
        };
        this.seriesAggregator = function () {
            if (typeof $scope.target.seriesAggregator == 'undefined') {
                this.target.seriesAggregator = "none";
            }
        };
        this.$watch('target.app', function (newValue) {
            if (newValue === '') {
                this.target.metric = '';
                this.target.hosts = '';
                this.target.cluster = '';
            }
        });
        if (!this.target.downsampleAggregator) {
            this.target.downsampleAggregator = 'avg';
        }

        this.datasource.getAggregators().then(function(aggs) {
            this.aggregators = aggs;
        });
    }

    targetBlur = function() {
        this.target.errors = this.validateTarget(this.target);

        // this does not work so good
        if (!_.isEqual(this.oldTarget, this.target) && _.isEmpty(this.target.errors)) {
            this.oldTarget = angular.copy(this.target);
            this.get_data();
        }
    };

    getTextValues = function(metricFindResult) {
        return _.map(metricFindResult, function(value) { return value.text; });
    };

    suggestApps = function(query, callback) {
        this.datasource.suggestApps(query)
            .then(this.getTextValues)
            .then(callback);
    };

    suggestClusters = function(query, callback) {
        this.datasource.suggestClusters(this.target.app)
            .then(this.getTextValues)
            .then(callback);
    };

    suggestHosts = function(query, callback) {
        this.datasource.suggestHosts(this.target.app, this.target.cluster)
            .then(this.getTextValues)
            .then(callback);
    };

    suggestMetrics = function(query, callback) {
        this.datasource.suggestMetrics(query, this.target.app)
            .then(this.getTextValues)
            .then(callback);
    };

    suggestTagKeys = function(query, callback) {
        this.datasource.metricFindQuery('tag_names(' + this.target.metric + ')')
            .then(this.getTextValues)
            .then(callback);
    };

    suggestTagValues = function(query, callback) {
        this.datasource.metricFindQuery('tag_values(' + this.target.metric + ',' + this.target.currentTagKey + ')')
            .then(this.getTextValues)
            .then(callback);
    };

    addTag = function() {
        if (!this.addTagMode) {
            this.addTagMode = true;
            return;
        }

        if (!this.target.tags) {
            this.target.tags = {};
        }

        this.target.errors = this.validateTarget(this.target);

        if (!this.target.errors.tags) {
            this.target.tags[this.target.currentTagKey] = this.target.currentTagValue;
            this.target.currentTagKey = '';
            this.target.currentTagValue = '';
            this.targetBlur();
        }

        this.addTagMode = false;
    };

    removeTag = function(key) {
        delete this.target.tags[key];
        this.targetBlur();
    };

    validateTarget = function(target) {
        var errs = {};

        if (target.tags && _.has(target.tags, target.currentTagKey)) {
            errs.tags = "Duplicate tag key '" + target.currentTagKey + "'.";
        }

        return errs;
    }
}
