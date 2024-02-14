import { DefaultsPipe } from './defaults.pipe';
        
describe('DefaultsPipe', () => {
    
    let pipe: DefaultsPipe;
    
    beforeEach(() => {
       pipe = new DefaultsPipe(); 
    });
    
    const defaults = {
        a: 1,
        b: 2,
        c: 3
    };
    
    it('Should return { a: 1, b: 2, c: 4 }', () => {
      expect(pipe.transform({ c: 4 }, defaults)).toEqual({ a: 1, b: 2, c: 4 });
    });

    it('Should return the defaults', () => {
      expect(pipe.transform(null, defaults)).toEqual(defaults);
      expect(pipe.transform(undefined, defaults)).toEqual(defaults);
    });

    it('Should return an array with defaults', () => {
      const value: any[] = [{ a: 2 }, undefined, { b: 1 }, null, { c: 4 }, 'a'];
      expect(pipe.transform(value, defaults)).toEqual([
        { a: 2, b: 2, c: 3 },
        defaults,
        { a: 1, b: 1, c: 3 },
        defaults,
        { a: 1, b: 2, c: 4 },
        'a'
      ]);
    });

    it('Should return the input', () => {
      expect(pipe.transform(true, defaults)).toEqual(true);
      expect(pipe.transform('a', defaults)).toEqual('a');
      expect(pipe.transform({ a:1 }, 'a')).toEqual({ a: 1 });
    });

});
