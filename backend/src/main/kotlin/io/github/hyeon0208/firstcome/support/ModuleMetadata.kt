package io.github.hyeon0208.firstcome.support

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

// 공통 기술 코드라 모든 모듈이 하위 패키지까지 쓸 수 있게 연다.
@ApplicationModule(type = ApplicationModule.Type.OPEN)
@PackageInfo
class ModuleMetadata
