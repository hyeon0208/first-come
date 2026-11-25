import { useEffect, useState } from 'react'

// 화면 시계의 지금(밀리초). 남은 시간을 그리려고 일정 간격으로 다시 그린다.
export function useNow(intervalMs = 250): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs)
    return () => window.clearInterval(timer)
  }, [intervalMs])
  return now
}
