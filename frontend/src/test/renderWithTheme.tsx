import { Theme } from '@astryxdesign/core/theme'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { defaultThemeSlug, themes } from '../astryx-themes'

export function renderWithTheme(ui: ReactElement) {
  return render(<Theme theme={themes[defaultThemeSlug]}>{ui}</Theme>)
}
