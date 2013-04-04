package com.jcabi.foo;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public final class SampleTest {

    @Test
    public void lombokAnnotationsWork() throws Exception {
        MatcherAssert.assertThat(new Sample(), Matchers.equalTo(new Sample()));
    }

    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void aspectjAnnotationsWork() throws Exception {
        new Sample().notNull(null);
    }

}
