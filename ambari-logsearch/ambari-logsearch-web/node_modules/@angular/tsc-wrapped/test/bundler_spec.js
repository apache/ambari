"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
var path = require("path");
var ts = require("typescript");
var bundler_1 = require("../src/bundler");
var collector_1 = require("../src/collector");
var typescript_mocks_1 = require("./typescript.mocks");
describe('metadata bundler', function () {
    it('should be able to bundle a simple library', function () {
        var host = new MockStringBundlerHost('/', exports.SIMPLE_LIBRARY);
        var bundler = new bundler_1.MetadataBundler('/lib/index', undefined, host);
        var result = bundler.getMetadataBundle();
        expect(Object.keys(result.metadata.metadata).sort()).toEqual([
            'ONE_CLASSES', 'One', 'OneMore', 'TWO_CLASSES', 'Two', 'TwoMore', 'ɵa', 'ɵb'
        ]);
        var originalOne = './src/one';
        var originalTwo = './src/two/index';
        expect(Object.keys(result.metadata.origins)
            .sort()
            .map(function (name) { return ({ name: name, value: result.metadata.origins[name] }); }))
            .toEqual([
            { name: 'ONE_CLASSES', value: originalOne }, { name: 'One', value: originalOne },
            { name: 'OneMore', value: originalOne }, { name: 'TWO_CLASSES', value: originalTwo },
            { name: 'Two', value: originalTwo }, { name: 'TwoMore', value: originalTwo },
            { name: 'ɵa', value: originalOne }, { name: 'ɵb', value: originalTwo }
        ]);
        expect(result.privates).toEqual([
            { privateName: 'ɵa', name: 'PrivateOne', module: originalOne },
            { privateName: 'ɵb', name: 'PrivateTwo', module: originalTwo }
        ]);
    });
    it('should be able to bundle an oddly constructed library', function () {
        var host = new MockStringBundlerHost('/', {
            'lib': {
                'index.ts': "\n          export * from './src/index';\n        ",
                'src': {
                    'index.ts': "\n            export {One, OneMore, ONE_CLASSES} from './one';\n            export {Two, TwoMore, TWO_CLASSES} from './two/index';\n          ",
                    'one.ts': "\n            class One {}\n            class OneMore extends One {}\n            class PrivateOne {}\n            const ONE_CLASSES = [One, OneMore, PrivateOne];\n            export {One, OneMore, PrivateOne, ONE_CLASSES};\n          ",
                    'two': {
                        'index.ts': "\n              class Two {}\n              class TwoMore extends Two {}\n              class PrivateTwo {}\n              const TWO_CLASSES = [Two, TwoMore, PrivateTwo];\n              export {Two, TwoMore, PrivateTwo, TWO_CLASSES};\n            "
                    }
                }
            }
        });
        var bundler = new bundler_1.MetadataBundler('/lib/index', undefined, host);
        var result = bundler.getMetadataBundle();
        expect(Object.keys(result.metadata.metadata).sort()).toEqual([
            'ONE_CLASSES', 'One', 'OneMore', 'TWO_CLASSES', 'Two', 'TwoMore', 'ɵa', 'ɵb'
        ]);
        expect(result.privates).toEqual([
            { privateName: 'ɵa', name: 'PrivateOne', module: './src/one' },
            { privateName: 'ɵb', name: 'PrivateTwo', module: './src/two/index' }
        ]);
    });
    it('should not output windows paths in metadata', function () {
        var host = new MockStringBundlerHost('/', {
            'index.ts': "\n        export * from './exports/test';\n      ",
            'exports': { 'test.ts': "export class TestExport {}" }
        });
        var bundler = new bundler_1.MetadataBundler('/index', undefined, host);
        var result = bundler.getMetadataBundle();
        expect(result.metadata.origins).toEqual({ 'TestExport': './exports/test' });
    });
    it('should convert re-exported to the export', function () {
        var host = new MockStringBundlerHost('/', {
            'index.ts': "\n        export * from './bar';\n        export * from './foo';\n      ",
            'bar.ts': "\n        import {Foo} from './foo';\n        export class Bar extends Foo {\n\n        }\n      ",
            'foo.ts': "\n        export {Foo} from 'foo';\n      "
        });
        var bundler = new bundler_1.MetadataBundler('/index', undefined, host);
        var result = bundler.getMetadataBundle();
        // Expect the extends reference to refer to the imported module
        expect(result.metadata.metadata.Bar.extends.module).toEqual('foo');
        expect(result.privates).toEqual([]);
    });
    it('should treat import then export as a simple export', function () {
        var host = new MockStringBundlerHost('/', {
            'index.ts': "\n        export * from './a';\n        export * from './c';\n      ",
            'a.ts': "\n        import { B } from './b';\n        export { B };\n      ",
            'b.ts': "\n        export class B { }\n      ",
            'c.ts': "\n        import { B } from './b';\n        export class C extends B { }\n      "
        });
        var bundler = new bundler_1.MetadataBundler('/index', undefined, host);
        var result = bundler.getMetadataBundle();
        expect(Object.keys(result.metadata.metadata).sort()).toEqual(['B', 'C']);
        expect(result.privates).toEqual([]);
    });
    it('should be able to bundle a private from a un-exported module', function () {
        var host = new MockStringBundlerHost('/', {
            'index.ts': "\n        export * from './foo';\n      ",
            'foo.ts': "\n        import {Bar} from './bar';\n        export class Foo extends Bar {\n\n        }\n      ",
            'bar.ts': "\n        export class Bar {}\n      "
        });
        var bundler = new bundler_1.MetadataBundler('/index', undefined, host);
        var result = bundler.getMetadataBundle();
        expect(Object.keys(result.metadata.metadata).sort()).toEqual(['Foo', 'ɵa']);
        expect(result.privates).toEqual([{ privateName: 'ɵa', name: 'Bar', module: './bar' }]);
    });
    it('should be able to bundle a library with re-exported symbols', function () {
        var host = new MockStringBundlerHost('/', {
            'public-api.ts': "\n        export * from './src/core';\n        export * from './src/externals';\n      ",
            'src': {
                'core.ts': "\n          export class A {}\n          export class B extends A {}\n        ",
                'externals.ts': "\n          export {E, F, G} from 'external_one';\n          export * from 'external_two';\n        "
            }
        });
        var bundler = new bundler_1.MetadataBundler('/public-api', undefined, host);
        var result = bundler.getMetadataBundle();
        expect(result.metadata.exports).toEqual([
            { from: 'external_two' }, {
                export: [{ name: 'E', as: 'E' }, { name: 'F', as: 'F' }, { name: 'G', as: 'G' }],
                from: 'external_one'
            }
        ]);
        expect(result.metadata.origins['E']).toBeUndefined();
    });
    it('should be able to de-duplicate symbols of re-exported modules', function () {
        var host = new MockStringBundlerHost('/', {
            'public-api.ts': "\n        export {A as A2, A, B as B1, B as B2} from './src/core';\n        export {A as A3} from './src/alternate';\n      ",
            'src': {
                'core.ts': "\n          export class A {}\n          export class B {}\n        ",
                'alternate.ts': " \n          export class A {} \n        ",
            }
        });
        var bundler = new bundler_1.MetadataBundler('/public-api', undefined, host);
        var result = bundler.getMetadataBundle();
        var _a = result.metadata.metadata, A = _a.A, A2 = _a.A2, A3 = _a.A3, B1 = _a.B1, B2 = _a.B2;
        expect(A.__symbolic).toEqual('class');
        expect(A2.__symbolic).toEqual('reference');
        expect(A2.name).toEqual('A');
        expect(A3.__symbolic).toEqual('class');
        expect(B1.__symbolic).toEqual('class');
        expect(B2.__symbolic).toEqual('reference');
        expect(B2.name).toEqual('B1');
    });
});
var MockStringBundlerHost = (function () {
    function MockStringBundlerHost(dirName, directory) {
        this.dirName = dirName;
        this.directory = directory;
        this.collector = new collector_1.MetadataCollector();
    }
    MockStringBundlerHost.prototype.getMetadataFor = function (moduleName) {
        var fileName = path.join(this.dirName, moduleName) + '.ts';
        var text = typescript_mocks_1.open(this.directory, fileName);
        if (typeof text == 'string') {
            var sourceFile = ts.createSourceFile(fileName, text, ts.ScriptTarget.Latest, /* setParent */ true, ts.ScriptKind.TS);
            var diagnostics = sourceFile.parseDiagnostics;
            if (diagnostics && diagnostics.length) {
                throw Error('Unexpected syntax error in test');
            }
            var result = this.collector.getMetadata(sourceFile);
            return result;
        }
    };
    return MockStringBundlerHost;
}());
exports.MockStringBundlerHost = MockStringBundlerHost;
exports.SIMPLE_LIBRARY = {
    'lib': {
        'index.ts': "\n      export * from './src/index';\n    ",
        'src': {
            'index.ts': "\n        export {One, OneMore, ONE_CLASSES} from './one';\n        export {Two, TwoMore, TWO_CLASSES} from './two/index';\n      ",
            'one.ts': "\n        export class One {}\n        export class OneMore extends One {}\n        export class PrivateOne {}\n        export const ONE_CLASSES = [One, OneMore, PrivateOne];\n      ",
            'two': {
                'index.ts': "\n          export class Two {}\n          export class TwoMore extends Two {}\n          export class PrivateTwo {}\n          export const TWO_CLASSES = [Two, TwoMore, PrivateTwo];\n        "
            }
        }
    }
};
//# sourceMappingURL=bundler_spec.js.map