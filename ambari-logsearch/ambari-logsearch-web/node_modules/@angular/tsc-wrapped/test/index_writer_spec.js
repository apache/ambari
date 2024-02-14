"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var bundler_1 = require("../src/bundler");
var index_writer_1 = require("../src/index_writer");
var bundler_spec_1 = require("./bundler_spec");
describe('index_writer', function () {
    it('should be able to write the index of a simple library', function () {
        var host = new bundler_spec_1.MockStringBundlerHost('/', bundler_spec_1.SIMPLE_LIBRARY);
        var bundler = new bundler_1.MetadataBundler('/lib/index', undefined, host);
        var bundle = bundler.getMetadataBundle();
        var result = index_writer_1.privateEntriesToIndex('./index', bundle.privates);
        expect(result).toContain("export * from './index';");
        expect(result).toContain("export {PrivateOne as \u0275a} from './src/one';");
        expect(result).toContain("export {PrivateTwo as \u0275b} from './src/two/index';");
    });
});
//# sourceMappingURL=index_writer_spec.js.map