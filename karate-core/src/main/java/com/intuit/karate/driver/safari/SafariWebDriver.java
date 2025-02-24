/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.driver.safari;

import com.intuit.karate.FileUtils;
import com.intuit.karate.Http;
import com.intuit.karate.JsonUtils;
import com.intuit.karate.Logger;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.driver.DriverOptions;
import com.intuit.karate.shell.CommandThread;
import com.intuit.karate.driver.WebDriver;
import java.util.Map;

/**
 *
 * @author pthomas3
 */
public class SafariWebDriver extends WebDriver {

    public SafariWebDriver(DriverOptions options, CommandThread command, Http http, String sessionId, String windowId) {
        super(options, command, http, sessionId, windowId);
    }

    public static SafariWebDriver start(ScenarioContext context, Map<String, Object> map, Logger logger) {
        DriverOptions options = new DriverOptions(context, map, logger, 5555, "safaridriver");
        options.arg("--port=" + options.port);
        CommandThread command = options.startProcess();
        String urlBase = "http://" + options.host + ":" + options.port;
        Http http = Http.forUrl(options.driverLogger, urlBase);
        String sessionId = http.path("session")
                .post("{ capabilities: { browserName: 'Safari' } }")
                .jsonPath("get[0] response..sessionId").asString();
        options.driverLogger.debug("init session id: {}", sessionId);
        http.url(urlBase + "/session/" + sessionId);
        String windowId = http.path("window").get().jsonPath("$.value").asString();
        options.driverLogger.debug("init window id: {}", windowId);
        SafariWebDriver driver = new SafariWebDriver(options, command, http, sessionId, windowId);
        driver.activate();
        return driver;
    }

    @Override
    public void setDimensions(Map<String, Object> map) {
        Integer x = (Integer) map.remove("left");
        Integer y = (Integer) map.remove("top");
        // webdriver bug where 0 or 1 is mis-interpreted as boolean !
        if (x != null) {
            map.put("x", x < 2 ? 2 : x);
        }
        if (y != null) {
            map.put("y", y < 2 ? 2 : y);
        }
        String json = JsonUtils.toJson(map);
        http.path("window", "rect").post(json);
    }    

    @Override
    public void activate() {
        if (!options.headless) {
            try {
                switch (FileUtils.getOsType()) {
                    case MACOSX:
                        Runtime.getRuntime().exec(new String[]{"osascript", "-e", "tell app \"Safari\" to activate"});
                        break;
                    default:

                }
            } catch (Exception e) {
                logger.warn("native window switch failed: {}", e.getMessage());
            }
        }
    }

}
