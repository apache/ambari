"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
var _this = this;
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var collector_1 = require("../src/collector");
var schema_1 = require("../src/schema");
var typescript_mocks_1 = require("./typescript.mocks");
describe('Collector', function () {
    var documentRegistry = ts.createDocumentRegistry();
    var host;
    var service;
    var program;
    var collector;
    beforeEach(function () {
        host = new typescript_mocks_1.Host(FILES, [
            '/app/app.component.ts',
            '/app/cases-data.ts',
            '/app/error-cases.ts',
            '/promise.ts',
            '/unsupported-1.ts',
            '/unsupported-2.ts',
            '/unsupported-3.ts',
            'class-arity.ts',
            'import-star.ts',
            'exported-classes.ts',
            'exported-functions.ts',
            'exported-enum.ts',
            'exported-consts.ts',
            'local-symbol-ref.ts',
            'local-function-ref.ts',
            'local-symbol-ref-func.ts',
            'local-symbol-ref-func-dynamic.ts',
            'private-enum.ts',
            're-exports.ts',
            're-exports-2.ts',
            'export-as.d.ts',
            'static-field-reference.ts',
            'static-method.ts',
            'static-method-call.ts',
            'static-method-with-if.ts',
            'static-method-with-default.ts',
            'class-inheritance.ts',
            'class-inheritance-parent.ts',
            'class-inheritance-declarations.d.ts',
            'interface-reference.ts'
        ]);
        service = ts.createLanguageService(host, documentRegistry);
        program = service.getProgram();
        collector = new collector_1.MetadataCollector({ quotedNames: true });
    });
    it('should not have errors in test data', function () { typescript_mocks_1.expectValidSources(service, program); });
    it('should return undefined for modules that have no metadata', function () {
        var sourceFile = program.getSourceFile('app/empty.ts');
        var metadata = collector.getMetadata(sourceFile);
        expect(metadata).toBeUndefined();
    });
    it('should return an interface reference for interfaces', function () {
        var sourceFile = program.getSourceFile('app/hero.ts');
        var metadata = collector.getMetadata(sourceFile);
        expect(metadata).toEqual({ __symbolic: 'module', version: 3, metadata: { Hero: { __symbolic: 'interface' } } });
    });
    it('should be able to collect a simple component\'s metadata', function () {
        var sourceFile = program.getSourceFile('app/hero-detail.component.ts');
        var metadata = collector.getMetadata(sourceFile);
        expect(metadata).toEqual({
            __symbolic: 'module',
            version: 3,
            metadata: {
                HeroDetailComponent: {
                    __symbolic: 'class',
                    decorators: [{
                            __symbolic: 'call',
                            expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Component' },
                            arguments: [{
                                    selector: 'my-hero-detail',
                                    template: "\n        <div *ngIf=\"hero\">\n          <h2>{{hero.name}} details!</h2>\n          <div><label>id: </label>{{hero.id}}</div>\n          <div>\n            <label>name: </label>\n            <input [(ngModel)]=\"hero.name\" placeholder=\"name\"/>\n          </div>\n        </div>\n      "
                                }]
                        }],
                    members: {
                        hero: [{
                                __symbolic: 'property',
                                decorators: [{
                                        __symbolic: 'call',
                                        expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Input' }
                                    }]
                            }]
                    }
                }
            }
        });
    });
    it('should be able to get a more complicated component\'s metadata', function () {
        var sourceFile = program.getSourceFile('/app/app.component.ts');
        var metadata = collector.getMetadata(sourceFile);
        expect(metadata).toEqual({
            __symbolic: 'module',
            version: 3,
            metadata: {
                AppComponent: {
                    __symbolic: 'class',
                    decorators: [{
                            __symbolic: 'call',
                            expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Component' },
                            arguments: [{
                                    selector: 'my-app',
                                    template: "\n        <h2>My Heroes</h2>\n        <ul class=\"heroes\">\n          <li *ngFor=\"#hero of heroes\"\n            (click)=\"onSelect(hero)\"\n            [class.selected]=\"hero === selectedHero\">\n            <span class=\"badge\">{{hero.id | lowercase}}</span> {{hero.name | uppercase}}\n          </li>\n        </ul>\n        <my-hero-detail [hero]=\"selectedHero\"></my-hero-detail>\n        ",
                                    directives: [
                                        {
                                            __symbolic: 'reference',
                                            module: './hero-detail.component',
                                            name: 'HeroDetailComponent',
                                        },
                                        { __symbolic: 'reference', module: 'angular2/common', name: 'NgFor' }
                                    ],
                                    providers: [{ __symbolic: 'reference', module: './hero.service', default: true }],
                                    pipes: [
                                        { __symbolic: 'reference', module: 'angular2/common', name: 'LowerCasePipe' },
                                        { __symbolic: 'reference', module: 'angular2/common', name: 'UpperCasePipe' }
                                    ]
                                }]
                        }],
                    members: {
                        __ctor__: [{
                                __symbolic: 'constructor',
                                parameters: [{ __symbolic: 'reference', module: './hero.service', default: true }]
                            }],
                        onSelect: [{ __symbolic: 'method' }],
                        ngOnInit: [{ __symbolic: 'method' }],
                        getHeroes: [{ __symbolic: 'method' }]
                    }
                }
            }
        });
    });
    it('should return the values of exported variables', function () {
        var sourceFile = program.getSourceFile('/app/mock-heroes.ts');
        var metadata = collector.getMetadata(sourceFile);
        expect(metadata).toEqual({
            __symbolic: 'module',
            version: 3,
            metadata: {
                HEROES: [
                    { 'id': 11, 'name': 'Mr. Nice', '$quoted$': ['id', 'name'] },
                    { 'id': 12, 'name': 'Narco', '$quoted$': ['id', 'name'] },
                    { 'id': 13, 'name': 'Bombasto', '$quoted$': ['id', 'name'] },
                    { 'id': 14, 'name': 'Celeritas', '$quoted$': ['id', 'name'] },
                    { 'id': 15, 'name': 'Magneta', '$quoted$': ['id', 'name'] },
                    { 'id': 16, 'name': 'RubberMan', '$quoted$': ['id', 'name'] },
                    { 'id': 17, 'name': 'Dynama', '$quoted$': ['id', 'name'] },
                    { 'id': 18, 'name': 'Dr IQ', '$quoted$': ['id', 'name'] },
                    { 'id': 19, 'name': 'Magma', '$quoted$': ['id', 'name'] },
                    { 'id': 20, 'name': 'Tornado', '$quoted$': ['id', 'name'] }
                ]
            }
        });
    });
    var casesFile;
    var casesMetadata;
    beforeEach(function () {
        casesFile = program.getSourceFile('/app/cases-data.ts');
        casesMetadata = collector.getMetadata(casesFile);
    });
    it('should provide any reference for an any ctor parameter type', function () {
        var casesAny = casesMetadata.metadata['CaseAny'];
        expect(casesAny).toBeTruthy();
        var ctorData = casesAny.members['__ctor__'];
        expect(ctorData).toEqual([{ __symbolic: 'constructor', parameters: [{ __symbolic: 'reference', name: 'any' }] }]);
    });
    it('should record annotations on set and get declarations', function () {
        var propertyData = {
            name: [{
                    __symbolic: 'property',
                    decorators: [{
                            __symbolic: 'call',
                            expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Input' },
                            arguments: ['firstName']
                        }]
                }]
        };
        var caseGetProp = casesMetadata.metadata['GetProp'];
        expect(caseGetProp.members).toEqual(propertyData);
        var caseSetProp = casesMetadata.metadata['SetProp'];
        expect(caseSetProp.members).toEqual(propertyData);
        var caseFullProp = casesMetadata.metadata['FullProp'];
        expect(caseFullProp.members).toEqual(propertyData);
    });
    it('should record references to parameterized types', function () {
        var casesForIn = casesMetadata.metadata['NgFor'];
        expect(casesForIn).toEqual({
            __symbolic: 'class',
            decorators: [{
                    __symbolic: 'call',
                    expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Injectable' }
                }],
            members: {
                __ctor__: [{
                        __symbolic: 'constructor',
                        parameters: [{
                                __symbolic: 'reference',
                                name: 'ClassReference',
                                arguments: [{ __symbolic: 'reference', name: 'NgForRow' }]
                            }]
                    }]
            }
        });
    });
    it('should report errors for destructured imports', function () {
        var unsupported1 = program.getSourceFile('/unsupported-1.ts');
        var metadata = collector.getMetadata(unsupported1);
        expect(metadata).toEqual({
            __symbolic: 'module',
            version: 3,
            metadata: {
                a: { __symbolic: 'error', message: 'Destructuring not supported', line: 1, character: 16 },
                b: { __symbolic: 'error', message: 'Destructuring not supported', line: 1, character: 19 },
                c: { __symbolic: 'error', message: 'Destructuring not supported', line: 2, character: 16 },
                d: { __symbolic: 'error', message: 'Destructuring not supported', line: 2, character: 19 },
                e: { __symbolic: 'error', message: 'Variable not initialized', line: 3, character: 15 }
            }
        });
    });
    it('should report an error for references to unexpected types', function () {
        var unsupported1 = program.getSourceFile('/unsupported-2.ts');
        var metadata = collector.getMetadata(unsupported1);
        var barClass = metadata.metadata['Bar'];
        var ctor = barClass.members['__ctor__'][0];
        var parameter = ctor.parameters[0];
        expect(parameter).toEqual({
            __symbolic: 'error',
            message: 'Reference to non-exported class',
            line: 3,
            character: 4,
            context: { className: 'Foo' }
        });
    });
    it('should be able to handle import star type references', function () {
        var importStar = program.getSourceFile('/import-star.ts');
        var metadata = collector.getMetadata(importStar);
        var someClass = metadata.metadata['SomeClass'];
        var ctor = someClass.members['__ctor__'][0];
        var parameters = ctor.parameters;
        expect(parameters).toEqual([
            { __symbolic: 'reference', module: 'angular2/common', name: 'NgFor' }
        ]);
    });
    it('should record all exported classes', function () {
        var sourceFile = program.getSourceFile('/exported-classes.ts');
        var metadata = collector.getMetadata(sourceFile);
        expect(metadata).toEqual({
            __symbolic: 'module',
            version: 3,
            metadata: {
                SimpleClass: { __symbolic: 'class' },
                AbstractClass: { __symbolic: 'class' },
                DeclaredClass: { __symbolic: 'class' }
            }
        });
    });
    it('should be able to record functions', function () {
        var exportedFunctions = program.getSourceFile('/exported-functions.ts');
        var metadata = collector.getMetadata(exportedFunctions);
        expect(metadata).toEqual({
            __symbolic: 'module',
            version: 3,
            metadata: {
                one: {
                    __symbolic: 'function',
                    parameters: ['a', 'b', 'c'],
                    value: {
                        a: { __symbolic: 'reference', name: 'a' },
                        b: { __symbolic: 'reference', name: 'b' },
                        c: { __symbolic: 'reference', name: 'c' }
                    }
                },
                two: {
                    __symbolic: 'function',
                    parameters: ['a', 'b', 'c'],
                    value: {
                        a: { __symbolic: 'reference', name: 'a' },
                        b: { __symbolic: 'reference', name: 'b' },
                        c: { __symbolic: 'reference', name: 'c' }
                    }
                },
                three: {
                    __symbolic: 'function',
                    parameters: ['a', 'b', 'c'],
                    value: [
                        { __symbolic: 'reference', name: 'a' }, { __symbolic: 'reference', name: 'b' },
                        { __symbolic: 'reference', name: 'c' }
                    ]
                },
                supportsState: {
                    __symbolic: 'function',
                    parameters: [],
                    value: {
                        __symbolic: 'pre',
                        operator: '!',
                        operand: {
                            __symbolic: 'pre',
                            operator: '!',
                            operand: {
                                __symbolic: 'select',
                                expression: {
                                    __symbolic: 'select',
                                    expression: { __symbolic: 'reference', name: 'window' },
                                    member: 'history'
                                },
                                member: 'pushState'
                            }
                        }
                    }
                },
                complexFn: { __symbolic: 'function' },
                declaredFn: { __symbolic: 'function' }
            }
        });
    });
    it('should be able to handle import star type references', function () {
        var importStar = program.getSourceFile('/import-star.ts');
        var metadata = collector.getMetadata(importStar);
        var someClass = metadata.metadata['SomeClass'];
        var ctor = someClass.members['__ctor__'][0];
        var parameters = ctor.parameters;
        expect(parameters).toEqual([
            { __symbolic: 'reference', module: 'angular2/common', name: 'NgFor' }
        ]);
    });
    it('should be able to collect the value of an enum', function () {
        var enumSource = program.getSourceFile('/exported-enum.ts');
        var metadata = collector.getMetadata(enumSource);
        var someEnum = metadata.metadata['SomeEnum'];
        expect(someEnum).toEqual({ A: 0, B: 1, C: 100, D: 101 });
    });
    it('should ignore a non-export enum', function () {
        var enumSource = program.getSourceFile('/private-enum.ts');
        var metadata = collector.getMetadata(enumSource);
        var publicEnum = metadata.metadata['PublicEnum'];
        var privateEnum = metadata.metadata['PrivateEnum'];
        expect(publicEnum).toEqual({ a: 0, b: 1, c: 2 });
        expect(privateEnum).toBeUndefined();
    });
    it('should be able to collect enums initialized from consts', function () {
        var enumSource = program.getSourceFile('/exported-enum.ts');
        var metadata = collector.getMetadata(enumSource);
        var complexEnum = metadata.metadata['ComplexEnum'];
        expect(complexEnum).toEqual({
            A: 0,
            B: 1,
            C: 30,
            D: 40,
            E: { __symbolic: 'reference', module: './exported-consts', name: 'constValue' }
        });
    });
    it('should be able to collect a simple static method', function () {
        var staticSource = program.getSourceFile('/static-method.ts');
        var metadata = collector.getMetadata(staticSource);
        expect(metadata).toBeDefined();
        var classData = metadata.metadata['MyModule'];
        expect(classData).toBeDefined();
        expect(classData.statics).toEqual({
            with: {
                __symbolic: 'function',
                parameters: ['comp'],
                value: [
                    { __symbolic: 'reference', name: 'MyModule' },
                    { provider: 'a', useValue: { __symbolic: 'reference', name: 'comp' } }
                ]
            }
        });
    });
    it('should be able to collect a call to a static method', function () {
        var staticSource = program.getSourceFile('/static-method-call.ts');
        var metadata = collector.getMetadata(staticSource);
        expect(metadata).toBeDefined();
        var classData = metadata.metadata['Foo'];
        expect(classData).toBeDefined();
        expect(classData.decorators).toEqual([{
                __symbolic: 'call',
                expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Component' },
                arguments: [{
                        providers: {
                            __symbolic: 'call',
                            expression: {
                                __symbolic: 'select',
                                expression: { __symbolic: 'reference', module: './static-method', name: 'MyModule' },
                                member: 'with'
                            },
                            arguments: ['a']
                        }
                    }]
            }]);
    });
    it('should be able to collect a static field', function () {
        var staticSource = program.getSourceFile('/static-field.ts');
        var metadata = collector.getMetadata(staticSource);
        expect(metadata).toBeDefined();
        var classData = metadata.metadata['MyModule'];
        expect(classData).toBeDefined();
        expect(classData.statics).toEqual({ VALUE: 'Some string' });
    });
    it('should be able to collect a reference to a static field', function () {
        var staticSource = program.getSourceFile('/static-field-reference.ts');
        var metadata = collector.getMetadata(staticSource);
        expect(metadata).toBeDefined();
        var classData = metadata.metadata['Foo'];
        expect(classData).toBeDefined();
        expect(classData.decorators).toEqual([{
                __symbolic: 'call',
                expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Component' },
                arguments: [{
                        providers: [{
                                provide: 'a',
                                useValue: {
                                    __symbolic: 'select',
                                    expression: { __symbolic: 'reference', module: './static-field', name: 'MyModule' },
                                    member: 'VALUE'
                                }
                            }]
                    }]
            }]);
    });
    it('should be able to collect a method with a conditional expression', function () {
        var source = program.getSourceFile('/static-method-with-if.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata).toBeDefined();
        var classData = metadata.metadata['MyModule'];
        expect(classData).toBeDefined();
        expect(classData.statics).toEqual({
            with: {
                __symbolic: 'function',
                parameters: ['cond'],
                value: [
                    { __symbolic: 'reference', name: 'MyModule' }, {
                        provider: 'a',
                        useValue: {
                            __symbolic: 'if',
                            condition: { __symbolic: 'reference', name: 'cond' },
                            thenExpression: '1',
                            elseExpression: '2'
                        }
                    }
                ]
            }
        });
    });
    it('should be able to collect a method with a default parameter', function () {
        var source = program.getSourceFile('/static-method-with-default.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata).toBeDefined();
        var classData = metadata.metadata['MyModule'];
        expect(classData).toBeDefined();
        expect(classData.statics).toEqual({
            with: {
                __symbolic: 'function',
                parameters: ['comp', 'foo', 'bar'],
                defaults: [undefined, true, false],
                value: [
                    { __symbolic: 'reference', name: 'MyModule' }, {
                        __symbolic: 'if',
                        condition: { __symbolic: 'reference', name: 'foo' },
                        thenExpression: { provider: 'a', useValue: { __symbolic: 'reference', name: 'comp' } },
                        elseExpression: { provider: 'b', useValue: { __symbolic: 'reference', name: 'comp' } }
                    },
                    {
                        __symbolic: 'if',
                        condition: { __symbolic: 'reference', name: 'bar' },
                        thenExpression: { provider: 'c', useValue: { __symbolic: 'reference', name: 'comp' } },
                        elseExpression: { provider: 'd', useValue: { __symbolic: 'reference', name: 'comp' } }
                    }
                ]
            }
        });
    });
    it('should be able to collect re-exported symbols', function () {
        var source = program.getSourceFile('/re-exports.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.exports).toEqual([
            { from: './static-field', export: ['MyModule'] },
            { from: './static-field-reference', export: [{ name: 'Foo', as: 'OtherModule' }] },
            { from: 'angular2/core' }
        ]);
    });
    it('should be able to collect a export as symbol', function () {
        var source = program.getSourceFile('export-as.d.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata).toEqual({ SomeFunction: { __symbolic: 'function' } });
    });
    it('should be able to collect exports with no module specifier', function () {
        var source = program.getSourceFile('/re-exports-2.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata).toEqual({
            MyClass: Object({ __symbolic: 'class' }),
            OtherModule: { __symbolic: 'reference', module: './static-field-reference', name: 'Foo' },
            MyOtherModule: { __symbolic: 'reference', module: './static-field', name: 'MyModule' }
        });
    });
    it('should collect an error symbol if collecting a reference to a non-exported symbol', function () {
        var source = program.getSourceFile('/local-symbol-ref.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata).toEqual({
            REQUIRED_VALIDATOR: {
                __symbolic: 'error',
                message: 'Reference to a local symbol',
                line: 3,
                character: 8,
                context: { name: 'REQUIRED' }
            },
            SomeComponent: {
                __symbolic: 'class',
                decorators: [{
                        __symbolic: 'call',
                        expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Component' },
                        arguments: [{ providers: [{ __symbolic: 'reference', name: 'REQUIRED_VALIDATOR' }] }]
                    }]
            }
        });
    });
    it('should collect an error symbol if collecting a reference to a non-exported function', function () {
        var source = program.getSourceFile('/local-function-ref.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata).toEqual({
            REQUIRED_VALIDATOR: {
                __symbolic: 'error',
                message: 'Reference to a non-exported function',
                line: 3,
                character: 13,
                context: { name: 'required' }
            },
            SomeComponent: {
                __symbolic: 'class',
                decorators: [{
                        __symbolic: 'call',
                        expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Component' },
                        arguments: [{ providers: [{ __symbolic: 'reference', name: 'REQUIRED_VALIDATOR' }] }]
                    }]
            }
        });
    });
    it('should collect an error for a simple function that references a local variable', function () {
        var source = program.getSourceFile('/local-symbol-ref-func.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata).toEqual({
            foo: {
                __symbolic: 'function',
                parameters: ['index'],
                value: {
                    __symbolic: 'error',
                    message: 'Reference to a local symbol',
                    line: 1,
                    character: 8,
                    context: { name: 'localSymbol' }
                }
            }
        });
    });
    it('should collect any for interface parameter reference', function () {
        var source = program.getSourceFile('/interface-reference.ts');
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata['SomeClass'].members).toEqual({
            __ctor__: [{
                    __symbolic: 'constructor',
                    parameterDecorators: [[{
                                __symbolic: 'call',
                                expression: { __symbolic: 'reference', module: 'angular2/core', name: 'Inject' },
                                arguments: ['a']
                            }]],
                    parameters: [{ __symbolic: 'reference', name: 'any' }]
                }]
        });
    });
    describe('with interpolations', function () {
        function e(expr, prefix) {
            var metadata = collectSource((prefix || '') + " export let value = " + expr + ";");
            return expect(metadata.metadata['value']);
        }
        it('should be able to collect a raw interpolated string', function () { e('`simple value`').toBe('simple value'); });
        it('should be able to interpolate a single value', function () { e('`${foo}`', 'const foo = "foo value"').toBe('foo value'); });
        it('should be able to interpolate multiple values', function () {
            e('`foo:${foo}, bar:${bar}, end`', 'const foo = "foo"; const bar = "bar";')
                .toBe('foo:foo, bar:bar, end');
        });
        it('should be able to interpolate with an imported reference', function () {
            e('`external:${external}`', 'import {external} from "./external";').toEqual({
                __symbolic: 'binop',
                operator: '+',
                left: 'external:',
                right: {
                    __symbolic: 'reference',
                    module: './external',
                    name: 'external',
                }
            });
        });
        it('should simplify a redundant template', function () {
            e('`${external}`', 'import {external} from "./external";')
                .toEqual({ __symbolic: 'reference', module: './external', name: 'external' });
        });
        it('should be able to collect complex template with imported references', function () {
            e('`foo:${foo}, bar:${bar}, end`', 'import {foo, bar} from "./external";').toEqual({
                __symbolic: 'binop',
                operator: '+',
                left: {
                    __symbolic: 'binop',
                    operator: '+',
                    left: {
                        __symbolic: 'binop',
                        operator: '+',
                        left: {
                            __symbolic: 'binop',
                            operator: '+',
                            left: 'foo:',
                            right: { __symbolic: 'reference', module: './external', name: 'foo' }
                        },
                        right: ', bar:'
                    },
                    right: { __symbolic: 'reference', module: './external', name: 'bar' }
                },
                right: ', end'
            });
        });
        it('should reject a tagged literal', function () {
            e('tag`some value`').toEqual({
                __symbolic: 'error',
                message: 'Tagged template expressions are not supported in metadata',
                line: 0,
                character: 20
            });
        });
    });
    it('should ignore |null or |undefined in type expressions', function () {
        var metadata = collectSource("\n      import {Foo} from './foo';\n      export class SomeClass {\n        constructor (a: Foo, b: Foo | null, c: Foo | undefined, d: Foo | undefined | null, e: Foo | undefined | null | Foo) {}\n      }\n    ");
        expect(metadata.metadata['SomeClass'].members).toEqual({
            __ctor__: [{
                    __symbolic: 'constructor',
                    parameters: [
                        { __symbolic: 'reference', module: './foo', name: 'Foo' },
                        { __symbolic: 'reference', module: './foo', name: 'Foo' },
                        { __symbolic: 'reference', module: './foo', name: 'Foo' },
                        { __symbolic: 'reference', module: './foo', name: 'Foo' },
                        { __symbolic: 'reference', module: './foo', name: 'Foo' }
                    ]
                }]
        });
    });
    it('should treat exported class expressions as a class', function () {
        var source = ts.createSourceFile('', "\n    export const InjectionToken: {new<T>(desc: string): InjectionToken<T>;} = class extends OpaqueToken {\n      constructor(desc: string) {\n        super(desc);\n      }\n\n      toString(): string { return `InjectionToken " + _this._desc + "`; }\n    } as any;", ts.ScriptTarget.Latest, true);
        var metadata = collector.getMetadata(source);
        expect(metadata.metadata).toEqual({ InjectionToken: { __symbolic: 'class' } });
    });
    describe('in strict mode', function () {
        it('should throw if an error symbol is collecting a reference to a non-exported symbol', function () {
            var source = program.getSourceFile('/local-symbol-ref.ts');
            expect(function () { return collector.getMetadata(source, true); }).toThrowError(/Reference to a local symbol/);
        });
        it('should throw if an error if collecting a reference to a non-exported function', function () {
            var source = program.getSourceFile('/local-function-ref.ts');
            expect(function () { return collector.getMetadata(source, true); })
                .toThrowError(/Reference to a non-exported function/);
        });
        it('should throw for references to unexpected types', function () {
            var unsupported2 = program.getSourceFile('/unsupported-2.ts');
            expect(function () { return collector.getMetadata(unsupported2, true); })
                .toThrowError(/Reference to non-exported class/);
        });
        it('should throw for errors in a static method', function () {
            var unsupported3 = program.getSourceFile('/unsupported-3.ts');
            expect(function () { return collector.getMetadata(unsupported3, true); })
                .toThrowError(/Reference to a non-exported class/);
        });
    });
    describe('with invalid input', function () {
        it('should not throw with a class with no name', function () {
            var fileName = '/invalid-class.ts';
            override(fileName, 'export class');
            var invalidClass = program.getSourceFile(fileName);
            expect(function () { return collector.getMetadata(invalidClass); }).not.toThrow();
        });
        it('should not throw with a function with no name', function () {
            var fileName = '/invalid-function.ts';
            override(fileName, 'export function');
            var invalidFunction = program.getSourceFile(fileName);
            expect(function () { return collector.getMetadata(invalidFunction); }).not.toThrow();
        });
    });
    describe('inheritance', function () {
        it('should record `extends` clauses for declared classes', function () {
            var metadata = collector.getMetadata(program.getSourceFile('/class-inheritance.ts'));
            expect(metadata.metadata['DeclaredChildClass'])
                .toEqual({ __symbolic: 'class', extends: { __symbolic: 'reference', name: 'ParentClass' } });
        });
        it('should record `extends` clauses for classes in the same file', function () {
            var metadata = collector.getMetadata(program.getSourceFile('/class-inheritance.ts'));
            expect(metadata.metadata['ChildClassSameFile'])
                .toEqual({ __symbolic: 'class', extends: { __symbolic: 'reference', name: 'ParentClass' } });
        });
        it('should record `extends` clauses for classes in a different file', function () {
            var metadata = collector.getMetadata(program.getSourceFile('/class-inheritance.ts'));
            expect(metadata.metadata['ChildClassOtherFile']).toEqual({
                __symbolic: 'class',
                extends: {
                    __symbolic: 'reference',
                    module: './class-inheritance-parent',
                    name: 'ParentClassFromOtherFile'
                }
            });
        });
        function expectClass(entry) {
            var result = schema_1.isClassMetadata(entry);
            expect(result).toBeTruthy();
            return result;
        }
        it('should collect the correct arity for a class', function () {
            var metadata = collector.getMetadata(program.getSourceFile('/class-arity.ts'));
            var zero = metadata.metadata['Zero'];
            if (expectClass(zero))
                expect(zero.arity).toBeUndefined();
            var one = metadata.metadata['One'];
            if (expectClass(one))
                expect(one.arity).toBe(1);
            var two = metadata.metadata['Two'];
            if (expectClass(two))
                expect(two.arity).toBe(2);
            var three = metadata.metadata['Three'];
            if (expectClass(three))
                expect(three.arity).toBe(3);
            var nine = metadata.metadata['Nine'];
            if (expectClass(nine))
                expect(nine.arity).toBe(9);
        });
    });
    describe('regerssion', function () {
        it('should be able to collect a short-hand property value', function () {
            var metadata = collectSource("\n        const children = { f1: 1 };\n        export const r = [\n          {path: ':locale', children}\n        ];\n      ");
            expect(metadata.metadata).toEqual({ r: [{ path: ':locale', children: { f1: 1 } }] });
        });
        // #17518
        it('should skip a default function', function () {
            var metadata = collectSource("\n        export default function () {\n\n          const mainRoutes = [\n            {name: 'a', abstract: true, component: 'main'},\n\n            {name: 'a.welcome', url: '/welcome', component: 'welcome'}\n          ];\n\n          return mainRoutes;\n\n        }");
            expect(metadata).toBeUndefined();
        });
        it('should skip a named default export', function () {
            var metadata = collectSource("\n        function mainRoutes() {\n\n          const mainRoutes = [\n            {name: 'a', abstract: true, component: 'main'},\n\n            {name: 'a.welcome', url: '/welcome', component: 'welcome'}\n          ];\n\n          return mainRoutes;\n\n        }\n\n        exports = foo;\n        ");
            expect(metadata).toBeUndefined();
        });
        it('should be able to collect an invalid access expression', function () {
            var source = createSource("\n        import {Component} from '@angular/core';\n\n        const value = [];\n        @Component({\n          provider: [{provide: 'some token', useValue: value[]}]\n        })\n        export class MyComponent {}\n      ");
            var metadata = collector.getMetadata(source);
            expect(metadata.metadata.MyComponent).toEqual({
                __symbolic: 'class',
                decorators: [{
                        __symbolic: 'error',
                        message: 'Expression form not supported',
                        line: 5,
                        character: 55
                    }]
            });
        });
    });
    describe('references', function () {
        beforeEach(function () { collector = new collector_1.MetadataCollector({ quotedNames: true }); });
        it('should record a reference to an exported field of a useValue', function () {
            var metadata = collectSource("\n        export var someValue = 1;\n        export const v = {\n          useValue: someValue\n        };\n      ");
            expect(metadata.metadata['someValue']).toEqual(1);
            expect(metadata.metadata['v']).toEqual({
                useValue: { __symbolic: 'reference', name: 'someValue' }
            });
        });
        it('should leave external references in place in an object literal', function () {
            var metadata = collectSource("\n        export const myLambda = () => [1, 2, 3];\n        const indirect = [{a: 1, b: 3: c: myLambda}];\n        export const v = {\n          v: {i: indirect}\n        }\n      ");
            expect(metadata.metadata['v']).toEqual({
                v: { i: [{ a: 1, b: 3, c: { __symbolic: 'reference', name: 'myLambda' } }] }
            });
        });
        it('should leave an external reference in place in an array literal', function () {
            var metadata = collectSource("\n        export const myLambda = () => [1, 2, 3];\n        const indirect = [1, 3, myLambda}];\n        export const v = {\n          v: {i: indirect}\n        }\n      ");
            expect(metadata.metadata['v']).toEqual({
                v: { i: [1, 3, { __symbolic: 'reference', name: 'myLambda' }] }
            });
        });
    });
    function override(fileName, content) {
        host.overrideFile(fileName, content);
        host.addFile(fileName);
        program = service.getProgram();
    }
    function collectSource(content) {
        var sourceFile = createSource(content);
        return collector.getMetadata(sourceFile);
    }
});
// TODO: Do not use \` in a template literal as it confuses clang-format
var FILES = {
    'app': {
        'app.component.ts': "\n      import {Component as MyComponent, OnInit} from 'angular2/core';\n      import * as common from 'angular2/common';\n      import {Hero} from './hero';\n      import {HeroDetailComponent} from './hero-detail.component';\n      import HeroService from './hero.service';\n      // thrown away\n      import 'angular2/core';\n\n      @MyComponent({\n        selector: 'my-app',\n        template:" +
            '`' +
            "\n        <h2>My Heroes</h2>\n        <ul class=\"heroes\">\n          <li *ngFor=\"#hero of heroes\"\n            (click)=\"onSelect(hero)\"\n            [class.selected]=\"hero === selectedHero\">\n            <span class=\"badge\">{{hero.id | lowercase}}</span> {{hero.name | uppercase}}\n          </li>\n        </ul>\n        <my-hero-detail [hero]=\"selectedHero\"></my-hero-detail>\n        " +
            '`' +
            ",\n        directives: [HeroDetailComponent, common.NgFor],\n        providers: [HeroService],\n        pipes: [common.LowerCasePipe, common.UpperCasePipe]\n      })\n      export class AppComponent implements OnInit {\n        public title = 'Tour of Heroes';\n        public heroes: Hero[];\n        public selectedHero: Hero;\n\n        constructor(private _heroService: HeroService) { }\n\n        onSelect(hero: Hero) { this.selectedHero = hero; }\n\n        ngOnInit() {\n            this.getHeroes()\n        }\n\n        getHeroes() {\n          this._heroService.getHeroesSlowly().then(heroes => this.heroes = heroes);\n        }\n      }",
        'hero.ts': "\n      export interface Hero {\n        id: number;\n        name: string;\n      }",
        'empty.ts': "",
        'hero-detail.component.ts': "\n      import {Component, Input} from 'angular2/core';\n      import {Hero} from './hero';\n\n      @Component({\n        selector: 'my-hero-detail',\n        template: " +
            '`' +
            "\n        <div *ngIf=\"hero\">\n          <h2>{{hero.name}} details!</h2>\n          <div><label>id: </label>{{hero.id}}</div>\n          <div>\n            <label>name: </label>\n            <input [(ngModel)]=\"hero.name\" placeholder=\"name\"/>\n          </div>\n        </div>\n      " +
            '`' +
            ",\n      })\n      export class HeroDetailComponent {\n        @Input() public hero: Hero;\n      }",
        'mock-heroes.ts': "\n      import {Hero as Hero} from './hero';\n\n      export const HEROES: Hero[] = [\n          {\"id\": 11, \"name\": \"Mr. Nice\"},\n          {\"id\": 12, \"name\": \"Narco\"},\n          {\"id\": 13, \"name\": \"Bombasto\"},\n          {\"id\": 14, \"name\": \"Celeritas\"},\n          {\"id\": 15, \"name\": \"Magneta\"},\n          {\"id\": 16, \"name\": \"RubberMan\"},\n          {\"id\": 17, \"name\": \"Dynama\"},\n          {\"id\": 18, \"name\": \"Dr IQ\"},\n          {\"id\": 19, \"name\": \"Magma\"},\n          {\"id\": 20, \"name\": \"Tornado\"}\n      ];",
        'default-exporter.ts': "\n      let a: string;\n      export default a;\n    ",
        'hero.service.ts': "\n      import {Injectable} from 'angular2/core';\n      import {HEROES} from './mock-heroes';\n      import {Hero} from './hero';\n\n      @Injectable()\n      class HeroService {\n          getHeros() {\n              return Promise.resolve(HEROES);\n          }\n\n          getHeroesSlowly() {\n              return new Promise<Hero[]>(resolve =>\n                setTimeout(()=>resolve(HEROES), 2000)); // 2 seconds\n          }\n      }\n      export default HeroService;",
        'cases-data.ts': "\n      import {Injectable, Input} from 'angular2/core';\n\n      @Injectable()\n      export class CaseAny {\n        constructor(param: any) {}\n      }\n\n      @Injectable()\n      export class GetProp {\n        private _name: string;\n        @Input('firstName') get name(): string {\n          return this._name;\n        }\n      }\n\n      @Injectable()\n      export class SetProp {\n        private _name: string;\n        @Input('firstName') set name(value: string) {\n          this._name = value;\n        }\n      }\n\n      @Injectable()\n      export class FullProp {\n        private _name: string;\n        @Input('firstName') get name(): string {\n          return this._name;\n        }\n        set name(value: string) {\n          this._name = value;\n        }\n      }\n\n      export class ClassReference<T> { }\n      export class NgForRow {\n\n      }\n\n      @Injectable()\n      export class NgFor {\n        constructor (public ref: ClassReference<NgForRow>) {}\n      }\n     ",
        'error-cases.ts': "\n      import HeroService from './hero.service';\n\n      export class CaseCtor {\n        constructor(private _heroService: HeroService) { }\n      }\n    "
    },
    'promise.ts': "\n    interface PromiseLike<T> {\n        then<TResult>(onfulfilled?: (value: T) => TResult | PromiseLike<TResult>, onrejected?: (reason: any) => TResult | PromiseLike<TResult>): PromiseLike<TResult>;\n        then<TResult>(onfulfilled?: (value: T) => TResult | PromiseLike<TResult>, onrejected?: (reason: any) => void): PromiseLike<TResult>;\n    }\n\n    interface Promise<T> {\n        then<TResult>(onfulfilled?: (value: T) => TResult | PromiseLike<TResult>, onrejected?: (reason: any) => TResult | PromiseLike<TResult>): Promise<TResult>;\n        then<TResult>(onfulfilled?: (value: T) => TResult | PromiseLike<TResult>, onrejected?: (reason: any) => void): Promise<TResult>;\n        catch(onrejected?: (reason: any) => T | PromiseLike<T>): Promise<T>;\n        catch(onrejected?: (reason: any) => void): Promise<T>;\n    }\n\n    interface PromiseConstructor {\n        prototype: Promise<any>;\n        new <T>(executor: (resolve: (value?: T | PromiseLike<T>) => void, reject: (reason?: any) => void) => void): Promise<T>;\n        reject(reason: any): Promise<void>;\n        reject<T>(reason: any): Promise<T>;\n        resolve<T>(value: T | PromiseLike<T>): Promise<T>;\n        resolve(): Promise<void>;\n    }\n\n    declare var Promise: PromiseConstructor;\n  ",
    'class-arity.ts': "\n    export class Zero {}\n    export class One<T> {}\n    export class Two<T, V> {}\n    export class Three<T1, T2, T3> {}\n    export class Nine<T1, T2, T3, T4, T5, T6, T7, T8, T9> {}\n  ",
    'unsupported-1.ts': "\n    export let {a, b} = {a: 1, b: 2};\n    export let [c, d] = [1, 2];\n    export let e;\n  ",
    'unsupported-2.ts': "\n    import {Injectable} from 'angular2/core';\n\n    class Foo {}\n\n    @Injectable()\n    export class Bar {\n      constructor(private f: Foo) {}\n    }\n  ",
    'unsupported-3.ts': "\n    class Foo {}\n\n    export class SomeClass {\n      static someStatic() {\n        return Foo;\n      }\n    }\n  ",
    'interface-reference.ts': "\n    import {Injectable, Inject} from 'angular2/core';\n    export interface Test {}\n\n    @Injectable()\n    export class SomeClass {\n      constructor(@Inject(\"a\") test: Test) {}\n    }\n  ",
    'import-star.ts': "\n    import {Injectable} from 'angular2/core';\n    import * as common from 'angular2/common';\n\n    @Injectable()\n    export class SomeClass {\n      constructor(private f: common.NgFor) {}\n    }\n  ",
    'exported-classes.ts': "\n    export class SimpleClass {}\n    export abstract class AbstractClass {}\n    export declare class DeclaredClass {}\n  ",
    'class-inheritance-parent.ts': "\n    export class ParentClassFromOtherFile {}\n  ",
    'class-inheritance.ts': "\n    import {ParentClassFromOtherFile} from './class-inheritance-parent';\n\n    export class ParentClass {}\n\n    export declare class DeclaredChildClass extends ParentClass {}\n\n    export class ChildClassSameFile extends ParentClass {}\n\n    export class ChildClassOtherFile extends ParentClassFromOtherFile {}\n  ",
    'exported-functions.ts': "\n    export function one(a: string, b: string, c: string) {\n      return {a: a, b: b, c: c};\n    }\n    export function two(a: string, b: string, c: string) {\n      return {a, b, c};\n    }\n    export function three({a, b, c}: {a: string, b: string, c: string}) {\n      return [a, b, c];\n    }\n    export function supportsState(): boolean {\n     return !!window.history.pushState;\n    }\n    export function complexFn(x: any): boolean {\n      if (x) {\n        return true;\n      } else {\n        return false;\n      }\n    }\n    export declare function declaredFn();\n  ",
    'exported-enum.ts': "\n    import {constValue} from './exported-consts';\n\n    export const someValue = 30;\n    export enum SomeEnum { A, B, C = 100, D };\n    export enum ComplexEnum { A, B, C = someValue, D = someValue + 10, E = constValue };\n  ",
    'exported-consts.ts': "\n    export const constValue = 100;\n  ",
    'static-method.ts': "\n    export class MyModule {\n      static with(comp: any): any[] {\n        return [\n          MyModule,\n          { provider: 'a', useValue: comp }\n        ];\n      }\n    }\n  ",
    'static-method-with-default.ts': "\n    export class MyModule {\n      static with(comp: any, foo: boolean = true, bar: boolean = false): any[] {\n        return [\n          MyModule,\n          foo ? { provider: 'a', useValue: comp } : {provider: 'b', useValue: comp},\n          bar ? { provider: 'c', useValue: comp } : {provider: 'd', useValue: comp}\n        ];\n      }\n    }\n  ",
    'static-method-call.ts': "\n    import {Component} from 'angular2/core';\n    import {MyModule} from './static-method';\n\n    @Component({\n      providers: MyModule.with('a')\n    })\n    export class Foo { }\n  ",
    'static-field.ts': "\n    export class MyModule {\n      static VALUE = 'Some string';\n    }\n  ",
    'static-field-reference.ts': "\n    import {Component} from 'angular2/core';\n    import {MyModule} from './static-field';\n\n    @Component({\n      providers: [ { provide: 'a', useValue: MyModule.VALUE } ]\n    })\n    export class Foo { }\n  ",
    'static-method-with-if.ts': "\n    export class MyModule {\n      static with(cond: boolean): any[] {\n        return [\n          MyModule,\n          { provider: 'a', useValue: cond ? '1' : '2' }\n        ];\n      }\n    }\n  ",
    're-exports.ts': "\n    export {MyModule} from './static-field';\n    export {Foo as OtherModule} from './static-field-reference';\n    export * from 'angular2/core';\n  ",
    're-exports-2.ts': "\n    import {MyModule} from './static-field';\n    import {Foo as OtherModule} from './static-field-reference';\n    class MyClass {}\n    export {OtherModule, MyModule as MyOtherModule, MyClass};\n  ",
    'export-as.d.ts': "\n     declare function someFunction(): void;\n     export { someFunction as SomeFunction };\n ",
    'local-symbol-ref.ts': "\n    import {Component, Validators} from 'angular2/core';\n\n    var REQUIRED;\n\n    export const REQUIRED_VALIDATOR: any = {\n      provide: 'SomeToken',\n      useValue: REQUIRED,\n      multi: true\n    };\n\n    @Component({\n      providers: [REQUIRED_VALIDATOR]\n    })\n    export class SomeComponent {}\n  ",
    'private-enum.ts': "\n    export enum PublicEnum { a, b, c }\n    enum PrivateEnum { e, f, g }\n  ",
    'local-function-ref.ts': "\n    import {Component, Validators} from 'angular2/core';\n\n    function required() {}\n\n    export const REQUIRED_VALIDATOR: any = {\n      provide: 'SomeToken',\n      useValue: required,\n      multi: true\n    };\n\n    @Component({\n      providers: [REQUIRED_VALIDATOR]\n    })\n    export class SomeComponent {}\n  ",
    'local-symbol-ref-func.ts': "\n    var localSymbol: any[];\n\n    export function foo(index: number): string {\n      return localSymbol[index];\n    }\n  ",
    'node_modules': {
        'angular2': {
            'core.d.ts': "\n          export interface Type extends Function { }\n          export interface TypeDecorator {\n              <T extends Type>(type: T): T;\n              (target: Object, propertyKey?: string | symbol, parameterIndex?: number): void;\n              annotations: any[];\n          }\n          export interface ComponentDecorator extends TypeDecorator { }\n          export interface ComponentFactory {\n              (obj: {\n                  selector?: string;\n                  inputs?: string[];\n                  outputs?: string[];\n                  properties?: string[];\n                  events?: string[];\n                  host?: {\n                      [key: string]: string;\n                  };\n                  bindings?: any[];\n                  providers?: any[];\n                  exportAs?: string;\n                  moduleId?: string;\n                  queries?: {\n                      [key: string]: any;\n                  };\n                  viewBindings?: any[];\n                  viewProviders?: any[];\n                  templateUrl?: string;\n                  template?: string;\n                  styleUrls?: string[];\n                  styles?: string[];\n                  directives?: Array<Type | any[]>;\n                  pipes?: Array<Type | any[]>;\n              }): ComponentDecorator;\n          }\n          export declare var Component: ComponentFactory;\n          export interface InputFactory {\n              (bindingPropertyName?: string): any;\n              new (bindingPropertyName?: string): any;\n          }\n          export declare var Input: InputFactory;\n          export interface InjectableFactory {\n              (): any;\n          }\n          export declare var Injectable: InjectableFactory;\n          export interface InjectFactory {\n            (binding?: any): any;\n            new (binding?: any): any;\n          }\n          export declare var Inject: InjectFactory;\n          export interface OnInit {\n              ngOnInit(): any;\n          }\n          export class Validators {\n            static required(): void;\n          }\n      ",
            'common.d.ts': "\n        export declare class NgFor {\n            ngForOf: any;\n            ngForTemplate: any;\n            ngDoCheck(): void;\n        }\n        export declare class LowerCasePipe  {\n          transform(value: string, args?: any[]): string;\n        }\n        export declare class UpperCasePipe {\n            transform(value: string, args?: any[]): string;\n        }\n      "
        }
    }
};
function createSource(text) {
    return ts.createSourceFile('', text, ts.ScriptTarget.Latest, true);
}
//# sourceMappingURL=collector.spec.js.map