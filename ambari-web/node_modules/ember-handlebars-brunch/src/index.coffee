
sysPath     = require 'path'
compileHBS  = require './ember-handlebars-compiler'

module.exports = class EmberHandlebarsCompiler
  brunchPlugin: yes
  type: 'template'
  extension: 'hbs'
  precompile: off

  constructor: (@config) ->
    if @config.files.templates.precompile is on
      @precompile = on
    null

  compile: (data, path, callback) ->
    try
      if @precompile is on
        content = compileHBS data.toString()
        result  = "\nEmber.TEMPLATES[module.id] = Ember.Handlebars.template(#{content});\n module.exports = module.id;"
      else
        content = JSON.stringify data.toString()
        result  = "\nEmber.TEMPLATES[module.id] = Ember.Handlebars.compile(#{content});\n module.exports = module.id;"
    catch err
      error = err
    finally
      callback error, result

  # include: [
  #   sysPath.join __dirname, '..', 'vendor', 'handlebars-1.0.0.beta.6.js'
  #   sysPath.join __dirname, '..', 'vendor', 'ember.js'
  # ]
