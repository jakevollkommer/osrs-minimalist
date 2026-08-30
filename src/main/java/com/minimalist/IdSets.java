// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Building blocks for assembling hidden-ID sets from config toggles. */
public final class IdSets
{
	public static Set<Integer> toggled(boolean enabled, Set<Integer> curatedIds)
	{
		return enabled ? curatedIds : Set.of();
	}

	@SafeVarargs
	public static Set<Integer> union(Set<Integer>... sets)
	{
		return Stream.of(sets)
			.flatMap(Set::stream)
			.collect(Collectors.toUnmodifiableSet());
	}

	private IdSets()
	{
	}
}
