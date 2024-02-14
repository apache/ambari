import { RepeatPipe } from './repeat.pipe';
        


describe('RepeatPipe', () => {
    
    let pipe: RepeatPipe;
    
    beforeEach(() => {
       pipe = new RepeatPipe(); 
    });
    
    it('Should do nothing', () => {
       
       expect(pipe.transform('a', 1, '')).toEqual('a');
    });

    it('Should do nothing #2', () => {
       
       expect(pipe.transform('a', 0, '')).toEqual('a');
    });
    
    it('Should repeat two times', () => {
       
       expect(pipe.transform('a', 2, '')).toEqual('aa');
    });
    
    it('Should repeat two times with space', () => {
       
       expect(pipe.transform('a', 2, ' ')).toEqual('a a');
    });


    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, null)).toEqual(1); 
    });
   
});