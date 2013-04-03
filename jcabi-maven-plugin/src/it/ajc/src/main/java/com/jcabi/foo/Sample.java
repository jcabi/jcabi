package com.jcabi.foo;

import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
final class Sample {
    public String notNull(@NotNull String value) {
        return value;
    }
}
