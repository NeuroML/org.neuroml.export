package org.neuroml.export.info.model.pairs;

import java.util.Iterator;
import java.util.List;

public class IterablePair<T1, T2> implements Iterable<Pair<T1, T2>>
{
	private final List<T1> first;
	private final List<T2> second;

	public IterablePair(List<T1> first, List<T2> second)
	{
		this.first = first;
		this.second = second;
	}

	@Override
	public Iterator<Pair<T1, T2>> iterator()
	{
		return new ParallelIterator<T1, T2>(first.iterator(), second.iterator());
	}
}