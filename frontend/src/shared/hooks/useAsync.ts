import { useCallback, useEffect, useState } from 'react'

export type AsyncState<T> = { status: 'loading' } | { status: 'error'; error: unknown } | { status: 'ready'; data: T }

// 화면에 들어올 때 한 번 불러오고, reload를 부르면 다시 불러온다. 자동으로 다시 받지 않는다.
// deps(숫자나 글자)가 바뀌면 다시 불러온다.
export function useAsync<T>(load: () => Promise<T>, deps: (string | number)[]): AsyncState<T> & { reload: () => void } {
  const [version, setVersion] = useState(0)
  const key = [...deps, version].join('|')
  // 어느 요청의 결과인지 함께 둔다. 지금 요청의 결과가 아니면 불러오는 중이다.
  const [result, setResult] = useState<{ key: string; state: AsyncState<T> } | null>(null)

  useEffect(() => {
    let cancelled = false
    load().then(
      (data) => {
        if (!cancelled) setResult({ key, state: { status: 'ready', data } })
      },
      (error) => {
        if (!cancelled) setResult({ key, state: { status: 'error', error } })
      },
    )
    return () => {
      cancelled = true
    }
    // load는 호출할 때마다 새로 만들어지므로 key로 다시 불러올 때를 정한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  const reload = useCallback(() => setVersion((value) => value + 1), [])
  const state: AsyncState<T> = result?.key === key ? result.state : { status: 'loading' }
  return { ...state, reload }
}
