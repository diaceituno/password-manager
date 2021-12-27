package drive.definition;

import drive.model.DriveFile;

import java.util.List;

public interface FileResolver {

    DriveRequestResult<DriveFile> resolve(List<String> keys);

}
