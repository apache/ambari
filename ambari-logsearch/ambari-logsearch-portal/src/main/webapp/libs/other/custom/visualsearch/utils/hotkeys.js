(function() {

var $ = jQuery; // Handle namespaced jQuery

// DocumentCloud workspace hotkeys. To tell if a key is currently being pressed,
// just ask `VS.app.hotkeys.[key]` on `keypress`, or ask `VS.app.hotkeys.key(e)`
// on `keydown`.
//
// For the most headache-free way to use this utility, check modifier keys,
// like shift and command, with `VS.app.hotkeys.shift`, and check every other
// key with `VS.app.hotkeys.key(e) == 'key_name'`.
VS.app.hotkeys = {

  // Keys that will be mapped to the `hotkeys` namespace.
  KEYS: {
    '16':  'shift',
    '17':  'command',
    '91':  'command',
    '93':  'command',
    '224': 'command',
    '13':  'enter',
    '37':  'left',
    '38':  'upArrow',
    '39':  'right',
    '40':  'downArrow',
    '46':  'delete',
    '8':   'backspace',
    '35':  'end',
    '36':  'home',
    '9':   'tab',
    '188': 'comma'
  },

  // Binds global keydown and keyup events to listen for keys that match `this.KEYS`.
  initialize : function() {
    _.bindAll(this, 'down', 'up', 'blur');
    $(document).bind('keydown', this.down);
    $(document).bind('keyup', this.up);
    $(window).bind('blur', this.blur);
  },

  // On `keydown`, turn on all keys that match.
  down : function(e) {
    var key = this.KEYS[e.which];
    if (key) this[key] = true;
  },

  // On `keyup`, turn off all keys that match.
  up : function(e) {
    var key = this.KEYS[e.which];
    if (key) this[key] = false;
  },

  // If an input is blurred, all keys need to be turned off, since they are no longer
  // able to modify the document.
  blur : function(e) {
    for (var key in this.KEYS) this[this.KEYS[key]] = false;
  },

  // Check a key from an event and return the common english name.
  key : function(e) {
    return this.KEYS[e.which];
  },

  // Colon is special, since the value is different between browsers.
  colon : function(e) {
    var charCode = e.which;
    return charCode && String.fromCharCode(charCode) == ":";
  },

  // Check a key from an event and match it against any known characters.
  // The `keyCode` is different depending on the event type: `keydown` vs. `keypress`.
  //
  // These were determined by looping through every `keyCode` and `charCode` that
  // resulted from `keydown` and `keypress` events and counting what was printable.
  printable : function(e) {
    var code = e.which;
    if (e.type == 'keydown') {
      if (code == 32 ||                      // space
          (code >= 48 && code <= 90) ||      // 0-1a-z
          (code >= 96 && code <= 111) ||     // 0-9+-/*.
          (code >= 186 && code <= 192) ||    // ;=,-./^
          (code >= 219 && code <= 222)) {    // (\)'
        return true;
      }
    } else {
      // [space]!"#$%&'()*+,-.0-9:;<=>?@A-Z[\]^_`a-z{|} and unicode characters
      if ((code >= 32 && code <= 126)  ||
          (code >= 160 && code <= 500) ||
          (String.fromCharCode(code) == ":")) {
        return true;
      }
    }
    return false;
  }

};

})();