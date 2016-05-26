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

/**
 *
 * @author Toni Spets <toni.spets@iki.fi>
 */
public class EnctypeX {

    private EnctypeX() { }

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

    static void funcx(byte[] encxkey, byte[] key, byte[] encxvalidate, byte[] data, int datalen) {
        int i, keylen;

        keylen = key.length;

        for (i = 0; i < datalen; i++) {
            encxvalidate[(key[i % keylen] * i) & 7] ^= encxvalidate[i & 7] ^ data[i];
        }

        func4(encxkey, encxvalidate, 8);
    }

    static void func4(byte[] encxkey, byte[] id, int idlen) {
        int i;
        MutableInteger n1 = new MutableInteger(0), n2 = new MutableInteger(0);
        int t1, t2;

        if (idlen < 1) return;

        for (i = 0; i < 256; i++) encxkey[i] = (byte)(i & 0xFF);

        for (i = 255; i >= 0; i--) {
            t1 = func5(encxkey, i, id, idlen, n1, n2);
            t2 = encxkey[i] & 0xFF;
            encxkey[i] = encxkey[t1];
            encxkey[t1] = (byte)(t2 & 0xFF);
        }

        encxkey[256] = encxkey[1];
        encxkey[257] = encxkey[3];
        encxkey[258] = encxkey[5];
        encxkey[259] = encxkey[7];
        encxkey[260] = encxkey[n1.getValue() & 0xFF];
    }

    static int func5(byte[] encxkey, int cnt, byte[] id, int idlen, MutableInteger mn1, MutableInteger mn2) {
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
            n1 = (encxkey[n1 & 0xFF] & 0xFF) + (id[n2] & 0xFF);
            n2++;
            if (n2 >= idlen) {
                n2 = 0;
                n1 += idlen;
            }
            tmp = n1 & mask;
            if (++i > 11) tmp %= cnt;
        } while(tmp > cnt);

        mn1.setValue(n1);
        mn2.setValue(n2);

        return tmp;
    }

    static byte func7e(byte[] encxkey, byte d) {
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

    static byte[] encode(byte[] key, byte[] validate, byte[] data) {
        byte[] encxkey = new byte[261];
        int a,b;
        byte[] encxvalidate = new byte[8];

        if (data.length < 1)
            throw new IllegalArgumentException("Data is not long enough");

        a = ((data[0] & 0xFF) ^ 0xEC) + 2;

        if (data.length < a)
            throw new IllegalArgumentException("Data is not long enough");

        b = (data[a - 1] & 0xFF) ^ 0xEA;

        if (data.length < (a + b))
            throw new IllegalArgumentException("Data is not long enough");

        System.arraycopy(validate, 0, encxvalidate, 0, 8);

        byte[] data2 = new byte[data.length - a];
        System.arraycopy(data, a, data2, 0, b);
        dmp("funcx data", data2);
        funcx(encxkey, key, validate, data2, b);
        a += b;

        byte[] ret = new byte[data.length - a];
        System.arraycopy(data, a, ret, 0, data.length - a);

        for (int i = 0; i < ret.length; i++) {
            ret[i] = func7e(encxkey, ret[i]); 
        }

        System.arraycopy(ret, 0, data, a, data.length - a);

        return data;
    }

    static byte[] encrypt(byte[] key, byte[] validate, byte[] data) {
        int i,rnd,keylen,vallen;
        int size = data.length;
        byte[] tmp = new byte[23];

        keylen = key.length;
        vallen = validate.length;
        //rnd = ~(int)(System.currentTimeMillis() / 1000);
        rnd = 0;

        for (i = 0; i < tmp.length; i++) {
            rnd = (rnd * 0x343FD) + 0x269EC3;
            tmp[i] = (byte)(rnd ^ key[i % keylen] ^ validate[i % vallen]);
        }

        tmp[0] = (byte)(0xEB & 0xFF);
        tmp[1] = 0x00;
        tmp[2] = 0x00;
        tmp[8] = (byte)(0xE4 & 0xFF);

        byte[] xdata = new byte[data.length + 23];
        for (i = size - 1; i >= 0; i--) {
            xdata[tmp.length + i] = data[i];
        }
        System.arraycopy(tmp, 0, xdata, 0, tmp.length);

        return encode(key, validate, xdata);
    }
}
