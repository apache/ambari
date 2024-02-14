/**
 * @license Angular v4.4.3
 * (c) 2010-2017 Google, Inc. https://angular.io/
 * License: MIT
 */
import { COMPILER_OPTIONS, Compiler, CompilerFactory, Component, Directive, Injectable, Injector, NgModule, Pipe, SecurityContext, createPlatformFactory, ɵstringify } from '@angular/core';
import { CompileMetadataResolver, CompileReflector, DirectiveResolver, NgModuleResolver, PipeResolver, platformCoreDynamic } from '@angular/compiler';
import { ɵTestingCompilerFactory } from '@angular/core/testing';

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
class MockSchemaRegistry {
    constructor(existingProperties, attrPropMapping, existingElements, invalidProperties, invalidAttributes) {
        this.existingProperties = existingProperties;
        this.attrPropMapping = attrPropMapping;
        this.existingElements = existingElements;
        this.invalidProperties = invalidProperties;
        this.invalidAttributes = invalidAttributes;
    }
    hasProperty(tagName, property, schemas) {
        const value = this.existingProperties[property];
        return value === void 0 ? true : value;
    }
    hasElement(tagName, schemaMetas) {
        const value = this.existingElements[tagName.toLowerCase()];
        return value === void 0 ? true : value;
    }
    allKnownElementNames() { return Object.keys(this.existingElements); }
    securityContext(selector, property, isAttribute) {
        return SecurityContext.NONE;
    }
    getMappedPropName(attrName) { return this.attrPropMapping[attrName] || attrName; }
    getDefaultComponentElementName() { return 'ng-component'; }
    validateProperty(name) {
        if (this.invalidProperties.indexOf(name) > -1) {
            return { error: true, msg: `Binding to property '${name}' is disallowed for security reasons` };
        }
        else {
            return { error: false };
        }
    }
    validateAttribute(name) {
        if (this.invalidAttributes.indexOf(name) > -1) {
            return {
                error: true,
                msg: `Binding to attribute '${name}' is disallowed for security reasons`
            };
        }
        else {
            return { error: false };
        }
    }
    normalizeAnimationStyleProperty(propName) { return propName; }
    normalizeAnimationStyleValue(camelCaseProp, userProvidedProp, val) {
        return { error: null, value: val.toString() };
    }
}

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * An implementation of {@link DirectiveResolver} that allows overriding
 * various properties of directives.
 */
