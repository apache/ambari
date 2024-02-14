"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const fs_1 = require("fs");
const transform_javascript_1 = require("../helpers/transform-javascript");
const class_fold_1 = require("../transforms/class-fold");
const import_tslib_1 = require("../transforms/import-tslib");
const prefix_classes_1 = require("../transforms/prefix-classes");
const prefix_functions_1 = require("../transforms/prefix-functions");
const scrub_file_1 = require("../transforms/scrub-file");
const wrap_enums_1 = require("../transforms/wrap-enums");
const whitelistedAngularModules = [
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)animations(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)common(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)compiler(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)core(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)forms(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)http(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)platform-browser-dynamic(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)platform-browser(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)platform-webworker-dynamic(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)platform-webworker(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)router(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)upgrade(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)material(\\|\/)/,
    /(\\|\/)node_modules(\\|\/)@angular(\\|\/)cdk(\\|\/)/,
];
function buildOptimizer(options) {
    const { inputFilePath } = options;
    let { content } = options;
    if (!inputFilePath && content === undefined) {
        throw new Error('Either filePath or content must be specified in options.');
    }
    if (content === undefined) {
        content = fs_1.readFileSync(inputFilePath, 'UTF-8');
    }
    // Determine which transforms to apply.
    const getTransforms = [];
    if (wrap_enums_1.testWrapEnums(content)) {
        getTransforms.push(wrap_enums_1.getWrapEnumsTransformer);
    }
    if (import_tslib_1.testImportTslib(content)) {
        getTransforms.push(import_tslib_1.getImportTslibTransformer);
    }
    if (prefix_classes_1.testPrefixClasses(content)) {
        getTransforms.push(prefix_classes_1.getPrefixClassesTransformer);
    }
    if (inputFilePath
        && whitelistedAngularModules.some((re) => re.test(inputFilePath))) {
        getTransforms.push(
        // getPrefixFunctionsTransformer is rather dangerous, apply only to known pure modules.
        // It will mark both `require()` calls and `console.log(stuff)` as pure.
        // We only apply it to whitelisted modules, since we know they are safe.
        // getPrefixFunctionsTransformer needs to be before getFoldFileTransformer.
        prefix_functions_1.getPrefixFunctionsTransformer, scrub_file_1.getScrubFileTransformer, class_fold_1.getFoldFileTransformer);
    }
    else if (scrub_file_1.testScrubFile(content)) {
        getTransforms.push(scrub_file_1.getScrubFileTransformer, class_fold_1.getFoldFileTransformer);
    }
    return transform_javascript_1.transformJavascript(Object.assign({}, options, { getTransforms, content }));
}
exports.buildOptimizer = buildOptimizer;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiYnVpbGQtb3B0aW1pemVyLmpzIiwic291cmNlUm9vdCI6Ii9Vc2Vycy9oYW5zbC9Tb3VyY2VzL2RldmtpdC8iLCJzb3VyY2VzIjpbInBhY2thZ2VzL2FuZ3VsYXJfZGV2a2l0L2J1aWxkX29wdGltaXplci9zcmMvYnVpbGQtb3B0aW1pemVyL2J1aWxkLW9wdGltaXplci50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOztBQUFBOzs7Ozs7R0FNRztBQUNILDJCQUFrQztBQUNsQywwRUFBaUc7QUFDakcseURBQWtFO0FBQ2xFLDZEQUF3RjtBQUN4RixpRUFBOEY7QUFDOUYscUVBQStFO0FBQy9FLHlEQUFrRjtBQUNsRix5REFBa0Y7QUFHbEYsTUFBTSx5QkFBeUIsR0FBRztJQUNoQyw0REFBNEQ7SUFDNUQsd0RBQXdEO0lBQ3hELDBEQUEwRDtJQUMxRCxzREFBc0Q7SUFDdEQsdURBQXVEO0lBQ3ZELHNEQUFzRDtJQUN0RCwwRUFBMEU7SUFDMUUsa0VBQWtFO0lBQ2xFLDRFQUE0RTtJQUM1RSxvRUFBb0U7SUFDcEUsd0RBQXdEO0lBQ3hELHlEQUF5RDtJQUN6RCwwREFBMEQ7SUFDMUQscURBQXFEO0NBQ3RELENBQUM7QUFVRix3QkFBK0IsT0FBOEI7SUFFM0QsTUFBTSxFQUFFLGFBQWEsRUFBRSxHQUFHLE9BQU8sQ0FBQztJQUNsQyxJQUFJLEVBQUUsT0FBTyxFQUFFLEdBQUcsT0FBTyxDQUFDO0lBRTFCLEVBQUUsQ0FBQyxDQUFDLENBQUMsYUFBYSxJQUFJLE9BQU8sS0FBSyxTQUFTLENBQUMsQ0FBQyxDQUFDO1FBQzVDLE1BQU0sSUFBSSxLQUFLLENBQUMsMERBQTBELENBQUMsQ0FBQztJQUM5RSxDQUFDO0lBRUQsRUFBRSxDQUFDLENBQUMsT0FBTyxLQUFLLFNBQVMsQ0FBQyxDQUFDLENBQUM7UUFDMUIsT0FBTyxHQUFHLGlCQUFZLENBQUMsYUFBdUIsRUFBRSxPQUFPLENBQUMsQ0FBQztJQUMzRCxDQUFDO0lBRUQsdUNBQXVDO0lBQ3ZDLE1BQU0sYUFBYSxHQUFHLEVBQUUsQ0FBQztJQUV6QixFQUFFLENBQUMsQ0FBQywwQkFBYSxDQUFDLE9BQU8sQ0FBQyxDQUFDLENBQUMsQ0FBQztRQUMzQixhQUFhLENBQUMsSUFBSSxDQUFDLG9DQUF1QixDQUFDLENBQUM7SUFDOUMsQ0FBQztJQUVELEVBQUUsQ0FBQyxDQUFDLDhCQUFlLENBQUMsT0FBTyxDQUFDLENBQUMsQ0FBQyxDQUFDO1FBQzdCLGFBQWEsQ0FBQyxJQUFJLENBQUMsd0NBQXlCLENBQUMsQ0FBQztJQUNoRCxDQUFDO0lBRUQsRUFBRSxDQUFDLENBQUMsa0NBQWlCLENBQUMsT0FBTyxDQUFDLENBQUMsQ0FBQyxDQUFDO1FBQy9CLGFBQWEsQ0FBQyxJQUFJLENBQUMsNENBQTJCLENBQUMsQ0FBQztJQUNsRCxDQUFDO0lBRUQsRUFBRSxDQUFDLENBQUMsYUFBYTtXQUNaLHlCQUF5QixDQUFDLElBQUksQ0FBQyxDQUFDLEVBQUUsS0FBSyxFQUFFLENBQUMsSUFBSSxDQUFDLGFBQWEsQ0FBQyxDQUNsRSxDQUFDLENBQUMsQ0FBQztRQUNELGFBQWEsQ0FBQyxJQUFJO1FBQ2hCLHVGQUF1RjtRQUN2Rix3RUFBd0U7UUFDeEUsd0VBQXdFO1FBQ3hFLDJFQUEyRTtRQUMzRSxnREFBNkIsRUFDN0Isb0NBQXVCLEVBQ3ZCLG1DQUFzQixDQUN2QixDQUFDO0lBQ0osQ0FBQztJQUFDLElBQUksQ0FBQyxFQUFFLENBQUMsQ0FBQywwQkFBYSxDQUFDLE9BQU8sQ0FBQyxDQUFDLENBQUMsQ0FBQztRQUNsQyxhQUFhLENBQUMsSUFBSSxDQUNoQixvQ0FBdUIsRUFDdkIsbUNBQXNCLENBQ3ZCLENBQUM7SUFDSixDQUFDO0lBRUQsTUFBTSxDQUFDLDBDQUFtQixtQkFBTSxPQUFPLElBQUUsYUFBYSxFQUFFLE9BQU8sSUFBRyxDQUFDO0FBQ3JFLENBQUM7QUFoREQsd0NBZ0RDIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuaW1wb3J0IHsgcmVhZEZpbGVTeW5jIH0gZnJvbSAnZnMnO1xuaW1wb3J0IHsgVHJhbnNmb3JtSmF2YXNjcmlwdE91dHB1dCwgdHJhbnNmb3JtSmF2YXNjcmlwdCB9IGZyb20gJy4uL2hlbHBlcnMvdHJhbnNmb3JtLWphdmFzY3JpcHQnO1xuaW1wb3J0IHsgZ2V0Rm9sZEZpbGVUcmFuc2Zvcm1lciB9IGZyb20gJy4uL3RyYW5zZm9ybXMvY2xhc3MtZm9sZCc7XG5pbXBvcnQgeyBnZXRJbXBvcnRUc2xpYlRyYW5zZm9ybWVyLCB0ZXN0SW1wb3J0VHNsaWIgfSBmcm9tICcuLi90cmFuc2Zvcm1zL2ltcG9ydC10c2xpYic7XG5pbXBvcnQgeyBnZXRQcmVmaXhDbGFzc2VzVHJhbnNmb3JtZXIsIHRlc3RQcmVmaXhDbGFzc2VzIH0gZnJvbSAnLi4vdHJhbnNmb3Jtcy9wcmVmaXgtY2xhc3Nlcyc7XG5pbXBvcnQgeyBnZXRQcmVmaXhGdW5jdGlvbnNUcmFuc2Zvcm1lciB9IGZyb20gJy4uL3RyYW5zZm9ybXMvcHJlZml4LWZ1bmN0aW9ucyc7XG5pbXBvcnQgeyBnZXRTY3J1YkZpbGVUcmFuc2Zvcm1lciwgdGVzdFNjcnViRmlsZSB9IGZyb20gJy4uL3RyYW5zZm9ybXMvc2NydWItZmlsZSc7XG5pbXBvcnQgeyBnZXRXcmFwRW51bXNUcmFuc2Zvcm1lciwgdGVzdFdyYXBFbnVtcyB9IGZyb20gJy4uL3RyYW5zZm9ybXMvd3JhcC1lbnVtcyc7XG5cblxuY29uc3Qgd2hpdGVsaXN0ZWRBbmd1bGFyTW9kdWxlcyA9IFtcbiAgLyhcXFxcfFxcLylub2RlX21vZHVsZXMoXFxcXHxcXC8pQGFuZ3VsYXIoXFxcXHxcXC8pYW5pbWF0aW9ucyhcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLyljb21tb24oXFxcXHxcXC8pLyxcbiAgLyhcXFxcfFxcLylub2RlX21vZHVsZXMoXFxcXHxcXC8pQGFuZ3VsYXIoXFxcXHxcXC8pY29tcGlsZXIoXFxcXHxcXC8pLyxcbiAgLyhcXFxcfFxcLylub2RlX21vZHVsZXMoXFxcXHxcXC8pQGFuZ3VsYXIoXFxcXHxcXC8pY29yZShcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLylmb3JtcyhcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLylodHRwKFxcXFx8XFwvKS8sXG4gIC8oXFxcXHxcXC8pbm9kZV9tb2R1bGVzKFxcXFx8XFwvKUBhbmd1bGFyKFxcXFx8XFwvKXBsYXRmb3JtLWJyb3dzZXItZHluYW1pYyhcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLylwbGF0Zm9ybS1icm93c2VyKFxcXFx8XFwvKS8sXG4gIC8oXFxcXHxcXC8pbm9kZV9tb2R1bGVzKFxcXFx8XFwvKUBhbmd1bGFyKFxcXFx8XFwvKXBsYXRmb3JtLXdlYndvcmtlci1keW5hbWljKFxcXFx8XFwvKS8sXG4gIC8oXFxcXHxcXC8pbm9kZV9tb2R1bGVzKFxcXFx8XFwvKUBhbmd1bGFyKFxcXFx8XFwvKXBsYXRmb3JtLXdlYndvcmtlcihcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLylyb3V0ZXIoXFxcXHxcXC8pLyxcbiAgLyhcXFxcfFxcLylub2RlX21vZHVsZXMoXFxcXHxcXC8pQGFuZ3VsYXIoXFxcXHxcXC8pdXBncmFkZShcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLyltYXRlcmlhbChcXFxcfFxcLykvLFxuICAvKFxcXFx8XFwvKW5vZGVfbW9kdWxlcyhcXFxcfFxcLylAYW5ndWxhcihcXFxcfFxcLyljZGsoXFxcXHxcXC8pLyxcbl07XG5cbmV4cG9ydCBpbnRlcmZhY2UgQnVpbGRPcHRpbWl6ZXJPcHRpb25zIHtcbiAgY29udGVudD86IHN0cmluZztcbiAgaW5wdXRGaWxlUGF0aD86IHN0cmluZztcbiAgb3V0cHV0RmlsZVBhdGg/OiBzdHJpbmc7XG4gIGVtaXRTb3VyY2VNYXA/OiBib29sZWFuO1xuICBzdHJpY3Q/OiBib29sZWFuO1xufVxuXG5leHBvcnQgZnVuY3Rpb24gYnVpbGRPcHRpbWl6ZXIob3B0aW9uczogQnVpbGRPcHRpbWl6ZXJPcHRpb25zKTogVHJhbnNmb3JtSmF2YXNjcmlwdE91dHB1dCB7XG5cbiAgY29uc3QgeyBpbnB1dEZpbGVQYXRoIH0gPSBvcHRpb25zO1xuICBsZXQgeyBjb250ZW50IH0gPSBvcHRpb25zO1xuXG4gIGlmICghaW5wdXRGaWxlUGF0aCAmJiBjb250ZW50ID09PSB1bmRlZmluZWQpIHtcbiAgICB0aHJvdyBuZXcgRXJyb3IoJ0VpdGhlciBmaWxlUGF0aCBvciBjb250ZW50IG11c3QgYmUgc3BlY2lmaWVkIGluIG9wdGlvbnMuJyk7XG4gIH1cblxuICBpZiAoY29udGVudCA9PT0gdW5kZWZpbmVkKSB7XG4gICAgY29udGVudCA9IHJlYWRGaWxlU3luYyhpbnB1dEZpbGVQYXRoIGFzIHN0cmluZywgJ1VURi04Jyk7XG4gIH1cblxuICAvLyBEZXRlcm1pbmUgd2hpY2ggdHJhbnNmb3JtcyB0byBhcHBseS5cbiAgY29uc3QgZ2V0VHJhbnNmb3JtcyA9IFtdO1xuXG4gIGlmICh0ZXN0V3JhcEVudW1zKGNvbnRlbnQpKSB7XG4gICAgZ2V0VHJhbnNmb3Jtcy5wdXNoKGdldFdyYXBFbnVtc1RyYW5zZm9ybWVyKTtcbiAgfVxuXG4gIGlmICh0ZXN0SW1wb3J0VHNsaWIoY29udGVudCkpIHtcbiAgICBnZXRUcmFuc2Zvcm1zLnB1c2goZ2V0SW1wb3J0VHNsaWJUcmFuc2Zvcm1lcik7XG4gIH1cblxuICBpZiAodGVzdFByZWZpeENsYXNzZXMoY29udGVudCkpIHtcbiAgICBnZXRUcmFuc2Zvcm1zLnB1c2goZ2V0UHJlZml4Q2xhc3Nlc1RyYW5zZm9ybWVyKTtcbiAgfVxuXG4gIGlmIChpbnB1dEZpbGVQYXRoXG4gICAgJiYgd2hpdGVsaXN0ZWRBbmd1bGFyTW9kdWxlcy5zb21lKChyZSkgPT4gcmUudGVzdChpbnB1dEZpbGVQYXRoKSlcbiAgKSB7XG4gICAgZ2V0VHJhbnNmb3Jtcy5wdXNoKFxuICAgICAgLy8gZ2V0UHJlZml4RnVuY3Rpb25zVHJhbnNmb3JtZXIgaXMgcmF0aGVyIGRhbmdlcm91cywgYXBwbHkgb25seSB0byBrbm93biBwdXJlIG1vZHVsZXMuXG4gICAgICAvLyBJdCB3aWxsIG1hcmsgYm90aCBgcmVxdWlyZSgpYCBjYWxscyBhbmQgYGNvbnNvbGUubG9nKHN0dWZmKWAgYXMgcHVyZS5cbiAgICAgIC8vIFdlIG9ubHkgYXBwbHkgaXQgdG8gd2hpdGVsaXN0ZWQgbW9kdWxlcywgc2luY2Ugd2Uga25vdyB0aGV5IGFyZSBzYWZlLlxuICAgICAgLy8gZ2V0UHJlZml4RnVuY3Rpb25zVHJhbnNmb3JtZXIgbmVlZHMgdG8gYmUgYmVmb3JlIGdldEZvbGRGaWxlVHJhbnNmb3JtZXIuXG4gICAgICBnZXRQcmVmaXhGdW5jdGlvbnNUcmFuc2Zvcm1lcixcbiAgICAgIGdldFNjcnViRmlsZVRyYW5zZm9ybWVyLFxuICAgICAgZ2V0Rm9sZEZpbGVUcmFuc2Zvcm1lcixcbiAgICApO1xuICB9IGVsc2UgaWYgKHRlc3RTY3J1YkZpbGUoY29udGVudCkpIHtcbiAgICBnZXRUcmFuc2Zvcm1zLnB1c2goXG4gICAgICBnZXRTY3J1YkZpbGVUcmFuc2Zvcm1lcixcbiAgICAgIGdldEZvbGRGaWxlVHJhbnNmb3JtZXIsXG4gICAgKTtcbiAgfVxuXG4gIHJldHVybiB0cmFuc2Zvcm1KYXZhc2NyaXB0KHsgLi4ub3B0aW9ucywgZ2V0VHJhbnNmb3JtcywgY29udGVudCB9KTtcbn1cbiJdfQ==