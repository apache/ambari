"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var tsc_1 = require("../src/tsc");
describe('options parsing', function () {
    var configData = "\n{\n    \"angularCompilerOptions\": {\n        \"googleClosureOutput\": true\n    },\n    \"compilerOptions\": {\n        \"module\": \"commonjs\",\n        \"outDir\": \"built\"\n    }\n}";
    var tsc = new tsc_1.Tsc(function () { return configData; }, function () { return ['tsconfig.json']; });
    var config = { path: 'basePath/tsconfig.json', contents: new Buffer(configData) };
    it('should combine all options into ngOptions', function () {
        var _a = tsc.readConfiguration('projectDir', 'basePath', { target: ts.ScriptTarget.ES2015 }), parsed = _a.parsed, ngOptions = _a.ngOptions;
        expect(ngOptions).toEqual({
            genDir: 'basePath',
            googleClosureOutput: true,
            module: ts.ModuleKind.CommonJS,
            outDir: 'basePath/built',
            configFilePath: undefined,
            target: ts.ScriptTarget.ES2015
        });
    });
    it('should combine all options into ngOptions from vinyl like object', function () {
        var _a = tsc.readConfiguration(config, 'basePath'), parsed = _a.parsed, ngOptions = _a.ngOptions;
        expect(ngOptions).toEqual({
            genDir: 'basePath',
            googleClosureOutput: true,
            module: ts.ModuleKind.CommonJS,
            outDir: 'basePath/built',
            configFilePath: undefined
        });
    });
});
//# sourceMappingURL=tsc.spec.js.map