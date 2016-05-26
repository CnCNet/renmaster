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

import java.nio.BufferOverflowException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class RenMaster {

    private static AvailableServer available;

    public static AvailableServer getAvailableServer() {
        return available;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Selector selector = Selector.open();
            available = new AvailableServer("0.0.0.0", 27900);
            available.register(selector);
            MasterServer master = new MasterServer("0.0.0.0", 28900);
            master.register(selector);

            long lastCleanup = 0;

            while (true) {
                if (selector.select() > 0) {

                    long now = System.currentTimeMillis();

                    if (now - lastCleanup > 10000) {
                        available.cleanup();
                    }

                    for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
                        SelectionKey k = i.next();
                        SocketEvent se = (SocketEvent)k.attachment();

                        try {
                            if (k.isReadable())
                                se.canRead();
                        } catch (Exception e) {
                            e.printStackTrace();
                            k.cancel();
                            i.remove();
                            continue;
                        }

                        if (k.channel().isOpen()) {
                            if (k.isAcceptable())
                                se.canAccept();

                            if (k.isConnectable())
                                se.canConnect();

                            if (k.isWritable())
                                se.canWrite();
                        }

                        if (!k.isValid() || !k.channel().isOpen())
                            se.onClose();

                        if (!k.channel().isOpen()) {
                            k.cancel();
                        } else {
                            k.channel().register(selector, se.getOps());
                            k.attach(se);
                        }

                        i.remove();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void dmp(String hdr, byte[] a) {
        dmp(hdr, a, a.length);
    }

    static void dmp(String hdr, byte[] a, int len) {
        System.out.printf(hdr + ": ");
        if (a == null) {
            System.out.printf(" (null)\n");
            return;
        }
        for (int i = 0; i < len; i++) {
           System.out.printf("%02X ", a[i]);
        }
        System.out.printf("(%d bytes)\n", a.length);
    }

    static public Map<String, String> parseQuery(String query) {
        while (query.length() > 1 && (query.endsWith("\u0000") || query.endsWith("\n") || query.endsWith("\r")))
            query = query.substring(0, query.length() - 1);

        HashMap<String, String> ret = new HashMap<String, String>();
        String[] parts = query.split("\\\\");

        String key = null;
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                ret.put(key, parts[i]);
            } else {
                key = parts[i];
            }
        }

        return ret;
    }
}
