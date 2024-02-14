import { Pipe, PipeTransform  } from '@angular/core';
import { every, CollectionPredicate } from '../utils/utils';

@Pipe({
  name: 'every'
})
export class EveryPipe implements PipeTransform {
  
  transform (input: any, predicate: CollectionPredicate): any {
    
    return every(input, predicate);
  }
}