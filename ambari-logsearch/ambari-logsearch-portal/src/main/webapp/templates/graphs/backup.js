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
    'moment',
    'd3.tip',
    'utils/Globals',
    'utils/Utils',
    'collections/VLogList',
    'hbs!tmpl/graphs/HistogramGraphLayoutView_tmpl',
    'bootstrap-daterangepicker',
    'nv'
],function(require,backbone,moment,tip,Globals,Utils,VLogList,HistogramGraphLayoutViewTmpl,daterangepicker,nv) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends HistogramGraphLayoutViewTmpl */
        {
            _viewName: 'HistogramGraphLayoutView',

            template: HistogramGraphLayoutViewTmpl,


            /** ui selector cache */
            ui: {
                histoGraph: "div[data-id='rHistogramGraph']",
                dateRange: "#dateRange",
                selectDateRange: ".selectDateRange",
                dateRangeTitle: "span[data-id='dateRangeTitle']",
                graphHeader: "div[data-id='graphHeader']"

            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.viewType] = 'onViewTypeChange';
                events['click [data-id="refresh-tab-graph"]'] = 'onTabRefresh';
                return events;
            },

            /**
             * intialize a new LogLevelView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'vent', 'globalVent', 'params', 'viewType'));
                this.collection = new VLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 999999999,

                    }
                });

                this.dateUtil = Utils.dateUtil;
                this.dateRangeLabel = new String();

                this.bindEvents();
                this.graphParams = {};
                this.unit = this.params.unit ? this.params.unit : "+1HOUR";
                this.firstRender = true;
                if (!this.viewType || this.viewType == Globals.graphType.HISTOGRAM.value) {
                    this.histogramView = true;
                    this.collection.url = Globals.baseURL + "service/logs/histogram";
                    this.collection.modelAttrName = "graphData";
                } else {
                    this.collection.url = Globals.baseURL + "audit/logs/linegraph";
                    this.collection.modelAttrName = "graphData";
                    this.lineView = true;
                }


            },
            bindEvents: function() {
                this.listenTo(this.collection, "reset", function(collection) {
                    this.createDataForGraph();
                    this.$(".loader").hide();
                    this.$("#loaderGraph").hide();
                }, this);
                this.listenTo(this.collection, 'request', function() {
                    this.$(".loader").show();
                    this.$("#loaderGraph").show();
                }, this);
                this.listenTo(this.collection, 'sync error', function() {
                    this.$(".loader").hide();
                    this.$("#loaderGraph").hide();
                }, this);
                this.listenTo(this.vent, "main:search tree:search type:mustNot type:mustBe level:filter search:include:exclude " +
                    "logtime:filter " + Globals.eventName.serviceLogsIncludeColumns + " " + Globals.eventName.serviceLogsExcludeColumns,
                    function(value) {
                        this.fetchGraphData(value);
                    }, this);
//                this.listenTo(this.vent, "tree:search", function(value) {
//                    this.fetchGraphData(value);
//                }, this);
//                this.listenTo(this.vent, "level:filter ", function(value) {
//                    this.fetchGraphData(value);
//                }, this);
//                this.listenTo(this.vent, "type:mustNot", function(value) {
//                    this.fetchGraphData(value);
//                }, this);
//                this.listenTo(this.vent, "type:mustBe", function(value) {
//                    this.fetchGraphData(value);
//                }, this);
//                this.listenTo(this.vent, "logtime:filter", function(value) {
//                    this.fetchGraphData(value);
//                }, this);
//                this.listenTo(this.vent, "search:include:exclude", function(value) {
//                    this.fetchGraphData(value);
//                }, this);
                this.listenTo(this.vent, "reinitialize:filter:logtime", function(value) {
                    this.unit = value.unit;
                    this.reinitializeFilter(value);
                }, this);
                this.listenTo(this.globalVent, "globalExclusion:component:message", function(value) {
                    this.fetchGraphData(value);
                }, this);
                this.listenTo(this.vent, "tab:refresh", function(params) {
                    this.reRenderView(params);
                }, this);
                /*
                 * Audit events
                 */
                this.listenTo(this.vent, "search:audit:query auditComponents:include auditComponents:exclude search:audit:include search:audit:exclude", function(value) {
                    this.fetchGraphData(value);
                }, this);

            },
            onRender: function() {
                var that = this;
                if (this.histogramView) {
                    this.ui.graphHeader.html('<i class="fa fa-signal"></i><span >Histogram</span>');
                } else {
                    this.ui.graphHeader.html('<i class="fa fa-line-chart"></i><span >Line</span>');
                }
                if (!this.params.dateRangeLabel) this.params['dateRangeLabel'] = "Today"
                this.initializeDateRangePicker();
                this.setValues(this.params);
                this.unit = that.checkDateRange(that.ui.dateRange.data("daterangepicker"));
                this.fetchGraphData(_.extend({
                    q: "*:*"
                }, this.params, {
                    unit: this.unit
                }));

                if (this.lineView) {
                    this.ui.histoGraph.addClass('myLineChart');
                }
                this.$("svg").on("mouseover",function(){
                	if($(this).find(".nv-noData").length){
                		that.$(".nvtooltip.xy-tooltip").hide();
                	}else
                		that.$(".nvtooltip.xy-tooltip").show();
                })
            },
            setValues: function(val) {
                var startDate, endDate;
                if (val.from) {
                    startDate = this.dateUtil.getMomentObject(val.from)
                    this.ui.dateRange.data('daterangepicker').setStartDate(startDate);
                }
                if (val.to) {
                    endDate = this.dateUtil.getMomentObject(val.to)
                    this.ui.dateRange.data('daterangepicker').setEndDate(endDate);
                }
                if (startDate && endDate)
                    this.setDateText(startDate, endDate);
                if (val.dateRangeLabel) this.ui.dateRangeTitle.html(val.dateRangeLabel);
                this.dateRangeLabel = val.dateRangeLabel;
            },
            initializeDateRangePicker: function() {
                var that = this,
                    ranges = {};
                //Apply moments for all ranges separately if you pass single instance then it will run into problem.
                _.each(Utils.relativeDates, function(k) {
                    ranges[k.text] = [];
                })
                this.ui.dateRange.daterangepicker(_.extend({
                    'ranges': ranges
                }, {
                    /*ranges: {
                        'Last 1 Hour': [],
                        'Today': [],
                        'Yesterday': [],
                        'Last 7 Days': that.dateUtil.getLast7DaysRange(),
                        'Last 30 Days': that.dateUtil.getLast30DaysRange(),
                        'This Month': that.dateUtil.getThisMonthRange(),
                        'Last Month': that.dateUtil.getLastMonthRange()
                    },*/
                    "timePicker": true,
                    "timePickerIncrement": 1,
                    "timePicker24Hour": true,
                    timePickerSeconds: true,
                    showWeekNumbers: true,
                    timeZone: 0,
                    locale: {
                        format: 'MM/DD/YYYY H:mm:ss,SSS'
                    },
                    parentEl: this.$el,
                }));
                this.bindDateRangePicker();
            },
            bindDateRangePicker: function() {
                var that = this;

                var elem = this.$('.daterangepicker');

                this.ui.dateRange.on('apply.daterangepicker ', function(ev, picker) {
                    that.ui.dateRangeTitle.html(that.dateRangeLabel);
                    that.unit = that.checkDateRange(picker);
                    var options = {
                        'from': (picker.startDate).toJSON(),
                        'to': (picker.endDate).toJSON(),
                        'unit': that.unit,
                        'dateRangeLabel': that.dateRangeLabel
                    }
                    that.vent.trigger("logtime:filter", options);
                    that.pickerOpend = false
                });
                this.ui.dateRange.on('show.daterangepicker', function(ev, picker) {
                    elem.find('li').removeClass('active');
                    elem.find('li:contains(' + that.dateRangeLabel + ')').addClass('active')
                });
                this.ui.dateRange.on('hide.daterangepicker', function(ev, picker) {
                    that.pickerOpend = true
                });

                this.ui.selectDateRange.on("click", 'button.goBtn', function() {
                    if (that.pickerOpend) {
                        var textRange = elem.find('li.active').text();
                        if (textRange == "Custom Range") that.dateRangeLabel = elem.find('li.active').text();
                    }
                    if (that.dateRangeLabel == "Last 1 Hour") {
                        var last1Hour = that.dateUtil.getRelativeDateFromString(that.dateRangeLabel);
                        that.setDateText(last1Hour[0], last1Hour[1]);
                    }
                    that.ui.dateRange.data('daterangepicker').clickApply();

                });

                elem.on("click", '.ranges ul li', function(e) {
                    that.dateRangeLabel = $(this).text();
                    if (that.dateRangeLabel !== "Custom Range") {
                        var last1Hour = that.dateUtil.getRelativeDateFromString(that.dateRangeLabel);
                        that.setDateText(last1Hour[0], last1Hour[1]);
                        that.ui.dateRange.data('daterangepicker').clickApply();
                    }
                });
            },
            checkDateRange: function(picker) {
                return this.dateUtil.calculateUnit(picker)
            },
            fetchGraphData: function(params) {
                var that = this;
                that.$("#loaderGraph").show();
                that.$(".loader").show();
                _.extend(this.collection.queryParams, params);
                this.collection.fetch({
                    reset: true
                });
            },
            createDataForGraph: function() {
                var data = [],
                    that = this,
                    dataL = [],
                    color = d3.scale.category20().range();
                _.each(this.collection.models, function(model, i) {
                    var Obj = {
                        key: model.get('name'),
                        values: model.get('dataCount').map(function(object) {
                            return {
                                x: that.dateUtil.getMomentObject(object.name), //(new Date(object.name)).toUTCString(),
                                y: parseFloat(object.value)
                            }
                        })
                    };
                    if (that.histogramView) {
                        Obj['color'] = ((model.get('name') === 'ERROR') ? ("#E81D1D") :
                            (model.get('name') === 'INFO') ? ("#2577B5") :
                            (model.get('name') === 'WARN') ? ("#FF8916") :
                            (model.get('name') === 'FATAL') ? ("#830A0A") :
                            (model.get('name') === 'DEBUG') ? ("#65E8FF") :
                            (model.get('name') === 'TRACE') ? ("#888888") : ("white"));
                    } else {
                        Obj['color'] = color[i];
                    }
                    data.push(Obj);
                })
                if (that.histogramView) {
                    for (var i = data.length - 1; i >= 0; i--) {
                        dataL.push(data[i])

                    }
                }

                this.$('svg').find('g.nv-x  g.tick').hide();
                (!this.firstRender) ? this.updateGraph(data, dataL): this.renderGraph(data, dataL);

            },
            //Using NVD3
            renderGraph: function(data, dataL) {
                var that = this,
                    formatValue = d3.format(".2s");
                // that.ui.histoGraph.find('svg').empty();
                nv.addGraph({
                    generate: function() {
                        /* var parentWidth = (that.ui.histoGraph.find('svg').parent().width()),
                             parentHeight = (that.ui.histoGraph.find('svg').parent().height())
                             width = ((parentWidth === 0) ? (891) : (parentWidth)), // -15 because  parent has 15 padding
                              height = ((parentHeight === 0) ? (640) : (parentHeight)) // -15 because  parent has 15 padding */
                        if (that.histogramView) {
                            that.chart = nv.models.multiBarChart()
                                /* .width(width)
                                 .height(height)*/
                                .stacked(true)
                                .showControls(false);
                            that.chart.groupSpacing(0.6) // for bar width and aspace
                        } else {
                            that.chart = nv.models.lineChart().options({
                                transitionDuration: 300,
                                useInteractiveGuideline: true
                            });
                            //We want nice looking tooltips and a guideline!

                            that.chart.showLegend(true); //Show the legend, allowing users to turn on/off line series.
                            that.chart.showYAxis(true); //Show the y-axis
                            that.chart.showXAxis(true);

                        }
                        that.ExtentValue = that.chart.xAxis
                            .tickFormat(function(d) {
                                var date = that.dateUtil.getTimeZoneFromMomentObject(((that.histogramView) ? (d) : (that.dateUtil.getMomentObject(d))));
                                return (((that.unit.search('HOUR') + 1) || (that.unit.search('MINUTE') + 1)) ?
                                    (d3.time.format('%H:%M - %m/%d/%y')(date)) :
                                    (((that.unit.search('MILLI') + 1)) ? (d3.time.format('%H:%M:%S.%L - %m/%d/%y')(date)) :
                                        (((that.unit.search('MONTH') + 1)) ? ((d3.time.format('%b %d')(date))) :
                                            (((that.unit.search('SECOND') + 1)) ? (d3.time.format('%H:%M:%S - %m/%d/%y')(date)) : (d3.time.format('%m/%d/%y')(date))))))
                            });
                        that.chart.yAxis
                            .tickFormat(function(d) {
                                return formatValue(d).replace('G', 'B');
                            });
                        // For legend and control margin
                        that.chart.legend.margin({
                            top: 5,
                            right: 50,
                            left: 90,
                            bottom: 10
                        });
                        that.chart.margin({
                            right: 30,
                            left: 30,
                        });
                        that.chart.dispatch.on('renderEnd', function() {
                            if (that.firstRender) {
                                that.firstRender = false
                                var svgElem = that.$('svg');
                                if (that.histogramView) {
                                    var elem = svgElem.find('g.nv-barsWrap.nvd3-svg .nv-groups');
                                } else {
                                    var elem = svgElem.find('g.nv-linesWrap.nvd3-svg .nv-groups');
                                }

                                that.svgHeight = elem.get(0).getBBox().height;
                                that.svgWidth = elem.get(0).getBBox().width;

                            }
                            that.graphXAxisBreak();
                            that.createBrush();
                        });

                        that.chart.tooltip.contentGenerator(
                            function(data) {
                                if (data.data.size <= 0)
                                    return "<div></div>";
                                var tootTipTemplate = '<div>' +
                                    '<table>' +
                                    '<thead>' +
                                    '<tr>' +
                                    '<td colspan="3"><strong class="x-value">' + data[((that.histogramView) ? ('data') : ('point'))].x.format('MM/DD/YYYY') + '</strong></td>' +
                                    '</tr>' +
                                    '<tr>' +
                                    '<td colspan="3"><strong class="x-value">' + data[((that.histogramView) ? ('data') : ('point'))].x.format('H:mm:ss,SSS') + '</strong></td>' +
                                    '</tr>' +
                                    '</thead>' +
                                    '<tbody>' +
                                    '<tr>' +
                                    '<td class="legend-color-guide">' +
                                    '<div style="background-color: ' + ((that.histogramView) ? (data.color) : (data.series[0].color)) + '"></div>' +
                                    '</td>' +
                                    '<td class="key">' + ((that.histogramView) ? (data.data.key) : (data.series[0].key)) + '</td>' +
                                    '<td class="value">' + ((that.histogramView) ? (data.data.size) : (data.series[0].value)) + '</td>' +
                                    '</tr>' +
                                    '</tbody>' +
                                    '</table>' +
                                    '</div>'

                                return tootTipTemplate
                            });


                        that.chart.legend.dispatch.legendClick = function(d, i) {
                            that.$('svg').find('g.nv-x  g.tick').hide();
                        };
                        /*that.chart.multibar.dispatch.elementDblClick = function(e) {
                            console.log(e)
                        }*/
                        var elem = that.$('svg');
                        that.svg = d3.select(elem[0]).datum(data);
                        that.svg.transition().duration(0).call(that.chart);
                        if (dataL.length > 0) {
                            that.svg.datum(dataL).call(that.chart.legend);
                            var legendObject = (that.$('svg').children()[0]);
                            if (that.$(legendObject).is('g[class="nvd3 nv-legend"]')) {
                                that.$(legendObject).remove();
                            } else if (that.$(legendObject).is('text[class="nvd3 nv-noData"]')) {
                                that.$(that.$('svg').children()[1]).remove();
                            }

                        }



                        return that.chart;
                    },
                    callback: function(graph) {
                        that.$(".box").resize(function() {
                            //                             var parentWidth = (that.ui.histoGraph.find('svg').parent().width()),
                            //                                 parentHeight = (that.ui.histoGraph.find('svg').parent().height()),
                            //                                 width = ((parentWidth === 0) ? (891) : (parentWidth)), // -15 because  parent has 15 padding
                            //                                 height = ((parentHeight === 0) ? (240) : (parentHeight)) // -15 because  parent has 15 padding
                            //                             graph.width(width).height(height);
                            //                          if (that.parentView) {
                            //                                  var elem = that.parentView.$el.find('[data-id="rHistogramGraph"] svg');
                            //                              } else {
                            //                                  var elem = that.$('[data-id=rHistogramGraph] svg');
                            //                              }
                            var elem = that.$('[data-id=rHistogramGraph] svg');
                            d3.select(elem[0])
                                //                                 .attr('width', width)
                                //                                 .attr('height', height)
                                .transition().duration(0)
                                .call(graph);
                        });
                    }
                });

            },
            updateGraph: function(data, dataL) {

                // Update the SVG with the new data and call chart
                this.svg.datum(data).transition().duration(0).call(this.chart);
                if (dataL.length > 0) {
                    this.svg.datum(dataL).transition().duration(0).call(this.chart.legend);
                    if (d3.select(this.$('svg').children().first()[0]).classed('nvd3 nv-legend')) {
                        d3.select(this.$('svg').children().first()[0]).remove();
                    }
                    var legendObject = (this.$('svg').children()[0]);
                    if (this.$(legendObject).is('g[class="nvd3 nv-legend"]')) {
                        this.$(legendObject).remove();
                    } else if (this.$(legendObject).is('text[class="nvd3 nv-noData"]')) {
                        this.$(this.$('svg').children()[1]).remove();
                    }
                }

            },
            createBrush: function() {
                var that = this;
                var svgElem = this.$('svg');

                if (that.histogramView) {
                    var elem = svgElem.find('g.nv-barsWrap.nvd3-svg .nv-groups');
                } else {
                    var elem = svgElem.find('g.nv-linesWrap.nvd3-svg .nv-groups');
                }
                if (elem.length == 0 || elem.length == undefined) {
                    return;
                }
                elem.find('.x-brush').parent().remove();

                var height = (elem.get(0).getBBox().height === 0) ? that.svgHeight : elem.get(0).getBBox().height + 2,
                    width = (elem.get(0).getBBox().width === 0) ? that.svgWidth : elem.get(0).getBBox().width

                var x = d3.time.scale().range([0, width]).domain(d3.extent(that.ExtentValue.domain().map(function(d) {
                    return that.dateUtil.getMomentObject(d);
                })));
                var brush = d3.svg.brush()
                    .x(x)
                    .on("brush", brushed)
                    .on('brushend', brushend);

                if (that.histogramView) {
                    var brushElem = svgElem.find('g.nv-barsWrap.nvd3-svg .nv-groups');
                } else {
                    var brushElem = svgElem.find('g.nv-linesWrap.nvd3-svg .nv-groups');
                }
                var svg = d3.select(brushElem[0])
                    .insert("g", ":first-child")
                    .attr("transform", "translate(" + elem.get(0).getBBox().x + ",-9)");


                svg.append("g")
                    .attr("class", "x-brush brush")
                    .call(brush)
                    .selectAll("rect")
                    .attr("y", 6)
                    .attr("height", height);

                function brushed() {
                    that.brushValue = brush.extent();
                    that.setDateText(that.dateUtil.getMomentObject(that.brushValue[0]), that.dateUtil.getMomentObject(that.brushValue[1]));
                }

                function brushend() {

                    if (moment(brush.extent()[0]).isSame(brush.extent()[1])) {
                        that.createBrush();
                        return;
                    }
                    $('.nvtooltip').css('opacity', '0');
                    that.$('g.x-brush').parent().remove()
                    that.dateRangeLabel = "Custom Range";
                    that.lastUnit = that.unit;
                    that.ui.dateRange.data('daterangepicker').clickApply();

                }

            },
            graphXAxisBreak: function() {
                var insertLinebreaks = function() {
                    var el = d3.select(this);
                    var words = $(this).text().split('-');
                    $(this).text('');
                    for (var i = 0; i < words.length; i++) {
                        var tspan = el.append('tspan').text(words[i]);
                        if (i > 0)
                            tspan.attr('x', 0).attr('dy', '19');
                    }
                };
                if (this.$('svg').find('g.nv-x  g.tick text').first().text().split('-').length > 1) {
                    this.$('svg').find('g.nv-x  g.tick text').each(insertLinebreaks);
                    this.$('svg').find('g.nv-x  g.nv-axisMaxMin text').each(insertLinebreaks);
                    this.$('svg').find('g.nv-x  g.tick').show();
                } else {
                    this.$('svg').find('g.nv-x  g.tick').show();
                }
            },
            setDateText: function(start, end) {

                this.ui.dateRange.val(this.dateUtil.getTimeZone(start, "MM/DD/YYYY H:mm:ss,SSS") + ' - ' + this.dateUtil.getTimeZone(end, "MM/DD/YYYY H:mm:ss,SSS"));
                this.ui.dateRange.data('daterangepicker').setStartDate(start);
                this.ui.dateRange.data('daterangepicker').setEndDate(end);

            },
            reinitializeFilter: function(value) {
                this.fetchGraphData(value);
                this.setValues(value);
            },
            onTabRefresh: function(e) {
                this.fetchGraphData({});
            },
            reRenderView: function(params) {
                this.fetchGraphData(params);
                this.setValues(_.extend(this.params, params));
            }
        });


});
