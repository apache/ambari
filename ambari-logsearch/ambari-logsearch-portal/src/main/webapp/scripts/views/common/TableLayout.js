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
/**
 * @file This is the common View file for displaying Table/Grid to be used overall in the application.
 */
define(['require',
	'backbone',
	'hbs!tmpl/common/TableLayout_tmpl',
	'backgrid-filter',
	'backgrid-paginator',
	'backgrid-columnmanager',
	'backgrid-sizeable',
	'backgrid-orderable'
],function(require,Backbone,FSTablelayoutTmpl){
    'use strict';
	
	var FSTableLayout = Backbone.Marionette.Layout.extend(
	/** @lends FSTableLayout */
	{
		_viewName : 'FSTableLayout',

    	template: FSTablelayoutTmpl,

		/** Layout sub regions */
    	regions: {
			'rTableList'	: 'div[data-id="r_tableList"]',
			'rTableSpinner'	: 'div[data-id="r_tableSpinner"]',
			'rPagination'	: 'div[data-id="r_pagination"]',
			'rFooterRecords': 'div[data-id="r_footerRecords"]'
		},

    	// /** ui selector cache */
    	ui: {
    		selectPageSize :'select[data-id="pageSize"]'
    	},

		gridOpts : {
			className: 'table table-bordered table-hover table-condensed backgrid',
			emptyText : 'No Records found!'
		},

		/**
		 * Backgrid.Filter default options
		 */
		filterOpts : {
			placeholder: 'plcHldr.searchByResourcePath',
			wait: 150
		},

		/**
		 * Paginator default options
		 */
		paginatorOpts : {
			// If you anticipate a large number of pages, you can adjust
			// the number of page handles to show. The sliding window
			// will automatically show the next set of page handles when
			// you click next at the end of a window.
			windowSize: 10, // Default is 10

			// Used to multiple windowSize to yield a number of pages to slide,
			// in the case the number is 5
			slideScale: 0.5, // Default is 0.5

			// Whether sorting should go back to the first page
			goBackFirstOnSort: false // Default is true
		},

		/**
	       page handlers for pagination
	    */
		controlOpts : {
			rewind: {
				label: "&#12298;",
				title: "First"
			},
			back: {
				label: "&#12296;",
				title: "Previous"
			},
			forward: {
				label: "&#12297;",
				title: "Next"
			},
			fastForward: {
				label: "&#12299;",
				title: "Last"
			}
	    },
	    columnOpts : {
	    	initialColumnsVisible: 4,
		    // State settings
		    saveState: false,
		    loadStateOnInit: true
	    },

		includePagination : true,

		includeFilter : false,

		includeHeaderSearch : false,

		includePageSize : false,

		includeFooterRecords : true,
		
		includeColumnManager : false,
		
		//includeSizeAbleColumns : false,

		/** ui events hash */
		events: function() {
			var events = {};
			events['change ' + this.ui.selectPageSize]  = 'onPageSizeChange';
			return events;
		},

    	/**
		* intialize a new HDFSTableLayout Layout
		* @constructs
		*/
		initialize: function(options) {
			_.extend(this, _.pick(options,	'collection', 'columns', 'includePagination',
											'includeHeaderSearch', 'includeFilter', 'includePageSize',
											'includeFooterRecords','includeColumnManager','includeSizeAbleColumns'));

			_.extend(this.gridOpts, options.gridOpts, {collection : this.collection, columns : this.columns});
			_.extend(this.filterOpts, options.filterOpts);
			_.extend(this.paginatorOpts, options.paginatorOpts);
			_.extend(this.controlOpts, options.controlOpts);
			_.extend(this.columnOpts, options.columnOpts);
			
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
            this.listenTo(this.collection, 'request', function(){
				this.$('div[data-id="r_tableSpinner"]').addClass('loading');
			},this);
            this.listenTo(this.collection, 'sync error', function(){
				this.$('div[data-id="r_tableSpinner"]').removeClass('loading');
			},this);

			this.listenTo(this.collection, 'reset', function(collection, response){
				if(this.includePagination) {
					this.renderPagination();
				}
				if(this.includeFooterRecords){
					this.renderFooterRecords(this.collection.state);
				}
			},this);

			/*This "sort" trigger event is fired when clicked on
			'sortable' header cell (backgrid).
			Collection.trigger event was fired because backgrid has
			removeCellDirection function (backgrid.js - line no: 2088)
			which is invoked when "sort" event is triggered
			on collection (backgrid.js - line no: 2081).
			removeCellDirection function - removes "ascending" and "descending"
			which in turn removes chevrons from every 'sortable' header-cells*/
			this.listenTo(this.collection, "backgrid:sort", function(){
				this.collection.trigger("sort");
			});

			/*this.listenTo(this.collection, 'remove', function(model, collection, response){
				if (model.isNew() || !this.includePagination) {
					return;
				}
				if (this.collection.state && this.collection.state.totalRecords>0) {
					this.collection.state.totalRecords-=1;
				}
				if (this.collection.length===0 && this.collection.state && this.collection.state.totalRecords>0) {

					if (this.collection.state.totalRecords>this.collection.state.currentPage*this.collection.state.pageSize) {
						this.collection.fetch({reset:true});
					} else {
						if (this.collection.state.currentPage>0) {
							this.collection.state.currentPage-=1;
							this.collection.fetch({reset:true});
						}
					}

				} else if (this.collection.length===0 && this.collection.state && this.collection.state.totalRecords===0) {
					this.collection.state.currentPage=0;
					this.collection.fetch({reset:true});
				}
			}, this);*/

			// It will show tool tip when td has ellipsis  Property
			this.listenTo(this.collection, "backgrid:refresh", function(){
				/*this.$('.table td').bind('mouseenter', function() {
		            var $this = $(this);
					if (this.offsetWidth < this.scrollWidth && !$this.attr('title')) {
		                $this.attr('title', $this.text());
		            }
				});*/
			},this);

		},

		/** on render callback */
		onRender: function() {
			this.renderTable();
			if(this.includePagination) {
				this.renderPagination();
			}
			if(this.includeFilter){
				this.renderFilter();
			}
			if(!this.includePageSize){
				this.ui.selectPageSize.remove();
			}
			if(this.includeFooterRecords){
				this.renderFooterRecords(this.collection.state);
			}
			if(this.includeColumnManager){
				this.renderColumnManager();
			}
//			if(this.includeSizeAbleColumns){
//				this.renderSizeAbleColumns();
//			}
			this.$('[data-id="pageSize"]').val(this.collection.state.pageSize);
			
						
		},
		renderOrder : function(){
			// Setup sortable column collection
			
			var columns = this.columns;
			//columns.setPositions().sort();
			
			// Add sizeable columns
			var sizeAbleCol = new Backgrid.Extension.SizeAbleColumns({
				grid: this.getGridObj(),
				collection: this.collection,
				columns: columns
			});
			this.$('thead').before(sizeAbleCol.render().el);

			// Make columns reorderable
			var orderHandler = new Backgrid.Extension.OrderableColumns({
			  grid: this.getGridObj(),
			  sizeAbleColumns: sizeAbleCol
			});
			this.$('thead').before(orderHandler.render().el);

		},
		
		/**
		 * show table
		 */
		renderTable : function(){
			var that = this;
			this.rTableList.show(new Backgrid.Grid(this.gridOpts));
		},

		/**
		 * show pagination buttons(first, last, next, prev and numbers)
		 */
		renderPagination : function(){
			var options = _.extend({
				collection: this.collection,
				controls: this.controlOpts
			}, this.paginatorOpts);

			// TODO - Debug this part
			if(this.rPagination){
				this.rPagination.show(new Backgrid.Extension.Paginator(options));
			}
			else if(this.regions.rPagination){
				this.$('div[data-id="r_pagination"]').show(new Backgrid.Extension.Paginator(options));
			}
		},

		/**
		 * show/hide pagination buttons of the grid
		 */
		showHidePager : function(){

			if(!this.includePagination) {
				return;
			}

			if(this.collection.state && this.collection.state.totalRecords > this.collection.state.pageSize) {
				this.$('div[data-id="r_pagination"]').show();
			} else {
				this.$('div[data-id="r_pagination"]').hide();
			}
		},

		/**
		 * show/hide filter of the grid
		 */
		renderFilter : function(){
			this.rFilter.show(new Backgrid.Extension.ServerSideFilter({
				  collection: this.collection,
				  name: ['name'],
				  placeholder: 'plcHldr.searchByResourcePath',
				  wait: 150
			}));

			setTimeout(function(){
				that.$('table').colResizable({liveDrag :true});
			},0);
		},

		/**
		 * show/hide footer details of the list/collection shown in the grid
		 */
		renderFooterRecords : function(collectionState){
			var collState 		= collectionState;
			var totalRecords 	= collState.totalRecords || 0;
			var pageStartIndex 	= totalRecords ? (collState.currentPage*collState.pageSize) : 0;
			var pageEndIndex 	= pageStartIndex+this.collection.length;

			this.$('[data-id="r_footerRecords"]').html('Showing '+(totalRecords ? pageStartIndex+1 : 0)+' to '+pageEndIndex+' of '+totalRecords+' entries');
			return this;
		},
		/**
		 * ColumnManager for the table
		 */
		renderColumnManager : function(){
			var $el = this.$("[data-id='control']");
			var colManager = new Backgrid.Extension.ColumnManager(this.columns, this.columnOpts);
			// Add control
			var colVisibilityControl = new Backgrid.Extension.ColumnManagerVisibilityControl({
			    columnManager: colManager
			});

			$el.append(colVisibilityControl.render().el);
		},
		
		/*renderSizeAbleColumns : function(){
			// Add sizeable columns
			var sizeAbleCol = new Backgrid.Extension.SizeAbleColumns({
			  collection: this.collection,
			  columns: this.columns,
			  grid :this.getGridObj() 
			});
			this.$('thead').before(sizeAbleCol.render().el);

			// Add resize handlers
			var sizeHandler = new Backgrid.Extension.SizeAbleColumnsHandlers({
			  sizeAbleColumns: sizeAbleCol,
			  grid: this.getGridObj(),
			  saveModelWidth: true
			});
			this.$('thead').before(sizeHandler.render().el);

			// Listen to resize events
			this.columns.on('resize', function(columnModel, newWidth, oldWidth) {
			  console.log('Resize event on column; name, model, new and old width: ', columnModel.get("name"), columnModel, newWidth, oldWidth);
			});
		},/*

		/** on close */
		onClose: function(){
		},

		/**
		 * get the Backgrid object
		 * @return {null}
		 */
		getGridObj : function(){
			if (this.rTableList.currentView){
				return this.rTableList.currentView;
			}
			return null;
		},

		/**
		 * handle change event on page size select box
		 * @param  {Object} e event
		 */
		onPageSizeChange : function(e){
			var pagesize = $(e.currentTarget).val();
			this.collection.state.pageSize = parseInt(pagesize,10);

			this.collection.state.currentPage = this.collection.state.firstPage;
			delete this.collection.queryParams.lastPage;
			this.collection.fetch({
				sort: false,
				reset: true,
				cache: false
			});
		}
	});

	return FSTableLayout;
});
