// 서버 호출의 한 곳. 오류 응답({code, message})은 ApiError로 바꿔 던진다.

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message)
  }
}

// 응답을 받지 못했다(네트워크 오류 등). 서버가 처리했는지 알 수 없다.
export class NetworkError extends Error {}

// 예매 화면의 모든 요청에 붙는 회차 번호 헤더
const PRACTICE_RUN_HEADER = 'X-Practice-Run'

interface RequestOptions {
  method?: 'GET' | 'POST' | 'DELETE'
  body?: unknown
  runId?: number
  headers?: Record<string, string>
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { ...options.headers }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (options.runId !== undefined) headers[PRACTICE_RUN_HEADER] = String(options.runId)

  let response: Response
  try {
    response = await fetch(`/api/v1${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      credentials: 'same-origin',
    })
  } catch {
    throw new NetworkError('서버에 닿지 못했습니다.')
  }

  if (response.status === 204) return undefined as T
  const text = await response.text()
  const data = text ? safeParse(text) : undefined
  if (!response.ok) {
    const error = data as { code?: string; message?: string } | undefined
    throw new ApiError(response.status, error?.code ?? 'UNKNOWN', error?.message ?? '')
  }
  return data as T
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

export function isApiError(error: unknown, ...codes: string[]): error is ApiError {
  return error instanceof ApiError && (codes.length === 0 || codes.includes(error.code))
}
