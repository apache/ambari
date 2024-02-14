import { Pipe, PipeTransform  } from '@angular/core';
import { isArray, isFunction, getProperty, CollectionPredicate } from '../utils/utils';


@Pipe({
  name: 'firstOrDefault'
})
export class FirstOrDefaultPipe implements PipeTransform {
  
  private static find (
    input: any[], 
    predicate: CollectionPredicate,
    defaultValue?: any
  ): any {
    
    let found = false;
    let result: any;
    let i = -1;
    
    while (!found && ++i < input.length) {
      found = predicate(input[i], i, input);
    }
    
    if (found) {
      result = input[i];
    }
    
    if (typeof result === 'undefined' && typeof defaultValue !== 'undefined') {
      result = defaultValue;
    }
    
    return result;
  }
  
  transform (
    input: any,
    predicate: CollectionPredicate|string[]|any,
    defaultValue?: any
  ): any {
    
    if (!isArray(input)) {
      return input
    }
    
    if (isFunction(predicate)) {
      return FirstOrDefaultPipe.find(input, <CollectionPredicate>predicate, defaultValue);
    }
    else if (isArray(predicate)) {
      const [key, value] = <string[]> predicate;
      return FirstOrDefaultPipe.find(input, (item: any) => getProperty(item, key) === value, defaultValue);
    }
    else if (predicate) {
      return FirstOrDefaultPipe.find(input, (item) => item === <any>predicate, defaultValue);
    }
    else {
      return input;
    }
  }
}