package drive.implementation;

import auth.definition.TokenHolder;
import com.google.gson.JsonObject;
import drive.definition.BlobUploader;
import drive.definition.FileRequestResult;
import drive.definition.FileResolver;
import drive.model.DriveFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class PasswordFileUploader implements BlobUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordFileUploader.class);

    //Simple Upload
    private static final String MEDIA_UPLOAD_TYPE = "?uploadType=media";
    private static final String METHOD_OVERRIDE = "X-HTTP-Method-Override";

    //Multipart Upload
    private static final String MULTIPART_UPLOAD_TYPE = "?uploadType=multipart";
    private static final String BOUNDARY = UUID.randomUUID().toString();
    private static final String PART_BOUNDARY = "--" + BOUNDARY;
    private static final String FINAL_BOUNDARY = PART_BOUNDARY + "--";
    private static final String MULTIPART_CONTENT_TYPE = "multipart/related; boundary=" + BOUNDARY;
    private static final String NEW_LINE = "\r\n";

    //File Metadata
    private static final String METADATA_CONTENT_TYPE = HttpHeaders.CONTENT_TYPE + ": " + MediaType.APPLICATION_JSON_VALUE;
    private static final String OCTET_STREAM_CONTENT_TYPE = HttpHeaders.CONTENT_TYPE + ": " + MediaType.APPLICATION_OCTET_STREAM_VALUE;
    private static final String FILE_NAME_METADATA_KEY = "name";

    @Value("${drive.file.name:vault}")
    private String vaultFileName;

    @Value("${drive.upload.url}")
    private String url;

    @Autowired
    private TokenHolder tokenHolder;

    @Autowired
    private FileResolver fileResolver;

    @Override
    public FileRequestResult<Void> upload(byte[] blob) {
        try {
            String existingVaultFileId = getExistingVaultFileId();
            String requestBody = buildRequestBody(blob, existingVaultFileId);
            HttpURLConnection connection = getConnection(requestBody.getBytes(StandardCharsets.UTF_8).length, getExistingVaultFileId());
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            return new FileRequestResult<>(connection.getResponseCode(), connection.getResponseMessage(), null);
        } catch (Exception e) {
            LOGGER.error("Error occurred while attempting to upload file", e);
            return new FileRequestResult<>(500, e.getMessage(), null);
        }
    }

    private String getExistingVaultFileId() {
        FileRequestResult<DriveFile> result = fileResolver.resolve(List.of(vaultFileName));
        if (result.getStatusCode() == 200) {
            return result.getData().getId();
        }
        return null;
    }

    private String buildRequestBody(byte[] blob, String existingVaultFileId) {
        if (existingVaultFileId != null) {
            return encodeBinaryData(blob);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(PART_BOUNDARY);
        stringBuilder.append(NEW_LINE);
        stringBuilder.append(buildMetadataPart());
        stringBuilder.append(NEW_LINE);
        stringBuilder.append(PART_BOUNDARY);
        stringBuilder.append(NEW_LINE);
        stringBuilder.append(buildOctetStreamPart(blob));
        stringBuilder.append(NEW_LINE);
        stringBuilder.append(FINAL_BOUNDARY);
        return stringBuilder.toString();
    }

    private String buildMetadataPart() {
        JsonObject metadata = new JsonObject();
        metadata.addProperty(FILE_NAME_METADATA_KEY, vaultFileName);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(METADATA_CONTENT_TYPE);
        stringBuilder.append(NEW_LINE).append(NEW_LINE);
        stringBuilder.append(metadata.toString());
        return stringBuilder.toString();
    }

    private String buildOctetStreamPart(byte[] blob) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(OCTET_STREAM_CONTENT_TYPE);
        stringBuilder.append(NEW_LINE).append(NEW_LINE);
        stringBuilder.append(encodeBinaryData(blob));
        return stringBuilder.toString();
    }

    private String encodeBinaryData(byte[] blob) {
        return Base64.getEncoder().encodeToString(blob);
    }


    private HttpURLConnection getConnection(int contentLength, String fileId) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getUploadUrl(fileId).openConnection();
        connection.setRequestMethod(HttpMethod.POST.toString());
        if (fileId != null) {
            connection.setRequestProperty(METHOD_OVERRIDE, HttpMethod.PATCH.toString());
        }
        connection.setRequestProperty(HttpHeaders.AUTHORIZATION, tokenHolder.getToken());
        connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MULTIPART_CONTENT_TYPE);
        connection.setRequestProperty(HttpHeaders.CONTENT_LENGTH, "" + contentLength);
        connection.setDoOutput(true);
        return connection;
    }


    private URL getUploadUrl(String fileId) throws MalformedURLException {
        if (fileId != null) {
            return new URL(url + "/" + fileId + MEDIA_UPLOAD_TYPE);
        }
        return new URL(url + MULTIPART_UPLOAD_TYPE);
    }

}
