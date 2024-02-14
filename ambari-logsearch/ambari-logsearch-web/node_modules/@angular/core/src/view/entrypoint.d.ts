import { NgModuleFactory } from '../linker/ng_module_factory';
import { Type } from '../type';
import { NgModuleDefinitionFactory, ProviderOverride } from './types';
export declare function overrideProvider(override: ProviderOverride): void;
export declare function clearProviderOverrides(): void;
export declare function createNgModuleFactory(ngModuleType: Type<any>, bootstrapComponents: Type<any>[], defFactory: NgModuleDefinitionFactory): NgModuleFactory<any>;
