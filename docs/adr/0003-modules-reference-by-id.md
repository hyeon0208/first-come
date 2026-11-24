---
status: accepted
---

# 모듈 사이에는 FK와 JOIN을 두지 않고 id로만 참조한다

모듈은 `user`, `show`, `performance`, `reservation`, `competitor`, `record`, `support`다. 엔티티는 JPA 연관관계를 쓰지 않고 다른 엔티티의 id만 가진다. 다른 모듈의 테이블에는 FK를 걸지 않고 JOIN하지 않으며, 다른 모듈의 데이터는 그 모듈의 `application` 패키지를 불러 읽는다. 경계는 Spring Modulith 검증 테스트로 지킨다. 의존은 `competitor → reservation → performance → show, user` 한 방향이고, 반대 방향 알림(회차 종료, 회차 데이터 삭제)은 애플리케이션 이벤트로 보낸다. 이후 단계에서 근거가 나온 모듈을 서비스로 떼어 낼 때 걸림돌을 없애려는 결정이다.

## Considered Options

- 모듈 사이에도 FK 유지: 무결성은 DB가 지켜 주지만, 서비스를 나누기 전에 FK부터 걷어내야 한다.
- 모듈을 열어 두기(Modulith OPEN): 기존 코드를 덜 고쳐도 되지만 검증이 아무것도 막지 못한다.

## Consequences

- 모듈을 넘는 무결성은 코드가 지킨다. 회차 데이터를 지울 때 예매 데이터도 이벤트로 함께 지운다.
- 같은 모듈 안의 테이블끼리는 FK를 둔다.
- 이벤트는 같은 프로세스 안의 동기 전달이다. Outbox와 메시지 발행은 2단계에서 직접 만든다.
