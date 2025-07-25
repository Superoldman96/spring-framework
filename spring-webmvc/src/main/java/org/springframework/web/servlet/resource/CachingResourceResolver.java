/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link org.springframework.web.servlet.resource.ResourceResolver} that
 * resolves resources from a {@link org.springframework.cache.Cache} or otherwise
 * delegates to the resolver chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	/**
	 * The prefix used for resolved resource cache keys.
	 */
	public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

	/**
	 * The prefix used for resolved URL path cache keys.
	 */
	public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";


	private final Cache cache;

	private final List<String> contentCodings = new ArrayList<>(EncodedResourceResolver.DEFAULT_CODINGS);


	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		this.cache = cache;
	}


	/**
	 * Return the configured {@code Cache}.
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * Configure the supported content codings from the
	 * {@literal "Accept-Encoding"} header for which to cache resource variations.
	 * <p>The codings configured here are generally expected to match those
	 * configured on {@link EncodedResourceResolver#setContentCodings(List)}.
	 * <p>By default this property is set to {@literal ["br", "gzip"]} based on
	 * the value of {@link EncodedResourceResolver#DEFAULT_CODINGS}.
	 * @param codings one or more supported content codings
	 * @since 5.1
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * Return a read-only list with the supported content codings.
	 * @since 5.1
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}


	@Override
	protected @Nullable Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = computeKey(request, requestPath);
		Resource resource = this.cache.get(key, Resource.class);

		if (resource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Resource resolved from cache");
			}
			return resource;
		}

		resource = chain.resolveResource(request, requestPath, locations);
		if (resource != null) {
			this.cache.put(key, resource);
		}

		return resource;
	}

	protected String computeKey(@Nullable HttpServletRequest request, String requestPath) {
		if (request != null) {
			String codingKey = getContentCodingKey(request);
			if (StringUtils.hasText(codingKey)) {
				return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath + "+encoding=" + codingKey;
			}
		}
		return RESOLVED_RESOURCE_CACHE_KEY_PREFIX + requestPath;
	}

	private @Nullable String getContentCodingKey(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
		if (!StringUtils.hasText(header)) {
			return null;
		}
		return Arrays.stream(StringUtils.tokenizeToStringArray(header, ","))
				.map(token -> {
					int index = token.indexOf(';');
					return (index >= 0 ? token.substring(0, index) : token).trim().toLowerCase(Locale.ROOT);
				})
				.filter(this.contentCodings::contains)
				.sorted()
				.collect(Collectors.joining(","));
	}

	@Override
	protected @Nullable String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;
		String resolvedUrlPath = this.cache.get(key, String.class);

		if (resolvedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Path resolved from cache");
			}
			return resolvedUrlPath;
		}

		resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
		if (resolvedUrlPath != null) {
			this.cache.put(key, resolvedUrlPath);
		}

		return resolvedUrlPath;
	}

}
