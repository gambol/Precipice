/*
 * Copyright 2014 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.samples.bigger;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

import java.util.ArrayList;
import java.util.List;

public class Servers {

    private List<Undertow> servers = new ArrayList<>();


    public Servers() {
        servers.add(create(6001, new ServerHandler("Weather-1")));
        servers.add(create(7001, new ServerHandler("Weather-2")));
    }

    public void start() {
        for (Undertow server : servers) {
            server.start();
        }
    }

    public void stop() {
        for (Undertow server : servers) {
            server.stop();
        }
    }

    private Undertow create(int port, HttpHandler handler) {
        return Undertow.builder().addHttpListener(port, "127.0.0.1").setHandler(handler).build();
    }
}
