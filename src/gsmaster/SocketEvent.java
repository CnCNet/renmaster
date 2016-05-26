/*
 * Copyright (c) 2012 Toni Spets <toni.spets@iki.fi>
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

/**
 *
 * @author Toni Spets
 */
public interface SocketEvent {
    /**
     * Called on OP_ACCEPT event
     * @throws IOException 
     */
    public void canAccept() throws IOException;

    /**
     * Called on OP_CONNECT event
     * @throws IOException 
     */
    public void canConnect() throws IOException;

    /**
     * Called on OP_READ event
     * @throws IOException 
     */
    public void canRead() throws IOException;

    /**
     * Called on OP_WRITE event
     * @throws IOException 
     */
    public void canWrite() throws IOException;

    /**
     * Called when the channel is to be closed, last chance to do cleanup
     */
    public void onClose() throws IOException;

    public int getOps();
}
