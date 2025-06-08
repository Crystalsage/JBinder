package org.jbinder.xsd.types;

public non-sealed class XsdStringType implements XsdTypeInfo {
    String pattern;
    int minLength;
    int maxLength;

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public String toString() {
        return "XsdStringType{" +
            "pattern='" + pattern + '\'' +
            ", minLength=" + minLength +
            ", maxLength=" + maxLength +
            '}';
    }
}