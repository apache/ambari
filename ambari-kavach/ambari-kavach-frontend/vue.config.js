const { defineConfig } = require('@vue/cli-service')

// Suppress "Proxy error: Could not proxy request" console noise when backend is not running.
const silentProxy = (target) => ({
  target,
  changeOrigin: true,
  logLevel: 'silent',
  onError: (err, req, res) => {
    res.writeHead(502, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ error: 'Backend unavailable' }))
  }
})

module.exports = defineConfig({
  transpileDependencies: [],
  devServer: {
    port: 8080,
    historyApiFallback: true,
    proxy: {
      '/api':         silentProxy('http://localhost:5000'),
      '/auth':        silentProxy('http://localhost:5000'),
      '/create_user': silentProxy('http://localhost:5000'),
      '/manager/':    silentProxy('http://localhost:5000')
    }
  }
})
