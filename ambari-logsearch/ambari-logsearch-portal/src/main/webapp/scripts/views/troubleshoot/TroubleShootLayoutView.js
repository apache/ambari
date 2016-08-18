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

define(['backbone',
    'utils/Utils',
    'utils/ViewUtils',
    'utils/Globals',
    'hbs!tmpl/troubleshoot/TroubleShootLayoutView_tmpl',
    'collections/VLogList',
    'collections/VNameValueList',
    'nv'
], function(Backbone, Utils, ViewUtils, Globals, TroubleShootLayoutView_Tmpl, VLogList, VNameValueList, nv) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends TroubleShootLayoutView */
        {
            _viewName: 'TroubleShootLayoutView',

            template: TroubleShootLayoutView_Tmpl,
            
            className : "clearfix",

            /** ui selector cache */
            ui: {
            	serviceContainer : ".services",
            	componentsContainer : "[data-id='components']",
            	logLevelTable : "[data-id='logLevelTable']",
            	components : "[data-id='componentsSelection']",
            	dependencyCont : ".dependContainer",
            	loader : '[data-id="loader"]'
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click .services button'] = 'onServicesChange';
                events['click [data-id="searchServiceLogs"]'] = 'onSearchServiceLogsClick';
                events['click [data-id="searchAuditLogs"]'] = 'onSearchAuditLogsClick';
                events['click .depLinks'] = 'onDependentServiceClick';
                events['click .expand-collapse'] = 'onExpandCollapseSections';
                return events;
            },
            regions: {
            	RDateRangePicker : "[data-id='dateRange']",
            	RServiceGraph : "[data-id='serviceGraph']"
            },

            /**
             * intialize a new TroubleShootLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params'));
                this.vent = new Backbone.Wreqr.EventAggregator();
                //this.servicesData = {services:{ranger:{label:"Ranger",components:[{name:"ranger_admin"}],dealsWithServices:[{name:"hdfs"},{name:"kms"}],dealsWithComponents:[{name:"security_admin"},{name:"portal"}],},ambari:{label:"Ambari",dealsWithServices:[{name:"ranger"},{name:"hive"}]},hdfs:{label:"Hdfs",components:[{name:"hdfs_namenode"},{name:"hdfs_datanode"}],dealsWithServices:[],dealsWithComponents:[],}}};
                var todayRange = Utils.dateUtil.getTodayRange();
                this.params = _.pick(ViewUtils.getDefaultParamsForHierarchy(),"dateRangeLabel","from","to","bundle_id","host_name","component_name","file_name");
                this.initializeCollection();
                this.bindEvents();
            },
            initializeCollection : function(){
            	this.serviceLogsCollection = new VLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 999999999,

                    }
                });
                this.serviceLogsCollection.url = Globals.baseURL + "dashboard/getAnyGraphData";
                this.serviceLogsCollection.modelAttrName = "graphData";
                
            	this.topUsers = new VNameValueList([],{
    				state: {
                        firstPage: 0,
                        pageSize: 9999
                    }
    			});
    			this.topUsers.url = Globals.baseURL + "audit/getTopAuditUsers";
    			this.topUsers.modelAttrName = "graphData";
    			
    			this.serviceLoadCollection = new VLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 999999999,

                    }
                });
            	this.serviceLoadCollection.url = Globals.baseURL + "audit/getServiceLoad";
                this.serviceLoadCollection.modelAttrName = "graphData";
            },
            bindEvents : function(){
            	this.listenTo(this.serviceLogsCollection,"reset",function(){
            		this.renderBarGraph();
            		this.renderLogLevelTable();
            	},this);
            	this.listenTo(this.serviceLogsCollection, 'request', function() {
            		this.ui.loader.addClass("loading");
                }, this);
                this.listenTo(this.serviceLogsCollection, 'sync error', function() {
                	this.ui.loader.removeClass("loading");
                }, this);
            	this.listenTo(this.topUsers,"reset",function(){
    				this.renderTopTenUsers();
    			},this);
            	this.listenTo(this.serviceLoadCollection,"reset",function(){
    				this.renderServiceLoadGraph();
    			},this);
            	
            	//datepicker fires logtime filter when date selection is made
            	this.listenTo(this.vent,"logtime:filter",function(params){
            		//this.fetchServiceLogsData(params);
            		//this.vent.trigger("graph:data:update",params,this.graphPropModel.attributes);
            		//this.renderGraph(params);
            		this.fetchLevelCollection(params);
            		this.fetchTopUsers(params);
            	},this);
            },
            onRender : function(){
            	var that = this;
            	this.fetchTopUsers(this.params);
            	this.serviceLogsCollection.getServicesInfo({
                	success : function(resp){
                		Globals.servicesInfo = resp;
                		that.servicesData = $.extend(true,resp);
                		that.renderServices(that.servicesData);
                	}
                });
            	this.renderDateRange();
            	//this.renderGraph(this.params);
            	this.fetchLevelCollection(this.params);
            },
            fetchTopUsers : function(params){
    			var that = this;
    			$.extend(this.topUsers.queryParams, params);
    			this.topUsers.fetch({
    				reset:true,
    				beforeSend : function(){
    					that.$("[data-id='usersLoader']").removeClass("hidden");
    				},
    				complete : function(){
    					that.$("[data-id='usersLoader']").addClass("hidden");
    				}
    			});
    			
    			$.extend(this.serviceLoadCollection.queryParams, params);
    			this.serviceLoadCollection.fetch({
    				reset:true,
    				beforeSend : function(){
    					that.$("[data-id='serviceLoadLoader']").removeClass("hidden");
    				},
    				complete : function(){
    					that.$("[data-id='serviceLoadLoader']").addClass("hidden");
    				}
    			});
    			
    		},
            fetchServiceLogsData : function(params){
            	$.extend(this.serviceLogsCollection.queryParams,params);
            	this.serviceLogsCollection.fetch({reset:true});
            },
            onServicesChange : function(e){
            	var that=this,selectedComponents=[],$el = $(e.currentTarget);
            	$el.siblings().removeClass("active");
            	$el.toggleClass("active");
            	this.ui.dependencyCont.empty();
            	this.ui.componentsContainer.empty();
            	_.each(this.ui.serviceContainer.find(".active"),function(el){
            		var $el = $(el);
            		if($el.data("name")){
                		var service = that.servicesData.service[$el.data("name")];
                		if(service){
                			if(_.isArray(service.components)){
//                				_.each(service.components,function(obj,key){
//                					that.ui.componentsContainer.append("<option selected>"+obj.name+"</option>");
//                				});
                				Array.prototype.push.apply(selectedComponents,_.pluck(service.components,"name"));
                			}
                		}
                	}
            	});
            	if($el.hasClass("active")){
            		var service = $el.data("name"),optionalComponents=[];
            		if(service){
            			_.each(that.servicesData.service[service].dependencies, function(d){
                			if(! d.required){
                				for(var z=0; z<d.components.length; z++){
                					selectedComponents.push(d.components[z]);
                				}
                			}else{
                				optionalComponents.push({label:that.servicesData.service[d.service].label,value : d.service});
                			}
                		});
            		}
            		if(optionalComponents.length){
            			for(var i=0; i<optionalComponents.length; i++){
                			this.ui.dependencyCont.append('<button data-service="'+optionalComponents[i].value+'" class="btn depLinks">'+optionalComponents[i].label+'</button>');
                		}
            			if(this.$(".dependencies").is(":hidden")){
                			this.$(".dependencies").slideDown();
                		}
            		}else{
            			this.$(".dependencies").slideUp();
            		}
            	}
            	this.ui.components.select2("val",selectedComponents);
            	var params = this.getParams();
            	//this.renderGraph(params);
            	//this.fetchServiceLogsData(params);
            	//this.fetchTopUsers(params);
            	this.fetchLevelCollection(params);
            },
            renderServices : function(data){
            	var that = this;
            	//that.ui.serviceContainer.append('<label class="btn btn-primary"> <input data-name="All" type="checkbox" name="services" id="option1">All</label>');
            	that.ui.serviceContainer.append('<button class="btn btn-trbl" href="javascript:void(0);">All</button>');
            	_.each(data.service,function(obj,key){
            		/*var html = '<label class="btn btn-primary"> <input data-name="'+key+'" type="checkbox" name="services" '+
    				' id="option1" />'+obj.label+'</label>';*/
            		var html = '<button class="btn btn-trbl" data-name="'+key+'" href="javascript:void(0);">'+obj.label+'</button>';
            		that.ui.serviceContainer.append(html);
            	});
            	var compos = _.pluck(data.service,"components");
            	var mainArr = [];
            	_.each(compos,function(v){
            		for(var z=0; z<v.length; z++){
            			if(_.isEmpty(_.findWhere(mainArr,{name:v[z].name}))){
            				mainArr.push({name:v[z].name, id:v[z].name});
            			}
            		}
            	});
            	this.ui.components.select2({
    				placeholder: 'Components',
    				tags:true,
    				allowClear : true,
    				width: '100%',
    				data: { results: mainArr, text: "name"},
    				formatSelection: function(item){
    					return item["name"];
    				},
        			formatResult: function(item){
        				return item["name"];
    				}
    			}).on("change",function(e){
    				var params = that.getParams();
    				//that.renderGraph(params);
                	//that.fetchServiceLogsData(params);
                	//that.fetchTopUsers(params);
    				that.fetchLevelCollection(params);
    			});
            },
            renderDateRange : function(){
            	var that=this;
            	require(['views/common/DatePickerLayout'],function(DatePickerLayout){
    	    		that.RDateRangePicker.show(new DatePickerLayout({
    					vent : that.vent,
    					globalVent:that.globalVent,
    					params : that.params,
    					rangeLabel: true,
    					hideFireButton : false,
    					buttonLabel : "Apply",
    					parentEl: that.$el.find(".row").first(),
    					datePickerPosition : "left"
    				}));
                });
            },
            onSearchServiceLogsClick : function(e){
            	this.globalVent.trigger("reinitialize:serviceLogs",_.extend({
            		mustNot: null,
                mustBe: null,
                iMessage: null,
                eMessage: null,
                query: null,
                includeQuery: null,
                excludeQuery: null
            	},this.getParams()));
            	this.globalVent.trigger("show:tab","hierarchy");
            },
            onSearchAuditLogsClick : function(){
            	this.globalVent.trigger("reinitialize:auditLogs",this.getParams());
            	this.globalVent.trigger("show:tab","audit");
            },
            getParams : function(){
            	var all = this.$("button[data-name='all']").hasClass("active"),that = this,params={},dates={};;
            	this.vent.trigger("date:getValues",dates);
            	if(_.isArray(dates.dates)){
            		params.from = dates.dates[0].toJSON();
            		params.to = dates.dates[1].toJSON();
            		params.dateRangeLabel = dates.dateRangeLabel;
            		params.unit = dates.unit;
            	}
            	//components
            	if(!all){
//            		_.each(this.ui.serviceContainer.find(".active"),function(el){
//                		var serviceName = $(el).data("name");
//                		var service = that.servicesData.service[serviceName];
//                		if(service && _.isArray(service.components)){
//                			for(var z=0; z<service.components.length; z++){
//                				(! _.contains(params.mustBe,service.components[z].name)) ? params.mustBe.push(service.components[z].name) : "";
//                			}
//                		}
//                	});
            		params.mustBe = this.ui.components.select2("val").toString();
            	}
            	return params;
            },
            fetchLevelCollection : function(params){
            	_.extend(this.serviceLogsCollection.queryParams, params,{"yAxis":"count",
        			"xAxis":"level"});
                this.serviceLogsCollection.fetch({
                    reset: true
                });
            },
            renderGraph : function(params){
            	//var that=this,model = new Backbone.Model({"id":"grid_histo0","title":"test","showX":"showX","xAxis":"access","xTimeFormat":"","xNormalFormat":"","showY":"showY","yAxis":"count","yAxisFormat":"","showLegend":"showLegend","stackOrGroup":"Normal","params":{"from":"2016-03-08T18:30:01.000Z","to":"2016-03-09T18:29:59.999Z","unit":"+1HOUR","yAxis":"count","xAxis":"access"},"myData":{"type":2,"dataId":"grid_histo0"},"col":1,"row":1,"size_x":3,"size_y":2});
            	var that=this,model = new Backbone.Model({
            		params: _.extend({
            			"yAxis":"count",
            			"xAxis":"level",
            			"stackBy":"level"},params),
            		xAxis : "level",
            		yaxis : "count",
            		//stackOrGroup : "Stack",
            		showX:"showX",
            		//rotateXaxis:"-20",
            		showLegend : false
            	});
            	require(['views/graphs/GridGraphLayoutView'],function(GridGraphLayoutView){
    	    		that.RServiceGraph.show(new GridGraphLayoutView({
    	    			collection : that.serviceLogsCollection,
    					vent : that.vent,
    					globalVent:that.globalVent,
    					params : that.params,
    					model : model,
    					viewType :Globals.graphType.HISTOGRAM.value,
    					showHeader : false
    				}));
                });
            },
            renderBarGraph : function(){
            	var data=[],that=this;
            	this.serviceLogsCollection.each(function(model){
            		var d = {
            				key : "Levels",
            				values : []
            		}
            		for(var z=0; z<model.get("dataCount").length; z++){
            			var name = model.get("dataCount")[z].name;
            			d.values.push({
            				label : (""+name).toUpperCase(),
            				value : parseInt(model.get("dataCount")[z].value,10),
            				color : (((""+name).toUpperCase() === 'ERROR') ? ("#E81D1D") :
                                ( (""+name).toUpperCase() === 'INFO') ? ("#2577B5") :
                                ( (""+name).toUpperCase() === 'WARN') ? ("#FF8916") :
                                ( (""+name).toUpperCase() === 'FATAL') ? ("#830A0A") :
                                ( (""+name).toUpperCase() === 'DEBUG') ? ("#65E8FF") :
                                ( (""+name).toUpperCase() === 'TRACE') ? ("#888888") : "")
            			});
            			
            		}
            		data.push(d);
            	});
                nv.addGraph(function() {
                    var chart = nv.models.discreteBarChart()
                        .x(function(d) {
                            return d.label })
                        .y(function(d) {
                            return d.value })
                        .staggerLabels(false)
                        .width(700)
                        .showValues(true)
                    chart.tooltip.enabled(false);
                    chart.yAxis
                        .tickFormat(d3.format('d'));
                    chart.valueFormat(d3.format('d'));

                    chart.margin({
                        right: 100,
                        left: 120,
                    });
                    d3.select(that.$("[data-id='serviceGraph'] svg")[0])
                        .datum(data)
                        .transition().duration(500)
                        .call(chart);
                    return chart;
                });
            },
            renderLogLevelTable : function(){
            	var that = this;
            	this.ui.logLevelTable.empty();
            	this.serviceLogsCollection.each(function(m){
            		var dataCount = m.get("dataCount");
            		for(var z=0; z<dataCount.length; z++){
            			if(that.ui.logLevelTable.find("."+Utils.toUpperCase(dataCount[z].name)).length){
            				var $el = that.ui.logLevelTable.find("[data-level="+dataCount[z].name+"]");
            				var val = $el.text();
            				$el.html("<b>"+(parseInt(val,10) + parseInt(dataCount[z].value,10))+"</b>");
            			}else{
            				that.ui.logLevelTable.append("<tr><td align='center' class='"+Utils.toUpperCase(dataCount[z].name)+"'>"+Utils.toUpperCase(dataCount[z].name)+"</td><td align='right' data-level='"+dataCount[z].name+"'><b>"+dataCount[z].value+"</b></td></tr>");
            			}
            		}
            	});
            },
            renderTopTenUsers : function(){
            	var obj = ViewUtils.formatAuditGraphData(this.topUsers);
            	this.renderHorizontalBar(this.$('[data-id="topUsersGraph"] svg')[0],obj.arr, {top: 5,right:10, bottom: 20,left:(obj.max * 7)+25});
            },
            renderServiceLoadGraph : function(){
            	var obj = ViewUtils.formatAuditGraphData(this.serviceLoadCollection);
            	this.renderHorizontalBar(this.$('[data-id="serviceLoadGraph"] svg')[0],obj.arr, {top: 5,right:10, bottom: 20,left:(obj.max * 7)+25});
            },
            renderHorizontalBar : function(el,data,margin,columnKey){
    			var that = this;
    			
    			nv.addGraph({generate : function() {
    				  var chart = nv.models.multiBarHorizontalChart()
    				      .x(function(d) { return d.label })
    				      .y(function(d) { return d.value })
    				      .margin(margin)
    				      .showValues(true)
    				      .valueFormat(d3.format('.0f'))
    				      .showControls(false);
    				  //setting height to minimun when data is less to avoid bar height to be big
    				  if(data.length > 0){
    					  var min = _.min(data,function(v){return v.values.length}).values.length;
    					  var max = _.max(data,function(v){return v.values.length}).values.length;
    					  var setMinHeight = false;
    					  if(max == min && min < 3){
    						  setMinHeight = true
    					  }else if(max < 3){
    						  setMinHeight = true
    					  }
    					  if(setMinHeight){
    						  chart.height("200");
    					  }
    				  }
    				  chart.tooltip.enabled();
    				  chart.yAxis
    				      .tickFormat(d3.format('d'));
//    				  chart.multibar.dispatch.on("elementClick", function(e) {
//    					  that.vent.trigger("toggle:facet",{viewName : "includeColumns",key :columnKey,value :e.data.label});
//    				  });
    				  d3.select(el)
    				  .datum(data)
    				    .transition().duration(500)
    				      .call(chart);
    				  return chart;
    			},
    			callback : function(graph){
    				d3.select(el)
    					.selectAll("rect")
    					.style("cursor","pointer");
    				that.$el.resize(function(){
                        d3.select(el)
//                            .attr('width', width)
//                            .attr('height', height)
                            .transition().duration(0)
                            .call(graph);
    				});
    			}
    			});
    		},
    		onDependentServiceClick : function(e){
    			var $el = $(e.currentTarget),that=this,service = $el.data("service"),serviceSelected = this.ui.serviceContainer.find(".active");
    			var getDependentServices = function(forServiceName,Service){
    				var serviceObj = that.servicesData.service[forServiceName];
        			if(serviceObj){
        				return _.findWhere(serviceObj.dependencies,{service:Service});
        			}
    			}
    			var prev = this.ui.dependencyCont.find(".active");
    			if(prev.length && serviceSelected.length){
    				var $prev = prev.first();
    				var found = getDependentServices(serviceSelected.data("name"),$prev.data("service"));
    				if(found && found.components){
    					for(var i=0; i<found.components.length; i++){
    						this.removeComponentFromSelect2(found.components[i]);
    					}
    				}
    			}
    			$el.siblings().removeClass("active");
    			$el.toggleClass("active");
        		if(serviceSelected.length){
//        			var serviceObj = this.servicesData.service[serviceSelected.data("name")];
//        			if(serviceObj){
        				var found = getDependentServices(serviceSelected.data("name"),service);
        				if(found && found.components){
        					for(var i=0; i<found.components.length; i++){
        						if($el.hasClass("active")){
        							this.addComponentToSelect2(found.components[i]);
        						}else
        							this.removeComponentFromSelect2(found.components[i]);
        					}
        					
        				}
//        			}
        		}
    		},
            addComponentToSelect2 : function(ele){
            	var arr = this.ui.components.select2("data");
            	arr.push({
            		id : ele,
            		name : ele
            	});
            	this.ui.components.select2("val",_.pluck(arr,"id"),true);
            },
            removeComponentFromSelect2 : function(id){
            	var arr = this.ui.components.select2("data");
            	for(var i=0; i<arr.length; i++){
            		if(arr[i].id === id){
            			delete arr[i];
            		}
            	}
            	this.ui.components.select2("val",_.pluck(arr,"id"),true);
            },
            onExpandCollapseSections : function(){
            	var sideRight = this.$(".sideRight"),sideLeft = this.$(".sideLeft"),rotate = this.$(".rotateIcon")
            	if( sideRight.hasClass('sideRightclose')){
            		sideLeft.removeClass('sideLeftOpen');
            		sideRight.removeClass('sideRightclose');
                    rotate.removeClass('toRight');
            	}else{
            		sideLeft.addClass('sideLeftOpen');
            		sideRight.addClass('sideRightclose');
                    rotate.addClass('toRight');
            	}
            }
        });
});