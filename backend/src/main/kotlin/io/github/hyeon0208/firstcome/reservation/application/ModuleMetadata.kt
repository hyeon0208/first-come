package io.github.hyeon0208.firstcome.reservation.application

import org.springframework.modulith.NamedInterface
import org.springframework.modulith.PackageInfo

// 다른 모듈이 쓸 수 있는 패키지로 공개한다.
@PackageInfo
@NamedInterface("application")
class ModuleMetadata
