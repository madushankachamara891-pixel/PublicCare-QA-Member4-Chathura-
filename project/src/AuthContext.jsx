import { useCallback, useEffect, useState } from 'react'
import { getCurrentUser } from './authApi.js'
import { AuthContext } from './authContext.js'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setUser(await getCurrentUser())
      setError('')
    } catch (requestError) {
      setUser(null)
      setError(requestError.status === 401 ? '' : requestError.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let mounted = true
    queueMicrotask(() => { if (mounted) refresh() })
    return () => { mounted = false }
  }, [refresh])

  return <AuthContext.Provider value={{ user, setUser, loading, error, refresh }}>
    {children}
  </AuthContext.Provider>
}
