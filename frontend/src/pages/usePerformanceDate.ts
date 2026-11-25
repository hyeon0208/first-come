// 공연 시작 시각은 회차를 만든 날로부터 30일 뒤 18:00이다. 회차를 만들기 전에 화면에 보여 줄 날짜를 같은 규칙으로 구한다.
export const SHOW_START_DAYS = 30
export const SHOW_START_TIME = '18:00'

export function expectedShowDate(today: Date = new Date()): string {
  const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + SHOW_START_DAYS)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
