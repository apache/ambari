import { Pipe, PipeTransform } from '@angular/core';
import { getProperty, isArray, isUndefined } from '../utils/utils';

@Pipe({
  name: 'groupBy'
})
export class GroupByPipe implements PipeTransform {

  transform(input: any, prop: string): Array<any> {

    if (!isArray(input)) {
      return input;
    }

    const arr: { [key: string]: Array<any> } = {};

    for (const value of input) {
      const field: any = getProperty(value, prop);

      if (isUndefined(arr[field])) {
        arr[field] = [];
      }

      arr[field].push(value);
    }

    return Object.keys(arr).map(key => ({ key, 'value': arr[key] }));
  }
}
