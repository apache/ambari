/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Component } from '@angular/core';
import { CompileReflector } from '../compile_reflector';
import * as o from '../output/output_ast';
export declare class JitReflector implements CompileReflector {
    private reflectionCapabilities;
    constructor();
    componentModuleUrl(type: any, cmpMetadata: Component): string;
    parameters(typeOrFunc: any): any[][];
    annotations(typeOrFunc: any): any[];
    propMetadata(typeOrFunc: any): {
        [key: string]: any[];
    };
    hasLifecycleHook(type: any, lcProperty: string): boolean;
    resolveExternalReference(ref: o.ExternalReference): any;
}
