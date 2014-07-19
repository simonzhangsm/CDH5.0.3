package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.BlockLocalPathInfo;
import org.apache.hadoop.hdfs.protocol.ClientDatanodeProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.security.UserGroupInformation;

//Stores the cache and proxy for a local datanode.
public final class LocalDatanodeInfo {
	private static final Log LOG = LogFactory.getLog(LocalDatanodeInfo.class);
	private ClientDatanodeProtocol proxy = null;
	private final Map<ExtendedBlock, BlockLocalPathInfo> cache;

	LocalDatanodeInfo() {
		final int cacheSize = 10000;
		final float hashTableLoadFactor = 0.75f;
		int hashTableCapacity = (int) Math
				.ceil(cacheSize / hashTableLoadFactor) + 1;
		cache = Collections
				.synchronizedMap(new LinkedHashMap<ExtendedBlock, BlockLocalPathInfo>(
						hashTableCapacity, hashTableLoadFactor, true) {
					private static final long serialVersionUID = 1;

					@Override
					protected boolean removeEldestEntry(
							Map.Entry<ExtendedBlock, BlockLocalPathInfo> eldest) {
						return size() > cacheSize;
					}
				});
	}

	public synchronized ClientDatanodeProtocol getDatanodeProxy(
			UserGroupInformation ugi, final DatanodeInfo node,
			final Configuration conf, final int socketTimeout,
			final boolean connectToDnViaHostname) throws IOException {
		if (proxy == null) {
			try {
				proxy = ugi
						.doAs(new PrivilegedExceptionAction<ClientDatanodeProtocol>() {
							@Override
							public ClientDatanodeProtocol run()
									throws Exception {
								return DFSUtil
										.createClientDatanodeProtocolProxy(
												node, conf, socketTimeout,
												connectToDnViaHostname);
							}
						});
			} catch (InterruptedException e) {
				LOG.warn("encountered exception ", e);
			}
		}
		return proxy;
	}

	public synchronized void resetDatanodeProxy() {
		if (null != proxy) {
			RPC.stopProxy(proxy);
			proxy = null;
		}
	}

	public BlockLocalPathInfo getBlockLocalPathInfo(ExtendedBlock b) {
		return cache.get(b);
	}

	public void setBlockLocalPathInfo(ExtendedBlock b, BlockLocalPathInfo info) {
		cache.put(b, info);
	}

	public void removeBlockLocalPathInfo(ExtendedBlock b) {
		cache.remove(b);
	}
}