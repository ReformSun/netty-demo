/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sunny.flink.io;


import com.sunny.flink.ssl.SSLHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetAddress;

import static com.sunny.util.Preconditions.checkArgument;
import static com.sunny.util.Preconditions.checkNotNull;

public class NettyConfig {

	private static final Logger LOG = LoggerFactory.getLogger(NettyConfig.class);


	// - Config keys ---------------------------------------------------------


	public enum TransportType {
		NIO, EPOLL, AUTO
	}

	public static final String SERVER_THREAD_GROUP_NAME = "Flink Netty Server";

	public static final String CLIENT_THREAD_GROUP_NAME = "Flink Netty Client";

	private final InetAddress serverAddress;

	private final int serverPort;

	private final int memorySegmentSize;

	private final int numberOfSlots;
	private String transportType;


	public NettyConfig(
			InetAddress serverAddress,
			int serverPort,
			int memorySegmentSize,
			int numberOfSlots,String transportType) {

		this.serverAddress = checkNotNull(serverAddress);

		checkArgument(serverPort >= 0 && serverPort <= 65536, "Invalid port number.");
		this.serverPort = serverPort;

		checkArgument(memorySegmentSize > 0, "Invalid memory segment size.");
		this.memorySegmentSize = memorySegmentSize;

		checkArgument(numberOfSlots > 0, "Number of slots");
		this.numberOfSlots = numberOfSlots;
		this.transportType = transportType;


		LOG.info(this.toString());
	}

	public InetAddress getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public int getMemorySegmentSize() {
		return memorySegmentSize;
	}

	public int getNumberOfSlots() {
		return numberOfSlots;
	}

	// ------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------

	public int getServerConnectBacklog() {
		return 1;
	}

	public int getNumberOfArenas() {
		// default: number of slots
		return 1;
	}

	public int getServerNumThreads() {
		return 1;
	}

	public int getClientNumThreads() {

		return 1;
	}

	public int getClientConnectTimeoutSeconds() {
		return 10;
	}

	public int getSendAndReceiveBufferSize() {
		return 1000;
	}

	public SSLHandlerFactory createClientSSLEngineFactory() throws Exception {
		return null;
	}
	public SSLHandlerFactory createServerSSLEngineFactory() throws Exception {
		return null;
	}

	public TransportType getTransportType() {
		switch (transportType) {
			case "nio":
				return TransportType.NIO;
			case "epoll":
				return TransportType.EPOLL;
			default:
				return TransportType.AUTO;
		}
	}

}
