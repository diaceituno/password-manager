package drive.implementation;

import auth.definition.TokenHolder;
import drive.definition.BlobDownloader;
import drive.definition.DriveRequestResult;
import drive.definition.FileResolver;
import drive.model.DriveFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Component
public class PasswordFileDownloader implements BlobDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordFileDownloader.class);
    private static final String MEDIA_TYPE_PARAMETER = "?alt=media";

    @Value("${drive.file.url}")
    private String url;

    @Value("${vault.file.name}")
    private String vaultName;

    @Autowired
    private FileResolver driveFileResolver;

    @Autowired
    private TokenHolder tokenHolder;

    @Override
    public DriveRequestResult<byte[]> download() {
        DriveRequestResult resolutionResult = driveFileResolver.resolve(List.of(vaultName));
        if (resolutionResult.getStatusCode() == 200) {
            return downloadDriveFileContent(resolutionResult);
        }
        return new DriveRequestResult<byte[]>(resolutionResult.getStatusCode(), resolutionResult.getMessage(), null);
    }

    private DriveRequestResult<byte[]> downloadDriveFileContent(DriveRequestResult<DriveFile> resolutionResult) {
        try {
            HttpURLConnection connection = getConnection(resolutionResult.getData());
            connection.setRequestMethod(HttpMethod.GET.toString());
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, tokenHolder.getToken());
            return new DriveRequestResult(//
                    connection.getResponseCode(), //
                    connection.getResponseMessage(), //
                    connection.getInputStream().readAllBytes());
        } catch (IOException e) {
            LOGGER.error("Error occurred while attemping to download vault file", e);
            return new DriveRequestResult(500, e.getMessage(), null);
        }
    }

    private HttpURLConnection getConnection(DriveFile driveFile) throws IOException {
        return (HttpURLConnection) getDownloadFilesURL(driveFile.getId()).openConnection();
    }

    private URL getDownloadFilesURL(String fileId) throws MalformedURLException {
        return new URL(url + "/" + fileId + MEDIA_TYPE_PARAMETER);
    }
}
