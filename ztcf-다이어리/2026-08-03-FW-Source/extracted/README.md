# FW Source 이미지 추출 Java

추출일: 2026-08-04  
출처: 채팅에 첨부된 Java 스크린샷(이미지 OCR)

## 파일

| 파일 | 내용 |
| --- | --- |
| `StringUtil-partial.java` | String 유틸 본문 추출. 클래스 앞부분·`isHangul(char)`·`ObjectUtil` 원본 없음 |
| `ValidateUtil.java` | `nhnis.fw.commons.util.ValidateUtil` 추출 |

## 주의

- OCR 기반이라 오탈자 가능. 원본 PDF(`img-*/`)와 대조할 것
- `StringUtil-partial`은 단독 컴파일 목적이 아님 (스텁·추정 클래스명 포함)
- `ValidateUtil`의 `IllegalArgumentException` 메시지가 빈 문자열인 부분은 원문 그대로
