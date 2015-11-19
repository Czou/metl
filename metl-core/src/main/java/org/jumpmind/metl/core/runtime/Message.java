/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;
    
    MessageHeader header;

    Serializable payload;
    
    public Message(String originatingStepId, Serializable payload) {
        this(originatingStepId);
        this.payload = payload;
    }
    
    public Message(String originatingStepId) {
        this.header = new MessageHeader(originatingStepId);
    }

    public MessageHeader getHeader() {
        return header;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getPayload() {
        return (T)payload;
    }

    public <T extends Serializable> void setPayload(T payload) {
        this.payload = payload;
    }
    
    public String getTextFromPayload() {
        StringBuilder b = new StringBuilder();
        if (payload instanceof Collection) {
            Iterator<?> i = ((Collection<?>)payload).iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                b.append(obj);
                if (i.hasNext()) {
                    b.append(System.getProperty("line.separator"));
                }
            }
        } else if (payload instanceof CharSequence) {
            b.append((CharSequence)payload);
        }
        return b.toString();
    }
    
}
