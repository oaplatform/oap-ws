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
import oap.http.HttpStatusCodes;
import oap.http.server.nio.HttpServerExchange;
import oap.reflect.Reflection;
import oap.ws.Session;
import oap.ws.interceptor.Interceptor;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.concurrent.ConcurrentHashMap;

import static oap.ws.sso.SSO.SESSION_USER_KEY;

/**
 * The main purpose of ThrottleLoginInterceptor is to prevent users from brut-forcing our login endpoints
 */
@Slf4j
public class ThrottleLoginInterceptor implements Interceptor {
    private static final Integer DEFAULT = 5;

    private final ConcurrentHashMap<String, Temporal> attemptCache = new ConcurrentHashMap<>();
    public Integer delay;

    /**
     * @param delay timeout between login attempt. In seconds
     */
    public ThrottleLoginInterceptor( Integer delay ) {
        this.delay = delay;
    }

    public ThrottleLoginInterceptor() {
        this.delay = DEFAULT;
    }

    @Override
    public boolean before( @Nonnull HttpServerExchange exchange, Session session, @Nonnull Reflection.Method method ) {
        var id = session.id;
        if( validateId( id ) || session.containsKey( SESSION_USER_KEY ) ) {
            return false;
        } else {
            if( log.isTraceEnabled() )
                log.trace( "Please wait {} seconds before next attempt", delay );
            exchange.setStatusCodeReasonPhrase( HttpStatusCodes.FORBIDDEN, "Please wait " + delay + " seconds before next attempt" );
            exchange.endExchange();
            return true;
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
            if( duration.getSeconds() <= this.delay ) {
                if( log.isTraceEnabled() )
                    log.trace( "{} is too short period has passed since previous attempt", duration.getSeconds() );
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
