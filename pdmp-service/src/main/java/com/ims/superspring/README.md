# IMS SuperSpring stub (로컬 컴파일용)

원본 SuperSpring JAR가 없을 때 DTO 생성 코드 컴파일을 위한 최소 stub입니다.

패키지: **`com.ims.superspring`**

| 클래스 | 경로 |
| --- | --- |
| `DataObject` | `com/ims/superspring/dto/DataObject.java` |
| `FieldProperty` | `com/ims/superspring/dto/engine/dto/record/common/FieldProperty.java` |
| `JsonMessage` | `com/ims/superspring/dto/engine/base/JsonMessage.java` |
| `MarshalException` | `com/ims/superspring/dto/engine/exception/MarshalException.java` |
| `UnmarshalException` | `com/ims/superspring/dto/engine/exception/UnmarshalException.java` |
| `MarshallException` | `com/ims/superspring/dto/engine/exception/MarshallException.java` (레거시 표기 호환) |

사내 Nexus에서 SuperSpring 의존성을 받을 수 있으면 **이 stub 패키지를 삭제**하고 원본 JAR를 사용한다.
