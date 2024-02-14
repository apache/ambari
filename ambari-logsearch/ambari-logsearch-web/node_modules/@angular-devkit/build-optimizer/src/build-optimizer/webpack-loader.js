"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const source_map_1 = require("source-map");
const loaderUtils = require('loader-utils');
const build_optimizer_1 = require("./build-optimizer");
function buildOptimizerLoader(content, previousSourceMap) {
    this.cacheable();
    const options = loaderUtils.getOptions(this) || {};
    const boOutput = build_optimizer_1.buildOptimizer({
        content,
        inputFilePath: this.resourcePath,
        // Add a name to the build optimizer output.
        // Without a name the sourcemaps cannot be properly chained.
        outputFilePath: this.resourcePath + '.build-optimizer.js',
        emitSourceMap: options.sourceMap,
    });
    if (boOutput.emitSkipped || boOutput.content === null) {
        // Webpack typings for previousSourceMap are wrong, they are JSON objects and not strings.
        // tslint:disable-next-line:no-any
        this.callback(null, content, previousSourceMap);
        return;
    }
    const intermediateSourceMap = boOutput.sourceMap;
    let newContent = boOutput.content;
    let newSourceMap;
    if (options.sourceMap && intermediateSourceMap) {
        // Webpack doesn't need sourceMappingURL since we pass them on explicitely.
        newContent = newContent.replace(/^\/\/# sourceMappingURL=[^\r\n]*/gm, '');
        if (previousSourceMap) {
            // If there's a previous sourcemap, we have to chain them.
            // See https://github.com/mozilla/source-map/issues/216#issuecomment-150839869 for a simple
            // source map chaining example.
            // Use http://sokra.github.io/source-map-visualization/ to validate sourcemaps make sense.
            // Fill in the intermediate sourcemap source as the previous sourcemap file.
            intermediateSourceMap.sources = [previousSourceMap.file];
            // Chain the sourcemaps.
            const consumer = new source_map_1.SourceMapConsumer(intermediateSourceMap);
            const generator = source_map_1.SourceMapGenerator.fromSourceMap(consumer);
            generator.applySourceMap(new source_map_1.SourceMapConsumer(previousSourceMap));
            newSourceMap = generator.toJSON();
        }
        else {
            // Otherwise just return our generated sourcemap.
            newSourceMap = intermediateSourceMap;
        }
    }
    // Webpack typings for previousSourceMap are wrong, they are JSON objects and not strings.
    // tslint:disable-next-line:no-any
    this.callback(null, newContent, newSourceMap);
}
exports.default = buildOptimizerLoader;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoid2VicGFjay1sb2FkZXIuanMiLCJzb3VyY2VSb290IjoiL1VzZXJzL2hhbnNsL1NvdXJjZXMvZGV2a2l0LyIsInNvdXJjZXMiOlsicGFja2FnZXMvYW5ndWxhcl9kZXZraXQvYnVpbGRfb3B0aW1pemVyL3NyYy9idWlsZC1vcHRpbWl6ZXIvd2VicGFjay1sb2FkZXIudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCwyQ0FBaUY7QUFFakYsTUFBTSxXQUFXLEdBQUcsT0FBTyxDQUFDLGNBQWMsQ0FBQyxDQUFDO0FBRTVDLHVEQUFtRDtBQU9uRCw4QkFDdUMsT0FBZSxFQUFFLGlCQUErQjtJQUNyRixJQUFJLENBQUMsU0FBUyxFQUFFLENBQUM7SUFDakIsTUFBTSxPQUFPLEdBQWdDLFdBQVcsQ0FBQyxVQUFVLENBQUMsSUFBSSxDQUFDLElBQUksRUFBRSxDQUFDO0lBRWhGLE1BQU0sUUFBUSxHQUFHLGdDQUFjLENBQUM7UUFDOUIsT0FBTztRQUNQLGFBQWEsRUFBRSxJQUFJLENBQUMsWUFBWTtRQUNoQyw0Q0FBNEM7UUFDNUMsNERBQTREO1FBQzVELGNBQWMsRUFBRSxJQUFJLENBQUMsWUFBWSxHQUFHLHFCQUFxQjtRQUN6RCxhQUFhLEVBQUUsT0FBTyxDQUFDLFNBQVM7S0FDakMsQ0FBQyxDQUFDO0lBRUgsRUFBRSxDQUFDLENBQUMsUUFBUSxDQUFDLFdBQVcsSUFBSSxRQUFRLENBQUMsT0FBTyxLQUFLLElBQUksQ0FBQyxDQUFDLENBQUM7UUFDdEQsMEZBQTBGO1FBQzFGLGtDQUFrQztRQUNsQyxJQUFJLENBQUMsUUFBUSxDQUFDLElBQUksRUFBRSxPQUFPLEVBQUUsaUJBQXdCLENBQUMsQ0FBQztRQUV2RCxNQUFNLENBQUM7SUFDVCxDQUFDO0lBRUQsTUFBTSxxQkFBcUIsR0FBRyxRQUFRLENBQUMsU0FBUyxDQUFDO0lBQ2pELElBQUksVUFBVSxHQUFHLFFBQVEsQ0FBQyxPQUFPLENBQUM7SUFFbEMsSUFBSSxZQUFZLENBQUM7SUFFakIsRUFBRSxDQUFDLENBQUMsT0FBTyxDQUFDLFNBQVMsSUFBSSxxQkFBcUIsQ0FBQyxDQUFDLENBQUM7UUFDL0MsMkVBQTJFO1FBQzNFLFVBQVUsR0FBRyxVQUFVLENBQUMsT0FBTyxDQUFDLG9DQUFvQyxFQUFFLEVBQUUsQ0FBQyxDQUFDO1FBRTFFLEVBQUUsQ0FBQyxDQUFDLGlCQUFpQixDQUFDLENBQUMsQ0FBQztZQUN0QiwwREFBMEQ7WUFDMUQsMkZBQTJGO1lBQzNGLCtCQUErQjtZQUMvQiwwRkFBMEY7WUFFMUYsNEVBQTRFO1lBQzVFLHFCQUFxQixDQUFDLE9BQU8sR0FBRyxDQUFDLGlCQUFpQixDQUFDLElBQUksQ0FBQyxDQUFDO1lBRXpELHdCQUF3QjtZQUN4QixNQUFNLFFBQVEsR0FBRyxJQUFJLDhCQUFpQixDQUFDLHFCQUFxQixDQUFDLENBQUM7WUFDOUQsTUFBTSxTQUFTLEdBQUcsK0JBQWtCLENBQUMsYUFBYSxDQUFDLFFBQVEsQ0FBQyxDQUFDO1lBQzdELFNBQVMsQ0FBQyxjQUFjLENBQUMsSUFBSSw4QkFBaUIsQ0FBQyxpQkFBaUIsQ0FBQyxDQUFDLENBQUM7WUFDbkUsWUFBWSxHQUFHLFNBQVMsQ0FBQyxNQUFNLEVBQUUsQ0FBQztRQUNwQyxDQUFDO1FBQUMsSUFBSSxDQUFDLENBQUM7WUFDTixpREFBaUQ7WUFDakQsWUFBWSxHQUFHLHFCQUFxQixDQUFDO1FBQ3ZDLENBQUM7SUFDSCxDQUFDO0lBRUQsMEZBQTBGO0lBQzFGLGtDQUFrQztJQUNsQyxJQUFJLENBQUMsUUFBUSxDQUFDLElBQUksRUFBRSxVQUFVLEVBQUUsWUFBbUIsQ0FBQyxDQUFDO0FBQ3ZELENBQUM7QUF0REQsdUNBc0RDIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuaW1wb3J0IHsgUmF3U291cmNlTWFwLCBTb3VyY2VNYXBDb25zdW1lciwgU291cmNlTWFwR2VuZXJhdG9yIH0gZnJvbSAnc291cmNlLW1hcCc7XG5pbXBvcnQgKiBhcyB3ZWJwYWNrIGZyb20gJ3dlYnBhY2snO1xuY29uc3QgbG9hZGVyVXRpbHMgPSByZXF1aXJlKCdsb2FkZXItdXRpbHMnKTtcblxuaW1wb3J0IHsgYnVpbGRPcHRpbWl6ZXIgfSBmcm9tICcuL2J1aWxkLW9wdGltaXplcic7XG5cblxuaW50ZXJmYWNlIEJ1aWxkT3B0aW1pemVyTG9hZGVyT3B0aW9ucyB7XG4gIHNvdXJjZU1hcDogYm9vbGVhbjtcbn1cblxuZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gYnVpbGRPcHRpbWl6ZXJMb2FkZXJcbiAgKHRoaXM6IHdlYnBhY2subG9hZGVyLkxvYWRlckNvbnRleHQsIGNvbnRlbnQ6IHN0cmluZywgcHJldmlvdXNTb3VyY2VNYXA6IFJhd1NvdXJjZU1hcCkge1xuICB0aGlzLmNhY2hlYWJsZSgpO1xuICBjb25zdCBvcHRpb25zOiBCdWlsZE9wdGltaXplckxvYWRlck9wdGlvbnMgPSBsb2FkZXJVdGlscy5nZXRPcHRpb25zKHRoaXMpIHx8IHt9O1xuXG4gIGNvbnN0IGJvT3V0cHV0ID0gYnVpbGRPcHRpbWl6ZXIoe1xuICAgIGNvbnRlbnQsXG4gICAgaW5wdXRGaWxlUGF0aDogdGhpcy5yZXNvdXJjZVBhdGgsXG4gICAgLy8gQWRkIGEgbmFtZSB0byB0aGUgYnVpbGQgb3B0aW1pemVyIG91dHB1dC5cbiAgICAvLyBXaXRob3V0IGEgbmFtZSB0aGUgc291cmNlbWFwcyBjYW5ub3QgYmUgcHJvcGVybHkgY2hhaW5lZC5cbiAgICBvdXRwdXRGaWxlUGF0aDogdGhpcy5yZXNvdXJjZVBhdGggKyAnLmJ1aWxkLW9wdGltaXplci5qcycsXG4gICAgZW1pdFNvdXJjZU1hcDogb3B0aW9ucy5zb3VyY2VNYXAsXG4gIH0pO1xuXG4gIGlmIChib091dHB1dC5lbWl0U2tpcHBlZCB8fCBib091dHB1dC5jb250ZW50ID09PSBudWxsKSB7XG4gICAgLy8gV2VicGFjayB0eXBpbmdzIGZvciBwcmV2aW91c1NvdXJjZU1hcCBhcmUgd3JvbmcsIHRoZXkgYXJlIEpTT04gb2JqZWN0cyBhbmQgbm90IHN0cmluZ3MuXG4gICAgLy8gdHNsaW50OmRpc2FibGUtbmV4dC1saW5lOm5vLWFueVxuICAgIHRoaXMuY2FsbGJhY2sobnVsbCwgY29udGVudCwgcHJldmlvdXNTb3VyY2VNYXAgYXMgYW55KTtcblxuICAgIHJldHVybjtcbiAgfVxuXG4gIGNvbnN0IGludGVybWVkaWF0ZVNvdXJjZU1hcCA9IGJvT3V0cHV0LnNvdXJjZU1hcDtcbiAgbGV0IG5ld0NvbnRlbnQgPSBib091dHB1dC5jb250ZW50O1xuXG4gIGxldCBuZXdTb3VyY2VNYXA7XG5cbiAgaWYgKG9wdGlvbnMuc291cmNlTWFwICYmIGludGVybWVkaWF0ZVNvdXJjZU1hcCkge1xuICAgIC8vIFdlYnBhY2sgZG9lc24ndCBuZWVkIHNvdXJjZU1hcHBpbmdVUkwgc2luY2Ugd2UgcGFzcyB0aGVtIG9uIGV4cGxpY2l0ZWx5LlxuICAgIG5ld0NvbnRlbnQgPSBuZXdDb250ZW50LnJlcGxhY2UoL15cXC9cXC8jIHNvdXJjZU1hcHBpbmdVUkw9W15cXHJcXG5dKi9nbSwgJycpO1xuXG4gICAgaWYgKHByZXZpb3VzU291cmNlTWFwKSB7XG4gICAgICAvLyBJZiB0aGVyZSdzIGEgcHJldmlvdXMgc291cmNlbWFwLCB3ZSBoYXZlIHRvIGNoYWluIHRoZW0uXG4gICAgICAvLyBTZWUgaHR0cHM6Ly9naXRodWIuY29tL21vemlsbGEvc291cmNlLW1hcC9pc3N1ZXMvMjE2I2lzc3VlY29tbWVudC0xNTA4Mzk4NjkgZm9yIGEgc2ltcGxlXG4gICAgICAvLyBzb3VyY2UgbWFwIGNoYWluaW5nIGV4YW1wbGUuXG4gICAgICAvLyBVc2UgaHR0cDovL3Nva3JhLmdpdGh1Yi5pby9zb3VyY2UtbWFwLXZpc3VhbGl6YXRpb24vIHRvIHZhbGlkYXRlIHNvdXJjZW1hcHMgbWFrZSBzZW5zZS5cblxuICAgICAgLy8gRmlsbCBpbiB0aGUgaW50ZXJtZWRpYXRlIHNvdXJjZW1hcCBzb3VyY2UgYXMgdGhlIHByZXZpb3VzIHNvdXJjZW1hcCBmaWxlLlxuICAgICAgaW50ZXJtZWRpYXRlU291cmNlTWFwLnNvdXJjZXMgPSBbcHJldmlvdXNTb3VyY2VNYXAuZmlsZV07XG5cbiAgICAgIC8vIENoYWluIHRoZSBzb3VyY2VtYXBzLlxuICAgICAgY29uc3QgY29uc3VtZXIgPSBuZXcgU291cmNlTWFwQ29uc3VtZXIoaW50ZXJtZWRpYXRlU291cmNlTWFwKTtcbiAgICAgIGNvbnN0IGdlbmVyYXRvciA9IFNvdXJjZU1hcEdlbmVyYXRvci5mcm9tU291cmNlTWFwKGNvbnN1bWVyKTtcbiAgICAgIGdlbmVyYXRvci5hcHBseVNvdXJjZU1hcChuZXcgU291cmNlTWFwQ29uc3VtZXIocHJldmlvdXNTb3VyY2VNYXApKTtcbiAgICAgIG5ld1NvdXJjZU1hcCA9IGdlbmVyYXRvci50b0pTT04oKTtcbiAgICB9IGVsc2Uge1xuICAgICAgLy8gT3RoZXJ3aXNlIGp1c3QgcmV0dXJuIG91ciBnZW5lcmF0ZWQgc291cmNlbWFwLlxuICAgICAgbmV3U291cmNlTWFwID0gaW50ZXJtZWRpYXRlU291cmNlTWFwO1xuICAgIH1cbiAgfVxuXG4gIC8vIFdlYnBhY2sgdHlwaW5ncyBmb3IgcHJldmlvdXNTb3VyY2VNYXAgYXJlIHdyb25nLCB0aGV5IGFyZSBKU09OIG9iamVjdHMgYW5kIG5vdCBzdHJpbmdzLlxuICAvLyB0c2xpbnQ6ZGlzYWJsZS1uZXh0LWxpbmU6bm8tYW55XG4gIHRoaXMuY2FsbGJhY2sobnVsbCwgbmV3Q29udGVudCwgbmV3U291cmNlTWFwIGFzIGFueSk7XG59XG4iXX0=