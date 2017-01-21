/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.util.concurrent.Future;

import org.opendaylight.yangtools.yang.common.RpcResult;

public interface LatencyRepo {
	
	public Long getCSLatency();
	public Long getSSLatency();
	public Future<RpcResult<Void>> execute() throws Exception;
	
}
