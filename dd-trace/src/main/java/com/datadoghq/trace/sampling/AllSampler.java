package com.datadoghq.trace.sampling;

import com.datadoghq.trace.DDBaseSpan;

/**
 * Sampler that always says yes...
 */
public class AllSampler extends AbstractSampler {

	@Override
	public boolean doSample(DDBaseSpan<?> span) {
		return true;
	}

}
