import { ClickableCard } from '@astryxdesign/core/ClickableCard'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { Grid } from '@astryxdesign/core/Grid'
import { Heading } from '@astryxdesign/core/Heading'
import { Skeleton } from '@astryxdesign/core/Skeleton'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { api } from '../../shared/api/endpoints'
import { useAsync } from '../../shared/hooks/useAsync'
import { MESSAGES } from '../../shared/messages'
import { priceRange } from './priceRange'
import { FakePoster } from '../../shared/ui/FakePoster'
import { LoadFailed } from '../../shared/ui/LoadFailed'

export function ShowList({ showDate, onSelect }: { showDate: string; onSelect: (showId: number) => void }) {
  const shows = useAsync(api.getShows, [])

  if (shows.status === 'loading') {
    return (
      <Grid columns={{ minWidth: 240, max: 3 }} gap={4}>
        {[0, 1, 2].map((index) => (
          <Skeleton key={index} height={420} index={index} />
        ))}
      </Grid>
    )
  }
  if (shows.status === 'error') return <LoadFailed message={MESSAGES.shows.failed} onRetry={shows.reload} />
  if (shows.data.length === 0) return <EmptyState title={MESSAGES.shows.empty} />

  return (
    <Grid columns={{ minWidth: 240, max: 3 }} gap={4}>
      {shows.data.map((show) => (
        <ClickableCard key={show.id} label={show.title} onClick={() => onSelect(show.id)} padding={3}>
          <VStack gap={2}>
            <FakePoster showId={show.id} title={show.title} />
            <Heading level={3}>{show.title}</Heading>
            <Text color="secondary">{show.venueName}</Text>
            <Text color="secondary">{showDate}</Text>
            <Text weight="semibold">{priceRange(show)}</Text>
          </VStack>
        </ClickableCard>
      ))}
    </Grid>
  )
}
