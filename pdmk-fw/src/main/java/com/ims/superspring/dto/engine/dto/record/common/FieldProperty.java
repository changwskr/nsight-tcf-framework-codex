package com.ims.superspring.dto.engine.dto.record.common;

/**
 * IMS SuperSpring FieldProperty 스텁.
 * DTO 코드제너레이터가 생성한 builder 호출을 수용한다.
 * 사내 SuperSpring 의존성이 있으면 이 클래스를 제거하고 원본을 사용한다.
 */
public final class FieldProperty {

    public static final int TYPE_OBJECT_STRING = 1;
    public static final int TYPE_OBJECT_INT = 2;
    public static final int TYPE_PRIMITIVE_INT = 3;
    public static final int TYPE_ABSTRACT_INCLUDE = 4;

    private final String physicalName;
    private final String logicalName;
    private final int type;
    private final int decimal;
    private final boolean nullable;
    private final boolean encrypt;
    private final String arrayProperty;
    private final String reference;

    private FieldProperty(Builder builder) {
        this.physicalName = builder.physicalName;
        this.logicalName = builder.logicalName;
        this.type = builder.type;
        this.decimal = builder.decimal;
        this.nullable = builder.nullable;
        this.encrypt = builder.encrypt;
        this.arrayProperty = builder.arrayProperty;
        this.reference = builder.reference;
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

    public String getArrayProperty() {
        return arrayProperty;
    }

    public String getReference() {
        return reference;
    }

    public static final class Builder {
        private String physicalName;
        private String logicalName;
        private int type;
        private int decimal = -1;
        private boolean nullable = true;
        private boolean encrypt = false;
        private String arrayProperty;
        private String reference;

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

        public Builder setArrayProperty(String arrayProperty) {
            this.arrayProperty = arrayProperty;
            return this;
        }

        /** 생성 코드 호환용 alias. */
        public Builder setArray(String arrayProperty) {
            return setArrayProperty(arrayProperty);
        }

        public Builder setReference(String reference) {
            this.reference = reference;
            return this;
        }

        public FieldProperty build() {
            return new FieldProperty(this);
        }
    }
}
