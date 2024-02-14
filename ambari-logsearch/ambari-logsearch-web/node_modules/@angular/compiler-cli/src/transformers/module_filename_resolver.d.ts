import * as ts from 'typescript';
import { CompilerOptions, ModuleFilenameResolver } from './api';
export declare function createModuleFilenameResolver(tsHost: ts.ModuleResolutionHost, options: CompilerOptions): ModuleFilenameResolver;
