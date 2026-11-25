import { Badge } from '@astryxdesign/core/Badge'
import { Table } from '@astryxdesign/core/Table'
import type { PerformanceResult, PracticeRecord } from '../../shared/api/types'
import { DIFFICULTY_NAMES, RESULT_NAMES } from '../../shared/messages'
import { formatDateTime, formatElapsed } from '../../shared/time/format'

// 결과는 색과 글자로 함께 보여 준다: 성공(성공 색), 매진·시간 초과(오류 색), 중단(회색).
const RESULT_VARIANT: Record<PerformanceResult, 'success' | 'error' | 'neutral'> = {
  SUCCESS: 'success',
  SOLD_OUT: 'error',
  TIMEOUT: 'error',
  ABANDONED: 'neutral',
}

export function RecordTable({ records }: { records: PracticeRecord[] }) {
  return (
    <Table
      data={records}
      idKey="id"
      columns={[
        { key: 'endedAt', header: '끝난 시각', renderCell: (record) => formatDateTime(record.endedAt) },
        { key: 'showTitle', header: '공연', renderCell: (record) => record.showTitle },
        { key: 'difficulty', header: '난이도', renderCell: (record) => DIFFICULTY_NAMES[record.difficulty] },
        { key: 'result', header: '결과', renderCell: (record) => <Badge label={RESULT_NAMES[record.result]} variant={RESULT_VARIANT[record.result]} /> },
        { key: 'elapsedSeconds', header: '걸린 시간', renderCell: (record) => formatElapsed(record.elapsedSeconds) },
      ]}
    />
  )
}
