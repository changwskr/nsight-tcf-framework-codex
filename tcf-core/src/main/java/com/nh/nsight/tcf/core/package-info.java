/**
 * TCF 프레임워크 코어 — 표준 전문·거래 파이프라인·공통 인프라.
 *
 * <p>업무 WAR({@code *-service})의 {@code entry/application/persistence}와 달리,
 * 본 모듈은 <b>프레임워크 JAR</b>로 패키지 이동 없이 역할별로 분리되어 있습니다.
 *
 * <ul>
 *   <li>업무 계층: {@code application}, {@code entry}, {@code persistence}</li>
 *   <li>설정: {@code config}</li>
 *   <li>프레임워크 지원: {@code support} — processor, dispatch, transaction, message, context, error, …</li>
 * </ul>
 */
package com.nh.nsight.tcf.core;
