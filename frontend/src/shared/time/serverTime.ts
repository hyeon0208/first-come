// 서버가 주는 시각(시간대 표기 없는 문자열)을 화면의 시계로 옮긴다.
// 서버 시각과 화면 시계가 어긋나 있어도 남은 시간이 맞도록, 응답에 함께 온 서버의 "지금"과의 차이만 쓴다.

function parseServerTime(value: string): number {
  // 두 값의 차이만 쓰므로 어떤 시간대로 읽어도 같다. 같은 방식으로 읽기만 하면 된다.
  return Date.parse(value.length > 23 ? `${value.slice(0, 23)}Z` : `${value}Z`)
}

// 서버 시각 serverTime이 화면 시계로 언제인지(밀리초). receivedAt은 응답을 받은 화면 시각이다.
export function toClientTime(serverTime: string, serverNow: string, receivedAt: number): number {
  return receivedAt + (parseServerTime(serverTime) - parseServerTime(serverNow))
}

export function toClientTimeOrNull(serverTime: string | null, serverNow: string, receivedAt: number): number | null {
  return serverTime === null ? null : toClientTime(serverTime, serverNow, receivedAt)
}

// 서버 시각 문자열에서 날짜와 시각 글자만 꺼낸다(화면 표시용).
export function splitServerTime(value: string): { year: number; month: number; day: number; hour: number; minute: number } {
  const [date, time] = value.split('T')
  const [year, month, day] = date.split('-').map(Number)
  const [hour, minute] = time.split(':').map(Number)
  return { year, month, day, hour, minute }
}
