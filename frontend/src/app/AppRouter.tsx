import { createBrowserRouter, Navigate, type RouteObject } from 'react-router'
import { BookingShell } from '../features/booking-shell'
import { CompletePage } from '../pages/CompletePage'
import { HomePage } from '../pages/HomePage'
import { MePage } from '../pages/MePage'
import { OrderPage } from '../pages/OrderPage'
import { PayPage } from '../pages/PayPage'
import { SeatPage } from '../pages/SeatPage'
import { ShowDetailPage } from '../pages/ShowDetailPage'
import { WaitingPage } from '../pages/WaitingPage'
import { RequireUser } from './RequireUser'

// 화면 10개에 경로 8개다. 닉네임 입력과 목록이 "/"를 함께 쓰고, 가격 선택은 좌석 화면의 ?step=price다.
export const routes: RouteObject[] = [
  { path: '/', element: <HomePage /> },
  {
    element: <RequireUser />,
    children: [
      { path: '/shows/:showId', element: <ShowDetailPage /> },
      { path: '/practice/:runId/waiting', element: <WaitingPage /> },
      {
        path: '/practice/:runId',
        element: <BookingShell />,
        children: [
          { index: true, element: <Navigate to="seat" replace /> },
          { path: 'seat', element: <SeatPage /> },
          { path: 'order', element: <OrderPage /> },
          { path: 'pay', element: <PayPage /> },
        ],
      },
      { path: '/practice/:runId/complete', element: <CompletePage /> },
      { path: '/me', element: <MePage /> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]

export function createAppRouter() {
  return createBrowserRouter(routes)
}
