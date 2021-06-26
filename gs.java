/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
Refer the following link for original code of Google Services Gradle Plugin(this is modified from that code to use it with java):
https://github.com/google/play-services-plugins/tree/master/google-services-plugin
*/

import java.io.File;
import java.util.TreeMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.IOException;
import com.google.common.base.Strings;

public class gs{

    private static File intermediateDir;
    private static final String STATUS_DISABLED = "1";
    private static final String STATUS_ENABLED = "2";
    private static final String OAUTH_CLIENT_TYPE_WEB = "3";

    public static void main(String[] args) throws IOException{
        System.out.println("Started");
        File quickstartFile = new File("google-services.json");
        if (quickstartFile == null || !quickstartFile.isFile()) {
            System.out.println("The Google Services Plugin cannot function without it.");
        }
        System.out.println("Parsing json file: " + quickstartFile.getPath());

        // delete content of outputdir.
        intermediateDir = new File("Interm");
        deleteFolder(intermediateDir);
        if (!intermediateDir.mkdirs()) {
            System.out.println("Failed to create folder: " + intermediateDir);
        }

        try
        {
            JsonElement root = new JsonParser().parse(Files.newReader(quickstartFile, Charsets.UTF_8));
            if (!root.isJsonObject()) {
                System.out.println("Malformed root json");
            }

            JsonObject rootObject = root.getAsJsonObject();

            Map<String, String> resValues = new TreeMap<>();
            Map<String, Map<String, String>> resAttributes = new TreeMap<>();

            handleProjectNumberAndProjectId(rootObject, resValues);
            handleFirebaseUrl(rootObject, resValues);

            JsonObject clientObject = getClientForPackageName(rootObject);

            if (clientObject != null) {
                handleAnalytics(clientObject, resValues);
                handleMapsService(clientObject, resValues);
                handleGoogleApiKey(clientObject, resValues);
                handleGoogleAppId(clientObject, resValues);
                handleWebClientId(clientObject, resValues);

            } else {
                System.out.println("No matching client found for package name ");
            }

            // write the values file.
            File values = new File(intermediateDir, "values");
            if (!values.exists() && !values.mkdirs()) {
                System.out.println("Failed to create folder: " + values);
            }

            Files.asCharSink(new File(values, "values.xml"), Charsets.UTF_8)
                    .write(getValuesContent(resValues, resAttributes));


        }
        catch (IOException ex)
        {
            System.out.println("FileNotFoundException");
        }


    }

