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

package oap.ws.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.joda.time.DateTime;

import java.io.Serializable;

@ToString
@EqualsAndHashCode
public class Authentication implements Serializable {
    private static final long serialVersionUID = -2221117654361445000L;
    public final String id;
    public final User user;
    public DateTime created;
    public boolean tfaEnabled = false;

    public Authentication( String id, User user ) {
        this.id = id;
        this.user = user;
        this.created = new DateTime();
    }

    public Authentication( String id, User user, boolean tfaEnabled ) {
        this( id, user );
        this.tfaEnabled = tfaEnabled;
    }

    @JsonIgnore
    public View view = new View();

    public class View implements Serializable {
        public String getId() {
            return id;
        }

        public DateTime getCreated() {
            return created;
        }

        public User.View getUser() {
            return user != null ? user.getView() : null;
        }

        public boolean isTfaEnabled() {
            return tfaEnabled;
        }
    }
}
