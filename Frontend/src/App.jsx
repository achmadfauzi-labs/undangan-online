import { useEffect, useState } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083'

function App() {
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch(API_BASE + '/actuator/health')
      .then((res) => {
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`)
        }
        return res.json()
      })
      .then((data) => {
        setStatus(data.status || 'UNKNOWN')
        setError(null)
      })
      .catch((err) => {
        setStatus('DOWN')
        setError(err.message)
      })
  }, [])

  let statusText = ''
  let statusColor = ''

  if (status === 'loading') {
    statusText = 'Loading...'
    statusColor = '#666666'
  } else if (status === 'UP') {
    statusText = 'Status: UP'
    statusColor = '#28a745'
  } else {
    statusText = error
      ? `Status: DOWN — ${error}`
      : 'Status: DOWN'
    statusColor = '#dc3545'
  }

  return (
    <div className="app">
      <h1>Undangan Online — Dev (Fase 0)</h1>
      <p className="status" style={{ color: statusColor }}>
        {statusText}
      </p>
    </div>
  )
}

export default App
