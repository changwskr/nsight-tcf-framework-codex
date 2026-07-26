# NSIGHT Tomcat 전체 설정 파일

> **참조 구성:** 17 WAR 목표 아키텍처를 설명하는 설정 자료이며, 현재 저장소의 ztomcat 16 WAR 배포 목록과는 별도입니다.

구조:
nh.marketing.com → Apache → Tomcat Cluster(DeltaManager) → 17 WAR

전제:
- Apache Sticky Session은 JSESSIONID + Tomcat jvmRoute 기준
- Tomcat DeltaManager로 센터 내부 WAS 간 세션 복제
- 모든 Tomcat 노드에 동일한 17개 WAR 배포
- 모든 WAR는 WEB-INF/web.xml에 <distributable/> 포함 필요
- 세션 객체는 Serializable 필수
