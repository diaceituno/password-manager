package drive.implementation;

import auth.definition.TokenHolder;
import com.google.gson.Gson;
import drive.definition.FileRequestResult;
import drive.definition.FileResolver;
import drive.model.DriveFile;
import drive.model.ListFilesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class FileFromNameResolver implements FileResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFromNameResolver.class);
    private static final Gson GSON = new Gson();

    @Value("${drive.file.url}")
    private String url;

    @Autowired
    private TokenHolder tokenHolder;

    @Override
    public FileRequestResult<DriveFile> resolve(List<String> keys) {
        try {
            DriveFile driveFile = findFile(getFileName(keys));
            return new FileRequestResult<>(200, null,driveFile);
        }catch(ResponseStatusException e) {
            LOGGER.error("Could not resolve file",e);
            return new FileRequestResult<>(e.getRawStatusCode(), "Vault File not found", null);
        }catch (IOException e) {
            LOGGER.error("Error occurred while resolving file", e);
            return new FileRequestResult<>(500, e.getMessage(), null);
        }
    }

    private DriveFile findFile(String fileName) throws IOException {
        return fetchFiles(fileName).stream()//
                .findFirst()//
                .orElseThrow((() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"")));
    }


    private List<DriveFile> fetchFiles(String fileName) throws IOException {
        HttpURLConnection connection = getConnection(fileName);
        connection.setRequestMethod(HttpMethod.GET.toString());
        connection.setRequestProperty(HttpHeaders.AUTHORIZATION, tokenHolder.getToken());
        InputStream inputStream = connection.getInputStream();
        return readResponse(inputStream);
    }

    private List<DriveFile> readResponse(InputStream connectionInputStream) throws IOException {
        String responseBody = new String(connectionInputStream.readAllBytes(), StandardCharsets.UTF_8);
        return GSON.fromJson(responseBody, ListFilesResponse.class).getFiles();
    }


    private HttpURLConnection getConnection(String fileName) throws IOException {
        return (HttpURLConnection) getListFilesUrl(fileName).openConnection();
    }

    private URL getListFilesUrl(String fileName) throws MalformedURLException, UnsupportedEncodingException {
        return new URL(url + "?q=" + URLEncoder.encode("name = '" + fileName +"'", StandardCharsets.UTF_8.toString()));
    }

    private String getFileName(List<String> keys) {
        return keys.get(0);
    }
}
