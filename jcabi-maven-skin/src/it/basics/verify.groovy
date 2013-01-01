/**
 * Copyright (c) 2012-2013, JCabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.rexsl.test.XhtmlMatchers
import com.rexsl.w3c.ValidatorBuilder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers

def version = new XmlParser().parse(new File(basedir, 'pom.xml')).version.text()
MatcherAssert.assertThat(
    new File(basedir, 'build.log').text,
    Matchers.not(Matchers.containsString('ERROR'))
)

[
    'basics-child/target/site/index.html',
].each {
    def file = new File(basedir, it)
    if (!file.exists()) {
        throw new IllegalStateException(
            "file ${file} doesn't exist"
        )
    }
}

def html = new File(basedir, 'target/site/index.html').text
MatcherAssert.assertThat(
    html,
    XhtmlMatchers.hasXPaths(
        '//xhtml:head/xhtml:link[@rel="shortcut icon"]',
        '//xhtml:body',
        "//xhtml:p[contains(.,'${version}')]",
        '//xhtml:p[contains(.,"test-org-name")]'
    )
)

def htmlResponse = new ValidatorBuilder().html().validate(html)
MatcherAssert.assertThat(
    htmlResponse.errors(),
    /**
     * @todo #86 This validation doesn't work because maven-site-plugin produces
     *  invalid HTML5 output (still using TT element, which is obsolete in
     * HTML5). We're expecting exactly one error here, because of that.
     */
    Matchers.describedAs(htmlResponse.toString(), Matchers.hasSize(1))
)
MatcherAssert.assertThat(
    htmlResponse.warnings(),
    /**
     * @todo #86 This validation doesn't work because maven-site-plugin produces
     *  invalid HTML5 output (it is using an obsolete NAME attribute on
     *  some HTML elements). We're expecting exactly one warning here,
     *  because of that.
     */
    Matchers.describedAs(htmlResponse.toString(), Matchers.hasSize(1))
)

def cssResponse = new ValidatorBuilder().css().validate(
    new File(basedir, 'target/site/css/jcabi.css').text
)
MatcherAssert.assertThat(
    cssResponse.valid(),
    Matchers.describedAs(cssResponse.toString(), Matchers.is(true))
)
