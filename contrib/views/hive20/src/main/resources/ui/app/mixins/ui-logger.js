import Ember from 'ember';

export default Ember.Mixin.create({
  logger: Ember.inject.service('alert-messages'),

  extractError(error) {
    if (Ember.isArray(error.errors) && (error.errors.length >= 0)) {
      return error.errors[0];
    } else if(!Ember.isEmpty(error.errors)) {
      return error.errors;
    } else {
      return error;
    }
  }
});