    static void deleteFolder(final File folder) {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    if (!file.delete()) {
                        System.out.println("Failed to delete: " + file);
                    }
                }
            }
        }
        if (!folder.delete()) {
            System.out.println("Failed to delete: " + folder);
        }
    }

    static void handleProjectNumberAndProjectId(JsonObject rootObject, Map<String, String> resValues)
            throws IOException {
        JsonObject projectInfo = rootObject.getAsJsonObject("project_info");
        if (projectInfo == null) {
            System.out.println("Missing project_info object");
        }

        JsonPrimitive projectNumber = projectInfo.getAsJsonPrimitive("project_number");
        if (projectNumber == null) {
            System.out.println("Missing project_info/project_number object");
        }

        resValues.put("gcm_defaultSenderId", projectNumber.getAsString());

        JsonPrimitive projectId = projectInfo.getAsJsonPrimitive("project_id");

        if (projectId == null) {
            System.out.println("Missing project_info/project_id object");
        }
        resValues.put("project_id", projectId.getAsString());

        JsonPrimitive bucketName = projectInfo.getAsJsonPrimitive("storage_bucket");
        if (bucketName != null) {
            resValues.put("google_storage_bucket", bucketName.getAsString());
        }
    }

    static void handleFirebaseUrl(JsonObject rootObject, Map<String, String> resValues)
            throws IOException {
        JsonObject projectInfo = rootObject.getAsJsonObject("project_info");
        if (projectInfo == null) {
            System.out.println("Missing project_info object");
        }

        JsonPrimitive firebaseUrl = projectInfo.getAsJsonPrimitive("firebase_url");
        if (firebaseUrl != null) {
            resValues.put("firebase_database_url", firebaseUrl.getAsString());
        }
    }

    static void handleAnalytics(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject analyticsService = getServiceByName(clientObject, "analytics_service");
        if (analyticsService == null) return;

        JsonObject analyticsProp = analyticsService.getAsJsonObject("analytics_property");
        if (analyticsProp == null) return;

        JsonPrimitive trackingId = analyticsProp.getAsJsonPrimitive("tracking_id");
        if (trackingId == null) return;

        resValues.put("ga_trackingId", trackingId.getAsString());

        File xml = new File(intermediateDir, "xml");
        if (!xml.exists() && !xml.mkdirs()) {
            System.out.println("Failed to create folder: " + xml);
        }

        Files.asCharSink(new File(xml, "global_tracker.xml"), Charsets.UTF_8)
                .write(getGlobalTrackerContent(trackingId.getAsString()));
    }

    static void handleMapsService(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject mapsService = getServiceByName(clientObject, "maps_service");
        if (mapsService == null) return;

        String apiKey = getAndroidApiKey(clientObject);
        if (apiKey != null) {
            resValues.put("google_maps_key", apiKey);
            return;
        }
        System.out.println("Missing api_key/current_key object");
    }

    static JsonObject getClientForPackageName(JsonObject jsonObject) {
        JsonArray array = jsonObject.getAsJsonArray("client");
        if (array != null) {
            final int count = array.size();
            for (int i = 0; i < count; i++) {
                JsonElement clientElement = array.get(i);
                if (clientElement == null || !clientElement.isJsonObject()) {
                    continue;
                }

                JsonObject clientObject = clientElement.getAsJsonObject();

                JsonObject clientInfo = clientObject.getAsJsonObject("client_info");
                if (clientInfo == null) continue;

                JsonObject androidClientInfo = clientInfo.getAsJsonObject("android_client_info");
                if (androidClientInfo == null) continue;

                JsonPrimitive clientPackageName = androidClientInfo.getAsJsonPrimitive("package_name");
                if (clientPackageName == null) continue;

                if (getApplicationId().equals(clientPackageName.getAsString())) {
                    return clientObject;
                }
            }
        }

        return null;
    }

    static JsonObject getServiceByName(JsonObject clientObject, String serviceName) {
        JsonObject services = clientObject.getAsJsonObject("services");
        if (services == null) return null;

        JsonObject service = services.getAsJsonObject(serviceName);
        if (service == null) return null;

        JsonPrimitive status = service.getAsJsonPrimitive("status");
        if (status == null) return null;

        String statusStr = status.getAsString();

        if (STATUS_DISABLED.equals(statusStr)) return null;
        if (!STATUS_ENABLED.equals(statusStr)) {
            System.out.println(
                            String.format(
                                    "Status with value '%1$s' for service '%2$s' is unknown",
                                    statusStr, serviceName));
            return null;
        }

        return service;
    }

    static String getGlobalTrackerContent(String ga_trackingId) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "    <string name=\"ga_trackingId\" translatable=\"false\">"
                + ga_trackingId
                + "</string>\n"
                + "</resources>\n";
    }

    static String getAndroidApiKey(JsonObject clientObject) {
        JsonArray array = clientObject.getAsJsonArray("api_key");
        if (array != null) {
            final int count = array.size();
            for (int i = 0; i < count; i++) {
                JsonElement apiKeyElement = array.get(i);
                if (apiKeyElement == null || !apiKeyElement.isJsonObject()) {
                    continue;
                }
                JsonObject apiKeyObject = apiKeyElement.getAsJsonObject();
                JsonPrimitive currentKey = apiKeyObject.getAsJsonPrimitive("current_key");
                if (currentKey == null) {
                    continue;
                }
                return currentKey.getAsString();
            }
        }
        return null;
    }

    static String getApplicationId(){
        return "com.example.testjsontostrings";
    }

    static void handleGoogleApiKey(JsonObject clientObject, Map<String, String> resValues) {
        String apiKey = getAndroidApiKey(clientObject);
        if (apiKey != null) {
            resValues.put("google_api_key", apiKey);
            // TODO: remove this once SDK starts to use google_api_key.
            resValues.put("google_crash_reporting_api_key", apiKey);
            return;
        }

        // if google_crash_reporting_api_key is missing.
        System.out.println("Missing api_key/current_key object");
    }

    static void handleGoogleAppId(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject clientInfo = clientObject.getAsJsonObject("client_info");
        if (clientInfo == null) {
            // Should not happen
            System.out.println("Client does not have client info");
        }

        JsonPrimitive googleAppId = clientInfo.getAsJsonPrimitive("mobilesdk_app_id");

        String googleAppIdStr = googleAppId == null ? null : googleAppId.getAsString();
        if (Strings.isNullOrEmpty(googleAppIdStr)) {
            System.out.println(
                    "Missing Google App Id. "
                            + "Please follow instructions on https://firebase.google.com/ to get a valid "
                            + "config file that contains a Google App Id");
        }

        resValues.put("google_app_id", googleAppIdStr);
    }

    static void handleWebClientId(JsonObject clientObject, Map<String, String> resValues) {
        JsonArray array = clientObject.getAsJsonArray("oauth_client");
        if (array != null) {
            final int count = array.size();
            for (int i = 0; i < count; i++) {
                JsonElement oauthClientElement = array.get(i);
                if (oauthClientElement == null || !oauthClientElement.isJsonObject()) {
                    continue;
                }
                JsonObject oauthClientObject = oauthClientElement.getAsJsonObject();
                JsonPrimitive clientType = oauthClientObject.getAsJsonPrimitive("client_type");
                if (clientType == null) {
                    continue;
                }
                String clientTypeStr = clientType.getAsString();
                if (!OAUTH_CLIENT_TYPE_WEB.equals(clientTypeStr)) {
                    continue;
                }
                JsonPrimitive clientId = oauthClientObject.getAsJsonPrimitive("client_id");
                if (clientId == null) {
                    continue;
                }
                resValues.put("default_web_client_id", clientId.getAsString());
                return;
            }
        }
    }

    static String getValuesContent(
            Map<String, String> values, Map<String, Map<String, String>> attributes) {
        StringBuilder sb = new StringBuilder(256);

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n");

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String name = entry.getKey();
            sb.append("    <string name=\"").append(name).append("\" translatable=\"false\"");
            if (attributes.containsKey(name)) {
                for (Map.Entry<String, String> attr : attributes.get(name).entrySet()) {
                    sb.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
                }
            }
            sb.append(">").append(entry.getValue()).append("</string>\n");
        }

        sb.append("</resources>\n");

        return sb.toString();
    }

}
