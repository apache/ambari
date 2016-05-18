/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 * 
 * BreadCrumbs Veiw  
 * BreadCrumbs is directly accessible through App object .But can also create a instance of it.
 * @array 
 */

define(['require',
	'backbone.marionette',
	'hbs!tmpl/common/breadcrumbs',
	'App'
],function(require,Marionette,tmpl,App) {


	var BreadCrumbs = Marionette.ItemView.extend({
		template : tmpl,
		templateHelpers : function(){
			return {
				breadcrumb : this.setLast(this.breadcrumb)
			};
		},
		initialize : function(options){
			this.breadcrumb = [];
			if(typeof options !== 'undefined'){
				this.breadcrumb = options.breadcrumb;
			}
			//In docs the breadcrubs region stays constant only inner content changes
	/*		this.listenTo(Vent,'draw:docs:breadcrumbs',function(breads){
				this.breadcrumb = breads;
				this.reRenderBookmarks();
			},this);*/
		},
		onRender : function(){

		},
		reRenderBookmarks : function(){
			this.breadcrumb = this.setLast(this.breadcrumb);
			this.render();
		},
		setLast : function(breadcrumb){
			if(breadcrumb.length > 0){
				//breadcrumb[breadcrumb.length -1].isLast = true;
				breadcrumb[breadcrumb.length -1] = _.extend({},breadcrumb[breadcrumb.length -1],{isLast : true});
			}
			return breadcrumb;
		},
		// showBreadCrumbs : function(view,breadcrumb){
			// var brRgn = view.$el.find('#brdCrumbs');
			// if(brRgn){
				// $(brRgn).html(Marionette.Renderer.render(tmpl,{breadcrumb : this.setLast(breadcrumb)}));	
			// }/*else{*/
			   // ////throw new Error('This view does not have a  #brdCrumbs id'); 
			// /*}*/
		// },
		onClose : function(){       
			console.log('OnItemClosed called of BreadCrumbs');
		//	this.stopListening(Vent);
		}
	});

	App.BreadCrumbs = new BreadCrumbs();	

	return BreadCrumbs;
});
