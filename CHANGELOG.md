# Changelog

## [1.11.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.10.0...v1.11.0) (2026-06-11)


### Features

* 게시글 목록 직군·경력 필터링 추가 ([#87](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/87)) ([df9b95b](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/df9b95bfa847f8ae174150dbf417254b707188a7))

## [1.10.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.9.0...v1.10.0) (2026-06-11)


### Features

* **cicd:** 릴리즈 시 운영 자동 배포 및 HTTP 포트 80 변경 ([#84](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/84)) ([f654836](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/f654836cd5ff9e22e3d45331c43944b8e6b096d6)), closes [#83](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/83)

## [1.9.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.8.0...v1.9.0) (2026-06-10)


### Features

* **mypage:** 몬스터 통계 API 구현 ([#81](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/81)) ([7656c70](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/7656c70e1f44d5039aeb5b8de58b834f959e0450))

## [1.8.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.7.0...v1.8.0) (2026-06-08)


### Features

* **comment:** 댓글 CRUD 및 댓글 공감 기능 구현 ([fc98647](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/fc98647c5d3d567953ed3f95f53857a73fa5acfe))
* **comment:** 댓글 CRUD 및 댓글 공감 기능 구현 ([d9ca155](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/d9ca155dfe841e18effce4917dc39c7334c8887c))
* Flyway 마이그레이션 도입 ([#80](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/80)) ([490727a](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/490727a4fb53255533bccb3cdfc87cd9db8af28f))


### Bug Fixes

* main 브랜치 머지 충돌 해결 및 코파일럿 리뷰 반영 ([0458816](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/04588164abd316a8f649cfeea9f866dd16059129))

## [1.7.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.6.1...v1.7.0) (2026-05-31)


### Features

* **post:** 게시글 목록/상세 조회 및 수정 API 구현 ([#73](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/73)) ([5bce840](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/5bce840bb5ec509733035a5c6e3ac3e35694e26f))

## [1.6.1](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.6.0...v1.6.1) (2026-05-28)


### Bug Fixes

* **deploy:** secret 값 특수문자로 인한 환경변수 누락 수정 ([ac449f0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/ac449f094a0c647d59dae44421614abcff219933))
* **deploy:** secret 특수문자로 인한 배포 실패 수정 ([15d5370](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/15d5370db9a459ee5e477dc6f9acbc9b6b557a63))
* **deploy:** 배포 파이프라인 전면 수정 ([9112c6c](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/9112c6c89f33608ae6e4d620d2883ad0ed526eaf))

## [1.6.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.5.1...v1.6.0) (2026-05-27)


### Features

* **security:** OAuth2 엔드포인트 허용 및 로컬 설정 파일 추적 제거 ([5d0e564](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/5d0e564894ca6d44034e384839aa439d3ad2f664))
* **security:** OAuth2 엔드포인트 허용 및 로컬 설정 파일 추적 제거 ([51cd74b](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/51cd74be6f2716b72078d03340dd853c0ddd6e9f))


### Bug Fixes

* **deploy:** main 머지 컨플릭트 해결 — DB_URL_PROD 반영 ([f7986ea](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/f7986ea314a83a686ffec4c9b2086c40c2cc41b3))
* **deploy:** 리뷰 반영 — secrets 쉘 확장 방지 + 필수 변수 누락 시 즉시 실패 ([dfdd9f7](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/dfdd9f7b02d070cecf9df8a9059967517d03d0fd))
* **deploy:** 프로덕션 배포 시 누락된 환경변수 추가 ([53f7a43](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/53f7a430671baa7b362056a8a4e876e9dfc0ec99))
* **deploy:** 프로덕션 배포 시 누락된 환경변수 추가 ([6acb2c6](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/6acb2c62be80de076305cf9099d90c46436db9b3))
* **security:** 리뷰 반영 — OAuth2 permitAll 최소 권한 + 프로필 기본값 환경변수화 ([fa660d8](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/fa660d887d6f3b6e252e170ad6ca0324dceaac4f))

## [1.5.1](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.5.0...v1.5.1) (2026-05-25)


### Bug Fixes

* dev 배포 환경변수 전달 누락 수정 ([#66](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/66)) ([728261a](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/728261a3216cf5f6d4096d77b772f719f04eea36))

## [1.5.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.4.0...v1.5.0) (2026-05-25)


### Features

* **cicd:** 특정 릴리즈 버전 수동 배포 지원 ([#61](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/61)) ([dc5fbfa](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/dc5fbfa11c73cc3a931363114a25bcfea0446aa1))

## [1.4.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.3.3...v1.4.0) (2026-05-21)


### Features

* **post:** 게시글 작성/삭제 및 감정·몬스터 연동 구현 ([#58](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/58)) ([ac307cc](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/ac307ccd385ac3c2499f5baa3b3b8352b45b7fc5))

## [1.3.3](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.3.2...v1.3.3) (2026-05-21)


### Bug Fixes

* **cicd:** 운영 배포에 JWT_SECRET 환경변수 추가 ([c2dc471](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/c2dc471e43eb5ad0a36fb876c2b378bfd3092703))

## [1.3.2](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.3.1...v1.3.2) (2026-05-18)


### Bug Fixes

* **ai:** Claude provider 제거 및 OpenAI 등록 복구 ([#53](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/53)) ([e3089c2](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/e3089c2ed1467c884891b8dedd9931ee60094272))

## [1.3.1](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.3.0...v1.3.1) (2026-05-16)


### Bug Fixes

* **ai:** disable claude provider by default ([#45](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/45)) ([19ab8bb](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/19ab8bb6b945c53a2f212782b98526bd753fffd5))

## [1.3.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.2.2...v1.3.0) (2026-05-16)


### Features

* **logging:** add prod access logs ([#40](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/40)) ([#41](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/41)) ([d6b5dd9](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/d6b5dd91fec8c9c6ed1099e85c52eef004b30060))

## [1.2.2](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.2.1...v1.2.2) (2026-05-16)


### Bug Fixes

* **swagger:** remove hardcoded OpenAPI servers ([a8e915a](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/a8e915a089ef7ac6109945d1c54b54d612153b1b))

## [1.2.1](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.2.0...v1.2.1) (2026-05-16)


### Bug Fixes

* **swagger:** enable prod docs and return 404 for missing resources ([cc863b9](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/cc863b94be37742a6eb880cf4b9a3c6865810126))

## [1.2.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.1.0...v1.2.0) (2026-05-16)


### Features

* **ai:** LLM Gateway Pattern 도입 및 댓글 요약 서비스 추가 ([#35](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/35)) ([#36](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/36)) ([595e642](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/595e6420aa93dfa64a92de0abf72017c3e1b15f8))

## [1.1.0](https://github.com/DDD-Community/DDD-13-WEBBB_BE/compare/v1.0.0...v1.1.0) (2026-05-14)


### Features

* AI 공통 감정 분석 서비스 추가 ([#33](https://github.com/DDD-Community/DDD-13-WEBBB_BE/issues/33)) ([d38438e](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/d38438e44fa78271995fd55f8bc6828530a1fc5d))

## 1.0.0 (2026-05-08)


### Features

* **poc:** Java 21 및 헥사고날 아키텍처 기반 AI 취준 회고 도우미 PoC 구현 ([33efa2e](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/33efa2e71b633422d073bd7b16e1dc66c2abb6ff))
* PR 메시지와 훅 연결하기 ([7f5a0d1](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/7f5a0d123cb97649cf35539890176016b1482804))


### Bug Fixes

* copilot review ([7bcfd8e](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/7bcfd8ee37caa5b2f6b1e1e5046d748bc9164528))
* 가독성 향상을 위한 양식 변경 ([0eafc91](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/0eafc919feffffe4c478ae0b6c9a59ff1848cb84))
* 멘션 기능 추가 ([ea26a12](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/ea26a1294455dde6a51d2749d1bc5e6901205108))
* 양식 ([f3b3ce4](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/f3b3ce4c77aba37b4aae6de929725cfb94c1f4ff))
* 양식 변경 ([050a393](https://github.com/DDD-Community/DDD-13-WEBBB_BE/commit/050a393b1a6b6b042c8f7b49641af66dedfe397f))
