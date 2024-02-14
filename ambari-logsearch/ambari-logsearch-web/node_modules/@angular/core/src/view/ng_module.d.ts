import { DepDef, DepFlags, NgModuleData, NgModuleDefinition, NgModuleProviderDef, NodeFlags } from './types';
export declare function moduleProvideDef(flags: NodeFlags, token: any, value: any, deps: ([DepFlags, any] | any)[]): NgModuleProviderDef;
export declare function moduleDef(providers: NgModuleProviderDef[]): NgModuleDefinition;
export declare function initNgModule(data: NgModuleData): void;
export declare function resolveNgModuleDep(data: NgModuleData, depDef: DepDef, notFoundValue?: any): any;
export declare function callNgModuleLifecycle(ngModule: NgModuleData, lifecycles: NodeFlags): void;
