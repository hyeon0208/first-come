import { Layout, LayoutContent } from '@astryxdesign/core/Layout'
import { Skeleton } from '@astryxdesign/core/Skeleton'
import { VStack } from '@astryxdesign/core/VStack'
import type { ReactNode } from 'react'

// 예매 정보 입력과 결제 화면의 틀: 가운데 한 칸.
export function BookingColumn({ children }: { children: ReactNode }) {
  return (
    <Layout height="auto" contentWidth={840}>
      <LayoutContent padding={5}>
        <VStack gap={5}>{children}</VStack>
      </LayoutContent>
    </Layout>
  )
}

export function ColumnSkeleton() {
  return (
    <VStack gap={3}>
      {[120, 80, 160].map((height, index) => (
        <Skeleton key={height} height={height} index={index} />
      ))}
    </VStack>
  )
}
