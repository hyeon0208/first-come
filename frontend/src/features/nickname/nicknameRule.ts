// 닉네임은 앞뒤 공백을 빼고 2~12자다. 서버도 같은 규칙으로 다시 확인한다.
export function isValidNickname(value: string): boolean {
  const length = value.trim().length
  return length >= 2 && length <= 12
}
