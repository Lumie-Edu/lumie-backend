package com.lumie.app.security.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.junit.jupiter.api.Tag;

@Suite
@Tag("bdd")
@IncludeEngines("cucumber")
@SelectClasspathResource("features/bdd")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.lumie.app")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty,json:build/reports/cucumber/bdd.json,html:build/reports/cucumber/bdd.html")
class StaffApiBddTest {
}
