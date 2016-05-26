/*
 * Copyright (c) 2013 Toni Spets <toni.spets@iki.fi>
 * 
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package gsmaster;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class AvailableServer implements SocketEvent {

    private DatagramChannel channel;
    private ByteBuffer buf;
    private HashMap<InetSocketAddress, Long> servers;

    public AvailableServer(String host, int port) throws IOException {
       channel = DatagramChannel.open();
       channel.configureBlocking(false);
       channel.bind(new InetSocketAddress(host, port));
       buf = ByteBuffer.allocate(1024);
       servers = new HashMap<InetSocketAddress, Long>();
    }

    public void register(Selector selector) throws IOException {
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        key.attach(this);
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    @Override
    public void canAccept() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void canConnect() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void canRead() throws IOException {
        buf.clear();
        InetSocketAddress from = (InetSocketAddress)channel.receive(buf);

        System.out.println("AvailableServer: Read " + buf.position() + " bytes from " + from.toString());
        buf.flip();

        try {
            byte[] packet = new byte[buf.limit()];
            buf.get(packet);

            String strPacket = new String(packet, 0, packet.length, "US-ASCII");
            System.out.println(strPacket);

            Map<String, String> keys = RenMaster.parseQuery(strPacket);

            String port = keys.get("heartbeat");
            String gamename = keys.get("gamename");

            if (port != null && gamename != null && gamename.equals("ccrenegade")) {
                    InetSocketAddress server = new InetSocketAddress(from.getAddress(), Integer.parseInt(port, 10));
                    servers.put(server, System.currentTimeMillis());
            }
        } catch (Exception e) {}
    }

    @Override
    public void canWrite() throws IOException {
    }

    @Override
    public void onClose() throws IOException {
    }

    @Override
    public int getOps() {
        return SelectionKey.OP_READ;
    }
    

    public Set<InetSocketAddress> getServers() {
        return servers.keySet();
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<InetSocketAddress, Long>> it = servers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<InetSocketAddress, Long> entry = it.next();
            if (entry.getValue() + 600000 < now) {
                it.remove();
            }
        }
    }
}
