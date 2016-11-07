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
	'utils/Globals',
	'hbs!tmpl/dashboard/HostListView_tmpl',
	'collections/VNodeList'
],function(require,Backbone,Globals,HostListViewTmpl,VNodeList){
    'use strict';
	
	var HostListView = Backbone.Marionette.ItemView.extend(
	/** @lends HostListView */
	{
		_viewName : 'HostListView',

		template: HostListViewTmpl,
		
		/** ui selector cache */
		ui: {
			hostNameTxt : "[data-id='hostName']",
			searcHostBtn : "[data-id='searchHost']",
			mainCheck : "#mainCheck"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events["change "+this.ui.mainCheck] = 'onMainCheckboxClick';
			events["click "+this.ui.searcHostBtn] = 'onSearchHostClick';
			events["change .tree input[type='checkbox']"] = 'onChangeNodeCheckbox';
			events["keypress "+this.ui.hostNameTxt] = 'onSearchHostKeypress';
			events["click .tree a[data-type='C']"] = 'onNewTabIconClick';
			events["mouseenter .tree li[data-type='C']"] = function(e){
				$(e.currentTarget).children("a").removeClass("hidden");
			};
			events["mouseleave .tree li[data-type='C']"] = function(e){
				$(e.currentTarget).children("a").addClass("hidden");
			};
			events["click [data-id='collapseAll']"] = function(e){
				//_.each(this.$("[data-state='expand']"));
				this.$("[data-state='expand']").click();
			};
			events["click [data-id='expandAll']"] = function(e){
				//_.each(this.$("[data-state='expand']"));
				this.$("[data-state='collapse']").click();
			};
			return events;
		},

		/**
		 * intialize a new HostListView ItemView
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent','globalVent','params'));
			this.searchParams = (this.params)? this.params :{};
			this.collection = new VNodeList([], {
                state: {
                    firstPage: 0,
                    pageSize: 50
                }
            });
			this.bindEvents();
			this.hostState = {};
		},
		/** all events binding here */
		bindEvents : function(){
			this.listenTo(this.vent,"main:search level:filter type:mustNot type:mustBe search:include:exclude " +
					"logtime:filter reinitialize:filter:tree tab:refresh " +
					Globals.eventName.serviceLogsIncludeColumns+" "+Globals.eventName.serviceLogsExcludeColumns,function(value){
            	_.extend(this.searchParams,value);
            	this.fetchHosts(this.searchParams);
            },this);
			
			this.listenTo(this.globalVent, "globalExclusion:component:message", function(value) {
				_.extend(this.searchParams,value);
            	this.fetchHosts(this.searchParams);
			},this);
			
//			this.listenTo(this.vent, "tab:refresh", function(params) {
//				_.extend(this.searchParams,params);
//            	this.fetchHosts(this.searchParams);
//			},this);
			
			this.listenTo(this.collection,"reset",function(){
				this.removeSpinner();
				this.renderHosts();
			});
		},
		/** on render callback */
		onRender: function() {
			this.fetchHosts((this.params) ? this.params : {q:"*:*"});
		},
		fetchHosts : function(params){
			var that = this;
			$.extend(this.collection.queryParams,params,{treeParams:null});
			this.collection.fetch({
				reset:true,
				complete : function(){
					that.removeSpinner();
				}
			});
		},
		renderHosts : function(){
			var $el = this.$(".hostNodes"),that=this;
			this.$('.nodebar').popover('destroy');
			$el.empty();
			var $ul;
			this.collection.each(function(data,i){
				//appending box for every host
				//if(i==0 || (i % 5 == 0)){
					//($ul) ? that.formatTree($ul.parent(".tree")) : "";
					$el.append("<div class='col-md-3'><div class='box box-dashed'><div class='box-contentHost'><div class='tree smart-form'><ul></ul></div></div></div></div>");
					$ul = $el.find(".tree.smart-form").last().find("ul");
				//}
				if(data.get("isParent")  === "true" || data.get("isParent") == true){
					$ul.append(that.getParentNode(data.attributes));
					var rootNode = $ul.find("li[data-node='"+data.get("name")+"']");
					that.appendChilNodes(data.attributes,rootNode);
					that.appendPopover(rootNode,data.attributes);
				}
			});
			this.formatTree();
			this.restoreCheckbox();
		},
		formatTree : function(){
			var that = this;
			this.$('.tree > ul').attr('role', 'tree').find('ul').attr('role', 'group');
		    this.$('.tree').find('li:has(ul)').addClass('parent_li').attr('role', 'treeitem').find(' > span').attr('title', 'Expand this branch').attr("data-state","collapse").on('click', function(e) {
		        var children = $(this).parent('li.parent_li').find(' > ul > li');
		        if (children.is(':visible')) {
		        	that.hostState[$(this).parent().data("node")] = false;
		            children.hide('fast');
		            $(this).attr('title', 'Expand this branch').attr("data-state","collapse").find(' > i').removeClass().addClass('fa fa-plus-circle');
		        } else {
		        	that.hostState[$(this).parent().data("node")] = true;
		            children.show('fast');
		            $(this).attr('title', 'Collapse this branch').attr("data-state","expand").find(' > i').removeClass().addClass('fa fa-minus-circle');
		        }
		        e.stopPropagation();
		    });
		    //this.$('[data-toggle="tooltip"]').tooltip();
		},
		getParentNode : function(node){
			return '<li data-type="'+node.type+'" data-parent="'+node.isParent+'" data-node = "'+node.name+'"> '+this.getCountDistribution(node)+this.getCheckbox(node)+' <span><i class="fa fa-plus-circle"></i> <strong>'+node.name.split(".")[0]+'</strong> ('+node.value+')</span></li>';
		},
		getChildNode : function(node){
			return '<li style="display:none;" data-type="'+node.type+'" data-node = "'+node.name+'" >'+
			'<a data-type="'+node.type+'" data-node = "'+node.name+'" href="javascript:void(0)" class="pull-right hidden"><i class="fa fa-share"></i></a>'+
			this.getCountDistribution(node)+
			//this.getCheckbox(node)+
			' <span><strong>'+node.name+'</strong> ('+node.value+')</span></li>';
		},
		getCheckbox : function(node){
			return '<label class="checkbox no-margin"> <input  data-parent="'+node.isParent+'" data-type="'+node.type+'" data-node="'+node.name+'" checked="checked" type="checkbox"> <i class="fa fa-square-o small"></i></label>';
		},
		appendChilNodes : function(data, $parentNode){
			$parentNode.append('<ul>');
			data.childs = _.isArray(data.childs) ? data.childs : new Array(data.childs);
			var $el = $parentNode.find('ul'),that=this;
			_.each(data.childs,function(node){
				if(! _.isUndefined(node.childs)){
					$el.append(this.getParentNode(node));
					var rootNode = $el.find("li[data-node='"+data.name+"']");
					that.appendChilNodes(node,rootNode);
				}
					
				if(node.isParent === "true" || node.isParent == true)
					$el.append(that.getParentNode(node));
				else{
					$el.append(that.getChildNode(node));
				}
				that.appendPopover($el.find('li').last(),node);
			});
		},
		appendPopover : function(node,data){
			node.children('.nodebar').popover({
				trigger: 'hover',
				placement: "top",
				html: true,
				container: 'body',
				template : '<div class="popover log-count" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>',
				content: this.getPopoverHTML(data)
			});
		},
		getPopoverHTML : function(node){
			//<span style='color:#E2D014'><i class='fa fa-circle'></i> 12876</span>
			if(! node.logLevelCount)
				return "";
			var html="";
			//if count for all elements is zero then popover should not appear
			if(_.find(node.logLevelCount,function(d){ return parseInt(d.value,10) > 0})){
				_.each(node.logLevelCount,function(data){
					html += "<span class='"+data.name+"'><i class='fa fa-circle'></i> "+data.name+": <strong>"+data.value+"</strong></span>";
				});
			}
			return html;
		},
		getCountDistribution : function(node){
			if(! node.logLevelCount)
				return "";
			return '<div data-node = "'+node.name+'" class="nodebar">'+this.getLevelDistribution(node)+'</div>';
		},
		getLevelDistribution : function(node){
			var html="";
			if(! _.isUndefined(node.logLevelCount) && ! _.isArray(node.logLevelCount))
				node.logLevelCount = [node.logLevelCount];
			var toPct = this.calculatePercentge(node.logLevelCount);
			_.each(node.logLevelCount,function(data){
				//html += '<div class="node '+data.name+'" style="width:'+toPct(data)+'%;" data-toggle="tooltip" title="'+data.value+'" data-original-title="'+data.value+'"></div>';
				html += '<div class="node '+data.name+'" style="width:'+toPct(data)+'%;"></div>';
			});
			return html;
		},
		calculatePercentge : function(values) {
		       var sum = 0;
		       for( var i = 0; i != values.length; ++i ) {
		    	   sum = sum + parseInt(values[i].value,10); 
		       }
		       var scale = 100/sum;
		       return function( x ){ 
		         return (parseInt(x.value,10)*scale)/*.toFixed(5)*/;
		       };
		},
		onNewTabIconClick : function(e){
			var $el = $(e.currentTarget),host,component,that=this;
			if($el.children().is('img')){
				this.onCompareLink($el);
			}else{
				if($el.parents("[data-parent=true]")){
					host = $el.parents("[data-parent=true]").data("node");
					component = $el.data("node");
					that.globalVent.trigger("render:tab",{
						params:_.extend({},{
							host_name :  host,
							component_name : component
						},that.searchParams,{treeParams:null}),
						globalVent : that.globalVent
					});
				}
			}
		
		},
		onChangeNodeCheckbox : function(e){
			var $el = $(e.currentTarget);
			if($el.data("parent") == "true" || $el.data("parent") == true){
				if($el[0].checked)
					$el.parent().siblings("ul").find("input").prop("checked",true);
				else
					$el.parent().siblings("ul").find("input").prop("checked",false);
			}else{
				var mainParent = $el.parents("[data-type='H']");
				var checkedLen = mainParent.find("ul :checkbox:checked").length;
				var totalCheckboxLen = mainParent.find("ul :checkbox").length;
				if(checkedLen > 0)
					mainParent.find("input[data-type='H']").prop("checked",true);
				else
					mainParent.find("input[data-type='H']").prop("checked",false);
				if(checkedLen < totalCheckboxLen)
					mainParent.find("input[data-type='H']").prop("indeterminate",true);
				else
					mainParent.find("input[data-type='H']").prop("indeterminate",false);
				
			}
			var data = this.getCheckedHierarchyData();
			this.vent.trigger("tree:search",{treeParams : JSON.stringify(_.pluck(data,"h"))});
		},
		getCheckedHierarchyData : function(){
			var data=[];
			var parents = this.$('.tree :checkbox:checked').filter('[data-parent="true"]');
			_.each(parents,function(p){
				var obj = {
						h : $(p).data("node"),
						c : []
				};
				_.each($(p).parent().siblings("ul").find(":checkbox:checked"),function(c){
					obj.c.push($(c).data("node"));
				});
				data.push(obj);
			});
			return data;
		},
		onMainCheckboxClick : function(e){
			if(e.target.checked){
				this.$('.tree  input[type="checkbox"]').prop({"checked":true,"indeterminate":false});
				
			}else
				this.$('.tree  input[type="checkbox"]').prop({"checked":false,"indeterminate":false});
			var data = this.getCheckedHierarchyData();
			this.params.treeParams = _.extend({},data);
			this.vent.trigger("tree:search",{treeParams : JSON.stringify(_.pluck(data,"h"))});
			
		},
		onSearchHostClick : function(e){
			var hostName = this.ui.hostNameTxt.val();
			this.searchHostNameCallBck(hostName);
		},
		searchHostNameCallBck : function(name){
			this.$('.nodebar').popover('destroy');
			this.addSpinner();
			this.$(".hostNodes").empty().html("Loading.....");
			this.fetchHosts({hostName:$.trim(name)});
		},
		onSearchHostKeypress : function(e){
			if(e.which == 13){
				this.searchHostNameCallBck(e.currentTarget.value);
			}
		},
		addSpinner : function(){
			this.ui.searcHostBtn.find("i").removeClass().addClass("fa fa-spinner fa-spin");
		},
		removeSpinner : function(){
			this.ui.searcHostBtn.find("i").removeClass().addClass("fa fa-search");
		},
		restoreCheckbox : function(){
			var params = (this.params.treeParams) ? JSON.parse(this.params.treeParams) : undefined,that=this;
			if(params){
				that.$("input[data-node]").prop("checked",false);
				_.each(params,function(node){
					if(node){
						that.$("input[data-type='H'][data-node='"+node+"']").prop("checked",true);
					}
					
						
				});
			}
			if(this.hostState){
				_.each(this.hostState,function(value,key){
					if(value){
						that.$("li[data-type='H'][data-node='"+key+"']").find("span").click();
					}
				});
			}
		},
		/** on close */
		onClose: function(){
		},
	});
	return HostListView;
});
