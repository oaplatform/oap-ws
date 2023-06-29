/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.ws.account.utils;

import de.taimos.totp.TOTP;
import oap.ws.account.User;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TfaUtils {

    public static String getGoogleAuthenticatorCode( User user ) {
        return "otpauth://totp/"
            + URLEncoder.encode( user.organizationName() + ":" + user.email, UTF_8 ).replace( "+", "%20" )
            + "?secret=" + URLEncoder.encode( user.secretKey, UTF_8 ).replace( "+", "%20" )
            + "&issuer=" + URLEncoder.encode( user.organizationName(), UTF_8 ).replace( "+", "%20" );
    }

    public static String getTOTPCode( String secretKey ) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode( secretKey );
        String hexKey = Hex.encodeHexString( bytes );
        return TOTP.getOTP( hexKey );
    }
}
