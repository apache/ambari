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
    'utils/Utils',
    'collections/VLogLevelList',
    'models/VCommonModel',
    'hbs!tmpl/tabs/TreeView_tmpl'
],function(require,Backbone,Globals,Utils,VLogLevel,VCommonModel,TreeViewTmpl){
    'use strict';
	
	return Backbone.Marionette.Layout.extend(
	/** @lends LogLevelView */
	{
		_viewName : 'TreeViewTmpl',

		template: TreeViewTmpl,

		/** Layout sub regions */
		regions: {
		},

		/** ui selector cache */
		ui: {
			mainCheck : "#mainCheck",
			next : "#nextSrch"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events["change "+this.ui.mainCheck] = 'onMainCheckboxClick';
			events["change .tree input[type='checkbox']"] = 'onChangeNodeCheckbox';
			events["click .tree a[data-type='C']"] = 'onNewTabIconClick';
			events["mouseenter .tree li[data-type='C']"] = function(e){
				$(e.currentTarget).children("a").removeClass("hidden");
			};
			events["mouseleave .tree li[data-type='C']"] = function(e){
				$(e.currentTarget).children("a").addClass("hidden");
			};
			events["click .panel-trigger"] = function (e) {
				this.$(".panel-box").fadeToggle('slow');
				this.$("input#searchNode").focus();
			};
			events["keyup input#searchNode"] = function(e){
				if(e.which==13){
					this.ui.next.trigger("click");
					return
				}
				var value = $(e.currentTarget).val();
				this.$el.removeHighlight();
				this.counter = 0;
				this.$el.highlight(value);
			};
			events["click #prevSrch"] = "onSearchByName";
			events["click "+this.ui.next] = "onSearchByName";
			return events;
		},

		/**
		 * intialize a new LogLevelView Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent','globalVent','params'));
			this.treeModel = new VCommonModel();
			this.searchParams = (this.params)? this.params :{};
			this.bindEvents();
			this.treeLoaded = false;
		},
		onRender : function(){
//			this.fetchCollection();
			this.fetchTreeData(this.searchParams);
		},
		fetchTreeData : function(params){
			var that = this;
			this.treeModel.fetch({
				data : params,
				success : function(model,data){
					if(! data.vNodeList){
						that.treeModel.set("vNodeList",[]);
					}
					if(! _.isUndefined(that.treeModel.get("vNodeList")) &&  !_.isArray(that.treeModel.get("vNodeList")))
						that.treeModel.set("vNodeList",[that.treeModel.get("vNodeList")]);
					/*if(! that.treeLoaded)
						that.renderTree();
					else{
						that.updateCount();
					}
					that.treeLoaded = true;
					*/
					that.renderTree();
				},
				error : function(){
				},
				complete : function(){
					that.$("#loaderGraph").hide();
				}
			});
		},
		reinitializeFilterTree : function(values){
			this.fetchTreeData(values);
		},
		bindEvents : function(){
			this.listenTo(this.vent,"main:search level:filter type:mustNot type:mustBe search:include:exclude logtime:filter",function(value){
            	_.extend(this.searchParams,value);
            	this.fetchTreeData(this.searchParams);
            });
			this.listenTo(this.vent,"tree:strike:component",function(values){
				this.$("li[data-type='C'] span").removeClass("text-strike");
				for(var i=0; i < values.length;i++){
					this.$(".tree li[data-node='"+values[i]+"'] span").addClass("text-strike");
				}
            });
			this.listenTo(this.vent,"reinitialize:filter:tree",function(value){
				_.extend(this.searchParams,value);
            	this.reinitializeFilterTree(value);
            });
		},
		renderTree : function(){
			var $el = this.$(".tree").find('ul'),that=this;
			$el.empty();
			if(! _.isUndefined(this.treeModel.get("vNodeList")) && ! _.isArray(this.treeModel.get("vNodeList")))
				this.treeModel.set("vNodeList",[this.treeModel.get("vNodeList")])
			_.each(this.treeModel.get("vNodeList"),function(data){
				if(data.isParent  === "true" || data.isParent == true){
					$el.append(that.getParentNode(data));
					var rootNode = $el.find("li[data-node='"+data.name+"']");
					that.appendChilNodes(data,rootNode);
					that.appendPopover(rootNode,data);
				}
			});
			this.updateCount();
			this.formatTree();
			this.restoreCheckbox();
		},
		restoreCheckbox : function(){
			var params = (this.params.treeParams) ? JSON.parse(this.params.treeParams) : undefined,that=this;
			if(params){
				that.$("input[data-node]").prop("checked",false);
				_.each(params,function(node){
					if(node.h){
						that.$("input[data-type='H'][data-node='"+node.h+"']").prop("checked",true);
						if(node.c){
							var parent = that.$("li[data-type='H'][data-node='"+node.h+"']")
							parent.find("input[data-type='C']").prop("checked",false);
							_.each(node.c,function(component){
								parent.find("input[data-type='C'][data-node='"+component+"']").prop("checked",true);
							});
						}
					}
					
						
				});
			}
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
		getCheckbox : function(node){
			return '<label class="checkbox no-margin"> <input  data-parent="'+node.isParent+'" data-type="'+node.type+'" data-node="'+node.name+'" checked="checked" type="checkbox"> <i class="fa fa-square-o small"></i></label>';
            
		},
		getParentNode : function(node){
			return '<li data-type="'+node.type+'" data-parent="'+node.isParent+'" data-node = "'+node.name+'"> '+this.getCountDistribution(node)+this.getCheckbox(node)+' <span><i class="fa fa-plus-circle"></i> '+node.name.split(".")[0]+'</span></li>';
		},
		getChildNode : function(node){
			return '<li style="display:none;" data-type="'+node.type+'" data-node = "'+node.name+'" >'+
			'<a data-type="'+node.type+'" data-node = "'+node.name+'" href="javascript:void(0)" class="pull-right hidden"><i class="fa fa-share"></i></a>'+
			this.getCountDistribution(node)+
			this.getCheckbox(node)+
			' <span>'+node.name+'</span></li>';
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
		appendPopover : function(node,data){
			node.children('.nodebar').popover({
				trigger: 'hover',
				placement: "bottom",
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
		formatTree : function(){
			this.$('.tree > ul').attr('role', 'tree').find('ul').attr('role', 'group');
		    this.$('.tree').find('li:has(ul)').addClass('parent_li').attr('role', 'treeitem').find(' > span').attr('title', 'Expand this branch').attr("data-state","collapse").on('click', function(e) {
		        var children = $(this).parent('li.parent_li').find(' > ul > li');
		        if (children.is(':visible')) {
		            children.hide('fast');
		            $(this).attr('title', 'Expand this branch').attr("data-state","collapse").find(' > i').removeClass().addClass('fa fa-plus-circle');
		        } else {
		            children.show('fast');
		            $(this).attr('title', 'Collapse this branch').attr("data-state","expand").find(' > i').removeClass().addClass('fa fa-minus-circle');
		        }
		        e.stopPropagation();
		    });
		    //this.$('[data-toggle="tooltip"]').tooltip();
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
		onMainCheckboxClick : function(e){
			if(e.target.checked){
				this.$('.tree  input[type="checkbox"]').prop({"checked":true,"indeterminate":false});
				
			}else
				this.$('.tree  input[type="checkbox"]').prop({"checked":false,"indeterminate":false});
			var data = this.getCheckedHierarchyData();
			this.params.treeParams = _.extend({},data);
			this.vent.trigger("tree:search",{treeParams : JSON.stringify(data)});
			
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
			this.vent.trigger("tree:search",{treeParams : JSON.stringify(data)});
		},
		onNewTabIconClick : function(e){
			var $el = $(e.currentTarget),host,component,that=this;
			if($el.parents("[data-parent=true]")){
				host = $el.parents("[data-parent=true]").data("node");
				component = $el.data("node");
				that.globalVent.trigger("render:tab",/*new LogFileView(*/{
					params:_.extend({},{
						host_name :  host,
						component_name : component
					},that.searchParams,{treeParams:null}),
					globalVent : that.globalVent
				}/*)*/);
			}
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
		updateCount : function(){
			var that= this;
			this.$('.nodebar').popover('destroy');
			_.each(this.treeModel.get("vNodeList"),function(data){
				if(data.isParent  === "true" || data.isParent == true){
					var html = that.getLevelDistribution(data);
					that.$(".nodebar[data-node='"+data.name+"']").html("").append(html);
					var parent = that.$(".nodebar[data-node='"+data.name+"']").parent();
					that.appendPopover(parent,data);
					_.each(data.childs,function(c){
						html = that.getLevelDistribution(c);
						parent.find(".nodebar[data-node='"+c.name+"']").html("").append(html);
						that.appendPopover(parent.find("li[data-node='"+c.name+"']"),c);
					});
				}
				
			});
		},
		onSearchByName: function(e){
			var type = (e.currentTarget.id === 'nextSrch') ? 'next' : 'prev';
			if(!this.searchFlag)
				this.searchFlag = type;
			else if(this.searchFlag !== type){
				this.counter = (type === 'next' ? this.counter + 2 : this.counter - 2);
				this.searchFlag = type;
			}
			this.counter = Utils.scrollToSearchString(this.$el.find('.highlight'), type, this.counter, 237,this.$(".tree"));
		}
	});
	
	
});