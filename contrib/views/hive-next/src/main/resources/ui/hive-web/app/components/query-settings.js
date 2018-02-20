import Ember from 'ember';

export default Ember.Component.extend({
  settingsService: Ember.inject.service('settings'),
  predefinedSettings: Ember.computed.alias('settingsService.predefinedSettings'),
  settings: Ember.computed.alias('settingsService.settings'),
  didInsertElement: function () {
    this.get('settingsService').loadDefaultSettings();
  },
  excluded: function () {
    var settings = this.get('settings');
    return this.get('predefinedSettings').filter(function (setting) {
      return settings.findBy('key.name', setting.name);
    });
  }.property('settings.@each.key'),

  actions: {
    add: function () {
      this.get('settingsService').add();
    },

    remove: function (setting) {
      this.get('settingsService').remove(setting);
    },

    addKey: function (name) {
      this.get('settingsService').createKey(name);
    },

    removeAll: function () {
      this.get('settingsService').removeAll();
    },

    saveDefaultSettings: function () {
      this.get('settingsService').saveDefaultSettings();
    }
  }
});
