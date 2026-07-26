# C06 — ServiceId-거래코드

**선행:** [C05-업무규칙-상태.md](./C05-업무규칙-상태.md)  
**다음:** [C07-요청-응답-DTO.md](./C07-요청-응답-DTO.md)  
**결과 파일:** [`결과/C06-ServiceId-거래코드.md`](./결과/C06-ServiceId-거래코드.md)

## 이 단계에서 얻는 것

ServiceId·거래코드·OM대상

## 지금 이 질문을 붙여 넣으세요

```text
C-MASTER를 적용한다. 결과/_확정정보원장.md 와 이전 결과/C*.md 를 인용한다.
코드는 C14 Gate 전 쓰지 마라. 질문 1개씩.
단계가 끝나면 결과/C06-ServiceId-거래코드.md 와 원장을 파일로 갱신한 뒤
「다음 단계로 갈까요?」만 물어라.

프로젝트 ServiceId 형식을 알려 주세요. 모르면 기준 모듈에서 찾겠습니다.

권장: {업무코드}.{도메인}.{행위}
예: AV.Sample.inquiry / AV.Sample.create …
```

## 추가 확인 (질문 1개씩)

검증: 대소문자·중복·Handler·OM·Timeout 연결.
CUD 명칭 미정이면 미확정으로 표기. 결과를 ServiceId 표로 저장.

## Gate

- 이벤트↔ServiceId
- 중복 없음
- OM 등록 대상 정의

## 결과 저장

`결과/C06-ServiceId-거래코드.md` 상단 확정표 + 산출물 본문. 원장 `_확정정보원장.md` 동기화.
