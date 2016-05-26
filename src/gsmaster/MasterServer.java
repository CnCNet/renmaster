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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class MasterServer implements SocketEvent {

    ServerSocketChannel channel;
    Selector selector;

    public MasterServer(String host, int port) throws IOException {
       channel = ServerSocketChannel.open();
       channel.configureBlocking(false);
       channel.bind(new InetSocketAddress(host, port));
    }

    @Override
    public void canAccept() throws IOException {
        System.out.println("MasterServer: Accepting new client.");
        SocketChannel clientChannel = channel.accept();
        clientChannel.configureBlocking(false);
        SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
        key.attach(new MasterClient(clientChannel));
    }

    public void register(Selector selector) throws IOException {
        SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(this);
        this.selector = selector;
    }

    @Override
    public void canConnect() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void canRead() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void canWrite() throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void onClose() throws IOException {
    }

    @Override
    public int getOps() {
        return SelectionKey.OP_ACCEPT;
    }
    
}
