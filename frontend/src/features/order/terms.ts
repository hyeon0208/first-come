export const REQUIRED_TERMS = ['cancelPolicy', 'usagePolicy'] as const

// 필수 약관에 모두 동의해야 다음으로 간다.
export function hasAgreedAll(agreed: string[]): boolean {
  return REQUIRED_TERMS.every((term) => agreed.includes(term))
}
