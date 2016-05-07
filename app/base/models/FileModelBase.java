package base.models;

import akka.actor.Scheduler;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.avaje.ebean.annotation.Transactional;
import base.MyConfigImplicit;
import base.controllers.EnvironmentAll;
import base.models.exceptions.UploadException;
import play.Logger;
import scala.Option;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.*;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@MappedSuperclass
public abstract class FileModelBase extends ModelBase {


    protected abstract String getFolder();
    protected abstract String getName();
    abstract public void setName(String name);
    protected abstract String getDownloadUrl();
    public String getKey() {
        return getFolder() + getName();
    }

    private FileModelBase(EnvironmentAll env){
        _config = MyConfigImplicit.MyConfig(env.config()).aws();
        _s3Client = new AmazonS3Client(new BasicAWSCredentials(_config.keyId(),_config.key()));
        _bucket = _config.s3BucketMain();
        _scheduler = env.akka().scheduler();
        _dispatcher = env.akka().dispatcher();
    }


    @Transient
    MyConfigImplicit.MyConfig.AwsConfig _config;
    @Transient
    private String _bucket;
    @Transient
    private AmazonS3 _s3Client;
    @Transient
    private Scheduler _scheduler;
    @Transient
    private ExecutionContextExecutor _dispatcher;

    private static String _defaultContentType = "application/octet-stream";
    private static int _uploadRetryCount = 5;



    @SuppressWarnings("unused")
    public FileModelBase(EnvironmentAll env, play.api.mvc.MultipartFormData.FilePart<play.api.libs.Files.TemporaryFile> upload) {
        this(env);
        setName(upload.filename());
        InputStream is;
        try {
            is = new FileInputStream(upload.ref().file());
        } catch (FileNotFoundException e) {
            throw new UploadException("Temp file not found during upload", e);
        }
        Option<String> contentType = upload.contentType();
        uploadToS3(is, contentType.isDefined()?contentType.get():_defaultContentType);
    }

    @SuppressWarnings("unused")
    public FileModelBase(EnvironmentAll env, InputStream is, String contentType){
        this(env);
        uploadToS3(is,contentType);
    }

    @SuppressWarnings("unused")
    public FileModelBase(EnvironmentAll env, InputStream is){
        this(env);
        uploadToS3(is,_defaultContentType);
    }



    protected Map<String, String> generateMetaData() {
        return null;
    }

    @Transactional
    private void uploadToS3(InputStream is, String contentType) {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        Map<String, String> userMetadata = generateMetaData();
        if (userMetadata != null) metadata.setUserMetadata(userMetadata);
        this.save(); //IMPORTANT to generate Id
        FileUploaded l = cachePut(is, id);
        final FileModelBase self = this;
        _scheduler.scheduleOnce(
                Duration.Zero(),
                ()->self.tryUploadS3(l,metadata,1),
                _dispatcher
        );
    }


    private void tryUploadS3(FileUploaded l, ObjectMetadata metadata, int tryCount) {
        try {
            if (!_config.disableUploads())
                _s3Client.putObject(_bucket, getKey(), l.getFile(), metadata);
        } catch (Exception e) {
            Logger.error("File upload failed " + getKey() + ". Try count " + tryCount);
            final FileModelBase self = this;
            if(tryCount<_uploadRetryCount)
                _scheduler.scheduleOnce(
                        Duration.Zero(),
                        ()->self.tryUploadS3(l,metadata,tryCount + 1),
                        _dispatcher
                );
        }
    }

    private FileUploaded getFileContentNoCache() {
        File tempFile;
        try {
            tempFile = File.createTempFile("file" + getFolder().replace('/', '.'), "." + id);
        } catch (IOException e) {
            throw new RuntimeException("File download, error creating temp file", e);
        }

        try {
            _s3Client.getObject(new GetObjectRequest(_bucket, getKey()), tempFile);
        } catch (AmazonS3Exception e) {
            if (_bucket.contains("dev")) {
                CopyObjectRequest copyObjRequest = new CopyObjectRequest(_bucket.replace("dev", ""), getKey(), _bucket, getKey());
                _s3Client.copyObject(copyObjRequest);
                _s3Client.getObject(new GetObjectRequest(_bucket, getKey()), tempFile);
            } else {
                throw e;
            }
        }
        FileUploaded l = cachePut(tempFile);
        return l;
    }

    @SuppressWarnings("unused")
    public InputStream getFileContent() {
        FileUploaded l = cacheGetItem();
        if (l == null) {
            return getFileContentNoCache().getFile();
        } else {
            InputStream r = l.getFile();
            if (r == null) {
                return getFileContentNoCache().getFile();
            } else {
                return r;
            }
        }
    }

    @SuppressWarnings("unused")
    public String getETag() {
        return _s3Client.getObjectMetadata(_bucket, getKey()).getETag();
    }

    @Override
    public String toString() {
        return getName();
    }

    private static final ConcurrentSkipListMap<String, FileUploaded> _uploadCache = new ConcurrentSkipListMap<>();

    private FileUploaded cachePut(File tmpFile) {
        FileUploaded f = this.new FileUploaded(tmpFile);
        _uploadCache.put(getKey(), f);
        return f;
    }

    private FileUploaded cachePut(InputStream is, Long id) {
        File tempFile;
        try {
            tempFile = File.createTempFile("file" + getFolder().replace('/', '.'), "." + id);
            Files.copy(is, tempFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Create temp file on upload failed", e);
        }
        return cachePut(tempFile);
    }

    private FileUploaded cacheGetItem() {
        return _uploadCache.get(getKey());
    }

    @SuppressWarnings("unused")
    public static void cacheRemoveOld(int hours) {
        long d = Calendar.getInstance().getTimeInMillis() - hours * 3600 * 1000;
        for (String id : _uploadCache.keySet()) {
            FileUploaded i = _uploadCache.get(id);
            //assuming map is sorted by date
            if (i.date > d) break;

            _uploadCache.remove(id);
            try {
                boolean r = i.file.delete();
                if(!r) Logger.warn("unsuccessful delete");
            } catch (Exception e) {
                Logger.warn("Delete old file error", e);
            }

        }
    }

    private class FileUploaded {
        FileUploaded(File tempFile) {
            this.file = tempFile;
            date = Calendar.getInstance().getTimeInMillis();
        }


        final long date;
        final File file;

        InputStream getFile() {
            InputStream is;
            try {
                is = new FileInputStream(file);
                return is;
            } catch (FileNotFoundException e) {
                Logger.error("Cache temp file missing", e);
                return null;
            }
        }
    }

}
