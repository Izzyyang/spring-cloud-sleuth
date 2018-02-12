/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.web.client;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TraceWebClientAutoConfigurationTests.Config.class)
public class TraceWebClientAutoConfigurationTests {

	@Autowired @Qualifier("firstRestTemplate") RestTemplate restTemplate;
	@Autowired @Qualifier("secondRestTemplate") RestTemplate secondRestTemplate;

	@Test
	public void should_add_rest_template_interceptors() {
		assertInterceptorsOrder(assertInterceptorsNotEmpty(this.restTemplate));
		assertInterceptorsOrder(assertInterceptorsNotEmpty(this.secondRestTemplate));
	}

	private List<ClientHttpRequestInterceptor> assertInterceptorsNotEmpty(RestTemplate restTemplate) {
		then(restTemplate).isNotNull();
		List<ClientHttpRequestInterceptor> interceptors = restTemplate
				.getInterceptors();
		then(interceptors).isNotEmpty();
		return interceptors;
	}

	private void assertInterceptorsOrder(
			List<ClientHttpRequestInterceptor> interceptors) {
		int traceInterceptorIndex = 0;
		int myInterceptorIndex = 0;
		int mySecondInterceptorIndex = 0;
		for (int i = 0; i < interceptors.size(); i++) {
			if (interceptors.get(i) instanceof TraceRestTemplateInterceptor) {
				traceInterceptorIndex = i;
			} else if (interceptors.get(i) instanceof MyClientHttpRequestInterceptor) {
				myInterceptorIndex = i;
			} else if (interceptors.get(i) instanceof MySecondClientHttpRequestInterceptor) {
				mySecondInterceptorIndex = i;
			}
		}
		then(traceInterceptorIndex)
				.isLessThan(myInterceptorIndex)
				.isLessThan(mySecondInterceptorIndex);
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

		@Bean
		@Qualifier("firstRestTemplate")
		RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder
					.additionalInterceptors(new MyClientHttpRequestInterceptor())
					.build();
		}

		@Bean
		@Qualifier("secondRestTemplate")
		RestTemplate secondRestTemplate(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder
					.additionalInterceptors(new MyClientHttpRequestInterceptor())
					.build();
		}

		@Bean
		RestTemplateCustomizer myRestTemplateCustomizer() {
			return restTemplate -> {
				restTemplate.getInterceptors().add(0, new MySecondClientHttpRequestInterceptor());
			};
		}

	}
}

class MyClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	@Override public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		return execution.execute(request, body);
	}
}

class MySecondClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	@Override public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		return execution.execute(request, body);
	}
}