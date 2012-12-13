# https://gist.github.com/2013669

module.exports = (->

  fs      = require 'fs'
  vm      = require 'vm'
  sysPath = require 'path'

  handlebarsjsPath = sysPath.join __dirname, '..', 'vendor', 'handlebars-1.0.0.beta.6.js'
  emberjsPath      = sysPath.join __dirname, '..', 'vendor', 'ember.js'

  handlebarsjs  = fs.readFileSync handlebarsjsPath, 'utf8'
  emberjs       = fs.readFileSync emberjsPath, 'utf8'

  # dummy jQuery
  jQuery = -> jQuery
  jQuery.ready = -> jQuery
  jQuery.inArray = -> jQuery
  jQuery.jquery = "1.7.1";

  # dummy DOM element
  element =
    firstChild: -> element
    innerHTML: -> element

  sandbox =
    # DOM
    document:
      createRange: false
      createElement: -> element

    # Console
    console: console

    # jQuery
    jQuery: jQuery
    $: jQuery

    # handlebars template to compile
    template: null

    # compiled handlebars template
    templatejs: null

  # window
  sandbox.window = sandbox

  # create a context for the vm using the sandbox data
  context = vm.createContext sandbox

  # load ember and handlebars in the vm
  vm.runInContext handlebarsjs, context, 'handlebars.js'
  vm.runInContext emberjs, context, 'ember.js'

  return (templateData)->

    context.template = templateData

    # compile the handlebars template inside the vm context
    vm.runInContext 'templatejs = Ember.Handlebars.precompile(template).toString();', context

    context.templatejs;

)()
