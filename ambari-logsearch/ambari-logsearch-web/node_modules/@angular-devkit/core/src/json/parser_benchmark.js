"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const benchmark_1 = require("@_/benchmark");
const parser_1 = require("./parser");
const testCase = {
    'hello': [0, 1, 'world', 2],
    'world': {
        'great': 123E-12,
    },
};
const testCaseJson = JSON.stringify(testCase);
describe('parserJson', () => {
    benchmark_1.benchmark('parseJsonAst', () => parser_1.parseJsonAst(testCaseJson), () => JSON.parse(testCaseJson));
    benchmark_1.benchmark('parseJson', () => parser_1.parseJson(testCaseJson));
});
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoicGFyc2VyX2JlbmNobWFyay5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9hbmd1bGFyX2RldmtpdC9jb3JlL3NyYy9qc29uL3BhcnNlcl9iZW5jaG1hcmsudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCw0Q0FBeUM7QUFDekMscUNBQW1EO0FBR25ELE1BQU0sUUFBUSxHQUFHO0lBQ2YsT0FBTyxFQUFFLENBQUMsQ0FBQyxFQUFFLENBQUMsRUFBRSxPQUFPLEVBQUUsQ0FBQyxDQUFDO0lBQzNCLE9BQU8sRUFBRTtRQUNQLE9BQU8sRUFBRSxPQUFPO0tBQ2pCO0NBQ0YsQ0FBQztBQUNGLE1BQU0sWUFBWSxHQUFHLElBQUksQ0FBQyxTQUFTLENBQUMsUUFBUSxDQUFDLENBQUM7QUFHOUMsUUFBUSxDQUFDLFlBQVksRUFBRTtJQUNyQixxQkFBUyxDQUFDLGNBQWMsRUFBRSxNQUFNLHFCQUFZLENBQUMsWUFBWSxDQUFDLEVBQUUsTUFBTSxJQUFJLENBQUMsS0FBSyxDQUFDLFlBQVksQ0FBQyxDQUFDLENBQUM7SUFDNUYscUJBQVMsQ0FBQyxXQUFXLEVBQUUsTUFBTSxrQkFBUyxDQUFDLFlBQVksQ0FBQyxDQUFDLENBQUM7QUFDeEQsQ0FBQyxDQUFDLENBQUMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBsaWNlbnNlXG4gKiBDb3B5cmlnaHQgR29vZ2xlIEluYy4gQWxsIFJpZ2h0cyBSZXNlcnZlZC5cbiAqXG4gKiBVc2Ugb2YgdGhpcyBzb3VyY2UgY29kZSBpcyBnb3Zlcm5lZCBieSBhbiBNSVQtc3R5bGUgbGljZW5zZSB0aGF0IGNhbiBiZVxuICogZm91bmQgaW4gdGhlIExJQ0VOU0UgZmlsZSBhdCBodHRwczovL2FuZ3VsYXIuaW8vbGljZW5zZVxuICovXG5pbXBvcnQgeyBiZW5jaG1hcmsgfSBmcm9tICdAXy9iZW5jaG1hcmsnO1xuaW1wb3J0IHsgcGFyc2VKc29uLCBwYXJzZUpzb25Bc3QgfSBmcm9tICcuL3BhcnNlcic7XG5cblxuY29uc3QgdGVzdENhc2UgPSB7XG4gICdoZWxsbyc6IFswLCAxLCAnd29ybGQnLCAyXSxcbiAgJ3dvcmxkJzoge1xuICAgICdncmVhdCc6IDEyM0UtMTIsXG4gIH0sXG59O1xuY29uc3QgdGVzdENhc2VKc29uID0gSlNPTi5zdHJpbmdpZnkodGVzdENhc2UpO1xuXG5cbmRlc2NyaWJlKCdwYXJzZXJKc29uJywgKCkgPT4ge1xuICBiZW5jaG1hcmsoJ3BhcnNlSnNvbkFzdCcsICgpID0+IHBhcnNlSnNvbkFzdCh0ZXN0Q2FzZUpzb24pLCAoKSA9PiBKU09OLnBhcnNlKHRlc3RDYXNlSnNvbikpO1xuICBiZW5jaG1hcmsoJ3BhcnNlSnNvbicsICgpID0+IHBhcnNlSnNvbih0ZXN0Q2FzZUpzb24pKTtcbn0pO1xuIl19