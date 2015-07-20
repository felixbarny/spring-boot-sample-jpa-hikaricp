/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jpa;


import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleJpaApplication.class)
@WebIntegrationTest
public class StagemonitorIntegrationTests {

	@Value("${local.server.port}")
	private int port;

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new TestRestTemplate();
		template.getInterceptors().add(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
				request.getHeaders().set("Accept", "text/html");
				return execution.execute(request, body);
			}
		});
	}

	@Test
	public void testStagemonitorIframeIsInjected() throws Exception {
		final String page = this.template.getForObject("http://localhost:" + this.port, String.class);
		assertTrue(page, page.contains("<iframe id=\"stagemonitor-modal\""));
	}

	@Test
	public void testStagemonitorSqlQueriesAreCollected() throws Exception {
		final String requestTrace = getRequestTrace(this.template.getForObject("http://localhost:" + this.port, String.class));
		assertTrue(requestTrace, requestTrace.contains("JpaNoteRepository.findAll"));
		assertTrue(requestTrace, requestTrace.contains("select note0_.id as id1_0_, note0_.body as body2_0_, note0_.title as title3_0_ from note note0_"));
	}

	private String getRequestTrace(String htmlPage) {
		Matcher m = Pattern.compile("data = (.*),").matcher(htmlPage);
		if (m.find()) {
			return m.group(1);
		} else {
			throw new IllegalStateException("Page does not include the request trace");
		}
	}

}
