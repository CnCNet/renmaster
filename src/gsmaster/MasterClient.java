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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class MasterClient implements SocketEvent {

    private SocketChannel channel;
    private ByteBuffer inbuf;
    private ByteBuffer outbuf;
    private boolean closeAfterWrite = false;

    public MasterClient(SocketChannel channel) throws UnsupportedEncodingException {
        this.channel = channel;
        inbuf = ByteBuffer.allocate(4096);
        outbuf = ByteBuffer.allocate(65536);
        outbuf.put("\\basic\\\\secure\\IGNORE".getBytes("US-ASCII"));
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
        inbuf.clear();
        channel.read(inbuf);

        if (inbuf.position() == 0) {
            channel.close();
            return;
        }

        System.out.println("MasterClient: Read " + inbuf.position() + " bytes.");
        inbuf.flip();

        byte[] packet = new byte[inbuf.limit()];
        inbuf.get(packet);

        String strPacket = new String(packet, 0, packet.length, "US-ASCII");
        Map<String, String> keys = RenMaster.parseQuery(strPacket);

        String gamename = keys.get("gamename");
        String enctype = keys.get("enctype");

        if (enctype != null && enctype.equals("0") && gamename != null && gamename.equals("ccrenegade")) {

            AvailableServer available = RenMaster.getAvailableServer();

            System.out.println("Returning " + available.getServers().size() + " unique servers.");
            System.out.println("outbuf pos is at " + outbuf.position());

            for (InetSocketAddress server : available.getServers()) {
                byte[] address = server.getAddress().getAddress();
                for (byte b : address) {
                    outbuf.put(b);
                }
                outbuf.putShort((short)(server.getPort() & 0xFFFF));
                System.out.println("outbuf pos is now at " + outbuf.position() + " / " + outbuf.limit());
            }

            outbuf.put("\\final\\".getBytes("US-ASCII"));

            closeAfterWrite = true;
            System.out.println("Reply is " + outbuf.position() + " bytes.");
        } else {
            channel.close();
        }
    }

    @Override
    public void canWrite() throws IOException {
        if (outbuf.position() > 0) {
            outbuf.flip();
            channel.write(outbuf);
            outbuf.clear();

            if (closeAfterWrite)
                channel.close();
        }
    }

    @Override
    public void onClose() throws IOException {
        System.out.println("MasterClient closed.");
    }

    @Override
    public int getOps() {
        int ret = closeAfterWrite ? 0 : SelectionKey.OP_READ;

        if (outbuf.position() > 0) {
            ret |= SelectionKey.OP_WRITE;
        }

        return ret;
    }
}
