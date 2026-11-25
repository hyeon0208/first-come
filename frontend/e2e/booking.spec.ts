import { expect, test, type Locator, type Page } from '@playwright/test'

// 닉네임 입력부터 내 예매 확인까지 한 번에 끝나고, 다시 시작하면 새 회차로 처음부터 되는지 확인한다.
// docker compose로 띄운 실제 서버, 화면, 모의 결제 서버, MySQL을 그대로 쓴다.

async function startPractice(page: Page, difficulty: string) {
  await page.getByRole('heading', { name: /모래시계 밴드/ }).first().click()
  await page.getByText(difficulty, { exact: true }).click()
  await page.getByRole('button', { name: '연습 시작' }).click()
  // 연습 시작 10초 뒤가 예매 오픈이다. 그 전에는 버튼이 남은 시간을 보여 주며 꺼져 있다.
  await expect(page.getByRole('button', { name: /예매 오픈까지 0:0\d/ })).toBeVisible()
  await page.getByRole('button', { name: '예매하기' }).click({ timeout: 20_000 })
  await expect(seatMap(page)).toBeVisible({ timeout: 20_000 })
}

function seatMap(page: Page): Locator {
  return page.getByRole('group', { name: '좌석 배치도' })
}

// 배치도에서 가장 뒤쪽의 선택 가능 좌석(가상 경쟁자가 가장 늦게 고르는 곳)
function lastAvailableSeat(page: Page): Locator {
  return seatMap(page).locator('[data-status="available"]').last()
}

// 좌석을 눌러 내 좌석이 count개가 될 때까지. 누른 좌석을 가상 경쟁자가 먼저 잡았으면 그 좌석은 회색이 되고 다음 좌석을 누른다.
async function pressSeatUntilSelected(page: Page, count: number) {
  await expect(async () => {
    await lastAvailableSeat(page).click({ timeout: 2_000 })
    await expect(page.getByRole('heading', { name: `선택 좌석 ${count}` })).toBeVisible({ timeout: 2_000 })
  }).toPass({ timeout: 20_000 })
}

test('닉네임부터 내 예매까지 한 번에 끝나고, 다시 시작하면 새 회차로 처음부터 된다', async ({ page }) => {
  await test.step('닉네임을 넣는다', async () => {
    await page.goto('/')
    await expect(page.getByRole('button', { name: '시작하기' })).toHaveAttribute('aria-disabled', 'true')
    await page.getByLabel('닉네임').fill('연습생')
    await page.getByRole('button', { name: '시작하기' }).click()
    await expect(page.getByRole('heading', { name: '공연', exact: true })).toBeVisible()
  })

  await test.step('공연을 고르고 쉬움 난이도로 연습을 시작해 예매 화면에 들어간다', async () => {
    await startPractice(page, '쉬움')
    await expect(page.getByText('좌석 선택 시간', { exact: true })).toBeVisible()
  })

  await test.step('좌석 둘을 누르고(가선점) 하나를 뺀 뒤 선택 완료(선점)한다', async () => {
    await pressSeatUntilSelected(page, 1)
    await pressSeatUntilSelected(page, 2)
    await page.getByRole('button', { name: /빼기$/ }).first().click()
    await expect(page.getByRole('heading', { name: '선택 좌석 1' })).toBeVisible()
    await page.getByRole('button', { name: '선택 완료' }).click()
    await expect(page.getByRole('heading', { name: '가격 선택' })).toBeVisible()
    await expect(page.getByText('예매 가능 시간', { exact: true })).toBeVisible()
    // 토스트는 화면에 보이는 것과 읽어 주는 영역, 두 곳에 들어간다.
    await expect(page.getByText('결제 가능 시간이 7분 남았습니다.').last()).toBeVisible()
  })

  await test.step('가격 수량을 좌석 수만큼 채운다', async () => {
    await expect(page.getByRole('button', { name: '예매하기', exact: true })).toHaveAttribute('aria-disabled', 'true')
    await page.getByRole('button', { name: /일반 수량 증가/ }).click()
    await expect(page.getByText('1/1')).toBeVisible()
    await page.getByRole('button', { name: '1매 예매하기' }).click()
    await expect(page.getByRole('heading', { name: '예매 정보 입력' })).toBeVisible()
  })

  await test.step('약관에 동의하고 결제한다', async () => {
    await expect(page.getByRole('button', { name: '예매 진행하기' })).toHaveAttribute('aria-disabled', 'true')
    await page.getByRole('checkbox', { name: '전체 동의' }).check()
    await page.getByRole('button', { name: '예매 진행하기' }).click()
    await expect(page.getByRole('heading', { name: '티켓 결제' })).toBeVisible()
    await page.getByRole('button', { name: /^총 [\d,]+원 결제하기$/ }).click()
    await expect(page.getByText('결제수단을 선택해주세요.')).toBeVisible()
    await page.getByRole('radio', { name: '카드' }).check()
    await page.getByRole('button', { name: /^총 [\d,]+원 결제하기$/ }).click()
    // 모의 결제 서버가 1~3초 뒤 승인한다.
    await expect(page.getByText('결제를 진행하고 있어요.')).toBeVisible()
  })

  await test.step('예매 완료 화면에서 걸린 시간과 예매 정보를 본다', async () => {
    await expect(page.getByRole('heading', { name: '예매 완료' })).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText(/오픈 뒤 (\d+분 )?\d+초/)).toBeVisible()
    await expect(page.getByText(/^T\d{10}$/)).toBeVisible()
    await expect(page.getByText('총 1매')).toBeVisible()
    await expect(page.getByText('결제 완료')).toBeVisible()
  })

  await test.step('내 예매에 성공 한 줄이 남는다', async () => {
    await page.getByRole('button', { name: '예매내역 확인' }).click()
    await expect(page.getByRole('heading', { name: '내 예매' })).toBeVisible()
    await expect(page.getByRole('row').filter({ hasText: '성공' })).toHaveCount(1)
    await expect(page.getByRole('row').filter({ hasText: '성공' })).toContainText('쉬움')
  })

  await test.step('다시 시작하면 새 회차가 만들어지고 좌석 화면이 처음 상태로 열린다', async () => {
    await page.getByRole('button', { name: '새 연습 시작' }).click()
    await startPractice(page, '쉬움')
    await expect(page.getByRole('heading', { name: '선택 좌석 0' })).toBeVisible()
    await expect(page.getByText('선택한 좌석이 없습니다.')).toBeVisible()
  })

  await test.step('예매 화면에서 나가려 하면 종료할지 묻고, 종료하면 중단으로 기록된다', async () => {
    await page.getByRole('button', { name: '내 예매' }).click()
    await expect(page.getByText('예매를 종료할까요?')).toBeVisible()
    await page.getByRole('button', { name: '예매 종료하기' }).click()
    await expect(page.getByRole('heading', { name: '내 예매' })).toBeVisible()
    await expect(page.getByRole('row').filter({ hasText: '중단' })).toHaveCount(1)
    await expect(page.getByRole('row').filter({ hasText: '성공' })).toHaveCount(1)
  })
})

