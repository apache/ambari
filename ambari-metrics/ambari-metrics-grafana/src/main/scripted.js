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

/* global _ */

/*
 * Complex scripted dashboard
 * This script generates a dashboard object that Grafana can load. It also takes a number of user
 * supplied URL parameters (in the ARGS variable)
 *
 * Return a dashboard object, or a function
 *
 * For async scripts, return a function, this function must take a single callback function as argument,
 * call this callback function with the dashboard object (look at scripted_async.js for an example)
 */

'use strict';

// accessible variables in this scope
var window, document, ARGS, $, jQuery, moment, kbn;

// Setup some variables
var dashboard;

// All url parameters are available via the ARGS object
var ARGS;

// Intialize a skeleton with nothing but a rows array and service object
dashboard = {
    rows : [],
};

// Set a title
dashboard.title = 'Scripted dash';

// Set default time
// time can be overriden in the url using from/to parameters, but this is
// handled automatically in grafana core during dashboard initialization


var obj = JSON.parse(ARGS.anomalies);
var metrics = obj.metrics;
var rows = metrics.length

dashboard.time = {
    from: "now-1h",
    to: "now"
};

var metricSet = new Set();

for (var i = 0; i < rows; i++) {

    var key = metrics[i].metricname;
    if (metricSet.has(key)) {
        continue;
    }
    metricSet.add(key)
    var metricKeyElements = key.split(":");
    var metricName = metricKeyElements[0];
    var appId = metricKeyElements[1];
    var hostname = metricKeyElements[2];

    dashboard.rows.push({
        title: 'Chart',
        height: '300px',
        panels: [
            {
                title: metricName,
                type: 'graph',
                span: 12,
                fill: 1,
                linewidth: 2,
                targets: [
                    {
                        "aggregator": "none",
                        "alias": metricName,
                        "app": appId,
                        "errors": {},
                        "metric": metricName,
                        "precision": "default",
                        "refId": "A",
                        "hosts": hostname
                    }
                ],
                seriesOverrides: [
                    {
                        alias: '/random/',
                        yaxis: 2,
                        fill: 0,
                        linewidth: 5
                    }
                ],
                tooltip: {
                    shared: true
                }
            }
        ]
    });
}


return dashboard;
