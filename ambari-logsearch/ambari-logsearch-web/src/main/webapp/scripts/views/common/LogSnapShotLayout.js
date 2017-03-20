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

define(['require',
    'backbone',
    'utils/Utils',
    'hbs!tmpl/common/LogSnapShotLayout_tmpl',
    'moment'
], function(require, backbone, Utils, LogSnapShotLayoutTmpl, moment) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends LogSnapShotLayout */
        {
            _viewName: 'LogSnapShotLayout',

            template: LogSnapShotLayoutTmpl,


            /** ui selector cache */
            ui: {
                startStop: '[data-id="startStop"]',
                start: '[data-id="start"]',
                stop: '[data-id="stop"]',
                counter: '[data-id="counter"]',
                showAutoTriggerMsg:'[data-id="showAutoTriggerMsg"]'
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.startStop] = 'onStartStopClick';
                /*  events["click " + this.ui.start] = 'onStartClick';
                  events["click " + this.ui.stop] = 'onStopClick';*/
                return events;
            },

            /**
             * intialize a new LogSnapShotLayout Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'vent', 'globalVent', 'params'));
                this.dateUtil = Utils.dateUtil;
            },
            bindEvents: function() {},
            onRender: function() {
                this.ui.stop.hide();
                this.ui.counter.hide();
                this.ui.counter.text("00:00");
            },
            onStartStopClick: function() {
                if (this.ui.start.is(":hidden")) {
                    this.onStopClick();
                } else {
                    this.onStartClick();
                }
            },
            onStartClick: function() {
                var that = this;
                this.startDate = this.dateUtil.getMomentObject();
                clearTimeout(that.autoStartTime);
                clearTimeout(this.textAutoRefresh);
                clearTimeout(this.refreshInterval);
                that.ui.showAutoTriggerMsg.hide();
                this.ui.start.hide();
                this.ui.stop.show();
                this.ui.counter.show();
                this.$('.snapShotTitle').hide();
                this.counterValue = new Date(moment().format("MM/DD/YY"));
                this.counter = 1;
                that.ui.counter.text('00:01');
                     this.interval = setInterval(function() {
                    if (that.counterValue.getSeconds() == 0) {
                        that.counter = 1;
                    }
                    if (that.counterValue.getMinutes() > 59 && that.counterValue.getHours() >= 0) {
                        that.onStopClick();
                        return;
                    }
                    that.counterValue.setSeconds(++that.counter);
                    that.ui.counter.text(((that.counterValue.getMinutes().toString().length > 1) ?
                            (that.counterValue.getMinutes()) : ("0" + that.counterValue.getMinutes())) + ':' +
                        ((that.counterValue.getSeconds().toString().length > 1) ?
                            (that.counterValue.getSeconds()) : ("0" + that.counterValue.getSeconds())));
                }, 1000);
            },
            onStopClick: function() {
                var that = this;
                Utils.alertPopup({
                     msg:"For more accurate results, automatic refresh will be triggered in 30 secs."
                }); 
                this.refreshSecond = 30;
                this.ui.showAutoTriggerMsg.text("Triggering auto-refresh in" + " " + ('30')).show();
                this.timerStartRefresh();
                this.autoStartTime = setTimeout(function(){
                    that.vent.trigger("tab:refresh",that.params);
                    that.ui.showAutoTriggerMsg.hide();
                }, 30000);
                this.endDate = this.dateUtil.getMomentObject();
                this.vent.trigger("date:click", {
                    'from': that.startDate,
                    'to': that.endDate,
                    dateRangeLabel: "Custom Range",
                });
                this.ui.stop.hide();
                this.ui.start.show();
                this.ui.counter.hide();
                this.$('.snapShotTitle').show();
                this.ui.counter.text("00:00");
                this.counterValue = new Date(moment().format("MM/DD/YY"));
                this.counter = 1;
                clearInterval(this.interval);
            },
            timerStartRefresh: function(){
                var that= this;
                var refreshCounter = new Date(moment().format("MM/DD/YY"));
                refreshCounter.setSeconds(30);
                this.refreshInterval = setInterval(function() {
                    var getCounterValue = refreshCounter.getSeconds();
                    refreshCounter.setSeconds(--getCounterValue);
                    that.ui.showAutoTriggerMsg.text("Triggering auto-refresh in" + " " + refreshCounter.getSeconds());
                }, 1000);
            }
        });
});
