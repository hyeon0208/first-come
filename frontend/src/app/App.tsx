import { InternationalizationProvider } from '@astryxdesign/core/i18n'
import koKR from '@astryxdesign/core/locales/ko-KR.generated.js'
import { Theme } from '@astryxdesign/core/theme'
import { useState } from 'react'
import { RouterProvider } from 'react-router'
import { SessionProvider } from '../features/session'
import { createAppRouter } from './AppRouter'
import { appTheme } from './theme'

export function App() {
  const [router] = useState(createAppRouter)
  return (
    <InternationalizationProvider locale="ko-KR" messages={{ 'ko-KR': koKR }}>
      <Theme theme={appTheme} mode="light">
        <SessionProvider>
          <RouterProvider router={router} />
        </SessionProvider>
      </Theme>
    </InternationalizationProvider>
  )
}
