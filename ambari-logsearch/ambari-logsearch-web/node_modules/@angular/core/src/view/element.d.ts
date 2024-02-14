/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { RendererType2 } from '../render/api';
import { SecurityContext } from '../security';
import { BindingFlags, ElementData, ElementHandleEventFn, NodeDef, NodeFlags, QueryValueType, ViewData, ViewDefinitionFactory } from './types';
export declare function anchorDef(flags: NodeFlags, matchedQueriesDsl: [string | number, QueryValueType][], ngContentIndex: number, childCount: number, handleEvent?: ElementHandleEventFn, templateFactory?: ViewDefinitionFactory): NodeDef;
export declare function elementDef(flags: NodeFlags, matchedQueriesDsl: [string | number, QueryValueType][], ngContentIndex: number, childCount: number, namespaceAndName: string, fixedAttrs?: [string, string][], bindings?: [BindingFlags, string, string | SecurityContext][], outputs?: ([string, string])[], handleEvent?: ElementHandleEventFn, componentView?: ViewDefinitionFactory, componentRendererType?: RendererType2 | null): NodeDef;
export declare function createElement(view: ViewData, renderHost: any, def: NodeDef): ElementData;
export declare function listenToElementOutputs(view: ViewData, compView: ViewData, def: NodeDef, el: any): void;
export declare function checkAndUpdateElementInline(view: ViewData, def: NodeDef, v0: any, v1: any, v2: any, v3: any, v4: any, v5: any, v6: any, v7: any, v8: any, v9: any): boolean;
export declare function checkAndUpdateElementDynamic(view: ViewData, def: NodeDef, values: any[]): boolean;