class MockDirectiveResolver extends DirectiveResolver {
    constructor(_injector, reflector) {
        super(reflector);
        this._injector = _injector;
        this._directives = new Map();
        this._providerOverrides = new Map();
        this._viewProviderOverrides = new Map();
        this._views = new Map();
        this._inlineTemplates = new Map();
    }
    get _compiler() { return this._injector.get(Compiler); }
    _clearCacheFor(component) { this._compiler.clearCacheFor(component); }
    resolve(type, throwIfNotFound = true) {
        let metadata = this._directives.get(type) || null;
        if (!metadata) {
            metadata = super.resolve(type, throwIfNotFound);
        }
        if (!metadata) {
            return null;
        }
        const providerOverrides = this._providerOverrides.get(type);
        const viewProviderOverrides = this._viewProviderOverrides.get(type);
        let providers = metadata.providers;
        if (providerOverrides != null) {
            const originalViewProviders = metadata.providers || [];
            providers = originalViewProviders.concat(providerOverrides);
        }
        if (metadata instanceof Component) {
            let viewProviders = metadata.viewProviders;
            if (viewProviderOverrides != null) {
                const originalViewProviders = metadata.viewProviders || [];
                viewProviders = originalViewProviders.concat(viewProviderOverrides);
            }
            let view = this._views.get(type) || metadata;
            let animations = view.animations;
            let templateUrl = view.templateUrl;
            let inlineTemplate = this._inlineTemplates.get(type);
            if (inlineTemplate) {
                templateUrl = undefined;
            }
            else {
                inlineTemplate = view.template;
            }
            return new Component({
                selector: metadata.selector,
                inputs: metadata.inputs,
                outputs: metadata.outputs,
                host: metadata.host,
                exportAs: metadata.exportAs,
                moduleId: metadata.moduleId,
                queries: metadata.queries,
                changeDetection: metadata.changeDetection,
                providers: providers,
                viewProviders: viewProviders,
                entryComponents: metadata.entryComponents,
                template: inlineTemplate,
                templateUrl: templateUrl,
                animations: animations,
                styles: view.styles,
                styleUrls: view.styleUrls,
                encapsulation: view.encapsulation,
                interpolation: view.interpolation,
                preserveWhitespaces: view.preserveWhitespaces,
            });
        }
        return new Directive({
            selector: metadata.selector,
            inputs: metadata.inputs,
            outputs: metadata.outputs,
            host: metadata.host,
            providers: providers,
            exportAs: metadata.exportAs,
            queries: metadata.queries
        });
    }
    /**
     * Overrides the {@link Directive} for a directive.
     */
    setDirective(type, metadata) {
        this._directives.set(type, metadata);
        this._clearCacheFor(type);
    }
    setProvidersOverride(type, providers) {
        this._providerOverrides.set(type, providers);
        this._clearCacheFor(type);
    }
    setViewProvidersOverride(type, viewProviders) {
        this._viewProviderOverrides.set(type, viewProviders);
        this._clearCacheFor(type);
    }
    /**
     * Overrides the {@link ViewMetadata} for a component.
     */
    setView(component, view) {
        this._views.set(component, view);
        this._clearCacheFor(component);
    }
    /**
     * Overrides the inline template for a component - other configuration remains unchanged.
     */
    setInlineTemplate(component, template) {
        this._inlineTemplates.set(component, template);
        this._clearCacheFor(component);
    }
}
MockDirectiveResolver.decorators = [
    { type: Injectable },
];
/** @nocollapse */
MockDirectiveResolver.ctorParameters = () => [
    { type: Injector, },
    { type: CompileReflector, },
];

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
class MockNgModuleResolver extends NgModuleResolver {
    constructor(_injector, reflector) {
        super(reflector);
        this._injector = _injector;
        this._ngModules = new Map();
    }
    /**
     * Overrides the {@link NgModule} for a module.
     */
    setNgModule(type, metadata) {
        this._ngModules.set(type, metadata);
        this._clearCacheFor(type);
    }
    /**
     * Returns the {@link NgModule} for a module:
     * - Set the {@link NgModule} to the overridden view when it exists or fallback to the
     * default
     * `NgModuleResolver`, see `setNgModule`.
     */
    resolve(type, throwIfNotFound = true) {
        return this._ngModules.get(type) || super.resolve(type, throwIfNotFound);
    }
    get _compiler() { return this._injector.get(Compiler); }
    _clearCacheFor(component) { this._compiler.clearCacheFor(component); }
}
MockNgModuleResolver.decorators = [
    { type: Injectable },
];
/** @nocollapse */
MockNgModuleResolver.ctorParameters = () => [
    { type: Injector, },
    { type: CompileReflector, },
];

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
class MockPipeResolver extends PipeResolver {
    constructor(_injector, refector) {
        super(refector);
        this._injector = _injector;
        this._pipes = new Map();
    }
    get _compiler() { return this._injector.get(Compiler); }
    _clearCacheFor(pipe) { this._compiler.clearCacheFor(pipe); }
    /**
     * Overrides the {@link Pipe} for a pipe.
     */
    setPipe(type, metadata) {
        this._pipes.set(type, metadata);
        this._clearCacheFor(type);
    }
    /**
     * Returns the {@link Pipe} for a pipe:
     * - Set the {@link Pipe} to the overridden view when it exists or fallback to the
     * default
     * `PipeResolver`, see `setPipe`.
     */
    resolve(type, throwIfNotFound = true) {
        let metadata = this._pipes.get(type);
        if (!metadata) {
            metadata = super.resolve(type, throwIfNotFound);
        }
        return metadata;
    }
}
MockPipeResolver.decorators = [
    { type: Injectable },
];
/** @nocollapse */
MockPipeResolver.ctorParameters = () => [
    { type: Injector, },
    { type: CompileReflector, },
];

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
let _nextReferenceId = 0;
class MetadataOverrider {
    constructor() {
        this._references = new Map();
    }
    /**
     * Creates a new instance for the given metadata class
     * based on an old instance and overrides.
     */
    overrideMetadata(metadataClass, oldMetadata, override) {
        const props = {};
        if (oldMetadata) {
            _valueProps(oldMetadata).forEach((prop) => props[prop] = oldMetadata[prop]);
        }
        if (override.set) {
            if (override.remove || override.add) {
                throw new Error(`Cannot set and add/remove ${ɵstringify(metadataClass)} at the same time!`);
            }
            setMetadata(props, override.set);
        }
        if (override.remove) {
            removeMetadata(props, override.remove, this._references);
        }
        if (override.add) {
            addMetadata(props, override.add);
        }
        return new metadataClass(props);
    }
}
function removeMetadata(metadata, remove, references) {
    const removeObjects = new Set();
    for (const prop in remove) {
        const removeValue = remove[prop];
        if (removeValue instanceof Array) {
            removeValue.forEach((value) => { removeObjects.add(_propHashKey(prop, value, references)); });
        }
        else {
            removeObjects.add(_propHashKey(prop, removeValue, references));
        }
    }
    for (const prop in metadata) {
        const propValue = metadata[prop];
        if (propValue instanceof Array) {
            metadata[prop] = propValue.filter((value) => !removeObjects.has(_propHashKey(prop, value, references)));
        }
        else {
            if (removeObjects.has(_propHashKey(prop, propValue, references))) {
                metadata[prop] = undefined;
            }
        }
    }
}
function addMetadata(metadata, add) {
    for (const prop in add) {
        const addValue = add[prop];
        const propValue = metadata[prop];
        if (propValue != null && propValue instanceof Array) {
            metadata[prop] = propValue.concat(addValue);
        }
        else {
            metadata[prop] = addValue;
        }
    }
}
function setMetadata(metadata, set) {
    for (const prop in set) {
        metadata[prop] = set[prop];
    }
}
function _propHashKey(propName, propValue, references) {
    const replacer = (key, value) => {
        if (typeof value === 'function') {
            value = _serializeReference(value, references);
        }
        return value;
    };
    return `${propName}:${JSON.stringify(propValue, replacer)}`;
}
function _serializeReference(ref, references) {
    let id = references.get(ref);
    if (!id) {
        id = `${ɵstringify(ref)}${_nextReferenceId++}`;
        references.set(ref, id);
    }
    return id;
}
function _valueProps(obj) {
    const props = [];
    // regular public props
    Object.keys(obj).forEach((prop) => {
        if (!prop.startsWith('_')) {
            props.push(prop);
        }
    });
    // getters
    let proto = obj;
    while (proto = Object.getPrototypeOf(proto)) {
        Object.keys(proto).forEach((protoProp) => {
            const desc = Object.getOwnPropertyDescriptor(proto, protoProp);
            if (!protoProp.startsWith('_') && desc && 'get' in desc) {
                props.push(protoProp);
            }
        });
    }
    return props;
}

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * @module
 * @description
 * Entry point for all APIs of the compiler package.
 *
 * <div class="callout is-critical">
 *   <header>Unstable APIs</header>
 *   <p>
 *     All compiler apis are currently considered experimental and private!
 *   </p>
 *   <p>
 *     We expect the APIs in this package to keep on changing. Do not rely on them.
 *   </p>
 * </div>
 */
