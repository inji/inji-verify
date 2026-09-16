package api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.testrig.apirig.utils.RestClient;
import io.restassured.response.Response;

/**
 * Loads verify-ui {@code config.json} and resolves credential display names used in VP selection UI.
 * Credential keys in {@code injiVerify.properties} map to DCQL credential ids; display names come from config.
 */
public final class VerifiableClaimsConfigManager {

    private static final Logger LOGGER = Logger.getLogger(VerifiableClaimsConfigManager.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CREDENTIAL_KEY_PREFIX = "uiAuto.credential.";
    private static final String FALLBACK_CONFIG_RESOURCE = "config/config.json";

    private static volatile boolean initialized;
    private static volatile Map<String, String> credentialIdToName = Collections.emptyMap();
    private static volatile List<String> claimDisplayNames = Collections.emptyList();
    private static volatile String essentialCredentialId;
    private static volatile String essentialCredentialName;

    private VerifiableClaimsConfigManager() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            JsonNode config = loadConfigJson();
            try {
                applyConfig(config);
            } catch (RuntimeException remoteOrPrimaryFailure) {
                // Remote config may return 200 with a different/empty schema; fall back to bundled config.
                LOGGER.warn("Primary config.json could not be used (" + remoteOrPrimaryFailure.getMessage()
                        + "). Falling back to classpath resource.");
                applyConfig(readJsonFromClasspath(FALLBACK_CONFIG_RESOURCE));
            }
            initialized = true;
            LOGGER.info("Loaded " + claimDisplayNames.size() + " verifiable claim(s) from config.json"
                    + (essentialCredentialName != null ? "; essential=" + essentialCredentialName : ""));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize verifiable claims config", e);
        }
    }

    public static String getCredentialName(String credentialKey) {
        ensureInitialized();
        String credentialId = InjiVerifyConfigManager.getproperty(CREDENTIAL_KEY_PREFIX + credentialKey);
        if (credentialId == null || credentialId.trim().isEmpty()) {
            throw new RuntimeException("Missing property '" + CREDENTIAL_KEY_PREFIX + credentialKey
                    + "' in injiVerify.properties");
        }
        return getCredentialNameById(credentialId.trim());
    }

    public static String getCredentialNameById(String credentialId) {
        ensureInitialized();
        String name = credentialIdToName.get(credentialId);
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Credential id '" + credentialId + "' not found in config.json verifiableClaims");
        }
        return name;
    }

    public static String getEssentialCredentialName() {
        ensureInitialized();

        // Prefer the claim marked essential:true. Do not resolve via DCQL id — combined
        // claims can reuse that id (e.g. "MOSIP ID + Life Insurance") while the UI
        // auto-selects the essential claim by name ("MOSIP ID").
        if (essentialCredentialName != null && !essentialCredentialName.trim().isEmpty()) {
            try {
                String propertyId = InjiVerifyConfigManager.getproperty("uiAuto.credential.essential");
                if (propertyId != null && !propertyId.trim().isEmpty()
                        && essentialCredentialId != null
                        && !propertyId.trim().equals(essentialCredentialId.trim())) {
                    LOGGER.warn("uiAuto.credential.essential='" + propertyId.trim()
                            + "' differs from config.json essential credential id='" + essentialCredentialId.trim()
                            + "'. Using config.json essential claim '" + essentialCredentialName + "'.");
                }
            } catch (RuntimeException e) {
                LOGGER.debug("Skipping essential property comparison: " + e.getMessage());
            }
            return essentialCredentialName;
        }

        if (essentialCredentialId != null && !essentialCredentialId.trim().isEmpty()) {
            return getCredentialNameById(essentialCredentialId.trim());
        }

        // Fall back to property as a DCQL credential id (not a short key suffix).
        String configuredId = InjiVerifyConfigManager.getproperty("uiAuto.credential.essential");
        if (configuredId != null && !configuredId.trim().isEmpty()) {
            return getCredentialNameById(configuredId.trim());
        }

        for (Map.Entry<String, String> entry : credentialIdToName.entrySet()) {
            if ("mosip_verifiable_credential_id".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("No essential credential found in config.json");
    }

    /**
     * Resolves a Cucumber/step argument to the UI display name.
     * Accepts either a {@code uiAuto.credential.*} property key (e.g. {@code healthInsurance})
     * or the config.json display name (e.g. {@code Health Insurance}).
     */
    public static String resolveCredentialName(String keyOrDisplayName) {
        ensureInitialized();
        if (keyOrDisplayName == null || keyOrDisplayName.trim().isEmpty()) {
            throw new RuntimeException("Credential name/key must not be blank");
        }
        String trimmed = keyOrDisplayName.trim();
        String propertyId = InjiVerifyConfigManager.getproperty(CREDENTIAL_KEY_PREFIX + trimmed);
        if (propertyId != null && !propertyId.trim().isEmpty()) {
            return getCredentialNameById(propertyId.trim());
        }
        for (String displayName : claimDisplayNames) {
            if (displayName.equalsIgnoreCase(trimmed)) {
                return displayName;
            }
        }
        throw new RuntimeException("Unknown credential '" + trimmed
                + "'. Use a uiAuto.credential.* key or a display name from config.json verifiableClaims.");
    }

    public static java.util.List<String> getNonEssentialCredentialNames() {
        ensureInitialized();
        String essentialName = getEssentialCredentialName();
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String displayName : claimDisplayNames) {
            if (!displayName.equals(essentialName)) {
                names.add(displayName);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    private static void applyConfig(JsonNode config) {
        MapBuildResult result = buildCredentialIdToNameMap(config);
        credentialIdToName = result.idToName;
        claimDisplayNames = result.claimNames;
        essentialCredentialId = result.essentialCredentialId;
        essentialCredentialName = result.essentialCredentialName;
    }

    private static JsonNode loadConfigJson() throws IOException {
        String configuredUrl = InjiVerifyConfigManager.getproperty("uiAuto.configJsonUrl");
        if (configuredUrl != null && !configuredUrl.trim().isEmpty()) {
            try {
                return readJsonFromUrl(configuredUrl.trim());
            } catch (IOException e) {
                LOGGER.warn("Unable to fetch config.json from uiAuto.configJsonUrl. Falling back to classpath resource. "
                        + e.getMessage());
            }
        }

        String injiVerifyUrl = InjiVerifyConfigManager.getInjiVerifyUi();
        if (injiVerifyUrl != null && !injiVerifyUrl.trim().isEmpty()) {
            String normalizedBase = injiVerifyUrl.trim();
            if (!normalizedBase.endsWith("/")) {
                normalizedBase = normalizedBase + "/";
            }
            try {
                return readJsonFromUrl(normalizedBase + "assets/config.json");
            } catch (IOException e) {
                LOGGER.warn("Unable to fetch config.json from deployed inji-verify UI. Falling back to classpath resource. "
                        + e.getMessage());
            }
        }

        return readJsonFromClasspath(FALLBACK_CONFIG_RESOURCE);
    }

    private static JsonNode readJsonFromUrl(String configUrl) throws IOException {
        try {
            Response response = RestClient.getRequest(configUrl, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
            if (response == null) {
                throw new IOException("Empty response fetching config.json from " + configUrl);
            }
            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                throw new IOException("Failed to fetch config.json from " + configUrl + ". HTTP status: " + statusCode);
            }
            String body = response.getBody() != null ? response.getBody().asString() : null;
            if (body == null || body.trim().isEmpty()) {
                throw new IOException("Empty body fetching config.json from " + configUrl);
            }
            return OBJECT_MAPPER.readTree(body);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to fetch config.json from " + configUrl + ": " + e.getMessage(), e);
        }
    }

    private static JsonNode readJsonFromClasspath(String resourcePath) throws IOException {
        InputStream inputStream = VerifiableClaimsConfigManager.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Classpath config resource not found: " + resourcePath);
        }
        try (InputStream stream = inputStream) {
            return OBJECT_MAPPER.readTree(stream);
        }
    }

    private static MapBuildResult buildCredentialIdToNameMap(JsonNode config) {
        JsonNode verifiableClaims = config.path("verifiableClaims");
        if (!verifiableClaims.isArray()) {
            throw new RuntimeException("config.json is missing verifiableClaims array");
        }

        Map<String, String> idToName = new HashMap<>();
        Set<String> standaloneIds = new HashSet<>();
        LinkedHashSet<String> claimNames = new LinkedHashSet<>();
        String essentialId = null;
        String essentialName = null;
        Iterator<JsonNode> claims = verifiableClaims.elements();
        while (claims.hasNext()) {
            JsonNode claim = claims.next();
            String displayName = claim.path("name").asText(null);
            if (displayName == null || displayName.trim().isEmpty()) {
                continue;
            }

            JsonNode credentials = claim.path("dcqlQuery").path("credentials");
            if (!credentials.isArray() || credentials.isEmpty()) {
                continue;
            }
            String trimmedName = displayName.trim();
            claimNames.add(trimmedName);
            boolean claimIsEssential = claim.path("essential").asBoolean(false);
            boolean claimIsStandalone = credentials.size() == 1;

            if (claimIsEssential) {
                if (essentialName != null && !essentialName.equals(trimmedName)) {
                    LOGGER.warn("Multiple essential:true claims in config.json. Keeping '"
                            + essentialName + "', ignoring '" + trimmedName + "'.");
                } else {
                    essentialName = trimmedName;
                }
            }

            for (JsonNode credential : credentials) {
                String credentialId = credential.path("id").asText(null);
                if (credentialId == null || credentialId.trim().isEmpty()) {
                    continue;
                }
                String trimmedId = credentialId.trim();
                if (claimIsEssential && essentialId == null) {
                    essentialId = trimmedId;
                }

                String previousName = idToName.get(trimmedId);
                if (previousName == null) {
                    idToName.put(trimmedId, trimmedName);
                    if (claimIsStandalone) {
                        standaloneIds.add(trimmedId);
                    }
                } else if (previousName.equals(trimmedName)) {
                    if (claimIsStandalone) {
                        standaloneIds.add(trimmedId);
                    }
                } else if (standaloneIds.contains(trimmedId) && !claimIsStandalone) {
                    LOGGER.warn("Duplicate DCQL credential id '" + trimmedId
                            + "' in config.json. Keeping standalone claim '"
                            + previousName + "', ignoring combined claim '" + trimmedName + "'.");
                } else if (!standaloneIds.contains(trimmedId) && claimIsStandalone) {
                    LOGGER.warn("Duplicate DCQL credential id '" + trimmedId
                            + "' in config.json. Preferring standalone claim '"
                            + trimmedName + "' over '" + previousName + "'.");
                    idToName.put(trimmedId, trimmedName);
                    standaloneIds.add(trimmedId);
                } else {
                    LOGGER.warn("Duplicate DCQL credential id '" + trimmedId
                            + "' in config.json verifiableClaims. Overwriting display name '"
                            + previousName + "' with '" + trimmedName + "'.");
                    idToName.put(trimmedId, trimmedName);
                }
            }
        }

        if (idToName.isEmpty()) {
            throw new RuntimeException("No credential definitions found in config.json verifiableClaims");
        }
        return new MapBuildResult(Collections.unmodifiableMap(idToName),
                Collections.unmodifiableList(new ArrayList<>(claimNames)),
                essentialId, essentialName);
    }

    private static final class MapBuildResult {
        private final Map<String, String> idToName;
        private final List<String> claimNames;
        private final String essentialCredentialId;
        private final String essentialCredentialName;

        private MapBuildResult(Map<String, String> idToName, List<String> claimNames,
                String essentialCredentialId, String essentialCredentialName) {
            this.idToName = idToName;
            this.claimNames = claimNames;
            this.essentialCredentialId = essentialCredentialId;
            this.essentialCredentialName = essentialCredentialName;
        }
    }

    static void resetForTests() {
        initialized = false;
        credentialIdToName = Collections.emptyMap();
        claimDisplayNames = Collections.emptyList();
        essentialCredentialId = null;
        essentialCredentialName = null;
    }

    static void loadFromJsonForTests(String json) throws IOException {
        resetForTests();
        applyConfig(OBJECT_MAPPER.readTree(json));
        initialized = true;
    }
}
