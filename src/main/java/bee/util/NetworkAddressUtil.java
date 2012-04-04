/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import kiss.I;

/**
 * @version 2012/04/05 1:26:08
 */
public final class NetworkAddressUtil {

    /** The port manager. */
    protected static final TimeWaitObserver OBSERVER = new TimeWaitObserver();

    /** The port range setting. */
    private static final int MIN_PORT = 1025;

    /** The port range setting. */
    private static final int MAX_PORT = 65535;

    /**
     * Select the suitable address for the given remote address.
     * 
     * @param local A local address.
     * @param remote A remote address.
     * @return A suitable local address.
     */
    public static InetAddress getExposedAddress(InetAddress local, InetAddress remote) {
        // assert null
        if (local == null) {
            throw new IllegalArgumentException("The local host is null.");
        }

        if (remote == null) {
            throw new IllegalArgumentException("The remote host is null.");
        }

        // check local address
        if (local.equals(remote) || isLocalAddress(remote)) {
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw I.quiet(e);
            }
        } else {
            return local;
        }
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of size 4 and returns
     * this as a long representation of the same IP address.
     * 
     * @param socket The local socket.
     * @return A long representation of the IP address.
     */
    public static long getIPAsLong(Socket socket) {
        return getIPAsLong(socket.getLocalAddress().getAddress());
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of size 4 and returns
     * this as a long representation of the same IP address.
     * 
     * @param socket The server socket.
     * @return A long representation of the IP address.
     */
    public static long getIPAsLong(ServerSocket socket) {
        return getIPAsLong(socket.getInetAddress());
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of size 4 and returns
     * this as a long representation of the same IP address.
     * 
     * @param address The local address.
     * @return A long representation of the IP address.
     */
    public static long getIPAsLong(InetAddress address) {
        return getIPAsLong(address.getAddress());
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of size 4 and returns
     * this as a long representation of the same IP address.
     * 
     * @param address The local address.
     * @return A long representation of the IP address.
     */
    public static long getIPAsLong(InetSocketAddress address) {
        return getIPAsLong(address.getAddress());
    }

    /**
     * A convenient method that accepts an IP address represented by a byte[] of size 4 and returns
     * this as a long representation of the same IP address.
     * 
     * @param address The byte[] of size 4 representing the IP address.
     * @return A long representation of the IP address.
     */
    public static long getIPAsLong(byte[] address) {
        // check address
        if (address.length != 4) {
            throw new IllegalArgumentException("The given byte array must be of length 4");
        }

        long ip = 0;
        long multiplier = 1;

        for (int i = 3; i >= 0; i--) {
            int byteValue = (address[i] + 256) % 256;
            ip += byteValue * multiplier;
            multiplier *= 256;
        }
        return ip;
    }

    /**
     * Gets the next available port number. If there is no available port, return 0.
     * 
     * @return An available port number or 0.
     */
    public static int getPort() {
        return getPort(MIN_PORT, MAX_PORT);
    }

    /**
     * Gets the next available port number. If there is no available port, return 0.
     * 
     * @param startPort A start port to scan for availability.
     * @return An available port number or 0.
     */
    public static int getPort(int startPort) {
        return getPort(startPort, MAX_PORT);
    }

    /**
     * Gets the next available port number. If there is no available port, return 0.
     * 
     * @param startPort A start port to scan for availability.
     * @param endPort A end port to scan for availability.
     * @return An available port number or 0.
     */
    public synchronized static int getPort(int startPort, int endPort) {
        // check port range
        if (startPort < MIN_PORT) {
            startPort = MIN_PORT;
        }

        if (MAX_PORT < endPort) {
            endPort = MAX_PORT;
        }

        // assert order
        if (startPort > endPort) {
            int t = startPort;
            endPort = startPort;
            startPort = t;
        }

        // scan port
        for (int i = startPort; i <= endPort; i++) {
            ServerSocket socket = null;

            try {
                socket = new ServerSocket(i);
                socket.setReuseAddress(true);

                if (OBSERVER.isAvailable(i)) {
                    return i;
                }
            } catch (IOException e) {
                // do nothing
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Cheke whether the given address is local address or not.
     * 
     * @param address A network address.
     * @return A result.
     */
    public static boolean isLocalAddress(InetAddress address) {
        // assert null
        if (address == null) {
            return false;
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();

                    if (inetAddress.equals(address)) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            // do nothing
        }
        return false;
    }

    /**
     * Check whether the given port number is in valid range or not. This method don't check the
     * availability.
     * 
     * @param port A port number.
     * @return A result.
     */
    public static boolean isValidPort(int port) {
        return 1 <= port && port <= MAX_PORT;
    }

    /**
     * This class can pseudoly observe TIME_WAIT socket.
     * 
     * @version 2010/02/24 9:47:46
     */
    protected static class TimeWaitObserver {

        /** The default TIME_WAIT time. (3 minutes) */
        private static final long WAIT_TIME = 3 * 60 * 1000L;

        /** The hisory of port usage. */
        private final List<TimeWait> history = new ArrayList<TimeWait>();

        /**
         * Check whether this given port number is available or not.
         * 
         * @param number A target port number.
         * @return A result.
         */
        protected boolean isAvailable(int number) {
            long current = System.currentTimeMillis();
            Iterator<TimeWait> iterator = history.iterator();

            while (iterator.hasNext()) {
                TimeWait wait = iterator.next();

                // Check wait time.
                // Last used time is equal to the initial value, it means that the port is active.
                if (wait.lastUsedTime != -1 && WAIT_TIME < current - wait.lastUsedTime) {
                    // this port pass over the wait time, so you may reuse it
                    iterator.remove();
                    continue;
                }

                // check port number
                if (number == wait.number) {
                    return false;
                }
            }

            // the given port is available
            history.add(new TimeWait(number));

            // API definition
            return true;
        }

        /**
         * Release the given port number.
         * 
         * @param number A target port number.
         */
        protected void release(int number) {
            for (TimeWait wait : history) {
                if (wait.number == number) {
                    wait.lastUsedTime = System.currentTimeMillis();
                    return;
                }
            }
        }

        /**
         * @version 2010/02/24 9:47:35
         */
        private class TimeWait {

            /** The port number. */
            private final int number;

            /** The last used time. */
            private long lastUsedTime = -1;

            /**
             * Create TimeWait instance.
             * 
             * @param number A port number.
             */
            private TimeWait(int number) {
                this.number = number;
            }
        }
    }
}
