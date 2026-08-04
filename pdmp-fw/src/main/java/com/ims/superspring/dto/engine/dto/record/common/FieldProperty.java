package com.ims.superspring.dto.engine.dto.record.common;

/**
 * IMS SuperSpring FieldProperty 스텁.
 * DTO 코드제너레이터가 생성한 builder 호출을 수용한다.
 * 사내 SuperSpring 의존성이 있으면 이 클래스를 제거하고 원본을 사용한다.
 */
public final class FieldProperty {

    public static final int TYPE_OBJECT_STRING = 1;
    public static final int TYPE_OBJECT_INT = 2;

    private final String physicalName;
    private final String logicalName;
    private final int type;
    private final int decimal;
    private final boolean nullable;
    private final boolean encrypt;

    private FieldProperty(Builder builder) {
        this.physicalName = builder.physicalName;
        this.logicalName = builder.logicalName;
        this.type = builder.type;
        this.decimal = builder.decimal;
        this.nullable = builder.nullable;
        this.encrypt = builder.encrypt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public int getType() {
        return type;
    }

    public int getDecimal() {
        return decimal;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public static final class Builder {
        private String physicalName;
        private String logicalName;
        private int type;
        private int decimal = -1;
        private boolean nullable = true;
        private boolean encrypt = false;

        public Builder setPhysicalName(String physicalName) {
            this.physicalName = physicalName;
            return this;
        }

        public Builder setLogicalName(String logicalName) {
            this.logicalName = logicalName;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setDecimal(int decimal) {
            this.decimal = decimal;
            return this;
        }

        public Builder setIsNullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder setIsEncrypt(boolean encrypt) {
            this.encrypt = encrypt;
            return this;
        }

        public FieldProperty build() {
            return new FieldProperty(this);
        }
    }
}
