import { ModuleWithProviders, Provider } from "@angular/core";
export * from "./src/translate.loader";
export * from "./src/translate.service";
export * from "./src/missing-translation-handler";
export * from "./src/translate.parser";
export * from "./src/translate.directive";
export * from "./src/translate.pipe";
export interface TranslateModuleConfig {
    loader?: Provider;
    parser?: Provider;
    missingTranslationHandler?: Provider;
    isolate?: boolean;
}
export declare class TranslateModule {
    /**
     * Use this method in your root module to provide the TranslateService
     * @param {TranslateModuleConfig} config
     * @returns {ModuleWithProviders}
     */
    static forRoot(config?: TranslateModuleConfig): ModuleWithProviders;
    /**
     * Use this method in your other (non root) modules to import the directive/pipe
     * @param {TranslateModuleConfig} config
     * @returns {ModuleWithProviders}
     */
    static forChild(config?: TranslateModuleConfig): ModuleWithProviders;
}
