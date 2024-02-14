module.exports = function (config) {
    
    config.set({
        
        basePath: './',
        
        files: [
            { pattern: 'node_modules/reflect-metadata/Reflect.js' },
            { pattern: 'src/base.spec.ts' },
            { pattern: 'src/**/*.ts' }
        ],

        exclude: [
            'src/**/*.d.ts'
        ],

        preprocessors: {
            '**/*.ts': ['karma-typescript']
        },

        frameworks: ['jasmine', 'karma-typescript'],
        browsers: ['Firefox'],
        plugins: [
            'karma-jasmine',
            'karma-spec-reporter',
            'karma-firefox-launcher',
            'karma-typescript'
        ],

        karmaTypescriptConfig: {
            reports: {
                'text-summary': '',
                html: 'coverage',
                lcovonly: 'coverage'
            },
            coverageOptions: {
                exclude: /((\.(d|spec|test|module))|index\.ts)/
            },
            compilerOptions: {
              lib: ['es6', 'dom']
            }
        },
        
        reporters: ['spec', 'karma-typescript'],
        singleRun: true,
        autoWatch: false,
        colors: true,
        logLevel: config.LOG_INFO
    });
    
}
