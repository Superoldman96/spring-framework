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

package org.springframework.context.event.test;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * A simple POJO that implements {@link ResolvableTypeProvider}.
 *
 * @author Stephane Nicoll
 */
public class GenericEventPojo<T> implements ResolvableTypeProvider {
	private final T value;

	public GenericEventPojo(T value) {
		this.value = value;
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(this.value));
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		GenericEventPojo<?> that = (GenericEventPojo<?>) o;

		return this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}
}
