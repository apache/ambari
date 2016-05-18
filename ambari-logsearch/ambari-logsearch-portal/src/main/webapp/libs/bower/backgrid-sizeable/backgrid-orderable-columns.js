/*
 backgrid-orderable-columns
 https://github.com/WRidder/backgrid-orderable-columns

 Copyright (c) 2014 Wilbert van de Ridder
 Licensed under the MIT @license.
 */
(function (root, factory) {
  // CommonJS
  if (typeof exports == "object") {
    module.exports = factory(require("underscore"), require("backgrid"), require("jquery"));
  }
  // AMD. Register as an anonymous module.
  else if (typeof define === 'function' && define.amd) {
    define(['underscore', 'backgrid', 'jquery'], factory);
  }
  // Browser
  else {
    factory(root._, root.Backgrid, root.jQuery);
  }
}(this, function (_, Backgrid, $) {
  "use strict";

  // Adds width support to columns
  Backgrid.Extension.OrderableColumns = Backbone.View.extend({
    dragHooks: {},

    /**
     * Initializer
     * @param options
     */
    initialize: function (options) {
      this.sizeAbleColumns = options.sizeAbleColumns;
      this.grid = this.sizeAbleColumns.grid;
      this.columns = this.grid.columns;
      this.header = this.grid.header;
      this.collection = this.grid.collection;
      this.moveThreshold = options.moveThreshold || 10;
      this.orderAlignTop = options.orderAlignTop;

      this.attachEvents();
      this.setHeaderElements();

      // Set scope handlers
      this.mouseMoveHandler = _.bind(this.mouseMoveHandler, this);
      this.mouseUpHandler = _.bind(this.mouseUpHandler, this);
    },

    /**
     * Adds handlers to reorder the columns
     * @returns {Backgrid.Extension.OrderableColumns}
     */
    render: function () {
      var self = this;
      self.$el.empty();

      // Create indicators
      self.addIndicators();

      // Loop all rows
      var headerRows = self.header.headerRows || [self.header.row];
      _.each(headerRows, function (row) {
        // Loop cells of row
        _.each(row.cells, function (cell) {
          // Get column model
          var columnModel = cell.column;

          // Attach handler if main orderable cell or has child
          var orderable = false;
          if (!columnModel.get("childColumns")) {
            orderable = typeof columnModel.get("orderable") == "undefined" || columnModel.get("orderable");
          }
          else {
            // Parent element is orderable if any of the children is orderable
            orderable = _.some(columnModel.get("childColumns"), function(child) {
              var childColumnModel = child.column;
              return typeof childColumnModel.get("orderable") == "undefined" || childColumnModel.get("orderable");
            });
          }

          // If orderable, add handler
          if (orderable) {
            cell.$el.on("mousedown",
              _.bind(self.mouseDownHandler, {
                view: self,
                cell: cell,
                column: columnModel
              })
            );
          }
        });
      });

      // Position drag handlers
      self.updateIndicatorPosition();

      return this;
    },

    /**
     * Drag state object
     */
    dragState: {
      dragIntention: false,
      dragging: false,
      $dragElement: null,
      $activeIndicator: null,
      column: null,
      cell: null,
      coordinateElementStartX: null,
      coordinatePointerStartX: null,
      oldFirstDisplayOrderValue: null,
      oldLastDisplayOrderValue: null,
      newDisplayOrderValue: null,
      orderPrevented: null
    },

    mouseDownHandler: function(evt) {
      var self = this.view;
      var cell = this.cell;
      var column = this.column;
      var $headerElement = $(cell.$el);

      // Check if left-click
      if (evt.which === 1) {
        self._stopEvent(evt);

        // Set drag state
        self.dragState.dragIntention = true;
        self.dragState.column = column;
        self.dragState.cell = cell;
        self.dragState.coordinatePointerStartX = evt.pageX;
        self.dragState.coordinateElementStartX = $headerElement.position().left;

        if (column.get("childColumns")) {
          self.dragState.oldFirstDisplayOrderValue = _.first(column.get("childColumns")).column.get("displayOrder");
          self.dragState.newDisplayOrderValue = _.first(column.get("childColumns")).column.get("displayOrder");
          self.dragState.oldLastDisplayOrderValue = _.last(column.get("childColumns")).column.get("displayOrder");
        }
        else {
          self.dragState.oldFirstDisplayOrderValue = column.get("displayOrder");
          self.dragState.newDisplayOrderValue = column.get("displayOrder");
          self.dragState.oldLastDisplayOrderValue = column.get("displayOrder");
        }

        // Create copy of column element
        self.dragState.$dragElement = $("<div/>")
          .addClass("orderable-draggable")
          .hide()
          .appendTo(self.$el)
          .width($headerElement.outerWidth())
          .height($headerElement.outerHeight())
          .css({
            left: $headerElement.position().left,
            top: $headerElement.position().top
          });

        // Add move and mouse up handler
        $(document).on("mousemove", self.mouseMoveHandler);
        $(document).on("mouseup", self.mouseUpHandler);
      }
    },

    /**
     * Mouse move event handler
     * @param evt
     */
    mouseMoveHandler: function(evt) {
      var self = this;
      var pageX = evt.pageX;
      var leftPosition = self.dragState.coordinateElementStartX + (pageX - self.dragState.coordinatePointerStartX);
      self._stopEvent(evt);
      var delta = Math.abs(pageX - self.dragState.coordinatePointerStartX);

      if (self.dragState.dragging) {
        // Highlight nearest indicator
        self.calculateDropPosition(leftPosition, evt);

        // Notify drag hooks
        self.dragHookInvoke("dragMove", self.dragState.$dragElement, evt, self.dragState.column);

        // Set draggable eleent position
        self.dragState.$dragElement.css({
          left: leftPosition
        });
      }
      // Only move beyond threshold
      else if (delta >= self.moveThreshold && !self.dragState.dragging) {
        self.dragState.cell.$el.addClass("orderable-ordering");
        self.dragState.dragging = true;

        // Notify drag hooks
        self.dragHookInvoke("dragStart", evt, self.dragState.column);

        // Show and position drag element
        self.dragState.$dragElement.css({
          left: leftPosition
        }).show();
      }
    },

    /**
     * Mouse up event handler
     * @param evt
     */
    mouseUpHandler: function(evt) {
      var self = this;

      // Remove handlers
      $(document).off("mousemove", self.mouseMoveHandler);
      $(document).off("mouseup", self.mouseUpHandler);

      // Notify drag hooks
      self.dragHookInvoke("dragEnd", evt, self.dragState.column);

      // Check if the columns have actually been re-ordered
      if (!self.dragState.orderPrevented &&
        self.dragState.oldFirstDisplayOrderValue !== self.dragState.newDisplayOrderValue) {

        // Update positions
        self.updateDisplayOrders();

        // Trigger event indicating column reordering
        self.columns.trigger("ordered");

        // Sort columns
        self.columns.sort();
      }

      // Reset drag state
      self.resetDragState();
    },

    /**
     * Find the drop position for the current position of the dragged header element
     * @param leftPosition
     * @param evt
     */
    calculateDropPosition: function(leftPosition, evt) {
      var self = this;

      // Find closest indicator
      var closest = null;
      var $closestIndicator = null;
      _.each(self.indicatorPositions, function (indicator, displayOrder) {
        if (closest == null ||
          Math.abs(indicator.x - leftPosition) < Math.abs(closest - leftPosition) &&
          (displayOrder <= self.dragState.oldFirstDisplayOrderValue || displayOrder > self.dragState.oldLastDisplayOrderValue + 1)
        ) {
          closest = indicator.x;
          $closestIndicator = indicator.$el;
        }
      });

      // Set active class on current indicator
      if ($closestIndicator !== self.dragState.$activeIndicator) {
        if (self.dragState.$activeIndicator) {
          self.dragState.$activeIndicator.removeClass('orderable-indicator-active');
        }
      }

      // Check if the move is valid
      if (!self.dragHookPreventOrder(self.dragState.$dragElement, evt, self.dragState.column)) {
        // Set active class on current indicator
        if ($closestIndicator !== self.dragState.$activeIndicator) {
          if (self.dragState.$activeIndicator) {
            self.dragState.$activeIndicator.removeClass('orderable-indicator-active');
          }
          self.dragState.$activeIndicator = $closestIndicator;
          $closestIndicator.addClass('orderable-indicator-active');

          // Save new order
          self.dragState.newDisplayOrderValue = $closestIndicator.data("column-displayOrder");
        }
        self.dragState.orderPrevented = false;
      }
      else {
        self.dragState.orderPrevented = true;
      }
    },

    /**
     * Calculates displayOrder attributes for columns after re-ordering
     */
    updateDisplayOrders: function() {
      var self = this;
      var oldFirstDO = self.dragState.oldFirstDisplayOrderValue;
      var oldLastDO = self.dragState.oldLastDisplayOrderValue;
      var newDO = self.dragState.newDisplayOrderValue;
      var movedRight = oldFirstDO < newDO;
      var span = (oldLastDO - oldFirstDO) + 1;
      var positionShift = (movedRight) ? (newDO - oldFirstDO - span) : (oldFirstDO - newDO);

      // Update position attributes
      self.columns.each(function (model) {
        var mDO = model.get("displayOrder");
        var nDO = mDO;
        if (movedRight) {
          if (mDO > oldLastDO && mDO < newDO) {
            nDO = mDO - span;
          }
          else if (mDO >= oldFirstDO && mDO <= oldLastDO) {
            nDO = mDO + positionShift;
          }
        }
        else {
          if (mDO >= newDO && mDO < oldFirstDO) {
            nDO = mDO + span;
          }
          else if (mDO >= oldFirstDO && mDO <= oldLastDO) {
            nDO = mDO - positionShift;
          }
        }

        // Update displayOrder value
        if (mDO !== nDO) {
          model.set("displayOrder", nDO, {silent: true});
        }
      });
    },

    /**
     * Reset drag state
     */
    resetDragState: function() {
      this.dragState.dragging = false;
      this.dragState.dragIntention = false;
      if (this.dragState.cell) {
        this.dragState.cell.$el.removeClass("orderable-ordering");
        this.dragState.cell = null;
      }
      if (this.dragState.$dragElement) {
        this.dragState.$dragElement.remove();
        this.dragState.$dragElement = null;
      }
      if (this.dragState.$activeIndicator) {
        this.dragState.$activeIndicator.removeClass('orderable-indicator-active');
      }
      this.dragState.$activeIndicator = null;
      this.dragState.column = null;
      this.dragState.coordinateElementStartX = null;
      this.dragState.coordinatePointerStartX = null;
      this.dragState.orderPrevented = null;
    },

    /**
     * Adds indicators which will show at which spot the column will be placed while dragging
     * @private
     */
    addIndicators: function () {
      var self = this;
      self.indicators = [];

      var previousIndicators = false;
      var previousDisplayOrder = 0;
      var previousRealDisplayOrder = 0;
      _.each(self.headerCells, function (headerCell) {
        var model = headerCell.column;
        if (previousIndicators || model.get("orderable")) {
          var DO = model.get("displayOrder");

          if (!previousIndicators) {
            previousDisplayOrder = previousRealDisplayOrder = DO - 1;
          }

          // Check whether to add columns after front or tail when gaps exist.
          if (!self.orderAlignTop && DO !== previousDisplayOrder + 1 && previousDisplayOrder === previousRealDisplayOrder) {
            DO = previousDisplayOrder + 1;
          }
          self.$el.append(self.createIndicator(DO, headerCell));

          // This boolean is used to see to what extend we can omit indicators upfront
          previousIndicators = true;
          previousDisplayOrder = DO;
          previousRealDisplayOrder = model.get("displayOrder");
        }
      });

      // Add trailing indicator
      if (!_.isEmpty(self.headerCells) && _.last(self.headerCells).column.get("orderable")) {
        self.$el.append(self.createIndicator(_.last(self.headerCells).column.get("displayOrder") + 1, null));
      }

      // Set indicator height
      self.setIndicatorHeight(self.grid.header.$el.height());
    },

    /**
     * Create a single indicator
     * @param {Integer} displayOrder
     * @returns {*|JQuery|any|jQuery}
     * @private
     */
    createIndicator: function (displayOrder, cell) {
      var self = this;

      // Create helper elements
      var $indicator = $("<div></div>")
        .addClass("orderable-indicator")
        .data("column-cell", cell)
        .data("column-displayOrder", displayOrder);
      self.indicators.push($indicator);

      return $indicator;
    },

    /**
     * Updates the position of all handlers
     * @private
     */
    updateIndicatorPosition: function () {
      var self = this;
      self.indicatorPositions = {};

      _.each(self.indicators, function ($indicator, indx) {
        var cell = $indicator.data("column-cell");
        var displayOrder = $indicator.data("column-displayOrder");

        var left;
        if (cell) {
          left = cell.$el.position().left;
        }
        else {
          var prevCell = self.indicators[indx - 1].data("column-cell");
          left = prevCell.$el.position().left + prevCell.$el.width();
        }
        self.indicatorPositions[displayOrder] = {
          x: left,
          $el: $indicator
        };

        // Get handler for current column and update position
        $indicator.css("left", left);
      });
      self.setIndicatorHeight();
    },

    /**
     * Sets height of all indicators matching the table header
     * @private
     */
    setIndicatorHeight: function () {
      this.$el.children().height(this.grid.header.$el.height());
    },

    /**
     * Attach event handlers
     * @private
     */
    attachEvents: function () {
      var self = this;
      self.listenTo(self.columns, "resize", self.handleColumnResize);
      self.listenTo(self.columns, "remove", self.handleColumnRemove);
      self.listenTo(self.columns, "sort", self.handleColumnSort);
      self.listenTo(self.grid.collection, "backgrid:colgroup:updated", self.updateIndicatorPosition);
      self.listenTo(self.grid.collection, "backgrid:colgroup:changed", self.handleHeaderRender);

      // Listen to window resize events
      var resizeEvtHandler = _.debounce(_.bind(self.updateIndicatorPosition, self), 250);
      self.listenTo(self._asEvents(window), "resize", resizeEvtHandler);
    },

    /**
     * Handlers when columns are resized
     * @private
     */
    handleColumnResize: function () {
      var self = this;
      self.updateIndicatorPosition();
      self.setIndicatorHeight();
    },

    /**
     * Handler when header is (re)rendered
     * @private
     */
    handleHeaderRender: function () {
      var self = this;
      // Wait for callstack to be cleared
      _.defer(function () {
        self.setHeaderElements();
        self.render();
        self.updateIndicatorPosition();
      });
    },

    /**
     * Handler for when a column is removed
     * @param {Backgrid.Column} model
     * @param {Backgrid.Columns} collection
     * @private
     */
    handleColumnRemove: function (model, collection) {
      // Get position of removed model
      var removedPosition = model.get("displayOrder");

      // Update position values of models
      collection.each(function (mod) {
        if (mod.get("displayOrder") > removedPosition) {
          mod.set("displayOrder", mod.get("displayOrder") - 1, {silent: true});
        }
      });
    },

    /**
     * Handler when the column collection is sorted
     * @private
     */
    handleColumnSort: function() {
      // Refresh body
      this.grid.body.refresh();
    },

    /**
     * Finds and saves current column header elements
     * @private
     */
    setHeaderElements: function () {
      var self = this;
      var rows = self.header.headerRows || [self.header.row];
      self.headerCells = [];

      // Loop all rows
      _.each(rows, function (row) {
        // Loop cells of row
        _.each(row.cells, function (cell) {
          var columnModel = self.columns.get({cid: cell.column.cid});
          if (!_.isEmpty(columnModel)) {
            self.headerCells.push({
              $el: cell.$el,
              el: cell.el,
              column: columnModel
            });
          }
        });
      });

      // Sort cells
      var headerCells = _.sortBy(self.headerCells, function (cell) {
        return self.columns.indexOf(cell.column);
      });

      // Filter cells
      self.headerCells = _.filter(headerCells, function (cell) {
        return cell.column.get("renderable") === true ||
          typeof cell.column.get("renderable") === "undefined"
      });

      self.headerElements = _.map(self.headerCells, function (cell) {
        return cell.el;
      });
    },

    /**
     * Adds a drag hook
     * @param {String} id
     * @param {Function} hook
     */
    addDragHook: function (id, hook) {
      this.dragHooks[id] = hook;
    },

    /**
     * Removes a drag hook
     * @param {String} id
     */
    removeDragHook: function (id) {
      if (this.dragHooks.hasOwnProperty(id)) {
        delete this.dragHooks[id];
      }
    },

    /**
     * Invokes a drag hook
     * @param {String} key
     * @private
     */
    dragHookInvoke: function (key) {
      var args = [].slice.apply(arguments);
      args.shift();
      _.each(this.dragHooks, function (obj) {
        if (typeof obj[key] == "function") {
          obj[key].apply(obj, args);
        }
      });
    },

    /**
     * Checks whether the ordering should be prevented
     * @returns {boolean}
     * @private
     */
    dragHookPreventOrder: function () {
      var prevent = false;
      _.each(this.dragHooks, function (obj) {
        if (typeof obj.preventOrder == "function") {
          prevent |= obj.preventOrder();
        }
      });
      return prevent;
    },

    /**
     * Helper function to stop event propagation
     * @param e
     * @private
     */
    _stopEvent: function (e) {
      if (e.stopPropagation) {
        e.stopPropagation();
      }
      if (e.preventDefault) {
        e.preventDefault();
      }
      e.cancelBubble = true;
      e.returnValue = false;
    },

    /**
     * Use Backbone Events listenTo/stopListening with any DOM element
     *
     * @param {DOM Element}
     * @return {Backbone Events style object}
     **/
    _asEvents: function(el) {
      var args;
      return {
        on: function(event, handler) {
          if (args) throw new Error("this is one off wrapper");
          el.addEventListener(event, handler, false);
          args = [event, handler];
        },
        off: function() {
          el.removeEventListener.apply(el, args);
        }
      };
    }
  });

  /**
   * Extendable
   * @type {Function}
   */
  var orderableDragHook = Backgrid.Extension.OrderableDragHook = function () {
    this.initialize.apply(this, arguments);
  };

  /**
   *  Prototype for the drag hook
   */
  _.extend(orderableDragHook.prototype, {
    initialize: function () {
    },
    dragStart: function () {
    },
    dragMove: function () {
    },
    dragEnd: function () {
    },
    preventOrder: function () {
    }
  });

  /**
   * Sample collection for orderable columns
   */
  Backgrid.Extension.OrderableColumns.orderableColumnCollection = Backgrid.Columns.extend({
    sortKey: "displayOrder",
    comparator: function (item) {
      return item.get(this.sortKey) || 1e6;
    },
    setPositions: function () {
      _.each(this.models, function (model, index) {
        // If a displayOrder is defined already, do not touch
        model.set("displayOrder", model.get("displayOrder") || index + 1, {silent: true});
      });
      return this;
    }
  });
}));
