import * as ts from 'typescript';
import { CodeWithSourceMap } from './metadata';
export interface UrlResolver {
    (url: string, d: ts.Decorator): string;
}
export interface TemplateTransformer {
    (template: string, url: string, d: ts.Decorator): CodeWithSourceMap;
}
export interface StyleTransformer {
    (style: string, url: string, d: ts.Decorator): CodeWithSourceMap;
}
export declare const LogLevel: {
    None: number;
    Error: number;
    Info: number;
    Debug: number;
};
export interface Config {
    interpolation: [string, string];
    resolveUrl: UrlResolver;
    transformTemplate: TemplateTransformer;
    transformStyle: StyleTransformer;
    predefinedDirectives: DirectiveDeclaration[];
    logLevel: number;
}
export interface DirectiveDeclaration {
    selector: string;
    exportAs?: string;
    inputs?: string[];
    outputs?: string[];
    hostProperties?: string[];
    hostAttributes?: string[];
    hostListeners?: string[];
}
export declare const Config: Config;
