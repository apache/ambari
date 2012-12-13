less = require 'less'
sysPath = require 'path'

module.exports = class LESSCompiler
  brunchPlugin: yes
  type: 'stylesheet'
  extension: 'less'
  _dependencyRegExp: /^ *@import ['"](.*)['"]/

  constructor: (@config) ->
    null

  compile: (data, path, callback) ->
    parser = new less.Parser
      paths: [@config.paths.root, (sysPath.dirname path)],
      filename: path
    parser.parse data, (error, tree) =>
      return callback error.message if error?
      result = null
      err = null

      try
        result = tree.toCSS()
      catch ex
        err = "#{ex.type}Error:#{ex.message}"
        if ex.filename
          err += " in '#{ex.filename}:#{ex.line}:#{ex.column}'"
      callback err, result

  getDependencies: (data, path, callback) =>
    parent = sysPath.dirname path
    dependencies = data
      .split('\n')
      .map (line) =>
        line.match(@_dependencyRegExp)
      .filter (match) =>
        match?.length > 0
      .map (match) =>
        match[1]
      .filter (path) =>
        !!path and path isnt 'nib'
      .map (path) =>
        if sysPath.extname(path) isnt ".#{@extension}"
          path + ".#{@extension}"
        else
          path
      .map (path) =>
        if path.charAt(0) is '/'
          sysPath.join @config.paths.root, path[1..]
        else
          sysPath.join parent, path
    process.nextTick =>
      callback null, dependencies
