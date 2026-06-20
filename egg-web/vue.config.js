module.exports = {
  devServer: {
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  },
  productionSourceMap: false,
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          elementUI: {
            name: 'chunk-elementUI',
            priority: 30,
            test: /[\\/]node_modules[\\/]element-ui[\\/]/,
            chunks: 'all'
          },
          vendors: {
            name: 'chunk-vendors',
            priority: 20,
            test: /[\\/]node_modules[\\/]/,
            chunks: 'all',
            reuseExistingChunk: true
          }
        }
      }
    }
  },
  chainWebpack: function (config) {
    // Disable prefetch — wastes bandwidth on lazy routes user may never visit
    config.plugins.delete('prefetch')
  }
}