test('가상 경쟁자가 먼저 잡은 좌석은 서버가 거절하고, 어려움 난이도는 곧 매진된다', async ({ page }) => {
  await page.goto('/')
  await page.getByLabel('닉네임').fill('경쟁확인')
  await page.getByRole('button', { name: '시작하기' }).click()
  await startPractice(page, '어려움')

  // 화면은 좌석 상태를 자동으로 다시 받지 않는다. 처음 받은 상태에서 선택 가능으로 보이는 무대 앞 좌석을 누른다.
  // 서버가 거절하는 길은 둘이다: 이미 잡힌 좌석이라는 안내, 또는 그 좌석이 마지막이었다면 매진 안내.
  const takenToast = page.getByText('선택하신 좌석이 이미 선점되었습니다. 다른 좌석을 선택해주세요.').last()
  const soldOutDialog = page.getByText('티켓 판매 완료')
  let rejected = false
  for (let attempt = 0; attempt < 20 && !rejected; attempt++) {
    await seatMap(page).locator('[data-status="available"]').first().click({ timeout: 2_000 }).catch(() => undefined)
    await page.waitForTimeout(400)
    rejected = (await takenToast.isVisible().catch(() => false)) || (await soldOutDialog.isVisible().catch(() => false))
    // 내가 4석을 채우면 더 누를 수 없다. 비우고 계속한다.
    if (!rejected && (await page.getByRole('heading', { name: '선택 좌석 4' }).isVisible())) {
      await page.getByRole('button', { name: '전체삭제' }).click()
      await expect(page.getByRole('heading', { name: '선택 좌석 0' })).toBeVisible()
    }
  }

  expect(rejected, '가상 경쟁자가 먼저 잡은 좌석을 눌러 거절당해야 한다').toBe(true)

  // 내가 잡은 좌석을 놓으면 뒤이어 들어온 가상 경쟁자가 가져간다.
  // 어려움은 약 10초에 매진된다. 새로고침으로 좌석 상태를 다시 받으면, 내 좌석이 없을 때 매진 안내가 뜬다.
  await page.getByRole('button', { name: '전체삭제' }).click({ timeout: 2_000 }).catch(() => undefined)
  await expect(async () => {
    if (!(await page.getByText('티켓 판매 완료').isVisible())) {
      await page.getByRole('button', { name: '좌석 상태 새로고침' }).click({ timeout: 2_000 })
    }
    await expect(page.getByText('티켓 판매 완료')).toBeVisible({ timeout: 1_000 })
  }).toPass({ timeout: 60_000 })
  await expect(page.getByText('아쉽게도 선택하신 회차에 준비된 티켓이 모두 판매 완료되었어요.')).toBeVisible()

  await page.getByRole('button', { name: '결과 보기' }).click()
  await expect(page.getByRole('row').filter({ hasText: '매진' })).toHaveCount(1)
})
