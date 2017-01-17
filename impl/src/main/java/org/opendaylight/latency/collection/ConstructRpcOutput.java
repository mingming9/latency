/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.network.latency.output.Pairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.network.latency.output.PairsBuilder;

public class ConstructRpcOutput {

	private List<NodeConnectorRef> srclist;
	private List<NodeConnectorRef> dstlist;
	private List<Long> timelist;
	private Map<String, Long> timestore = new ConcurrentHashMap<>();
	public ConstructRpcOutput() {
		// TODO Auto-generated constructor stub
	}
	
	public ConstructRpcOutput(List<NodeConnectorRef> src, List<NodeConnectorRef> dst, List<Long> time) {
		this.srclist = src;
		this.dstlist = dst;
		this.timelist = time;
	}



	public NetworkLatencyOutput getSwSwLatencyOutput() {
		compressToMap(srclist, dstlist, timelist);
		NetworkLatencyOutputBuilder networkLatencyOutputBuilder = new NetworkLatencyOutputBuilder();
		List<Pairs> pairsList = new ArrayList<Pairs>();
		//List<Long> dpIpList = new ArrayList<Long>();
		Set<String> keys = timestore.keySet();
		Iterator it = keys.iterator();
		while(it.hasNext()) {
			//extract data
			String key = (String) it.next();
			String[] keysplit = key.split(";");
			BigInteger srcdpId = new BigInteger(keysplit[0]);
			BigInteger dstdpId = new BigInteger(keysplit[1]);
			Long latencyTime = timestore.get(key);
			//construct 
			PairsBuilder pair = new PairsBuilder();
			pair.setADpId(srcdpId);
			pair.setBDpId(dstdpId);
			pair.setLatencyTime(latencyTime);
			Pairs pairs = pair.build();
			pairsList.add(pairs);	
		}
		return networkLatencyOutputBuilder.setPairs(pairsList).build();
	}

	private void compressToMap(List<NodeConnectorRef> srclist2,
			List<NodeConnectorRef> dstlist2, List<Long> timelist2) {
		int lensrc = srclist2.size();
		int lendst = dstlist2.size();
		int lentime = timelist2.size();
		for(int i = 0; i < lensrc; i++) {
			NodeConnectorRef src = srclist2.get(i);
			for(int j = i + 1; j < lendst; j++) {
				NodeConnectorRef dst = dstlist2.get(j);
				if (src.equals(dst)) {
					String mapkey = constructkey(src, dstlist2.get(i));
					Long time = (timelist2.get(i) + timelist2.get(j))/2;
					timestore.put(mapkey, time);
					srclist2.remove(j);
					dstlist2.remove(j);
					timelist2.remove(j);
					lensrc = srclist2.size();
					lendst = dstlist2.size();
					lentime = timelist2.size();
				}
			}
		}
		System.out.println("compress to map result is " + timestore);
		
	}

	private String constructkey(NodeConnectorRef src,
			NodeConnectorRef dst) {
		BigInteger srcdpId = LatencyUtil.getDpId(InventoryUtil.getNodeIdFromNodeConnectorRef(src));
		BigInteger dstdpId = LatencyUtil.getDpId(InventoryUtil.getNodeIdFromNodeConnectorRef(dst));
		String key = srcdpId.toString() + ";" + dstdpId.toString();
		return key;
	}
	

}