class TestingCompilerFactoryImpl {
    constructor(_compilerFactory) {
        this._compilerFactory = _compilerFactory;
    }
    createTestingCompiler(options) {
        const compiler = this._compilerFactory.createCompiler(options);
        return new TestingCompilerImpl(compiler, compiler.injector.get(MockDirectiveResolver), compiler.injector.get(MockPipeResolver), compiler.injector.get(MockNgModuleResolver), compiler.injector.get(CompileMetadataResolver));
    }
}
TestingCompilerFactoryImpl.decorators = [
    { type: Injectable },
];
/** @nocollapse */
TestingCompilerFactoryImpl.ctorParameters = () => [
    { type: CompilerFactory, },
];
class TestingCompilerImpl {
    constructor(_compiler, _directiveResolver, _pipeResolver, _moduleResolver, _metadataResolver) {
        this._compiler = _compiler;
        this._directiveResolver = _directiveResolver;
        this._pipeResolver = _pipeResolver;
        this._moduleResolver = _moduleResolver;
        this._metadataResolver = _metadataResolver;
        this._overrider = new MetadataOverrider();
    }
    get injector() { return this._compiler.injector; }
    compileModuleSync(moduleType) {
        return this._compiler.compileModuleSync(moduleType);
    }
    compileModuleAsync(moduleType) {
        return this._compiler.compileModuleAsync(moduleType);
    }
    compileModuleAndAllComponentsSync(moduleType) {
        return this._compiler.compileModuleAndAllComponentsSync(moduleType);
    }
    compileModuleAndAllComponentsAsync(moduleType) {
        return this._compiler.compileModuleAndAllComponentsAsync(moduleType);
    }
    getNgContentSelectors(component) {
        return this._compiler.getNgContentSelectors(component);
    }
    getComponentFactory(component) {
        return this._compiler.getComponentFactory(component);
    }
    checkOverrideAllowed(type) {
        if (this._compiler.hasAotSummary(type)) {
            throw new Error(`${ɵstringify(type)} was AOT compiled, so its metadata cannot be changed.`);
        }
    }
    overrideModule(ngModule, override) {
        this.checkOverrideAllowed(ngModule);
        const oldMetadata = this._moduleResolver.resolve(ngModule, false);
        this._moduleResolver.setNgModule(ngModule, this._overrider.overrideMetadata(NgModule, oldMetadata, override));
    }
    overrideDirective(directive, override) {
        this.checkOverrideAllowed(directive);
        const oldMetadata = this._directiveResolver.resolve(directive, false);
        this._directiveResolver.setDirective(directive, this._overrider.overrideMetadata(Directive, oldMetadata, override));
    }
    overrideComponent(component, override) {
        this.checkOverrideAllowed(component);
        const oldMetadata = this._directiveResolver.resolve(component, false);
        this._directiveResolver.setDirective(component, this._overrider.overrideMetadata(Component, oldMetadata, override));
    }
    overridePipe(pipe, override) {
        this.checkOverrideAllowed(pipe);
        const oldMetadata = this._pipeResolver.resolve(pipe, false);
        this._pipeResolver.setPipe(pipe, this._overrider.overrideMetadata(Pipe, oldMetadata, override));
    }
    loadAotSummaries(summaries) { this._compiler.loadAotSummaries(summaries); }
    clearCache() { this._compiler.clearCache(); }
    clearCacheFor(type) { this._compiler.clearCacheFor(type); }
}
/**
 * Platform for dynamic tests
 *
 * @experimental
 */
const platformCoreDynamicTesting = createPlatformFactory(platformCoreDynamic, 'coreDynamicTesting', [
    {
        provide: COMPILER_OPTIONS,
        useValue: {
            providers: [
                MockPipeResolver,
                { provide: PipeResolver, useExisting: MockPipeResolver },
                MockDirectiveResolver,
                { provide: DirectiveResolver, useExisting: MockDirectiveResolver },
                MockNgModuleResolver,
                { provide: NgModuleResolver, useExisting: MockNgModuleResolver },
            ]
        },
        multi: true
    },
    { provide: ɵTestingCompilerFactory, useClass: TestingCompilerFactoryImpl }
]);

/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * @module
 * @description
 * Entry point for all public APIs of the compiler/testing package.
 */

export { TestingCompilerFactoryImpl, TestingCompilerImpl, platformCoreDynamicTesting, MockSchemaRegistry, MockDirectiveResolver, MockNgModuleResolver, MockPipeResolver };
//# sourceMappingURL=testing.js.map
