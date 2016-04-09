(function() {

var $ = jQuery; // Handle namespaced jQuery

// Naive English transformations on words. Only used for a few transformations 
// in VisualSearch.js.
VS.utils.inflector = {

  // Delegate to the ECMA5 String.prototype.trim function, if available.
  trim : function(s) {
    return s.trim ? s.trim() : s.replace(/^\s+|\s+$/g, '');
  },
  
  // Escape strings that are going to be used in a regex. Escapes punctuation
  // that would be incorrect in a regex.
  escapeRegExp : function(s) {
    return s.replace(/([.*+?^${}()|[\]\/\\])/g, '\\$1');
  }
};

})();