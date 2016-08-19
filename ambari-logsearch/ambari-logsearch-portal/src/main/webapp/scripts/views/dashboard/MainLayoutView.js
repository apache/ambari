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
	'utils/ViewUtils',
	'utils/Globals',
	'hbs!tmpl/dashboard/MainLayoutView_tmpl',
	'select2',
	'sparkline',
	'd3.tip'
],function(require,Backbone,Utils,ViewUtils,Globals,MainLayoutViewTmpl){
    'use strict';

	var MainLayoutView = Backbone.Marionette.Layout.extend(
	/** @lends MainLayoutView */
	{
		_viewName : 'MainLayoutView',

		template: MainLayoutViewTmpl,

		/** Layout sub regions */
		regions: {
			RLogLevel : "#r_logLevel",
			RComponents : "#r_components",
			RHosts : "#r_hosts",
			RHierarchyTab : "#r_Hierarchy",
			RHostInfoTab : "#r_HostInfo",
			RBubbleTable : "#r_BubbleTable",
			RAuditTab : "#r_AuditInfo",
			RAuditDashboard:"#r_AuditDashboard",
			RTroubleShoot:"#r_TroubleShoot"
		},

		/** ui selector cache */
		ui: {
//			graph : "#graphAgg",
			searchBox : '[data-id="mainSearch"]',
			searchBoxBtn : '[data-id="mainSearchBtn"]',
			compare:".compare .panel-heading",
			CompareButton:"[data-id='CompareButton']",
			CompareClearAll:"[data-id='CompareClearAll']",
			CloseCompareComponent:".hostCompList .closeComponent"
			//viewType: "input[name='viewType']",
		},

		/** ui events hash */
		events: function() {
			var events = {};
			//events['click #searchLog'] = 'onSearchLogClick';
			events["click "+this.ui.searchBoxBtn] = 'onSearchLogClick';
			events['change ' + this.ui.viewType]  = 'onViewTypeChange';
			events['click button[data-tab-id]']  = 'onDeleteTabClick';
			events["click "+this.ui.compare] = function(e){
				this.togglePanelPosition(false,false);
			}
			events["click "+this.ui.CompareButton] = 'onCompareButtonClick';
			events["click "+this.ui.CompareClearAll] = 'onCompareClearAllClick';
			events["click "+this.ui.CloseCompareComponent] = function(e){
				this.onCloseCompareComponentClick($(e.currentTarget).parents('span').data().id,true);
			}
			events["click .nav.nav-tabs li"] = function(e){
				this.hideContextMenu();
			}
			return events;
		},

		/**
		 * intialize a new MainLayoutView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'globalVent'));
//			this.collection = new VLogList([], {
//                state: {
//                    firstPage: 0,
//                    pageSize: 50
//                }
//            });
//			this.collection.url = Globals.baseURL + "service/logs";
			this.vent = new Backbone.Wreqr.EventAggregator();
			this.dateUtil = Utils.dateUtil;
			this.bindEvents();
			this.componetList =[];
		},
		bindEvents : function(){
			this.listenTo(this.globalVent,"render:tab",function(options){
				var that = this;
				this.hideContextMenu();
            	this.renderLogFileTab(options);
            	setTimeout(function(){
            		that.reAdjustTab()
            	},1000);

            },this);
            this.listenTo(this.globalVent,"render:comparison:tab",function(options){
				this.hideContextMenu();
            	this.renderComparisonTab(options);
            },this);
            this.listenTo(this.globalVent,"show:tab",function(tabName){
            	this.showTab(tabName);
            },this);
            this.listenTo(this.globalVent,"add:compare",function($el){
            	this.quickMenuCompare = true;
            	this.onCompareLink($el);
            },this);
		},
		onRender : function(){
			this.renderTroubleShootTab();
			this.renderHierarchyTab();
			this.renderAuditTab();
			//this.renderDashBoardTab();
			this.togglePanelPosition(true);
			this.bindTabCheckboxClick();
			this.bindTabClickListener();
			this.tabScrollBind();
		},
		onShow : function(){
			//navigating to specific component tab
			var params = ViewUtils.getDefaultParams();
			if(params.host_name && params.component_name){
				this.globalVent.trigger("render:tab",{
					params:_.extend({},{
						host :  params.host_name,
						component : params.component_name
					},params),
					globalVent : this.globalVent
				});
			}
		},
		renderLogFileTab : function(view){
			var that = this;
			require(['views/tabs/LogFileView'], function(LogFileView){
				var tabName = (view.params.host + view.params.component).replace(/\./g,"_");
				if(_.isUndefined(that[tabName])){
					var region = {};
					region[tabName] = '#' + tabName;
					$('<div/>', {
						'id': tabName,
						'class': 'tab-pane',
						'role':"tabpanel"
					}).appendTo(that.$('.tab-content'));
					that.addRegions(region);
					var region = that.getRegion(tabName);
					region.show(new LogFileView(view));
					that.$(".nav.nav-tabs").append('<li data-id="'+tabName+'" role="presentation">'+
							'<a data-id="'+tabName+'" data-host="'+view.params.host+'" data-component="'+view.params.component+'" href="#'+tabName+'" aria-controls="profile" role="tab" data-toggle="tab" title="'+view.params.host.split(".")[0]+' >> '+view.params.component+' ">'+view.params.host.split(".")[0]+'<b> >> </b>'+view.params.component+'</a>'+
	//						'<span class="air air-top-right">'+
								'<button data-tab-id="'+tabName+'" class="btn-closeTab"><i class="fa fa-times-circle"></i></button>'+
								'<div class="compareClick" title="Compare"><i class="fa fa-square-o"></i></div>');
	//							'<i class="fa fa-times"></i>'+
	//							'</button></span></li>');
				}else{
					if(that[tabName].currentView){
						_.extend(that[tabName].currentView.params,view.params);
						that[tabName].currentView.render();
					}
				}
				//$("html, body").animate({ scrollTop: 0 }, 500);
				that.showTab(tabName);
			});
		},
		renderComparisonTab:function(view){
			var that = this;
			require(['views/tabs/ComparisonLayoutView'], function(ComparisonLayoutView){
				var tabName = "";
				_.each(view.componetList,function(object){
					if(object.host && object.component){
						tabName += (object.host + object.component).replace(/\./g,"_");
					}
				});
				if(_.isUndefined(that[tabName])){
					var region = {};
					region[tabName] = '#' + tabName;
					$('<div/>', {
						'id': tabName,
						'class': 'tab-pane',
						'role':"tabpanel"
					}).appendTo(that.$('.tab-content'));
					that.addRegions(region);
					var region = that.getRegion(tabName);
					region.show(new ComparisonLayoutView(view));
					that.$(".nav.nav-tabs").append('<li data-id="'+tabName+'" role="presentation">'+
							'<a data-id="'+tabName+'"  href="#'+tabName+'" aria-controls="profile" role="tab" data-toggle="tab">Compare</a>'+
	//						'<span class="air air-top-right">'+
								'<button data-tab-id="'+tabName+'" class="btn-closeTab"><i class="fa fa-times-circle"></i></button>');
	//							'<i class="fa fa-times"></i>'+
	//							'</button></span></li>');
				}else{
					if(that[tabName].currentView){
						_.extend(that[tabName].currentView.params,view.params);
						that[tabName].currentView.render();
					}
				}
				$("html, body").animate({ scrollTop: 0 }, 500);
				that.showTab(tabName);
			});

		},
		showTab : function(tabId){
			this.$(".nav.nav-tabs li").removeClass("active");
			this.$("li[data-id='"+tabId+"']").addClass("active");
			this.$(".tab-pane").removeClass("active");
			this.$("#"+tabId).addClass("active");
			this.tabOpen = true;
			this.reAdjustTab();
		},
		onDeleteTabClick : function(e){
			var tabId = $(e.currentTarget).data("tab-id");
			if(this[tabId]){
				this[tabId].close && this[tabId].close();
				this.removeRegion(tabId);
				this.$("li[data-id="+tabId+"]").remove();
				this.$("#"+tabId).remove();
				this.showTab(this.$(".nav.nav-tabs li").last().data("id"));
			}
		},
		bindDraggableEvent : function(){
			Utils.bindDraggableEvent(this.$( "div.box").not('.no-drop'));
		},
		renderLogLevel : function(){
			var that = this;
			require(['views/dashboard/LogLevelView'], function(LogLevelView){
			 	that.RLogLevel.show(new LogLevelView({
					vent : that.vent,
					globalVent:that.globalVent
				}));
			})

		},
		renderComponents : function(){
			var that = this;
			require(['views/dashboard/ComponentsView'], function(ComponentsView){
				that.RComponents.show(new ComponentsView({
					vent : that.vent,
					globalVent:that.globalVent
				}));
			})
		},
		renderHosts : function(){
			var that = this;
			require(['views/dashboard/HostsView'], function(HostsView){
				that.RHosts.show(new HostsView({
					vent : that.vent,
					globalVent:that.globalVent
				}));
			});
		},
		renderBubbleTableView : function(){
			var that = this;
			require(['views/dashboard/BubbleGraphTableLayoutView'], function(BubbleTableLayoutView){
				that.RBubbleTable.show(new BubbleTableLayoutView({
					vent : that.vent,
					globalVent:that.globalVent
				}));
			});
		},
		renderTroubleShootTab:function(){
			var that = this;
			require(['views/troubleshoot/TroubleShootLayoutView'], function(TroubleShootLayoutView){

				that.RTroubleShoot.show(new TroubleShootLayoutView({
					globalVent:that.globalVent
				}));
			});
		},
		renderHierarchyTab : function(){
			var that = this;
			require(['views/tabs/HierarchyTabLayoutView'], function(HierarchyTabLayoutView){
				that.RHierarchyTab.show(new HierarchyTabLayoutView({
					globalVent:that.globalVent
				}));
			});
		},
		renderHostInfoTab : function(){
			var that = this;
			require(['views/tabs/HostInfoTabLayoutView'], function(HostInfoTabLayoutView){
				that.RHostInfoTab.show(new HostInfoTabLayoutView({
					globalVent:that.globalVent
				}));
			});
		},
		renderAuditTab : function(){
			var that = this;
			require(['views/audit/AuditTabLayoutView'], function(AuditTabLayoutView){
				that.RAuditTab.show(new AuditTabLayoutView({
					globalVent:that.globalVent
				}));
			});
		},
		renderDashBoardTab:function(){
			var that = this;
			require(['views/dashboard/DashboardLayoutView'], function(DashboardLayoutView){
				that.RAuditDashboard.show(new DashboardLayoutView({
					globalVent:that.globalVent
				}));
			});
		},
		hideContextMenu : function(){
			$(".contextMenu").hide();
		},
		onSearchLogClick : function(){
			var value = this.ui.searchBox.val();
			if(_.isEmpty(value)){
				this.ui.searchBox.val("*:*");
				value = "*:*";
			}
//			this.fetchGraphData({q : value});
//			this.fetchTableData(value);
			this.vent.trigger("main:search",{q:value});
		},
		//Style 2
		renderGraph : function(){
			var root = {
					name : "",
					dataList : this.graphModel.get("graphData")
			};
			var margin = 20,
		    diameter = 880;//this.ui.graph.width();//960;
			this.ui.graph.empty();
//		var color = d3.scale.linear()
//		    .domain([-1, 5])
//		    .range(["hsl(152,90%,90%)", "hsl(228,30%,40%)"])
//		    .interpolate(d3.interpolateHcl);
		var color = d3.scale.ordinal()
		    .domain([0,1])
		    //.range(["#ECFCBD","#ECFCBD","#ECE78F","#f4f4c8"]);
			.range(["#dddddd","#cccccc","#F5F5F5"]);
		var pack = d3.layout.pack()
		    .padding(2)
		    .size([diameter - margin, diameter - margin])
		    .value(function(d) {
		    	return d.count; })
		    .children(function(d){
		    	return d.dataList;
		    })

		var svg = d3.select(this.ui.graph[0]).append("svg")
		    .attr("width", diameter)
		    .attr("height", diameter)
		  .append("g")
		    .attr("transform", "translate(" + diameter / 2 + "," + diameter / 2 + ")");

		//d3.json("flare.json", function(error, root) {

		  var focus = root,
		      nodes = pack.nodes(root),
		      view;
		  /*
		   * Tip
		   */
		  var tipCirclePack = tip()
	          .attr('class', 'd3-tip')
	          .offset([-10, 0])
	          .html(function(d) {
	        	  var tempName = "<div>";
	              if(d.parent){
	            	  if(d.depth > 1)
	            		  tempName += d.parent.name+" => ";
	            	  tempName += d.name;
	              }
	              return tempName + "</div>";
	          })
          svg.call(tipCirclePack);
		  var circle = svg.selectAll("circle")
		      .data(nodes)
		    .enter().append("circle")
		      .attr("class", function(d) {
		    	  return d.parent ? d.children ? "node" : "node node--leaf "+d.name : "node node--root"; })
		      .style("fill", function(d) {
		    	  return d.children ? color(d.depth) : null; })
		      .on("click", function(d) {
		    	  if(d3.event.shiftKey){
		    		  alert("open in new tab")
		    	  }else{
		    		  if (focus !== d) zoom(d), d3.event.stopPropagation();
		    	  }

		      })
		      .on('mouseover', function (d,i) {
                    if (d.x) {
                        tipCirclePack.show(d);
                    }
                })
              .on('mouseout', function (d,i) {
                    if (d.x) {
                        tipCirclePack.hide(d);
                    }
                });

		  var text = svg.selectAll("text")
		      .data(nodes)
		    .enter().append("text")
		      .attr("class", "label")
		      .style("fill-opacity", function(d) { return d.parent === root ? 1 : 0; })
		      .style("display", function(d) { return d.parent === root ? null : "none"; })
		      .text(function(d) {
		    	  if(d.count){
		    		  if(d.count > 0)
		    			  return d.name;
		    		  else
		    			  return "";
		    	  }else
		    		  return d.name;

		      });

		  var node = svg.selectAll("circle,text");

		  d3.select(this.ui.graph[0])
		      .style("background", color(-1))
		      .on("click", function() { zoom(root); });

		  zoomTo([root.x, root.y, root.r * 2 + margin]);
		  function zoom(d) {
		    var focus0 = focus; focus = d;

		    var transition = d3.transition()
		        .duration(d3.event.altKey ? 7500 : 750)
		        .tween("zoom", function(d) {
		          var i = d3.interpolateZoom(view, [focus.x, focus.y, focus.r * 2 + margin]);
		          return function(t) { zoomTo(i(t)); };
		        });

		    transition.selectAll("text")
		      .filter(function(d) { return d.parent === focus || this.style.display === "inline"; })
		        .style("fill-opacity", function(d) { return d.parent === focus ? 1 : 0; })
		        .each("start", function(d) { if (d.parent === focus) this.style.display = "inline"; })
		        .each("end", function(d) { if (d.parent !== focus) this.style.display = "none"; });
		  }

		  function zoomTo(v) {
		    var k = diameter / v[2]; view = v;
		    node.attr("transform", function(d) { return "translate(" + (d.x - v[0]) * k + "," + (d.y - v[1]) * k + ")"; });
		    circle.attr("r", function(d) { return d.r * k; });
		  }
		},
		bindTabCheckboxClick:function(){
			var that = this;
			this.$('div[role="tabpanel"] ul').on('click','li div.compareClick',function(){
				that.tabcheckBoxSelectDeselect($(this))
			})
		},
		tabcheckBoxSelectDeselect:function(el,fromEvent){
			var that = this,
		    clickedId = this.$('div[role="tabpanel"] ul').find(el).parents('li').data('id');
			if (el.find('i').hasClass('fa-square-o')) {
				var idList = _.pluck(this.componetList, 'id');
			    if (! _.contains(idList, clickedId)) {
			    	if(this.componetList.length >= 4){
			    		Utils.alertPopup({
			    			msg: "Currently only four components comparison supported."
			    		});
			    		return;
			    	}else{
			    		el.find('i').removeClass('fa-square-o').addClass('fa-check-square-o');
			    		this.quickMenuCompare = false;
			    		this.onCompareLink(el);
			    	}
			    }else{
			    	el.find('i').removeClass('fa-square-o').addClass('fa-check-square-o');
			    }
			} else {
			    el.find('i').removeClass('fa-check-square-o').addClass('fa-square-o');
			    if (!fromEvent) {
			        this.onCloseCompareComponentClick(el.parents('li').find('a').data().id)
			    }
			}
		},
		onCompareLink:function($el){
			this.togglePanelPosition(false, true);
			var clickedId = "",
			    newValue = true,
			    dataValue;
			if (this.quickMenuCompare) {
			    dataValue = $el.data();
			    if(dataValue.host){
			    	dataValue.id = dataValue.host.replace(/\./g, '_') + dataValue.node;
			    }
			} else {
			    dataValue = $el.parents('li').find('a').data();
			}
			if (dataValue.id) {
			    var clickedId = dataValue.id;
			}
			_.each(this.componetList, function(object) {
			    if (object.id.match(clickedId)) {
			        newValue = false;
			    }
			});
			if (this.componetList.length >= 4) {
			    if (newValue) {
			        Utils.alertPopup({
			            msg: "Currently only four components comparison supported."
			        });
			        return;
			    }
			}
			if (this.componetList.length <= 3 && newValue) {
			    if (dataValue.host && (dataValue.component || dataValue.node)) {
			        var host = dataValue.host;
			        var component = dataValue.component || dataValue.node;
			        var spanLength = this.$('.compare .panel-body span.hasNode');
			        if (spanLength.length != 0 && spanLength.length >= 1) {
			            this.componetList.push({ 'host': host, 'component': component, id: clickedId });
			            this.$('.compare .panel-body .hostCompList').append('<span class="hasNode" data-id="' + clickedId + '"><i class=" closeComponent fa fa-times-circle"></i>' + host.split(".")[0] + ' <i class="fa fa-angle-double-right"></i><br> ' + component + '</span>');
			        } else {
			            this.componetList.push({ 'host': host, 'component': component, id: clickedId });
			            this.$('.compare .panel-body .hostCompList').html('<span class="hasNode" data-id="' + clickedId + '"><i class=" closeComponent fa fa-times-circle"></i>' + host.split(".")[0] + ' <i class="fa fa-angle-double-right"></i><br> ' + component + '</span>');
			        }
			    }
			}
			this.quickMenuCompare = false;
		},
		onCompareButtonClick:function(){
			if(this.componetList.length == 1){
				Utils.alertPopup({
                        msg: "Minimum two components are required for comparison. Please select one more component and try again."
                });
			}else{
				var dateRangeLabel ='Last 1 Hour';
				var dateObj = this.dateUtil.getRelativeDateFromString(dateRangeLabel);

				if (this.RHierarchyTab.currentView && this.RHierarchyTab.currentView.defaultParams) {
				    var dateParams = this.RHierarchyTab.currentView.defaultParams;
				    if (!_.isUndefined(dateParams) && _.isObject(dateParams)) {
				        dateObj = {
				            from: dateParams.from,
				            to: dateParams.to,
				            dateRangeLabel: dateParams.dateRangeLabel
				        };
				    }
				}

				this.globalVent.trigger("render:comparison:tab",{
					params: dateObj,
					componetList:this.componetList,
					globalVent : this.globalVent
				});
			}
			this.togglePanelPosition(false,false)
		},
		togglePanelPosition:function(hideFully,clickFromLi){
			if(hideFully){
				this.$('.compare').css('bottom', "-136px");
				this.$('.compare .panel-heading').addClass("down");
				return;
			}
			if (this.$('.compare .panel-heading').hasClass('down')) {
			    this.$('.compare').css('bottom', "0px");
			    this.$('.compare .panel-heading').removeClass("down")
			        /*   setTimeout(function() {
			               this.$('.compare').css('bottom', (-(this.$('.compare .panel-body').height() + 39)) + "px");
			               this.$('.compare .panel-heading').addClass("down");;
			           }, 2000);*/
			} else if (!clickFromLi) {
			    this.$('.compare').css('bottom', (-(this.$('.compare .panel-body').height() + 32)) + "px");
			    this.$('.compare .panel-heading').addClass("down");
			}
		},
		onCompareClearAllClick:function(e){
			e.stopPropagation();
			this.componetList =[];
			this.$('.compare .panel-body .hostCompList').html('');
			this.$('div[role="tabpanel"] ul').find('li div.compareClick i').removeClass('fa-check-square-o').addClass('fa-square-o');
			this.togglePanelPosition(true)
		},
		onCloseCompareComponentClick:function(id,fromEvent){
			var clickedId = id,that = this;
			if (clickedId) {
			    var clickedIndex = undefined;
			    _.each(this.componetList, function(object, i) {
			        if (object.id.match(clickedId)) {
			            that.$('.compare .panel-body .hostCompList').find('span[data-id="'+id+'"]').remove();
			            clickedIndex = i + 1;
			        }
			    });
			    if (clickedIndex) {
			        this.componetList.splice(clickedIndex - 1, 1);
			        if(this.componetList.length == 0){
			        	this.togglePanelPosition(true);
			        }else{
			        	this.togglePanelPosition(false,true);
			        }
			    }
			    if(fromEvent){
					this.tabcheckBoxSelectDeselect(this.$('div[role="tabpanel"] ul').find('li[data-id="'+clickedId+'"] div.compareClick'),true)
				}
			}
		},
		bindTabClickListener:function(){
			var that = this;
			this.$("ul[role='tablist']").on('click','li',function(){
				that.globalVent.trigger("tab:click",this);
			});
		},
		tabScrollBind:function(){
			var hidWidth;
			var scrollBarWidths = 40;
			var that = this;

			var widthOfList = function(){
			  var itemsWidth = 0;
			  that.$('.list li').each(function(){
			    var itemWidth = $(this).outerWidth();
			    itemsWidth+=itemWidth;
			  });
			  return itemsWidth;
			};

			var widthOfHidden = function(){
			  return (($('.wrapper').outerWidth())-widthOfList()-getLeftPosi())-scrollBarWidths;
			};

			var getLeftPosi = function(){
			  return that.$('.list').position().left;
			};

			this.reAdjustTab = function(){
			  if ((that.$('.wrapper').outerWidth()) < widthOfList()) {
			  	if(that.tabOpen){
			  		that.$('.list').animate({left:"+="+widthOfHidden()+"px"},'slow');
			  		that.tabOpen = false;
			  	}
			    that.$('.scroller-right').show();
			  }
			  else {
			    that.$('.scroller-right').hide();
			  }

			  if (getLeftPosi()<0) {
			    that.$('.scroller-left').show();
			  }
			  else {
			    that.$('.item').animate({left:"-="+(-200)+"px"},'slow');
			  	that.$('.scroller-left').hide();
			  }
			}


			this.$('.scroller-right').click(function(e) {

			 /* that.$('.scroller-left').fadeIn('slow');
			  that.$('.scroller-right').fadeOut('slow');*/
			  //console.log(widthOfHidden())
			  if(widthOfHidden()+55 < 0){
			  	 that.$('.list').animate({left:"+="+(-43)+"px"},0,function(){
			  		that.reAdjustTab();
			 	 });
			  }

			});

			this.$('.scroller-left').click(function() {

				/*that.$('.scroller-right').fadeIn('slow');
				that.$('.scroller-left').fadeOut('slow');*/
			  	//console.log(getLeftPosi())
			  	if(getLeftPosi() < 0){
			  		that.$('.list').animate({left:"-="+(-40)+"px"},0,function(){
			  		that.reAdjustTab();
			  	});
			  	}

			});
		},
	});
	return MainLayoutView;

});
