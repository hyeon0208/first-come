import { SEAT_TOKENS } from './designTokens'

// 공연 이름 글자와 도형으로 그린 가짜 포스터. 사진을 쓰지 않는다.
const PALETTES = [
  { background: 'var(--color-accent)', shape: 'var(--seat-grade-r)' },
  { background: 'var(--seat-grade-s)', shape: 'var(--seat-map-floor)' },
  { background: 'var(--seat-grade-r)', shape: 'var(--color-accent)' },
]

export function FakePoster({ showId, title }: { showId: number; title: string }) {
  const palette = PALETTES[showId % PALETTES.length]
  const lines = splitTitle(title)
  return (
    <svg viewBox="0 0 300 400" role="img" aria-label={`${title} 포스터`} width="100%" style={SEAT_TOKENS}>
      <rect width="300" height="400" fill={palette.background} />
      <circle cx={70 + (showId % 3) * 60} cy="130" r="74" fill={palette.shape} opacity="0.85" />
      <rect x="150" y={170 + (showId % 2) * 30} width="110" height="110" fill="var(--seat-map-floor)" opacity="0.25" />
      {lines.map((line, index) => (
        <text key={line} x="24" y={310 + index * 30} fill="var(--color-on-accent)" fontSize="22" fontWeight="700">
          {line}
        </text>
      ))}
    </svg>
  )
}

// 한 줄에 10자쯤 들어간다. 띄어쓰기에서 나눈다.
function splitTitle(title: string): string[] {
  const lines: string[] = []
  let line = ''
  for (const word of title.split(' ')) {
    if (line && (line + ' ' + word).length > 10) {
      lines.push(line)
      line = word
    } else {
      line = line ? `${line} ${word}` : word
    }
  }
  if (line) lines.push(line)
  return lines.slice(0, 3)
}
