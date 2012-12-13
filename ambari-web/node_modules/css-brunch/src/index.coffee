module.exports = class CSSCompiler
  brunchPlugin: yes
  type: 'stylesheet'
  extension: 'css'

  constructor: (@config) ->
    null

  compile: (data, path, callback) ->
    callback null, data
