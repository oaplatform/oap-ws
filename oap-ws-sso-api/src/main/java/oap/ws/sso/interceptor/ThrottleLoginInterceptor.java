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

package oap.ws.sso.interceptor;

import lombok.extern.slf4j.Slf4j;
import oap.http.HttpResponse;
import oap.http.Request;
import oap.reflect.Reflection;
import oap.ws.Session;
import oap.ws.interceptor.Interceptor;
import oap.ws.sso.Authenticator;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

/**
 * The main purpose of ThrottleLoginInterceptor is to prevent users from brut-forcing our login endpoints
 */
@Slf4j
public class ThrottleLoginInterceptor implements Interceptor {
    private static final Integer DEFAULT = 5;

    private final Authenticator authenticator;
    private final ConcurrentHashMap<String, Temporal> attemptCache = new ConcurrentHashMap<>();
    private final Integer delay;

    public ThrottleLoginInterceptor( Authenticator authenticator, Integer delay ) {
        this.authenticator = authenticator;
        this.delay = delay;
    }

    public ThrottleLoginInterceptor( Authenticator authenticator ) {
        this.authenticator = authenticator;
        this.delay = DEFAULT;
    }

    @Override
    @Nonnull
    public Optional<HttpResponse> before( @Nonnull Request request, Session session, @Nonnull Reflection.Method method ) {
        var id = session.id;
        if( validateId( id ) ) {
            return Optional.empty();
        } else {
            if( log.isTraceEnabled() )
                log.trace( "Please wait {} seconds before next attempt", delay );
            return Optional.of( HttpResponse.status( HTTP_FORBIDDEN, "Please wait " + delay + " seconds before next attempt" ).response() );
        }
    }

    /**
     * Utility method for managing timeline between login attempts
     *
     * @param id user session id
     */
    private synchronized boolean validateId( String id ) {
        var now = LocalDateTime.now();
        var ts = attemptCache.putIfAbsent( id, now );
        if( ts != null ) {
            var duration = Duration.between( ts, now );
            if( duration.getSeconds() <= delay ) {
                if( log.isTraceEnabled() ) log.trace( "{} is too short period has passed since previous attempt", duration.getSeconds() );
                attemptCache.computeIfPresent( id, ( k, v ) -> now );
                return false;
            } else {
                if( log.isTraceEnabled() ) log.trace( "{} key has expired {}", id, duration.getSeconds() );
                attemptCache.remove( id );
                return true;
            }
        }
        return true;
    }
}
