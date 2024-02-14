import { TakeUntilPipe } from './take-until.pipe';


describe('TakeUntilPipe', () => {
    
    let pipe: TakeUntilPipe;
    
    beforeEach(() => {
       pipe = new TakeUntilPipe(); 
    });
    
    it ('Should return until the condition is met', () => {
      const predicate = (item) => item === 1;

      expect(pipe.transform([2, 3, 4, 5, 1, 7, 8], predicate)).toEqual([2, 3, 4, 5]);
    });

      it ('Should return until the condition is met #2', () => {
      const predicate = (item) => item === 1;

      expect(pipe.transform([2, 3, 4, 5, 7, 8], predicate)).toEqual([2, 3, 4, 5, 7, 8]);
    });


    it('Should return the objects until a is 5', () => {
       
       const predicate = (item: any) => item.a === 4;

       const array = [{a:2}, {a:3}, {a:5}, {a:4}, {a:1}];
       
       expect(pipe.transform(array, predicate)).toEqual([{a:2}, {a:3}, {a:5}]);
    });
    
    it('Should return the input', () => {
        expect(pipe.transform([1])).toEqual([1]);
    });
    
    it('Should return the value unchanged', () => {
       expect(pipe.transform('a', null)).toEqual('a'); 
    });
});