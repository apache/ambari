import { SlugifyPipe } from './slugify.pipe';

describe('SlugifyPipe', () => {
    
    let pipe: SlugifyPipe;
    
    beforeEach(() => {
       pipe = new SlugifyPipe();
    });
    
    it ('Must return string grouped by hyphen and lowercase', () => {
       expect(pipe.transform('Hello World it Work')).toEqual('hello-world-it-work');
    });

    it('Should return the input', () => {
      expect(pipe.transform(2)).toEqual(2);
    });
   
});