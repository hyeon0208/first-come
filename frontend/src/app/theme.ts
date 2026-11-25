import { defineTheme } from '@astryxdesign/core/theme'
import { defaultThemeSlug, themes } from '../astryx-themes'

// 기본 테마(neutral)에 강조색만 바꾼다. 밝은 화면만 쓰므로 밝은 쪽과 어두운 쪽에 같은 값을 준다.
const ACCENT = '#0B6E4F'
const ON_ACCENT = '#FFFFFF'
const ACCENT_MUTED = '#0B6E4F1F'

export const appTheme = defineTheme({
  name: 'first-come',
  extends: themes[defaultThemeSlug],
  tokens: {
    '--color-accent': [ACCENT, ACCENT],
    '--color-on-accent': [ON_ACCENT, ON_ACCENT],
    '--color-text-accent': [ACCENT, ACCENT],
    '--color-accent-muted': [ACCENT_MUTED, ACCENT_MUTED],
  },
})
