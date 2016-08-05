(function() {

var $ = jQuery; // Handle namespaced jQuery

// Collection which holds all of the individual facets (category: value).
// Used for finding and removing specific facets.
VS.model.SearchQuery = Backbone.Collection.extend({

  // Model holds the category and value of the facet.
  model : VS.model.SearchFacet,
  
  // Turns all of the facets into a single serialized string.
  serialize : function() {
    return this.map(function(facet){ return facet.serialize(); }).join(' ');
  },
  
  facets : function() {
    return this.map(function(facet) {
      var value = {};
      value[facet.get('category')] = facet.get('value');
      return value;
    });
  },

  // Find a facet by its category. Multiple facets with the same category
  // is fine, but only the first is returned.
  find : function(category) {
    var facet = this.detect(function(facet) {
      return facet.get('category').toLowerCase() == category.toLowerCase();
    });
    return facet && facet.get('value');
  },

  // Counts the number of times a specific category is in the search query.
  count : function(category) {
    return this.select(function(facet) {
      return facet.get('category').toLowerCase() == category.toLowerCase();
    }).length;
  },

  // Returns an array of extracted values from each facet in a category.
  values : function(category) {
    var facets = this.select(function(facet) {
      return facet.get('category').toLowerCase() == category.toLowerCase();
    });
    return _.map(facets, function(facet) { return facet.get('value'); });
  },

  // Checks all facets for matches of either a category or both category and value.
  has : function(category, value) {
    return this.any(function(facet) {
      var categoryMatched = facet.get('category').toLowerCase() == category.toLowerCase();
      if (!value) return categoryMatched;
      return categoryMatched && facet.get('value') == value;
    });
  },

  // Used to temporarily hide specific categories and serialize the search query.
  withoutCategory : function() {
    var categories = _.map(_.toArray(arguments), function(cat) { return cat.toLowerCase(); });
    return this.map(function(facet) {
      if (!_.include(categories, facet.get('category').toLowerCase())) { 
        return facet.serialize();
      };
    }).join(' ');
  }

});

})();
