/*
 * Copyright © 2015 Mingming Chen and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.latency.collection;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.latency.echo.EchoMsg;
import org.opendaylight.latency.impl.LatencyProvider;
import org.opendaylight.latency.ping.Ping;
import org.opendaylight.latency.util.InventoryUtil;
import org.opendaylight.latency.util.LatencyPacketUtil;
import org.opendaylight.latency.util.LatencyUtil;
import org.opendaylight.latency.util.TopologyUtil;
import org.opendaylight.openflowplugin.applications.lldpspeaker.LLDPSpeaker;
import org.opendaylight.openflowplugin.applications.lldpspeaker.NodeConnectorEventsObserver;
import org.opendaylight.openflowplugin.applications.topology.lldp.utils.LLDPDiscoveryUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.latency.rev150105.NetworkLatencyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.applications.lldp.speaker.rev141023.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

public class NetworkLatency implements LatencyRepo {
	private static final Logger LOG = LoggerFactory.getLogger(NetworkLatency.class);
	public static Map<NodeConnectorRef, Long> pktOutTimeMap = new ConcurrentHashMap<>();
	private PacketProcessingService packetProcessingService;
	private DataBroker dataBroker;
	public static String TOPO_ID = "flow:1";
	private ListenerRegistration<NotificationListener> notificationListnerReg;
	public ListenableFuture<RpcResult<java.lang.Void>> listenablefuture;
	public Future<RpcResult<Void>> futureSend;
	private ListenerRegistration<NotificationListener> nlReg;
	private NotificationProviderService nps;
	private NetworkLatency nl;
	
	
	
	public NetworkLatency(PacketProcessingService pps,
			DataBroker dataBroker) {
		this.packetProcessingService = pps;
		this.dataBroker = dataBroker;
		pktOutTimeMap.clear();
		
	}

	@Override
	public Future<RpcResult<java.lang.Void>> execute() throws Exception {
		LOG.info("NetworkLatency is running");
		InstanceIdentifier<Topology> topoIId = TopologyUtil.createTopoIId(TOPO_ID);
		Topology topo = (Topology) TopologyUtil.readTopo(topoIId,dataBroker);
		List<Link> linkList = topo.getLink();
		for(Link link : linkList) {
			//src
			NodeId srcNodeId = new NodeId(link.getSource().getSourceNode().getValue());
			NodeRef srcNodeRef = InventoryUtil.getNodeRefFromNodeId(srcNodeId);
			FlowCapableNode srcflowCapableNode = (FlowCapableNode) InventoryUtil.readFlowCapableNodeFromNodeId(srcNodeId, dataBroker);
			IpAddress srcipAddress = srcflowCapableNode.getIpAddress();
			NodeConnectorRef srcNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getSource().getSourceTp());
			NodeConnectorId srcnodeConnectorId = InventoryUtil.getNodeConnectorIdFromNodeConnectorRef(srcNCRef);
			InstanceIdentifier<NodeConnector> srcncIId = (InstanceIdentifier<NodeConnector>) srcNCRef.getValue();
			FlowCapableNodeConnector srcflowCapableNodeConnector = (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(srcncIId, dataBroker);
			MacAddress srcMac = srcflowCapableNodeConnector.getHardwareAddress();
			Long srcPortNo = srcflowCapableNodeConnector.getPortNumber().getUint32();
			
			//dst
			NodeConnectorRef dstNCRef = TopologyUtil.getNodeConnectorRefFromTpId(link.getDestination().getDestTp());
			InstanceIdentifier<NodeConnector> dstncIId = (InstanceIdentifier<NodeConnector>) dstNCRef.getValue();
			FlowCapableNodeConnector dstflowCapableNodeConnector= (FlowCapableNodeConnector) InventoryUtil.readFlowCapableNodeConnectorFromNodeConnectorIId(dstncIId, dataBroker);
			MacAddress dstMac = dstflowCapableNodeConnector.getHardwareAddress();;
			
			//pktout
			byte[] srcpayload = LatencyPacketUtil.buildLldpFrame(srcNodeId, srcnodeConnectorId, srcMac, srcPortNo, dstMac);
			TransmitPacketInput srclldppkt = LatencyUtil.createPacketOut(srcpayload, srcNodeRef, srcNCRef);	
			Date srcdate = new Date();
 		    Long srcpktOutTime = srcdate.getTime(); 
 		    pktOutTimeMap.put(srcNCRef, srcpktOutTime);
			futureSend = packetProcessingService.transmitPacket(srclldppkt);
	    
            
            LOG.info("size in pktout is " + pktOutTimeMap.size());	
			
		}

		return futureSend;
		
	}
	

	@Override
	public Long getCSLatency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getSSLatency() {
		// TODO Auto-generated method stub
		return null;
	}









}
