import './App.css'

function App() {
  function switchToClassic(){
    window.location.href=window.location.href.replace("latest","classic")
  }

  return (
    <>
      <button className='btn' onClick={switchToClassic}>
        Switch to classic UI
      </button>
    </>
  )
}

export default App
