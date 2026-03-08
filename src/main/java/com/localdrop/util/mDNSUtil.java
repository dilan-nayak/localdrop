// mDNSUtil.java (placeholder for future device discovery)
package com.localdrop.util;

/**
 * 🔍 Purpose:
 * Placeholder utility for implementing mDNS (Multicast DNS) based device discovery in the future.
 * This class is intended to allow devices on the same LAN (local Wi-Fi network) to discover each other
 * without manually entering IP addresses — similar to how Apple's AirDrop or Chromecast works.
 *
 * 🧠 What is mDNS?
 * - mDNS (Multicast DNS) allows devices to broadcast their presence on the network
 * - It works without requiring a central DNS server
 * - Example tool: JmDNS (Java Multicast DNS library)
 *
 * 🚀 Future Implementation Ideas:
 * - Use JmDNS to advertise a service like `_localdrop._tcp.local`
 * - Discover other devices broadcasting the same service
 * - Automatically connect peers without user input
 *
 * 🔧 Technologies to Explore:
 * - [JmDNS](https://github.com/jmdns/jmdns)
 * - `javax.jmdns.ServiceInfo` and `ServiceListener`
 * - Background threads for peer updates
 */

public class mDNSUtil
{
    // Device discovery feature can use jmdns or similar libraries for LAN peer listing
    // Placeholder for future implementation
}
