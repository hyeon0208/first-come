import { Heading } from '@astryxdesign/core/Heading'
import { Theme } from '@astryxdesign/core/theme'
import { defaultThemeSlug, themes } from '../astryx-themes'

export function App() {
  return (
    <Theme theme={themes[defaultThemeSlug]} mode="light">
      <main>
        <Heading level={1}>선착순 연습장</Heading>
      </main>
    </Theme>
  )
}
