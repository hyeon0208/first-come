// 화면에 보이는 안내 문구. 예매 규칙 표의 "안내 문구"와 화면별 상태 표에 있는 문장만 둔다.

export const MESSAGES = {
  appName: '선착순 연습장',
  intro: '실제 예매와 같은 순서로 선착순 예매를 연습합니다.',

  nickname: {
    invalid: '닉네임은 2~12자로 입력해 주세요.',
    failed: '시작하지 못했어요. 잠시 후 다시 시도해 주세요.',
  },
  shows: {
    empty: '지금 연습할 수 있는 공연이 없어요.',
    failed: '공연을 불러오지 못했어요.',
  },
  inProgress: '이미 진행 중인 예매가 있습니다.',
  practiceFull: '지금은 연습 자리가 모두 찼어요. 잠시 후 다시 시도해 주세요.',
  notOpenYet: '판매 시작 시간 이전입니다.',
  noSeatCount: '잔여석이 안내되지 않는 상품이에요.',
  waiting: {
    notice: '잠시만 기다려 주세요. 순서가 되면 예매 화면으로 자동으로 이동합니다.',
    failed: '현재 접속이 원활하지 않습니다.',
  },

  timer: {
    seatSelection: '좌석 선택 시간',
    booking: '예매 가능 시간',
    help: '제한 시간이 지나면 처음부터 다시 예매해야 돼요. 시간 내 선택 및 입력을 완료해 주세요.',
  },
  seatTimeLeft: (minutes: number) => `좌석 선택 가능 시간이 ${minutes}분 남았어요`,
  payTimeLeft: (minutes: number) => `결제 가능 시간이 ${minutes}분 남았습니다.`,
  payTimeLastMinute: '1분 후, 결제 가능 시간이 종료됩니다.',

  seatTimeOver: {
    title: '좌석을 선택할 수 있는 시간 10분이 종료되었어요',
    description: '처음부터 다시 예매해 주세요.',
  },
  bookingTimeOver: {
    title: '결제할 수 있는 시간이 종료되었어요.',
    description: '처음부터 다시 예매해 주세요.',
    paymentCancelled: '결제는 자동으로 취소되었어요.',
  },
  sessionExpired: '세션이 만료되었습니다.',
  generalError: '예매 진행 중 오류가 발생했습니다.',
  busy: '이전 요청을 처리 중이에요. 잠시 후 다시 시도해 주세요.',

  seat: {
    loading: '예매 화면을 불러오고 있어요',
    loadFailed: '좌석 화면을 불러오는 도중 오류가 발생했습니다.',
    none: '선택한 좌석이 없습니다.',
    taken: '선택하신 좌석이 이미 선점되었습니다. 다른 좌석을 선택해주세요.',
    partlyTaken: '다른 고객님께서 이미 선택한 좌석은 선택 해제됩니다.',
    limit: '선택 가능한 매수를 초과했어요.',
    selectFailed: '좌석 선택 도중 오류가 발생했습니다.',
    releaseFailed: '좌석 해제 도중 오류가 발생했습니다.',
    released: '선택한 좌석이 해제됐어요.',
    unavailable: '선택 불가',
  },
  preselectWarning: {
    title: '선택한 좌석이 곧 해제돼요',
    description: '다음 단계로 진행하지 않으면 선택한 좌석은 2분 이내에 해제되어 다른 고객이 선택할 수 있어요.',
    action: '다음 단계로 이동',
  },
  soldOut: {
    title: '티켓 판매 완료',
    description: '아쉽게도 선택하신 회차에 준비된 티켓이 모두 판매 완료되었어요.',
  },
  backToSeat: {
    title: '좌석을 다시 선택하러 갈까요?',
    description: '선택했던 좌석은 해제됩니다.',
    cancel: '아니요',
    action: '다시 선택하기',
  },
  backBlocked: {
    title: '예매 가능 시간이 얼마 남지 않아서 좌석선택 단계로 이동할 수 없어요.',
    description: '선택한 좌석의 결제를 진행해주세요.',
    action: '계속 예매하기',
  },
  backToPrice: {
    title: '작성 중인 정보가 사라집니다. 이동하시겠습니까?',
    cancel: '아니요',
    action: '이동하기',
  },
  exit: {
    title: '예매를 종료할까요?',
    description: '상품예매가 완료되지 않았어요.',
    cancel: '아니요',
    action: '예매 종료하기',
  },
  seatLost: '예매 페이지 접속 후 일정 시간 이상 예매를 진행하지 않았거나, 판매된 좌석입니다. 처음부터 예매 시도 부탁 드립니다.',

  pay: {
    selectMethod: '결제수단을 선택해주세요.',
    processing: '결제를 진행하고 있어요.',
    failed: '결제 진행 중 오류가 발생했습니다.',
  },
  complete: {
    title: '예매 완료',
    description: '예매 상세 내역을 확인해주세요.',
    failed: '예매 내역을 불러오지 못했어요.',
    paid: '결제 완료',
  },
  records: {
    empty: '아직 연습한 기록이 없어요.',
    failed: '기록을 불러오지 못했어요.',
  },

  buttons: {
    start: '시작하기',
    retry: '다시 시도',
    resume: '이어서 하기',
    restart: '처음부터 다시',
    startPractice: '연습 시작',
    book: '예매하기',
    selectDone: '선택 완료',
    clearAll: '전체삭제',
    bookCount: (count: number) => `${count}매 예매하기`,
    proceed: '예매 진행하기',
    pay: (amount: string) => `총 ${amount} 결제하기`,
    history: '예매내역 확인',
    confirm: '확인',
    result: '결과 보기',
    newPractice: '새 연습 시작',
    myBookings: '내 예매',
  },
  map: {
    refresh: '좌석 상태 새로고침',
    zoomIn: '좌석도 확대',
    zoomOut: '좌석도 축소',
    fit: '좌석도 전체보기',
  },
  order: {
    title: '예매 정보 입력',
    booker: '예매자 정보',
    pickup: '티켓 수령 방법',
    pickupOnSite: '현장수령',
    notice: '예매 유의사항',
    noticeBody: '연습용 예매이며 실제 결제가 일어나지 않습니다.',
    terms: '약관 동의',
    agreeAll: '전체 동의',
    cancelPolicy: '(필수) 취소 규정 안내',
    usagePolicy: '(필수) 티켓 이용정책 동의',
  },
  payment: {
    title: '티켓 결제',
    method: '결제수단',
    info: '결제 정보',
    ticketAmount: '티켓금액',
    fee: '예매수수료',
    total: '최종 결제금액',
  },
} as const

export const DIFFICULTY_NAMES = { EASY: '쉬움', NORMAL: '보통', HARD: '어려움' } as const
export const RESULT_NAMES = { SUCCESS: '성공', SOLD_OUT: '매진', TIMEOUT: '시간 초과', ABANDONED: '중단' } as const
