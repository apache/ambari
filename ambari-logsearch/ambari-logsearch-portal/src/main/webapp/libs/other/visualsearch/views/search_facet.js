(function() {

var $ = jQuery; // Handle namespaced jQuery

// This is the visual search facet that holds the category and its autocompleted
// input field.
VS.ui.SearchFacet = Backbone.View.extend({

  type : 'facet',

  className : 'search_facet',

  events : {
    'click .category'           : 'selectFacet',
    'keydown input'             : 'keydown',
    'mousedown input'           : 'enableEdit',
    'mouseover .VS-icon-cancel' : 'showDelete',
    'mouseout .VS-icon-cancel'  : 'hideDelete',
    'click .VS-icon-cancel'     : 'remove'
  },

  initialize : function(options) {
    this.options = _.extend({}, this.options, options);

    this.flags = {
      canClose : false
    };
    _.bindAll(this, 'set', 'keydown', 'deselectFacet', 'deferDisableEdit');
    this.app = this.options.app;
  },

  // Rendering the facet sets up autocompletion, events on blur, and populates
  // the facet's input with its starting value.
  render : function() {
    $(this.el).html(JST['search_facet']({
      model : this.model,
      readOnly: this.app.options.readOnly
    }));

    this.setMode('not', 'editing');
    this.setMode('not', 'selected');
    this.box = this.$('input');
    this.box.val(this.model.label());
    this.box.bind('blur', this.deferDisableEdit);
    // Handle paste events with `propertychange`
    this.box.bind('input propertychange', this.keydown);
    this.setupAutocomplete();

    return this;
  },

  // This method is used to setup the facet's input to auto-grow.
  // This is defered in the searchBox so it can be attached to the
  // DOM to get the correct font-size.
  calculateSize : function() {
    this.box.autoGrowInput();
    this.box.unbind('updated.autogrow');
    this.box.bind('updated.autogrow', _.bind(this.moveAutocomplete, this));
  },

  // Forces a recalculation of this facet's input field's value. Called when
  // the facet is focused, removed, or otherwise modified.
  resize : function(e) {
    this.box.trigger('resize.autogrow', e);
  },

  // Watches the facet's input field to see if it matches the beginnings of
  // words in `autocompleteValues`, which is different for every category.
  // If the value, when selected from the autocompletion menu, is different
  // than what it was, commit the facet and search for it.
  setupAutocomplete : function() {
    this.box.autocomplete({
      source    : _.bind(this.autocompleteValues, this),
      minLength : 0,
      delay     : 0,
      autoFocus : true,
      position  : {offset : "0 5"},
      create    : _.bind(function(e, ui) {
        $(this.el).find('.ui-autocomplete-input').css('z-index','auto');
      }, this),
      select    : _.bind(function(e, ui) {
        e.preventDefault();
        var originalValue = this.model.get('value');
        this.set(ui.item.value);
        if (originalValue != ui.item.value || this.box.val() != ui.item.value) {
          if (this.app.options.autosearch) {
            this.search(e);
          } else {
              this.app.searchBox.renderFacets();
              this.app.searchBox.focusNextFacet(this, 1, {viewPosition: this.options.order});
          }
        }
        return false;
      }, this),
      open      : _.bind(function(e, ui) {
        var box = this.box;
        this.box.autocomplete('widget').find('.ui-menu-item').each(function() {
          var $value = $(this),
              autoCompleteData = $value.data('item.autocomplete') || $value.data('ui-autocomplete-item');

          if (autoCompleteData['value'] == box.val() && box.data('ui-autocomplete').menu.activate) {
            box.data('ui-autocomplete').menu.activate(new $.Event("mouseover"), $value);
          }
        });
      }, this)
    });

    this.box.autocomplete('widget').addClass('VS-interface');
  },

  // As the facet's input field grows, it may move to the next line in the
  // search box. `autoGrowInput` triggers an `updated` event on the input
  // field, which is bound to this method to move the autocomplete menu.
  moveAutocomplete : function() {
    var autocomplete = this.box.data('ui-autocomplete');
    if (autocomplete) {
      autocomplete.menu.element.position({
        my        : "left top",
        at        : "left bottom",
        of        : this.box.data('ui-autocomplete').element,
        collision : "flip",
        offset    : "0 5"
      });
    }
  },

  // When a user enters a facet and it is being edited, immediately show
  // the autocomplete menu and size it to match the contents.
  searchAutocomplete : function(e) {
    var autocomplete = this.box.data('ui-autocomplete');
    if (autocomplete) {
      var menu = autocomplete.menu.element;
      autocomplete.search();

      // Resize the menu based on the correctly measured width of what's bigger:
      // the menu's original size or the menu items' new size.
      menu.outerWidth(Math.max(
        menu.width('').outerWidth(),
        autocomplete.element.outerWidth()
      ));
    }
  },

  // Closes the autocomplete menu. Called on disabling, selecting, deselecting,
  // and anything else that takes focus out of the facet's input field.
  closeAutocomplete : function() {
    var autocomplete = this.box.data('ui-autocomplete');
    if (autocomplete) autocomplete.close();
  },

  // Search terms used in the autocomplete menu. These are specific to the facet,
  // and only match for the facet's category. The values are then matched on the
  // first letter of any word in matches, and finally sorted according to the
  // value's own category. You can pass `preserveOrder` as an option in the
  // `facetMatches` callback to skip any further ordering done client-side.
  autocompleteValues : function(req, resp) {
    var category = this.model.get('category');
    var value    = this.model.get('value');
    var searchTerm = req.term;

    this.app.options.callbacks.valueMatches(category, searchTerm, function(matches, options) {
      options = options || {};
      matches = matches || [];

      if (searchTerm && value != searchTerm) {
        if (options.preserveMatches) {
          resp(matches);
        } else {
          var re = VS.utils.inflector.escapeRegExp(searchTerm || '');
          var matcher = new RegExp('\\b' + re, 'i');
          matches = $.grep(matches, function(item) {
            return matcher.test(item) ||
                   matcher.test(item.value) ||
                   matcher.test(item.label);
        });
        }
      }

      if (options.preserveOrder) {
        resp(matches);
      } else {
        resp(_.sortBy(matches, function(match) {
          if (match == value || match.value == value) return '';
          else return match;
        }));
      }
    });

  },

  // Sets the facet's model's value.
  set : function(value) {
    if (!value) return;
    this.model.set({'value': value});
  },

  // Before the searchBox performs a search, we need to close the
  // autocomplete menu.
  search : function(e, direction) {
    if (!direction) direction = 1;
    this.closeAutocomplete();
    this.app.searchBox.searchEvent(e);
    _.defer(_.bind(function() {
      this.app.searchBox.focusNextFacet(this, direction, {viewPosition: this.options.order});
    }, this));
  },

  // Begin editing the facet's input. This is called when the user enters
  // the input either from another facet or directly clicking on it.
  //
  // This method tells all other facets and inputs to disable so it can have
  // the sole focus. It also prepares the autocompletion menu.
  enableEdit : function() {
    if (this.app.options.readOnly) return;
    if (this.modes.editing != 'is') {
      this.setMode('is', 'editing');
      this.deselectFacet();
      if (this.box.val() == '') {
        this.box.val(this.model.get('value'));
      }
    }

    this.flags.canClose = false;
    this.app.searchBox.disableFacets(this);
    this.app.searchBox.addFocus();
    _.defer(_.bind(function() {
      this.app.searchBox.addFocus();
    }, this));
    this.resize();
    this.searchAutocomplete();
    this.box.focus();
  },

  // When the user blurs the input, they may either be going to another input
  // or off the search box entirely. If they go to another input, this facet
  // will be instantly disabled, and the canClose flag will be turned back off.
  //
  // However, if the user clicks elsewhere on the page, this method starts a timer
  // that checks if any of the other inputs are selected or are being edited. If
  // not, then it can finally close itself and its autocomplete menu.
  deferDisableEdit : function() {
    this.flags.canClose = true;
    _.delay(_.bind(function() {
      if (this.flags.canClose && !this.box.is(':focus') &&
          this.modes.editing == 'is' && this.modes.selected != 'is') {
        this.disableEdit();
      }
    }, this), 250);
  },

  // Called either by other facets receiving focus or by the timer in `deferDisableEdit`,
  // this method will turn off the facet, remove any text selection, and close
  // the autocomplete menu.
  disableEdit : function() {
    var newFacetQuery = VS.utils.inflector.trim(this.box.val());
    if (newFacetQuery != this.model.get('value')) {
      this.set(newFacetQuery);
    }
    this.flags.canClose = false;
    this.box.selectRange(0, 0);
    this.box.blur();
    this.setMode('not', 'editing');
    this.closeAutocomplete();
    this.app.searchBox.removeFocus();
  },

  // Selects the facet, which blurs the facet's input and highlights the facet.
  // If this is the only facet being selected (and not part of a select all event),
  // we attach a mouse/keyboard watcher to check if the next action by the user
  // should delete this facet or just deselect it.
  selectFacet : function(e) {
    if (e) e.preventDefault();
    if (this.app.options.readOnly) return;
    var allSelected = this.app.searchBox.allSelected();
    if (this.modes.selected == 'is') return;

    if (this.box.is(':focus')) {
      this.box.setCursorPosition(0);
      this.box.blur();
    }

    this.flags.canClose = false;
    this.closeAutocomplete();
    this.setMode('is', 'selected');
    this.setMode('not', 'editing');
    if (!allSelected || e) {
      $(document).unbind('keydown.facet', this.keydown);
      $(document).unbind('click.facet', this.deselectFacet);
      _.defer(_.bind(function() {
        $(document).unbind('keydown.facet').bind('keydown.facet', this.keydown);
        $(document).unbind('click.facet').one('click.facet', this.deselectFacet);
      }, this));
      this.app.searchBox.disableFacets(this);
      this.app.searchBox.addFocus();
    }
    return false;
  },

  // Turns off highlighting on the facet. Called in a variety of ways, this
  // only deselects the facet if it is selected, and then cleans up the
  // keyboard/mouse watchers that were created when the facet was first
  // selected.
  deselectFacet : function(e) {
    if (e) e.preventDefault();
    if (this.modes.selected == 'is') {
      this.setMode('not', 'selected');
      this.closeAutocomplete();
      this.app.searchBox.removeFocus();
    }
    $(document).unbind('keydown.facet', this.keydown);
    $(document).unbind('click.facet', this.deselectFacet);
    return false;
  },

  // Is the user currently focused in this facet's input field?
  isFocused : function() {
    return this.box.is(':focus');
  },

  // Hovering over the delete button styles the facet so the user knows that
  // the delete button will kill the entire facet.
  showDelete : function() {
    $(this.el).addClass('search_facet_maybe_delete');
  },

  // On `mouseout`, the user is no longer hovering on the delete button.
  hideDelete : function() {
    $(this.el).removeClass('search_facet_maybe_delete');
  },

  // When switching between facets, depending on the direction the cursor is
  // coming from, the cursor in this facet's input field should match the original
  // direction.
  setCursorAtEnd : function(direction) {
    if (direction == -1) {
      this.box.setCursorPosition(this.box.val().length);
    } else {
      this.box.setCursorPosition(0);
    }
  },

  // Deletes the facet and sends the cursor over to the nearest input field.
  remove : function(e) {
    var committed = this.model.get('value');
    this.deselectFacet();
    this.disableEdit();
    this.app.searchQuery.remove(this.model);
    if (committed && this.app.options.autosearch) {
      this.search(e, -1);
    } else {
      this.app.searchBox.renderFacets();
      this.app.searchBox.focusNextFacet(this, -1, {viewPosition: this.options.order});
    }
  },

  // Selects the text in the facet's input field. When the user tabs between
  // facets, convention is to highlight the entire field.
  selectText: function() {
    this.box.selectRange(0, this.box.val().length);
  },

  // Handles all keyboard inputs when in the facet's input field. This checks
  // for movement between facets and inputs, entering a new value that needs
  // to be autocompleted, as well as the removal of this facet.
  keydown : function(e) {
    var key = VS.app.hotkeys.key(e);

    if (key == 'enter' && this.box.val()) {
      this.disableEdit();
      this.search(e);
    } else if (key == 'left') {
      if (this.modes.selected == 'is') {
        this.deselectFacet();
        this.app.searchBox.focusNextFacet(this, -1, {startAtEnd: -1});
      } else if (this.box.getCursorPosition() == 0 && !this.box.getSelection().length) {
        this.selectFacet();
      }
    } else if (key == 'right') {
      if (this.modes.selected == 'is') {
        e.preventDefault();
        this.deselectFacet();
        this.setCursorAtEnd(0);
        this.enableEdit();
      } else if (this.box.getCursorPosition() == this.box.val().length) {
        e.preventDefault();
        this.disableEdit();
        this.app.searchBox.focusNextFacet(this, 1);
      }
    } else if (VS.app.hotkeys.shift && key == 'tab') {
      e.preventDefault();
      this.app.searchBox.focusNextFacet(this, -1, {
        startAtEnd  : -1,
        skipToFacet : true,
        selectText  : true
      });
    } else if (key == 'tab') {
      e.preventDefault();
      this.app.searchBox.focusNextFacet(this, 1, {
        skipToFacet : true,
        selectText  : true
      });
    } else if (VS.app.hotkeys.command && (e.which == 97 || e.which == 65)) {
      e.preventDefault();
      this.app.searchBox.selectAllFacets();
      return false;
    } else if (VS.app.hotkeys.printable(e) && this.modes.selected == 'is') {
      this.app.searchBox.focusNextFacet(this, -1, {startAtEnd: -1});
      this.remove(e);
    } else if (key == 'backspace') {
       $(document).on('keydown.backspace', function(e) {
          if (VS.app.hotkeys.key(e) === 'backspace') {
             e.preventDefault();
          }
       });

       $(document).on('keyup.backspace', function(e) {
          $(document).off('.backspace');
       });

      if (this.modes.selected == 'is') {
        e.preventDefault();
        this.remove(e);
      } else if (this.box.getCursorPosition() == 0 &&
                 !this.box.getSelection().length) {
        e.preventDefault();
        this.selectFacet();
      }
       e.stopPropagation();
    }

    // Handle paste events
    if (e.which == null) {
        // this.searchAutocomplete(e);
        _.defer(_.bind(this.resize, this, e));
    } else {
      this.resize(e);
    }
  }

});

})();
