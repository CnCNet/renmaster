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
/*
GS enctypeX servers list decoder/encoder 0.1.3
by Luigi Auriemma
e-mail: aluigi@autistici.org
web:    aluigi.org

This is the algorithm used by ANY new and old game which contacts the Gamespy master server.
It has been written for being used in gslist so there are no explanations or comments here,
if you want to understand something take a look to gslist.c

    Copyright 2008,2009 Luigi Auriemma

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

    http://www.gnu.org/licenses/gpl-2.0.txt
*/
package gsmaster;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a direct conversion of Luigi Auriemma's enctypex_decoder.c for
 * encoding parts.
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public final class EnctypeXOutputStream extends FilterOutputStream {

    private byte[] key;
    private byte[] validate;
    private byte[] encxkey;
    private OutputStream os;

    private EnctypeXOutputStream(OutputStream out) {
        super(out);
    }
    
    public EnctypeXOutputStream(OutputStream out, String key, String validate) throws IOException {
        super(out);

        this.os = out;
        this.key = key.getBytes("US-ASCII");
        this.validate = validate.getBytes("US-ASCII");
        this.encxkey = new byte[261];

        if (this.validate.length < 1)
            throw new IllegalArgumentException("Validate needs to be at least 1 byte.");

        // init
        byte[] header = new byte[23];
        int rnd = ~(int)(System.currentTimeMillis() / 1000);

        for (int i = 0; i < header.length; i++) {
            rnd = (rnd * 0x343FD) + 0x269EC3;
            header[i] = (byte)(rnd ^ this.key[i % this.key.length] ^ this.validate[i % this.validate.length]);
        }

        header[0] = (byte)(0xEB & 0xFF);
        header[1] = 0x00;
        header[2] = 0x00;
        header[8] = (byte)(0xE4 & 0xFF);

        // funcx
        for (int i = 0; i < 14; i++) {
            this.validate[(this.key[i % this.key.length] * i) & 7] ^= this.validate[i & 7] ^ header[9 + i];
        }

        // func4
        MutableInteger n1 = new MutableInteger(0), n2 = new MutableInteger(0);
        int t1, t2;

        for (int i = 0; i < 256; i++) {
            encxkey[i] = (byte)(i & 0xFF);
        }

        for (int i = 255; i >= 0; i--) {
            t1 = func5(i, n1, n2);
            t2 = encxkey[i] & 0xFF;
            encxkey[i] = encxkey[t1];
            encxkey[t1] = (byte)(t2 & 0xFF);
        }

        encxkey[256] = encxkey[1];
        encxkey[257] = encxkey[3];
        encxkey[258] = encxkey[5];
        encxkey[259] = encxkey[7];
        encxkey[260] = encxkey[n1.getValue() & 0xFF];

        os.write(header, 0, header.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] tmp = new byte[len];

        for (int i = 0; i < len; i++) {
            tmp[i] = func7e(b[off + i]); 
        }

        os.write(tmp, 0, len);
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte)b}, 0, 1);
    }

    private int func5(int cnt, MutableInteger mn1, MutableInteger mn2) {
        int i, tmp, mask = 1;
        int n1 = mn1.getValue(), n2 = mn2.getValue();

        if (cnt == 0) return 0;

        if (cnt > 1) {
            do {
                mask = (mask << 1) + 1;
            } while(mask < cnt);
        }

        i = 0;
        do {
            n1 = (encxkey[n1 & 0xFF] & 0xFF) + (validate[n2] & 0xFF);
            n2++;
            if (n2 >= validate.length) {
                n2 = 0;
                n1 += validate.length;
            }
            tmp = n1 & mask;
            if (++i > 11) tmp %= cnt;
        } while(tmp > cnt);

        mn1.setValue(n1);
        mn2.setValue(n2);

        return tmp;
    }

    private byte func7e(byte d) {
        byte a, b, c;

        a = encxkey[256];
        b = encxkey[257];
        c = encxkey[a & 0xFF];
        encxkey[256] = (byte)(a + 1);
        encxkey[257] = (byte)(b + c);
        a = encxkey[260];
        b = encxkey[257];
        b = encxkey[b & 0xFF];
        c = encxkey[a & 0xFF];
        encxkey[a & 0xFF] = b;
        a = encxkey[259];
        b = encxkey[257];
        a = encxkey[a & 0xFF];
        encxkey[b & 0xFF] = a;
        a = encxkey[256];
        b = encxkey[259];
        a = encxkey[a & 0xFF];
        encxkey[b & 0xFF] = a;
        a = encxkey[256];
        encxkey[a & 0xFF] = c;
        b = encxkey[258];
        a = encxkey[c & 0xFF];
        c = encxkey[259];
        b += a;
        encxkey[258] = b;
        a = b;
        c = encxkey[c & 0xFF];
        b = encxkey[257];
        b = encxkey[b & 0xFF];
        a = encxkey[a & 0xFF];
        c += b;
        b = encxkey[260];
        b = encxkey[b & 0xFF];
        c += b;
        b = encxkey[c & 0xFF];
        c = encxkey[256];
        c = encxkey[c & 0xFF];
        a += c;
        c = encxkey[b & 0xFF];
        b = encxkey[a & 0xFF];
        c ^= b ^ d;
        encxkey[260] = c;
        encxkey[259] = d;

        return c;
    }
}
