package com.jcabi.foo;

import org.junit.Test;

public final class SampleTest {

    @Test
    public void lombokAnnotationsWork() throws Exception {
        MatcherAssert.assertThat(new Sample(), Matchers.equalTo(new Sample()));
    }

    @Test
    public void aspectjAnnotationsWork() throws Exception {
        new Sample().notNull(null);
    }

}
