const istanbul = require('istanbul-api');

function CoverageIstanbulReporter(baseReporterDecorator, logger, config) {
  baseReporterDecorator(this);

  const log = logger.create('reporter.coverage-istanbul');

  const browserCoverage = new WeakMap();

  this.onBrowserComplete = function (browser, result) {
    if (!result || !result.coverage) {
      return;
    }

    browserCoverage.set(browser, result.coverage);
  };

  const baseReporterOnRunComplete = this.onRunComplete;
  this.onRunComplete = function (browsers) {
    baseReporterOnRunComplete.apply(this, arguments);

    const coverageIstanbulReporter = config.coverageIstanbulReporter || {};
    const reportConfig = istanbul.config.loadObject({reporting: coverageIstanbulReporter});
    const reportTypes = reportConfig.reporting.config.reports;

    browsers.forEach(browser => {
      const coverage = browserCoverage.get(browser);
      browserCoverage.delete(browser);

      if (!coverage) {
        return;
      }

      const reporter = istanbul.createReporter(reportConfig);
      reporter.addAll(reportTypes);

      const coverageMap = istanbul.libCoverage.createCoverageMap();
      const sourceMapStore = istanbul.libSourceMaps.createSourceMapStore();

      Object.keys(coverage).forEach(filename => {
        const fileCoverage = coverage[filename];
        if (fileCoverage.inputSourceMap && coverageIstanbulReporter.fixWebpackSourcePaths) {
          fileCoverage.inputSourceMap.sources = fileCoverage.inputSourceMap.sources.map(source => {
            if (source.indexOf('!') !== -1) {
              source = source.split('!').pop();
            }
            if (source.indexOf('?') !== -1) {
              source = source.split('?')[0];
            }
            return source;
          });
        }
        coverageMap.addFileCoverage(fileCoverage);
      });

      const remappedCoverageMap = sourceMapStore.transformCoverage(coverageMap).map;

      log.debug('Writing coverage reports:', reportTypes);

      reporter.write(remappedCoverageMap);
    });
  };
}

CoverageIstanbulReporter.$inject = ['baseReporterDecorator', 'logger', 'config'];

module.exports = {
  'reporter:coverage-istanbul': ['type', CoverageIstanbulReporter]
};
